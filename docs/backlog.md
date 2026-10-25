# Backlog

## Scheduled for Prompt 6 (advanced messaging interactions)

Approved on 2026-10-25: build all of these during Prompt 6, alongside that prompt's own requirements. They are
features Apple's Messages doesn't offer. All of them work with mock data and local storage (Prompt 5).

### Staying organized
- [ ] Custom folders/labels (e.g. Work, Family, Groups), available as extra choices in the inbox filter menu.
- [ ] Snooze a conversation until a chosen time; it returns marked unread.
- [ ] Mute for a set time (1 hour, 8 hours, until tomorrow, always).
- [ ] "Remind me about this message" from the long-press menu (local notification).
- [ ] Starred/saved messages, with one list across all conversations.
- [ ] Draft badge in the inbox: rows with unsent text show "Draft: …" in place of the preview.

### Inside a conversation
- [ ] "N new messages" pill that jumps to the first unread message when opening a busy conversation.
- [ ] Pin a message to the top of a conversation.
- [ ] Quote part of a message: select some text and reply to just that.
- [ ] Jump to date in long threads.
- [ ] One-tap "Copy code" chip on verification-code messages.
- [ ] Private notes on a conversation, visible only to you.

### Personalization and accessibility
- [ ] Bubble color per conversation.
- [ ] Compact density mode.
- [ ] High-contrast bubbles (Apple's accessible blue, #1E6EF4 light, #5CB8FF dark, from the HIG).
- [ ] One-handed mode: pull top controls down within thumb reach.

### Android-only
- [ ] Android conversation bubbles (floating chat heads).
- [ ] Home-screen conversation widgets.
- [ ] Pop a conversation out into its own window; drag text and messages between panes on tablets.
- [ ] Predictive back gesture.

## Delivery plan (Prompt 6 + iOS features + this backlog)

Each batch ends with all tests passing and a fresh preview APK.

1. **Motion and smoothness** (done): iOS push transitions with gesture-driven back, gradient scroll edges,
   screen state built off the main thread.
2. **Media (Prompt 6 core):** Android Photo Picker for photos and videos, composer previews with remove, copies
   kept in app storage, thumbnails, graceful "missing file" placeholders, forwarding.
3. **iOS conversation features:** emoji tapbacks beyond the six, edit and unsend (with the iOS time limits),
   inline reply threads, message effects (simulated), "Delivered quietly"-style labels, link previews (offline,
   from the URL only), copy-code chip, partial quote, pinned messages, jump to date, "N new messages" jump.
4. **Organization:** folders/labels, snooze, timed mute, reminders, starred messages, draft badge, private notes.
5. **Personalization and Android extras:** per-chat bubble color, compact density, high-contrast bubbles,
   one-handed mode, conversation bubbles, widgets, pop-out windows.
