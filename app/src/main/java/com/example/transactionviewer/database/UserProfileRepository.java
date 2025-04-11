package com.example.transactionviewer.database;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserProfileRepository {
    private static final String TAG = "UserProfileRepository";
    private AppDatabase database;
    private ExecutorService executorService;

    public interface ProfileCallback<T> {
        void onProfileLoaded(T data);
    }

    public UserProfileRepository(Context context) {
        database = AppDatabase.getInstance(context);
        executorService = Executors.newSingleThreadExecutor();
    }

    public void saveUserProfile(String name, String profileImagePath) {
        executorService.execute(() -> {
            try {
                UserProfileEntity profileEntity = new UserProfileEntity(1, name, profileImagePath);
                database.userProfileDao().insertUserProfile(profileEntity);
                Log.d(TAG, "Saved user profile with image: " + profileImagePath);
            } catch (Exception e) {
                Log.e(TAG, "Error saving user profile", e);
            }
        });
    }

    public void getUserProfile(ProfileCallback<UserProfileEntity> callback) {
        executorService.execute(() -> {
            try {
                UserProfileEntity profileEntity = database.userProfileDao().getUserProfile();

                // Create default profile if none exists
                if (profileEntity == null) {
                    profileEntity = new UserProfileEntity(1, "Admin", null);
                    database.userProfileDao().insertUserProfile(profileEntity);
                }

                UserProfileEntity finalProfileEntity = profileEntity;
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onProfileLoaded(finalProfileEntity));
            } catch (Exception e) {
                Log.e(TAG, "Error loading user profile", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onProfileLoaded(new UserProfileEntity(1, "Admin", null)));
            }
        });
    }
}
