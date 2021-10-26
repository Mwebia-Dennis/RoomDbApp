package com.penguinstech.cloudy.utils;

public enum AppSubscriptionPlans {
    //id, total size in bytes
    FREE("0", Util.convertMbToBytes(0)),
    BRONZE("1", Util.convertMbToBytes(5)),
    SILVER("2", Util.convertMbToBytes(10)),
    GOLD("3", Util.convertMbToBytes(15));

    private final String key;
    private final long value;

    AppSubscriptionPlans(String key, long value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }
    public long getValue() {
        return value;
    }


}
