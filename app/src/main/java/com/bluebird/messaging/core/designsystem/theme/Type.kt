package com.bluebird.messaging.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Type scale modelled on the rhythm of iOS Messages: bold, tightly tracked titles and a 17sp reading size.
 * Uses the system font so nothing extra has to load.
 */
@Immutable
data class BluebirdTypography(
    val largeTitle: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subheadline: TextStyle,
    val footnote: TextStyle,
    val caption: TextStyle,
    val caption2: TextStyle,
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
)

val DefaultBluebirdTypography = BluebirdTypography(
    largeTitle = style(34, 41, FontWeight.Bold, -0.022),
    title1 = style(28, 34, FontWeight.Bold, -0.018),
    title2 = style(22, 28, FontWeight.SemiBold, -0.014),
    title3 = style(20, 25, FontWeight.SemiBold, -0.012),
    headline = style(17, 22, FontWeight.SemiBold, -0.01),
    body = style(17, 22, FontWeight.Normal, -0.01),
    callout = style(16, 21, FontWeight.Normal, -0.008),
    subheadline = style(15, 20, FontWeight.Normal, -0.006),
    footnote = style(13, 18, FontWeight.Normal, 0.0),
    caption = style(12, 16, FontWeight.Medium, 0.0),
    caption2 = style(11, 13, FontWeight.Medium, 0.01),
)

/** So stock Material components (dialogs, menus, text fields) pick up the same scale. */
internal fun BluebirdTypography.toMaterial() = Typography(
    displayLarge = largeTitle,
    displayMedium = largeTitle,
    displaySmall = title1,
    headlineLarge = title1,
    headlineMedium = title2,
    headlineSmall = title3,
    titleLarge = title2,
    titleMedium = headline,
    titleSmall = subheadline.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = body,
    bodyMedium = subheadline,
    bodySmall = footnote,
    labelLarge = subheadline.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = caption,
    labelSmall = caption2,
)
