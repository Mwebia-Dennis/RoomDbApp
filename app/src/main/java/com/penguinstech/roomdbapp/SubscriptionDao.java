package com.penguinstech.roomdbapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SubscriptionDao {

    @Query("SELECT * FROM "+Configs.subscriptionTableName)
    List<Subscription> getAll();

    @Insert
    void insertAll(List<Subscription> subscriptions);

    @Delete
    void delete(Subscription subscription);
}
