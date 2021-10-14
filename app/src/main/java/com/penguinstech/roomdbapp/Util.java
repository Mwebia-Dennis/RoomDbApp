package com.penguinstech.roomdbapp;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;
import java.util.Map;

public class Util {


    public Task convertMapToTaskObject (Map<String, String> taskMap) {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(taskMap);
        return gson.fromJson(jsonElement, Task.class);
    }
}
