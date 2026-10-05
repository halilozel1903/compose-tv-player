package io.github.halilozel1903.tvplayer.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.darkColorScheme

/**
 * D-pad presses can't be timed reliably through adb on a fresh emulator, so
 * `scripts/screenshots.sh` starts the app with `--es scene <scene>` to set up each screenshot.
 * Scenes play a [io.github.halilozel1903.tvplayer.FakePlayerEngine] over generated frames and
 * never touch the network:
 *
 * - `playing`: the controls over a playing episode, play/pause focused
 * - `scrubbing`: the seek bar focused with the preview bubble on "Storm warning"
 * - `skipintro`: the opening titles with the "Skip intro" button focused
 * - `tracks`: the subtitles and audio panel open
 *
 * Without the extra the app streams a public sample video with ExoPlayer.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        setContent {
            MaterialTheme(colorScheme = SampleColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    colors = SurfaceDefaults.colors(containerColor = Color.Black),
                ) {
                    SampleApp(scene)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}

private val SampleColors = darkColorScheme(
    primary = Color(0xFFFFB86B),
    onPrimary = Color(0xFF3A1D00),
    secondary = Color(0xFF9ECBFF),
    background = Color(0xFF07090D),
    onBackground = Color(0xFFF1F3F8),
    surface = Color(0xFF0B0E14),
    onSurface = Color(0xFFF1F3F8),
    surfaceVariant = Color(0xFF1C2230),
    onSurfaceVariant = Color(0xFFA7B0C0),
)
