import android.text.InputType;
    private final static int OPTION_FAKE_EDIT = 9997;
    private final static int OPTION_RESET_FAKE_EDIT = 9998;
            case OPTION_FAKE_EDIT: {
                if (selectedObject != null) {
                    showFakeEditDialog(selectedObject);
                }
                break;
            }
            case OPTION_RESET_FAKE_EDIT: {
                if (selectedObject != null) {
                    resetFakeEdit(selectedObject);
                }
                break;
            }
        if (fluffyConfig.devModeEnabled && message != null && canFakeEdit(message)) {
            items.add(LocaleController.getString(R.string.FG_FakeEdit));
            options.add(OPTION_FAKE_EDIT);
            icons.add(R.drawable.msg_edit);
        }
        if (fluffyConfig.devModeEnabled && message != null && message.isFakeEdited) {
            items.add(LocaleController.getString(R.string.FG_ResetFakeEdit));
            options.add(OPTION_RESET_FAKE_EDIT);
            icons.add(R.drawable.msg_cancel);
        }

    private boolean canFakeEdit(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        if (messageObject.messageOwner instanceof TLRPC.TL_messageService) {
            return false;
        }
        if (!messageObject.isFakeEdited && messageObject.isEdited()) {
            return false;
        }
        if (!messageObject.isFakeEdited && TextUtils.isEmpty(messageObject.messageOwner.message)) {
            return false;
        }
        return !messageObject.isSponsored();
    }

    private void showFakeEditDialog(MessageObject messageObject) {
        if (!fluffyConfig.devModeEnabled || !canFakeEdit(messageObject) || getParentActivity() == null) {
            return;
        }
        Context context = getParentActivity();
        String currentText = messageObject.messageOwner.message != null ? messageObject.messageOwner.message : "";
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setText(currentText);
        editText.setSelection(editText.length());
        editText.setGravity(Gravity.START | Gravity.TOP);
        editText.setMinLines(1);
        editText.setMaxLines(5);
        editText.setPadding(0, 0, 0, AndroidUtilities.dp(6));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        FrameLayout frameLayout = new FrameLayout(context);
        int horizontalPadding = AndroidUtilities.dp(24);
        int topPadding = AndroidUtilities.dp(16);
        int bottomPadding = AndroidUtilities.dp(12);
        frameLayout.setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding);
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.FG_FakeEditTitle));
        builder.setMessage(LocaleController.getString(R.string.FG_FakeEditHint));
        builder.setView(frameLayout);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> applyFakeEdit(messageObject, editText.getText().toString()));
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        editText.requestFocus();
        AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(editText));
    }

    private void applyFakeEdit(MessageObject messageObject, String newText) {
        if (messageObject == null || newText == null || (!messageObject.isFakeEdited && !canFakeEdit(messageObject))) {
            return;
        }
        String originalText = messageObject.messageOwner.message != null ? messageObject.messageOwner.message : "";
        if (TextUtils.equals(originalText, newText)) {
            return;
        }
        String fakeText = newText;
        if (fakeText == null) {
            fakeText = "";
        }
        int time = getConnectionsManager().getCurrentTime();
        if (!messageObject.isFakeEdited) {
            getMessagesStorage().saveFakeEditHistory(messageObject.messageOwner.dialog_id, messageObject.getId(), time, originalText);
        }
        messageObject.messageOwner.message = fakeText;
        messageObject.messageOwner.flags |= TLRPC.MESSAGE_FLAG_EDITED;
        messageObject.messageOwner.edit_date = time;
        messageObject.messageOwner.edit_hide = false;
        messageObject.isFakeEdited = true;
        messageObject.generateCaption();
        messageObject.updateMessageText();
        messageObject.resetLayout();
        getMessagesStorage().updateMessageData(messageObject.messageOwner);
        updateVisibleRows();
    }

    private void resetFakeEdit(MessageObject messageObject) {
        if (messageObject == null || !messageObject.isFakeEdited) {
            return;
        }
        String originalText = getMessagesStorage().getFakeEditOriginalText(messageObject.messageOwner.dialog_id, messageObject.getId());
        if (originalText == null) {
            String errorText = LocaleController.getString(R.string.FG_FakeEditResetFailed);
            if (BulletinFactory.canShowBulletin(this)) {
                BulletinFactory.of(this).createErrorBulletin(errorText, themeDelegate).show();
            } else {
                AlertsCreator.showSimpleToast(this, errorText);
            }
            return;
        }
        messageObject.messageOwner.message = originalText;
        messageObject.messageOwner.flags &= ~TLRPC.MESSAGE_FLAG_EDITED;
        messageObject.messageOwner.edit_date = 0;
        messageObject.isFakeEdited = false;
        messageObject.generateCaption();
        messageObject.updateMessageText();
        messageObject.resetLayout();
        getMessagesStorage().clearFakeEditHistory(messageObject.messageOwner.dialog_id, messageObject.getId());
        getMessagesStorage().updateMessageData(messageObject.messageOwner);
        updateVisibleRows();
    }

