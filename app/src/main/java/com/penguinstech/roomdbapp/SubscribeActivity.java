package com.penguinstech.roomdbapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.os.Bundle;
import android.widget.TextView;

import com.penguinstech.roomdbapp.room_db.AppDatabase;
import com.penguinstech.roomdbapp.room_db.Subscription;
import com.penguinstech.roomdbapp.utils.Util;
import com.penguinstech.roomdbapp.utils.AppSubscriptionPlans;
import com.penguinstech.roomdbapp.utils.Configs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubscribeActivity extends AppCompatActivity {

    AppDatabase localDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);

        init();
    }

    private void init() {
        localDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, Configs.DatabaseName).build();

        updateUI();

        findViewById(R.id.freePlanBtn).setOnClickListener(v->{
            insertSubscription(
                AppSubscriptionPlans.FREE.getKey(),
                String.valueOf(AppSubscriptionPlans.FREE.getValue())
            );
        });
        findViewById(R.id.fiveBtn).setOnClickListener(v->{
            insertSubscription(
                AppSubscriptionPlans.BRONZE.getKey(),
                String.valueOf(AppSubscriptionPlans.BRONZE.getValue())
            );
        });
        findViewById(R.id.tenBtn).setOnClickListener(v->{
            insertSubscription(
                AppSubscriptionPlans.SILVER.getKey(),
                String.valueOf(AppSubscriptionPlans.SILVER.getValue())
            );
        });
        findViewById(R.id.fifteenBtn).setOnClickListener(v->{
            insertSubscription(
                AppSubscriptionPlans.GOLD.getKey(),
                String.valueOf(AppSubscriptionPlans.GOLD.getValue())
            );
        });
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

}