package io.github.halilozel1903.tvplayer

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import io.github.halilozel1903.tvplayer.core.PlaybackSnapshot
import io.github.halilozel1903.tvplayer.core.TrackKind
import io.github.halilozel1903.tvplayer.core.TrackOption

/**
 * The small surface of a player that [TvPlayerState] needs. [Media3PlayerEngine] wraps a Media3
 * [Player]; [FakePlayerEngine] plays nothing and drives previews, tests and screenshots.
 */
public interface PlayerEngine {

    /** The Media3 player that renders video, or `null` for engines without video. */
    public val player: Player?

    /** What the player reports right now. Called often, so keep it cheap. */
    public fun snapshot(): PlaybackSnapshot

    public fun play()

    public fun pause()

    public fun seekTo(positionMillis: Long)

    /** Plays [track] instead of the current track of its kind (and turns subtitles on). */
    public fun selectTrack(track: TrackOption)

    /** Turns subtitles off. */
    public fun disableSubtitles()

    /**
     * Calls [onChange] whenever the player state changes (playing, seeking, tracks, ...).
     * Closing the returned handle removes the listener. The position is polled separately.
     */
    public fun addListener(onChange: () -> Unit): AutoCloseable
}

/**
 * A [PlayerEngine] over a Media3 [Player], usually an `ExoPlayer`. The engine does not own the
 * player: create, prepare and release it yourself.
 *
 * Track ids have the form `"<group index>:<track index>"` of [Player.getCurrentTracks].
 */
public class Media3PlayerEngine(override val player: Player) : PlayerEngine {

    private var cachedTracks: Tracks? = null
    private var cachedOptions: List<TrackOption> = emptyList()

    override fun snapshot(): PlaybackSnapshot {
        val state = player.playbackState
        val duration = player.duration
        return PlaybackSnapshot(
            positionMillis = player.currentPosition.coerceAtLeast(0L),
            durationMillis = if (duration == C.TIME_UNSET) -1L else duration,
            bufferedPositionMillis = player.bufferedPosition.coerceAtLeast(0L),
            isPlaying = player.isPlaying || (player.playWhenReady && state == Player.STATE_BUFFERING),
            isBuffering = state == Player.STATE_BUFFERING,
            isEnded = state == Player.STATE_ENDED,
            tracks = trackOptions(),
        )
    }

    override fun play() {
        when (player.playbackState) {
            Player.STATE_IDLE -> player.prepare()
            Player.STATE_ENDED -> player.seekToDefaultPosition()
            else -> Unit
        }
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekTo(positionMillis: Long) {
        player.seekTo(positionMillis.coerceAtLeast(0L))
    }

    override fun selectTrack(track: TrackOption) {
        if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return
        val parts = track.id.split(':')
        val groupIndex = parts.getOrNull(0)?.toIntOrNull() ?: return
        val trackIndex = parts.getOrNull(1)?.toIntOrNull() ?: return
        val group = player.currentTracks.groups.getOrNull(groupIndex) ?: return
        if (trackIndex !in 0 until group.length) return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(group.type, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
            .build()
    }

    override fun disableSubtitles() {
        if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    override fun addListener(onChange: () -> Unit): AutoCloseable {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                onChange()
            }
        }
        player.addListener(listener)
        return AutoCloseable { player.removeListener(listener) }
    }

    private fun trackOptions(): List<TrackOption> {
        if (!player.isCommandAvailable(Player.COMMAND_GET_TRACKS)) return emptyList()
        val tracks = player.currentTracks
        if (tracks === cachedTracks) return cachedOptions
        val options = buildList<TrackOption> {
            tracks.groups.forEachIndexed { groupIndex, group ->
                val kind = when (group.type) {
                    C.TRACK_TYPE_AUDIO -> TrackKind.Audio
                    C.TRACK_TYPE_TEXT -> TrackKind.Subtitle
                    else -> return@forEachIndexed
                }
                for (trackIndex in 0 until group.length) {
                    if (!group.isTrackSupported(trackIndex)) continue
                    val format = group.getTrackFormat(trackIndex)
                    val mime = format.sampleMimeType.orEmpty()
                    add(
                        TrackOption(
                            id = "$groupIndex:$trackIndex",
                            kind = kind,
                            language = format.language,
                            label = format.label,
                            channelCount = format.channelCount.coerceAtLeast(0),
                            isForced = format.selectionFlags and C.SELECTION_FLAG_FORCED != 0,
                            isClosedCaption = format.roleFlags and C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND != 0 ||
                                mime == "application/cea-608" || mime == "application/cea-708",
                            isSelected = group.isTrackSelected(trackIndex),
                        ),
                    )
                }
            }
        }
        cachedTracks = tracks
        cachedOptions = options
        return options
    }
}

/**
 * A [PlayerEngine] without video: a clock that moves while "playing", plus the tracks you give it.
 * Use it for `@Preview`s, UI tests and screenshots that must not depend on the network.
 *
 * @param durationMillis the length of the pretend media.
 * @param positionMillis the start position.
 * @param playing start in the playing state.
 * @param tracks audio and subtitle tracks; selecting one updates [TrackOption.isSelected].
 * @param bufferAheadMillis how far ahead of the playhead the buffer pretends to be.
 * @param advancesWithClock `false` freezes the playhead even while playing (stable screenshots).
 * @param clock milliseconds of a monotonic clock.
 */
public class FakePlayerEngine(
    private val durationMillis: Long,
    positionMillis: Long = 0L,
    playing: Boolean = true,
    tracks: List<TrackOption> = emptyList(),
    private val bufferAheadMillis: Long = 90_000L,
    private val advancesWithClock: Boolean = true,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L },
) : PlayerEngine {

    override val player: Player? get() = null

    private var basePosition = positionMillis.coerceIn(0L, durationMillis.coerceAtLeast(0L))
    private var anchor = clock()
    private var playing = playing
    private var tracks = tracks
    private val listeners = mutableListOf<() -> Unit>()

    /** The current pretend position. */
    public val positionMillis: Long
        get() {
            if (!playing || !advancesWithClock) return basePosition
            return (basePosition + (clock() - anchor)).coerceAtMost(durationMillis)
        }

    override fun snapshot(): PlaybackSnapshot {
        val position = positionMillis
        val ended = durationMillis in 1..position
        return PlaybackSnapshot(
            positionMillis = position,
            durationMillis = durationMillis,
            bufferedPositionMillis = (position + bufferAheadMillis).coerceAtMost(durationMillis),
            isPlaying = playing && !ended,
            isBuffering = false,
            isEnded = ended,
            tracks = tracks,
        )
    }

    override fun play() {
        if (playing) return
        if (positionMillis >= durationMillis) basePosition = 0L
        anchor = clock()
        playing = true
        notifyListeners()
    }

    override fun pause() {
        if (!playing) return
        basePosition = positionMillis
        playing = false
        notifyListeners()
    }

    override fun seekTo(positionMillis: Long) {
        basePosition = positionMillis.coerceIn(0L, durationMillis.coerceAtLeast(0L))
        anchor = clock()
        notifyListeners()
    }

    override fun selectTrack(track: TrackOption) {
        tracks = tracks.map { if (it.kind == track.kind) it.copy(isSelected = it.id == track.id) else it }
        notifyListeners()
    }

    override fun disableSubtitles() {
        tracks = tracks.map { if (it.kind == TrackKind.Subtitle) it.copy(isSelected = false) else it }
        notifyListeners()
    }

    override fun addListener(onChange: () -> Unit): AutoCloseable {
        listeners += onChange
        return AutoCloseable { listeners -= onChange }
    }

    private fun notifyListeners() {
        listeners.toList().forEach { it() }
    }
}
