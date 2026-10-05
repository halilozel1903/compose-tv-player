package io.github.halilozel1903.tvplayer

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import io.github.halilozel1903.tvplayer.core.Chapter
import io.github.halilozel1903.tvplayer.core.TrackKind
import io.github.halilozel1903.tvplayer.core.TrackOption

/**
 * A full screen TV player: the video of [state]'s Media3 player with [TvPlayerControls] on top.
 *
 * When the engine has no video ([PlayerEngine.player] is `null`, as with [FakePlayerEngine]),
 * [placeholder] is drawn instead, which keeps previews and screenshots free of real media.
 *
 * ```
 * val player = remember { ExoPlayer.Builder(context).build() }
 * val state = rememberTvPlayerState(player, chapters = chapters, skipWindows = skipWindows)
 * TvPlayer(state, title = "The Lantern Coast", subtitle = "Harbor Lights · S1 E4")
 * ```
 *
 * @param placeholder drawn behind the controls when there is no video.
 */
@Composable
public fun TvPlayer(
    state: TvPlayerState,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    thumbnails: SeekThumbnailProvider? = null,
    labels: TvPlayerLabels = TvPlayerLabels(),
    colors: TvPlayerColors = TvPlayerDefaults.colors(),
    placeholder: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.background(Color.Black)) {
        val player = state.engine.player
        if (player != null) {
            TvVideoSurface(player, Modifier.fillMaxSize())
        } else {
            placeholder()
        }
        TvPlayerControls(
            state = state,
            title = title,
            modifier = Modifier.fillMaxSize(),
            subtitle = subtitle,
            thumbnails = thumbnails,
            labels = labels,
            colors = colors,
        )
    }
}

/**
 * The video (and subtitles) of a Media3 [player], rendered by a Media3 `PlayerView` without its
 * own controller. The view never takes D-pad focus, so the Compose controls keep it.
 */
@Composable
public fun TvVideoSurface(player: Player, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                isFocusable = false
                isFocusableInTouchMode = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                this.player = player
            }
        },
        modifier = modifier,
        update = { view -> if (view.player !== player) view.player = player },
        onRelease = { view -> view.player = null },
    )
}

@Preview(device = "id:tv_1080p")
@Composable
private fun TvPlayerPreview() {
    val engine = remember {
        FakePlayerEngine(
            durationMillis = 2_832_000L,
            positionMillis = 761_000L,
            tracks = listOf(TrackOption("s-en", TrackKind.Subtitle, language = "en", isSelected = true)),
            advancesWithClock = false,
        )
    }
    val state = rememberTvPlayerState(
        engine = engine,
        chapters = listOf(Chapter("Opening", 0L), Chapter("Storm warning", 1_102_000L), Chapter("Credits", 2_730_000L)),
        config = TvPlayerConfig(autoHideMillis = null),
    )
    MaterialTheme {
        TvPlayer(
            state = state,
            title = "The Lantern Coast",
            subtitle = "Harbor Lights · S1 E4",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
