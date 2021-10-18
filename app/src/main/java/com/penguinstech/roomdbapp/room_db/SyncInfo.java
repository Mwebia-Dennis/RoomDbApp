package com.penguinstech.roomdbapp.room_db;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.penguinstech.roomdbapp.utils.Configs;

@Entity(tableName = Configs.syncTableName)
public class SyncInfo {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "sync_status")
    public String syncStatus;

    @ColumnInfo(name = "updated_at")
    public String updatedAt;


    public SyncInfo(){
        //empty constructor very important
    }

    public SyncInfo(String userId, String syncStatus, String updatedAt) {
        this.userId = userId;
        this.syncStatus = syncStatus;
        this.updatedAt = updatedAt;
    }
}
