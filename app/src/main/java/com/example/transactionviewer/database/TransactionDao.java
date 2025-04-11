package com.example.transactionviewer.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    List<TransactionEntity> getAllTransactions();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TransactionEntity> transactions);

    @Query("DELETE FROM transactions")
    void deleteAll();
}