package com.penguinstech.roomdbapp.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.penguinstech.roomdbapp.AppDatabase;
import com.penguinstech.roomdbapp.Configs;
import com.penguinstech.roomdbapp.MainActivity;
import com.penguinstech.roomdbapp.R;
import com.penguinstech.roomdbapp.Task;
import com.penguinstech.roomdbapp.Util;

import java.util.List;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    ContentResolver contentResolver;
    FirebaseFirestore db;
    AppDatabase localDatabase;
    Boolean hasDataLoaded = false;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        contentResolver = context.getContentResolver();
        FirebaseApp.initializeApp(context);
        db = FirebaseFirestore.getInstance();
        localDatabase = Room.databaseBuilder(context,
                AppDatabase.class, Configs.DatabaseName).build();
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

        //check if database is empty
        //if empty, load data from firebase if any.
        //if there is data in database, compare if its upto to date with server and update as necessary
        List<Task> allTasks = localDatabase.taskDao().getAll();
        Log.i("sfdsfds", "sdfsfdd");
        if(allTasks.size() > 0) {
            backupToFirestore();
        }else {
            getNotesFromFirestore();
        }





    }


    public void getNotesFromFirestore() {
        db.collection(Configs.userId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("from firestore", "onSuccess: LIST EMPTY");
                    } else {
                        // get all data and add to database
                        if (!hasDataLoaded){

                            List<Task> backedUpTasks = queryDocumentSnapshots.toObjects(Task.class);

                            Log.d("size", String.valueOf(backedUpTasks.size()));
                            Util.saveDataToRoomDb(localDatabase.taskDao(), backedUpTasks);
                            getContext().getContentResolver().notifyChange(Configs.URI_TASK, null, false);
                            hasDataLoaded = true;
                        }
                    }
                })
                .addOnFailureListener(e -> {

                    Log.d("from firestore", "onFailure: True");
                });
    }

    public void backupToFirestore () {

        /**
         * get the last inserted data in firebase by ordering by date field
         * get the last inserted data in local database
         * if both match return
         * else update
         */

        db.collection(Configs.userId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {

            if (!queryDocumentSnapshots.isEmpty()) {
                List<Task> lastTask = queryDocumentSnapshots.toObjects(Task.class);
                Task task = lastTask.get(0);
                Log.d("last task", task.title);
            }

        })
                .addOnFailureListener(e -> {

                    Log.d("firestore: retrieving", " last item failed");
                });

    }
}