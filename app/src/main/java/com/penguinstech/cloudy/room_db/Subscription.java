package com.penguinstech.cloudy.room_db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.penguinstech.cloudy.utils.Configs;


@Entity(tableName = Configs.subscriptionTableName)
public class Subscription {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "subscription_store_id")
    public String subscriptionStoreId;

    @ColumnInfo(name = "subscription_type")
    public String subscriptionType;

    //@param totalSize: subscription plan offered size
    @ColumnInfo(name = "total_size")
    public String totalSize;//in bytes

    //@param coveredSize: size covered by user backed up documents
    @ColumnInfo(name = "covered_size")
    public String coveredSize;//in bytes

    @ColumnInfo(name = "order_id")
    public String orderId;

    @ColumnInfo(name = "payment_status")
    public String paymentStatus;

    @ColumnInfo(name = "purchase_time")
    public String purchaseTime;

    @ColumnInfo(name = "purchase_token")
    public String purchaseToken;

    @ColumnInfo(name = "updated_at")
    public String updatedAt;



    public Subscription(){
        //empty constructor very important
    }

    public Subscription(String userId, String orderId,String subscriptionStoreId, String subscriptionType, String totalSize,
                        String coveredSize, String paymentStatus,String purchaseTime, String purchaseToken, String updatedAt) {
        this.userId = userId;
        this.orderId = orderId;
        this.subscriptionStoreId = subscriptionStoreId;
        this.subscriptionType = subscriptionType;
        this.paymentStatus = paymentStatus;
        this.purchaseTime = purchaseTime;
        this.purchaseToken = purchaseToken;
        this.totalSize = totalSize;
        this.coveredSize = coveredSize;
        this.updatedAt = updatedAt;
    }
}
