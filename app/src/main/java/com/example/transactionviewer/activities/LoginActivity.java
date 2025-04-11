package com.example.transactionviewer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.transactionviewer.api.ApiClient;
import com.example.transactionviewer.api.ApiService;
import com.example.transactionviewer.databinding.ActivityLoginBinding;
import com.example.transactionviewer.model.LoginRequest;
import com.example.transactionviewer.model.LoginResponse;
import com.example.transactionviewer.security.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = new TokenManager(this);

        boolean isSessionExpired = getIntent().getBooleanExtra("SESSION_EXPIRED", false);
        if (isSessionExpired) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            tokenManager.clearToken();
        } else if (tokenManager.getToken() != null) {
            Log.d(TAG, "Token found, going directly to main");
            navigateToMain();
        } else {
            Log.d(TAG, "No token found, showing login screen");
        }

        binding.loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = binding.usernameEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        LoginRequest loginRequest = new LoginRequest(username, password);

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess()) {
                        String token = loginResponse.getToken();
                        Log.d(TAG, "Token received: " + (token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "null"));

                        tokenManager.saveToken(token);
                        navigateToMain();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Login failed: " + loginResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Login failed. ";
                    if (response.code() == 401) {
                        errorMsg += "Invalid credentials.";
                    } else if (response.code() >= 500) {
                        errorMsg += "Server error. Please try again later.";
                    } else {
                        errorMsg += "Please try again.";
                    }
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                Log.e(TAG, "Login network error", t);
                Toast.makeText(LoginActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!isLoading);
    }
}
