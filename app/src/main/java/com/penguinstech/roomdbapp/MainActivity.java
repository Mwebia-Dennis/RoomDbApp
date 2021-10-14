package com.penguinstech.roomdbapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    FirebaseFirestore db;
    AppDatabase localDatabase;
    Map<String, String> taskInfo = new HashMap<>();
    TaskDao taskDao;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.addTask) {
            startActivity(new Intent(MainActivity.this, AddTaskActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void init() {
        FirebaseApp.initializeApp(MainActivity.this);
        db = FirebaseFirestore.getInstance();
        localDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, DatabaseConfigs.DatabaseName).build();
        taskDao = localDatabase.taskDao();
        tv = findViewById(R.id.textView);

        taskInfo.put("title", "fgfdgdggfd");
        taskInfo.put("description", "fgfdgdggfd");
        taskInfo.put("updated_on", "10/10/2021");

        findViewById(R.id.save_to_fireStore).setOnClickListener(v -> {


        });
        findViewById(R.id.get_data).setOnClickListener(v -> {

            getUser();

        });



    }




    public void getUser() {
        new Thread() {
            @Override
            public void run() {

                Task task = taskDao.loadTaskById(1);
                if (task != null){
                    try {
                        runOnUiThread(() -> {
                            tv.setText(String.valueOf(task.title));
                        });
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.i("local db user is ", "null");
                }
            }
        }.start();
    }
}