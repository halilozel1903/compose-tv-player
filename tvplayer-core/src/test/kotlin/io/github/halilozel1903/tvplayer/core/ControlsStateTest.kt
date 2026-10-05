package io.github.halilozel1903.tvplayer.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ControlsStateTest {

    private val playing = ControlsState(visible = true, autoHideMillis = 5_000, isPlaying = true)

    @Test
    fun hidesAfterTheIdleTimeoutWhilePlaying() {
        var state = playing.reduce(ControlsEvent.Tick(4_999))
        assertTrue(state.visible)
        assertEquals(1L, state.millisUntilHide)
        state = state.reduce(ControlsEvent.Tick(1))
        assertFalse(state.visible)
        assertNull(state.millisUntilHide)
    }

    @Test
    fun interactionRestartsTheTimer() {
        val state = playing
            .reduce(ControlsEvent.Tick(4_000))
            .reduce(ControlsEvent.Interaction)
            .reduce(ControlsEvent.Tick(4_000))
        assertTrue(state.visible)
    }

    @Test
    fun interactionShowsHiddenControls() {
        val hidden = playing.reduce(ControlsEvent.Tick(5_000))
        assertTrue(hidden.reduce(ControlsEvent.Interaction).visible)
        assertTrue(hidden.reduce(ControlsEvent.Show).visible)
    }

    @Test
    fun pausedControlsStayUp() {
        val paused = playing.reduce(ControlsEvent.Tick(5_000)).reduce(ControlsEvent.PlaybackChanged(false))
        assertTrue(paused.visible)
        assertTrue(paused.reduce(ControlsEvent.Tick(60_000)).visible)
        assertNull(paused.millisUntilHide)
        // Resuming starts the timer again.
        val resumed = paused.reduce(ControlsEvent.PlaybackChanged(true)).reduce(ControlsEvent.Tick(5_000))
        assertFalse(resumed.visible)
    }

    @Test
    fun scrubbingAndPanelsHoldTheControls() {
        val scrubbing = playing.reduce(ControlsEvent.ScrubbingChanged(true))
        assertTrue(scrubbing.reduce(ControlsEvent.Tick(60_000)).visible)
        assertTrue(scrubbing.reduce(ControlsEvent.Hide).visible)
        val panel = playing.reduce(ControlsEvent.PanelChanged(true))
        assertTrue(panel.reduce(ControlsEvent.Tick(60_000)).visible)
        assertTrue(panel.reduce(ControlsEvent.Hide).visible)
        val closed = panel.reduce(ControlsEvent.PanelChanged(false)).reduce(ControlsEvent.Tick(5_000))
        assertFalse(closed.visible)
    }

    @Test
    fun scrubbingShowsHiddenControls() {
        val hidden = playing.reduce(ControlsEvent.Hide)
        assertFalse(hidden.visible)
        assertTrue(hidden.reduce(ControlsEvent.ScrubbingChanged(true)).visible)
    }

    @Test
    fun autoHideCanBeTurnedOff() {
        val state = playing.copy(autoHideMillis = null).reduce(ControlsEvent.Tick(60_000))
        assertTrue(state.visible)
        assertFalse(state.canAutoHide)
    }

    @Test
    fun repeatedPlaybackEventsKeepTheTimer() {
        val state = playing.reduce(ControlsEvent.Tick(3_000)).reduce(ControlsEvent.PlaybackChanged(true))
        assertEquals(3_000L, state.idleMillis)
    }
}
