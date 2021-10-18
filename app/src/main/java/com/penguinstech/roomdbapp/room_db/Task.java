package com.penguinstech.roomdbapp.room_db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Task {

    @PrimaryKey(autoGenerate = true)
    public int id;

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

    public Task(String title, String description, String updatedAt, int isDeleted) {
        this.title = title;
        this.description = description;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    public Task(int id, String title, String description, String updatedAt, int isDeleted) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

}
