package com.minis.glassh.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Reusable glass-pane container. Renders a translucent white tint with a
 * top-edge "highlight" gradient and a subtle 1dp gradient border, mirroring
 * the look of layered frosted glass on iOS 18 panels.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tintAlpha: Float = 0.18f,
    content: @Composable () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tint = if (isDark) Color.White.copy(alpha = tintAlpha * 0.7f)
    else Color.White.copy(alpha = tintAlpha + 0.15f)
    val highlight = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (isDark) 0.18f else 0.55f),
        0.4f to Color.Transparent,
    )
    val borderBrush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.45f else 0.85f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.15f),
        )
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(tint, shape)
            .background(highlight, shape)
            .border(BorderStroke(1.dp, borderBrush), shape)
    ) {
        content()
    }
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
