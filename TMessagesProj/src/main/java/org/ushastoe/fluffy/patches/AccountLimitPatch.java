package org.ushastoe.fluffy.patches;

import org.telegram.messenger.UserConfig;

public final class AccountLimitPatch {

    private static final int MAX_ACCOUNT_DEFAULT_COUNT = 8;
    private static final int MAX_ACCOUNT_COUNT = 10;

    private AccountLimitPatch() {
    }

    public static int getMaxAccountDefaultCount() {
        return MAX_ACCOUNT_DEFAULT_COUNT;
    }

    public static int getMaxAccountCount() {
        return MAX_ACCOUNT_COUNT;
    }

    public static int getAllowedAccountCount(boolean hasPremiumOnAccounts) {
        return hasPremiumOnAccounts ? MAX_ACCOUNT_COUNT : MAX_ACCOUNT_DEFAULT_COUNT;
    }

    public static int findNextAvailableAccount() {
        for (int account = 0; account < MAX_ACCOUNT_COUNT; account++) {
            if (!UserConfig.getInstance(account).isClientActivated()) {
                return account;
            }
        }
        return -1;
    }

    public static boolean canAddAnotherAccount(boolean hasPremiumOnAccounts) {
        return UserConfig.getActivatedAccountsCount() < getAllowedAccountCount(hasPremiumOnAccounts);
    }
}
