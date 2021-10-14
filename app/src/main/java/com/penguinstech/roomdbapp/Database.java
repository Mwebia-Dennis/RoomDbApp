package com.penguinstech.roomdbapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Task.class}, version = 1)
abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
}