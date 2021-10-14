package com.penguinstech.roomdbapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddTaskActivity extends AppCompatActivity {

    FirebaseFirestore db;
    AppDatabase localDatabase;
    Map<String, String> taskInfo = new HashMap<>();
    TaskDao taskDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        init();
    }

    private void init() {
        FirebaseApp.initializeApp(AddTaskActivity.this);
        db = FirebaseFirestore.getInstance();
        localDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, DatabaseConfigs.DatabaseName).build();
        taskDao = localDatabase.taskDao();
        EditText titleET = findViewById(R.id.title_ET);
        EditText descriptionET = findViewById(R.id.description_ET);
        findViewById(R.id.addNoteBtn).setOnClickListener(v->{

            String title = titleET.getText().toString();
            String description = descriptionET.getText().toString();

            if(title.equals("")) {
                titleET.setError("This field is required");
            }else if (description.equals("")) {
                descriptionET.setError("This field is required");
            }else {

                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                taskInfo.put("title", title);
                taskInfo.put("description", description);
                taskInfo.put("updated_on", formatter.format(new Date()));
                saveDataToFirestore();
                saveDataToRoomDb(taskInfo);
            }
        });
    }


    public void saveDataToRoomDb(Map<String, String> taskMap) {

        new Thread() {
            @Override
            public void run() {

                taskDao.insertAll(new Util().convertMapToTaskObject(taskMap));
                try {
                    runOnUiThread(() -> Toast.makeText(AddTaskActivity.this, "Object added to local DB", Toast.LENGTH_LONG).show());
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void saveDataToFirestore() {

        db.collection(DatabaseConfigs.NoteCollection)
                .document("user_1")
                .set(taskInfo)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddTaskActivity.this, "Document added successfully", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddTaskActivity.this, "Could not add document", Toast.LENGTH_LONG).show();
                });
    }
}