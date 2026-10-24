package com.bluebird.messaging.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Width buckets matching Android's window size class breakpoints. */
enum class WindowClass { Compact, Medium, Expanded }

/**
 * Adaptive layout tokens. Derived from the current window width, so split-screen and resizable windows
 * get the right values rather than assuming one fixed tablet size.
 */
@Immutable
data class BluebirdLayout(
    val windowClass: WindowClass,
    /** Horizontal screen padding. */
    val gutter: Dp,
    /** Maximum width for reading content (message timeline, settings) so lines never stretch across a tablet. */
    val readableWidth: Dp,
    /** Width of the conversation list pane when two panes are shown. */
    val listPaneWidth: Dp,
    /** Bubbles never exceed this fraction of the timeline width, nor [bubbleMaxWidth]. */
    val bubbleMaxWidthFraction: Float,
    val bubbleMaxWidth: Dp,
    val twoPane: Boolean,
) {
    companion object {
        fun forWidth(widthDp: Int): BluebirdLayout = when {
            widthDp < 600 -> BluebirdLayout(
                windowClass = WindowClass.Compact,
                gutter = 16.dp,
                readableWidth = Dp.Unspecified,
                listPaneWidth = Dp.Unspecified,
                bubbleMaxWidthFraction = 0.78f,
                bubbleMaxWidth = 480.dp,
                twoPane = false,
            )
            widthDp < 840 -> BluebirdLayout(
                windowClass = WindowClass.Medium,
                gutter = 24.dp,
                readableWidth = 640.dp,
                listPaneWidth = 320.dp,
                bubbleMaxWidthFraction = 0.7f,
                bubbleMaxWidth = 480.dp,
                twoPane = false,
            )
            else -> BluebirdLayout(
                windowClass = WindowClass.Expanded,
                gutter = 24.dp,
                readableWidth = 720.dp,
                listPaneWidth = if (widthDp >= 1200) 400.dp else 360.dp,
                bubbleMaxWidthFraction = 0.66f,
                bubbleMaxWidth = 520.dp,
                twoPane = true,
            )
        }
    }
}
