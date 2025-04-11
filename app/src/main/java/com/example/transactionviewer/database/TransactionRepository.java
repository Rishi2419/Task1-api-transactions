package com.example.transactionviewer.database;

import android.content.Context;
import android.util.Log;

import com.example.transactionviewer.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {
    private static final String TAG = "TransactionRepository";
    private AppDatabase database;
    private ExecutorService executorService;

    public TransactionRepository(Context context) {
        database = AppDatabase.getInstance(context);
        executorService = Executors.newSingleThreadExecutor();
    }

    public interface DataCallback<T> {
        void onDataLoaded(T data);
    }

    // Save transactions to local database
    public void saveTransactions(List<Transaction> transactions) {
        executorService.execute(() -> {
            try {
                List<TransactionEntity> entities = new ArrayList<>();
                for (Transaction transaction : transactions) {
                    entities.add(new TransactionEntity(
                            transaction.getId(),
                            transaction.getDate(),
                            transaction.getAmount(),
                            transaction.getCategory(),
                            transaction.getDescription()
                    ));
                }
                database.transactionDao().deleteAll(); // Clear old data
                database.transactionDao().insertAll(entities);
                Log.d(TAG, "Saved " + entities.size() + " transactions to local database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving transactions", e);
            }
        });
    }

    // Get all transactions from local database
    public void getTransactions(DataCallback<List<Transaction>> callback) {
        executorService.execute(() -> {
            try {
                List<TransactionEntity> entities = database.transactionDao().getAllTransactions();
                List<Transaction> transactions = new ArrayList<>();
                for (TransactionEntity entity : entities) {
                    transactions.add(new Transaction(
                            entity.getId(),
                            entity.getDate(),
                            entity.getAmount(),
                            entity.getCategory(),
                            entity.getDescription()
                    ));
                }
                Log.d(TAG, "Loaded " + transactions.size() + " transactions from local database");

                // Return data on main thread
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onDataLoaded(transactions));
            } catch (Exception e) {
                Log.e(TAG, "Error loading transactions", e);
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onDataLoaded(new ArrayList<>()));
            }
        });
    }
}