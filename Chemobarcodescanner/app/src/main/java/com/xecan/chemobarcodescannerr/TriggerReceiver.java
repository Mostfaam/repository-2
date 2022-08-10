package com.xecan.chemobarcodescannerr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

/**
 * Created by femi on 4/4/18.
 */

public class TriggerReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        /*
        // print it out
        Log.d("ActivityManager", intent.getAction()+"/"+intent.getCategories()+"/"+intent.toUri(Intent.URI_INTENT_SCHEME).toString());
        Bundle b = intent.getExtras();
        for(String s : b.keySet()){
            Log.d("ActivityManager", "["+s+"] => "+String.valueOf(b.get(s)));
        }
        */

        // get the key event
        if(intent.hasExtra(Intent.EXTRA_KEY_EVENT)){
            KeyEvent ev = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            // [android.intent.extra.KEY_EVENT] => KeyEvent { action=ACTION_DOWN, keyCode=KEYCODE_SCAN_TRIG_GER1, scanCode=0, metaState=0, flags=0x0, repeatCount=0, eventTime=4835147000000000, downTime=4835147000000000, deviceId=-1, source=0x0 }
            // [android.intent.extra.KEY_EVENT] => KeyEvent { action=ACTION_UP, keyCode=KEYCODE_SCAN_TRIG_GER1, scanCode=0, metaState=0, flags=0x0, repeatCount=0, eventTime=4835147290000000, downTime=4835147290000000, deviceId=-1, source=0x0 }
            if(KeyEvent.ACTION_DOWN == ev.getAction()) {
                Intent i = new Intent(context, Home.class);
                i.putExtra(Intent.EXTRA_INTENT, "wakeup");
                context.startActivity(i);

                i = new Intent(context, ScannerService.class);
                i.putExtra(Intent.EXTRA_INTENT, App.MSG_START_SCAN);
                context.startService(i);
            }else if(KeyEvent.ACTION_UP == ev.getAction()) {
                Intent i = new Intent(context, ScannerService.class);
                i.putExtra(Intent.EXTRA_INTENT, App.MSG_STOP_SCAN);
                context.startService(i);

                i = new Intent(context, ScannerService.class);
                i.putExtra(Intent.EXTRA_INTENT, App.MSG_STOP_SCAN);
                context.startService(i);
            }
        }
/*
        StringBuilder sb = new StringBuilder();
        sb.append("Action: " + intent.getAction() + "\n");
        sb.append("URI: " + intent.toUri(Intent.URI_INTENT_SCHEME).toString() + "\n");
        String log = sb.toString();
        Log.d(App.TAG, log);
        Toast.makeText(context, log, Toast.LENGTH_LONG).show();
        */
    }
}
