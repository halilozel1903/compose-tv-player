package io.github.halilozel1903.tvplayer

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import io.github.halilozel1903.tvplayer.core.SkipKind
import io.github.halilozel1903.tvplayer.core.SkipWindow
import io.github.halilozel1903.tvplayer.core.TimeFormat
import io.github.halilozel1903.tvplayer.core.TrackMenu
import java.util.Locale

/**
 * Colors of the player controls. Get defaults that follow the theme from [TvPlayerDefaults.colors].
 *
 * @param accent the played part of the seek bar and the thumb.
 * @param track the unplayed part of the seek bar.
 * @param buffered the buffered part of the seek bar.
 * @param content text and icons over the video.
 * @param contentDim secondary text such as the remaining time.
 * @param scrim the color of the gradients behind the controls.
 * @param panel the background of the subtitle and audio panel and the preview bubble.
 * @param focusedContainer the background of a focused button.
 * @param onFocusedContainer text and icons of a focused button.
 */
@Immutable
public data class TvPlayerColors(
    val accent: Color,
    val track: Color,
    val buffered: Color,
    val content: Color,
    val contentDim: Color,
    val scrim: Color,
    val panel: Color,
    val focusedContainer: Color,
    val onFocusedContainer: Color,
)

/**
 * Every text the player shows, to translate it.
 *
 * @param displayLocale the language that track language names are written in.
 */
@Immutable
public data class TvPlayerLabels(
    val play: String = "Play",
    val pause: String = "Pause",
    val rewind: String = "Rewind",
    val forward: String = "Fast forward",
    val seekBar: String = "Seek bar",
    val subtitlesAndAudio: String = "Subtitles & audio",
    val subtitles: String = "Subtitles",
    val audio: String = "Audio",
    val skipRecap: String = "Skip recap",
    val skipIntro: String = "Skip intro",
    val skipCredits: String = "Skip credits",
    val trackMenu: TrackMenu.Labels = TrackMenu.Labels(),
    val time: TimeFormat.Labels = TimeFormat.Labels(),
    val displayLocale: Locale = Locale.getDefault(),
) {
    /** The button text for [window]: its own label, or the default for its kind. */
    public fun skipLabel(window: SkipWindow): String = window.label ?: when (window.kind) {
        SkipKind.Recap -> skipRecap
        SkipKind.Intro -> skipIntro
        SkipKind.Credits -> skipCredits
    }
}

/** Default sizes and colors of the TV player. */
public object TvPlayerDefaults {

    /** Horizontal title safe area of a TV screen (5% of 960 dp). */
    public val SafeAreaHorizontal: Dp = 48.dp

    /** Vertical title safe area of a TV screen (5% of 540 dp). */
    public val SafeAreaVertical: Dp = 27.dp

    /** Width of the seek preview thumbnail; the height is 16:9. */
    public val ThumbnailWidth: Dp = 224.dp

    /** Width of the subtitle and audio panel. */
    public val PanelWidth: Dp = 360.dp

    /** Colors that follow the tv-material theme, over a dark scrim. */
    @Composable
    public fun colors(
        accent: Color = MaterialTheme.colorScheme.primary,
        track: Color = Color.White.copy(alpha = 0.28f),
        buffered: Color = Color.White.copy(alpha = 0.55f),
        content: Color = Color.White,
        contentDim: Color = Color.White.copy(alpha = 0.72f),
        scrim: Color = Color.Black,
        panel: Color = Color(0xF2141820),
        focusedContainer: Color = Color.White,
        onFocusedContainer: Color = Color(0xFF101318),
    ): TvPlayerColors = TvPlayerColors(
        accent = accent,
        track = track,
        buffered = buffered,
        content = content,
        contentDim = contentDim,
        scrim = scrim,
        panel = panel,
        focusedContainer = focusedContainer,
        onFocusedContainer = onFocusedContainer,
    )
}

/**
 * Pads content into the TV title safe area: the window's safe drawing insets (display cutouts and
 * system bars, usually none on a TV) plus the overscan margins.
 *
 * It is `@Composable` because it reads [WindowInsets.Companion.safeDrawing].
 */
@Composable
public fun Modifier.tvSafeAreaPadding(
    horizontal: Dp = TvPlayerDefaults.SafeAreaHorizontal,
    vertical: Dp = TvPlayerDefaults.SafeAreaVertical,
): Modifier = this
    .windowInsetsPadding(WindowInsets.safeDrawing)
    .padding(horizontal = horizontal, vertical = vertical)
