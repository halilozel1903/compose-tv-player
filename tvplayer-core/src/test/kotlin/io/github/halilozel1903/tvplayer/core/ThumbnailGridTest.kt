package io.github.halilozel1903.tvplayer.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThumbnailGridTest {

    private val grid = ThumbnailGrid(columns = 5, rows = 4, intervalMillis = 10_000, tileCount = 18)

    @Test
    fun tilesGoLeftToRightThenDown() {
        assertEquals(ThumbnailGrid.Tile(0, 0, 0), grid.tileAt(0))
        assertEquals(ThumbnailGrid.Tile(0, 0, 0), grid.tileAt(9_999))
        assertEquals(ThumbnailGrid.Tile(1, 1, 0), grid.tileAt(10_000))
        assertEquals(ThumbnailGrid.Tile(7, 2, 1), grid.tileAt(75_000))
    }

    @Test
    fun clampsToTheSheet() {
        assertEquals(ThumbnailGrid.Tile(17, 2, 3), grid.tileAt(10_000_000))
        assertEquals(ThumbnailGrid.Tile(0, 0, 0), grid.tileAt(-5))
    }

    @Test
    fun rejectsInvalidGrids() {
        assertFailsWith<IllegalArgumentException> { ThumbnailGrid(0, 1, 1_000) }
        assertFailsWith<IllegalArgumentException> { ThumbnailGrid(2, 2, 1_000, tileCount = 5) }
        assertFailsWith<IllegalArgumentException> { ThumbnailGrid(2, 2, 0) }
    }
}
