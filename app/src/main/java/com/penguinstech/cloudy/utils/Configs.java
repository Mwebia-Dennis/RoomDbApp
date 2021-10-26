package com.penguinstech.cloudy.utils;

import android.net.Uri;

public interface Configs {

    String licenseKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArB7hQyyL3hQ5mYPai8ZH+SqXcKrZmhZLptjIINvQOiVJnxwAXjBCySai/I4XcdRtCuLCvN9yPtRxi+wCaUo2gcB4giRAgthH5Cp1OJk2kWhXufQ1vfduZdEI2bvGPqwCzmwci+ZU3RO1Ws6LTTkj2Dl/e0SCZM0v/kl4K9xrYm18yrAhf+TVjp5Mni313itIK7rGVzx/fTs2BxJEHSjr7yPO0NILoSxXtl4QseXw4IuYXiW1dWH+PGoeErJBACNMzuTelaLtb+S5di/unykddf1CexEIN01TQ6WL8w9brAdEo9kpLzWZ+6wnHThquAnRaYBn4Jh5IIghuhYUQXYHOQIDAQAB";
    String DatabaseName = "LocalAppDb";
    String tableName = "task";
    String subscriptionTableName = "subscriptions";
    String syncTableName = "sync_info";
    String tokensTableName = "tokens";
    String filesTableName = "files";
    String AUTHORITY = "com.penguinstech.cloudy.provider";
    String ACCOUNT_TYPE = "penguinstech.com";
    String ACCOUNT = "com.penguinstech.cloudy";
    Uri URI_TASK = Uri.parse(
            "content://" + Configs.AUTHORITY + "/" + Configs.tableName);
}
