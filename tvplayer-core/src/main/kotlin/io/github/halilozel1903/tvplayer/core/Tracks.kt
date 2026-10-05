package io.github.halilozel1903.tvplayer.core

import java.util.Locale

/** The kind of a selectable track. */
public enum class TrackKind {
    Audio,
    Subtitle,
}

/**
 * One audio or subtitle track as the player reports it.
 *
 * @param id a stable key the player engine uses to select the track again.
 * @param language a BCP 47 language tag such as `"en"` or `"pt-BR"`, or `null` when unknown.
 * @param label a name from the stream (`"Director's commentary"`); it wins over the language name.
 * @param channelCount audio channels, `0` when unknown.
 * @param isForced a forced subtitle track (only signs and foreign dialogue).
 * @param isClosedCaption subtitles with sound descriptions for deaf and hard of hearing viewers.
 * @param isSelected whether the player plays this track now.
 */
public data class TrackOption(
    val id: String,
    val kind: TrackKind,
    val language: String? = null,
    val label: String? = null,
    val channelCount: Int = 0,
    val isForced: Boolean = false,
    val isClosedCaption: Boolean = false,
    val isSelected: Boolean = false,
)

/**
 * A row of the subtitle or audio menu. [track] is `null` for the "Off" row.
 */
public data class TrackMenuItem(
    val key: String,
    val title: String,
    val detail: String?,
    val isSelected: Boolean,
    val track: TrackOption?,
) {
    /** `true` for the "Off" row of the subtitle menu. */
    val isOff: Boolean get() = track == null
}

/**
 * Turns tracks into menu rows with readable names: language tags become `"English"` or
 * `"Portuguese (Brazil)"`, audio gets `"5.1"` or `"Stereo"`, subtitles get `"CC"` or `"Forced"`,
 * and rows that would still look the same are numbered.
 *
 * @param labels the words used in the menu, to translate it.
 * @param displayLocale the language that language names are written in.
 */
public class TrackMenu(
    private val labels: Labels = Labels(),
    private val displayLocale: Locale = Locale.ENGLISH,
) {

    /** Subtitle rows: "Off" first (selected when no subtitle track is), then the tracks. */
    public fun subtitles(tracks: List<TrackOption>): List<TrackMenuItem> {
        val subtitleTracks = tracks.filter { it.kind == TrackKind.Subtitle }
        val anySelected = subtitleTracks.any { it.isSelected }
        return buildList<TrackMenuItem> {
            add(TrackMenuItem(key = OFF_KEY, title = labels.off, detail = null, isSelected = !anySelected, track = null))
            addAll(rows(subtitleTracks))
        }
    }

    /** Audio rows, without an "Off" row. */
    public fun audio(tracks: List<TrackOption>): List<TrackMenuItem> =
        rows(tracks.filter { it.kind == TrackKind.Audio })

    /** The title of the selected row, for a button such as `"Subtitles: English"`. */
    public fun selectedTitle(items: List<TrackMenuItem>): String? = items.firstOrNull { it.isSelected }?.title

    /**
     * The name of a language tag in [displayLocale], with a capital first letter:
     * `"en"` -> `"English"`, `"pt-BR"` -> `"Portuguese (Brazil)"`. Unknown or empty tags give
     * [Labels.unknown].
     */
    public fun languageName(tag: String?): String {
        val clean = tag?.trim()?.replace('_', '-')
        if (clean.isNullOrEmpty() || clean.equals("und", ignoreCase = true)) return labels.unknown
        val locale = Locale.forLanguageTag(clean)
        val name = locale.getDisplayName(displayLocale)
        if (name.isBlank() || locale.language.isEmpty()) return clean
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(displayLocale) else it.toString() }
    }

    /** The row title of a track: its label, or the language name. */
    public fun title(track: TrackOption): String =
        track.label?.takeIf { it.isNotBlank() } ?: languageName(track.language)

    /** The row detail: channel layout for audio, CC or forced for subtitles. */
    public fun detail(track: TrackOption): String? = when (track.kind) {
        TrackKind.Audio -> channelLayout(track.channelCount)
        TrackKind.Subtitle -> when {
            track.isForced -> labels.forced
            track.isClosedCaption -> labels.closedCaptions
            else -> null
        }
    }

    /** `"Mono"`, `"Stereo"`, `"5.1"`, `"7.1"`, `"N ch"`, or `null` when unknown. */
    public fun channelLayout(channelCount: Int): String? = when {
        channelCount <= 0 -> null
        channelCount == 1 -> labels.mono
        channelCount == 2 -> labels.stereo
        channelCount == 6 -> "5.1"
        channelCount == 8 -> "7.1"
        else -> "$channelCount ${labels.channels}"
    }

    private fun rows(tracks: List<TrackOption>): List<TrackMenuItem> {
        val base = tracks.map { track ->
            TrackMenuItem(key = track.id, title = title(track), detail = detail(track), isSelected = track.isSelected, track = track)
        }
        // Number rows that still look identical ("English", "English") so they can be told apart.
        val counts = base.groupingBy { it.title to it.detail }.eachCount()
        val seen = HashMap<Pair<String, String?>, Int>()
        return base.map { item ->
            val key = item.title to item.detail
            if ((counts[key] ?: 0) < 2) {
                item
            } else {
                val number = (seen[key] ?: 0) + 1
                seen[key] = number
                item.copy(title = "${item.title} $number")
            }
        }
    }

    /** Words used by [TrackMenu]. */
    public data class Labels(
        val off: String = "Off",
        val unknown: String = "Unknown",
        val forced: String = "Forced",
        val closedCaptions: String = "CC",
        val mono: String = "Mono",
        val stereo: String = "Stereo",
        val channels: String = "ch",
    )

    public companion object {
        /** The [TrackMenuItem.key] of the "Off" row. */
        public const val OFF_KEY: String = "off"
    }
}
