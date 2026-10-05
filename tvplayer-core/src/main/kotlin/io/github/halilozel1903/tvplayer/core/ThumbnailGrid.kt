package io.github.halilozel1903.tvplayer.core

/**
 * Finds the tile for a position in a trick play sprite sheet (a storyboard): one image with
 * [columns] x [rows] thumbnails, one every [intervalMillis], left to right and top to bottom.
 *
 * @param tileCount thumbnails actually in the sheet; the last row may be partly empty.
 */
public data class ThumbnailGrid(
    val columns: Int,
    val rows: Int,
    val intervalMillis: Long,
    val tileCount: Int = columns * rows,
) {
    init {
        require(columns > 0 && rows > 0) { "columns and rows must be positive" }
        require(intervalMillis > 0L) { "intervalMillis must be positive" }
        require(tileCount in 1..columns * rows) { "tileCount must be in 1..${columns * rows}, was $tileCount" }
    }

    /** A tile position in the sheet. */
    public data class Tile(val index: Int, val column: Int, val row: Int)

    /** The tile that shows [positionMillis]; positions past the last tile use the last tile. */
    public fun tileAt(positionMillis: Long): Tile {
        val index = (positionMillis.coerceAtLeast(0L) / intervalMillis).coerceAtMost(tileCount - 1L).toInt()
        return Tile(index = index, column = index % columns, row = index / columns)
    }
}
