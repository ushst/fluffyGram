package org.ushastoe.fluffy.patches;

public final class DeleteForEveryoneDefaultPatch {

    private DeleteForEveryoneDefaultPatch() {
    }

    public static boolean shouldEnableByDefault() {
        return true;
    }

    public static boolean shouldShowDeleteForAllOption(boolean scheduled, boolean isSavedMessages, boolean isChannel, boolean hasEncryptedChat) {
        return PremiumSettingsPatch.isSaveDeletedMessagesEnabled()
                && !scheduled
                && !isSavedMessages
                && !isChannel
                && !hasEncryptedChat;
    }
}
