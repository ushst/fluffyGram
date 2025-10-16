                    final ArrayList<Integer> messageIds = new ArrayList<>(messages);
                    final long dialogIdFinal = dialogId;
                    getMessagesStorage().getStorageQueue().postRunnable(() -> {
                        MessagesStorage storage = getMessagesStorage();
                        for (int id : messageIds) {
                            storage.markMessagesAsIsDeletedInternal(dialogIdFinal, id);
                        }
                        AndroidUtilities.runOnUIThread(() ->
                                getNotificationCenter().postNotificationName(fluffyConfig.MESSAGES_DELETED_NOTIFICATION, dialogIdFinal, messageIds));
                    });
            if (!deleteForOpponent || !fluffyConfig.saveDeletedMessages) {
                getNotificationCenter().postNotificationName(NotificationCenter.messagesDeleted, messages, channelId, scheduled, false, movedToScheduled, movedToScheduledMessageId);
            }
