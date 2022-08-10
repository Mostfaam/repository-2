package com.xecan.chemobarcodescannerr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.snackbar.Snackbar;

public class BaseDisplayActivity extends AppCompatActivity
{
    private boolean isBroadcastRegistered;
    private Snackbar currentBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isBroadcastRegistered = false;
        currentBar = null;
    }

    protected void onPause() {
        super.onPause();

        if (isBroadcastRegistered) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(notifier);
            } catch (IllegalArgumentException e) {
                // Do nothing
            }
            isBroadcastRegistered = false;
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (!isBroadcastRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(App.ACTION_NOTIFY_SUCCESS);
            filter.addAction(App.ACTION_NOTIFY_ERROR);
            filter.addAction(App.ACTION_NOTIFY_INFO);
            filter.addAction(App.ACTION_NOTIFY_COMMAND);
            LocalBroadcastManager.getInstance(this).registerReceiver(notifier, filter);
            isBroadcastRegistered = true;
        }
    }

    /*
    protected void handleNotification(Intent i)
    {
        if (App.ACTION_NOTIFY_ERROR.equals(i.getAction())) {
            SpannableString msg = new SpannableString(i.getStringExtra(Intent.EXTRA_TEXT));
            msg.setSpan(new ForegroundColorSpan(Color.RED), 0, msg.length(), 0);
            Snackbar bar = currentBar != null ? currentBar : Snackbar.make(findViewById(android.R.id.content), msg, i.hasExtra(App.EXTRA_PERSIST_NOTIFICATION) ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
            View snackbarView = bar.getView();
            TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            currentBar = null;
            if(bar == currentBar) {
                textView.setText(msg);
                bar.setDuration(i.hasExtra(App.EXTRA_PERSIST_NOTIFICATION) ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
                bar.show();
            }else {
                textView.setMaxLines(5);
                bar.setCallback(sb);
                bar.show();
            }
        } else if (App.ACTION_NOTIFY_SUCCESS.equals(i.getAction())) {
            SpannableString msg = new SpannableString(i.getStringExtra(Intent.EXTRA_TEXT));
            msg.setSpan(new ForegroundColorSpan(Color.GREEN), 0, msg.length(), 0);
            Snackbar bar = currentBar != null ? currentBar : Snackbar.make(findViewById(android.R.id.content), msg, i.hasExtra(App.EXTRA_PERSIST_NOTIFICATION) ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
            View snackbarView = bar.getView();
            TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            currentBar = null;
            if(bar == currentBar) {
                textView.setText(msg);
                bar.setDuration(i.hasExtra(App.EXTRA_PERSIST_NOTIFICATION) ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
                bar.show();
            }else {
                textView.setMaxLines(5);
                bar.setCallback(sb);
                bar.show();
            }
        } else if (App.ACTION_NOTIFY_INFO.equals(i.getAction())) {
            Snackbar bar = currentBar != null ? currentBar : Snackbar.make(findViewById(android.R.id.content), i.getStringExtra(Intent.EXTRA_TEXT), i.hasExtra(App.EXTRA_PERSIST_NOTIFICATION) ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
            View snackbarView = bar.getView();
            TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            currentBar = null;
            if(bar == currentBar) {
                textView.setText(i.getStringExtra(Intent.EXTRA_TEXT));
                bar.setDuration(i.hasExtra(App.EXTRA_PERSIST_NOTIFICATION) ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
                bar.show();
            }else {
                textView.setMaxLines(5);
                bar.setCallback(sb);
                bar.show();
            }
        }else{
            processNotification(i);
        }
    }
    */

    protected void handleNotification(Intent i)
    {
        if (App.ACTION_NOTIFY_ERROR.equals(i.getAction())) {
            SpannableString msg = new SpannableString(i.getStringExtra(Intent.EXTRA_TEXT));
            msg.setSpan(new ForegroundColorSpan(Color.RED), 0, msg.length(), 0);
            Snackbar bar = Snackbar.make(findViewById(android.R.id.content), msg, i.hasExtra(App.EXTRA_PERSIST_NOTIFICATION) ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
            View snackbarView = bar.getView();
/*
            TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setMaxLines(5);
*/
            bar.setCallback(sb);
            bar.show();
        } else if (App.ACTION_NOTIFY_SUCCESS.equals(i.getAction())) {
            SpannableString msg = new SpannableString(i.getStringExtra(Intent.EXTRA_TEXT));
            msg.setSpan(new ForegroundColorSpan(Color.GREEN), 0, msg.length(), 0);
            Snackbar bar = Snackbar.make(findViewById(android.R.id.content), msg, i.hasExtra(App.EXTRA_PERSIST_NOTIFICATION) ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
            View snackbarView = bar.getView();
        /*    TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setMaxLines(5);*/
            bar.setCallback(sb);
            bar.show();
        } else if (App.ACTION_NOTIFY_INFO.equals(i.getAction())) {
            Snackbar bar = Snackbar.make(findViewById(android.R.id.content), i.getStringExtra(Intent.EXTRA_TEXT), i.hasExtra(App.EXTRA_PERSIST_NOTIFICATION) ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
            View snackbarView = bar.getView();
/*
            TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setMaxLines(5);
*/
            bar.setCallback(sb);
            bar.show();
        }else{
            processNotification(i);
        }
    }

    protected void processNotification(Intent i)
    {

    }

    private BroadcastReceiver notifier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i)
        {
            handleNotification(i);
        }
    };

    private Snackbar.Callback sb = new Snackbar.Callback() {
        @Override
        public void onDismissed(Snackbar snackbar, int event) {
            super.onDismissed(snackbar, event);
            currentBar = null;
        }

        @Override
        public void onShown(Snackbar snackbar) {
            super.onShown(snackbar);
            currentBar = snackbar;
        }
    };
}
