package io.github.halilozel1903.tvplayer.core

import kotlin.math.abs

/** Which way a D-pad press moves the seek position. */
public enum class SeekDirection(public val sign: Int) {
    Backward(-1),
    Forward(1),
}

/**
 * How far one D-pad press moves the seek position. A single press moves [baseStepMillis]; holding
 * the key down sends repeated key events, and every [repeatsPerLevel] repeats the step grows to
 * the next entry of [levels]. Steps never go above [maxFractionOfDuration] of the media (but
 * never below [baseStepMillis] either), so a three minute clip does not jump by two minutes.
 *
 * @param baseStepMillis the step of a single press.
 * @param levels steps used while the key is held, in order.
 * @param repeatsPerLevel key repeats before moving to the next level. Android repeats a held key
 *   about 20 times per second, so the default of 8 grows the step every ~0.4 seconds.
 * @param maxFractionOfDuration the largest step as a fraction of the duration.
 */
public data class SeekAcceleration(
    val baseStepMillis: Long = 10_000L,
    val levels: List<Long> = listOf(10_000L, 20_000L, 30_000L, 60_000L, 120_000L),
    val repeatsPerLevel: Int = 8,
    val maxFractionOfDuration: Float = 0.05f,
) {
    init {
        require(baseStepMillis > 0L) { "baseStepMillis must be positive" }
        require(levels.isNotEmpty() && levels.all { it > 0L }) { "levels must be positive steps" }
        require(repeatsPerLevel > 0) { "repeatsPerLevel must be positive" }
        require(maxFractionOfDuration > 0f) { "maxFractionOfDuration must be positive" }
    }

    /**
     * The step for a key event.
     *
     * @param repeatCount `KeyEvent.getRepeatCount()`: 0 for a fresh press, then 1, 2, ... while held.
     * @param durationMillis the media duration, or a negative value when unknown.
     */
    public fun stepFor(repeatCount: Int, durationMillis: Long): Long {
        val raw = if (repeatCount <= 0) {
            baseStepMillis
        } else {
            levels[(repeatCount / repeatsPerLevel).coerceAtMost(levels.lastIndex)]
        }
        if (durationMillis <= 0L) return raw
        val cap = maxOf(baseStepMillis, (durationMillis * maxFractionOfDuration).toLong())
        return raw.coerceAtMost(cap)
    }

    /** Level index for [repeatCount], `0` for a single press. Useful to show "2x", "3x" hints. */
    public fun levelFor(repeatCount: Int): Int =
        if (repeatCount <= 0) 0 else (repeatCount / repeatsPerLevel).coerceAtMost(levels.lastIndex)
}

/**
 * A scrub in progress: the playhead was at [originMillis] when it started, and the seek bar now
 * previews [previewMillis]. [chapter] is the chapter at the preview position and [snapped] tells
 * whether the last step landed exactly on a chapter start.
 */
public data class ScrubState(
    val originMillis: Long,
    val previewMillis: Long,
    val chapter: Chapter? = null,
    val snapped: Boolean = false,
    val repeatLevel: Int = 0,
) {
    /** How far the preview is from where the scrub started; negative when scrubbing back. */
    val offsetMillis: Long get() = previewMillis - originMillis
}

/**
 * Moves a seek preview with the D-pad, with step acceleration and snapping to chapter starts.
 *
 * Snapping: when a step lands close to a chapter start that lies in the direction of travel, the
 * preview stops exactly on that chapter start. "Close" is [snapThresholdMillis] or half the step,
 * whichever is larger, so even accelerated steps stop at chapter boundaries they nearly hit. The
 * next press leaves the chapter start again, since a snap never targets the current position.
 *
 * @param durationMillis the media duration; a negative or zero value disables clamping at the end.
 * @param chapters chapters to snap to; empty disables snapping.
 * @param acceleration step sizes.
 * @param snapThresholdMillis the smallest snapping distance; `0` disables snapping.
 */
public class Scrubber(
    private val durationMillis: Long,
    private val chapters: Chapters = Chapters.Empty,
    private val acceleration: SeekAcceleration = SeekAcceleration(),
    private val snapThresholdMillis: Long = 4_000L,
) {

    /** Starts a scrub at the current playhead. */
    public fun start(positionMillis: Long): ScrubState {
        val position = clamp(positionMillis)
        return ScrubState(originMillis = position, previewMillis = position, chapter = chapters.chapterAt(position))
    }

    /** Moves the preview one key event in [direction]. */
    public fun step(state: ScrubState, direction: SeekDirection, repeatCount: Int = 0): ScrubState {
        val step = acceleration.stepFor(repeatCount, durationMillis)
        val raw = clamp(state.previewMillis + direction.sign * step)
        val snapTarget = if (snapThresholdMillis > 0L) snapTarget(state.previewMillis, raw, direction, step) else null
        val target = snapTarget ?: raw
        return state.copy(
            previewMillis = target,
            chapter = chapters.chapterAt(target),
            snapped = snapTarget != null,
            repeatLevel = acceleration.levelFor(repeatCount),
        )
    }

    /** Moves the preview to [positionMillis] directly, without snapping. */
    public fun moveTo(state: ScrubState, positionMillis: Long): ScrubState {
        val target = clamp(positionMillis)
        return state.copy(previewMillis = target, chapter = chapters.chapterAt(target), snapped = false, repeatLevel = 0)
    }

    private fun snapTarget(from: Long, raw: Long, direction: SeekDirection, step: Long): Long? {
        val threshold = maxOf(snapThresholdMillis, step / 2)
        return chapters.list
            .asSequence()
            .map { it.startMillis }
            .filter { start -> if (direction == SeekDirection.Forward) start > from else start < from }
            .filter { start -> durationMillis <= 0L || start < durationMillis }
            .filter { start -> abs(start - raw) <= threshold }
            .minByOrNull { start -> abs(start - raw) }
    }

    private fun clamp(position: Long): Long {
        val nonNegative = position.coerceAtLeast(0L)
        return if (durationMillis > 0L) nonNegative.coerceAtMost(durationMillis) else nonNegative
    }
}
