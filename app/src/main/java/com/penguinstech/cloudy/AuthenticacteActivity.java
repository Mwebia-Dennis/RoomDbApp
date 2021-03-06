package com.penguinstech.cloudy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.os.Bundle;
import android.widget.EditText;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.penguinstech.cloudy.room_db.AppDatabase;
import com.penguinstech.cloudy.room_db.Subscription;
import com.penguinstech.cloudy.utils.AppSubscriptionPlans;
import com.penguinstech.cloudy.utils.Configs;
import com.penguinstech.cloudy.utils.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AuthenticacteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticacte);

        AppDatabase localDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, Configs.DatabaseName).build();
        findViewById(R.id.loginBtn).setOnClickListener(v->{

            EditText userNameET = findViewById(R.id.userNameET);
            String userName = userNameET.getText().toString();
            if(userName.trim().equals("")){
                userNameET.setError("invalid user name");
            }else {

                Util.login(AuthenticacteActivity.this, userName);
                Util.setNewSubscription(localDatabase,userName);
                finish();
            }

        });
    }
}