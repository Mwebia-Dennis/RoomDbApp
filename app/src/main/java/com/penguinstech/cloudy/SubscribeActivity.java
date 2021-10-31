package com.penguinstech.cloudy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.penguinstech.cloudy.room_db.AppDatabase;
import com.penguinstech.cloudy.room_db.Subscription;
import com.penguinstech.cloudy.utils.AppSubscriptionPlans;
import com.penguinstech.cloudy.utils.SubscrtiptionsAdapter;
import com.penguinstech.cloudy.utils.Util;
import com.penguinstech.cloudy.utils.Configs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator;

public class SubscribeActivity extends AppCompatActivity {

    AppDatabase localDatabase;
    private BillingClient billingClient;
    private SubscrtiptionsAdapter adapter;
    private List<SkuDetails> listOfSubscriptions;
    public static String SELECTED_SUBSCRIPTION_ID;
    String userName;
    DatabaseReference firebaseDatabase;
    Purchase currentPurchase = null;
//    BillingProcessor bp;

    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {

            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.d("billing error", billingResult.getDebugMessage());
            Toast.makeText(SubscribeActivity.this, "billing error"+billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
//            Toast.makeText(SubscribeActivity.this, "Sorry, cannot process your subscription", Toast.LENGTH_SHORT).show();
        } else {
            // Handle any other error codes.
            Log.d("billing error", billingResult.getDebugMessage());
            Toast.makeText(SubscribeActivity.this, "billing error"+billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
//            Toast.makeText(SubscribeActivity.this, "Sorry, cannot process your subscription", Toast.LENGTH_SHORT).show();
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);

        init();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.subscriptions_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.manage_subscriptions) {

            Toast.makeText(SubscribeActivity.this, "Loading, please wait...", Toast.LENGTH_SHORT).show();
            //get user subscription info
            if (currentPurchase != null){
                //get if user has active subscription
                //if true redirect to the subscription page
                //else redirect to  subscriptions history page
                if (currentPurchase.isAutoRenewing()){
                    Util.redirectToLink(SubscribeActivity.this, "https://play.google.com/store/account/subscriptions?sku="+
                            currentPurchase.getSkus().get(0)+"&package="+Configs.APP_PACKAGE);
                }else {
                    Util.redirectToLink(SubscribeActivity.this, "https://play.google.com/store/account/subscriptions");
                }
            }else {
                Toast.makeText(SubscribeActivity.this, "You have 0 subscriptions ", Toast.LENGTH_SHORT).show();
            }

        }
        return super.onOptionsItemSelected(item);
    }


    private void init() {
        userName = Util.getUserName(SubscribeActivity.this);
        localDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, Configs.DatabaseName).build();
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        listOfSubscriptions = new ArrayList<>();
        RecyclerView recyclerView = findViewById(R.id.subscriptionRV);
        adapter = new SubscrtiptionsAdapter(SubscribeActivity.this, listOfSubscriptions, billingClient);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
        //start billing connection
        startConnection();
        updateUI();




    }

    private void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    getUserSubscription();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                startConnection();
            }
        });
    }

    private void updateUI() {

        firebaseDatabase.child(userName).child(Configs.subscriptionTableName)
                .child("subscription_details").get().addOnSuccessListener(dataSnapshot -> {

                    if (dataSnapshot.exists()){

                        Subscription subscription = dataSnapshot.getValue(Subscription.class);
                        TextView current_planTV = findViewById(R.id.current_planTV);
                        TextView current_spaceTV = findViewById(R.id.current_spaceTV);
                        TextView totalSizeTV = findViewById(R.id.totalSizeTV);

                        CircularProgressIndicator circularProgress = findViewById(R.id.circular_progress);
                        long coveredSize = Math.round(Double.parseDouble(subscription.coveredSize));
                        long totalSize = Math.round(Double.parseDouble(subscription.totalSize));
                        if(subscription != null){
                            current_planTV.setText("Plan: " + subscription.subscriptionType);
                            circularProgress.setProgress(coveredSize,
                                    (totalSize>0)?totalSize:coveredSize
                            );
                            current_spaceTV.setText("Covered Space: "+(Long.parseLong(subscription.coveredSize))+" bytes");
                            totalSizeTV.setText("Total Size: "+(Long.parseLong(subscription.totalSize))+" bytes");
                        }
                    }
                });
    }

    private void getSubscriptionsList() {
        ArrayList<String> productIds = new ArrayList<>();
        productIds.add("1");
        productIds.add("2");
        productIds.add("3");

        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(productIds).setType(BillingClient.SkuType.SUBS);
        billingClient.querySkuDetailsAsync(params.build(),
                (billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        new Thread(()->{
                            Log.d("list", String.valueOf(list.size()));
                            if(list.size() > 0)Log.d("list", list.get(0).getTitle());
                            Subscription subscription = localDatabase.subscriptionDao().getLastSubscription();
                            try {
                                runOnUiThread(() -> {

                                    if(subscription == null){
                                        adapter.setPurchaseToken("");
                                        adapter.setSubscriptionStoreId("");
                                    }else {
                                        adapter.setPurchaseToken(subscription.purchaseToken);
                                        adapter.setSubscriptionStoreId(subscription.subscriptionStoreId);
                                    }

                                    listOfSubscriptions.clear();
                                    listOfSubscriptions.addAll(list);
                                    adapter.notifyDataSetChanged();
                                });
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }).start();
                    }else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ERROR){
                        //HANDLE ERROR

                        Log.d("billing error", billingResult.getDebugMessage());
                        Toast.makeText(SubscribeActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {

                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        getUserSubscription();
                        Toast.makeText(SubscribeActivity.this, "Subscription has been acknowledged", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void getUserSubscription() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, (billingResult, list) -> {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                //check if the current subscription is present in database.
                if(list.size() > 0){
                    Purchase lastPurchase = list.get(list.size() - 1);
                    currentPurchase= lastPurchase;
                    long planSize = Util.getPlanTotalSize(lastPurchase.getSkus().get(0));
                    final Subscription newSubscription = new Subscription(
                            userName,
                            lastPurchase.getOrderId(),
                            lastPurchase.getSkus().get(0),
                            lastPurchase.getPackageName(),
                            String.valueOf(planSize),
//                            String.valueOf(Util.convertMbToBytes(Long.parseLong(lastPurchase.getSkus().get(0)))),
                            "0",
                            String.valueOf(lastPurchase.getPurchaseState()),
                            String.valueOf(lastPurchase.getPurchaseTime()),
                            lastPurchase.getPurchaseToken(),
                            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date())
                    );
                    firebaseDatabase.child(userName).child(Configs.subscriptionTableName)
                            .child("subscription_details").get().addOnSuccessListener(dataSnapshot -> {

                        if (dataSnapshot.exists()){
                            Subscription subscription = dataSnapshot.getValue(Subscription.class);
                            if (!subscription.orderId.equals(lastPurchase.getOrderId())) {
                                //update firebase
                                //add the previous covered space to our new subscription
                                newSubscription.coveredSize = subscription.coveredSize;
                                updateDb(newSubscription);
                            }

                        }else{
                            updateDb(newSubscription);
                        }
                        getSubscriptionsList();
                    });

                }else {
                    //user has no subscription or it is expired
                    //so update databases
                    //check if subscription exists in db
                    firebaseDatabase.child(userName).child(Configs.subscriptionTableName)
                            .child("subscription_details").get().addOnSuccessListener(dataSnapshot -> {

                        if (dataSnapshot.exists()) {

                            Subscription sub = dataSnapshot.getValue(Subscription.class);
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
                                        updateDb(subscription);

                                    }else {

                                        getSubscriptionsList();
                                    }
                                }

                            }else {

                                getSubscriptionsList();
                            }
                        }
                    });


                }

            }

        });
    }

    private void updateDb(Subscription subscription){

        firebaseDatabase.child(userName).child(Configs.subscriptionTableName)
                .child("subscription_details")
                .setValue(subscription).addOnSuccessListener(aVoid -> {

            new Thread(()->{

                List<Subscription> list = new ArrayList<>();
                list.add(subscription);
                localDatabase.subscriptionDao().insertAll(list);
                updateUI();
            }).start();

        });

    }

}