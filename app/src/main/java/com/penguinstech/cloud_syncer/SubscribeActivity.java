package com.penguinstech.cloud_syncer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.penguinstech.cloud_syncer.room_db.AppDatabase;
import com.penguinstech.cloud_syncer.room_db.Subscription;
import com.penguinstech.cloud_syncer.utils.SubscrtiptionsAdapter;
import com.penguinstech.cloud_syncer.utils.Util;
import com.penguinstech.cloud_syncer.utils.Configs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubscribeActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler {

    AppDatabase localDatabase;
    BillingProcessor bp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);

        init();
    }


    private void init() {
        localDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, Configs.DatabaseName).build();

        bp = new BillingProcessor(SubscribeActivity.this, Configs.licenseKey, this);

        //prevent freedom attack (add merchant id)
        //get merchant id from https://pay.google.com/gp/w/u/0/home/settings
//        bp = new BillingProcessor(SubscribeActivity.this, Configs.licenseKey, merchantId, this);
        bp.initialize();


    }

    private void updateUI() {
        new Thread() {
            @Override
            public void run() {

                List<Subscription> allSubs = localDatabase.subscriptionDao().getAll();
                TextView current_planTV = findViewById(R.id.current_planTV);
                try {
                    runOnUiThread(() -> {

                        if(allSubs.size() > 0)current_planTV.setText("Your subscription plan is: " + allSubs.get(0).subscriptionType);
                    });
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void getSubscriptionsList() {
        ArrayList<String> productIds = new ArrayList<>();
        productIds.add("1");
        productIds.add("2");
        productIds.add("3");
        List<SkuDetails> listOfSubscriptions = new ArrayList<>();
        listOfSubscriptions.clear();
        List<SkuDetails> results = bp.getSubscriptionListingDetails(productIds);
        if(results != null){
            listOfSubscriptions.addAll(results);
        }
        RecyclerView recyclerView = findViewById(R.id.subscriptionRV);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new SubscrtiptionsAdapter(SubscribeActivity.this, listOfSubscriptions, bp));
    }


    private void insertSubscription(String plan, String totalSize) {


        Util.saveSubscriptionToRoomDb(
                localDatabase.subscriptionDao(),
                new Subscription(
                        Util.getUserName(SubscribeActivity.this),
                        plan,
                        totalSize,
                        "0",
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date())
                )
        );
        updateUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!bp.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        if (bp != null) {
            bp.release();
        }
        super.onDestroy();
    }


    @Override
    public void onProductPurchased(@NonNull String productId, TransactionDetails details) {

        Toast.makeText(this, "Subscription Done, you bought product "+productId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPurchaseHistoryRestored() {

        Toast.makeText(this, "Purchase History Restored", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {

        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBillingInitialized() {

//        Toast.makeText(this, "Billing initialized", Toast.LENGTH_SHORT).show();

        getSubscriptionsList();
    }
}