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
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAw4vQd2pb5H0JKmS0CBf+" +
            "NuHkTzShxUClAAqWAF70+VruMcrQ88Nhm6ZBx2A9DaeO0/gzF+4YeqjqtqlXg9gY" +
            "yMvrg65BGqk8+Vh8N+YGa0NBySnz45yPp4Ds7hP1mOoBsWYKDkAlAAXUyZ7iJsmu" +
            "xlIb4TyKXTT3qdyA7e0BPY3VRk5YqDkLZfOp/S3w6ZrzkR7wTfVNAV1fENyJ0L0p" +
            "Le8IfV3PMNK0i6qwfbKEmf3zdSdVqQGQPvYbh9Zk4uRfGQj2OGcyHWdVQFO1c0W+" +
            "dS8ivwxOxjUAG72GxWaw9pLtYCHX2+nGk+VUZRWgczw9O/XXIwIDAQAB";

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
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(getPublicKey());
            signature.update(String.valueOf(userId).getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.decode(code, Base64.DEFAULT));
        } catch (Exception e) {
            FileLog.e("Secret settings verification failed", e);
            return false;
        }
    }

    private static PublicKey getPublicKey() throws GeneralSecurityException {
        byte[] decoded = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }
}
