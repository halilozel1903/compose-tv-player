package io.github.halilozel1903.tvplayer.core

/** What a [SkipWindow] skips. */
public enum class SkipKind {
    /** "Previously on": skipping lands at the end of the recap. */
    Recap,

    /** Opening titles. */
    Intro,

    /** End credits; apps often use this to offer the next episode instead. */
    Credits,
}

/**
 * A span of the media the viewer can skip, from [startMillis] to [endMillis]. Pressing the skip
 * button seeks to [targetMillis], which is [endMillis] unless you pass another target.
 *
 * @param label the button text; `null` uses the default text for [kind].
 */
public data class SkipWindow(
    val kind: SkipKind,
    val startMillis: Long,
    val endMillis: Long,
    val label: String? = null,
    val targetMillis: Long = endMillis,
) {
    init {
        require(startMillis >= 0L) { "startMillis must not be negative, was $startMillis" }
        require(endMillis > startMillis) { "endMillis ($endMillis) must be after startMillis ($startMillis)" }
    }

    /** `true` when [positionMillis] is inside the window. */
    public operator fun contains(positionMillis: Long): Boolean = positionMillis in startMillis until endMillis
}

/**
 * When the skip button is on screen.
 *
 * The button shows while the playhead is inside a window, except for the last [minRemainingMillis]
 * (skipping one second is pointless). Like most streaming apps it only stays on its own for the
 * first [standaloneMillis] of a window; after that it shows only together with the controls.
 *
 * @param minRemainingMillis hide the button this close to the window's end.
 * @param standaloneMillis how long the button shows without the controls; `null` keeps it up for
 *   the whole window.
 */
public data class SkipRules(
    val minRemainingMillis: Long = 1_000L,
    val standaloneMillis: Long? = 10_000L,
) {

    /**
     * The window whose button should be visible at [positionMillis], or `null`.
     *
     * @param controlsVisible whether the player controls are on screen.
     */
    public fun visibleWindow(
        windows: List<SkipWindow>,
        positionMillis: Long,
        controlsVisible: Boolean,
    ): SkipWindow? = windows.firstOrNull { window ->
        positionMillis >= window.startMillis &&
            positionMillis < window.endMillis - minRemainingMillis &&
            (controlsVisible || standaloneMillis == null || positionMillis - window.startMillis < standaloneMillis)
    }

    /** The window that contains [positionMillis], visible or not. */
    public fun activeWindow(windows: List<SkipWindow>, positionMillis: Long): SkipWindow? =
        windows.firstOrNull { positionMillis in it }
}
