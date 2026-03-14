package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.UserConfig;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public final class PremiumAccessPatch {

    private static final String PREFS_NAME = "fluffy_premium_access";
    private static final String KEY_TOKEN_PREFIX = "signed_access_token_";

    private PremiumAccessPatch() {
    }

    public static boolean hasPremiumAccess() {
        long currentUserId = getCurrentUserId();
        if (currentUserId == 0) {
            return false;
        }
        return validateToken(getStoredToken(currentUserId), currentUserId);
    }

    public static boolean importPremiumAccessToken(String token) {
        long tokenUserId = extractTokenUserId(token);
        if (tokenUserId == 0 || !validateToken(token, tokenUserId)) {
            return false;
        }
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return false;
        }
        preferences.edit().putString(getTokenKey(tokenUserId), token).apply();
        return true;
    }

    public static void clearPremiumAccessToken() {
        clearPremiumAccessToken(getCurrentUserId());
    }

    public static void clearPremiumAccessToken(long userId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || userId == 0) {
            return;
        }
        preferences.edit().remove(getTokenKey(userId)).apply();
    }

    public static boolean hasConfiguredPublicKey() {
        return !TextUtils.isEmpty(normalizePublicKey(BuildConfig.FLUFFY_PREMIUM_PUBLIC_KEY));
    }

    private static String getStoredToken(long userId) {
        SharedPreferences preferences = getPreferences();
        return preferences == null || userId == 0 ? null : preferences.getString(getTokenKey(userId), null);
    }

    private static boolean validateToken(String token, long expectedUserId) {
        if (TextUtils.isEmpty(token) || expectedUserId == 0) {
            return false;
        }
        try {
            ParsedToken parsedToken = parseToken(token);
            if (parsedToken == null) {
                return false;
            }
            JSONObject payload = parsedToken.payload;
            long tokenUserId = payload.optLong("uid", payload.optLong("user_id", 0));
            boolean forever = payload.optBoolean("forever", false) || payload.optInt("forever", 0) == 1;
            long expiresAt = payload.optLong("exp", 0);
            if (tokenUserId != expectedUserId) {
                return false;
            }

            if (!forever) {
                if (expiresAt <= 0) {
                    return false;
                }
                long nowSeconds = System.currentTimeMillis() / 1000L;
                if (expiresAt <= nowSeconds) {
                    return false;
                }
            }

            if (!hasConfiguredPublicKey()) {
                return false;
            }

            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(getPublicKey());
            verifier.update(parsedToken.signedData.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(decodeBase64Url(parsedToken.signaturePart));
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static long extractTokenUserId(String token) {
        try {
            ParsedToken parsedToken = parseToken(token);
            if (parsedToken == null) {
                return 0;
            }
            return parsedToken.payload.optLong("uid", parsedToken.payload.optLong("user_id", 0));
        } catch (Throwable ignore) {
            return 0;
        }
    }

    private static ParsedToken parseToken(String token) throws Exception {
        if (TextUtils.isEmpty(token)) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2 && parts.length != 3) {
            return null;
        }

        final String signedData;
        final String payloadPart;
        final String signaturePart;
        if (parts.length == 2) {
            signedData = parts[0];
            payloadPart = parts[0];
            signaturePart = parts[1];
        } else {
            signedData = parts[0] + "." + parts[1];
            payloadPart = parts[1];
            signaturePart = parts[2];
        }
        JSONObject payload = new JSONObject(new String(decodeBase64Url(payloadPart), StandardCharsets.UTF_8));
        return new ParsedToken(signedData, signaturePart, payload);
    }

    private static PublicKey getPublicKey() throws Exception {
        String normalized = normalizePublicKey(BuildConfig.FLUFFY_PREMIUM_PUBLIC_KEY);
        byte[] keyBytes = Base64.decode(normalized, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static String normalizePublicKey(String publicKey) {
        if (TextUtils.isEmpty(publicKey)) {
            return "";
        }
        return publicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private static byte[] decodeBase64Url(String value) {
        String normalized = value.replace('-', '+').replace('_', '/');
        int padding = normalized.length() % 4;
        if (padding == 2) {
            normalized += "==";
        } else if (padding == 3) {
            normalized += "=";
        } else if (padding == 1) {
            throw new IllegalArgumentException("Invalid base64url length");
        }
        return Base64.decode(normalized, Base64.DEFAULT);
    }

    private static long getCurrentUserId() {
        int account = UserConfig.selectedAccount;
        return UserConfig.getInstance(account).getClientUserId();
    }

    private static String getTokenKey(long userId) {
        return KEY_TOKEN_PREFIX + userId;
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static final class ParsedToken {
        private final String signedData;
        private final String signaturePart;
        private final JSONObject payload;

        private ParsedToken(String signedData, String signaturePart, JSONObject payload) {
            this.signedData = signedData;
            this.signaturePart = signaturePart;
            this.payload = payload;
        }
    }
}
