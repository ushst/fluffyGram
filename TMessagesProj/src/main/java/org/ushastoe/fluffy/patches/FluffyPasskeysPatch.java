package org.ushastoe.fluffy.patches;

import android.os.Build;
import android.util.Base64;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetPublicKeyCredentialOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.ushastoe.fluffy.hooks.FluffyPasskeysHook;

public final class FluffyPasskeysPatch {
    private FluffyPasskeysPatch() {
    }

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= 34;
    }

    public static FluffyPasskeysHook.CreateOptions prepareCreateOptions(String optionsData) throws Exception {
        JSONObject publicKeyObj = getPublicKey(optionsData);
        String requestJson = publicKeyObj.toString();
        JSONObject rpObj = publicKeyObj.getJSONObject("rp");
        String origin = "https://" + rpObj.getString("id");
        String challenge = publicKeyObj.getString("challenge");
        String clientDataJson = buildClientDataJson(false, challenge, origin);
        byte[] clientDataHash = computeClientDataHash(clientDataJson);
        CreatePublicKeyCredentialRequest request = new CreatePublicKeyCredentialRequest(requestJson, clientDataHash, false, origin);
        return new FluffyPasskeysHook.CreateOptions(request, clientDataJson);
    }

    public static void applyRegisterResponse(TL_account.registerPasskey request, String responseJson, String clientDataJson) throws Exception {
        JSONObject json = new JSONObject(responseJson);
        JSONObject response = json.getJSONObject("response");

        request.credential = new TL_account.inputPasskeyCredentialPublicKey();
        request.credential.id = json.getString("id");
        request.credential.raw_id = json.getString("rawId");

        TL_account.inputPasskeyResponseRegister passkeyResponse = new TL_account.inputPasskeyResponseRegister();
        passkeyResponse.client_data = new TLRPC.TL_dataJSON();
        passkeyResponse.client_data.data = clientDataJson;
        passkeyResponse.attestation_object = Base64.decode(response.getString("attestationObject"), Base64.URL_SAFE);
        request.credential.response = passkeyResponse;
    }

    public static FluffyPasskeysHook.LoginOptions prepareLoginOptions(String optionsData, boolean clickedButton) throws Exception {
        JSONObject publicKeyObj = getPublicKey(optionsData);
        String requestJson = publicKeyObj.toString();
        String origin = "https://" + publicKeyObj.getString("rpId");
        String challenge = publicKeyObj.getString("challenge");
        String clientDataJson = buildClientDataJson(true, challenge, origin);
        byte[] clientDataHash = computeClientDataHash(clientDataJson);
        GetPublicKeyCredentialOption credentialOption = new GetPublicKeyCredentialOption(requestJson, clientDataHash);
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(credentialOption)
                .setPreferImmediatelyAvailableCredentials(!clickedButton)
                .setOrigin(origin)
                .build();
        return new FluffyPasskeysHook.LoginOptions(request, clientDataJson);
    }

    public static FluffyPasskeysHook.LoginResult applyLoginResponse(TL_account.finishPasskeyLogin request, String responseJson, String clientDataJson) throws Exception {
        JSONObject json = new JSONObject(responseJson);
        JSONObject response = json.getJSONObject("response");

        request.credential = new TL_account.inputPasskeyCredentialPublicKey();
        request.credential.id = json.getString("id");
        request.credential.raw_id = json.getString("rawId");

        TL_account.inputPasskeyResponseLogin passkeyResponse = new TL_account.inputPasskeyResponseLogin();
        passkeyResponse.client_data = new TLRPC.TL_dataJSON();
        passkeyResponse.client_data.data = clientDataJson;
        passkeyResponse.authenticator_data = Base64.decode(response.getString("authenticatorData"), Base64.URL_SAFE);
        passkeyResponse.signature = Base64.decode(response.getString("signature"), Base64.URL_SAFE);
        passkeyResponse.user_handle = new String(Base64.decode(response.getString("userHandle"), Base64.URL_SAFE), StandardCharsets.UTF_8);
        request.credential.response = passkeyResponse;

        String[] parts = passkeyResponse.user_handle.split(":");
        int datacenterId = Integer.parseInt(parts[0]);
        long userId = Long.parseLong(parts[1]);
        return new FluffyPasskeysHook.LoginResult(datacenterId, userId);
    }

    private static JSONObject getPublicKey(String optionsData) throws JSONException {
        return new JSONObject(optionsData).getJSONObject("publicKey");
    }

    private static String buildClientDataJson(boolean login, String challenge, String origin) throws JSONException {
        JSONObject clientData = new JSONObject();
        clientData.put("type", login ? "webauthn.get" : "webauthn.create");
        clientData.put("challenge", challenge);
        clientData.put("origin", origin);
        return clientData.toString();
    }

    private static byte[] computeClientDataHash(String clientDataJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(clientDataJson.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
