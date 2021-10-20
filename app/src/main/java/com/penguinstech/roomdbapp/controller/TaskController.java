package com.penguinstech.roomdbapp.controller;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.penguinstech.roomdbapp.room_db.AppDatabase;
import com.penguinstech.roomdbapp.room_db.Task;
import com.penguinstech.roomdbapp.utils.Configs;
import com.penguinstech.roomdbapp.utils.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskController implements  MainController{
    Context context;
    AppDatabase localDatabase;
    FirebaseFirestore db;//firestore instance
    Boolean hasDataLoaded = false;// prevents duplicate data in db
    final int PAGINATOR  = 100;
    String tableName;
    public TaskController(Context context, AppDatabase localDatabase) {
        this.context = context;
        this.localDatabase = localDatabase;
        FirebaseApp.initializeApp(context);
        db = FirebaseFirestore.getInstance();
        tableName = Configs.tableName;
    }
    @Override
    public void syncAllDataFromFirestore() {

        db.collection(Util.getUserName(context)).document(tableName).collection(tableName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("firestore onSuccess", "LIST EMPTY");
                    } else {
                        // get all data and add to database
                        if (!hasDataLoaded){

                            //update accordingly per table
                            if (tableName.equals(Configs.tableName)){

                                //convert whole queryDocumentSnapshots to list
                                List<Task> backedUpTasks = queryDocumentSnapshots.toObjects(Task.class);

                                Log.d("size", String.valueOf(backedUpTasks.size()));
                                //save data to room
                                Util.saveDataToRoomDb(localDatabase.taskDao(), backedUpTasks);
                            }
//                            else if (tableName == Configs.tableName2){
//                                //repeat as the above if body but accessing the dao and class related to the current table
//                            }
                            //update last sync token
                            Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.tableName);
                            //notify the main ui thread
                            context.getContentResolver().notifyChange(Configs.URI_TASK, null, false);
                            hasDataLoaded = true;
                        }
                    }
                })
                .addOnFailureListener(e -> {

                    Log.d("firestore onFailure", ": True");
                });
    }

    @Override
    public void saveDataToFirestore(boolean isAllData, String updatedAt) {

        new Thread() {
            @Override
            public void run() {

                //get tasks from room db
                //get data in batches to avoid memory overload
                //update accordingly per table
                if (tableName.equals(Configs.tableName)){

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
                                                int value = task.isDeleted;
                                                Log.d("value", String.valueOf(value));
                                                if (docsList.size() > 0){
                                                    if (task.isDeleted == 1) {
                                                        //delete data from firebase
                                                        db.collection(Util.getUserName(context)).document(tableName).collection(tableName)
                                                                .document(docsList.get(0).getId()).delete()
                                                                .addOnSuccessListener(aVoid -> {

                                                                    Log.d("Deleting data", "successful");
                                                                });
                                                    }else {
                                                        updateServer(task, docsList.get(0).getId());
                                                    }
                                                }

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
//                else if(tableName.equals("calendars") {
//
//                    uploadCalendars(tableName,updatedAt,isAllData);
//                }

                Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.tableName);
            }
        }.start();


    }

    @Override
    public void compareRoomToFirestoreData(String tokenLastSync) {

        /**
         * get all data in local db that was updated after the lastSync date
         * get data in batches
         * check if there is similar data in firestore
         * if present update or delete the oldest task
         * get data in firestore that is not in local and add to local
         */

        new Thread(()->{

            //getting data in small batches to avoid filling the memory with data
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
                                                updateServer(task, doc.getId());
                                            }

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
                                //if not add to firebase
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
            compareFirestoreToRoomData(tokenLastSync);
            Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.tableName);

        }).start();
    }

    @Override
    public void compareFirestoreToRoomData( String tokenLastSync) {

        /**
         * get all data that is in firestore and not in local and update local
         * get all data from firestore with update date greater than token last sync
         * loop each item
         * check if item exists in room
         * if it doesnt add to local
         * since @function compareRoomToFirestoreData updates both firebase and local when data exists on both, then no need to update again
         */

        new Thread(()->{
            //getting data in small batches to avoid filling the memory with data
            final List<Integer> batchSizes = new ArrayList<>();
            batchSizes.add(PAGINATOR);
            final List<String> lastSyncDates = new ArrayList<>();
            lastSyncDates.add(tokenLastSync);
            //if the results from firebase are less than 100, then it means there is no more data.
            boolean hasData = true;
            while (hasData) {


                db.collection(Util.getUserName(context)).document(tableName).collection(tableName)
                        .whereGreaterThan("updatedAt",lastSyncDates.get(lastSyncDates.size()-1))
                        .limit(PAGINATOR)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {

                            if (!queryDocumentSnapshots.isEmpty()) {


                                List<Task> backedUpTasks = queryDocumentSnapshots.toObjects(Task.class);
                                List<Task> newTasksFromFirestoreList = new ArrayList<>();
                                for (Task task : backedUpTasks) {
                                    if(localDatabase.taskDao().loadTaskById(task.id) == null){
                                        //get all tasks in firestore but not in local
                                        newTasksFromFirestoreList.add(task);
                                    }
                                }
                                //insert to database
                                if(newTasksFromFirestoreList.size() > 0)localDatabase.taskDao().insertAll(newTasksFromFirestoreList);
                                //update last sync for counter
                                lastSyncDates.add(backedUpTasks.get(backedUpTasks.size()-1).updatedAt);
                                //update our counter checker
                                batchSizes.add(newTasksFromFirestoreList.size());
                            }
                        });


                hasData = (batchSizes.get(batchSizes.size() - 1) > 0);
            }


            Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.tableName);

        }).start();
    }

    @Override
    public void updateServer(Object item, String docId) {
        //add data to map and update firestore
        Task task = (Task)item;
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
