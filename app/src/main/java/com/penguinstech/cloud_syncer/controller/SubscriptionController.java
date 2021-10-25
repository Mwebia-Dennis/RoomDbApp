package com.penguinstech.cloud_syncer.controller;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.penguinstech.cloud_syncer.room_db.AppDatabase;
import com.penguinstech.cloud_syncer.room_db.Subscription;
import com.penguinstech.cloud_syncer.utils.Configs;
import com.penguinstech.cloud_syncer.utils.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ro.alexmamo.firestore_document.FirestoreDocument;

public class SubscriptionController {
    FirestoreDocument firestoreDocument;
    FirebaseFirestore db;
    AppDatabase localDatabase;
    Context context;
    DatabaseReference firebaseDatabase;//firebase realtime db

    public SubscriptionController(Context context, AppDatabase localDatabase){
        this.context = context;
        this.localDatabase = localDatabase;
        firestoreDocument = FirestoreDocument.getInstance();
        FirebaseApp.initializeApp(context);
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
    }


    public void updateCoveredSize(DocumentReference ref, boolean isNewData) {
        ref.get().addOnCompleteListener(task->{

            if (task.isSuccessful()){
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    int documentSize = firestoreDocument.getSize(document);
                    updateDatabases(documentSize, isNewData);
                }
            }
        });
    }

    public void updateDatabases(int documentSize, boolean isNewData) {
        String userName = Util.getUserName(context);
        DatabaseReference subscriptionRef = firebaseDatabase.child(userName).child(Configs.subscriptionTableName)
                .child("subscription_details");
        subscriptionRef.get().addOnSuccessListener(dataSnapshot -> {

            if (dataSnapshot.exists()){
                Subscription subscription = dataSnapshot.getValue(Subscription.class);
                subscription.coveredSize = (isNewData)?
                        String.valueOf((Integer.parseInt(subscription.coveredSize) + documentSize))//increase total size covered
                        :String.valueOf((Integer.parseInt(subscription.coveredSize) - documentSize));//data has been removed so decrease total soze covered
                subscription.updatedAt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date());

                firebaseDatabase.child(userName).child(Configs.subscriptionTableName)
                        .child("subscription_details").setValue(subscription).addOnSuccessListener(aVoid -> {

                    //update room db
                    new Thread(()->{
                        if(localDatabase.subscriptionDao().getCount() == 0){
                            List<Subscription> newSubscriptions = new ArrayList<>();
                            newSubscriptions.add(subscription);
                            localDatabase.subscriptionDao().insertAll(newSubscriptions);
                        }else {
                            localDatabase.subscriptionDao().update(subscription);
                        }
                    }).start();
                });

            }

        });
    }
}
