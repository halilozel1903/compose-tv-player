package io.github.halilozel1903.tvplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Icons drawn with paths, so the library needs no icon dependency. */
internal enum class PlayerIcon { Play, Pause, Rewind, Forward, Subtitles, Check, SkipNext }

@Composable
internal fun PlayerIconImage(icon: PlayerIcon, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        when (icon) {
            PlayerIcon.Play -> play(tint)
            PlayerIcon.Pause -> pause(tint)
            PlayerIcon.Rewind -> circularArrow(tint, forward = false)
            PlayerIcon.Forward -> circularArrow(tint, forward = true)
            PlayerIcon.Subtitles -> subtitles(tint)
            PlayerIcon.Check -> check(tint)
            PlayerIcon.SkipNext -> skipNext(tint)
        }
    }
}

private fun DrawScope.play(tint: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.28f, h * 0.16f)
        lineTo(w * 0.86f, h * 0.5f)
        lineTo(w * 0.28f, h * 0.84f)
        close()
    }
    drawPath(path, tint)
    drawPath(path, tint, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))
}

private fun DrawScope.pause(tint: Color) {
    val w = size.width
    val h = size.height
    val barWidth = w * 0.22f
    val radius = CornerRadius(barWidth * 0.3f)
    drawRoundRect(tint, topLeft = Offset(w * 0.22f, h * 0.16f), size = Size(barWidth, h * 0.68f), cornerRadius = radius)
    drawRoundRect(tint, topLeft = Offset(w * 0.56f, h * 0.16f), size = Size(barWidth, h * 0.68f), cornerRadius = radius)
}

private fun DrawScope.circularArrow(tint: Color, forward: Boolean) {
    val w = size.width
    val stroke = w * 0.1f
    val inset = w * 0.16f
    val diameter = w - inset * 2
    // An open circle with a gap at the top (from 11 to 1 o'clock) ...
    drawArc(
        color = tint,
        startAngle = -60f,
        sweepAngle = 300f,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = Size(diameter, diameter),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    // ... and an arrow head at one end of the gap, pointing in the direction of travel.
    val baseX = if (forward) w * 0.36f else w * 0.64f
    val tipX = if (forward) w * 0.58f else w * 0.42f
    val head = Path().apply {
        moveTo(baseX, w * 0.06f)
        lineTo(tipX, w * 0.21f)
        lineTo(baseX, w * 0.36f)
        close()
    }
    drawPath(head, tint)
}

private fun DrawScope.subtitles(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.08f
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.08f, h * 0.18f),
        size = Size(w * 0.84f, h * 0.64f),
        cornerRadius = CornerRadius(w * 0.1f),
        style = Stroke(width = stroke),
    )
    drawLine(tint, Offset(w * 0.24f, h * 0.48f), Offset(w * 0.4f, h * 0.48f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.5f, h * 0.48f), Offset(w * 0.76f, h * 0.48f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.24f, h * 0.64f), Offset(w * 0.6f, h * 0.64f), strokeWidth = stroke, cap = StrokeCap.Round)
}

private fun DrawScope.check(tint: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.18f, h * 0.52f)
        lineTo(w * 0.42f, h * 0.74f)
        lineTo(w * 0.84f, h * 0.28f)
    }
    drawPath(path, tint, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.skipNext(tint: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.18f, h * 0.2f)
        lineTo(w * 0.66f, h * 0.5f)
        lineTo(w * 0.18f, h * 0.8f)
        close()
    }
    drawPath(path, tint)
    drawRoundRect(tint, topLeft = Offset(w * 0.68f, h * 0.2f), size = Size(w * 0.14f, h * 0.6f), cornerRadius = CornerRadius(w * 0.04f))
}
