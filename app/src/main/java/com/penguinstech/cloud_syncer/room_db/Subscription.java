package com.penguinstech.cloud_syncer.room_db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.penguinstech.cloud_syncer.utils.Configs;


@Entity(tableName = Configs.subscriptionTableName)
public class Subscription {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "subscription_type")
    public String subscriptionType;

    //@param totalSize: subscription plan offered size
    @ColumnInfo(name = "total_size")
    public String totalSize;//in bytes

    //@param coveredSize: size covered by user backed up documents
    @ColumnInfo(name = "covered_size")
    public String coveredSize;//in bytes

    @ColumnInfo(name = "updated_at")
    public String updatedAt;


    public Subscription(){
        //empty constructor very important
    }

    public Subscription(String userId, String subscriptionType, String totalSize, String coveredSize, String updatedAt) {
        this.userId = userId;
        this.subscriptionType = subscriptionType;
        this.totalSize = totalSize;
        this.coveredSize = coveredSize;
        this.updatedAt = updatedAt;
    }
}
