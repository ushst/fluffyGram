package org.ushastoe.fluffy.patches;

public final class DeleteForEveryoneDefaultPatch {

    private DeleteForEveryoneDefaultPatch() {
    }

    public static boolean shouldEnableByDefault() {
        return true;
    }
}