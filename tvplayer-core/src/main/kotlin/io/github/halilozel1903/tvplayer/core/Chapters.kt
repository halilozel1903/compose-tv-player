package io.github.halilozel1903.tvplayer.core

/**
 * A named section of the media that starts at [startMillis] and runs until the next chapter.
 */
public data class Chapter(
    val title: String,
    val startMillis: Long,
) {
    init {
        require(startMillis >= 0L) { "startMillis must not be negative, was $startMillis" }
    }
}

/**
 * The chapters of one media item, sorted by start time. Duplicated start times keep the first
 * chapter. The list may be empty; every lookup then returns `null`.
 */
public class Chapters(chapters: List<Chapter>) {

    /** The chapters in playback order. */
    public val list: List<Chapter> = chapters.sortedBy { it.startMillis }.distinctBy { it.startMillis }

    /** Number of chapters. */
    public val size: Int get() = list.size

    /** `true` when there are no chapters. */
    public fun isEmpty(): Boolean = list.isEmpty()

    /** Index of the chapter that plays at [positionMillis], or `-1` before the first one. */
    public fun indexAt(positionMillis: Long): Int {
        var result = -1
        for ((index, chapter) in list.withIndex()) {
            if (chapter.startMillis <= positionMillis) result = index else break
        }
        return result
    }

    /** The chapter that plays at [positionMillis], or `null` before the first one. */
    public fun chapterAt(positionMillis: Long): Chapter? = list.getOrNull(indexAt(positionMillis))

    /** The first chapter that starts after [positionMillis]. */
    public fun next(positionMillis: Long): Chapter? = list.firstOrNull { it.startMillis > positionMillis }

    /**
     * Where a "previous chapter" press should go: the start of the current chapter, or the one
     * before it when the current chapter started less than [restartThresholdMillis] ago (the usual
     * double press behaviour).
     */
    public fun previous(positionMillis: Long, restartThresholdMillis: Long = 3_000L): Chapter? {
        val index = indexAt(positionMillis)
        if (index < 0) return null
        val current = list[index]
        return if (positionMillis - current.startMillis >= restartThresholdMillis || index == 0) {
            current
        } else {
            list[index - 1]
        }
    }

    /**
     * Fractions in `0..1` where the seek bar draws a chapter boundary. A chapter at 0 and chapters
     * at or past the end are left out.
     */
    public fun markerFractions(durationMillis: Long): List<Float> {
        if (durationMillis <= 0L) return emptyList()
        return list
            .filter { it.startMillis in 1 until durationMillis }
            .map { Timeline.fraction(it.startMillis, durationMillis) }
    }

    /** The end of the chapter at [index]: the next chapter's start, or [durationMillis]. */
    public fun endOf(index: Int, durationMillis: Long): Long =
        list.getOrNull(index + 1)?.startMillis ?: durationMillis

    override fun equals(other: Any?): Boolean = other is Chapters && other.list == list

    override fun hashCode(): Int = list.hashCode()

    override fun toString(): String = "Chapters($list)"

    public companion object {
        /** No chapters. */
        public val Empty: Chapters = Chapters(emptyList())
    }
}
