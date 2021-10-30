package com.penguinstech.cloudy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.penguinstech.cloudy.room_db.AppDatabase;
import com.penguinstech.cloudy.room_db.Task;
import com.penguinstech.cloudy.room_db.TaskDao;
import com.penguinstech.cloudy.utils.Util;
import com.penguinstech.cloudy.utils.Configs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddTaskActivity extends AppCompatActivity {

    FirebaseFirestore db;
    AppDatabase localDatabase;
    Map<String, Object> taskInfo = new HashMap<>();
    TaskDao taskDao;
    Task receivedTask = null;

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
                AppDatabase.class, Configs.DatabaseName).build();
        taskDao = localDatabase.taskDao();
        EditText titleET1 = findViewById(R.id.title_ET);
        EditText descriptionET1 = findViewById(R.id.description_ET);

        Intent intent = getIntent();
        if(intent.getStringExtra("id") != null) {
            receivedTask = new Task(
                    Integer.parseInt(intent.getStringExtra("id")),
                    intent.getStringExtra("task_id"),
                    intent.getStringExtra("title"),
                    intent.getStringExtra("description"),
                    intent.getStringExtra("updatedAt"),
                    Integer.parseInt(intent.getStringExtra("isDeleted"))
                    );
            titleET1.setText(receivedTask.title);
            descriptionET1.setText(receivedTask.description);
        }

        findViewById(R.id.addToLocalNoteBtn).setOnClickListener(v->{

            saveData(titleET1, descriptionET1);

        });
//        findViewById(R.id.addToFirestoreNoteBtn).setOnClickListener(v->{
//            saveData(titleET1, descriptionET1, 2);
//        });
//        findViewById(R.id.addNoteBtn).setOnClickListener(v->{
//            saveData(titleET1, descriptionET1, 3);
//        });
    }


    private void saveData(EditText titleET, EditText descriptionET) {

        String title = titleET.getText().toString();
        String description = descriptionET.getText().toString();

        if(title.equals("")) {
            titleET.setError("This field is required");
        }else if (description.equals("")) {
            descriptionET.setError("This field is required");
        }else {

            //if received taskk is not null,update room db else add new task to room db
            if(receivedTask != null){
                receivedTask.title = title;
                receivedTask.description = description;
                receivedTask.updatedAt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date());
                new Thread(()->{
                    taskDao.update(receivedTask);
                }).start();
            }else {

                List<Task> taskList = new ArrayList<>();
                taskInfo.put("taskId", UUID.randomUUID().toString());
                taskInfo.put("title", title);
                taskInfo.put("description", description);
                taskInfo.put("updatedAt", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date()));


                //save to local
                taskList.add(Util.convertMapToTaskObject(taskInfo));
                Util.saveDataToRoomDb(taskDao, taskList);
            }

            Toast.makeText(AddTaskActivity.this, "Document added successfully", Toast.LENGTH_LONG).show();
            finish();
        }
    }


}