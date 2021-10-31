package com.penguinstech.cloudy.room_db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.penguinstech.cloudy.utils.Configs;

import java.util.List;

@Dao
public interface TaskDao {

    @Query("SELECT task_id, title, description, updated_at, is_deleted  FROM "+ Configs.tableName)
    List<Task> getAll();


    @Query("SELECT * FROM "+ Configs.tableName+" where is_deleted != 1 order by updated_at desc")
    List<Task> getNonDeletedTasks();


    @Query("SELECT * FROM "+ Configs.tableName+" order by updated_at asc limit :limit offset :offset")
    List<Task> getAll(int limit, int offset);


    @Query("SELECT * FROM "+Configs.tableName+" order by updated_at desc limit 1")
    List<Task> getLatestTask();

    @Query("SELECT * FROM "+Configs.tableName+" where :colName = :value")
    List<Task> filterByCol(String colName, String value);

    @Query("SELECT task_id, title, description, updated_at, is_deleted FROM "+Configs.tableName+
            " where updated_at >= :date order by updated_at asc limit :limit offset :offset")
    List<Task> filterByDate(String date, int limit, int offset);

    @Query("SELECT count(id) FROM "+Configs.tableName+" where updated_at > :date")
    int filterByDateCount(String date);


    @Query("SELECT count(id) FROM "+Configs.tableName)
    int getCount();


    @Query("SELECT count(id) FROM "+Configs.tableName+" WHERE task_id = :taskId")
    int exists(String taskId);

    @Query("SELECT * FROM "+Configs.tableName+" order by updated_at asc limit 1")
    Task getFirstTask();

    @Query("SELECT * FROM "+Configs.tableName+" WHERE id = :id")
    Task loadTaskById(int id);

    @Insert
    void insertAll(List<Task> tasks);
    @Update
    void update(Task task);

    @Query("UPDATE "+Configs.tableName+" SET is_deleted = 1,updated_at = :updatedAt WHERE id = :taskId")
    void delete(int taskId, String updatedAt);
}
