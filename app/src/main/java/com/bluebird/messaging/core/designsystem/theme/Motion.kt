package com.bluebird.messaging.core.designsystem.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Motion conventions. Everything is short, spring-based, and interruptible so animation never delays input.
 * When the system has animations turned off, every spec collapses to an instant snap.
 */
@Immutable
data class BluebirdMotion(val reduced: Boolean) {

    /** Press feedback, toggles, small state changes. */
    fun <T> snappy(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessHigh)

    /** Size and position changes, panels, list items appearing. */
    fun <T> gentle(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)

    /** The "settle": a single soft overshoot for things arriving (send button, new reaction). */
    fun <T> settle(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.55f, stiffness = 700f)

    /** Opacity fades. */
    fun <T> fade(durationMillis: Int = Durations.STANDARD): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(durationMillis)

    /** Whether looping indicators (typing dots) may animate; when false, callers render a static state. */
    val loops: Boolean get() = !reduced

    object Durations {
        const val QUICK = 120
        const val STANDARD = 220
        const val EMPHASIZED = 320
    }

    companion object {
        fun fromSystem(context: Context): BluebirdMotion {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
            return BluebirdMotion(reduced = scale == 0f)
        }
    }
}
