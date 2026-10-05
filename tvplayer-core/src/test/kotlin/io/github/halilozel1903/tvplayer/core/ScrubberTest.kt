package io.github.halilozel1903.tvplayer.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrubberTest {

    private val hour = 3_600_000L

    @Test
    fun singlePressUsesTheBaseStep() {
        val acceleration = SeekAcceleration()
        assertEquals(10_000L, acceleration.stepFor(0, hour))
        assertEquals(0, acceleration.levelFor(0))
    }

    @Test
    fun holdingTheKeyAccelerates() {
        val acceleration = SeekAcceleration(repeatsPerLevel = 8)
        assertEquals(10_000L, acceleration.stepFor(7, hour))
        assertEquals(20_000L, acceleration.stepFor(8, hour))
        assertEquals(30_000L, acceleration.stepFor(16, hour))
        assertEquals(60_000L, acceleration.stepFor(24, hour))
        assertEquals(120_000L, acceleration.stepFor(32, hour))
        assertEquals(120_000L, acceleration.stepFor(500, hour))
        assertEquals(4, acceleration.levelFor(500))
    }

    @Test
    fun stepsAreCappedForShortMedia() {
        val acceleration = SeekAcceleration(maxFractionOfDuration = 0.05f)
        // 5% of 10 minutes is 30 seconds.
        assertEquals(30_000L, acceleration.stepFor(100, 600_000))
        // Never below the base step, even for a clip shorter than 200 seconds.
        assertEquals(10_000L, acceleration.stepFor(100, 60_000))
        // Unknown duration: no cap.
        assertEquals(120_000L, acceleration.stepFor(100, -1))
    }

    @Test
    fun rejectsInvalidAcceleration() {
        assertFailsWith<IllegalArgumentException> { SeekAcceleration(levels = emptyList()) }
        assertFailsWith<IllegalArgumentException> { SeekAcceleration(baseStepMillis = 0) }
    }

    @Test
    fun scrubMovesThePreviewNotThePlayhead() {
        val scrubber = Scrubber(durationMillis = hour)
        val start = scrubber.start(100_000)
        val moved = scrubber.step(start, SeekDirection.Forward)
        assertEquals(100_000L, moved.originMillis)
        assertEquals(110_000L, moved.previewMillis)
        assertEquals(10_000L, moved.offsetMillis)
        val back = scrubber.step(moved, SeekDirection.Backward)
        assertEquals(100_000L, back.previewMillis)
    }

    @Test
    fun scrubIsClampedToTheMedia() {
        val scrubber = Scrubber(durationMillis = 60_000)
        assertEquals(0L, scrubber.step(scrubber.start(4_000), SeekDirection.Backward).previewMillis)
        assertEquals(60_000L, scrubber.step(scrubber.start(55_000), SeekDirection.Forward).previewMillis)
        assertEquals(60_000L, scrubber.start(90_000).previewMillis)
    }

    @Test
    fun snapsToANearbyChapterStartInTheDirectionOfTravel() {
        val chapters = Chapters(listOf(Chapter("One", 0), Chapter("Two", 113_000)))
        val scrubber = Scrubber(durationMillis = hour, chapters = chapters, snapThresholdMillis = 4_000)
        val state = scrubber.step(scrubber.start(100_000), SeekDirection.Forward)
        // 110 s raw, chapter at 113 s is within half a step (5 s): snap.
        assertEquals(113_000L, state.previewMillis)
        assertTrue(state.snapped)
        assertEquals("Two", state.chapter?.title)
        // The next press leaves the chapter start.
        val next = scrubber.step(state, SeekDirection.Forward)
        assertEquals(123_000L, next.previewMillis)
        assertFalse(next.snapped)
    }

    @Test
    fun doesNotSnapBehindOrFarAway() {
        val chapters = Chapters(listOf(Chapter("One", 0), Chapter("Two", 130_000)))
        val scrubber = Scrubber(durationMillis = hour, chapters = chapters, snapThresholdMillis = 4_000)
        val state = scrubber.step(scrubber.start(100_000), SeekDirection.Forward)
        assertEquals(110_000L, state.previewMillis)
        assertFalse(state.snapped)
        // Going back from just after a chapter start does not snap forward onto it.
        val back = scrubber.step(scrubber.start(131_000), SeekDirection.Backward)
        assertEquals(121_000L, back.previewMillis)
    }

    @Test
    fun snapsWhileScrubbingBackward() {
        val chapters = Chapters(listOf(Chapter("One", 0), Chapter("Two", 88_000)))
        val scrubber = Scrubber(durationMillis = hour, chapters = chapters)
        val state = scrubber.step(scrubber.start(100_000), SeekDirection.Backward)
        assertEquals(88_000L, state.previewMillis)
        assertEquals("Two", state.chapter?.title)
    }

    @Test
    fun snappingCanBeDisabled() {
        val chapters = Chapters(listOf(Chapter("Two", 113_000)))
        val scrubber = Scrubber(durationMillis = hour, chapters = chapters, snapThresholdMillis = 0)
        assertEquals(110_000L, scrubber.step(scrubber.start(100_000), SeekDirection.Forward).previewMillis)
    }

    @Test
    fun moveToJumpsWithoutSnapping() {
        val chapters = Chapters(listOf(Chapter("Two", 113_000)))
        val scrubber = Scrubber(durationMillis = hour, chapters = chapters)
        val state = scrubber.moveTo(scrubber.start(0), 112_000)
        assertEquals(112_000L, state.previewMillis)
        assertFalse(state.snapped)
    }
}
