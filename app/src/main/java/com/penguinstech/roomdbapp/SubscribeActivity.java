package com.penguinstech.roomdbapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class SubscribeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);

        init();
    }

    private void init() {
        TextView current_planTV = findViewById(R.id.current_planTV);
        findViewById(R.id.freePlanBtn).setOnClickListener(v->{
            current_planTV.setText(R.string.current_plan);
        });
        findViewById(R.id.fiveBtn).setOnClickListener(v->{
            current_planTV.setText(R.string.bronze_plan);
        });
        findViewById(R.id.tenBtn).setOnClickListener(v->{
            current_planTV.setText(R.string.silver_plan);
        });
        findViewById(R.id.fifteenBtn).setOnClickListener(v->{
            current_planTV.setText(R.string.gold_plan);
        });
    }
}