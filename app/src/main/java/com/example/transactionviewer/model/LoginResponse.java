package com.example.transactionviewer.model;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }
}