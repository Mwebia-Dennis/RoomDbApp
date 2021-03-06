package com.penguinstech.cloudy.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import com.penguinstech.cloudy.room_db.AppDatabase;
import com.penguinstech.cloudy.room_db.Files;
import com.penguinstech.cloudy.room_db.Subscription;
import com.penguinstech.cloudy.room_db.SubscriptionDao;
import com.penguinstech.cloudy.room_db.Task;
import com.penguinstech.cloudy.room_db.TaskDao;
import com.penguinstech.cloudy.room_db.Token;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Util {

    public static void login(Context context, String username) {
        SharedPreferences.Editor editor = context.getSharedPreferences("userCredentials", Context.MODE_PRIVATE).edit();
        editor.putString("user_name", username);
        editor.apply();
    }

    public static String getUserName(Context context){
        return context.getSharedPreferences("userCredentials", Context.MODE_PRIVATE).getString("user_name", "");
    }

    public static void saveDataToRoomDb(TaskDao taskDao, List<Task> taskList) {

        new Thread() {
            @Override
            public void run() {

                //remove duplicates
                Set<Task> set = new LinkedHashSet<>(taskList);
                taskList.clear();
                taskList.addAll(set);
                //add to database
                taskDao.insertAll(taskList);
                Log.d("Local db ", "all objects added to db");
            }
        }.start();
    }



    public static void updateToken(Context context, ContentResolver contentResolver, AppDatabase localDatabase, String tableName, String date) {

        DatabaseReference firebaseDatabase;//firebase realtime db
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        //update both local and firestore last sync date
        String deviceId= Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
        if (date.equals("")) {
            date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date());
        }
        Token newToken = new Token(deviceId, tableName, date);

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

    public static  void setNewSubscription(AppDatabase localDatabase, String userName){

        new Thread(()->{

            DatabaseReference firebaseDatabase = FirebaseDatabase.getInstance().getReference();
            DatabaseReference ref = firebaseDatabase.child(userName).child(Configs.subscriptionTableName)
                    .child("subscription_details");
            ref.get().addOnSuccessListener(dataSnapshot -> {

                if (!dataSnapshot.exists()){
                    Subscription subscription = new Subscription(
                            userName,
                            "",
                            AppSubscriptionPlans.FREE.getKey(),
                            "FREE",
                            String.valueOf(AppSubscriptionPlans.FREE.getValue()),
                            "0",
                            "",
                            "",
                            "",
                            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date()));
                    ref.setValue(subscription).addOnSuccessListener(aVoid -> {
                        new Thread(()->{

                            List<Subscription> list = new ArrayList<>();
                            list.add(subscription);
                            localDatabase.subscriptionDao().insertAll(list);

                        }).start();
                    });
                }else {
                    Subscription subscription = dataSnapshot.getValue(Subscription.class);
                    new Thread(()->{
                        List<Subscription> list = new ArrayList<>();
                        list.add(subscription);
                        localDatabase.subscriptionDao().insertAll(list);
                    }).start();
                }
            });
        }).start();
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

    public static void redirectToLink(Context context, String url) {
        Intent viewIntent =
                new Intent("android.intent.action.VIEW",
                        Uri.parse(url)
                );
        context.startActivity(viewIntent);
    }


    public static Task convertMapToTaskObject (Map<String, Object> taskMap) {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.toJsonTree(taskMap);
        return gson.fromJson(jsonElement, Task.class);
    }

    public static long getPlanTotalSize(String planId) {

        long totalSize = 0;
        AppSubscriptionPlans[] appSubscriptionPlans = {AppSubscriptionPlans.FREE, AppSubscriptionPlans.BRONZE,
                AppSubscriptionPlans.SILVER,AppSubscriptionPlans.GOLD};
        for (AppSubscriptionPlans plan:appSubscriptionPlans){

            if (plan.getKey().equals(planId)) {
                totalSize = plan.getValue();
                break;
            }
        }
        return totalSize;
    }

    public static String subtractFiveMinutes(String currentDate) {
        Calendar cal = Calendar.getInstance();
        String date = currentDate;
        try {
            SimpleDateFormat format =new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
            cal.setTime(format.parse(currentDate));
            cal.add(Calendar.MINUTE, -5);
            date = format.format(cal.getTime());

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;

    }

    public static long convertMbToBytes(long sizeInMb) {
        return sizeInMb * 1024*1024;
    }
    public static long convertBytesToMb(long sizeInBytes) {
        return sizeInBytes / (1024*1024);
    }
    public static long convertGbToBytes(long sizeInMb) {
        return sizeInMb * 1024*1024*1024;
    }



}
