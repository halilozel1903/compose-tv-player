package io.github.halilozel1903.tvplayer.core

/**
 * What a player reports at one moment, independent of the player library.
 *
 * @param positionMillis the playhead.
 * @param durationMillis the media duration, or a negative value while unknown (live streams,
 *   before the media is prepared).
 * @param bufferedPositionMillis how far the media is buffered.
 * @param isPlaying `true` while playback runs or is about to run (playing or buffering with play
 *   requested). This decides between the play and pause icon.
 * @param isBuffering waiting for data.
 * @param isEnded playback reached the end.
 * @param tracks audio and subtitle tracks of the current media.
 */
public data class PlaybackSnapshot(
    val positionMillis: Long = 0L,
    val durationMillis: Long = -1L,
    val bufferedPositionMillis: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val tracks: List<TrackOption> = emptyList(),
) {
    /** `true` once the duration is known. */
    val hasDuration: Boolean get() = durationMillis > 0L

    /** The played fraction in `0..1`. */
    val fraction: Float get() = Timeline.fraction(positionMillis, durationMillis)

    /** The buffered fraction in `0..1`, never behind [fraction]. */
    val bufferedFraction: Float get() = Timeline.bufferedFraction(bufferedPositionMillis, positionMillis, durationMillis)

    /** Audio tracks only. */
    val audioTracks: List<TrackOption> get() = tracks.filter { it.kind == TrackKind.Audio }

    /** Subtitle tracks only. */
    val subtitleTracks: List<TrackOption> get() = tracks.filter { it.kind == TrackKind.Subtitle }
}
