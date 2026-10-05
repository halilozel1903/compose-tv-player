package io.github.halilozel1903.tvplayer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.tvplayer.core.SeekDirection
import io.github.halilozel1903.tvplayer.core.TimeFormat
import io.github.halilozel1903.tvplayer.core.TrackMenu

/**
 * The player overlay for a TV remote: a title on top; a chaptered seek bar with D-pad scrubbing
 * and a preview bubble, the times and a row of buttons at the bottom; a skip intro button that
 * takes the focus when its window starts; and the subtitle and audio side panel.
 *
 * Remote behaviour:
 * - Any key shows hidden controls (`Left` / `Right` also start a scrub). They hide again after
 *   [TvPlayerConfig.autoHideMillis] while playing.
 * - Back closes the panel, cancels a scrub, or hides the controls, in that order.
 * - Media keys (play/pause, rewind, fast forward) work whether the controls are up or not.
 *
 * @param state the player state from [rememberTvPlayerState].
 * @param title the big title, usually the episode or movie name.
 * @param subtitle a line above the title, such as `"Harbor Lights · S1 E4"`.
 * @param thumbnails draws the seek preview pictures; `null` shows only the time and chapter.
 */
@Composable
public fun TvPlayerControls(
    state: TvPlayerState,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    thumbnails: SeekThumbnailProvider? = null,
    labels: TvPlayerLabels = TvPlayerLabels(),
    colors: TvPlayerColors = TvPlayerDefaults.colors(),
) {
    val focus = remember { ControlsFocus() }
    val swallowedKeys = remember { mutableSetOf<Key>() }
    var skipFocused by remember { mutableStateOf(false) }
    val skipWindow = state.visibleSkipWindow
    val controlsVisible = state.controlsVisible

    BackHandler(enabled = state.canHandleBack) { state.handleBack() }

    // Where the focus goes when the controls appear or disappear.
    LaunchedEffect(controlsVisible) {
        val target = when {
            state.pendingFocus != null -> return@LaunchedEffect
            controlsVisible && skipWindow != null -> focus.skip
            controlsVisible -> focus.playPause
            skipWindow != null -> focus.skip
            else -> focus.root
        }
        target.requestFocusWhenReady()
    }
    // The skip button takes the focus when it appears and gives it back when it goes away.
    LaunchedEffect(skipWindow != null) {
        if (skipWindow != null) {
            focus.skip.requestFocusWhenReady()
        } else if (skipFocused) {
            (if (state.controlsVisible) focus.playPause else focus.root).requestFocusWhenReady()
        }
    }
    val pending = state.pendingFocus
    LaunchedEffect(pending) {
        if (pending == null) return@LaunchedEffect
        val requester = when (pending) {
            PlayerFocusTarget.PlayPause -> focus.playPause
            PlayerFocusTarget.SeekBar -> focus.seekBar
            PlayerFocusTarget.SkipButton -> focus.skip
            PlayerFocusTarget.TracksButton -> focus.tracks
        }
        requester.requestFocusWhenReady()
        state.consumePendingFocus(pending)
    }

    Box(
        modifier
            .fillMaxSize()
            .focusRequester(focus.root)
            .onPreviewKeyEvent { event ->
                val key = event.key
                if (event.type == KeyEventType.KeyUp) return@onPreviewKeyEvent swallowedKeys.remove(key)
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val repeat = event.nativeKeyEvent.repeatCount
                val handled = when (key) {
                    Key.MediaPlayPause -> {
                        state.togglePlayPause()
                        true
                    }
                    Key.MediaPlay -> {
                        state.play()
                        true
                    }
                    Key.MediaPause -> {
                        state.pause()
                        state.showControls()
                        true
                    }
                    Key.MediaFastForward, Key.MediaRewind -> {
                        state.requestFocus(PlayerFocusTarget.SeekBar)
                        state.scrub(if (key == Key.MediaFastForward) SeekDirection.Forward else SeekDirection.Backward, repeat)
                        true
                    }
                    Key.Back, Key.Escape -> false
                    else -> if (state.controlsVisible) {
                        state.onUserInteraction()
                        false
                    } else if (skipFocused && key in ConfirmKeys) {
                        false
                    } else {
                        when (key) {
                            Key.DirectionLeft, Key.DirectionRight -> {
                                state.requestFocus(PlayerFocusTarget.SeekBar)
                                state.scrub(if (key == Key.DirectionRight) SeekDirection.Forward else SeekDirection.Backward, repeat)
                            }
                            else -> state.showControls()
                        }
                        true
                    }
                }
                if (handled) swallowedKeys += key
                handled
            }
            .focusable(),
    ) {
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ControlsLayer(state, title, subtitle, thumbnails, labels, colors, focus)
        }

        val skipBottom by animateDpAsState(if (controlsVisible) 196.dp else 64.dp, label = "skipBottom")
        AnimatedVisibility(
            visible = skipWindow != null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = TvPlayerDefaults.SafeAreaHorizontal, bottom = skipBottom),
            enter = fadeIn() + slideInHorizontally { it / 2 },
            exit = fadeOut(),
        ) {
            // Keep the last window while the exit animation runs.
            val shown = remember { mutableStateOf(skipWindow) }
            if (skipWindow != null) shown.value = skipWindow
            val window = shown.value
            if (window != null) {
                SkipButton(
                    window = window,
                    onClick = { state.skip(window) },
                    modifier = Modifier
                        .focusRequester(focus.skip)
                        .onFocusChanged { skipFocused = it.isFocused },
                    labels = labels,
                    colors = colors,
                )
            }
        }

        AnimatedVisibility(
            visible = state.isTrackPanelOpen,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it },
        ) {
            TrackSelectionPanel(state = state, labels = labels, colors = colors)
        }
    }
}

@Composable
private fun ControlsLayer(
    state: TvPlayerState,
    title: String,
    subtitle: String?,
    thumbnails: SeekThumbnailProvider?,
    labels: TvPlayerLabels,
    colors: TvPlayerColors,
    focus: ControlsFocus,
) {
    val snapshot = state.snapshot
    val canFocus = !state.isTrackPanelOpen
    val menu = remember(labels) { TrackMenu(labels.trackMenu, labels.displayLocale) }
    val subtitleChoice = remember(menu, snapshot.tracks) { menu.selectedTitle(menu.subtitles(snapshot.tracks)) }
    val hasTracks = snapshot.tracks.isNotEmpty()

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(colors.scrim.copy(alpha = 0.7f), Color.Transparent))),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.scrim.copy(alpha = 0.88f)))),
        )

        Column(Modifier.align(Alignment.TopStart).tvSafeAreaPadding().padding(end = TvPlayerDefaults.PanelWidth)) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .tvSafeAreaPadding(),
        ) {
            SeekPreviewRow(state, thumbnails, colors, Modifier.padding(bottom = 10.dp))
            TvSeekBar(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus.seekBar)
                    .focusProperties { this.canFocus = canFocus },
                colors = colors,
                labels = labels,
            )
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = TimeFormat.clock(state.displayPositionMillis, snapshot.durationMillis),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.content,
                )
                Text(
                    text = state.currentChapter?.title.orEmpty(),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.contentDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = TimeFormat.remaining(state.displayPositionMillis, snapshot.durationMillis),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.contentDim,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRestorer(focus.playPause),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RoundControlButton(
                    icon = if (snapshot.isPlaying) PlayerIcon.Pause else PlayerIcon.Play,
                    contentDescription = if (snapshot.isPlaying) labels.pause else labels.play,
                    onClick = { state.togglePlayPause() },
                    colors = colors,
                    modifier = Modifier.focusRequester(focus.playPause),
                    canFocus = canFocus,
                )
                val stepSeconds = (state.config.seekStepMillis / 1_000L).toString()
                RoundControlButton(
                    icon = PlayerIcon.Rewind,
                    contentDescription = labels.rewind,
                    onClick = { state.seekBy(-state.config.seekStepMillis) },
                    colors = colors,
                    canFocus = canFocus,
                    badge = stepSeconds,
                )
                RoundControlButton(
                    icon = PlayerIcon.Forward,
                    contentDescription = labels.forward,
                    onClick = { state.seekBy(state.config.seekStepMillis) },
                    colors = colors,
                    canFocus = canFocus,
                    badge = stepSeconds,
                )
                Spacer(Modifier.weight(1f))
                if (hasTracks) {
                    PillControlButton(
                        icon = PlayerIcon.Subtitles,
                        text = labels.subtitlesAndAudio,
                        detail = subtitleChoice,
                        onClick = { state.openTrackPanel() },
                        colors = colors,
                        modifier = Modifier.focusRequester(focus.tracks),
                        canFocus = canFocus,
                    )
                }
            }
        }
    }
}

/** The focus requesters of the overlay. */
private class ControlsFocus {
    val root = FocusRequester()
    val playPause = FocusRequester()
    val seekBar = FocusRequester()
    val skip = FocusRequester()
    val tracks = FocusRequester()
}

private val ConfirmKeys = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/**
 * Requests focus once the target is composed and attached; a freshly shown control may need a
 * frame or two.
 */
internal suspend fun FocusRequester.requestFocusWhenReady() {
    repeat(10) {
        withFrameNanos { }
        if (runCatching { requestFocus() }.isSuccess) return
    }
}
