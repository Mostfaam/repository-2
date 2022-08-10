package com.xecan.chemobarcodescannerr;

/**
 * Created by femi on 2/17/16.
 */
import android.app.Application;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

//import com.google.android.gms.security.ProviderInstaller;

public class App extends Application
{
    public static final String TAG = "ChemoBarcode";

    public static final int MSG_FINISH_ACTIVITY = 1;
    public static final int MSG_DESELECT_TAB = 2;
    public static final int MSG_SUCCESS = 3;
    public static final int MSG_TIMEOUT = 4;
    public static final int MSG_DOWNLOADING = 5;
    public static final int MSG_START_SCAN = 6;
    public static final int MSG_STOP_SCAN = 7;
    public static final int MSG_CONFIGURED = 8;
    public static final int MSG_PROCESS_BARCODE = 9;
    public static final int MSG_CHECK_DATABASE = 10;
    public static final int MSG_AWAKE = 11;

    static String EXTRA_PERSIST_NOTIFICATION = "io.mailform.android.App.EXTRA_INFO_DURATION";
    static String ACTION_NOTIFY_INFO = "io.mailform.android.App.NOTIFY_INFO";
    static String ACTION_NOTIFY_SUCCESS = "io.mailform.android.App.NOTIFY_SUCCESS";
    static String ACTION_NOTIFY_ERROR = "io.mailform.android.App.NOTIFY_ERROR";
    static String ACTION_NOTIFY_COMMAND = "io.mailform.android.App.NOTIFY_COMMAND";

    public void onCreate()
    {
        super.onCreate();

        /*
        new Thread(new Runnable(){
            public void run()
            {
                int cnt = db.hourlyDataDao().getAll().size();
                cnt = cnt + 1;
            }
        }).start();
        */
        /*
        if (android.os.Build.VERSION.SDK_INT < 21) {
            try {
                ProviderInstaller.installIfNeededAsync(this, new ProviderInstaller.ProviderInstallListener() {
                    @Override
                    public void onProviderInstalled() {
                    }

                    @Override
                    public void onProviderInstallFailed(int i, Intent intent) {
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */
    }

    static void notifySuccess(LocalBroadcastManager lbm, String message)
    {
        notifySuccess(lbm, message, false);
    }

    static void notifySuccess(LocalBroadcastManager lbm, String message, boolean persist)
    {
        Intent i = new Intent();
        i.setAction(ACTION_NOTIFY_SUCCESS);
        i.putExtra(Intent.EXTRA_TEXT, message);
        if(persist) i.putExtra(EXTRA_PERSIST_NOTIFICATION, true);
        lbm.sendBroadcast(i);
    }

    static void notifyError(LocalBroadcastManager lbm, String message)
    {
        Intent i = new Intent();
        i.setAction(ACTION_NOTIFY_ERROR);
        i.putExtra(Intent.EXTRA_TEXT, message);
        lbm.sendBroadcast(i);
    }

    static void notifyInfo(LocalBroadcastManager lbm, String message)
    {
        notifyInfo(lbm, message, false);
    }

    static void notifyInfo(LocalBroadcastManager lbm, String message, boolean persist)
    {
        Intent i = new Intent();
        i.setAction(ACTION_NOTIFY_INFO);
        i.putExtra(Intent.EXTRA_TEXT, message);
        if(persist) i.putExtra(EXTRA_PERSIST_NOTIFICATION, true);
        lbm.sendBroadcast(i);
    }

    static void notifyCommand(LocalBroadcastManager lbm, int command)
    {
        Intent i = new Intent();
        i.setAction(ACTION_NOTIFY_COMMAND);
        i.putExtra(Intent.EXTRA_INTENT, command);
        lbm.sendBroadcast(i);
    }
}
