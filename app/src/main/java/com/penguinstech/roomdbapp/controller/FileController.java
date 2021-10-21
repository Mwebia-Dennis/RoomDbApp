package com.penguinstech.roomdbapp.controller;

 import android.content.Context;
 import android.net.Uri;
 import android.util.Log;

 import androidx.annotation.NonNull;

 import com.google.android.gms.tasks.OnCompleteListener;
 import com.google.android.gms.tasks.OnFailureListener;
 import com.google.android.gms.tasks.OnSuccessListener;
 import com.google.android.gms.tasks.Task;
 import com.google.firebase.FirebaseApp;
 import com.google.firebase.firestore.DocumentSnapshot;
 import com.google.firebase.firestore.FirebaseFirestore;
 import com.google.firebase.storage.FirebaseStorage;
 import com.google.firebase.storage.StorageReference;
 import com.google.firebase.storage.UploadTask;
 import com.google.gson.Gson;
 import com.google.gson.JsonElement;
 import com.penguinstech.roomdbapp.room_db.AppDatabase;
 import com.penguinstech.roomdbapp.room_db.Files;
 import com.penguinstech.roomdbapp.room_db.TaskDao;
 import com.penguinstech.roomdbapp.utils.Configs;
 import com.penguinstech.roomdbapp.utils.Util;

 import java.io.File;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.UUID;

public class FileController implements MainController {
     Context context;
     AppDatabase localDatabase;
     FirebaseFirestore db;//firestore instance
     StorageReference storageRef;
     Boolean hasDataLoaded = false;// prevents duplicate data in db
     final int PAGINATOR  = 100;
     String tableName;

     public FileController(Context context, AppDatabase localDatabase){

         this.context = context;
         this.localDatabase = localDatabase;
         FirebaseApp.initializeApp(context);
         db = FirebaseFirestore.getInstance();
         tableName = Configs.filesTableName;
         storageRef = FirebaseStorage.getInstance().getReference("files/"+Util.getUserName(context));
     }
    @Override
    public void syncAllDataFromFirestore() {
         //only store the links, no need to download
        db.collection(Util.getUserName(context)).document(tableName).collection(tableName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("firestore onSuccess", "LIST EMPTY");
                    } else {
                        // get all data and add to database
                        if (!hasDataLoaded){

                            //update accordingly per table
                            if (tableName.equals(Configs.filesTableName)){

                                //convert whole queryDocumentSnapshots to list
                                List<Files> backedUpFiles = queryDocumentSnapshots.toObjects(Files.class);

                                Log.d("size", String.valueOf(backedUpFiles.size()));
                                //save data to room
                                saveDataToRoomDb(backedUpFiles);
                            }
//                            else if (tableName == Configs.tableName2){
//                                //repeat as the above if body but accessing the dao and class related to the current table
//                            }
                            //update last sync token
                            Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.filesTableName);
                            //notify the main ui thread
//                            context.getContentResolver().notifyChange(Configs.URI_TASK, null, false);
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
                if (tableName.equals(Configs.filesTableName)){

                    int totalNewData = isAllData?localDatabase.fileDao().getCount():localDatabase.fileDao().filterByDateCount(updatedAt);
                    Log.d("totalNewData", String.valueOf(totalNewData));
                    int batchSize = 0;
                    while (batchSize <= totalNewData){


                        //get first 100
                        List<Files> newFiles = isAllData?localDatabase.fileDao()
                                .getAll():localDatabase.fileDao().filterByDate(updatedAt, PAGINATOR, batchSize);
                        //upload items to firestore so as data matches the room data
                        for(Files file: newFiles) {

                            //if @param isAllData = true: upload all files to server
                            //else
                            //check if doc is in server and update
                            if(isAllData) {
                                storageRef.child(UUID.randomUUID().toString()).putFile(Uri.parse(Util.getPath(context, Uri.parse(file.localPath))))
                                        .addOnCompleteListener(task -> {

                                            task.getResult().getStorage().getDownloadUrl().addOnSuccessListener(uri -> {

                                                file.firestorePath = uri.toString();

                                                //update firebase path in firestore
                                                db.collection(Util.getUserName(context))
                                                        .document(tableName).collection(tableName)
                                                        .add(file)
                                                        .addOnSuccessListener(documentReference -> {
                                                            Log.d("backing Data", "Successful");
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.d("backing Data", "Failed");
                                                        });
                                                //update firebase path in room db
                                                new Thread(()->{
                                                    localDatabase.fileDao().update(file);
                                                }).start();

                                            });


                                        })
                                        .addOnFailureListener(e -> {

                                            Log.d("backing files", "Failed");
                                        });

                            }else {
                                db.collection(Util.getUserName(context)).document(tableName).collection(tableName)
                                        .whereEqualTo("id", file.id)
                                        .get().addOnSuccessListener(queryDocumentSnapshots -> {

                                            //check if the document exists
                                            if (!queryDocumentSnapshots.isEmpty()) {

                                                //update server
                                                List<DocumentSnapshot> docsList = queryDocumentSnapshots.getDocuments();
                                                if (docsList.size() > 0){
                                                    if (file.isDeleted == 1) {
                                                        //delete file and data from firebase
                                                        FirebaseStorage.getInstance().getReferenceFromUrl(file.firestorePath).delete()
                                                                .addOnSuccessListener(aVoid -> {

                                                                    Log.d("Deleting file", "successful");
                                                                });
                                                        db.collection(Util.getUserName(context)).document(tableName).collection(tableName)
                                                                .document(docsList.get(0).getId()).delete()
                                                                .addOnSuccessListener(aVoid -> {

                                                                    Log.d("Deleting data", "successful");
                                                                });
                                                    }else {
                                                        updateServer(file, docsList.get(0).getId());
                                                    }
                                                }

                                            } else {

                                                //if not exists, add file to firestore
                                                storageRef.child(UUID.randomUUID().toString()).putFile(Uri.parse(file.localPath))
                                                        .addOnCompleteListener(task -> {

                                                            task.getResult()
                                                                    .getStorage().getDownloadUrl()
                                                                    .addOnSuccessListener(uri -> {

                                                                        file.firestorePath = uri.toString();
                                                                        //update firebase path in firestore

                                                                        db.collection(Util.getUserName(context))
                                                                                .document(tableName).collection(tableName)
                                                                                .add(file)
                                                                                .addOnSuccessListener(documentReference -> {
                                                                                    Log.d("backing Data", "Successful");
                                                                                })
                                                                                .addOnFailureListener(e -> {
                                                                                    Log.d("backing Data", "Failed");
                                                                                });
                                                                        //update firebase path in room db
                                                                        new Thread(()->{
                                                                            localDatabase.fileDao().update(file);
                                                                        }).start();

                                                                    });


                                                        })
                                                        .addOnFailureListener(e -> {

                                                            e.printStackTrace();
                                                            Log.d("error", e.getMessage());
                                                        });
                                            }
                                        }
                                );
                            }


                        }
                        batchSize += PAGINATOR;
                    }

                }

                Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.filesTableName);
            }
        }.start();

    }

    @Override
    public void compareRoomToFirestoreData(String tokenLastSync) {
        new Thread(()->{

            //getting data in small batches to avoid filling the memory with data
            int totalNewData = localDatabase.fileDao().filterByDateCount(tokenLastSync);
            int batchSize = 0;
            while (batchSize <= totalNewData) {

                List<Files> listOfRoomFiles = localDatabase.fileDao().filterByDate(tokenLastSync, PAGINATOR, batchSize);
                if (listOfRoomFiles.size() > 0) {

                    for (Files files : listOfRoomFiles) {
                        db.collection(Util.getUserName(context)).document(tableName).collection(tableName)
                                .whereEqualTo("id", files.id)
                                .get().addOnSuccessListener(queryDocumentSnapshots -> {

                            //check if the document exists
                            if (!queryDocumentSnapshots.isEmpty()) {

                                List<DocumentSnapshot> documentSnapshots = queryDocumentSnapshots.getDocuments();
                                for (DocumentSnapshot doc : documentSnapshots) {

                                    //convert map to  task object
                                    Files firestoreFile = convertMapToFilesObject(doc.getData());
                                    try {
                                        //convert the items date from string to date
                                        Date localTaskDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)
                                                .parse(files.updatedAt);
                                        Date firestoreTaskDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)
                                                .parse(firestoreFile.updatedAt);

                                        //check if local date is after the firestore date
                                        if (localTaskDate.after(firestoreTaskDate)) {
                                            //update firebase
                                            //if deleted is true then delete from firebase
                                            if (files.isDeleted == 1) {

                                                //delete data from firebase

                                                FirebaseStorage.getInstance().getReferenceFromUrl(files.firestorePath).delete()
                                                        .addOnSuccessListener(aVoid -> {

                                                            Log.d("Deleting file", "successful");
                                                        });
                                                db.collection(Util.getUserName(context)).document(tableName).collection(tableName)
                                                        .document(doc.getId()).delete()
                                                        .addOnSuccessListener(aVoid -> {

                                                            Log.d("Deleting data", "successful");
                                                        });

                                            } else {
                                                //add data to map and update firestore
                                                updateServer(files, doc.getId());
                                            }

                                        } else if (firestoreTaskDate.after(localTaskDate)) {//is firestore after the local updated date
                                            //update the local database
                                            new Thread(() -> {
                                                localDatabase.fileDao().update(firestoreFile);
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
                                if(files.isDeleted  != 1) {

                                    storageRef.child(UUID.randomUUID().toString())
                                            .putFile(Uri.parse(Util.getPath(context, Uri.parse(files.localPath))))
                                            .addOnCompleteListener(task -> {

                                                files.firestorePath = task.getResult().toString();
                                                db.collection(Util.getUserName(context))
                                                        .add(files)
                                                        .addOnSuccessListener(documentReference -> {
                                                            Log.d("firebase", "updated succesfully");
                                                        });
                                                new Thread(()->{
                                                    localDatabase.fileDao().update(files);
                                                }).start();
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
    public void compareFirestoreToRoomData(String tokenLastSync) {

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
                                List<Files> newFilesFromFirestoreList = new ArrayList<>();
                                for (DocumentSnapshot doc : documentSnapshots) {
                                    //convert map to  task object
                                    Files file = convertMapToFilesObject(doc.getData());
                                    if(localDatabase.fileDao().exists(file.id) == 0){
                                        //get all tasks in firestore but not in local
                                        newFilesFromFirestoreList.add(file);
                                    }else {
                                        Log.d("data", "present");
                                    }
                                }
                                //insert to database
                                localDatabase.fileDao().insertAll(newFilesFromFirestoreList);
                                //get next batch
                                if(documentSnapshots.size() >= PAGINATOR) {
                                    Files lastFile = convertMapToFilesObject(documentSnapshots.get(documentSnapshots.size()-1).getData());
                                    compareFirestoreToRoomData(lastFile.updatedAt);
                                }
                            }).start();

                        }
                    });


            Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.filesTableName);

        }).start();
    }

    @Override
    public void updateServer(Object item, String docId) {

         //delete original file
        //add new file
        //update firestore and room

        Files file = (Files) item;
        FirebaseStorage.getInstance().getReferenceFromUrl(file.firestorePath).delete()
                .addOnSuccessListener(aVoid -> {

                    Log.d("Deleting file", "successful");
                });

        storageRef.child(UUID.randomUUID().toString())
                .putFile(Uri.parse(Util.getPath(context, Uri.parse(file.localPath))))
                .addOnCompleteListener(task -> {
                    task.getResult().getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                        file.firestorePath = uri.toString();
                        Map<String, Object> newData = new HashMap<>();
                        newData.put("deviceId", file.deviceId);
                        newData.put("localPath", file.localPath);
                        newData.put("firestorePath", file.firestorePath);
                        newData.put("isDeleted", file.isDeleted);
                        newData.put("id", file.id);
                        newData.put("updatedAt", file.updatedAt);
                        db.collection(Util.getUserName(context))
                                .document(tableName).collection(tableName)
                                .document(docId).update(newData).addOnSuccessListener(aVoid -> {
                            Log.d("firebase", "updated succesfully");
                        });
                        new Thread(()->{
                            localDatabase.fileDao().update(file);
                        });
                    });
                });


    }

     public void saveDataToRoomDb(List<Files> data) {
         new Thread() {
             @Override
             public void run() {

                 localDatabase.fileDao().insertAll(data);
                 Log.d("Local db ", "all objects added to db");
             }
         }.start();
     }


     public static Files convertMapToFilesObject (Map<String, Object> taskMap) {
         Gson gson = new Gson();
         JsonElement jsonElement = gson.toJsonTree(taskMap);
         return gson.fromJson(jsonElement, Files.class);
     }
}
