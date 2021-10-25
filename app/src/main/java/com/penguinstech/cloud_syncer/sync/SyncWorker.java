package com.penguinstech.cloud_syncer.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.penguinstech.cloud_syncer.utils.Configs;
import com.penguinstech.cloud_syncer.MainActivity;

public class SyncWorker extends Worker {
    Context context;
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        //run the sync adapter
        forceSyncing();
        // Indicate whether the work finished successfully with the Result
        return Result.success();
    }

    private void forceSyncing() {

        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        /*
         * Request the sync for the default account, authority, and
         * manual sync settings
         */
        ContentResolver.requestSync(MainActivity.CreateSyncAccount(context), Configs.AUTHORITY, settingsBundle);
    }
}
