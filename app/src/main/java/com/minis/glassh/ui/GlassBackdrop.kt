package com.minis.glassh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Iridescent gradient + soft blurred color blobs that mimic the iOS 17/18
 * "liquid glass" backdrop. All [GlassSurface] panels float above this.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val isDark = !MaterialTheme.colorScheme.background.isLight()
    Box(modifier = modifier.fillMaxSize()) {
        // Base diagonal iridescent wash
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (isDark) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF12101F),
                                Color(0xFF1B1535),
                                Color(0xFF18223A),
                                Color(0xFF231A2E),
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFEFE3FF),
                                Color(0xFFE0EFFF),
                                Color(0xFFFFE4F0),
                                Color(0xFFFFEED2),
                            )
                        )
                    }
                )
        )
        val blobs = if (isDark) {
            listOf(
                Triple(Color(0xFF7B5BFF), 360, Pair(-80, -60)),
                Triple(Color(0xFF5BA8FF), 320, Pair(180, 60)),
                Triple(Color(0xFFFF7BB3), 280, Pair(40, 380)),
                Triple(Color(0xFFFFC857), 240, Pair(220, 540)),
            )
        } else {
            listOf(
                Triple(Color(0xFFB89CFF), 360, Pair(-80, -60)),
                Triple(Color(0xFF8AC9FF), 320, Pair(180, 60)),
                Triple(Color(0xFFFFA6CB), 280, Pair(40, 380)),
                Triple(Color(0xFFFFD68A), 240, Pair(220, 540)),
            )
        }
        blobs.forEach { (color, size, offset) ->
            Box(
                Modifier
                    .offset(x = offset.first.dp, y = offset.second.dp)
                    .size(size.dp)
                    .blur(110.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (isDark) 0.55f else 0.65f))
            )
        }
        content()
    }
}

private fun Color.isLight(): Boolean {
    val luminance = (0.2126f * red + 0.7152f * green + 0.0722f * blue)
    return luminance > 0.5f
}
