package com.example.transactionviewer.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.transactionviewer.R;
import com.example.transactionviewer.activities.LoginActivity;
import com.example.transactionviewer.adapter.TransactionAdapter;
import com.example.transactionviewer.api.ApiClient;
import com.example.transactionviewer.api.ApiService;
import com.example.transactionviewer.database.TransactionRepository;
import com.example.transactionviewer.database.UserProfileRepository;
import com.example.transactionviewer.databinding.DialogFilterBinding;
import com.example.transactionviewer.databinding.FragmentTransactionBinding;
import com.example.transactionviewer.model.Transaction;
import com.example.transactionviewer.security.TokenManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionFragment extends Fragment {

    private static final String TAG = "TransactionFragment";

    private FragmentTransactionBinding binding;
    private TransactionAdapter adapter;
    private TokenManager tokenManager;
    private TransactionRepository repository;
    private UserProfileRepository profileRepository;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 1;
    private boolean isOfflineMode = false;
    private Dialog filterDialog;

    public TransactionFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenManager = new TokenManager(requireContext());
        repository = new TransactionRepository(requireContext());
        profileRepository = new UserProfileRepository(requireContext());
        String token = tokenManager.getToken();

        if (token == null) {
            navigateToLogin(false);
            return;
        }

        adapter = new TransactionAdapter();
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.transactionsRecyclerView.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(this::fetchTransactions);

        // Load profile image into header
        loadProfileImage();

        setupSearch();
        setupFilter();

        fetchTransactions();
    }

    private void loadProfileImage() {
        profileRepository.getUserProfile(profile -> {
            if (profile != null && profile.getProfileImagePath() != null) {
                File imageFile = new File(profile.getProfileImagePath());
                if (imageFile.exists()) {
                    Glide.with(requireContext())
                            .load(imageFile)
                            .circleCrop()
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(binding.profileImageSmall);
                } else {
                    binding.profileImageSmall.setImageResource(R.drawable.default_profile);
                }
            } else {
                binding.profileImageSmall.setImageResource(R.drawable.default_profile);
            }
        });
    }

    private void setupSearch() {
        // Set search icon color
        ImageView searchIcon = binding.searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        if (searchIcon != null) {
            searchIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.purple_200),
                    PorterDuff.Mode.SRC_IN);
        }

        // Make the whole search area clickable to show keyboard
        View searchPlate = binding.searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setOnClickListener(v -> {
                binding.searchView.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(binding.searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
            });
        }

        // Make entire search view clickable to show keyboard
        binding.searchView.setOnClickListener(v -> {
            binding.searchView.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
        });

        // Set up query listeners
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.setSearchQuery(query);
                // Hide keyboard after submission
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.searchView.getWindowToken(), 0);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.setSearchQuery(newText);
                return true;
            }
        });

        binding.searchView.setOnCloseListener(() -> {
            adapter.setSearchQuery("");
            return false;
        });
    }

    private void setupFilter() {
        updateActiveFilterText();
        binding.filterButton.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        DialogFilterBinding dialogBinding = DialogFilterBinding.inflate(getLayoutInflater());
        filterDialog = new Dialog(requireContext());
        filterDialog.setContentView(dialogBinding.getRoot());


        // Set the dialog width to 90% of the screen width
        if (filterDialog.getWindow() != null) {
            filterDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        RadioGroup radioGroup = dialogBinding.filterRadioGroup;

        // Set initial selection based on current sort mode
        switch (adapter.getSortMode()) {
            case TransactionAdapter.SORT_BY_DATE_DESC:
                radioGroup.check(dialogBinding.radioLatest.getId());
                break;
            case TransactionAdapter.SORT_BY_DATE_ASC:
                radioGroup.check(dialogBinding.radioOldest.getId());
                break;
            case TransactionAdapter.SORT_BY_AMOUNT_DESC:
                radioGroup.check(dialogBinding.radioAmountDesc.getId());
                break;
            case TransactionAdapter.SORT_BY_AMOUNT_ASC:
                radioGroup.check(dialogBinding.radioAmountAsc.getId());
                break;
        }

        // Set up listener for radio button changes
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int sortMode;

            if (checkedId == dialogBinding.radioLatest.getId()) {
                sortMode = TransactionAdapter.SORT_BY_DATE_DESC;
            } else if (checkedId == dialogBinding.radioOldest.getId()) {
                sortMode = TransactionAdapter.SORT_BY_DATE_ASC;
            } else if (checkedId == dialogBinding.radioAmountDesc.getId()) {
                sortMode = TransactionAdapter.SORT_BY_AMOUNT_DESC;
            } else if (checkedId == dialogBinding.radioAmountAsc.getId()) {
                sortMode = TransactionAdapter.SORT_BY_AMOUNT_ASC;
            } else {
                sortMode = TransactionAdapter.SORT_BY_DATE_DESC;
            }

            adapter.setSortMode(sortMode);
            updateActiveFilterText();

            // Dismiss dialog after selection
            if (filterDialog != null && filterDialog.isShowing()) {
                filterDialog.dismiss();
            }
        });

        filterDialog.show();
    }

    private void updateActiveFilterText() {
        String filterName = adapter.getSortModeName();
        binding.activeFilterText.setText("Filter: " + filterName);
        binding.activeFilterText.setVisibility(View.VISIBLE);
    }

    private void fetchTransactions() {
        String authHeader = tokenManager.getAuthHeaderValue();
        if (authHeader == null) {
            navigateToLogin(true);
            return;
        }

        binding.swipeRefresh.setRefreshing(true);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getTransactions(authHeader).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                binding.swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    retryCount = 0;
                    List<Transaction> transactions = response.body();
                    repository.saveTransactions(transactions);
                    adapter.setTransactions(transactions);
                    setOfflineMode(false);
                } else {
                    loadFromLocalDatabase();
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Log.e(TAG, "Network error", t);

                loadFromLocalDatabase();

                if (t instanceof IOException && retryCount < MAX_RETRIES) {
                    retryCount++;
                    Toast.makeText(requireContext(), "Connection error. Using offline data.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Opps! You are offline", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadFromLocalDatabase() {
        repository.getTransactions(transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                adapter.setTransactions(transactions);
                setOfflineMode(true);
            } else {
                Toast.makeText(requireContext(), "No offline data available.", Toast.LENGTH_SHORT).show();
                setOfflineMode(true);
            }
        });
    }

    private void setOfflineMode(boolean offline) {
        isOfflineMode = offline;
        binding.offlineModeText.setVisibility(offline ? View.VISIBLE : View.GONE);
    }

    private void handleErrorResponse(Response<?> response) {
        if (response.code() == 401) {
            Toast.makeText(getContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            logout();
        } else if (response.code() == 403) {
            Toast.makeText(getContext(), "You don't have permission to view these transactions.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error code: " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        tokenManager.clearToken();
        navigateToLogin(false);
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
    public void onDestroyView() {
        super.onDestroyView();
        if (filterDialog != null && filterDialog.isShowing()) {
            filterDialog.dismiss();
        }
        binding = null;
    }
}