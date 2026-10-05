package io.github.halilozel1903.tvplayer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.tvplayer.core.SeekDirection
import io.github.halilozel1903.tvplayer.core.TimeFormat

/**
 * A focusable seek bar for the D-pad.
 *
 * - `Left` / `Right` move a seek preview ([TvPlayerState.scrub]); holding the key accelerates and
 *   the preview stops on chapter starts it comes close to.
 * - Center commits the preview, or toggles play and pause when there is none.
 * - Chapters split the bar into segments, the buffered part is lighter, and while scrubbing a
 *   small mark stays at the playhead.
 *
 * @param interactionSource observe focus from outside.
 */
@Composable
public fun TvSeekBar(
    state: TvPlayerState,
    modifier: Modifier = Modifier,
    colors: TvPlayerColors = TvPlayerDefaults.colors(),
    labels: TvPlayerLabels = TvPlayerLabels(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val barHeight by animateDpAsState(if (focused) 8.dp else 5.dp, label = "barHeight")
    val thumbRadius by animateDpAsState(if (focused) 9.dp else 0.dp, label = "thumbRadius")
    val snapshot = state.snapshot
    val fraction = state.displayFraction
    val markers = state.chapters.markerFractions(snapshot.durationMillis)
    val playheadFraction = snapshot.fraction
    val scrubbing = state.isScrubbing
    val display = state.displayPositionMillis

    Box(
        modifier
            .height(28.dp)
            .semantics {
                contentDescription = labels.seekBar
                stateDescription = TimeFormat.spoken(display, labels.time)
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            }
            .onPreviewKeyEvent { event ->
                when (event.key) {
                    Key.DirectionLeft, Key.DirectionRight -> {
                        if (event.type == KeyEventType.KeyDown) {
                            val direction = if (event.key == Key.DirectionRight) SeekDirection.Forward else SeekDirection.Backward
                            state.scrub(direction, event.nativeKeyEvent.repeatCount)
                        }
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (event.type == KeyEventType.KeyUp) {
                            if (state.isScrubbing) state.commitScrub() else state.togglePlayPause()
                        }
                        true
                    }
                    else -> false
                }
            }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val height = barHeight.toPx()
            val top = (size.height - height) / 2f
            val gap = 3.dp.toPx()
            val bounds = buildList<Float> {
                add(0f)
                addAll(markers)
                add(1f)
            }
            for (i in 0 until bounds.lastIndex) {
                val start = bounds[i]
                val end = bounds[i + 1]
                val from = start * size.width + if (i > 0) gap / 2f else 0f
                val to = end * size.width - if (i < bounds.lastIndex - 1) gap / 2f else 0f
                if (to <= from) continue
                segment(from, to, from, to, top, height, colors.track)
                segment(from, to, from, snapshot.bufferedFraction * size.width, top, height, colors.buffered)
                segment(from, to, from, fraction * size.width, top, height, colors.accent)
            }
            if (scrubbing) {
                val x = playheadFraction * size.width
                drawRoundRect(
                    color = colors.content,
                    topLeft = Offset(x - 1.5.dp.toPx(), top - 4.dp.toPx()),
                    size = Size(3.dp.toPx(), height + 8.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx()),
                )
            }
            val radius = thumbRadius.toPx()
            if (radius > 0f) {
                val center = Offset(fraction * size.width, size.height / 2f)
                drawCircle(Color.Black.copy(alpha = 0.35f), radius = radius + 3.dp.toPx(), center = center)
                drawCircle(colors.content, radius = radius, center = center)
                drawCircle(colors.accent, radius = radius * 0.55f, center = center)
            }
        }
    }
}

/** Draws the part of the segment [segmentStart]..[segmentEnd] that lies in [from]..[to]. */
private fun DrawScope.segment(
    segmentStart: Float,
    segmentEnd: Float,
    from: Float,
    to: Float,
    top: Float,
    height: Float,
    color: Color,
) {
    val left = maxOf(segmentStart, from)
    val right = minOf(segmentEnd, to)
    if (right <= left) return
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(right - left, height),
        cornerRadius = CornerRadius(height / 2f),
    )
}

/**
 * The bubble above the seek bar while scrubbing: a thumbnail, the previewed time and the chapter.
 *
 * @param thumbnails draws the picture; `null` shows only the time and chapter.
 */
@Composable
public fun SeekPreviewBubble(
    positionMillis: Long,
    durationMillis: Long,
    modifier: Modifier = Modifier,
    chapterTitle: String? = null,
    thumbnails: SeekThumbnailProvider? = null,
    colors: TvPlayerColors = TvPlayerDefaults.colors(),
    width: Dp = TvPlayerDefaults.ThumbnailWidth,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .width(width)
            .clip(shape)
            .background(colors.panel)
            .border(2.dp, colors.content.copy(alpha = 0.9f), shape),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (thumbnails != null) {
            thumbnails.Thumbnail(
                positionMillis = positionMillis,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            )
        }
        Text(
            text = TimeFormat.clock(positionMillis, durationMillis),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.content,
            maxLines = 1,
        )
        Text(
            text = chapterTitle.orEmpty(),
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = colors.contentDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Places [SeekPreviewBubble] above the point of the seek bar it previews, kept inside the row.
 * Put it right above a full width [TvSeekBar].
 */
@Composable
internal fun SeekPreviewRow(
    state: TvPlayerState,
    thumbnails: SeekThumbnailProvider?,
    colors: TvPlayerColors,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
        val scrub = state.scrub ?: return@BoxWithConstraints
        val bubbleWidth = TvPlayerDefaults.ThumbnailWidth
        val center = maxWidth * state.displayFraction
        val x = (center - bubbleWidth / 2).coerceIn(0.dp, (maxWidth - bubbleWidth).coerceAtLeast(0.dp))
        SeekPreviewBubble(
            positionMillis = scrub.previewMillis,
            durationMillis = state.durationMillis,
            modifier = Modifier.offset(x = x),
            chapterTitle = scrub.chapter?.title,
            thumbnails = thumbnails,
            colors = colors,
            width = bubbleWidth,
        )
    }
}
