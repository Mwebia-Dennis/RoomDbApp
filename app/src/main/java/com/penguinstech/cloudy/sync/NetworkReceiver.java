package com.penguinstech.cloudy.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.format.DateUtils;

import androidx.room.Room;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.penguinstech.cloudy.MainActivity;
import com.penguinstech.cloudy.room_db.AppDatabase;
import com.penguinstech.cloudy.room_db.Token;
import com.penguinstech.cloudy.utils.Configs;
import com.penguinstech.cloudy.utils.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class NetworkReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        final android.net.NetworkInfo wifi = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        final android.net.NetworkInfo mobile = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifi.isAvailable() || mobile.isAvailable()) {

            //check if internet is available
            //get user name and token
            //if the data had already been synced then dont sync
            //else set scheduler to sync
            // since the scheduler is set to run at midnight
            // it means that the sync will run immediately because
            // token last sync time is passed

            String userName = Util.getUserName(context);
            if (!userName.equals("")) {
                AppDatabase localDatabase = Room.databaseBuilder(context,
                        AppDatabase.class, Configs.DatabaseName).build();
                new Thread(()->{
                    Token token = localDatabase.tokenDao().loadLastSyncToken(Configs.tableName);
                    if(token != null && token.lastSync.equals("")) {
                        try {

                            Date date;
                            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
                            date = format.parse(token.lastSync);
                            if (date != null && !DateUtils.isToday(date.getTime()))
                                new SyncReceiver().setScheduler(context);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }
}
