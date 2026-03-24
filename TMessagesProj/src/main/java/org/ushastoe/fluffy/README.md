# org.ushastoe.fluffy

Custom FluffyGram code lives here.

Keep Telegram core edits minimal and delegate behavior to patch classes in this package tree.
Write new patch classes in Kotlin (`.kt`) by default.

## UI Menu Anchor Pattern

Use this pattern when a popup menu must appear near a specific drawn sub-element inside a Telegram cell, not near the whole cell.

Problem:
- `ItemOptions.makeOptions(fragment, cell)` anchors to the whole view.
- In `ChatMessageCell` this can place the popup too high and visually overlap the `ActionBar`.

Recommended approach:
1. Add a minimal helper in the Telegram core cell that returns the bounds of the exact drawn element.
2. Keep popup/menu logic in `org.ushastoe.fluffy.patches`.
3. In the patch, create a temporary invisible anchor view in `fragment.getLayoutContainer()`.
4. Position that anchor view using the cell's window coordinates plus the element bounds.
5. Open `ItemOptions` using that temporary anchor view.
6. Remove the temporary anchor in `setOnDismiss(...)`.

Reference implementation:
- Telegram hook point: [ChatActivity.java](/F:/Users/krol/fluffyGram/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java)
- Telegram bounds helper: [ChatMessageCell.java](/F:/Users/krol/fluffyGram/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java)
- Fluffy hook: [InlineCallbackDataHook.java](/F:/Users/krol/fluffyGram/TMessagesProj/src/main/java/org/ushastoe/fluffy/hooks/InlineCallbackDataHook.java)
- Fluffy patch: [InlineCallbackDataPatch.java](/F:/Users/krol/fluffyGram/TMessagesProj/src/main/java/org/ushastoe/fluffy/patches/InlineCallbackDataPatch.java)

Use this for:
- inline bot button menus
- custom drawn reaction/label menus
- message sub-control menus
- any popup that must spawn at the exact visual control location
