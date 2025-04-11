package com.example.transactionviewer.model;

public class CategoryLegendItem {
    private String category;
    private String color;
    private double amount;

    public CategoryLegendItem(String category, String color, double amount) {
        this.category = category;
        this.color = color;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public String getColor() {
        return color;
    }

    public double getAmount() {
        return amount;
    }
}