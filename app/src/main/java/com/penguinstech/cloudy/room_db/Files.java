package com.penguinstech.cloudy.room_db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.penguinstech.cloudy.utils.Configs;

@Entity(tableName = Configs.filesTableName)
public class Files {

    @PrimaryKey(autoGenerate = true)
    public Integer id;

    @ColumnInfo(name = "file_id")
    public String fileId;

    @ColumnInfo(name = "device_id")
    public String deviceId;

    @ColumnInfo(name = "local_path")
    public String localPath;

    @ColumnInfo(name = "firestore_path")
    public String firestorePath;

    @ColumnInfo(name = "updated_at")
    public String updatedAt;


    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    public int isDeleted;


    public Files(){
        //empty constructor very important
    }

    public Files(String fileId, String deviceId, String localPath, String firestorePath, String updatedAt, int isDeleted) {
        this.fileId = fileId;
        this.deviceId = deviceId;
        this.localPath = localPath;
        this.firestorePath = firestorePath;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    public Files(int id, String fileId, String deviceId, String localPath, String firestorePath, String updatedAt, int isDeleted) {
        this.id = id;
        this.fileId = fileId;
        this.deviceId = deviceId;
        this.localPath = localPath;
        this.firestorePath = firestorePath;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

}
