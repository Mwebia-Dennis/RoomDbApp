package com.penguinstech.cloud_syncer.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.penguinstech.cloud_syncer.room_db.AppDatabase;
import com.penguinstech.cloud_syncer.room_db.Subscription;
import com.penguinstech.cloud_syncer.room_db.SubscriptionDao;
import com.penguinstech.cloud_syncer.room_db.Task;
import com.penguinstech.cloud_syncer.room_db.TaskDao;
import com.penguinstech.cloud_syncer.room_db.Token;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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



    public static void updateToken(Context context, ContentResolver contentResolver, AppDatabase localDatabase, String tableName) {

        DatabaseReference firebaseDatabase;//firebase realtime db
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        //update both local and firestore last sync date
        String deviceId= Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
        Token newToken = new Token(deviceId, tableName, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date()));

        firebaseDatabase.child(Util.getUserName(context)).child(tableName).child("last_sync_token").setValue(newToken).addOnSuccessListener(aVoid -> {

            Log.d("firebasse token", "updated successfully");
        });
        new Thread() {
            @Override
            public void run() {

                List<Token> listOfTokens = new ArrayList<>();
                listOfTokens.add(newToken);
                localDatabase.tokenDao().insertAll(listOfTokens);
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


    public static String getPath(Context context, Uri uri ) {
        String result = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver( ).query( uri, proj, null, null, null );
        if(cursor != null){
            if ( cursor.moveToFirst( ) ) {
                int column_index = cursor.getColumnIndexOrThrow( proj[0] );
                result = cursor.getString( column_index );
            }
            cursor.close( );
        }
        return result;
    }


    public static Task convertMapToTaskObject (Map<String, Object> taskMap) {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(taskMap);
        return gson.fromJson(jsonElement, Task.class);
    }





}
