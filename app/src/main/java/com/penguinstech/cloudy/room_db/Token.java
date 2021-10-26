package com.penguinstech.cloudy.room_db;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.penguinstech.cloudy.utils.Configs;

@Entity(tableName = Configs.tokensTableName)
public class Token {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "device_id")
    public String deviceId;


    @ColumnInfo(name = "table_name")
    public String tableName;

    @ColumnInfo(name = "last_sync")
    public String lastSync;


    public Token(){
        //empty constructor very important
    }

    public Token(String deviceId, String tableName, String lastSync) {
        this.deviceId = deviceId;
        this.tableName = tableName;
        this.lastSync = lastSync;
    }
}
