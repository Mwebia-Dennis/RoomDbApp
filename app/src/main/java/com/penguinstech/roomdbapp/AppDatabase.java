package com.penguinstech.roomdbapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Task.class, Subscription.class, SyncInfo.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
    public abstract SubscriptionDao subscriptionDao();
    public abstract SyncDao syncDao();
}
