package com.penguinstech.cloudy.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.penguinstech.cloudy.controller.FileController;
import com.penguinstech.cloudy.controller.TaskController;
import com.penguinstech.cloudy.room_db.AppDatabase;
import com.penguinstech.cloudy.room_db.Subscription;
import com.penguinstech.cloudy.room_db.Token;
import com.penguinstech.cloudy.utils.AppSubscriptionPlans;
import com.penguinstech.cloudy.utils.Configs;
import com.penguinstech.cloudy.utils.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    long limiter = 5;
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
                            updateUserSubscription(userName, subscription);
                            String[] listOfTables = new String[] {Configs.tableName, Configs.filesTableName};
                            for (String tableName: listOfTables) {

                                syncData(tableName, subscription);
                            }



                        }else {
                            //user has no subscription set.
                            Util.setNewSubscription(localDatabase,userName);
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


                    //check if user is subcribed
                    if (!subscription.subscriptionStoreId.equals(AppSubscriptionPlans.FREE.getKey())) {
                        //check if user has enough space in cloud
                        //give allowance of about 10mb.
                        long remainingSpace = (Long.parseLong(subscription.totalSize) - Long.parseLong(subscription.coveredSize));
                        long lim = Util.convertMbToBytes(limiter);
                        if(remainingSpace > lim) {

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


                    //check if user is subcribed
                    if (!subscription.subscriptionStoreId.equals(AppSubscriptionPlans.FREE.getKey())) {
                        //check if user has enough space in cloud
                        //give allowance of about 10mb.
                        if ((Long.parseLong(subscription.totalSize) - Long.parseLong(subscription.coveredSize)) < Util.convertMbToBytes(limiter)) {

                            //firestore is emmpty. update with room data.
                            if (tableName.equals(Configs.tableName)) {
                                new TaskController(context, localDatabase).saveDataToFirestore(true, "");
                            } else if (tableName.equals(Configs.filesTableName)) {
                                new FileController(context, localDatabase).saveDataToFirestore(true, "");
                            }
                        }
                    }

                }else {

                    //if both are null then user has no data.
                    //check if user is subcribed
                    if (!subscription.subscriptionStoreId.equals(AppSubscriptionPlans.FREE.getKey())) {

                        Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.tableName);
                        Util.updateToken(context, context.getContentResolver(),localDatabase,Configs.filesTableName);
                    }
                }
            }).start();

        }).addOnFailureListener(e -> {

            Log.d("firebase token", "failed");
        });



    }


    private void updateUserSubscription(String userName, Subscription subscription) {
        BillingClient billingClient = BillingClient.newBuilder(context)
                .setListener((billingResult, list) -> {

                })
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult1) {
                if (billingResult1.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, (billingResult, list) -> {

                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            //check if the current subscription is present in database.
                            if(list.size() > 0){
                                Purchase lastPurchase = list.get(list.size() - 1);
                                long planSize = Util.getPlanTotalSize(lastPurchase.getSkus().get(0));
                                final Subscription newSubscription = new Subscription(
                                        userName,
                                        lastPurchase.getOrderId(),
                                        lastPurchase.getSkus().get(0),
                                        lastPurchase.getPackageName(),
                                        String.valueOf(Util.convertMbToBytes(planSize)),
//                            String.valueOf(Util.convertMbToBytes(Long.parseLong(lastPurchase.getSkus().get(0)))),
                                        "0",
                                        String.valueOf(lastPurchase.getPurchaseState()),
                                        String.valueOf(lastPurchase.getPurchaseTime()),
                                        lastPurchase.getPurchaseToken(),
                                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date())
                                );
                                if (subscription != null){
                                    if (!subscription.orderId.equals(lastPurchase.getOrderId())) {
                                        //update firebase
                                        //add the previous covered space to our new subscription
                                        newSubscription.coveredSize = subscription.coveredSize;
                                        updateDb(newSubscription, userName);
                                    }

                                }else{
                                    updateDb(newSubscription, userName);
                                }

                            }else {
                                //user has no subscription or it is expired
                                //so update databases
                                //check if subscription exists in db
                                Subscription sub = subscription;
                                if (sub != null) {

                                    if (sub.subscriptionStoreId != null) {
                                        if(!sub.subscriptionStoreId.equals(AppSubscriptionPlans.FREE.getKey())) {
                                            //if user had no free subscription, then it means subscription is expired
                                            //so update subscription
                                            Subscription subscription = new Subscription(
                                                    userName,
                                                    "",
                                                    AppSubscriptionPlans.FREE.getKey(),
                                                    "FREE",
                                                    String.valueOf(AppSubscriptionPlans.FREE.getValue()),
                                                    sub.coveredSize,
                                                    "",
                                                    "",
                                                    "",
                                                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date()));
                                            updateDb(subscription, userName);

                                        }
                                    }

                                }


                            }

                        }

                    });
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                updateUserSubscription(userName, subscription);
            }
        });
    }

    private void updateDb(Subscription subscription, String userName){

        firebaseDatabase.child(userName).child(Configs.subscriptionTableName)
                .child("subscription_details")
                .setValue(subscription).addOnSuccessListener(aVoid -> {

            new Thread(()->{

                List<Subscription> list = new ArrayList<>();
                list.add(subscription);
                localDatabase.subscriptionDao().insertAll(list);
            }).start();

        });

    }



}