package io.github.halilozel1903.tvplayer.core

/**
 * Formats playback times the way TV players show them: `"0:05"`, `"42:07"`, `"1:02:03"`.
 *
 * Negative values are treated as zero, so `C.TIME_UNSET` from Media3 never prints garbage.
 */
public object TimeFormat {

    /**
     * A clock style time.
     *
     * @param millis the time to format.
     * @param referenceMillis a time that sets the layout, usually the duration: when it is an hour
     *   or longer, [millis] also gets an hour field (`"0:12:03"`), so the elapsed and remaining
     *   labels keep the same width while playing.
     */
    public fun clock(millis: Long, referenceMillis: Long = millis): String {
        val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        val withHours = hours > 0L || referenceMillis >= 3_600_000L
        return if (withHours) {
            "$hours:${minutes.pad()}:${seconds.pad()}"
        } else {
            "$minutes:${seconds.pad()}"
        }
    }

    /** The time left with a leading minus, `"-34:31"`, laid out like [durationMillis]. */
    public fun remaining(positionMillis: Long, durationMillis: Long): String =
        "-" + clock(Timeline.remaining(positionMillis, durationMillis), durationMillis)

    /** `"12:41 / 47:12"`. */
    public fun progress(positionMillis: Long, durationMillis: Long): String =
        "${clock(positionMillis, durationMillis)} / ${clock(durationMillis)}"

    /**
     * A spoken description for accessibility, `"12 minutes 41 seconds"`.
     *
     * @param labels unit words, to translate it.
     */
    public fun spoken(millis: Long, labels: Labels = Labels()): String {
        val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        val parts = buildList<String> {
            if (hours > 0L) add("$hours ${if (hours == 1L) labels.hour else labels.hours}")
            if (minutes > 0L) add("$minutes ${if (minutes == 1L) labels.minute else labels.minutes}")
            if (seconds > 0L || isEmpty()) add("$seconds ${if (seconds == 1L) labels.second else labels.seconds}")
        }
        return parts.joinToString(" ")
    }

    /** Unit words for [spoken]. */
    public data class Labels(
        val hour: String = "hour",
        val hours: String = "hours",
        val minute: String = "minute",
        val minutes: String = "minutes",
        val second: String = "second",
        val seconds: String = "seconds",
    )

    private fun Long.pad(): String = toString().padStart(2, '0')
}
