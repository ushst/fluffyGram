import org.ushastoe.fluffy.quickreplies.FluffyQuickReply;
            if (object instanceof FluffyQuickReply) {
                if (chatActivityEnterView != null) {
                    FluffyQuickReply reply = (FluffyQuickReply) object;
                    String message = reply.message == null ? "" : reply.message;
                    if (!message.isEmpty() && !Character.isWhitespace(message.charAt(message.length() - 1))) {
                        message += " ";
                    }
                    chatActivityEnterView.replaceWithText(start, len, message, true);
                }
                return;
            } else if (object instanceof QuickRepliesController.QuickReply) {
