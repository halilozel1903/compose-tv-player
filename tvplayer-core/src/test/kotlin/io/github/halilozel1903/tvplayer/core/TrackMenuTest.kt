package io.github.halilozel1903.tvplayer.core

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackMenuTest {

    private val menu = TrackMenu()

    @Test
    fun languageNames() {
        assertEquals("English", menu.languageName("en"))
        assertEquals("Portuguese (Brazil)", menu.languageName("pt-BR"))
        assertEquals("Portuguese (Brazil)", menu.languageName("pt_BR"))
        assertEquals("Japanese", menu.languageName("ja"))
        assertEquals("Unknown", menu.languageName(null))
        assertEquals("Unknown", menu.languageName("und"))
        assertEquals("Unknown", menu.languageName(" "))
    }

    @Test
    fun languageNamesInAnotherLocale() {
        val german = TrackMenu(displayLocale = Locale.GERMAN)
        assertEquals("Englisch", german.languageName("en"))
        // French writes language names in lower case; menus show them capitalized.
        assertEquals("Anglais", TrackMenu(displayLocale = Locale.FRENCH).languageName("en"))
    }

    @Test
    fun subtitlesStartWithOff() {
        val tracks = listOf(
            TrackOption("t1", TrackKind.Subtitle, language = "en", isClosedCaption = true),
            TrackOption("t2", TrackKind.Subtitle, language = "es"),
            TrackOption("a1", TrackKind.Audio, language = "en"),
        )
        val items = menu.subtitles(tracks)
        assertEquals(listOf("Off", "English", "Spanish"), items.map { it.title })
        assertTrue(items[0].isOff)
        assertTrue(items[0].isSelected)
        assertEquals("CC", items[1].detail)
        assertEquals(TrackMenu.OFF_KEY, items[0].key)
    }

    @Test
    fun offIsNotSelectedWhenASubtitleIs() {
        val items = menu.subtitles(listOf(TrackOption("t1", TrackKind.Subtitle, language = "fr", isSelected = true)))
        assertFalse(items[0].isSelected)
        assertTrue(items[1].isSelected)
        assertEquals("French", menu.selectedTitle(items))
    }

    @Test
    fun audioRowsWithChannelLayouts() {
        val tracks = listOf(
            TrackOption("a1", TrackKind.Audio, language = "en", channelCount = 6, isSelected = true),
            TrackOption("a2", TrackKind.Audio, language = "en", channelCount = 2),
            TrackOption("a3", TrackKind.Audio, language = "en", label = "Audio description", channelCount = 2),
            TrackOption("a4", TrackKind.Audio, language = "ja", channelCount = 8),
            TrackOption("a5", TrackKind.Audio, language = "de", channelCount = 1),
        )
        val items = menu.audio(tracks)
        assertEquals(listOf("English", "English", "Audio description", "Japanese", "German"), items.map { it.title })
        assertEquals(listOf("5.1", "Stereo", "Stereo", "7.1", "Mono"), items.map { it.detail })
        assertTrue(items.none { it.isOff })
        assertEquals("a1", items.single { it.isSelected }.key)
    }

    @Test
    fun identicalRowsAreNumbered() {
        val tracks = listOf(
            TrackOption("t1", TrackKind.Subtitle, language = "en"),
            TrackOption("t2", TrackKind.Subtitle, language = "en"),
            TrackOption("t3", TrackKind.Subtitle, language = "en", isForced = true),
        )
        val titles = menu.subtitles(tracks).map { it.title to it.detail }
        assertEquals(listOf("Off" to null, "English 1" to null, "English 2" to null, "English" to "Forced"), titles)
    }

    @Test
    fun channelLayouts() {
        assertNull(menu.channelLayout(0))
        assertEquals("4 ch", menu.channelLayout(4))
    }

    @Test
    fun translatedLabels() {
        val turkish = TrackMenu(TrackMenu.Labels(off = "Kapalı"), Locale.forLanguageTag("tr"))
        assertEquals("Kapalı", turkish.subtitles(emptyList()).single().title)
        assertEquals("İngilizce", turkish.languageName("en"))
    }
}
