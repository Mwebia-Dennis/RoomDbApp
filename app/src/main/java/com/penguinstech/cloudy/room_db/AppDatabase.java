package com.penguinstech.cloudy.room_db;

import androidx.room.Database;
import androidx.room.RoomDatabase;


@Database(entities = {Task.class, Subscription.class, SyncInfo.class, Token.class, Files.class}, version = 7)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
    public abstract SubscriptionDao subscriptionDao();
    public abstract SyncDao syncDao();
    public abstract TokenDao tokenDao();
    public abstract FileDao fileDao();
}
