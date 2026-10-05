package io.github.halilozel1903.tvplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.halilozel1903.tvplayer.core.SkipKind
import io.github.halilozel1903.tvplayer.core.SkipWindow

/** A round icon button of the control row. */
@Composable
internal fun RoundControlButton(
    icon: PlayerIcon,
    contentDescription: String,
    onClick: () -> Unit,
    colors: TvPlayerColors,
    modifier: Modifier = Modifier,
    canFocus: Boolean = true,
    badge: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(52.dp)
            .semantics { this.contentDescription = contentDescription }
            .focusProperties { this.canFocus = canFocus },
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.12f),
            contentColor = colors.content,
            focusedContainerColor = colors.focusedContainer,
            focusedContentColor = colors.onFocusedContainer,
            pressedContainerColor = colors.focusedContainer.copy(alpha = 0.8f),
            pressedContentColor = colors.onFocusedContainer,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
        interactionSource = interactionSource,
    ) {
        Box(Modifier.align(Alignment.Center)) {
            PlayerIconImage(icon, tint = LocalContentColor.current, size = 28.dp)
            if (badge != null) {
                Text(
                    text = badge,
                    modifier = Modifier.align(Alignment.Center).padding(top = 3.dp),
                    color = LocalContentColor.current,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** A pill button with an icon and a text, used for "Subtitles & audio". */
@Composable
internal fun PillControlButton(
    icon: PlayerIcon,
    text: String,
    onClick: () -> Unit,
    colors: TvPlayerColors,
    modifier: Modifier = Modifier,
    canFocus: Boolean = true,
    detail: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.focusProperties { this.canFocus = canFocus },
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.12f),
            contentColor = colors.content,
            focusedContainerColor = colors.focusedContainer,
            focusedContentColor = colors.onFocusedContainer,
            pressedContainerColor = colors.focusedContainer.copy(alpha = 0.8f),
            pressedContentColor = colors.onFocusedContainer,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlayerIconImage(icon, tint = LocalContentColor.current, size = 24.dp)
            Text(text = text, style = MaterialTheme.typography.titleSmall, color = LocalContentColor.current, maxLines = 1)
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The skip intro, recap or credits button. [TvPlayerControls] shows it and moves the focus to it
 * when its window starts; use it directly in your own controls.
 */
@Composable
public fun SkipButton(
    window: SkipWindow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labels: TvPlayerLabels = TvPlayerLabels(),
    colors: TvPlayerColors = TvPlayerDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.panel,
            contentColor = colors.content,
            focusedContainerColor = colors.focusedContainer,
            focusedContentColor = colors.onFocusedContainer,
            pressedContainerColor = colors.focusedContainer.copy(alpha = 0.8f),
            pressedContentColor = colors.onFocusedContainer,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(2.dp, colors.content.copy(alpha = 0.6f)),
                shape = MaterialTheme.shapes.small,
            ),
        ),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlayerIconImage(
                icon = if (window.kind == SkipKind.Credits) PlayerIcon.SkipNext else PlayerIcon.Forward,
                tint = LocalContentColor.current,
                size = 22.dp,
            )
            Text(
                text = labels.skipLabel(window),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LocalContentColor.current,
                maxLines = 1,
            )
        }
    }
}
