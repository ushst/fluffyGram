package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.DeleteForEveryoneDefaultPatch;

public final class DeleteForEveryoneDefaultHook {

    private DeleteForEveryoneDefaultHook() {
    }

    public static boolean shouldEnableByDefault() {
        return DeleteForEveryoneDefaultPatch.shouldEnableByDefault();
    }
}