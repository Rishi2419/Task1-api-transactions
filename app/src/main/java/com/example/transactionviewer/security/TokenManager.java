package com.example.transactionviewer.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenManager {
    private static final String TAG = "TokenManager";
    private static final String PREF_FILE_NAME = "secure_transaction_prefs";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_TOKEN_TYPE = "token_type";  // Add token type storage
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    private SharedPreferences encryptedPrefs;

    public TokenManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error initializing EncryptedSharedPreferences", e);
            // Fallback to regular SharedPreferences in case of error
            encryptedPrefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        }
    }

    public void saveToken(String token) {
        // Parse the token to separate token type and actual token if needed
        if (token != null && token.startsWith("Bearer ")) {
            encryptedPrefs.edit()
                    .putString(KEY_TOKEN_TYPE, "Bearer")
                    .putString(KEY_AUTH_TOKEN, token.substring(7))
                    .apply();
        } else {
            encryptedPrefs.edit()
                    .putString(KEY_TOKEN_TYPE, "Bearer")  // Default to Bearer
                    .putString(KEY_AUTH_TOKEN, token)
                    .apply();
        }
    }

    public String getToken() {
        String token = encryptedPrefs.getString(KEY_AUTH_TOKEN, null);
        return token;
    }

    /**
     * Get the complete authorization header value
     */
    public String getAuthHeaderValue() {
        String token = getToken();
        if (token == null) {
            return null;
        }
        String tokenType = encryptedPrefs.getString(KEY_TOKEN_TYPE, "Bearer");
        return tokenType + " " + token;
    }

    public void clearToken() {
        encryptedPrefs.edit()
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_TOKEN_TYPE)
                .apply();
    }

    public void setBiometricEnabled(boolean enabled) {
        encryptedPrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return encryptedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }
}