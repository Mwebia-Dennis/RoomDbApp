package com.penguinstech.cloudy.sync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class SyncReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
        {

            //used to restart the scheduler at phone boot time
            //the receiver is registered to manifest to receive broadcast when phone is booted up
            setScheduler(context);

        }else {

            Intent i = new Intent(context, SyncService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(i);
            else
                context.startService(i);
        }
    }

    public void setScheduler(Context context) {


        //check if the scheduler has been set
        //if not set the scheduler
        if (!isSchedulerSet(context))
        {
            //set up the alarm manager and the reference point ie pending intent
            //set repeating scheduler which repeats after 24 hours at midnight
            Calendar midnight = Calendar.getInstance();
            midnight.set(Calendar.HOUR_OF_DAY, 12);
            midnight.set(Calendar.AM_PM, Calendar.AM);
//            midnight.set(Calendar.HOUR, 1);
            midnight.set(Calendar.MINUTE, 0);
            midnight.set(Calendar.SECOND, 0);
//            midnight.set(Calendar.AM_PM, Calendar.PM);

            AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, SyncReceiver.class);
            //set action so as the onReceive is triggered
            //the action should be the same as the declared action name in the manifest
            intent.setAction("com.penguinstech.cloudy.sync.scheduler");
            PendingIntent pi = PendingIntent.getBroadcast(context, 0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);//note flag_update_current which tells system how to handle new and existing pending intent
            am.setRepeating(AlarmManager.RTC_WAKEUP,
                    midnight.getTimeInMillis(), 1000 * 60 * 5, pi); // Millisec * Second * Minute  = 1 hour

        }
    }

    private boolean isSchedulerSet(Context context) {
        return (PendingIntent.getBroadcast(context, 0,
                new Intent(context, SyncReceiver.class),
                PendingIntent.FLAG_NO_CREATE) != null);
    }
}