package com.penguinstech.cloudy.room_db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Task {

    @PrimaryKey(autoGenerate = true)
    public Integer id;

    @ColumnInfo(name = "task_id")
    public String taskId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "updated_at")
    public String updatedAt;


    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    public int isDeleted;


    public Task(){
        //empty constructor very important
    }

    public Task(String taskId,String title, String description, String updatedAt, int isDeleted) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    public Task(int id, String taskId, String title, String description, String updatedAt, int isDeleted) {
        this.id = id;
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

}
