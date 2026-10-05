package io.github.halilozel1903.tvplayer.core

/** Something that affects whether the player controls are on screen. */
public sealed interface ControlsEvent {
    /** A key press or click: show the controls and restart the idle timer. */
    public data object Interaction : ControlsEvent

    /** Time passed; hides the controls after the idle timeout when nothing holds them up. */
    public data class Tick(val deltaMillis: Long) : ControlsEvent

    /** Playback started or stopped. Pausing shows the controls and keeps them up. */
    public data class PlaybackChanged(val isPlaying: Boolean) : ControlsEvent

    /** A D-pad scrub started or ended. Scrubbing keeps the controls up. */
    public data class ScrubbingChanged(val isScrubbing: Boolean) : ControlsEvent

    /** A side panel (subtitles and audio) opened or closed. An open panel keeps the controls up. */
    public data class PanelChanged(val isOpen: Boolean) : ControlsEvent

    /** Show the controls now, as if the viewer pressed a key. */
    public data object Show : ControlsEvent

    /** Hide the controls now (the Back key). Ignored while scrubbing or while a panel is open. */
    public data object Hide : ControlsEvent
}

/**
 * The auto-hide state machine of the player controls, as an immutable value.
 *
 * The controls hide after [autoHideMillis] without input, but only while the media plays and
 * nothing holds them up: a pause, a scrub or an open panel keep them on screen.
 *
 * ```
 * var controls = ControlsState(autoHideMillis = 5_000)
 * controls = controls.reduce(ControlsEvent.Tick(16))
 * ```
 *
 * @param visible whether the controls are on screen.
 * @param autoHideMillis idle time before hiding; `null` never hides on its own.
 * @param idleMillis time since the last interaction while the controls could hide.
 */
public data class ControlsState(
    val visible: Boolean = true,
    val autoHideMillis: Long? = 5_000L,
    val idleMillis: Long = 0L,
    val isPlaying: Boolean = false,
    val isScrubbing: Boolean = false,
    val isPanelOpen: Boolean = false,
) {

    /** `true` when the idle timer runs. */
    val canAutoHide: Boolean
        get() = autoHideMillis != null && isPlaying && !isScrubbing && !isPanelOpen

    /** Time left before the controls hide, or `null` when they will not hide on their own. */
    val millisUntilHide: Long?
        get() = if (visible && canAutoHide) (autoHideMillis!! - idleMillis).coerceAtLeast(0L) else null

    /** The state after [event]. */
    public fun reduce(event: ControlsEvent): ControlsState = when (event) {
        ControlsEvent.Interaction, ControlsEvent.Show -> copy(visible = true, idleMillis = 0L)
        is ControlsEvent.Tick -> tick(event.deltaMillis)
        is ControlsEvent.PlaybackChanged -> when {
            event.isPlaying == isPlaying -> this
            event.isPlaying -> copy(isPlaying = true, idleMillis = 0L)
            else -> copy(isPlaying = false, visible = true, idleMillis = 0L)
        }
        is ControlsEvent.ScrubbingChanged -> copy(
            isScrubbing = event.isScrubbing,
            visible = visible || event.isScrubbing,
            idleMillis = 0L,
        )
        is ControlsEvent.PanelChanged -> copy(
            isPanelOpen = event.isOpen,
            visible = visible || event.isOpen,
            idleMillis = 0L,
        )
        ControlsEvent.Hide -> if (isScrubbing || isPanelOpen) this else copy(visible = false, idleMillis = 0L)
    }

    private fun tick(deltaMillis: Long): ControlsState {
        if (!visible || !canAutoHide || deltaMillis <= 0L) return this
        val idle = idleMillis + deltaMillis
        return if (idle >= autoHideMillis!!) copy(visible = false, idleMillis = 0L) else copy(idleMillis = idle)
    }
}
