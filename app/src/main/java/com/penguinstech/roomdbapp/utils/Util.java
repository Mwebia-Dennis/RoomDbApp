package com.penguinstech.roomdbapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.penguinstech.roomdbapp.room_db.Subscription;
import com.penguinstech.roomdbapp.room_db.SubscriptionDao;
import com.penguinstech.roomdbapp.room_db.Task;
import com.penguinstech.roomdbapp.room_db.TaskDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Util {

    public static void login(Context context, String username) {
        SharedPreferences.Editor editor = context.getSharedPreferences("userCredentials", Context.MODE_PRIVATE).edit();
        editor.putString("user_name", username);
        editor.apply();
    }

    public static String getUserName(Context context){
        return context.getSharedPreferences("userCredentials", Context.MODE_PRIVATE).getString("user_name", "");
    }

    public static void saveDataToRoomDb(TaskDao taskDao, List<Task> task) {

        new Thread() {
            @Override
            public void run() {

                taskDao.insertAll(task);
                Log.d("Local db ", "all objects added to db");
            }
        }.start();
    }

    public static void saveSubscriptionToRoomDb(SubscriptionDao subscriptionDao, Subscription subscription) {

        new Thread() {
            @Override
            public void run() {

                List<Subscription> availableSubs = subscriptionDao.getAll();

                if (availableSubs.size() > 0) {
                    //there exists subscription so update
                    //updating id so as it can update in db
                    subscription.id = availableSubs.get(0).id;
                    subscriptionDao.update(subscription);
                }else{
                    //add new subscription to storage
                    List<Subscription> newSubs = new ArrayList<>();
                    newSubs.add(subscription);
                    subscriptionDao.insertAll(newSubs);
                }



            }
        }.start();
    }

    public static Task convertMapToTaskObject (Map<String, Object> taskMap) {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(taskMap);
        return gson.fromJson(jsonElement, Task.class);
    }


}