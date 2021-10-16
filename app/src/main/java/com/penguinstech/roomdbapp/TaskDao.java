package com.penguinstech.roomdbapp;
import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TaskDao {

    @Query("SELECT * FROM "+Configs.tableName)
    List<Task> getAll();


    @Query("SELECT * FROM "+Configs.tableName+" order by updated_at desc limit 1")
    List<Task> getLatestTask();

    @Query("SELECT * FROM "+Configs.tableName+" where :colName = :value")
    List<Task> filterByCol(String colName, String value);

    @Query("SELECT * FROM "+Configs.tableName+" where updated_at > :date")
    List<Task> filterByDate(String date);

    @Query("SELECT count(id) FROM "+Configs.tableName)
    int getCount();

    @Query("SELECT * FROM "+Configs.tableName+" WHERE id = :taskId")
    Task loadTaskById(int taskId);

    @Insert
    void insertAll(List<Task> tasks);

    @Delete
    void delete(Task task);
}
