package com.bluebird.messaging.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.AvatarSize
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme

private val UnreadDotSize = 9.dp
private val UnreadGutter = 14.dp
private val RowAvatarSize = AvatarSize.lg

/** Start inset for dividers between rows, so they line up with the text rather than the avatar. */
val ConversationRowDividerInset: Dp = UnreadGutter + RowAvatarSize + 12.dp

/**
 * One inbox row. Purely presentational: all text is pre-formatted by the caller so rows stay cheap to compose
 * while scrolling. [selected] highlights the open conversation on tablets.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationRow(
    title: String,
    preview: String,
    timestamp: String,
    unread: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    /** Search text to emphasize in the title and preview. */
    highlight: String? = null,
    avatar: @Composable () -> Unit,
) {
    val colors = BluebirdTheme.colors
    val typography = BluebirdTheme.typography
    val accent = colors.accent
    val titleText = remember(title, highlight, accent) { highlightMatches(title, highlight, accent) }
    val previewText = remember(preview, highlight, accent) { highlightMatches(preview, highlight, accent) }
    val interaction = remember { MutableInteractionSource() }
    val unreadColor = colors.unread
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = if (unread) "Unread" else "Read" }
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        if (selected) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accentMuted),
            )
        }
        Row(
            modifier = Modifier.padding(end = BluebirdTheme.layout.gutter, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(UnreadGutter), contentAlignment = Alignment.Center) {
                if (unread) {
                    Box(Modifier.size(UnreadDotSize).clip(CircleShape).drawBehind { drawRect(unreadColor) })
                }
            }
            Box(Modifier.size(RowAvatarSize)) { avatar() }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = titleText,
                        style = typography.headline,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (pinned) {
                        Icon(
                            BluebirdIcons.Pin,
                            contentDescription = "Pinned",
                            tint = colors.warmStrong,
                            modifier = Modifier.padding(start = 6.dp, end = 4.dp).size(14.dp),
                        )
                    }
                    Text(
                        text = timestamp,
                        style = typography.footnote,
                        color = if (unread) colors.accent else colors.textSecondary,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Text(
                    text = previewText,
                    style = typography.subheadline.copy(
                        fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = if (unread) colors.textPrimary else colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** Bolds and tints every case-insensitive occurrence of [query] in [text]. */
internal fun highlightMatches(text: String, query: String?, color: Color): AnnotatedString {
    val q = query?.trim().orEmpty()
    if (q.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var start = 0
        while (true) {
            val index = text.indexOf(q, start, ignoreCase = true)
            if (index < 0) break
            append(text.substring(start, index))
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(text.substring(index, index + q.length)) }
            start = index + q.length
        }
        append(text.substring(start))
    }
}
