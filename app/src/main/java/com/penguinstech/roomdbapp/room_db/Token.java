package com.penguinstech.roomdbapp.room_db;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.penguinstech.roomdbapp.utils.Configs;

@Entity(tableName = Configs.tokensTableName)
public class Token {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "device_id")
    public String deviceId;

    @ColumnInfo(name = "last_sync")
    public String lastSync;


    public Token(){
        //empty constructor very important
    }

    public Token(String deviceId, String lastSync) {
        this.deviceId = deviceId;
        this.lastSync = lastSync;
    }
}
