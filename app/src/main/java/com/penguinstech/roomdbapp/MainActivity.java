package com.penguinstech.roomdbapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    FirebaseFirestore db;
    AppDatabase localDatabase;
    Map<String, String> taskInfo = new HashMap<>();
    TaskDao taskDao;
    NotesAdapter adapter;
    List<Task> allTasks;
    Boolean hasDataLoaded = false;

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
        }
        return super.onOptionsItemSelected(item);
    }

    public void init() {
        FirebaseApp.initializeApp(MainActivity.this);
        db = FirebaseFirestore.getInstance();
        localDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, Configs.DatabaseName).build();
        taskDao = localDatabase.taskDao();
        getAllNotes();

    }

    @Override
    protected void onResume() {
        super.onResume();
        getAllNotes();
    }

    public void getAllNotes() {
        new Thread() {
            @Override
            public void run() {

                allTasks = taskDao.getAll();
                try {
                    runOnUiThread(() -> {

                        if(allTasks.size() > 0) {
                            updateUi(allTasks);
                        }else {
                            getNotesFromFirestore();
                        }
                    });
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void updateUi (List<Task> list) {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
        adapter = new NotesAdapter(MainActivity.this, list);
        recyclerView.setAdapter(adapter);
    }
    public void getNotesFromFirestore() {
        Snackbar.make(findViewById(R.id.mainLayout), "Syncing data, please wait...",
                Snackbar.LENGTH_LONG)
                .show();
        db.collection(Configs.userId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("from firestore", "onSuccess: LIST EMPTY");
                        Snackbar.make(findViewById(R.id.mainLayout), "User has no data backed up",
                                Snackbar.LENGTH_LONG)
                                .show();
                    } else {
                        // get all data and add to database
                        if (!hasDataLoaded){
                            allTasks = queryDocumentSnapshots.toObjects(Task.class);

                            Log.d("size", String.valueOf(allTasks.size()));
                            Util.saveDataToRoomDb(taskDao, allTasks);
                            updateUi(allTasks);
                            Log.d("from firestore"
                                    , "onSuccess: " + allTasks);
                            hasDataLoaded = true;
                        }
                    }
        })
                .addOnFailureListener(e -> {

                    Log.d("from firestore", "onFailure: True");
                    Toast.makeText(MainActivity.this, "could not load documents", Toast.LENGTH_SHORT).show();
                });
    }

}