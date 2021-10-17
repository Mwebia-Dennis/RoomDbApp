package com.penguinstech.roomdbapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    FirebaseFirestore db;
    AppDatabase localDatabase;
//    Map<String, String> taskInfo = new HashMap<>();
    TaskDao taskDao;
    NotesAdapter adapter;
    List<Task> allTasks;
    ContentObserver mObserver;


    // Instance fields
    Account mAccount;

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
        }else if (item.getItemId() == R.id.subscribe) {
            startActivity(new Intent(MainActivity.this, SubscribeActivity.class));
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
        mAccount = CreateSyncAccount(this);
//        ContentResolver mResolver = getContentResolver();
        /*
         * Turn on periodic syncing
         */


        ContentResolver.addPeriodicSync(
                mAccount,
                Configs.AUTHORITY,
                Bundle.EMPTY,
                70);

        Log.i("mACCOUNT", "PASSED");


        //register an observer to notify when room db is updated
        mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            public void onChange(boolean selfChange) {
                Log.d("SyncAdapter notif: ", "received");
                try {
                    //delay for a second then check if db is updated
                    TimeUnit.MILLISECONDS.sleep(100);
                    getAllNotes();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        getContentResolver().registerContentObserver(Configs.URI_TASK, true, mObserver);

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

                        Log.d("Received list", String.valueOf(allTasks.size()));
                        updateUi(allTasks);
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




    /**
     * Create a new placeholder account for the sync adapter
     *
     * @param context The application context
     */
    public static Account CreateSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                Configs.ACCOUNT, Configs.ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        ACCOUNT_SERVICE);
        Log.i("mACCOUNT FUN", "PASSED");


        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {

            ContentResolver.setIsSyncable(newAccount, Configs.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(newAccount, Configs.AUTHORITY, true);


        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */

            Log.d("Account", "exists");
        }


        return  newAccount;
    }



}