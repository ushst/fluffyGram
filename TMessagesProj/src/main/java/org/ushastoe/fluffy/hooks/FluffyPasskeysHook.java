package org.ushastoe.fluffy.hooks;

import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.GetCredentialRequest;
import org.telegram.tgnet.tl.TL_account;
import org.ushastoe.fluffy.patches.FluffyPasskeysPatch;

public final class FluffyPasskeysHook {
    private FluffyPasskeysHook() {
    }

    public static boolean isSupported() {
        return FluffyPasskeysPatch.isSupported();
    }

    public static CreateOptions prepareCreateOptions(String optionsData) throws Exception {
        return FluffyPasskeysPatch.prepareCreateOptions(optionsData);
    }

    public static void applyRegisterResponse(TL_account.registerPasskey request, String responseJson, String clientDataJson) throws Exception {
        FluffyPasskeysPatch.applyRegisterResponse(request, responseJson, clientDataJson);
    }

    public static LoginOptions prepareLoginOptions(String optionsData, boolean clickedButton) throws Exception {
        return FluffyPasskeysPatch.prepareLoginOptions(optionsData, clickedButton);
    }

    public static LoginResult applyLoginResponse(TL_account.finishPasskeyLogin request, String responseJson, String clientDataJson) throws Exception {
        return FluffyPasskeysPatch.applyLoginResponse(request, responseJson, clientDataJson);
    }

    public static final class CreateOptions {
        public final CreatePublicKeyCredentialRequest request;
        public final String clientDataJson;

        public CreateOptions(CreatePublicKeyCredentialRequest request, String clientDataJson) {
            this.request = request;
            this.clientDataJson = clientDataJson;
        }
    }

    public static final class LoginOptions {
        public final GetCredentialRequest request;
        public final String clientDataJson;

        public LoginOptions(GetCredentialRequest request, String clientDataJson) {
            this.request = request;
            this.clientDataJson = clientDataJson;
        }
    }

    public static final class LoginResult {
        public final int datacenterId;
        public final long userId;

        public LoginResult(int datacenterId, long userId) {
            this.datacenterId = datacenterId;
            this.userId = userId;
        }
    }
}
