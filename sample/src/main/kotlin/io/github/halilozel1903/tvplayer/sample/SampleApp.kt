package io.github.halilozel1903.tvplayer.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import io.github.halilozel1903.tvplayer.FakePlayerEngine
import io.github.halilozel1903.tvplayer.PlayerFocusTarget
import io.github.halilozel1903.tvplayer.SeekThumbnailProvider
import io.github.halilozel1903.tvplayer.TvPlayer
import io.github.halilozel1903.tvplayer.TvPlayerConfig
import io.github.halilozel1903.tvplayer.TvPlayerState
import io.github.halilozel1903.tvplayer.core.Chapters
import io.github.halilozel1903.tvplayer.core.SkipRules
import io.github.halilozel1903.tvplayer.rememberTvPlayerState

/** The screenshot scenes, see [MainActivity]. [Live] streams the public sample video. */
enum class Scene(val id: String) {
    Live("live"),
    Playing("playing"),
    Scrubbing("scrubbing"),
    SkipIntro("skipintro"),
    Tracks("tracks"),
    ;

    companion object {
        fun from(id: String?): Scene = entries.firstOrNull { it.id == id } ?: Live
    }
}

@Composable
fun SampleApp(scene: Scene) {
    if (scene == Scene.Live) LivePlayer() else ScenePlayer(scene)
}

/** Streams [SAMPLE_VIDEO_URL] with ExoPlayer. */
@Composable
private fun LivePlayer() {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(SAMPLE_VIDEO_URL))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    val state = rememberTvPlayerState(
        player = player,
        chapters = SampleVideo.chapters,
        skipWindows = SampleVideo.skipWindows,
    )
    TvPlayer(
        state = state,
        title = SampleVideo.TITLE,
        subtitle = SampleVideo.SUBTITLE,
        modifier = Modifier.fillMaxSize(),
    )
}

/** A made-up episode on a [FakePlayerEngine] over [CinematicFrame]s; no network, no video. */
@Composable
private fun ScenePlayer(scene: Scene) {
    val chapters = remember { Chapters(SampleEpisode.chapters) }
    val engine = remember(scene) {
        FakePlayerEngine(
            durationMillis = SampleEpisode.durationMillis,
            positionMillis = when (scene) {
                Scene.SkipIntro -> 72_000L
                else -> 761_000L
            },
            playing = true,
            tracks = SampleEpisode.tracks,
            // A frozen playhead keeps every screenshot identical.
            advancesWithClock = false,
        )
    }
    val state = rememberTvPlayerState(
        engine = engine,
        chapters = SampleEpisode.chapters,
        skipWindows = SampleEpisode.skipWindows,
        config = TvPlayerConfig(
            autoHideMillis = null,
            showControlsOnStart = scene != Scene.SkipIntro,
            scrubCommitDelayMillis = null,
            skipRules = SkipRules(standaloneMillis = null),
        ),
    )
    // Set up the scene before the controls pick their first focus.
    LaunchedEffect(state) { setUpScene(scene, state) }

    val thumbnails = remember(chapters) {
        SeekThumbnailProvider { position -> CinematicFrame(FrameLooks.at(chapters, position), Modifier.fillMaxSize()) }
    }
    TvPlayer(
        state = state,
        title = SampleEpisode.TITLE,
        subtitle = SampleEpisode.SUBTITLE,
        modifier = Modifier.fillMaxSize(),
        thumbnails = thumbnails,
        placeholder = { CinematicFrame(FrameLooks.at(chapters, state.positionMillis), Modifier.fillMaxSize()) },
    )
}

private fun setUpScene(scene: Scene, state: TvPlayerState) {
    when (scene) {
        Scene.Scrubbing -> {
            state.requestFocus(PlayerFocusTarget.SeekBar)
            // As if Right was held from 12:41 until the preview stopped on the "Storm warning" chapter.
            state.scrubTo(SampleEpisode.chapters.first { it.title == "Storm warning" }.startMillis, autoCommit = false)
        }
        Scene.Tracks -> state.openTrackPanel()
        Scene.Live, Scene.Playing, Scene.SkipIntro -> Unit
    }
}
