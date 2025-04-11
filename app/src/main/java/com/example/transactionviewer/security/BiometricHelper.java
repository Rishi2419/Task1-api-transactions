package com.example.transactionviewer.security;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class BiometricHelper {
    private static final String TAG = "BiometricHelper";

    private final Context context;
    private final Executor executor;
    private final BiometricPrompt.AuthenticationCallback authCallback;

    public interface BiometricAuthCallback {
        void onSuccess();

        void onError(int errorCode, String errorMessage);
    }

    public BiometricHelper(FragmentActivity activity, BiometricAuthCallback callback) {
        this.context = activity;
        this.executor = ContextCompat.getMainExecutor(context);

        this.authCallback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d(TAG, "Biometric authentication succeeded");
                callback.onSuccess();
            }


            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);

                Log.e(TAG, "Authentication error: " + errorCode + " - " + errString);

                switch (errorCode) {
                    case BiometricPrompt.ERROR_USER_CANCELED:
                    case BiometricPrompt.ERROR_CANCELED:
                        // User tapped outside / pressed back
                        Log.d(TAG, "User canceled authentication via back/outside tap");
                        // Treat as failed, allow retry
                        callback.onError(errorCode, "Authentication cancelled. Please try again.");
                        break;

                    case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                        // User tapped cancel explicitly
                        Log.d(TAG, "User clicked cancel button - re-showing biometric prompt");
                        // Re-trigger biometric prompt
                        if (context instanceof FragmentActivity) {
                            FragmentActivity activity = (FragmentActivity) context;
                            showBiometricPrompt(activity,
                                    "Unlock App",
                                    "Use PIN or biometrics to continue",
                                    "Verify your identity to access your transactions");
                        } else {
                            callback.onError(errorCode, "Activity context invalid.");
                        }
                        break;

                    case BiometricPrompt.ERROR_LOCKOUT:
                    case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                    case BiometricPrompt.ERROR_NO_BIOMETRICS:
                        // Serious error – show fallback
                        callback.onError(errorCode, errString.toString());
                        break;

                    default:
                        // Generic error
                        callback.onError(errorCode, errString.toString());
                        break;
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.d(TAG, "Authentication failed - biometric recognized but didn't match");
            }
        };
    }

    public boolean isBiometricAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            androidx.biometric.BiometricManager biometricManager =
                    androidx.biometric.BiometricManager.from(context);

            int canAuth;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                canAuth = biometricManager.canAuthenticate(
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            } else {
                canAuth = biometricManager.canAuthenticate();
            }

            Log.d(TAG, "Biometric availability status: " + canAuth);
            return canAuth == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS;
        }
        return false;
    }

    public void showBiometricPrompt(FragmentActivity activity) {
        showBiometricPrompt(activity, "Biometric Authentication",
                "Log in using your biometric credential",
                "Verify your identity to access your transactions");
    }

    public void showBiometricPrompt(FragmentActivity activity, String title, String subtitle, String description) {
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, authCallback);

        BiometricPrompt.PromptInfo promptInfo;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription(description)
                    .setAllowedAuthenticators(
                            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .setConfirmationRequired(false)
                    .build();
        } else {
            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription(description)
                    .setNegativeButtonText("Cancel")
                    .setConfirmationRequired(false)
                    .build();
        }

        Log.d(TAG, "Launching biometric prompt");
        biometricPrompt.authenticate(promptInfo);
    }

    public void showBiometricPromptWithNegativeButton(FragmentActivity activity, String title,
                                                      String subtitle, String description) {
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, authCallback);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText("Cancel")
                .setConfirmationRequired(false)
                .build();

        Log.d(TAG, "Launching biometric prompt with negative button");
        biometricPrompt.authenticate(promptInfo);
    }
}
