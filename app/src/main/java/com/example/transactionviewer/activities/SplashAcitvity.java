package com.example.transactionviewer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.transactionviewer.R;
import com.example.transactionviewer.security.BiometricHelper;
import com.example.transactionviewer.security.TokenManager;

public class SplashAcitvity extends AppCompatActivity {

    private TokenManager tokenManager;
    private BiometricHelper biometricHelper;
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_acitvity);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tokenManager = new TokenManager(this);

        new Handler(Looper.getMainLooper()).postDelayed(this::handleLaunchFlow, 1000);
    }

    private void handleLaunchFlow() {
        if (tokenManager.getToken() == null) {
            Log.d(TAG, "No token found. Launching login screen.");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            boolean isBiometricEnabled = tokenManager.isBiometricEnabled();

            if (isBiometricEnabled) {
                Log.d(TAG, "Token found. Biometric is ON. Showing biometric prompt.");
                setupBiometricAuthentication();
            } else {
                Log.d(TAG, "Token found. Biometric OFF. Skipping biometric.");
                goToMain();
            }
        }
    }

    private void setupBiometricAuthentication() {
        biometricHelper = new BiometricHelper(this, new BiometricHelper.BiometricAuthCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(SplashAcitvity.this, "Authentication successful", Toast.LENGTH_SHORT).show();
                    goToMain();
                });
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                Log.e(TAG, "Biometric failed: " + errorMessage);

                if (errorCode == BiometricPrompt.ERROR_LOCKOUT || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    runOnUiThread(() -> {
                        Toast.makeText(SplashAcitvity.this,
                                "Biometric locked. Please use your PIN",
                                Toast.LENGTH_SHORT).show();
                    });
                    startActivity(new Intent(SplashAcitvity.this, LoginActivity.class));
                    finish();
                } else if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    // Do not proceed to main or login, just re-prompt
                    runOnUiThread(() -> {
                        Toast.makeText(SplashAcitvity.this,
                                "Authentication cancelled. Please try again.",
                                Toast.LENGTH_SHORT).show();

                        // Show biometric prompt again
                        biometricHelper.showBiometricPrompt(SplashAcitvity.this,
                                "Unlock App",
                                "Use PIN or biometrics to continue",
                                "Verify your identity to access your transactions");
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(SplashAcitvity.this,
                                "Authentication error: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    });

                    startActivity(new Intent(SplashAcitvity.this, LoginActivity.class));
                    finish();
                }
            }

        });

        if (biometricHelper.isBiometricAvailable()) {
            biometricHelper.showBiometricPrompt(this,
                    "Unlock App",
                    "Use PIN or biometrics to continue",
                    "Verify your identity to access your transactions");
        } else {
            Log.d(TAG, "Biometric not available. Redirecting to Main.");
            goToMain();
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
