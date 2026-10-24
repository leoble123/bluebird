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

/** Curated two-tone fills: cool, icy tones plus one warm apricot, chosen to read well with white initials. */
internal object AvatarPalette {
    val tones: List<Pair<Color, Color>> = listOf(
        Color(0xFF6F9BEF) to Color(0xFF3F6FD8), // glacier blue
        Color(0xFF5BB5C2) to Color(0xFF2F8A9C), // meltwater teal
        Color(0xFF8E8FE6) to Color(0xFF6466C9), // twilight periwinkle
        Color(0xFFF2A779) to Color(0xFFE07E45), // apricot
        Color(0xFF6CC3A0) to Color(0xFF3E9D7A), // sea glass
        Color(0xFFCB8DB8) to Color(0xFFA9629A), // dusk rose
        Color(0xFF8CA0B8) to Color(0xFF627790), // storm slate
        Color(0xFF5F86D6) to Color(0xFF2E57B5), // deep cobalt
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
            .semantics { contentDescription = name },
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
