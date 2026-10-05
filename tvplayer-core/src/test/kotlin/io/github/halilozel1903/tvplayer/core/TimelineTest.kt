package io.github.halilozel1903.tvplayer.core

import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineTest {

    @Test
    fun clockWithoutHours() {
        assertEquals("0:00", TimeFormat.clock(0))
        assertEquals("0:05", TimeFormat.clock(5_999))
        assertEquals("42:07", TimeFormat.clock(42 * 60_000L + 7_000))
    }

    @Test
    fun clockWithHours() {
        assertEquals("1:02:03", TimeFormat.clock(3_723_000))
        assertEquals("0:12:03", TimeFormat.clock(12 * 60_000L + 3_000, referenceMillis = 2 * 3_600_000L))
    }

    @Test
    fun negativeAndUnsetTimesPrintZero() {
        assertEquals("0:00", TimeFormat.clock(-5_000))
        assertEquals("0:00", TimeFormat.clock(Long.MIN_VALUE + 1))
    }

    @Test
    fun remainingAndProgress() {
        val duration = 47 * 60_000L + 12_000
        val position = 12 * 60_000L + 41_000
        assertEquals("-34:31", TimeFormat.remaining(position, duration))
        assertEquals("12:41 / 47:12", TimeFormat.progress(position, duration))
        assertEquals("-0:00", TimeFormat.remaining(duration + 5_000, duration))
    }

    @Test
    fun spokenTime() {
        assertEquals("12 minutes 41 seconds", TimeFormat.spoken(761_000))
        assertEquals("1 hour 1 minute 1 second", TimeFormat.spoken(3_661_000))
        assertEquals("0 seconds", TimeFormat.spoken(0))
    }

    @Test
    fun fractions() {
        assertEquals(0.25f, Timeline.fraction(25, 100))
        assertEquals(1f, Timeline.fraction(150, 100))
        assertEquals(0f, Timeline.fraction(50, -1))
        assertEquals(0f, Timeline.fraction(50, 0))
    }

    @Test
    fun bufferedFractionNeverBehindThePlayhead() {
        assertEquals(0.6f, Timeline.bufferedFraction(60, 40, 100))
        assertEquals(0.4f, Timeline.bufferedFraction(10, 40, 100))
        assertEquals(1f, Timeline.bufferedFraction(500, 40, 100))
        assertEquals(0f, Timeline.bufferedFraction(500, 40, -1))
    }

    @Test
    fun positionAtAndSeekBy() {
        assertEquals(50L, Timeline.positionAt(0.5f, 100))
        assertEquals(100L, Timeline.positionAt(2f, 100))
        assertEquals(0L, Timeline.seekBy(5_000, -10_000, 60_000))
        assertEquals(60_000L, Timeline.seekBy(55_000, 10_000, 60_000))
        assertEquals(65_000L, Timeline.seekBy(55_000, 10_000, -1))
        assertEquals(0L, Timeline.remaining(10, -1))
    }

    @Test
    fun snapshotDerivedValues() {
        val snapshot = PlaybackSnapshot(positionMillis = 30, durationMillis = 100, bufferedPositionMillis = 20)
        assertEquals(0.3f, snapshot.fraction)
        assertEquals(0.3f, snapshot.bufferedFraction)
        assertEquals(true, snapshot.hasDuration)
        assertEquals(false, PlaybackSnapshot().hasDuration)
    }
}
