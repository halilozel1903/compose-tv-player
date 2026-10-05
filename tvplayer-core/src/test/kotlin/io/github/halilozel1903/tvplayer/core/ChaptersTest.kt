package io.github.halilozel1903.tvplayer.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChaptersTest {

    private val chapters = Chapters(
        listOf(
            Chapter("Storm warning", 600_000),
            Chapter("Previously", 0),
            Chapter("Opening titles", 60_000),
            Chapter("Duplicate start", 60_000),
            Chapter("Credits", 1_200_000),
        ),
    )

    @Test
    fun sortsAndDropsDuplicatedStarts() {
        assertEquals(listOf("Previously", "Opening titles", "Storm warning", "Credits"), chapters.list.map { it.title })
        assertEquals(4, chapters.size)
    }

    @Test
    fun chapterAtPosition() {
        assertEquals("Previously", chapters.chapterAt(0)?.title)
        assertEquals("Opening titles", chapters.chapterAt(599_999)?.title)
        assertEquals("Storm warning", chapters.chapterAt(600_000)?.title)
        assertEquals("Credits", chapters.chapterAt(5_000_000)?.title)
        assertNull(Chapters(listOf(Chapter("Late", 10_000))).chapterAt(5_000))
        assertEquals(-1, Chapters.Empty.indexAt(10))
    }

    @Test
    fun nextAndPrevious() {
        assertEquals("Storm warning", chapters.next(60_000)?.title)
        assertNull(chapters.next(1_200_000))
        // Well into a chapter: back to its start.
        assertEquals("Storm warning", chapters.previous(700_000)?.title)
        // Right after a chapter start: the chapter before.
        assertEquals("Opening titles", chapters.previous(601_000)?.title)
        // The first chapter has nothing before it.
        assertEquals("Previously", chapters.previous(1_000)?.title)
    }

    @Test
    fun markerFractionsSkipStartAndEnd() {
        val markers = chapters.markerFractions(1_200_000)
        assertEquals(listOf(0.05f, 0.5f), markers)
        assertTrue(chapters.markerFractions(-1).isEmpty())
    }

    @Test
    fun endOfChapter() {
        assertEquals(600_000L, chapters.endOf(1, 1_500_000))
        assertEquals(1_500_000L, chapters.endOf(3, 1_500_000))
    }

    @Test
    fun rejectsNegativeStarts() {
        assertFailsWith<IllegalArgumentException> { Chapter("Bad", -1) }
    }
}
