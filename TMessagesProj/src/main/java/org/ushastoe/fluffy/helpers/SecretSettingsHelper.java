package org.ushastoe.fluffy.helpers;

import android.util.Base64;

import org.telegram.messenger.FileLog;
import org.ushastoe.fluffy.fluffyConfig;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public final class SecretSettingsHelper {

    private static final String KEY_SECRET_SETTINGS_UNLOCKED_PREFIX = "secretSettingsUnlocked_";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    // TODO: replace with the actual public key issued by the server
    private static final String PUBLIC_KEY_BASE64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqNz86dTI8p4whE8zi/Yp" +
            "scPnZqEjADHFhLt9hXiboU/m/Kxyr9M+bq0qXu8w9n9wmxbhbA/U+0F/HnPbTmOw" +
            "V4gec7C/DxHCPaePVfTvtehbsq4voSR+O+QCfzQpe5ebUylfSnFjXQ9pggyOl6Ro" +
            "+XBYkelheQzY3OcCCAElCqIMv0BG5Qg/Is7k2VgFYLKoKEzw83VisOVw6aLebqZp" +
            "hDMmKgRncRh+6iRR7MxG6XP0H+vOGiNWZPia8ecLEuKrRirPVz+c7rkC3wsxSD1I" +
            "R45fwAstejZcEjaZ1vCMgSUwUlmgtRmD0cI2eVMZ3Dwv31SuERZExsEuQwF+L0Pg" +
            "KQIDAQAB";

    private SecretSettingsHelper() {
    }

    public static boolean isSecretSettingsUnlocked(long userId) {
        if (userId == 0) {
            return false;
        }
        return fluffyConfig.getPreferences()
                .getBoolean(KEY_SECRET_SETTINGS_UNLOCKED_PREFIX + userId, false);
    }

    public static void setSecretSettingsUnlocked(long userId, boolean unlocked) {
        if (userId == 0) {
            return;
        }
        fluffyConfig.getPreferences()
                .edit()
                .putBoolean(KEY_SECRET_SETTINGS_UNLOCKED_PREFIX + userId, unlocked)
                .apply();
    }

    public static boolean verifyUnlockCode(long userId, String code) {
        if (userId == 0 || code == null || code.isEmpty()) {
            return false;
        }
        String normalized = code.trim().replace(" ", "+");
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(getPublicKey());
            signature.update(String.valueOf(userId).getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.decode(normalized, Base64.DEFAULT));
        } catch (Exception e) {
            try {
                Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
                signature.initVerify(getPublicKey());
                signature.update(String.valueOf(userId).getBytes(StandardCharsets.UTF_8));
                String urlSafe = normalized.replace("-", "+").replace("_", "/");
                return signature.verify(Base64.decode(urlSafe, Base64.DEFAULT));
            } catch (Exception e2) {
                FileLog.e("Secret settings verification failed", e);
                return false;
            }
        }
    }

    private static PublicKey getPublicKey() throws GeneralSecurityException {
        byte[] decoded = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }
}
