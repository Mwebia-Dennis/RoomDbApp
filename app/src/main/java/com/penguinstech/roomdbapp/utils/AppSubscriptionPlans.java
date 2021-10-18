package com.penguinstech.roomdbapp.utils;

public enum AppSubscriptionPlans {
    FREE("Free", 0), BRONZE("Bronze", 5), SILVER("Silver", 10), GOLD("Gold", 15);

    private final String key;
    private final Integer value;

    AppSubscriptionPlans(String key, Integer value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }
    public Integer getValue() {
        return value;
    }
}
