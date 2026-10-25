package com.bluebird.messaging.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp grid. */
@Immutable
object Spacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp

    /** Vertical gap between bubbles in the same group vs. between groups. */
    val bubbleGroupGap: Dp = 2.dp
    val bubbleGroupBreak: Dp = 10.dp
}

@Immutable
object Radii {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 14.dp
    val lg: Dp = 20.dp
    val xl: Dp = 28.dp

    val bubble: Dp = 20.dp
    /** Corner on the side where bubbles in a group touch. */
    val bubbleJoined: Dp = 6.dp
    /** The facet: the tapered outer corner on the last bubble of a group. */
    val bubbleFacet: Dp = 3.dp

    val pill = RoundedCornerShape(percent = 50)
    val card = RoundedCornerShape(lg)
    val menu = RoundedCornerShape(md)
}

/**
 * Depth comes from tonal layers (see surface roles in [BluebirdColors]) and glass, not heavy drop shadows.
 * These sizes exist for components that genuinely float above content.
 */
@Immutable
object Elevation {
    val none: Dp = 0.dp
    val hairline: Dp = 1.dp
    val floating: Dp = 2.dp
}

@Immutable
object AvatarSize {
    val xs: Dp = 24.dp
    val sm: Dp = 32.dp
    val md: Dp = 40.dp
    val lg: Dp = 52.dp
    val xl: Dp = 80.dp
    /** Contact cards and a future full-screen call view. */
    val hero: Dp = 120.dp
}

@Immutable
object IconSize {
    val sm: Dp = 16.dp
    val md: Dp = 22.dp
    val lg: Dp = 28.dp
}

/** Minimum touch target, per Android accessibility guidance. */
val MinTouchTarget: Dp = 48.dp
