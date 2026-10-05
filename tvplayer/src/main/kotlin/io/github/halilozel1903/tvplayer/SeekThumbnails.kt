package io.github.halilozel1903.tvplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.halilozel1903.tvplayer.core.ThumbnailGrid
import kotlin.math.roundToInt

/**
 * Draws the picture in the seek preview bubble for a position. Implement it to show frames from
 * your trick play storyboard, an image loader, or anything else.
 */
@Stable
public interface SeekThumbnailProvider {

    /** Draws the thumbnail for [positionMillis], filling [modifier]'s bounds (16:9). */
    @Composable
    public fun Thumbnail(positionMillis: Long, modifier: Modifier)
}

/**
 * A [SeekThumbnailProvider] from a composable lambda:
 *
 * ```
 * val thumbnails = SeekThumbnailProvider { position -> AsyncImage(storyboardUrl(position), null) }
 * ```
 */
public fun SeekThumbnailProvider(content: @Composable BoxScope.(positionMillis: Long) -> Unit): SeekThumbnailProvider =
    object : SeekThumbnailProvider {
        @Composable
        override fun Thumbnail(positionMillis: Long, modifier: Modifier) {
            Box(modifier) { content(positionMillis) }
        }
    }

/**
 * Thumbnails cut from one sprite sheet (the storyboard most streaming backends generate for trick
 * play): [ThumbnailGrid.columns] x [ThumbnailGrid.rows] equally sized tiles, one every
 * [ThumbnailGrid.intervalMillis].
 *
 * @param sheet the decoded sprite sheet.
 * @param grid the layout of the tiles in [sheet].
 */
public class SpriteSheetThumbnailProvider(
    private val sheet: ImageBitmap,
    private val grid: ThumbnailGrid,
) : SeekThumbnailProvider {

    @Composable
    override fun Thumbnail(positionMillis: Long, modifier: Modifier) {
        val tile = grid.tileAt(positionMillis)
        val tileWidth = sheet.width / grid.columns
        val tileHeight = sheet.height / grid.rows
        Canvas(modifier) {
            drawImage(
                image = sheet,
                srcOffset = IntOffset(tile.column * tileWidth, tile.row * tileHeight),
                srcSize = IntSize(tileWidth, tileHeight),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}
