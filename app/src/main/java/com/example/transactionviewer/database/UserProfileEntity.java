package com.example.transactionviewer.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfileEntity {
    @PrimaryKey
    private int id;
    private String name;
    private String profileImagePath;

    public UserProfileEntity(int id, String name, String profileImagePath) {
        this.id = id;
        this.name = name;
        this.profileImagePath = profileImagePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImagePath() {
        return profileImagePath;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }
}