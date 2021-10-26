package com.penguinstech.cloudy.controller;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.penguinstech.cloudy.room_db.AppDatabase;
import com.penguinstech.cloudy.room_db.Subscription;
import com.penguinstech.cloudy.utils.Configs;
import com.penguinstech.cloudy.utils.Util;

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
                    long documentSize = firestoreDocument.getSize(document);
                    updateDatabases(documentSize, isNewData);
                }
            }
        });
    }

    public void updateDatabases(long documentSize, boolean isNewData) {
        String userName = Util.getUserName(context);
        DatabaseReference subscriptionRef = firebaseDatabase.child(userName).child(Configs.subscriptionTableName)
                .child("subscription_details");
        subscriptionRef.get().addOnSuccessListener(dataSnapshot -> {

            if (dataSnapshot.exists()){
                Subscription subscription = dataSnapshot.getValue(Subscription.class);
                subscription.coveredSize = (isNewData)?
                        String.valueOf((Long.parseLong(subscription.coveredSize) + documentSize))//increase total size covered
                        :String.valueOf((Long.parseLong(subscription.coveredSize) - documentSize));//data has been removed so decrease total soze covered
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
