package com.penguinstech.roomdbapp;

import android.net.Uri;

public interface Configs {
    String DatabaseName = "LocalAppDb";
    String tableName = "task";
    String subscriptionTableName = "subscriptions";
    String syncTableName = "sync_info";
    String AUTHORITY = "com.penguinstech.roomdbapp.provider";
    String ACCOUNT_TYPE = "penguinstech.com";
    String ACCOUNT = "com.penguinstech.roomdbapp";
    Uri URI_TASK = Uri.parse(
            "content://" + Configs.AUTHORITY + "/" + Configs.tableName);
}
