package com.example.transactionviewer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.transactionviewer.R;
import com.example.transactionviewer.databinding.ItemTransactionBinding;
import com.example.transactionviewer.model.Transaction;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private String currentSearchQuery = "";
    private int currentSortMode = SORT_BY_DATE_DESC; // Latest first as default
    private Context context;

    // Category icon mapping
    private Map<String, CategoryInfo> categoryIcons = new HashMap<>();

    // Sort constants
    public static final int SORT_BY_DATE_DESC = 0;  // Latest first
    public static final int SORT_BY_DATE_ASC = 1;   // Oldest first
    public static final int SORT_BY_AMOUNT_DESC = 2; // High to low
    public static final int SORT_BY_AMOUNT_ASC = 3;  // Low to high
    public static final int SORT_BY_CATEGORY = 4;    // Alphabetically by category

    public TransactionAdapter() {
        setupCategoryIcons();
        setupCurrencyFormat();
    }

    private void setupCurrencyFormat() {
        currencyFormat.setCurrency(java.util.Currency.getInstance("INR"));
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(2);
    }

    private void setupCategoryIcons() {
        // Add category icons and colors
        categoryIcons.put("food", new CategoryInfo(R.drawable.ic_food, "#FFFDE7", "#F9A825"));
        categoryIcons.put("travel", new CategoryInfo(R.drawable.ic_transport, "#FCE4EC", "#E91E63"));
        categoryIcons.put("shopping", new CategoryInfo(R.drawable.ic_shopping, "#E8F5E9", "#4CAF50"));
        categoryIcons.put("health", new CategoryInfo(R.drawable.ic_health, "#FFEBEE", "#F44336"));
        categoryIcons.put("education", new CategoryInfo(R.drawable.ic_education, "#E0F7FA", "#00BCD4"));
        categoryIcons.put("other", new CategoryInfo(R.drawable.ic_other, "#EDE7F6", "#673AB7"));
    }

    public void setTransactions(List<Transaction> transactions) {
        this.allTransactions = new ArrayList<>(transactions);
        applyFilters();
    }

    public void setSearchQuery(String query) {
        currentSearchQuery = query.toLowerCase().trim();
        applyFilters();
    }

    public void setSortMode(int sortMode) {
        currentSortMode = sortMode;
        applyFilters();
    }

    public int getSortMode() {
        return currentSortMode;
    }

    public String getSortModeName() {
        switch (currentSortMode) {
            case SORT_BY_DATE_DESC:
                return "Latest first";
            case SORT_BY_DATE_ASC:
                return "Oldest first";
            case SORT_BY_AMOUNT_DESC:
                return "Amount (high to low)";
            case SORT_BY_AMOUNT_ASC:
                return "Amount (low to high)";
            case SORT_BY_CATEGORY:
                return "Category";
            default:
                return "Latest first";
        }
    }

    private void applyFilters() {
        // First filter by search query
        List<Transaction> tempList = new ArrayList<>();

        if (currentSearchQuery.isEmpty()) {
            tempList.addAll(allTransactions);
        } else {
            for (Transaction transaction : allTransactions) {
                if (matches(transaction, currentSearchQuery)) {
                    tempList.add(transaction);
                }
            }
        }

        // Then sort
        sortTransactions(tempList);

        // Update filtered list
        filteredTransactions = tempList;
        notifyDataSetChanged();
    }

    private boolean matches(Transaction transaction, String query) {
        return transaction.getCategory().toLowerCase().contains(query) ||
                transaction.getDescription().toLowerCase().contains(query) ||
                String.valueOf(transaction.getAmount()).contains(query) ||
                transaction.getDate().toLowerCase().contains(query);
    }

    private void sortTransactions(List<Transaction> transactions) {
        switch (currentSortMode) {
            case SORT_BY_DATE_DESC:
                Collections.sort(transactions, new DateComparator(false));
                break;
            case SORT_BY_DATE_ASC:
                Collections.sort(transactions, new DateComparator(true));
                break;
            case SORT_BY_AMOUNT_DESC:
                Collections.sort(transactions, new AmountComparator(false));
                break;
            case SORT_BY_AMOUNT_ASC:
                Collections.sort(transactions, new AmountComparator(true));
                break;
            case SORT_BY_CATEGORY:
                Collections.sort(transactions, new CategoryComparator());
                break;
        }
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = filteredTransactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return filteredTransactions.size();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        public TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Transaction transaction) {
            binding.descriptionTextView.setText(transaction.getDescription());
            binding.categoryTextView.setText(transaction.getCategory());

            String formattedAmount = currencyFormat.format(transaction.getAmount());
            formattedAmount = formattedAmount.replace("₹", "").trim();
            binding.amountTextView.setText("₹" + formattedAmount);

            // Format and set the date
            try {
                Date transactionDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .parse(transaction.getDate());
                if (transactionDate != null) {
                    binding.dateTextView.setText(displayDateFormat.format(transactionDate));
                } else {
                    binding.dateTextView.setText(transaction.getDate());
                }
            } catch (ParseException e) {
                binding.dateTextView.setText(transaction.getDate());
            }

            // Apply category styling
            String categoryKey = transaction.getCategory().toLowerCase();
            CategoryInfo categoryInfo = getCategoryInfo(categoryKey);

            // Set category icon
            binding.categoryIcon.setImageResource(categoryInfo.iconRes);

            // Set category background color
            binding.categoryIconBackground.setBackgroundColor(Color.parseColor(categoryInfo.backgroundColor));

            // Set category text color based on category
            binding.categoryTextView.setTextColor(Color.parseColor(categoryInfo.textColor));
        }
    }

    private CategoryInfo getCategoryInfo(String categoryKey) {
        CategoryInfo info = categoryIcons.get(categoryKey);
        if (info == null) {
            info = categoryIcons.get("other"); // Fallback to "other" category
        }
        return info;
    }

    // Helper class to store category styling information
    private static class CategoryInfo {
        int iconRes;
        String backgroundColor;
        String textColor;

        CategoryInfo(int iconRes, String backgroundColor, String textColor) {
            this.iconRes = iconRes;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
        }
    }

    // Comparator classes for sorting
    class DateComparator implements Comparator<Transaction> {
        private boolean ascending;

        DateComparator(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public int compare(Transaction t1, Transaction t2) {
            try {
                Date date1 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(t1.getDate());
                Date date2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(t2.getDate());

                if (date1 != null && date2 != null) {
                    return ascending ? date1.compareTo(date2) : date2.compareTo(date1);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            // Fallback to string comparison if parsing fails
            return ascending ?
                    t1.getDate().compareTo(t2.getDate()) :
                    t2.getDate().compareTo(t1.getDate());
        }
    }

    class AmountComparator implements Comparator<Transaction> {
        private boolean ascending;

        AmountComparator(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public int compare(Transaction t1, Transaction t2) {
            double diff = t1.getAmount() - t2.getAmount();
            if (ascending) {
                return diff < 0 ? -1 : (diff > 0 ? 1 : 0);
            } else {
                return diff > 0 ? -1 : (diff < 0 ? 1 : 0);
            }
        }
    }

    class CategoryComparator implements Comparator<Transaction> {
        @Override
        public int compare(Transaction t1, Transaction t2) {
            return t1.getCategory().compareToIgnoreCase(t2.getCategory());
        }
    }
}