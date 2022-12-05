package com.app.AltisSocket;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyService extends Service {

    ScheduledExecutorService myschedule_executor;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        myschedule_executor = Executors.newScheduledThreadPool(1);
        myschedule_executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                MainActivity.textView.setText("Current Time: " + new Date());
            }
        }, 1, 1, TimeUnit.SECONDS);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myschedule_executor.shutdown();
    }
}
