package com.bluebird.messaging.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * Bluebird's own icon set: rounded 1.9-unit strokes on a 24-unit grid. Drawn in-house so the app doesn't
 * carry stock Material iconography, and so no icon library has to load. Tint them with Icon's `tint`.
 */
object BluebirdIcons {
    val Back by lazy { icon("Back") { line(2.2f) { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) } } }
    val ChevronRight by lazy { icon("ChevronRight") { line(2f) { moveTo(9.5f, 6f); lineTo(15.5f, 12f); lineTo(9.5f, 18f) } } }
    val ChevronDown by lazy { icon("ChevronDown") { line(2.2f) { moveTo(6f, 9.5f); lineTo(12f, 15.5f); lineTo(18f, 9.5f) } } }
    /** Three lines of decreasing length, the iOS filter glyph. */
    val Filter by lazy { icon("Filter") { line(2f) { moveTo(4.5f, 7f); horizontalLineTo(19.5f); moveTo(7.5f, 12f); horizontalLineTo(16.5f); moveTo(10.5f, 17f); horizontalLineTo(13.5f) } } }
    val More by lazy { icon("More") { solid { circle(5.5f, 12f, 1.6f) }; solid { circle(12f, 12f, 1.6f) }; solid { circle(18.5f, 12f, 1.6f) } } }
    val Send by lazy {
        icon("Send") {
            line(2.3f) {
                moveTo(12f, 18.5f); verticalLineTo(5.5f)
                moveTo(6.5f, 11f); lineTo(12f, 5.5f); lineTo(17.5f, 11f)
            }
        }
    }
    val Plus by lazy { icon("Plus") { line(2.1f) { moveTo(12f, 5f); verticalLineTo(19f); moveTo(5f, 12f); horizontalLineTo(19f) } } }
    val Close by lazy { icon("Close") { line(2f) { moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f); moveTo(17.5f, 6.5f); lineTo(6.5f, 17.5f) } } }
    val Check by lazy { icon("Check") { line(2.1f) { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) } } }
    val Search by lazy {
        icon("Search") {
            line { circle(11f, 11f, 6.25f) }
            line(2.1f) { moveTo(15.8f, 15.8f); lineTo(20f, 20f) }
        }
    }
    val Compose by lazy {
        icon("Compose") {
            line {
                moveTo(11f, 4.5f); horizontalLineTo(7f)
                arcTo(2.5f, 2.5f, 0f, false, false, 4.5f, 7f)
                verticalLineTo(17f)
                arcTo(2.5f, 2.5f, 0f, false, false, 7f, 19.5f)
                horizontalLineTo(17f)
                arcTo(2.5f, 2.5f, 0f, false, false, 19.5f, 17f)
                verticalLineTo(13f)
            }
            line {
                moveTo(17.6f, 4.1f)
                arcTo(2.05f, 2.05f, 0f, false, true, 20.5f, 7f)
                lineTo(13f, 14.5f); lineTo(9.5f, 15.5f); lineTo(10.5f, 12f); close()
            }
        }
    }
    val Pin by lazy {
        icon("Pin") {
            line {
                moveTo(9f, 4f); horizontalLineTo(15f)
                moveTo(10f, 4f); verticalLineTo(9.5f); lineTo(7f, 13.5f); horizontalLineTo(17f); lineTo(14f, 9.5f); verticalLineTo(4f)
                moveTo(12f, 13.5f); verticalLineTo(20f)
            }
        }
    }
    val Settings by lazy {
        icon("Settings") {
            line {
                moveTo(4f, 7.5f); horizontalLineTo(12.5f); moveTo(17.5f, 7.5f); horizontalLineTo(20f)
                moveTo(4f, 16.5f); horizontalLineTo(6.5f); moveTo(11.5f, 16.5f); horizontalLineTo(20f)
            }
            line { circle(15f, 7.5f, 2.5f) }
            line { circle(9f, 16.5f, 2.5f) }
        }
    }
    val Person by lazy {
        icon("Person") {
            line { circle(12f, 8.5f, 3.75f) }
            line { moveTo(4.5f, 20f); curveTo(5f, 16.4f, 8f, 14.5f, 12f, 14.5f); curveTo(16f, 14.5f, 19f, 16.4f, 19.5f, 20f) }
        }
    }
    val People by lazy {
        icon("People") {
            line { circle(9f, 9f, 3.25f) }
            line { moveTo(3f, 19.5f); curveTo(3.4f, 16.2f, 5.8f, 14.5f, 9f, 14.5f); curveTo(12.2f, 14.5f, 14.6f, 16.2f, 15f, 19.5f) }
            line { moveTo(15f, 5.9f); arcTo(3.1f, 3.1f, 0f, false, true, 15.6f, 12f); moveTo(17.2f, 14.8f); curveTo(19.4f, 15.5f, 20.7f, 17.2f, 21f, 19.5f) }
        }
    }
    val Photo by lazy {
        icon("Photo") {
            line { roundRect(3.5f, 5f, 20.5f, 19f, 2.5f) }
            line { circle(8.75f, 10f, 1.6f) }
            line { moveTo(20.5f, 15f); lineTo(15.5f, 10.5f); lineTo(6.5f, 19f) }
        }
    }
    val Camera by lazy {
        icon("Camera") {
            line {
                moveTo(4f, 8.5f); arcTo(2f, 2f, 0f, false, true, 6f, 6.5f); horizontalLineTo(8f); lineTo(9.5f, 4.5f)
                horizontalLineTo(14.5f); lineTo(16f, 6.5f); horizontalLineTo(18f); arcTo(2f, 2f, 0f, false, true, 20f, 8.5f)
                verticalLineTo(17f); arcTo(2f, 2f, 0f, false, true, 18f, 19f); horizontalLineTo(6f)
                arcTo(2f, 2f, 0f, false, true, 4f, 17f); close()
            }
            line { circle(12f, 12.75f, 3.4f) }
        }
    }
    val Smile by lazy {
        icon("Smile") {
            line { circle(12f, 12f, 8.75f) }
            line { moveTo(8.5f, 14.25f); curveTo(9.4f, 15.7f, 10.6f, 16.4f, 12f, 16.4f); curveTo(13.4f, 16.4f, 14.6f, 15.7f, 15.5f, 14.25f) }
            solid { circle(9.25f, 10f, 1.15f) }
            solid { circle(14.75f, 10f, 1.15f) }
        }
    }
    val Info by lazy {
        icon("Info") {
            line { circle(12f, 12f, 8.75f) }
            line(2.1f) { moveTo(12f, 11f); verticalLineTo(16.5f) }
            solid { circle(12f, 7.9f, 1.2f) }
        }
    }
    val Warning by lazy {
        icon("Warning") {
            line { moveTo(10.3f, 5f); lineTo(3.6f, 17.3f); curveTo(2.9f, 18.6f, 3.8f, 20f, 5.2f, 20f); horizontalLineTo(18.8f); curveTo(20.2f, 20f, 21.1f, 18.6f, 20.4f, 17.3f); lineTo(13.7f, 5f); curveTo(12.9f, 3.7f, 11.1f, 3.7f, 10.3f, 5f); close() }
            line(2.1f) { moveTo(12f, 9.5f); verticalLineTo(13.5f) }
            solid { circle(12f, 16.6f, 1.15f) }
        }
    }
    val Bubble by lazy {
        icon("Bubble") {
            line {
                moveTo(12f, 4.25f); curveTo(16.9f, 4.25f, 20.75f, 7.6f, 20.75f, 11.6f); curveTo(20.75f, 15.6f, 16.9f, 18.9f, 12f, 18.9f)
                curveTo(10.9f, 18.9f, 9.9f, 18.75f, 8.95f, 18.45f); lineTo(4.6f, 19.9f); lineTo(5.7f, 16.4f)
                curveTo(4.1f, 15.1f, 3.25f, 13.4f, 3.25f, 11.6f); curveTo(3.25f, 7.6f, 7.1f, 4.25f, 12f, 4.25f); close()
            }
        }
    }
    val Phone by lazy {
        icon("Phone") {
            line {
                moveTo(6.6f, 3.75f); lineTo(9.1f, 4.1f); lineTo(10.4f, 7.9f); lineTo(8.7f, 9.4f)
                curveTo(9.8f, 11.7f, 12.3f, 14.2f, 14.6f, 15.3f); lineTo(16.1f, 13.6f); lineTo(19.9f, 14.9f); lineTo(20.25f, 17.4f)
                curveTo(20.25f, 19f, 19f, 20.25f, 17.4f, 20.25f); curveTo(10.3f, 19.9f, 4.1f, 13.7f, 3.75f, 6.6f)
                curveTo(3.75f, 5f, 5f, 3.75f, 6.6f, 3.75f); close()
            }
        }
    }
    val Video by lazy {
        icon("Video") {
            line { roundRect(3f, 6f, 15f, 18f, 2.5f) }
            line { moveTo(15f, 10.5f); lineTo(20.5f, 7.25f); verticalLineTo(16.75f); lineTo(15f, 13.5f) }
        }
    }
    val Trash by lazy {
        icon("Trash") {
            line {
                moveTo(4.5f, 7f); horizontalLineTo(19.5f)
                moveTo(9.5f, 7f); verticalLineTo(4.75f); horizontalLineTo(14.5f); verticalLineTo(7f)
                moveTo(6.5f, 7f); lineTo(7.4f, 19.25f); horizontalLineTo(16.6f); lineTo(17.5f, 7f)
                moveTo(10.25f, 11f); verticalLineTo(15.5f); moveTo(13.75f, 11f); verticalLineTo(15.5f)
            }
        }
    }
    val Copy by lazy {
        icon("Copy") {
            line { roundRect(8.5f, 8.5f, 20f, 20f, 2.25f) }
            line {
                moveTo(15.5f, 8.5f); verticalLineTo(6.25f); arcTo(2.25f, 2.25f, 0f, false, false, 13.25f, 4f)
                horizontalLineTo(6.25f); arcTo(2.25f, 2.25f, 0f, false, false, 4f, 6.25f); verticalLineTo(13.25f)
                arcTo(2.25f, 2.25f, 0f, false, false, 6.25f, 15.5f); horizontalLineTo(8.5f)
            }
        }
    }
    val Reply by lazy {
        icon("Reply") {
            line { moveTo(10f, 7.5f); lineTo(5f, 12f); lineTo(10f, 16.5f) }
            line { moveTo(5.5f, 12f); horizontalLineTo(13.5f); arcTo(6f, 6f, 0f, false, true, 19.5f, 18f) }
        }
    }
    val Moon by lazy {
        icon("Moon") {
            line {
                moveTo(19.5f, 14.2f); arcTo(7.75f, 7.75f, 0f, true, true, 9.8f, 4.5f)
                arcTo(6.25f, 6.25f, 0f, false, false, 19.5f, 14.2f); close()
            }
        }
    }
    val Bell by lazy {
        icon("Bell") {
            line {
                moveTo(6f, 16.5f); verticalLineTo(10.5f); arcTo(6f, 6f, 0f, false, true, 18f, 10.5f); verticalLineTo(16.5f)
                lineTo(19.5f, 18f); horizontalLineTo(4.5f); close()
            }
            line { moveTo(10f, 20.25f); horizontalLineTo(14f) }
        }
    }
    val Lock by lazy {
        icon("Lock") {
            line { roundRect(5f, 10.5f, 19f, 20f, 2.5f) }
            line { moveTo(8f, 10.5f); verticalLineTo(8f); arcTo(4f, 4f, 0f, false, true, 16f, 8f); verticalLineTo(10.5f) }
        }
    }
    /** Small four-point sparkle, used sparingly as a decorative detail. */
    val Sparkle by lazy {
        icon("Sparkle") {
            solid {
                moveTo(12f, 2.5f); curveTo(12.5f, 7.9f, 16.1f, 11.5f, 21.5f, 12f); curveTo(16.1f, 12.5f, 12.5f, 16.1f, 12f, 21.5f)
                curveTo(11.5f, 16.1f, 7.9f, 12.5f, 2.5f, 12f); curveTo(7.9f, 11.5f, 11.5f, 7.9f, 12f, 2.5f); close()
            }
        }
    }
}

private inline fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = "Bluebird.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private inline fun ImageVector.Builder.line(width: Float = 1.9f, block: PathBuilder.() -> Unit) {
    addPath(
        pathData = PathBuilder().apply(block).nodes,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}

private inline fun ImageVector.Builder.solid(block: PathBuilder.() -> Unit) {
    addPath(pathData = PathBuilder().apply(block).nodes, fill = SolidColor(Color.Black))
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, true, true, r * 2, 0f)
    arcToRelative(r, r, 0f, true, true, -r * 2, 0f)
    close()
}

private fun PathBuilder.roundRect(left: Float, top: Float, right: Float, bottom: Float, r: Float) {
    moveTo(left + r, top)
    horizontalLineTo(right - r)
    arcTo(r, r, 0f, false, true, right, top + r)
    verticalLineTo(bottom - r)
    arcTo(r, r, 0f, false, true, right - r, bottom)
    horizontalLineTo(left + r)
    arcTo(r, r, 0f, false, true, left, bottom - r)
    verticalLineTo(top + r)
    arcTo(r, r, 0f, false, true, left + r, top)
    close()
}
