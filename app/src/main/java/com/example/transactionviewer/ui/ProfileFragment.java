package com.example.transactionviewer.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.transactionviewer.R;
import com.example.transactionviewer.activities.LoginActivity;
import com.example.transactionviewer.database.UserProfileRepository;
import com.example.transactionviewer.databinding.FragmentProfileBinding;
import com.example.transactionviewer.security.BiometricHelper;
import com.example.transactionviewer.security.TokenManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private BiometricHelper biometricHelper;
    private TokenManager tokenManager;
    private UserProfileRepository profileRepository;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private String currentPhotoPath;
    private static final String DARK_MODE_PREF = "dark_mode_preference";

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    public ProfileFragment() {
        // Required empty public constructor
    }

    private void logout() {
        // Show confirmation dialog instead of direct logout
        showLogoutConfirmationDialog();
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Proceed with logout
                    tokenManager.clearToken();
                    navigateToLogin(false);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Dismiss dialog
                    dialog.dismiss();
                })
                .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.dialog_background))
                .show();
    }

    private void navigateToLogin(boolean sessionExpired) {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        if (sessionExpired) {
            intent.putExtra("SESSION_EXPIRED", true);
        }
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenManager = new TokenManager(requireContext());
        profileRepository = new UserProfileRepository(requireContext());

        registerActivityResultLaunchers();

        binding.logoutButton.setOnClickListener(v -> logout());
        binding.profileImage.setOnClickListener(v -> showImagePickerDialog());

        setupBiometricHelper();
        setupToggles();
        loadUserProfile();
    }

    private void registerActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            saveImageFromUri(selectedImageUri);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        File file = new File(currentPhotoPath);
                        if (file.exists()) {
                            profileRepository.getUserProfile(profile -> {
                                profileRepository.saveUserProfile(profile.getName(), currentPhotoPath);
                                loadUserProfile();
                            });
                        }
                    }
                });
    }

    private void setupBiometricHelper() {
        biometricHelper = new BiometricHelper(requireActivity(), new BiometricHelper.BiometricAuthCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    // Toggle biometric setting after authentication success
                    boolean newState = !tokenManager.isBiometricEnabled();
                    updateBiometricToggle(newState);
                });
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                requireActivity().runOnUiThread(() -> {
                    // Revert switch to previous state on authentication failure
                    binding.switchBiometrics.setChecked(tokenManager.isBiometricEnabled());
                    Toast.makeText(getContext(), "Authentication failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupToggles() {
        // Get the current dark mode state from preferences
        boolean isDarkModeOn = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getBoolean(DARK_MODE_PREF, false);

        // Set the initial state of the toggle
        binding.switchDarkMode.setChecked(isDarkModeOn);

//        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            // Save the dark mode preference
//            requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
//                    .edit()
//                    .putBoolean(DARK_MODE_PREF, isChecked)
//                    .apply();
//
//            // Apply dark mode and recreate activity to apply theme properly
//            if (isChecked) {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
//                Toast.makeText(getContext(), "Dark Mode Enabled", Toast.LENGTH_SHORT).show();
//            } else {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
//                Toast.makeText(getContext(), "Dark Mode Disabled", Toast.LENGTH_SHORT).show();
//            }
//
//            // Add a slight delay to ensure the toast is shown before recreation
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                if (isAdded() && getActivity() != null) {
//                    // Recreate the activity to apply theme changes properly
//                    getActivity().recreate();
//                }
//            }, 300);
//        });

        // Set initial state for biometrics
        binding.switchBiometrics.setChecked(tokenManager.isBiometricEnabled());

        binding.switchBiometrics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Prevent default toggle behavior - we'll handle it after authentication
            if (isChecked != tokenManager.isBiometricEnabled()) {
                // Revert switch to previous state until authentication completes
                binding.switchBiometrics.setChecked(tokenManager.isBiometricEnabled());

                // Show biometric prompt to verify user before changing setting
                if (biometricHelper.isBiometricAvailable()) {
                    biometricHelper.showBiometricPrompt(
                            requireActivity(),
                            "Confirm Identity",
                            "Authentication required to change security settings",
                            "Verify your identity to modify biometric lock settings"
                    );
                } else {
                    Toast.makeText(getContext(), "Biometric authentication not available on this device", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateBiometricToggle(boolean isEnabled) {
        // Update persistent storage
        tokenManager.setBiometricEnabled(isEnabled);

        // Update UI
        binding.switchBiometrics.setChecked(isEnabled);

        // Show feedback to user
        String message = isEnabled ? "Biometric Lock Enabled" : "Biometric Lock Disabled";
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadUserProfile() {
        profileRepository.getUserProfile(profile -> {
            binding.userNameText.setText(profile.getName());

            if (profile.getProfileImagePath() != null) {
                Glide.with(requireContext())
                        .load(new File(profile.getProfileImagePath()))
                        .circleCrop()
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(binding.profileImage);
            } else {
                binding.profileImage.setImageResource(R.drawable.default_profile);
            }
        });
    }

    private void showImagePickerDialog() {
        String[] options = {"Choose from Gallery", "Cancel"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Choose Profile Photo")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Choose from Gallery
                            if (checkAndRequestStoragePermissions()) {
                                openGallery();
                            }
                            break;
                        case 1: // Cancel
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private boolean checkAndRequestCameraPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private boolean checkAndRequestStoragePermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try again
                showImagePickerDialog();
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(requireContext(), "Error creating image file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.example.transactionviewer.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void saveImageFromUri(Uri uri) {
        try {
            // Create a file to save the image
            File imageFile = createImageFile();

            // Copy the content from URI to our file
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(imageFile);

            byte[] buffer = new byte[4 * 1024]; // 4k buffer
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            // Save profile with new image path
            profileRepository.getUserProfile(profile -> {
                profileRepository.saveUserProfile(profile.getName(), currentPhotoPath);
                loadUserProfile();
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update toggle state in case it was changed from outside the fragment
        boolean isDarkModeOn = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getBoolean(DARK_MODE_PREF, false);
        binding.switchDarkMode.setChecked(isDarkModeOn);
    }
}