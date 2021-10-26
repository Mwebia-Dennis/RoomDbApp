package com.penguinstech.cloudy.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import androidx.room.Room;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.penguinstech.cloudy.controller.FileController;
import com.penguinstech.cloudy.controller.TaskController;
import com.penguinstech.cloudy.room_db.AppDatabase;
import com.penguinstech.cloudy.room_db.Subscription;
import com.penguinstech.cloudy.room_db.Token;
import com.penguinstech.cloudy.utils.Configs;
import com.penguinstech.cloudy.utils.Util;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    ContentResolver contentResolver;
    FirebaseFirestore db;//firestore instance
    DatabaseReference firebaseDatabase;//firebase realtime db
    AppDatabase localDatabase;//rooom db
    Context context;
    long limiter = 10;
    final int PAGINATOR  = 100;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */

        //debugger
//        android.os.Debug.waitForDebugger();

        contentResolver = context.getContentResolver();
        FirebaseApp.initializeApp(context);
        db = FirebaseFirestore.getInstance();
        localDatabase = Room.databaseBuilder(context,
                AppDatabase.class, Configs.DatabaseName).build();
        this.context = context;
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        contentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {

        Log.i("perfomingSync", "True");
        //ensure user id exists
        String userName = Util.getUserName(context);
        if (!userName.equals("")) {

            //check if user is subscribed
            firebaseDatabase.child(userName).child(Configs.subscriptionTableName)
                    .child("subscription_details").get().addOnSuccessListener(dataSnapshot -> {

                        if (dataSnapshot.exists()) {

                            Subscription subscription = dataSnapshot.getValue(Subscription.class);
                            String[] listOfTables = new String[] {Configs.tableName, Configs.filesTableName};
                            for (String tableName: listOfTables) {

                                syncData(tableName, subscription);
                            }



                        }
                    });


        }





    }


    private void syncData(String tableName, Subscription subscription) {

        /**
         *
         * retrieve the the last sync token
         * compare with servers last sync
         * check which data has been affected and compare which is the latest and update.
         * update last sync
         *
         */

        //retrieve firebase last sync from firebase database

        firebaseDatabase.child(Util.getUserName(context)).child(tableName).child("last_sync_token").get().addOnSuccessListener(dataSnapshot -> {

            new Thread(()->{

                //get token from room
                Token token = localDatabase.tokenDao().loadLastSyncToken(tableName);
                if (dataSnapshot.exists() && token != null){
                    //check if user has enough space in cloud
                    //give allowance of about 10mb.
                    if((Long.parseLong(subscription.totalSize) - Long.parseLong(subscription.coveredSize)) < Util.convertMbToBytes(limiter)) {

                        //check if the current client was the last one to update the server
                        Token firebaseToken = dataSnapshot.getValue(Token.class);
                        Log.d("firebase device", firebaseToken.deviceId);
                        Log.d("local device", firebaseToken.deviceId);
                        Log.d("ids", (token.deviceId.trim().equals(firebaseToken.deviceId.trim()))?"true":"false");
                        if(token.deviceId.trim().equals(firebaseToken.deviceId.trim())){
                            //update server
                            if (tableName.equals(Configs.tableName)) {
                                new TaskController(context, localDatabase).saveDataToFirestore(false, token.lastSync);
                            }else if (tableName.equals(Configs.filesTableName)) {
                                new FileController(context, localDatabase).saveDataToFirestore(false, token.lastSync);
                            }
                        }else {

                            //compare both  local to and from firestore data and update
                            if (tableName.equals(Configs.tableName)) {
                                new TaskController(context, localDatabase).compareRoomToFirestoreData(token.lastSync);
                            }else if (tableName.equals(Configs.filesTableName)) {
                                new FileController(context, localDatabase).compareRoomToFirestoreData(token.lastSync);
                            }

                        }
                    }



                }else if (dataSnapshot.exists() && token == null) {
                    //local db has no data
                    //retrieve all data from firestore and  update the local db with data from firebase
                    //dont check subscription
                    if (tableName.equals(Configs.tableName)) {
                        new TaskController(context, localDatabase).syncAllDataFromFirestore();
                    }else if (tableName.equals(Configs.filesTableName)) {
                        new FileController(context, localDatabase).syncAllDataFromFirestore();
                    }

                }else if (!dataSnapshot.exists() && token != null) {
                    //check if user has enough space in cloud
                    //give allowance of about 10mb.
                    if((Long.parseLong(subscription.totalSize) - Long.parseLong(subscription.coveredSize)) < Util.convertMbToBytes(limiter)) {

                        //firestore is emmpty. update with room data.
                        if (tableName.equals(Configs.tableName)) {
                            new TaskController(context, localDatabase).saveDataToFirestore(true, "");
                        }else if (tableName.equals(Configs.filesTableName)) {
                            new FileController(context, localDatabase).saveDataToFirestore(true, "");
                        }
                    }

                }else {

                    //if both are null then user has no data.
                    Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.tableName);
                    Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.filesTableName);
                }
            }).start();

        }).addOnFailureListener(e -> {

            Log.d("firebase token", "failed");
        });



    }



}