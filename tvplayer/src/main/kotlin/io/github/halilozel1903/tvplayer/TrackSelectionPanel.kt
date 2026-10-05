package io.github.halilozel1903.tvplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.halilozel1903.tvplayer.core.TrackMenu
import io.github.halilozel1903.tvplayer.core.TrackMenuItem

/**
 * The side panel with the subtitle and audio menus. Up and down move through both lists, center
 * selects a row, Back closes the panel (handled by [TvPlayerControls]). The focus starts on the
 * selected subtitle row and stays inside the panel while it is open.
 */
@Composable
public fun TrackSelectionPanel(
    state: TvPlayerState,
    modifier: Modifier = Modifier,
    labels: TvPlayerLabels = TvPlayerLabels(),
    colors: TvPlayerColors = TvPlayerDefaults.colors(),
    width: Dp = TvPlayerDefaults.PanelWidth,
) {
    val menu = remember(labels) { TrackMenu(labels.trackMenu, labels.displayLocale) }
    val tracks = state.snapshot.tracks
    val subtitles = remember(menu, tracks) { menu.subtitles(tracks) }
    val audio = remember(menu, tracks) { menu.audio(tracks) }
    val initialFocus = remember { FocusRequester() }
    val initialKey = subtitles.firstOrNull { it.isSelected }?.key

    LaunchedEffect(Unit) { initialFocus.requestFocusWhenReady() }

    Column(
        modifier
            .width(width)
            .fillMaxHeight()
            .background(colors.panel)
            // Keep the focus in the panel: nothing to the left or right of it should take it.
            .onPreviewKeyEvent { it.key == Key.DirectionLeft || it.key == Key.DirectionRight }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = TvPlayerDefaults.SafeAreaVertical + 8.dp),
    ) {
        MenuHeading(labels.subtitles, colors)
        for (item in subtitles) {
            TrackRow(
                item = item,
                colors = colors,
                onClick = { state.selectTrack(item) },
                modifier = if (item.key == initialKey) Modifier.focusRequester(initialFocus) else Modifier,
            )
        }
        if (audio.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            MenuHeading(labels.audio, colors)
            for (item in audio) {
                TrackRow(item = item, colors = colors, onClick = { state.selectTrack(item) })
            }
        }
    }
}

@Composable
private fun MenuHeading(text: String, colors: TvPlayerColors) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = colors.content,
    )
}

@Composable
private fun TrackRow(
    item: TrackMenuItem,
    colors: TvPlayerColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .semantics { selected = item.isSelected },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = colors.content,
            focusedContainerColor = colors.focusedContainer,
            focusedContentColor = colors.onFocusedContainer,
            pressedContainerColor = colors.focusedContainer.copy(alpha = 0.8f),
            pressedContentColor = colors.onFocusedContainer,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(22.dp)) {
                if (item.isSelected) {
                    PlayerIconImage(PlayerIcon.Check, tint = LocalContentColor.current, size = 22.dp)
                }
            }
            Text(
                text = item.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = LocalContentColor.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = item.detail
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current.copy(alpha = 0.65f),
                    maxLines = 1,
                )
            }
        }
    }
}
