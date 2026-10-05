package io.github.halilozel1903.tvplayer.sample

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.halilozel1903.tvplayer.core.Chapters

/**
 * The "video" of the screenshot scenes: a coast with a lighthouse, drawn with gradients and simple
 * shapes. Each chapter has its own light (dawn, day, storm, dusk, night), so the seek preview
 * thumbnails change while scrubbing. No copyrighted artwork, no image files.
 */
@Composable
fun CinematicFrame(look: FrameLook, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawCoast(look) }
}

/** The light of one frame. */
class FrameLook(
    val skyTop: Color,
    val skyHorizon: Color,
    val glow: Color,
    val sunY: Float,
    val sea: Color,
    val land: Color,
    val beam: Float,
    val rain: Boolean = false,
)

object FrameLooks {
    val Dawn = FrameLook(Color(0xFF1B2A4A), Color(0xFFF2A65A), Color(0xFFFFD39A), sunY = 0.5f, sea = Color(0xFF173B57), land = Color(0xFF0E1726), beam = 0.25f)
    val Day = FrameLook(Color(0xFF2E6FA8), Color(0xFFBFE0F0), Color(0xFFFFF4D6), sunY = 0.2f, sea = Color(0xFF1F5F86), land = Color(0xFF1C2E2A), beam = 0f)
    val Letter = FrameLook(Color(0xFF3B4C6B), Color(0xFFE7B98A), Color(0xFFFFE2B8), sunY = 0.32f, sea = Color(0xFF2A4A66), land = Color(0xFF16202C), beam = 0.1f)
    val Storm = FrameLook(Color(0xFF151B24), Color(0xFF4B5A6B), Color(0xFFB8C7D9), sunY = 0.18f, sea = Color(0xFF1B2733), land = Color(0xFF0A0E13), beam = 0.6f, rain = true)
    val Dusk = FrameLook(Color(0xFF2B1B45), Color(0xFFE2725B), Color(0xFFFFB27A), sunY = 0.48f, sea = Color(0xFF2B2447), land = Color(0xFF120E1E), beam = 0.45f)
    val Night = FrameLook(Color(0xFF060B18), Color(0xFF1D2B4A), Color(0xFFE8EEFF), sunY = 0.22f, sea = Color(0xFF0B1426), land = Color(0xFF03060C), beam = 0.9f)

    private val byChapter = listOf(Dusk, Dusk, Dawn, Letter, Storm, Night, Day, Night)

    /** The look of the chapter at [positionMillis]. */
    fun at(chapters: Chapters, positionMillis: Long): FrameLook =
        byChapter[chapters.indexAt(positionMillis).coerceIn(0, byChapter.lastIndex)]
}

private fun DrawScope.drawCoast(look: FrameLook) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.62f

    // Sky and the sun or moon with its glow.
    drawRect(Brush.verticalGradient(listOf(look.skyTop, look.skyHorizon), startY = 0f, endY = horizon))
    val sun = Offset(w * 0.34f, h * look.sunY)
    drawCircle(
        brush = Brush.radialGradient(listOf(look.glow.copy(alpha = 0.55f), look.glow.copy(alpha = 0f)), center = sun, radius = h * 0.45f),
        radius = h * 0.45f,
        center = sun,
    )
    drawCircle(look.glow, radius = h * 0.06f, center = sun)

    // Clouds: soft horizontal bands.
    for (i in 0 until 4) {
        val y = h * (0.12f + i * 0.09f)
        drawOval(
            color = Color.White.copy(alpha = if (look.rain) 0.10f else 0.06f),
            topLeft = Offset(w * (0.05f + i * 0.21f), y),
            size = Size(w * 0.42f, h * 0.035f),
        )
    }

    // Far hills.
    val far = Path().apply {
        moveTo(0f, horizon)
        cubicTo(w * 0.15f, horizon - h * 0.09f, w * 0.3f, horizon - h * 0.02f, w * 0.45f, horizon - h * 0.06f)
        cubicTo(w * 0.55f, horizon - h * 0.09f, w * 0.62f, horizon - h * 0.03f, w * 0.7f, horizon)
        close()
    }
    drawPath(far, look.land.copy(alpha = 0.55f))

    // Sea with the sun's reflection.
    drawRect(
        Brush.verticalGradient(listOf(look.sea, look.sea.copy(alpha = 1f).darken(0.55f)), startY = horizon, endY = h),
        topLeft = Offset(0f, horizon),
        size = Size(w, h - horizon),
    )
    for (i in 0 until 14) {
        val y = horizon + (h - horizon) * (i + 1) / 15f
        val spread = w * (0.04f + i * 0.012f)
        drawRect(
            color = look.glow.copy(alpha = 0.32f - i * 0.018f),
            topLeft = Offset(sun.x - spread / 2f + (i % 3 - 1) * w * 0.01f, y),
            size = Size(spread, h * 0.006f),
        )
    }

    // Cliff on the right with the lighthouse.
    val cliff = Path().apply {
        moveTo(w * 0.62f, h)
        cubicTo(w * 0.66f, h * 0.74f, w * 0.7f, h * 0.6f, w * 0.78f, h * 0.55f)
        lineTo(w, h * 0.5f)
        lineTo(w, h)
        close()
    }
    drawPath(cliff, look.land)
    val towerBase = Offset(w * 0.84f, h * 0.535f)
    val tower = Path().apply {
        moveTo(towerBase.x - w * 0.018f, towerBase.y)
        lineTo(towerBase.x - w * 0.011f, h * 0.3f)
        lineTo(towerBase.x + w * 0.011f, h * 0.3f)
        lineTo(towerBase.x + w * 0.018f, towerBase.y)
        close()
    }
    drawPath(tower, look.land.lighten(0.12f))
    for (stripe in 0 until 3) {
        val y = h * (0.33f + stripe * 0.065f)
        drawRect(Color(0xFFB8463F).copy(alpha = 0.75f), topLeft = Offset(towerBase.x - w * 0.016f, y), size = Size(w * 0.032f, h * 0.025f))
    }
    val lamp = Offset(towerBase.x, h * 0.28f)
    drawRect(look.land, topLeft = Offset(lamp.x - w * 0.016f, h * 0.295f), size = Size(w * 0.032f, h * 0.012f))
    drawCircle(Color(0xFFFFE7A8), radius = h * 0.022f, center = lamp)
    if (look.beam > 0f) {
        val beam = Path().apply {
            moveTo(lamp.x, lamp.y)
            lineTo(w * 0.18f, lamp.y - h * 0.16f)
            lineTo(w * 0.16f, lamp.y + h * 0.1f)
            close()
        }
        drawPath(
            beam,
            Brush.horizontalGradient(
                listOf(Color(0x00FFE7A8), Color(0xFFFFE7A8).copy(alpha = 0.35f * look.beam)),
                startX = w * 0.16f,
                endX = lamp.x,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFFE7A8).copy(alpha = 0.6f * look.beam), Color.Transparent), center = lamp, radius = h * 0.12f),
            radius = h * 0.12f,
            center = lamp,
        )
    }

    // A small boat with a light.
    val boat = Offset(w * 0.5f, horizon + h * 0.07f)
    val hull = Path().apply {
        moveTo(boat.x - w * 0.03f, boat.y)
        lineTo(boat.x + w * 0.03f, boat.y)
        lineTo(boat.x + w * 0.02f, boat.y + h * 0.018f)
        lineTo(boat.x - w * 0.022f, boat.y + h * 0.018f)
        close()
    }
    drawPath(hull, look.land)
    drawRect(look.land, topLeft = Offset(boat.x - w * 0.002f, boat.y - h * 0.06f), size = Size(w * 0.004f, h * 0.06f))
    drawCircle(Color(0xFFFFD27A), radius = h * 0.006f, center = Offset(boat.x + w * 0.012f, boat.y - h * 0.01f))

    // Rain for the storm.
    if (look.rain) {
        for (i in 0 until 90) {
            val x = (i * 97 % 1000) / 1000f * w
            val y = (i * 61 % 1000) / 1000f * h
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(x, y),
                end = Offset(x - w * 0.008f, y + h * 0.05f),
                strokeWidth = 1.5f,
            )
        }
    }

    // Vignette, like a graded film frame.
    drawRect(
        Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
            center = Offset(w / 2f, h / 2f),
            radius = maxOf(w, h) * 0.75f,
        ),
    )
}

private fun Color.darken(factor: Float): Color = Color(red * factor, green * factor, blue * factor, alpha)

private fun Color.lighten(amount: Float): Color =
    Color((red + amount).coerceAtMost(1f), (green + amount).coerceAtMost(1f), (blue + amount).coerceAtMost(1f), alpha)
