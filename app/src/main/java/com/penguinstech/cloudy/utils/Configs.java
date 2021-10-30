package com.penguinstech.cloudy.utils;

import android.net.Uri;

public interface Configs {

    String DatabaseName = "LocalAppDb";
    String tableName = "task";
    String subscriptionTableName = "subscriptions";
    String syncTableName = "sync_info";
    String tokensTableName = "tokens";
    String filesTableName = "files";
    String AUTHORITY = "com.penguinstech.cloudy.provider";
    String APP_PACKAGE = "com.penguinstech.cloudy";
    Uri URI_TASK = Uri.parse(
            "content://" + Configs.AUTHORITY + "/" + Configs.tableName);
}
