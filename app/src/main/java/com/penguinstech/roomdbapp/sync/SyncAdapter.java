package com.penguinstech.roomdbapp.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.penguinstech.roomdbapp.AuthenticacteActivity;
import com.penguinstech.roomdbapp.MainActivity;
import com.penguinstech.roomdbapp.controller.TaskController;
import com.penguinstech.roomdbapp.room_db.AppDatabase;
import com.penguinstech.roomdbapp.room_db.TaskDao;
import com.penguinstech.roomdbapp.room_db.Token;
import com.penguinstech.roomdbapp.utils.Configs;
import com.penguinstech.roomdbapp.room_db.Task;
import com.penguinstech.roomdbapp.utils.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        if (!Util.getUserName(context).equals("")) {

            String[] listOfTables = new String[] {Configs.tableName};
            for (String tableName: listOfTables) {

                syncData(tableName);
            }
        }





    }


    private void syncData(String tableName) {

        /**
         *
         * retrieve the the last sync token
         * compare with servers last sync
         * check which data has been affected and compare which is the latest and update.
         * update last sync
         *
         */

        //get token from room
        Token token = localDatabase.tokenDao().loadLastSyncToken();
        //retrieve firebase last sync from firebase database

        firebaseDatabase.child(Util.getUserName(context)).child(tableName).child("last_sync_token").get().addOnSuccessListener(dataSnapshot -> {

            if (dataSnapshot.exists() && token != null){
                //check if the current client was the last one to update the server
                Token firebaseToken = dataSnapshot.getValue(Token.class);
                Log.d("firebase device", firebaseToken.deviceId);
                Log.d("local device", firebaseToken.deviceId);
                Log.d("ids", (token.deviceId.trim().equals(firebaseToken.deviceId.trim()))?"true":"false");
                if(token.deviceId.trim().equals(firebaseToken.deviceId.trim())){
                    //update server
                        if (tableName.equals(Configs.tableName)) {
                        new TaskController(context, localDatabase).saveDataToFirestore(false, token.lastSync);
                    }
                }else {

                    //compare both  local to and from firestore data and update
                    if (tableName.equals(Configs.tableName)) {
                        new TaskController(context, localDatabase).compareRoomToFirestoreData(token.lastSync);
                    }

                }



            }else if (dataSnapshot.exists() && token == null) {
                //local db has no data
                //retrieve all data from firestore and  update the local db with data from firebase
                if (tableName.equals(Configs.tableName)) {
                    new TaskController(context, localDatabase).syncAllDataFromFirestore();
                }

            }else if (!dataSnapshot.exists() && token != null) {
                //firestore is emmpty. update with room data.
                if (tableName.equals(Configs.tableName)) {
                    new TaskController(context, localDatabase).saveDataToFirestore(true, "");
                }

            }else {

                //if both are null then user has no data.
                Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.tableName);
            }

        }).addOnFailureListener(e -> {

            Log.d("firebase token", "failed");
        });



    }



}