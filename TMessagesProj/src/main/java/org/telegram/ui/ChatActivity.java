    private final static int OPTION_BLOCK_STICKER = 9996;
            case OPTION_BLOCK_STICKER: {
                if (selectedObject != null && selectedObject.getDocument() != null) {
                    fluffyConfig.addBlockedSticker(selectedObject.getDocument().id);
                    BulletinHelper.showSimpleBulletin(this, getString(R.string.StickerAddedToBlacklist), null);
                    updateVisibleRows();
                }
                break;
            }
                        items.add(LocaleController.getString(R.string.BlockSticker));
                        options.add(OPTION_BLOCK_STICKER);
                        icons.add(R.drawable.msg_block);
                    items.add(LocaleController.getString(R.string.BlockSticker));
                    options.add(OPTION_BLOCK_STICKER);
                    icons.add(R.drawable.msg_block);
