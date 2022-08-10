package com.xecan.chemobarcodescannerr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by femi on 4/4/18.
 */

public class PlugInReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent i = new Intent(context, ScannerService.class);
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_POWER_CONNECTED))
        {
            i.putExtra(Intent.EXTRA_INTENT, App.MSG_STOP_SCAN);
            context.startService(i);
        }
        else if(action.equals(Intent.ACTION_POWER_DISCONNECTED))
        {
            //i.putExtra(Intent.EXTRA_INTENT, App.MSG_START_SCAN);
            //context.startService(i);

            // start the activity
            i = new Intent(context, Home.class);
            context.startActivity(i);
        }
    }
}
