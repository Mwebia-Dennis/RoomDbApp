package com.penguinstech.roomdbapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;

import com.penguinstech.roomdbapp.room_db.AppDatabase;
import com.penguinstech.roomdbapp.room_db.Files;
import com.penguinstech.roomdbapp.utils.Configs;
import com.penguinstech.roomdbapp.utils.FileAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ImageActivity extends AppCompatActivity {

    List<Files> filesList;
    AppDatabase localDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        init();
    }

    private void init() {
        localDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, Configs.DatabaseName).build();
        filesList = new ArrayList<>();
        getPermission();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.images_activity_menu, menu);
        return true;
    }


    public final int MY_PERMISSIONS_REQUEST_READ_IMAGES = 2323;

    private void getPermission() {

        if(Build.VERSION.SDK_INT >= 23 &&
                Objects.requireNonNull(this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_IMAGES);
            }
        }else {

            getAllFiles();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == MY_PERMISSIONS_REQUEST_READ_IMAGES && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            getAllFiles();
        } else {

            Toast.makeText(this, "Could not get permission to access your images", Toast.LENGTH_SHORT).show();
        }
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Uri data = result.getData().getData();
                    new Thread(()->{
                        List<Files> newFilesList = new ArrayList<>();
                        String deviceId= Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                        newFilesList.add(new Files(
                                deviceId,
                                data.toString(),
                                "",
                                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH).format(new Date()),
                                0
                        ));
                        localDatabase.fileDao().insertAll(newFilesList);
                        getAllFiles();
                    }).start();


                }
            });

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.addFile) {

            //open gallery app


            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            someActivityResultLauncher.launch(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getAllFiles();
    }

    public void getAllFiles() {

        new Thread() {
            @Override
            public void run() {

                filesList.clear();
                filesList = localDatabase.fileDao().getNonDeletedFiles();
                try {
                    runOnUiThread(() -> {

                        Log.d("Received list", String.valueOf(filesList.size()));
                        updateUi(filesList);
                    });
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void updateUi (List<Files> listOfFiles) {

        FileAdapter adapter = new FileAdapter(ImageActivity.this, listOfFiles);
        GridView gridView = findViewById(R.id.gallery_grid_view);
        gridView.setAdapter(adapter);
    }


}