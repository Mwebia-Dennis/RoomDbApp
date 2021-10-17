package com.penguinstech.roomdbapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SubscriptionDao {

    @Query("SELECT * FROM "+Configs.subscriptionTableName)
    List<Subscription> getAll();


    @Query("SELECT count(id) FROM "+Configs.subscriptionTableName)
    int getCount();


    @Insert
    void insertAll(List<Subscription> subscriptions);

    @Update
    void update(Subscription subscription);


    @Delete
    void delete(Subscription subscription);
}
