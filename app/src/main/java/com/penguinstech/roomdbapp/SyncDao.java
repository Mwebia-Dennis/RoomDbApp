package com.penguinstech.roomdbapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SyncDao {


    @Query("SELECT * FROM "+Configs.syncTableName+" WHERE user_id = :userId")
    SyncInfo loadSyncInfoByUserId(int userId);

    @Insert
    void insertAll(List<SyncInfo> syncInfoList);

    @Delete
    void delete(SyncInfo syncInfo);
}
