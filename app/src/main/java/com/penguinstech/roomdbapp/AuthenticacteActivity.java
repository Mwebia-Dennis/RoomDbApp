package com.penguinstech.roomdbapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;

import com.penguinstech.roomdbapp.utils.Util;

public class AuthenticacteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticacte);

        findViewById(R.id.loginBtn).setOnClickListener(v->{

            EditText userNameET = findViewById(R.id.userNameET);
            String userName = userNameET.getText().toString();
            if(userName.trim().equals("")){
                userNameET.setError("invalid user name");
            }else {

                Util.login(AuthenticacteActivity.this, userName);
                finish();
            }

        });
    }
}