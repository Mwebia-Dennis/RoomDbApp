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
import com.penguinstech.roomdbapp.room_db.AppDatabase;
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
    Boolean hasDataLoaded = false;// prevents duplicate data in db
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

        //check if database is empty
        //if empty, load data from firebase if any.
        //if there is data in database, compare if its upto to date with server and update as necessary
//        List<Task> allTasks = localDatabase.taskDao().getAll();
        Log.i("perfomingSync", "True");
//        Log.i("Sync results", String.valueOf(allTasks.size()));
//        if(allTasks.size() > 0) {
//            backupToFirestore();
//        }else {
//            syncAllNotesFromFirestore();
//        }

        //ensure user id exists
        if (!Util.getUserName(context).equals("")) {
            syncData(Configs.tableName);
        }





    }


    public void syncAllNotesFromFirestore(String tableName) {
        db.collection(Util.getUserName(context)).document(tableName).collection(tableName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("firestore onSuccess", "LIST EMPTY");
                    } else {
                        // get all data and add to database
                        if (!hasDataLoaded){

                            //convert whole queryDocumentSnapshots to list
                            List<Task> backedUpTasks = queryDocumentSnapshots.toObjects(Task.class);

                            Log.d("size", String.valueOf(backedUpTasks.size()));
                            //save data to room
                            Util.saveDataToRoomDb(localDatabase.taskDao(), backedUpTasks);
                            //update last sync token
                            updateToken(Configs.tableName);
                            //notify the main ui thread
                            getContext().getContentResolver().notifyChange(Configs.URI_TASK, null, false);
                            hasDataLoaded = true;
                        }
                    }
                })
                .addOnFailureListener(e -> {

                    Log.d("firestore onFailure", ": True");
                });
    }
//
//    public void backupToFirestore () {
//
//        /**
//         * get the last inserted data in firebase by ordering by date field
//         * get the last inserted data in local database
//         * if both match return
//         * else update
//         */
//
//        //@var localLatestTask -> the latest item in room db
////        List<Task> localLatestTask = localDatabase.taskDao().getLatestTask();
////        Log.d("latestTask", localLatestTask.get(0).title);
////        //query firebase to get latest added item
////        db.collection(Util.getUserName(context))
////                .orderBy("updatedAt", Query.Direction.DESCENDING)
////                .limit(1)
////                .get().addOnSuccessListener(queryDocumentSnapshots -> {
////
////            if (!queryDocumentSnapshots.isEmpty()) {
////                //firebase results
////                List<Task> lastTask = queryDocumentSnapshots.toObjects(Task.class);
////                //get the item
////                Task task = lastTask.get(0);
//////                Log.d("last task", task.title);
////                try {
////
////                    //convert the items date from string to date
////                    Date localLatestTaskDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)
////                            .parse(localLatestTask.get(0).updatedAt);
////                    Date firestoreLatestTaskDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)
////                            .parse(task.updatedAt);
////
////                    //check if room DB is ahead of firestore
////                    if(localLatestTaskDate.after(firestoreLatestTaskDate)) {
////                        //update firebase
////                        //get all data that is not backed up ie date after @param firestore Last Task Date
////                        Log.d("firestore", "not updated");
////                        saveDataToFirestore(false, task.updatedAt);
////
////                    }else if(firestoreLatestTaskDate.after(localLatestTaskDate)) {
////                        //update the local database
//////                        saveDataToLocalDb(localLatestTaskDate.toString());
////                        //todo: check if user has deleted task from local db
////                    }
////                } catch (ParseException e) {
////                    e.printStackTrace();
////                }
////
////
////            }else {
////                //no backed up data so update the whole firestore
////                saveDataToFirestore(true, "");
////                Log.d("backup data", "0");
////            }
////
////        })
////                .addOnFailureListener(e -> {
////
////                    Log.d("firestore: retrieving", " last item failed");
////                });
//
//    }


    private void saveDataToFirestore(String tableName, boolean isAllData, String updatedAt) {

        new Thread() {
            @Override
            public void run() {

                //get tasks from room db that are not
                //get data in batches to avoid memory overload

                //get count of data to be stored in firestore

                int totalNewData = isAllData?localDatabase.taskDao().getCount():localDatabase.taskDao().filterByDateCount(updatedAt);
                Log.d("totalNewData", String.valueOf(totalNewData));
                int batchSize = 0;
                while (batchSize <= totalNewData){

                    //get first 100
                    List<Task> newTasks = isAllData?localDatabase.taskDao()
                            .getAll():localDatabase.taskDao().filterByDate(updatedAt, PAGINATOR, batchSize);
                    //upload items to firestore so as data matches the room data
                    for(Task task: newTasks) {

                        //if @param isAllData = true: send all data to server
                        //else
                        //check if doc is in server and update
                        if(isAllData) {
                            db.collection(Util.getUserName(context))
                                    .document(tableName).collection(tableName)
                                    .add(task)
                                    .addOnSuccessListener(documentReference -> {
                                        Log.d("backing Data", "Successful");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.d("backing Data", "Failed");
                                    });
                        }else {
                            db.collection(Util.getUserName(context)).document(tableName).collection(tableName)
                                    .whereEqualTo("id", task.id)
                                    .get().addOnSuccessListener(queryDocumentSnapshots -> {

                                        //check if the document exists
                                        if (!queryDocumentSnapshots.isEmpty()) {

                                            //update server
                                            List<DocumentSnapshot> docsList = queryDocumentSnapshots.getDocuments();
                                            if (docsList.size() > 0)updateServer(task, tableName, docsList.get(0).getId());

                                        } else {
                                            db.collection(Util.getUserName(context))
                                                    .document(tableName).collection(tableName)
                                                    .add(task)
                                                    .addOnSuccessListener(documentReference -> {
                                                        Log.d("backing Data", "Successful");
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.d("backing Data", "Failed");
                                                    });
                                        }
                                    }
                            );
                        }


                    }
                    batchSize += PAGINATOR;
                }

            }
        }.start();


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

        firebaseDatabase.child(tableName).child("last_sync_token").get().addOnSuccessListener(dataSnapshot -> {

            if (dataSnapshot.exists() && token != null){
                //check if the current client was the last one to update the server
                Token firebaseToken = dataSnapshot.getValue(Token.class);
                Log.d("firebase device", firebaseToken.deviceId);
                Log.d("local device", firebaseToken.deviceId);
                Log.d("ids", (token.deviceId.trim().equals(firebaseToken.deviceId.trim()))?"true":"false");
                if(token.deviceId.trim().equals(firebaseToken.deviceId.trim())){
                    //update server
                    saveDataToFirestore(tableName, false, token.lastSync);
                    updateToken(tableName);
                }else {

                    //compare both  local and firestore data and update

                    compareRoomAndFirestoreData(tableName, token.lastSync, firebaseToken.lastSync);
                }



            }else if (dataSnapshot.exists() && token == null) {
                //local db has no data
                //retrieve all data from firestore and  update the local db with data from firebase
                syncAllNotesFromFirestore(tableName);

            }else if (!dataSnapshot.exists() && token != null) {
                //firestore is emmpty. update with room data.
                saveDataToFirestore(tableName, true, "");
                updateToken(tableName);

            }else {

                //if both are null then user has no data.
                updateToken(tableName);
            }

        }).addOnFailureListener(e -> {

           Log.d("firebase token", "failed");
        });



    }

    private void compareRoomAndFirestoreData(String tableName, String tokenLastSync, String firebaseTokenlastSync) {

        /**
         * get all data in local db that was updated after the lastSync date
         * get data in batches
         * check if there is similar data in firestore
         * if present update or delete the oldest task
         * get all data that is in firestore and not in local and update local
         */

        new Thread(()->{

            //paginating to avoid filling the memory with data
            int totalNewData = localDatabase.taskDao().filterByDateCount(tokenLastSync);
            int batchSize = 0;
            while (batchSize <= totalNewData) {

                List<Task> listOfRoomTasks = localDatabase.taskDao().filterByDate(tokenLastSync, PAGINATOR, batchSize);
                if (listOfRoomTasks.size() > 0) {

                    for (Task task : listOfRoomTasks) {
                        db.collection(Util.getUserName(context)).document(tableName).collection(tableName)
                                .whereEqualTo("id", task.id)
                                .get().addOnSuccessListener(queryDocumentSnapshots -> {

                            //check if the document exists
                            if (!queryDocumentSnapshots.isEmpty()) {

                                List<DocumentSnapshot> documentSnapshots = queryDocumentSnapshots.getDocuments();
                                for (DocumentSnapshot doc : documentSnapshots) {

                                    //convert map to  task object
                                    Task firestoreTask = Util.convertMapToTaskObject(doc.getData());
                                    try {
                                        //convert the items date from string to date
                                        Date localTaskDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)
                                                .parse(task.updatedAt);
                                        Date firestoreTaskDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)
                                                .parse(firestoreTask.updatedAt);

                                        //check if local date is after the firestore date
                                        if (localTaskDate.after(firestoreTaskDate)) {
                                            //update firebase
                                            //if deleted is true then delete from firebase
                                            if (task.isDeleted == 1) {

                                                //delete data from firebase
                                                db.collection(Util.getUserName(context)).document(tableName).collection(tableName)
                                                        .document(doc.getId()).delete()
                                                        .addOnSuccessListener(aVoid -> {

                                                            Log.d("Deleting data", "successful");
                                                        });

                                            } else {
                                                //add data to map and update firestore
                                                updateServer(task, tableName, doc.getId());                                            }

                                        } else if (firestoreTaskDate.after(localTaskDate)) {//is firestore after the local updated date
                                            //update the local database
                                            new Thread(() -> {
                                                localDatabase.taskDao().update(firestoreTask);
                                            }).start();
                                        }
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                }


                            }else {
                                ///doc does not exist in firebase
                                //check if it was deleted
                                //if not add to firebaase
                                if(task.isDeleted  != 1) {
                                    db.collection(Util.getUserName(context))
                                            .add(task)
                                            .addOnSuccessListener(documentReference -> {
                                                Log.d("firebase", "updated succesfully");
                                            });
                                }
                            }
                        });

                    }
                }
                batchSize += PAGINATOR;

            }
            updateToken(Configs.tableName);

        }).start();




//        db.collection(Util.getUserName(context))
//                .whereEqualTo("updatedAt", tokenLastSync)
//                .get().addOnSuccessListener(queryDocumentSnapshots -> {
//
//            if (!queryDocumentSnapshots.isEmpty()) {
//                //firebase results
//                List<Task> listOfFirestoreTasks = queryDocumentSnapshots.toObjects(Task.class);
//                //get the item
//            }
//        });
    }


    private void updateToken(String tableName) {

        String deviceId= Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
        Token newToken = new Token(deviceId, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date()));

        firebaseDatabase.child(tableName).child("last_sync_token").setValue(newToken).addOnSuccessListener(aVoid -> {

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

    private void updateServer(Task task, String tableName, String docId) {
        //add data to map and update firestore
        Map<String, Object> newData = new HashMap<>();
        newData.put("title", task.title);
        newData.put("description", task.description);
        newData.put("id", task.id);
        newData.put("updatedAt", task.updatedAt);
        db.collection(Util.getUserName(context))
                .document(tableName).collection(tableName)
                .document(docId).update(newData).addOnSuccessListener(aVoid -> {
            Log.d("firebase", "updated succesfully");
        });
    }

}