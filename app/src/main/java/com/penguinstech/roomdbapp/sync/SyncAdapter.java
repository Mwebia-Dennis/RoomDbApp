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
import com.penguinstech.roomdbapp.AddTaskActivity;
import com.penguinstech.roomdbapp.AppDatabase;
import com.penguinstech.roomdbapp.Configs;
import com.penguinstech.roomdbapp.MainActivity;
import com.penguinstech.roomdbapp.R;
import com.penguinstech.roomdbapp.Task;
import com.penguinstech.roomdbapp.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    ContentResolver contentResolver;
    FirebaseFirestore db;
    AppDatabase localDatabase;
    Boolean hasDataLoaded = false;
    Context context;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
//        android.os.Debug.waitForDebugger();

        contentResolver = context.getContentResolver();
        FirebaseApp.initializeApp(context);
        db = FirebaseFirestore.getInstance();
        localDatabase = Room.databaseBuilder(context,
                AppDatabase.class, Configs.DatabaseName).build();
        this.context = context;
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
        Log.i("perfomingSync", "True");
        Log.i("Sync results", String.valueOf(allTasks.size()));
        if(allTasks.size() > 0) {
            backupToFirestore();
        }else {
            syncAllNotesFromFirestore();
        }





    }


    public void syncAllNotesFromFirestore() {
        db.collection(Util.getUserName(context)).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("firestore onSuccess", "LIST EMPTY");
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

                    Log.d("firestore onFailure", ": True");
                });
    }

    public void backupToFirestore () {

        /**
         * get the last inserted data in firebase by ordering by date field
         * get the last inserted data in local database
         * if both match return
         * else update
         */

        List<Task> localLatestTask = localDatabase.taskDao().getLatestTask();
        Log.d("latestTask", localLatestTask.get(0).title);
        db.collection(Util.getUserName(context))
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {

            if (!queryDocumentSnapshots.isEmpty()) {
                List<Task> lastTask = queryDocumentSnapshots.toObjects(Task.class);
                Task task = lastTask.get(0);
//                Log.d("last task", task.title);
                try {

                    Date localLatestTaskDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)
                            .parse(localLatestTask.get(0).updatedAt);
                    Date firestoreLatestTaskDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)
                            .parse(task.updatedAt);

                    if(localLatestTaskDate.after(firestoreLatestTaskDate)) {
                        //update firebase
                        //get all data that is not backed up ie date after @param firestore Last Task Date
                        Log.d("firestore", "not updated");
                        saveDataToFirestore(false, task.updatedAt);

                    }else if(firestoreLatestTaskDate.after(localLatestTaskDate)) {
                        //update the local database
//                        saveDataToLocalDb(localLatestTaskDate.toString());
                        //todo: check if user has deleted task from local db
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }


            }else {
                //no backed up data so update the whole firestore
                saveDataToFirestore(true, "");
                Log.d("backup data", "0");
            }

        })
                .addOnFailureListener(e -> {

                    Log.d("firestore: retrieving", " last item failed");
                });

    }

    private void saveDataToLocalDb(String localLatestDate) {
        db.collection(Util.getUserName(context))
                .whereGreaterThan("updatedAt", localLatestDate)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (!queryDocumentSnapshots.isEmpty()) {


                        List<Task> backedUpTasks = queryDocumentSnapshots.toObjects(Task.class);
                        Log.d("size", String.valueOf(backedUpTasks.size()));
                        Util.saveDataToRoomDb(localDatabase.taskDao(), backedUpTasks);
                    }

                })
                .addOnFailureListener(e->{
                    e.printStackTrace();
                   Log.d("Sync", "failed");
                });
    }

    private void saveDataToFirestore(boolean isAllData, String updatedAt) {

        new Thread() {
            @Override
            public void run() {

                List<Task> newTasks = isAllData?localDatabase.taskDao().getAll():localDatabase.taskDao().filterByDate(updatedAt);
                for (Task task: newTasks) {
                    db.collection(Util.getUserName(context))
                            .add(task)
                            .addOnSuccessListener(documentReference -> {
                                Log.d("backing Data", "Successful");
                            })
                            .addOnFailureListener(e -> {
                                Log.d("backing Data", "Failed");
                            });
                }
            }
        }.start();


    }

}