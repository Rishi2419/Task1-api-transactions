package com.example.transactionviewer.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    UserProfileEntity getUserProfile();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserProfile(UserProfileEntity userProfile);

}