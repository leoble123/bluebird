package com.bluebird.messaging.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.AvatarSize
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme

/** The iOS-style monogram fill: one soft gray gradient for everyone without a photo. */
internal object AvatarPalette {
    val tones: List<Pair<Color, Color>> = listOf(
        Color(0xFFA5ABB8) to Color(0xFF858994),
    )

    /** Deterministic: the same contact always gets the same tone, on every device. */
    fun toneIndexFor(seed: String): Int = Math.floorMod(seed.hashCode(), tones.size)
}

/** Up to two initials from the first and last words that start with a letter. Empty when there are none. */
internal fun initialsFor(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.firstOrNull()?.isLetter() == true }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words.first().take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
}

@Composable
fun Avatar(
    name: String,
    seed: String,
    modifier: Modifier = Modifier,
    size: Dp = AvatarSize.md,
    /** Null when the name is already shown or read nearby, so screen readers don't hear it twice. */
    contentDescription: String? = null,
) {
    val (top, bottom) = AvatarPalette.tones[remember(seed) { AvatarPalette.toneIndexFor(seed) }]
    val initials = remember(name) { initialsFor(name) }
    val brush = remember(top, bottom) { Brush.verticalGradient(listOf(top, bottom)) }
    val fontSize = with(LocalDensity.current) { (size * 0.4f).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .drawBehind { drawRect(brush) }
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isNotEmpty()) {
            Text(
                text = initials,
                color = Color.White,
                style = BluebirdTheme.typography.headline.copy(
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        } else {
            Icon(BluebirdIcons.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/** Two overlapping avatars for group conversations, separated by a ring in the background color. */
@Composable
fun GroupAvatar(
    first: Pair<String, String>,
    second: Pair<String, String>,
    modifier: Modifier = Modifier,
    size: Dp = AvatarSize.md,
    ringColor: Color = BluebirdTheme.colors.background,
) {
    val small = size * 0.62f
    val ring = 2.dp
    Box(modifier.size(size)) {
        Avatar(first.first, first.second, Modifier.align(Alignment.TopStart), size = small)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = ring, y = ring)
                .clip(CircleShape)
                .drawBehind { drawRect(ringColor) }
                .padding(ring),
        ) {
            Avatar(second.first, second.second, size = small)
        }
    }
}
