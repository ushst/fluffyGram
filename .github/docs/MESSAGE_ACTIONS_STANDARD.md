# Message Actions & Interaction Functions Standard

## Overview
Any additional functionality added to the message context menu (interaction menu) **must** include a toggle to enable/disable it in Fluffy Settings.

This ensures:
1. **User control**: Users can opt-out of features they don't need.
2. **Consistency**: All message actions follow the same control pattern.
3. **Maintainability**: Settings are centralized and predictable.

## Architecture

### 1. Define the Feature Flag
Create a hook method in `org.ushastoe.fluffy.hooks.MessageActionsHook`:

```java
public static boolean isMyFeatureEnabled() {
    return MessageActionsPatch.isMyFeatureEnabled();
}

public static void setMyFeatureEnabled(boolean enabled) {
    MessageActionsPatch.setMyFeatureEnabled(enabled);
}
```

### 2. Implement Storage in Patch
In `org.ushastoe.fluffy.patches.MessageActionsPatch`:

```java
public static boolean isMyFeatureEnabled() {
    return MessagesController.getInstance(0)
        .getAppPreferences()
        .getBoolean("my_feature_enabled", true); // default: enabled
}

public static void setMyFeatureEnabled(boolean enabled) {
    MessagesController.getInstance(0).getAppPreferences()
        .edit()
        .putBoolean("my_feature_enabled", enabled)
        .apply();
}
```

**Naming convention**: `feature_name_enabled` (lowercase with underscores).

### 3. Add UI Control in FluffyAppearanceActivity or MessageActionsActivity
In `FluffyAppearanceActivity` or create a dedicated `FluffyMessageActionsActivity`:

```java
// Add a checkbox cell for the feature
items.add(new ItemInner(VIEW_TYPE_TEXT_CHECK, ROW_MY_FEATURE, 
    LocaleController.getString(R.string.MyFeatureName), 
    null, 
    MessageActionsHook.isMyFeatureEnabled()));

// Handle toggle in onClick
private void onMyFeatureToggled() {
    MessageActionsHook.setMyFeatureEnabled(!MessageActionsHook.isMyFeatureEnabled());
    adapter.notifyDataSetChanged();
}
```

### 4. Use the Hook in Message Context Menu
In the patch that adds the message action (e.g., in `org.telegram.ui.ChatActivity` or message actions builder):

```java
// Telegram core: minimal hook call
if (MessageActionsHook.isMyFeatureEnabled()) {
    // Add the action to the menu
    addActionItem(myFeatureAction);
}
```

Full logic belongs in `org.ushastoe.fluffy.patches.MessageActionsPatch`:

```java
public static void applyMyFeatureAction(ChatActivity activity, MessageObject message) {
    // Full implementation here
}
```

## Settings Hierarchy

Message action controls belong in **Appearance** or a dedicated **Message Actions** submenu:

```
Fluffy Settings
├── Appearance
│   ├── Title Mode
│   ├── Font Override
│   └── Message Actions (optional submenu)
│       ├── Feature A
│       ├── Feature B
│       └── Feature C
├── Premium
└── Debug
```

## Example: Add "Copy Formatted Text" Feature

### Step 1: Hook
```java
// MessageActionsHook.java
public static boolean isCopyFormattedTextEnabled() {
    return MessageActionsPatch.isCopyFormattedTextEnabled();
}

public static void setCopyFormattedTextEnabled(boolean enabled) {
    MessageActionsPatch.setCopyFormattedTextEnabled(enabled);
}
```

### Step 2: Patch
```java
// MessageActionsPatch.java
public static boolean isCopyFormattedTextEnabled() {
    SharedPreferences prefs = MessagesController.getInstance(0).getSettingsPreferences();
    return prefs.getBoolean("copy_formatted_text_enabled", true);
}

public static void setCopyFormattedTextEnabled(boolean enabled) {
    MessagesController.getInstance(0).getSettingsPreferences()
        .edit()
        .putBoolean("copy_formatted_text_enabled", enabled)
        .apply();
}

public static void copyFormattedText(Context context, MessageObject message) {
    String formattedText = formatMessageText(message);
    copyToClipboard(context, formattedText);
    // Show toast confirmation
}
```

### Step 3: Settings UI
Add to `FluffyAppearanceActivity`:

```java
// In updateItems()
items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_MESSAGE_ACTIONS_SECTION, 
    LocaleController.getString(R.string.MessageActionsSection), null, 0));
items.add(new ItemInner(VIEW_TYPE_TEXT_CHECK, ROW_COPY_FORMATTED_TEXT, 
    LocaleController.getString(R.string.CopyFormattedText), 
    null, 
    MessageActionsHook.isCopyFormattedTextEnabled()));
items.add(new ItemInner(VIEW_TYPE_INFO, ROW_MESSAGE_ACTIONS_INFO, 
    LocaleController.getString(R.string.MessageActionsInfo), null, 0));
```

### Step 4: Telegram Core Hook
In `org.telegram.ui.ChatActivity` (message context menu builder):

```java
// Minimal hook call in Telegram core
if (MessageActionsHook.isCopyFormattedTextEnabled()) {
    addActionItem(ActionType.COPY_FORMATTED, () -> {
        MessageActionsPatch.copyFormattedText(getContext(), selectedMessage);
    });
}
```

## Testing Checklist

- [ ] Feature toggle works in Fluffy Settings
- [ ] Toggle persists across app restarts
- [ ] Menu action appears when enabled, hidden when disabled
- [ ] Default state matches intention (enabled/disabled)
- [ ] No crashes with both states
- [ ] Settings string keys are consistent with resource IDs

## Migration Path for Existing Features

If a message action already exists in Telegram core without a control:

1. Add the hook + patch storage layer.
2. Add the UI control in Fluffy Settings (default to enabled if no stored value).
3. Add the conditional in Telegram core.
4. Test on both old and new installs.

---

**Last updated**: 18.03.2026  
**Maintainer**: Fluffy Telegram Fork Dev
