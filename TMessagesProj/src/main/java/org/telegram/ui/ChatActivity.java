    public final static int OPTION_MARK_AS_READ = 36;
            case OPTION_MARK_AS_READ: {
                if (selectedObject != null && selectedObject.isContentUnread()) {
                    selectedObject.setContentIsRead();
                    MessagesController.getInstance(currentAccount).markMessageContentAsRead(selectedObject);
                    updateVisibleRows();
                }
                break;
            }
                    if ((selectedObject.isVoice() || selectedObject.isRoundVideo()) && selectedObject.isContentUnread() && !selectedObject.isOut()) {
                        items.add(LocaleController.getString(R.string.MarkAsRead));
                        options.add(OPTION_MARK_AS_READ);
                        icons.add(R.drawable.msg_markread);
                    }
