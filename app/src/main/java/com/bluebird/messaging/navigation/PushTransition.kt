package com.bluebird.messaging.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry

/**
 * The iOS navigation push.
 *
 * Opening a screen slides it in over the current one, which drifts a third of the way left and dims; the moving
 * screen casts a soft shadow on its leading edge. Going back reverses it. With Android's back gesture (swipe in
 * from the screen edge) the same motion follows your finger: let go far enough and the screen leaves, otherwise it
 * settles back. Nothing moves when system animations are turned off.
 */
internal class PushTransitions(private val reduced: Boolean) {

    /** Whether the running navigation is a "back"; read while drawing the dim, set by NavHost's callbacks below. */
    private var popping = false

    /** Whether the running "back" is driven by the user's finger, so the motion tracks it linearly. */
    private var tracking = false

    private fun mark(pop: Boolean, finger: Boolean) {
        popping = pop
        tracking = finger
    }

    private fun easing(): Easing = if (tracking) LinearEasing else PushEasing

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        mark(pop = false, finger = false)
        if (reduced) EnterTransition.None else slideInHorizontally(tween(PUSH_MS, easing = PushEasing)) { it }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        mark(pop = false, finger = false)
        if (reduced) ExitTransition.None else slideOutHorizontally(tween(PUSH_MS, easing = PushEasing)) { -it / 3 }
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        mark(pop = true, finger = false)
        if (reduced) EnterTransition.None else slideInHorizontally(tween(PUSH_MS, easing = PushEasing)) { -it / 3 }
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        mark(pop = true, finger = false)
        if (reduced) ExitTransition.None else slideOutHorizontally(tween(PUSH_MS, easing = PushEasing)) { it }
    }
    val gesturePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = {
        mark(pop = true, finger = true)
        if (reduced) EnterTransition.None else slideInHorizontally(tween(PUSH_MS, easing = LinearEasing)) { -it / 3 }
    }
    val gesturePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = {
        mark(pop = true, finger = true)
        if (reduced) ExitTransition.None else slideOutHorizontally(tween(PUSH_MS, easing = LinearEasing)) { it }
    }

    /**
     * Wraps a destination so it dims while another screen covers it, and casts the edge shadow while it is the
     * screen moving on top. Both are drawn, never composed, so a transition costs no recomposition.
     */
    @Composable
    fun AnimatedContentScope.Screen(content: @Composable () -> Unit) {
        if (reduced) {
            content()
            return
        }
        // 0 at rest, 1 when fully pushed aside (or not yet arrived).
        val away = transition.animateFloat(
            transitionSpec = { tween(PUSH_MS, easing = easing()) },
            label = "push",
        ) { state -> if (state == EnterExitState.Visible) 0f else 1f }
        Box(
            Modifier.drawWithContent {
                drawContent()
                val amount = away.value
                if (amount <= 0f) return@drawWithContent
                // Arriving during a "back", or leaving during a push, means this is the screen underneath.
                val arriving = transition.targetState == EnterExitState.Visible
                if (arriving == popping) {
                    drawRect(Color.Black.copy(alpha = MAX_DIM * amount))
                } else {
                    val shadow = ShadowWidth.toPx()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = SHADOW_ALPHA),
                            startX = -shadow,
                            endX = 0f,
                        ),
                        topLeft = Offset(-shadow, 0f),
                        size = Size(shadow, size.height),
                    )
                }
            },
        ) { content() }
    }

    private companion object {
        const val PUSH_MS = 380
        const val MAX_DIM = 0.12f
        const val SHADOW_ALPHA = 0.14f
        val ShadowWidth = 14.dp

        /** Fast start and a long, soft landing: close to UIKit's navigation spring. */
        val PushEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    }
}
