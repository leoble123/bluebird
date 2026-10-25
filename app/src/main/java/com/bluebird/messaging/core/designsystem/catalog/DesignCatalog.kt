package com.bluebird.messaging.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bluebird.messaging.core.designsystem.component.GlassTextButton
import com.bluebird.messaging.core.designsystem.component.GroupedIconButton
import com.bluebird.messaging.core.designsystem.component.GlassButtonGroup
import com.bluebird.messaging.core.designsystem.component.GlassIconButton
import com.bluebird.messaging.core.designsystem.component.Avatar
import com.bluebird.messaging.core.designsystem.component.BackButton
import com.bluebird.messaging.core.designsystem.component.BluebirdButton
import com.bluebird.messaging.core.designsystem.component.BluebirdCard
import com.bluebird.messaging.core.designsystem.component.BluebirdComposer
import com.bluebird.messaging.core.designsystem.component.BluebirdDivider
import com.bluebird.messaging.core.designsystem.component.BluebirdIconButton
import com.bluebird.messaging.core.designsystem.component.BluebirdSearchField
import com.bluebird.messaging.core.designsystem.component.BluebirdTopBar
import com.bluebird.messaging.core.designsystem.component.BubblePosition
import com.bluebird.messaging.core.designsystem.component.BubbleRow
import com.bluebird.messaging.core.designsystem.component.ButtonVariant
import com.bluebird.messaging.core.designsystem.component.ConversationRow
import com.bluebird.messaging.core.designsystem.component.ConversationRowDividerInset
import com.bluebird.messaging.core.designsystem.component.DeliveryStatus
import com.bluebird.messaging.core.designsystem.component.EmptyState
import com.bluebird.messaging.core.designsystem.component.ErrorState
import com.bluebird.messaging.core.designsystem.component.MenuDivider
import com.bluebird.messaging.core.designsystem.component.MenuItem
import com.bluebird.messaging.core.designsystem.component.MenuSurface
import com.bluebird.messaging.core.designsystem.component.GroupAvatar
import com.bluebird.messaging.core.designsystem.component.IconBadge
import com.bluebird.messaging.core.designsystem.component.IconButtonStyle
import com.bluebird.messaging.core.designsystem.component.LoadingState
import com.bluebird.messaging.core.designsystem.component.MessageBubble
import com.bluebird.messaging.core.designsystem.component.TapbackBadges
import com.bluebird.messaging.core.designsystem.component.SectionHeader
import com.bluebird.messaging.core.designsystem.component.StatusPill
import com.bluebird.messaging.core.designsystem.component.TypingIndicator
import com.bluebird.messaging.core.designsystem.icon.BluebirdIcons
import com.bluebird.messaging.core.designsystem.theme.AvatarSize
import com.bluebird.messaging.core.designsystem.theme.BluebirdTheme
import com.bluebird.messaging.core.designsystem.theme.Radii
import com.bluebird.messaging.core.designsystem.theme.Spacing
import com.bluebird.messaging.core.designsystem.theme.GlassHost
import com.bluebird.messaging.core.designsystem.theme.glass
import com.bluebird.messaging.core.designsystem.theme.glassSource
import com.bluebird.messaging.core.model.DeliveryState

/**
 * A living catalog of the design system, built from the real components. Reachable from Settings, and
 * rendered section by section by the screenshot tests.
 */
@Composable
fun DesignCatalogScreen(onBack: () -> Unit) {
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
    GlassHost {
        Box(Modifier.fillMaxSize().background(BluebirdTheme.colors.background)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().glassSource()) {
                item { Spacer(Modifier.height(140.dp)) }
                item { CatalogSection("Glass") { GlassSample() } }
                item { CatalogSection("Color") { ColorSample() } }
                item { CatalogSection("Type") { TypeSample() } }
                item { CatalogSection("Buttons") { ButtonSample() } }
                item { CatalogSection("Conversation rows") { RowSample() } }
                item { CatalogSection("Messages") { BubbleSample() } }
                item { CatalogSection("Inputs") { InputSample() } }
                item { CatalogSection("States") { StateSample() } }
                item { Spacer(Modifier.height(48.dp)) }
            }
            BluebirdTopBar(
                title = "Design system",
                large = !scrolled,
                glass = scrolled,
                navigation = { BackButton(onBack) },
            )
        }
    }
}

@Composable
fun CatalogSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = Spacing.xxl)) {
        SectionHeader(title)
        content()
    }
}

/** Frosted glass over colorful content: a bar and a menu blurring what sits behind them. */
@Composable
fun GlassSample() {
    val colors = BluebirdTheme.colors
    GlassHost {
        // Liquid Glass floating controls over the same content.
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(300.dp)
                .clip(Radii.card)
                .background(colors.surfaceSunken),
        ) {
            // Colorful content underneath, standing in for photos and bubbles scrolling by.
            Box(Modifier.fillMaxSize().glassSource()) {
                Box(Modifier.offset(20.dp, 10.dp).size(120.dp).clip(CircleShape).background(colors.accent))
                Box(Modifier.offset(170.dp, 30.dp).size(100.dp).clip(CircleShape).background(colors.warm))
                Box(Modifier.offset(90.dp, 150.dp).size(140.dp).clip(CircleShape).background(colors.success))
                Text(
                    "Messages scroll beneath glass ".repeat(10),
                    style = BluebirdTheme.typography.title3,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().height(56.dp).glass().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Glass bar", style = BluebirdTheme.typography.headline, color = colors.textPrimary, modifier = Modifier.weight(1f))
                StatusPill("Simulated")
            }
            Row(
                Modifier.align(Alignment.BottomStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(BluebirdIcons.Back, "Back", onClick = {})
                GlassButtonGroup {
                    GroupedIconButton(BluebirdIcons.Filter, "Filter") {}
                    GroupedIconButton(BluebirdIcons.More, "More") {}
                }
            }
            Row(
                Modifier.align(Alignment.CenterStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassTextButton("Cancel", onClick = {})
                GlassIconButton(BluebirdIcons.Compose, "New message", onClick = {}, prominent = true)
            }
            MenuSurface(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                MenuItem("Reply", onClick = {}, icon = BluebirdIcons.Reply)
                MenuItem("Copy", onClick = {}, icon = BluebirdIcons.Copy)
                MenuDivider()
                MenuItem("Delete", onClick = {}, icon = BluebirdIcons.Trash, destructive = true)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSample() {
    val c = BluebirdTheme.colors
    val swatches = listOf(
        "Accent" to c.accent, "Accent muted" to c.accentMuted, "Warm" to c.warm, "Warm muted" to c.warmMuted,
        "Background" to c.background, "Raised" to c.surfaceRaised, "Sunken" to c.surfaceSunken,
        "Incoming" to c.bubbleIncoming, "Success" to c.success, "Danger" to c.danger,
    )
    FlowRow(
        Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        swatches.forEach { (name, color) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(color)
                        .then(if (color == c.background || color == c.surfaceRaised) Modifier.border(1.dp, c.separator, RoundedCornerShape(14.dp)) else Modifier),
                )
                Text(name, style = BluebirdTheme.typography.caption2, color = c.textSecondary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun TypeSample() {
    val t = BluebirdTheme.typography
    val c = BluebirdTheme.colors.textPrimary
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Messages", style = t.largeTitle, color = c)
        Text("Title 2 · Contact name", style = t.title2, color = c)
        Text("Headline · Maya Chen", style = t.headline, color = c)
        Text("Body · The quick brown fox jumps over the lazy dog.", style = t.body, color = c)
        Text("Subheadline · Latest message preview", style = t.subheadline, color = BluebirdTheme.colors.textSecondary)
        Text("Footnote · 9:41 AM", style = t.footnote, color = BluebirdTheme.colors.textSecondary)
        Text("CAPTION · DELIVERED", style = t.caption, color = BluebirdTheme.colors.textTertiary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonSample() {
    val colors = BluebirdTheme.colors
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BluebirdButton("New message", onClick = {}, leadingIcon = BluebirdIcons.Compose)
            BluebirdButton("Tonal", onClick = {}, variant = ButtonVariant.Tonal)
            BluebirdButton("Secondary", onClick = {}, variant = ButtonVariant.Secondary)
            BluebirdButton("Plain", onClick = {}, variant = ButtonVariant.Plain)
            BluebirdButton("Delete", onClick = {}, variant = ButtonVariant.Destructive)
            BluebirdButton("Disabled", onClick = {}, enabled = false)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BluebirdIconButton(BluebirdIcons.Compose, "New message", onClick = {})
            BluebirdIconButton(BluebirdIcons.Search, "Search", onClick = {}, style = IconButtonStyle.Gray)
            BluebirdIconButton(BluebirdIcons.Plus, "Add", onClick = {}, style = IconButtonStyle.Tonal)
            BluebirdIconButton(BluebirdIcons.Send, "Send", onClick = {}, style = IconButtonStyle.Filled)
            BluebirdIconButton(BluebirdIcons.Phone, "Call (future)", onClick = {}, enabled = false)
            BluebirdIconButton(BluebirdIcons.Video, "Video (future)", onClick = {}, enabled = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBadge(BluebirdIcons.Moon, colors.accent)
            IconBadge(BluebirdIcons.Bell, colors.danger)
            IconBadge(BluebirdIcons.Lock, Color(0xFF627790))
            IconBadge(BluebirdIcons.Bubble, colors.success)
            IconBadge(BluebirdIcons.Sparkle, colors.warm)
        }
    }
}

@Composable
fun RowSample() {
    Column {
        ConversationRow(
            title = "Maya Chen", preview = "Perfect. I'll grab a table 🙌", timestamp = "9:41 AM",
            unread = true, pinned = true, onClick = {},
            avatar = { Avatar("Maya Chen", "c1", size = AvatarSize.lg) },
        )
        BluebirdDivider(startInset = ConversationRowDividerInset)
        ConversationRow(
            title = "Sam Okafor, Lena Fischer", preview = "I can do chips and salsa", timestamp = "Yesterday",
            unread = false, selected = true, onClick = {},
            avatar = { GroupAvatar("Sam Okafor" to "c4", "Lena Fischer" to "c5", size = AvatarSize.lg) },
        )
        BluebirdDivider(startInset = ConversationRowDividerInset)
        ConversationRow(
            title = "+1 555 0199", preview = "A very long preview that keeps going so we can see how the row handles two full lines of text before truncating",
            timestamp = "Tuesday", unread = false, onClick = {},
            avatar = { Avatar("+1 555 0199", "c9", size = AvatarSize.lg) },
        )
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(AvatarSize.xs, AvatarSize.sm, AvatarSize.md, AvatarSize.lg, AvatarSize.xl).forEachIndexed { i, s ->
                Avatar(listOf("Priya Raman", "Diego Alvarez", "Jordan Ellis", "Lena Fischer", "Maya Chen")[i], "c$i", size = s)
            }
        }
    }
}

@Composable
fun BubbleSample() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.bubbleGroupGap)) {
        BubbleRow(outgoing = false) { MessageBubble("Are we still on for dinner tonight?", outgoing = false, position = BubblePosition.First) }
        BubbleRow(outgoing = false) { MessageBubble("Thinking 7:30", outgoing = false, position = BubblePosition.Middle) }
        BubbleRow(outgoing = false) {
            Box(Modifier.padding(top = 14.dp)) {
                MessageBubble("Usual place?", outgoing = false, position = BubblePosition.Last)
                TapbackBadges(listOf("❤️" to true, "😂" to false), Modifier.align(Alignment.TopEnd).offset(x = 30.dp, y = (-16).dp))
            }
        }
        Spacer(Modifier.height(Spacing.bubbleGroupBreak))
        BubbleRow(outgoing = true) { MessageBubble("Yes! See you there", outgoing = true, position = BubblePosition.First) }
        BubbleRow(outgoing = true) {
            MessageBubble(
                "I'll text when I'm leaving. Traffic looks a little heavy downtown so maybe 7:40 to be safe",
                outgoing = true,
                position = BubblePosition.Last,
            )
        }
        BubbleRow(outgoing = true) {
            DeliveryStatus(DeliveryState.Read, "9:41 AM", Modifier.padding(top = 2.dp, end = 4.dp))
        }
        Spacer(Modifier.height(Spacing.bubbleGroupBreak))
        BubbleRow(outgoing = false) { MessageBubble("🎉🎉", outgoing = false) }
        BubbleRow(outgoing = true) { MessageBubble("Selected message", outgoing = true, selected = true) }
        BubbleRow(outgoing = true) { DeliveryStatus(DeliveryState.Failed, null, Modifier.padding(end = 4.dp)) }
        Spacer(Modifier.height(Spacing.bubbleGroupBreak))
        BubbleRow(outgoing = false) { TypingIndicator() }
    }
}

@Composable
fun InputSample() {
    val search = rememberTextFieldState()
    val draft = rememberTextFieldState("Sounds good — ")
    val empty = rememberTextFieldState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BluebirdSearchField(search, Modifier.padding(horizontal = 16.dp))
        BluebirdComposer(state = empty, onSend = {}, onAttach = {}, onEmoji = {})
        BluebirdComposer(state = draft, onSend = {}, onAttach = {})
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("Simulated")
            StatusPill(
                "Connected",
                dotColor = BluebirdTheme.colors.success,
                container = BluebirdTheme.colors.success.copy(alpha = 0.12f),
                content = BluebirdTheme.colors.success,
            )
        }
    }
}

@Composable
fun StateSample() {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BluebirdCard(Modifier.fillMaxWidth().height(300.dp)) {
            EmptyState(
                icon = BluebirdIcons.Bubble,
                title = "No conversations yet",
                message = "Start a conversation and it will appear here.",
                actionLabel = "New message",
                onAction = {},
            )
        }
        BluebirdCard(Modifier.fillMaxWidth().height(280.dp)) {
            ErrorState(message = "Couldn't load messages from local storage.", onRetry = {})
        }
        BluebirdCard(Modifier.fillMaxWidth()) { LoadingState(rows = 3) }
    }
}
