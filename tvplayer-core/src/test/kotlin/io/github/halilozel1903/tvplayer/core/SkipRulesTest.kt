package io.github.halilozel1903.tvplayer.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkipRulesTest {

    private val intro = SkipWindow(SkipKind.Intro, startMillis = 60_000, endMillis = 125_000)
    private val credits = SkipWindow(SkipKind.Credits, startMillis = 2_700_000, endMillis = 2_832_000, label = "Next episode")
    private val windows = listOf(intro, credits)

    @Test
    fun visibleInsideTheWindow() {
        val rules = SkipRules()
        assertNull(rules.visibleWindow(windows, 59_999, controlsVisible = false))
        assertEquals(intro, rules.visibleWindow(windows, 60_000, controlsVisible = false))
        assertEquals(credits, rules.visibleWindow(windows, 2_700_500, controlsVisible = false))
    }

    @Test
    fun hiddenRightBeforeTheEnd() {
        val rules = SkipRules(minRemainingMillis = 1_000)
        assertEquals(intro, rules.visibleWindow(windows, 123_999, controlsVisible = true))
        assertNull(rules.visibleWindow(windows, 124_000, controlsVisible = true))
    }

    @Test
    fun standaloneOnlyAtTheStartOfTheWindow() {
        val rules = SkipRules(standaloneMillis = 10_000)
        assertEquals(intro, rules.visibleWindow(windows, 69_999, controlsVisible = false))
        assertNull(rules.visibleWindow(windows, 70_000, controlsVisible = false))
        // With the controls up the button is back.
        assertEquals(intro, rules.visibleWindow(windows, 90_000, controlsVisible = true))
        // Without a standalone limit it stays.
        assertEquals(intro, SkipRules(standaloneMillis = null).visibleWindow(windows, 90_000, controlsVisible = false))
    }

    @Test
    fun activeWindowIgnoresVisibilityRules() {
        assertEquals(intro, SkipRules().activeWindow(windows, 124_500))
        assertNull(SkipRules().activeWindow(windows, 125_000))
    }

    @Test
    fun windowContainsAndTarget() {
        assertTrue(70_000 in intro)
        assertFalse(125_000 in intro)
        assertEquals(125_000L, intro.targetMillis)
        assertEquals(3_000_000L, credits.copy(targetMillis = 3_000_000).targetMillis)
    }

    @Test
    fun rejectsEmptyWindows() {
        assertFailsWith<IllegalArgumentException> { SkipWindow(SkipKind.Intro, 10, 10) }
        assertFailsWith<IllegalArgumentException> { SkipWindow(SkipKind.Intro, -1, 10) }
    }
}
