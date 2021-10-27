package com.penguinstech.cloudy.controller;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.penguinstech.cloudy.room_db.AppDatabase;
import com.penguinstech.cloudy.room_db.Task;
import com.penguinstech.cloudy.utils.Configs;
import com.penguinstech.cloudy.utils.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class  TaskController implements  MainController{
    Context context;
    AppDatabase localDatabase;
    FirebaseFirestore db;//firestore instance
    Boolean hasDataLoaded = false;// prevents duplicate data in db
    final int PAGINATOR  = 100;
    String tableName;
    SubscriptionController subscriptionController;

    public TaskController(Context context, AppDatabase localDatabase) {
        this.context = context;
        this.localDatabase = localDatabase;
        FirebaseApp.initializeApp(context);
        db = FirebaseFirestore.getInstance();
        tableName = Configs.tableName;
        subscriptionController = new SubscriptionController(context, localDatabase);
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
                                //update last sync token
                                Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.tableName);
                                //notify the main ui thread
                                context.getContentResolver().notifyChange(Configs.URI_TASK, null, false);
                            }
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
                                            //update space covered in server
                                            subscriptionController.updateCoveredSize(documentReference, true);
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
                                                        //update covered size
                                                        subscriptionController.updateCoveredSize(
                                                                db.collection(Util.getUserName(context)).document(tableName).collection(tableName).document(docsList.get(0).getId())
                                                                , false);
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
                                                            subscriptionController.updateCoveredSize(documentReference, true);
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

                                                //update covered size
                                                subscriptionController.updateCoveredSize(
                                                        db.collection(Util.getUserName(context)).document(tableName).collection(tableName).document(doc.getId())
                                                        , false);
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

                                                subscriptionController.updateCoveredSize(documentReference, true);
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

            db.collection(Util.getUserName(context)).document(tableName).collection(tableName)
                    .whereGreaterThan("updatedAt",tokenLastSync)
                    .limit(PAGINATOR)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {

                        if (!queryDocumentSnapshots.isEmpty()) {


                            new Thread(()->{

                                List<DocumentSnapshot> documentSnapshots = queryDocumentSnapshots.getDocuments();
                                List<Task> newTasksFromFirestoreList = new ArrayList<>();
                                for (DocumentSnapshot doc : documentSnapshots) {
                                    //convert map to  task object
                                    Task task = Util.convertMapToTaskObject(doc.getData());
                                    if(localDatabase.taskDao().exists(task.id) == 0){
                                        //get all tasks in firestore but not in local
                                        newTasksFromFirestoreList.add(task);
                                    }else {
                                        Log.d("data", "present");
                                    }
                                }
                                //insert to database
                                localDatabase.taskDao().insertAll(newTasksFromFirestoreList);
                                //get next batch
                                if(documentSnapshots.size() >= PAGINATOR) {
                                    Task lastTask = Util.convertMapToTaskObject(documentSnapshots.get(documentSnapshots.size()-1).getData());
                                    compareFirestoreToRoomData(lastTask.updatedAt);
                                }
                            }).start();

                        }
                    });


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
        DocumentReference ref = db.collection(Util.getUserName(context))
                .document(tableName).collection(tableName)
                .document(docId);

        subscriptionController.updateCoveredSize(ref, false);
        ref.update(newData).addOnSuccessListener(aVoid -> {
            subscriptionController.updateCoveredSize(ref, true);
            Log.d("firebase", "updated succesfully");
        });
    }

}
