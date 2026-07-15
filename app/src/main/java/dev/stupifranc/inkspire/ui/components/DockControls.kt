package dev.stupifranc.inkspire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants

/** Circular icon button shared by the floating tool dock and the history pill. */
@Composable
internal fun DockIconButton(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable (tint: Color) -> Unit,
) {
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    val view = LocalView.current
    val hapticOnClick = {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onClick()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = hapticOnClick),
    ) {
        icon(tint)
    }
}

@Composable
internal fun DockDivider() {
    Spacer(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(width = 1.dp, height = 24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
