package io.github.halilozel1903.tvplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import io.github.halilozel1903.tvplayer.core.Chapter
import io.github.halilozel1903.tvplayer.core.Chapters
import io.github.halilozel1903.tvplayer.core.ControlsEvent
import io.github.halilozel1903.tvplayer.core.ControlsState
import io.github.halilozel1903.tvplayer.core.PlaybackSnapshot
import io.github.halilozel1903.tvplayer.core.ScrubState
import io.github.halilozel1903.tvplayer.core.Scrubber
import io.github.halilozel1903.tvplayer.core.SeekAcceleration
import io.github.halilozel1903.tvplayer.core.SeekDirection
import io.github.halilozel1903.tvplayer.core.SkipRules
import io.github.halilozel1903.tvplayer.core.SkipWindow
import io.github.halilozel1903.tvplayer.core.Timeline
import io.github.halilozel1903.tvplayer.core.TrackMenuItem
import kotlinx.coroutines.delay

/**
 * Behaviour of [TvPlayerState].
 *
 * @param autoHideMillis hide the controls after this long without input while playing; `null`
 *   keeps them up.
 * @param showControlsOnStart show the controls when the player appears.
 * @param scrubCommitDelayMillis seek to the previewed position this long after the last D-pad
 *   press on the seek bar; `null` waits for the center button.
 * @param seekStepMillis the jump of the rewind and forward buttons.
 * @param acceleration step sizes while scrubbing with the D-pad.
 * @param chapterSnapMillis how close a scrub must land to a chapter start to stop on it; `0`
 *   disables snapping.
 * @param skipRules when the skip intro and skip credits button shows.
 * @param pollIntervalMillis how often the position is read from the player.
 */
@Immutable
public data class TvPlayerConfig(
    val autoHideMillis: Long? = 5_000L,
    val showControlsOnStart: Boolean = true,
    val scrubCommitDelayMillis: Long? = 1_200L,
    val seekStepMillis: Long = 10_000L,
    val acceleration: SeekAcceleration = SeekAcceleration(),
    val chapterSnapMillis: Long = 4_000L,
    val skipRules: SkipRules = SkipRules(),
    val pollIntervalMillis: Long = 250L,
)

/** A control that [TvPlayerState.requestFocus] can move the focus to. */
public enum class PlayerFocusTarget {
    PlayPause,
    SeekBar,
    SkipButton,
    TracksButton,
}

/**
 * State of a TV player screen: the latest [snapshot] of the [engine], whether the controls are
 * up, the D-pad scrub in progress, the skip button and the subtitle and audio panel.
 *
 * Create it with [rememberTvPlayerState]. All functions must be called on the main thread.
 */
@Stable
public class TvPlayerState internal constructor(
    public val engine: PlayerEngine,
    chapters: List<Chapter>,
    skipWindows: List<SkipWindow>,
    config: TvPlayerConfig,
) {
    private var chapterInput: List<Chapter> = chapters

    /** Chapters of the media, sorted. */
    public var chapters: Chapters by mutableStateOf(Chapters(chapters))
        private set

    /** Skippable windows such as the intro and the credits. */
    public var skipWindows: List<SkipWindow> by mutableStateOf(skipWindows)
        private set

    /** The current behaviour. */
    public var config: TvPlayerConfig by mutableStateOf(config)
        private set

    /** The latest player state; refreshed every [TvPlayerConfig.pollIntervalMillis] and on player events. */
    public var snapshot: PlaybackSnapshot by mutableStateOf(engine.snapshot())
        private set

    private var controls by mutableStateOf(
        ControlsState(visible = config.showControlsOnStart, autoHideMillis = config.autoHideMillis, isPlaying = snapshot.isPlaying),
    )

    /** The scrub in progress, or `null`. */
    public var scrub: ScrubState? by mutableStateOf(null)
        private set

    /** `true` while the subtitle and audio panel is open. */
    public var isTrackPanelOpen: Boolean by mutableStateOf(false)
        private set

    /** A focus move asked for with [requestFocus] that the controls have not done yet. */
    public var pendingFocus: PlayerFocusTarget? by mutableStateOf(null)
        private set

    private var scrubAutoCommit = true
    private var scrubIdleMillis = 0L

    /** Whether the controls overlay is on screen. */
    public val controlsVisible: Boolean get() = controls.visible

    /** `true` while the viewer scrubs the seek bar. */
    public val isScrubbing: Boolean get() = scrub != null

    /** The playhead. */
    public val positionMillis: Long get() = snapshot.positionMillis

    /** The duration, or a negative value while unknown. */
    public val durationMillis: Long get() = snapshot.durationMillis

    /** The position the seek bar shows: the preview while scrubbing, else the playhead. */
    public val displayPositionMillis: Long get() = scrub?.previewMillis ?: snapshot.positionMillis

    /** [displayPositionMillis] as a fraction of the duration. */
    public val displayFraction: Float get() = Timeline.fraction(displayPositionMillis, snapshot.durationMillis)

    /** The chapter at [displayPositionMillis]. */
    public val currentChapter: Chapter? get() = chapters.chapterAt(displayPositionMillis)

    /** The skip window whose button should be on screen now, or `null`. */
    public val visibleSkipWindow: SkipWindow?
        get() = if (isScrubbing || isTrackPanelOpen) {
            null
        } else {
            config.skipRules.visibleWindow(skipWindows, snapshot.positionMillis, controls.visible)
        }

    public fun play() {
        engine.play()
        refresh()
    }

    public fun pause() {
        engine.pause()
        refresh()
    }

    public fun togglePlayPause() {
        if (snapshot.isPlaying) pause() else play()
        onUserInteraction()
    }

    /** Seeks to [positionMillis], clamped to the media. Ends a scrub in progress. */
    public fun seekTo(positionMillis: Long) {
        if (scrub != null) endScrub()
        engine.seekTo(Timeline.seekBy(positionMillis, 0L, snapshot.durationMillis))
        refresh()
        onUserInteraction()
    }

    /** Seeks relative to the playhead, for rewind and forward buttons. */
    public fun seekBy(deltaMillis: Long) {
        seekTo(Timeline.seekBy(snapshot.positionMillis, deltaMillis, snapshot.durationMillis))
    }

    /**
     * Moves the seek preview by one D-pad press. The first call starts a scrub at the playhead;
     * the player seeks when the scrub is committed (center button, or after
     * [TvPlayerConfig.scrubCommitDelayMillis] without presses).
     *
     * @param repeatCount the key event's repeat count; holding the key accelerates.
     */
    public fun scrub(direction: SeekDirection, repeatCount: Int = 0) {
        val scrubber = scrubber()
        val current = scrub ?: scrubber.start(snapshot.positionMillis)
        updateScrub(scrubber.step(current, direction, repeatCount), autoCommit = true)
    }

    /**
     * Moves the seek preview to [positionMillis] directly.
     *
     * @param autoCommit seek after [TvPlayerConfig.scrubCommitDelayMillis]; `false` keeps the
     *   preview up until [commitScrub] or [cancelScrub].
     */
    public fun scrubTo(positionMillis: Long, autoCommit: Boolean = true) {
        val scrubber = scrubber()
        val current = scrub ?: scrubber.start(snapshot.positionMillis)
        updateScrub(scrubber.moveTo(current, positionMillis), autoCommit)
    }

    /** Seeks to the previewed position and ends the scrub. */
    public fun commitScrub() {
        val current = scrub ?: return
        endScrub()
        engine.seekTo(current.previewMillis)
        refresh()
    }

    /** Ends the scrub without seeking. */
    public fun cancelScrub() {
        if (scrub == null) return
        endScrub()
    }

    /** Seeks past [window], by default the one whose button is visible. */
    public fun skip(window: SkipWindow? = visibleSkipWindow) {
        val target = window ?: return
        seekTo(target.targetMillis)
    }

    /** Opens the subtitle and audio panel. */
    public fun openTrackPanel() {
        cancelScrub()
        isTrackPanelOpen = true
        controls = controls.reduce(ControlsEvent.PanelChanged(true))
    }

    /** Closes the subtitle and audio panel and returns the focus to its button. */
    public fun closeTrackPanel() {
        if (!isTrackPanelOpen) return
        isTrackPanelOpen = false
        controls = controls.reduce(ControlsEvent.PanelChanged(false))
        requestFocus(PlayerFocusTarget.TracksButton)
    }

    /** Selects a row of the subtitle or audio menu; the "Off" row turns subtitles off. */
    public fun selectTrack(item: TrackMenuItem) {
        val track = item.track
        if (track == null) engine.disableSubtitles() else engine.selectTrack(track)
        refresh()
        onUserInteraction()
    }

    public fun showControls() {
        controls = controls.reduce(ControlsEvent.Show)
    }

    /** Hides the controls, unless a scrub or the panel holds them up. */
    public fun hideControls() {
        controls = controls.reduce(ControlsEvent.Hide)
    }

    /** Restarts the auto-hide timer; the controls call this on every key press. */
    public fun onUserInteraction() {
        controls = controls.reduce(ControlsEvent.Interaction)
    }

    /** Asks the controls to focus [target] as soon as it is on screen. Shows the controls. */
    public fun requestFocus(target: PlayerFocusTarget) {
        pendingFocus = target
        if (target != PlayerFocusTarget.SkipButton) showControls()
    }

    internal fun consumePendingFocus(target: PlayerFocusTarget) {
        if (pendingFocus == target) pendingFocus = null
    }

    /** `true` when the Back key has something to close: the panel, a scrub or the controls. */
    public val canHandleBack: Boolean
        get() = isTrackPanelOpen || isScrubbing || controls.visible

    /**
     * Handles Back: closes the panel, cancels a scrub or hides the controls, in that order.
     *
     * @return `false` when there was nothing to close, so the screen should go back.
     */
    public fun handleBack(): Boolean = when {
        isTrackPanelOpen -> {
            closeTrackPanel()
            true
        }
        isScrubbing -> {
            cancelScrub()
            true
        }
        controls.visible -> {
            hideControls()
            true
        }
        else -> false
    }

    internal fun update(chapters: List<Chapter>, skipWindows: List<SkipWindow>, config: TvPlayerConfig) {
        if (chapters != chapterInput) {
            chapterInput = chapters
            this.chapters = Chapters(chapters)
        }
        this.skipWindows = skipWindows
        if (config != this.config) {
            this.config = config
            controls = controls.copy(autoHideMillis = config.autoHideMillis)
        }
    }

    /** Reads the engine, runs the auto-hide timer and commits idle scrubs until cancelled. */
    internal suspend fun run() {
        val subscription = engine.addListener { refresh() }
        try {
            var last = System.nanoTime()
            while (true) {
                delay(config.pollIntervalMillis.coerceAtLeast(16L))
                val now = System.nanoTime()
                tick((now - last) / 1_000_000L)
                last = now
            }
        } finally {
            subscription.close()
        }
    }

    internal fun tick(deltaMillis: Long) {
        refresh()
        controls = controls.reduce(ControlsEvent.Tick(deltaMillis))
        val commitDelay = config.scrubCommitDelayMillis
        if (scrub != null && scrubAutoCommit && commitDelay != null) {
            scrubIdleMillis += deltaMillis
            if (scrubIdleMillis >= commitDelay) commitScrub()
        }
    }

    internal fun refresh() {
        val next = engine.snapshot()
        if (next.isPlaying != snapshot.isPlaying) {
            controls = controls.reduce(ControlsEvent.PlaybackChanged(next.isPlaying))
        }
        snapshot = next
    }

    private fun scrubber(): Scrubber = Scrubber(
        durationMillis = snapshot.durationMillis,
        chapters = chapters,
        acceleration = config.acceleration,
        snapThresholdMillis = config.chapterSnapMillis,
    )

    private fun updateScrub(next: ScrubState, autoCommit: Boolean) {
        val started = scrub == null
        scrub = next
        scrubAutoCommit = autoCommit
        scrubIdleMillis = 0L
        if (started) controls = controls.reduce(ControlsEvent.ScrubbingChanged(true))
        onUserInteraction()
    }

    private fun endScrub() {
        scrub = null
        scrubIdleMillis = 0L
        controls = controls.reduce(ControlsEvent.ScrubbingChanged(false))
    }
}

/**
 * Creates a [TvPlayerState] for [engine] and keeps it in sync with the player while it is in
 * the composition.
 *
 * @param chapters chapters for the seek bar; they can change later.
 * @param skipWindows intro, recap and credits windows for the skip button.
 * @param config timing and seek behaviour.
 */
@Composable
public fun rememberTvPlayerState(
    engine: PlayerEngine,
    chapters: List<Chapter> = emptyList(),
    skipWindows: List<SkipWindow> = emptyList(),
    config: TvPlayerConfig = TvPlayerConfig(),
): TvPlayerState {
    val state = remember(engine) { TvPlayerState(engine, chapters, skipWindows, config) }
    SideEffect { state.update(chapters, skipWindows, config) }
    LaunchedEffect(state) { state.run() }
    return state
}

/**
 * Creates a [TvPlayerState] for a Media3 [player] (usually an `ExoPlayer`). You keep owning the
 * player: prepare it, and release it when the screen goes away.
 */
@Composable
public fun rememberTvPlayerState(
    player: Player,
    chapters: List<Chapter> = emptyList(),
    skipWindows: List<SkipWindow> = emptyList(),
    config: TvPlayerConfig = TvPlayerConfig(),
): TvPlayerState {
    val engine = remember(player) { Media3PlayerEngine(player) }
    return rememberTvPlayerState(engine, chapters, skipWindows, config)
}
