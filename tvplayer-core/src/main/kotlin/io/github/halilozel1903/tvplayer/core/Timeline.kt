package io.github.halilozel1903.tvplayer.core

/**
 * Position math for a seek bar. Every function accepts the raw values a player reports, including
 * an unknown (negative) duration, and never divides by zero.
 */
public object Timeline {

    /** The played fraction in `0..1`, or `0` while the duration is unknown. */
    public fun fraction(positionMillis: Long, durationMillis: Long): Float {
        if (durationMillis <= 0L) return 0f
        return (positionMillis.coerceIn(0L, durationMillis).toDouble() / durationMillis).toFloat()
    }

    /**
     * The buffered fraction in `0..1`. It is never behind the played fraction, because a player
     * can briefly report a buffered position below the playhead right after a seek.
     */
    public fun bufferedFraction(bufferedPositionMillis: Long, positionMillis: Long, durationMillis: Long): Float {
        if (durationMillis <= 0L) return 0f
        return maxOf(fraction(bufferedPositionMillis, durationMillis), fraction(positionMillis, durationMillis))
    }

    /** The position at [fraction] of the duration, clamped to the media. */
    public fun positionAt(fraction: Float, durationMillis: Long): Long {
        if (durationMillis <= 0L) return 0L
        return (fraction.coerceIn(0f, 1f).toDouble() * durationMillis).toLong()
    }

    /** The time left, never negative. */
    public fun remaining(positionMillis: Long, durationMillis: Long): Long {
        if (durationMillis <= 0L) return 0L
        return (durationMillis - positionMillis.coerceAtLeast(0L)).coerceAtLeast(0L)
    }

    /** [positionMillis] moved by [deltaMillis] and clamped to `0..durationMillis`. */
    public fun seekBy(positionMillis: Long, deltaMillis: Long, durationMillis: Long): Long {
        val target = (positionMillis + deltaMillis).coerceAtLeast(0L)
        return if (durationMillis > 0L) target.coerceAtMost(durationMillis) else target
    }
}
