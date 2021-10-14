package com.penguinstech.roomdbapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Util {


    public static void saveDataToRoomDb(TaskDao taskDao, List<Task> task) {

        new Thread() {
            @Override
            public void run() {

                taskDao.insertAll(task);
                Log.d("Local db ", "all objects added to db");
            }
        }.start();
    }

    public static Task convertMapToTaskObject (Map<String, String> taskMap) {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(taskMap);
        return gson.fromJson(jsonElement, Task.class);
    }

}
