package com.example.transactionviewer.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.transactionviewer.R;
import com.example.transactionviewer.adapter.CategoryLegendAdapter;
import com.example.transactionviewer.api.ApiClient;
import com.example.transactionviewer.api.ApiService;
import com.example.transactionviewer.database.UserProfileRepository;
import com.example.transactionviewer.databinding.FragmentAnalysisBinding;
import com.example.transactionviewer.model.CategoryLegendItem;
import com.example.transactionviewer.model.Transaction;
import com.example.transactionviewer.security.TokenManager;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalysisFragment extends Fragment {

    private FragmentAnalysisBinding binding;
    private TokenManager tokenManager;
    private SharedPreferences preferences;
    private double budget = 5000; // Default budget
    private double totalExpenses = 0;

    private UserProfileRepository profileRepository;

    private CategoryLegendAdapter legendAdapter;
    private NumberFormat currencyFormat;
    private static final String PREF_NAME = "BudgetPrefs";
    private static final String BUDGET_KEY = "userBudget";

    private Map<String, CategoryInfo> categoryInfo = new HashMap<>();

    public AnalysisFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalysisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize preferences and token manager
        preferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        tokenManager = new TokenManager(requireContext());
        setupCurrencyFormat();
        profileRepository = new UserProfileRepository(requireContext());

        // Setup UI components
        setupCategoryLegend();
        setupPieChart();
        setupCurrentMonth();

        // Load saved budget
        loadSavedBudget();

        loadProfileImage();

        // Setup budget input and save button
        binding.budgetEditText.setText(String.valueOf(budget));
        binding.saveBudgetButton.setOnClickListener(v -> saveBudget());

        // Fetch transactions
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

    private void setupCurrencyFormat() {
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currencyFormat.setCurrency(java.util.Currency.getInstance("INR"));
        currencyFormat.setMaximumFractionDigits(0);
    }

    private void setupCategoryLegend() {
        // Initialize category colors map with same colors used in adapter
        categoryInfo.put("food", new CategoryInfo("#F9A825"));
        categoryInfo.put("travel", new CategoryInfo("#E91E63"));
        categoryInfo.put("shopping", new CategoryInfo("#4CAF50"));
        categoryInfo.put("health", new CategoryInfo("#F44336"));
        categoryInfo.put("education", new CategoryInfo("#00BCD4"));
        categoryInfo.put("other", new CategoryInfo("#673AB7"));

        // Setup RecyclerView for category legend
        legendAdapter = new CategoryLegendAdapter();
        binding.categoryLegendRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.categoryLegendRecyclerView.setAdapter(legendAdapter);
    }

    private void setupPieChart() {
        // Configure pie chart appearance
        binding.expensesPieChart.setUsePercentValues(false);
        binding.expensesPieChart.getDescription().setEnabled(false);
        binding.expensesPieChart.setExtraOffsets(5, 10, 5, 5);
        binding.expensesPieChart.setDragDecelerationFrictionCoef(0.95f);
        binding.expensesPieChart.setDrawHoleEnabled(true);
        binding.expensesPieChart.setHoleColor(Color.WHITE);
        binding.expensesPieChart.setTransparentCircleRadius(61f);
        binding.expensesPieChart.setDrawEntryLabels(false);
        binding.expensesPieChart.setEntryLabelColor(Color.WHITE);
        binding.expensesPieChart.setEntryLabelTextSize(12f);
        binding.expensesPieChart.setCenterText("");

        // Configure legend
        Legend legend = binding.expensesPieChart.getLegend();
        legend.setEnabled(false); // Disable default legend as we have custom legend
    }

    private void setupCurrentMonth() {
        // Set current month text
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        binding.monthText.setText(monthYearFormat.format(calendar.getTime()));
    }

    private void loadSavedBudget() {
        budget = preferences.getFloat(BUDGET_KEY, 5000f);
        updateBudgetText(budget);
    }

    private void saveBudget() {
        String budgetText = binding.budgetEditText.getText().toString().trim();
        if (!budgetText.isEmpty()) {
            try {
                budget = Double.parseDouble(budgetText);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putFloat(BUDGET_KEY, (float) budget);
                editor.apply();

                updateBudgetText(budget);
                updateExpenseStatus();

                // Update the pie chart based on the new budget
                if (binding.expensesPieChart.getData() != null) {
                    // Get current category expenses from existing data
                    Map<String, Double> categoryExpenses = getCurrentCategoryExpenses();
                    updatePieChart(categoryExpenses);
                }

                Toast.makeText(requireContext(), "Budget saved", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Please enter a budget amount", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to extract current category expenses from pie chart data
    private Map<String, Double> getCurrentCategoryExpenses() {
        Map<String, Double> categoryExpenses = new HashMap<>();

        if (binding.expensesPieChart.getData() != null) {
            PieDataSet dataSet = (PieDataSet) binding.expensesPieChart.getData().getDataSet();
            if (dataSet != null) {
                for (PieEntry entry : dataSet.getValues()) {
                    String category = entry.getLabel();
                    float value = entry.getValue();

                    // Skip the "remaining" entry as it's not a real category
                    if (!"remaining".equals(category)) {
                        categoryExpenses.put(category, (double) value);
                    }
                }
            }
        }

        return categoryExpenses;
    }

    private void fetchTransactions() {
        String authHeader = tokenManager.getAuthHeaderValue();
        if (authHeader == null) {
            navigateToLogin();
            return;
        }

        binding.loadingProgressBar.setVisibility(View.VISIBLE);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getTransactions(authHeader).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                binding.loadingProgressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    processTransactions(response.body());
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                binding.loadingProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load data or You are offline",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processTransactions(List<Transaction> transactions) {
        // Calculate total expenses and category breakdown
        Map<String, Double> categoryExpenses = new HashMap<>();
        totalExpenses = 0;

        for (Transaction transaction : transactions) {
            double amount = transaction.getAmount();
            totalExpenses += amount;

            String category = transaction.getCategory().toLowerCase();
            categoryExpenses.put(category, categoryExpenses.getOrDefault(category, 0.0) + amount);
        }

        // Update UI with animated values
        animateExpensesValue(totalExpenses);
        updatePieChart(categoryExpenses);
        updateLegend(categoryExpenses);
        updateExpenseStatus();
    }

    private void animateExpensesValue(double finalValue) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, (float) finalValue);
        animator.setDuration(1500);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            updateExpensesText(animatedValue);
        });
        animator.start();
    }

    private void updatePieChart(Map<String, Double> categoryExpenses) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        // Calculate total expenses
        double totalAmount = 0;
        for (Double amount : categoryExpenses.values()) {
            totalAmount += amount;
        }

        // Add remaining budget as an entry if expenses are less than budget
        double remainingBudget = budget - totalAmount;
        if (remainingBudget > 0) {
            entries.add(new PieEntry((float) remainingBudget, "remaining"));
            colors.add(Color.parseColor("#EEEEEE")); // Light gray for remaining budget
        }

        // Create entries for pie chart
        for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
            String category = entry.getKey();
            double amount = entry.getValue();

            entries.add(new PieEntry((float) amount, category));

            // Get color for this category
            CategoryInfo info = categoryInfo.getOrDefault(category, categoryInfo.get("other"));
            colors.add(Color.parseColor(info.color));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expenses");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(0f); // Set text size to 0 to hide all values
        dataSet.setValueTextColor(Color.TRANSPARENT); // Make text color transparent as a fallback
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        binding.expensesPieChart.setData(data);
        binding.expensesPieChart.invalidate();

        // Animate the chart
        binding.expensesPieChart.animateY(1400, Easing.EaseInOutQuad);
    }

    private void updateLegend(Map<String, Double> categoryExpenses) {
        List<CategoryLegendItem> legendItems = new ArrayList<>();

        for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
            String category = entry.getKey();
            double amount = entry.getValue();

            // Get color for this category
            CategoryInfo info = categoryInfo.getOrDefault(category, categoryInfo.get("other"));
            legendItems.add(new CategoryLegendItem(
                    category,
                    info.color,
                    amount
            ));
        }

        legendAdapter.setItems(legendItems);
    }

    private void updateExpenseStatus() {
        // Update status icon based on budget vs expenses
        if (totalExpenses <= budget) {
            binding.statusIcon.setImageResource(R.drawable.ic_trending_down);
            binding.expensesAmountText.setTextColor(Color.parseColor("#4CAF50"));
            binding.statusIcon.setColorFilter(Color.parseColor("#4CAF50"));
        } else {
            binding.statusIcon.setImageResource(R.drawable.ic_trending_up);
            binding.statusIcon.setColorFilter(Color.parseColor("#F44336"));
            binding.expensesAmountText.setTextColor(Color.parseColor("#F44336"));
        }
    }

    private void updateBudgetText(double value) {
        binding.budgetAmountText.setText(formatCurrency(value));
    }

    private void updateExpensesText(double value) {

        if (binding != null && binding.expensesAmountText != null) {
            binding.expensesAmountText.setText(formatCurrency(value));
        }

    }

    private String formatCurrency(double value) {
        return currencyFormat.format(value);
    }

    private void handleErrorResponse(Response<?> response) {
        if (response.code() == 401) {
            Toast.makeText(getContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        } else {
            Toast.makeText(getContext(), "Error code: " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToLogin() {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Helper class to store category color information
    private static class CategoryInfo {
        String color;

        CategoryInfo(String color) {
            this.color = color;
        }
    }
}