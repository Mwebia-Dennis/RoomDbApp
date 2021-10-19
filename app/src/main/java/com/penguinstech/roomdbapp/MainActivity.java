package com.penguinstech.roomdbapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.penguinstech.roomdbapp.room_db.AppDatabase;
import com.penguinstech.roomdbapp.room_db.Task;
import com.penguinstech.roomdbapp.room_db.TaskDao;
import com.penguinstech.roomdbapp.utils.Util;
import com.penguinstech.roomdbapp.sync.SyncWorker;
import com.penguinstech.roomdbapp.utils.Configs;
import com.penguinstech.roomdbapp.utils.NotesAdapter;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    FirebaseFirestore db;
    AppDatabase localDatabase;
//    Map<String, String> taskInfo = new HashMap<>();
    TaskDao taskDao;
    NotesAdapter adapter;
    List<Task> allTasks;
    ContentObserver mObserver;
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
            Intent i = new Intent(MainActivity.this, AddTaskActivity.class);
            i.putExtra("task", "test");
            startActivity(i);
        }else if (item.getItemId() == R.id.subscribe) {
            startActivity(new Intent(MainActivity.this, SubscribeActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    public void init() {
        checkIfUserIsLoggedIn();
        FirebaseApp.initializeApp(MainActivity.this);
        db = FirebaseFirestore.getInstance();
        localDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, Configs.DatabaseName).build();
        taskDao = localDatabase.taskDao();
        Snackbar.make(findViewById(R.id.mainLayout), "Loading data please wait...", Snackbar.LENGTH_SHORT).show();
        getAllNotes();

        mAccount = CreateSyncAccount(this);
//        ContentResolver mResolver = getContentResolver();
        /*
         * Turn on periodic syncing after every 1 hr
         */

        ContentResolver.addPeriodicSync(
                mAccount,
                Configs.AUTHORITY,
                Bundle.EMPTY,
                (60 * 60));

        final PeriodicWorkRequest periodicWorkRequest
                = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(periodicWorkRequest);

        //register an observer to notify when room db is updated
        mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            public void onChange(boolean selfChange) {
                Log.d("SyncAdapter notif: ", "received");
                try {
                    //delay for a second then check if db is updated
                    TimeUnit.MILLISECONDS.sleep(100);
                    Snackbar.make(findViewById(R.id.mainLayout), "Sync results updated successfully", Snackbar.LENGTH_LONG).show();
                    getAllNotes();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        getContentResolver().registerContentObserver(Configs.URI_TASK, true, mObserver);

    }

    private void checkIfUserIsLoggedIn() {

        Log.d("username", Util.getUserName(MainActivity.this));
        if (Util.getUserName(MainActivity.this).equals("")) {
            startActivity(new Intent(MainActivity.this, AuthenticacteActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfUserIsLoggedIn();
        getAllNotes();
    }

    public void getAllNotes() {

        new Thread() {
            @Override
            public void run() {

                allTasks = taskDao.getNonDeletedTasks();
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