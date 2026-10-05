package io.github.halilozel1903.tvplayer.sample

import io.github.halilozel1903.tvplayer.core.Chapter
import io.github.halilozel1903.tvplayer.core.SkipKind
import io.github.halilozel1903.tvplayer.core.SkipWindow
import io.github.halilozel1903.tvplayer.core.TrackKind
import io.github.halilozel1903.tvplayer.core.TrackOption

/**
 * A made-up episode of a made-up series on "Driftwood", the sample's fictional streaming app.
 * No video is bundled: the screenshot scenes play it with a FakePlayerEngine over generated frames.
 */
object SampleEpisode {
    const val SERIES = "Harbor Lights"
    const val TITLE = "The Lantern Coast"
    const val SUBTITLE = "$SERIES · S1 E4"

    val durationMillis: Long = mmss(47, 12)

    val chapters: List<Chapter> = listOf(
        Chapter("Previously", 0),
        Chapter("Opening titles", mmss(0, 58)),
        Chapter("The harbor at dawn", mmss(2, 5)),
        Chapter("A letter from the north", mmss(9, 40)),
        Chapter("Storm warning", mmss(18, 22)),
        Chapter("The lighthouse keeper", mmss(27, 5)),
        Chapter("Low tide", mmss(36, 48)),
        Chapter("Credits", mmss(45, 30)),
    )

    val skipWindows: List<SkipWindow> = listOf(
        SkipWindow(SkipKind.Recap, startMillis = 0, endMillis = mmss(0, 58)),
        SkipWindow(SkipKind.Intro, startMillis = mmss(0, 58), endMillis = mmss(2, 5)),
        SkipWindow(SkipKind.Credits, startMillis = mmss(45, 30), endMillis = durationMillis, label = "Next episode"),
    )

    val tracks: List<TrackOption> = listOf(
        TrackOption("a-en-51", TrackKind.Audio, language = "en", channelCount = 6, isSelected = true),
        TrackOption("a-en-20", TrackKind.Audio, language = "en", channelCount = 2),
        TrackOption("a-en-ad", TrackKind.Audio, language = "en", label = "English · Audio description", channelCount = 2),
        TrackOption("a-es", TrackKind.Audio, language = "es", channelCount = 6),
        TrackOption("a-ja", TrackKind.Audio, language = "ja", channelCount = 2),
        TrackOption("s-en-cc", TrackKind.Subtitle, language = "en", isClosedCaption = true, isSelected = true),
        TrackOption("s-en-forced", TrackKind.Subtitle, language = "en", isForced = true),
        TrackOption("s-es", TrackKind.Subtitle, language = "es"),
        TrackOption("s-fr", TrackKind.Subtitle, language = "fr"),
        TrackOption("s-de", TrackKind.Subtitle, language = "de"),
        TrackOption("s-pt-br", TrackKind.Subtitle, language = "pt-BR"),
        TrackOption("s-ja", TrackKind.Subtitle, language = "ja"),
    )
}

/**
 * A public sample video for the normal launch (no `scene` extra): "Big Buck Bunny" by the
 * Blender Foundation (CC BY 3.0), served from Google's public sample bucket. Screenshots never
 * load it.
 */
const val SAMPLE_VIDEO_URL: String = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

/** Chapters and an intro window for the public sample video, to try the features on real media. */
object SampleVideo {
    const val TITLE = "Big Buck Bunny"
    const val SUBTITLE = "Blender Foundation · Public sample"

    val chapters: List<Chapter> = listOf(
        Chapter("Opening", 0),
        Chapter("Morning in the meadow", mmss(1, 0)),
        Chapter("Uninvited guests", mmss(3, 30)),
        Chapter("The plan", mmss(6, 0)),
        Chapter("Credits", mmss(8, 10)),
    )

    val skipWindows: List<SkipWindow> = listOf(
        SkipWindow(SkipKind.Intro, startMillis = 0, endMillis = mmss(0, 25)),
    )
}

/** Minutes and seconds to milliseconds. */
private fun mmss(minutes: Int, seconds: Int): Long = (minutes * 60L + seconds) * 1_000L
