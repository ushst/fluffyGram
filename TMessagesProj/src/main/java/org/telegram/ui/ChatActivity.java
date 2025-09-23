        boolean showBizBot = !fluffyConfig.hideTopBar && currentEncryptedChat == null && getUserConfig().isPremium() && preferences.getLong("dialog_botid" + did, 0) != 0 || DEBUG_TOP_PANELS;
