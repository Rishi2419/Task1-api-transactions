package com.example.transactionviewer.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.transactionviewer.databinding.ItemCategoryLegendBinding;
import com.example.transactionviewer.model.CategoryLegendItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryLegendAdapter extends RecyclerView.Adapter<CategoryLegendAdapter.LegendViewHolder> {

    private List<CategoryLegendItem> items = new ArrayList<>();
    private NumberFormat currencyFormat;

    public CategoryLegendAdapter() {
        setupCurrencyFormat();
    }

    private void setupCurrencyFormat() {
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currencyFormat.setCurrency(java.util.Currency.getInstance("INR"));
        currencyFormat.setMaximumFractionDigits(0);
    }

    public void setItems(List<CategoryLegendItem> items) {
        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LegendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryLegendBinding binding = ItemCategoryLegendBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LegendViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LegendViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class LegendViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryLegendBinding binding;

        public LegendViewHolder(ItemCategoryLegendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CategoryLegendItem item) {
            // Set color indicator
            GradientDrawable background = (GradientDrawable) binding.categoryColorIndicator.getBackground();
            background.setColor(Color.parseColor(item.getColor()));

            // Set category name with proper capitalization
            String categoryName = item.getCategory();
            if (categoryName != null && !categoryName.isEmpty()) {
                categoryName = categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1);
            }
            binding.categoryNameText.setText(categoryName);

            // Format amount
            String formattedAmount = currencyFormat.format(item.getAmount());
            formattedAmount = formattedAmount.replace("₹", "").trim();
            binding.categoryAmountText.setText("₹" + formattedAmount);
        }
    }
}
