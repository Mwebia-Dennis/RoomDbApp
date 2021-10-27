package com.penguinstech.cloudy.room_db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.penguinstech.cloudy.utils.Configs;

import java.util.List;

@Dao
public interface SubscriptionDao {

    @Query("SELECT * FROM "+ Configs.subscriptionTableName)
    List<Subscription> getAll();


    @Query("SELECT count(id) FROM "+Configs.subscriptionTableName)
    int getCount();

    @Query("SELECT * FROM "+Configs.subscriptionTableName+" where :colName = :value")
    Subscription filterByCol(String colName, String value);


    @Query("SELECT * FROM "+Configs.subscriptionTableName+" order by updated_at desc limit 1")
    Subscription getLastSubscription();

    @Insert
    void insertAll(List<Subscription> subscriptions);

    @Update
    void update(Subscription subscription);


    @Delete
    void delete(Subscription subscription);
}
