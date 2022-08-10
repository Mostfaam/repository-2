package com.xecan.chemobarcodescannerr;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ConnectionState;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.Scanner.TriggerType;
import com.symbol.emdk.barcode.StatusData.ScannerStates;
import com.symbol.emdk.barcode.StatusData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by femi on 4/4/18.
 */

public class ScannerService extends Service implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener
{
    private static ScannerService instance;

    private IBinder mBinder;

    private EMDKManager emdkManager;
    private BarcodeManager barcodeManager;
    private Scanner scanner;

    private List<ScannerInfo> deviceList;
    private List<String> friendlyNameList;

    private int defaultIndex = 0; // Keep the default scanner

    private ScannerInfo defaultScanner;

    private ScannerStates latestScannerState;

    private boolean stop;

    public static class ScannerServiceBinder extends Binder
    {
        ScannerService getService() {
            return instance;
        }
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return mBinder;
    }

    public ScannerService()
    {
        super();

        emdkManager = null;
        barcodeManager = null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        deviceList = null;
        scanner = null;
        latestScannerState = null;
        stop = false;

        android.util.Log.v(App.TAG,"onCreate! "+barcodeManager+" => "+emdkManager);

        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            android.util.Log.v(App.TAG, "Status: " + "EMDKManager object request failed!");
        }

        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(powerMonitor, screenStateFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent != null) {
            if (intent.getIntExtra(Intent.EXTRA_INTENT, 0) == App.MSG_START_SCAN) {
                android.util.Log.v(App.TAG, "Starting scan");
                startScan();
            } else if (intent.getIntExtra(Intent.EXTRA_INTENT, 0) == App.MSG_STOP_SCAN) {
                msgHandler.removeMessages(App.MSG_START_SCAN);
                stopScan();
                android.util.Log.v(App.TAG, "Stopping scan");
            }
        }

        return START_STICKY;
    }


    private BroadcastReceiver powerMonitor = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                msgHandler.removeMessages(App.MSG_START_SCAN);
                stopScan();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {

            }
        }
    };

    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case App.MSG_START_SCAN:
                    ScannerService.this.startScan();
                    break;
            }
        }
    };

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        unregisterReceiver(powerMonitor);

        // De-initialize scanner
        deInitScanner();

        // Remove connection listener
        if (barcodeManager != null) {
            barcodeManager.removeConnectionListener(this);
            barcodeManager = null;
        }

        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;

        }

    }

    private synchronized void setupScanner()
    {
        if(barcodeManager == null) {
            // Acquire the barcode manager resources
            barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);
        }
        android.util.Log.v(App.TAG, "Status: 2");

        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);

            android.util.Log.v(App.TAG, "Status: 3:"+barcodeManager);

            // Enumerate scanner devices
            enumerateScannerDevices();

            android.util.Log.v(App.TAG, "Status: 4");
            initScanner();
            android.util.Log.v(App.TAG, "Status: 5");
            setTrigger();
            android.util.Log.v(App.TAG, "Status: 6");
            setDecoders();
            android.util.Log.v(App.TAG, "Status: 7");
        }else{
            android.util.Log.v(App.TAG, "Status: 8");
            android.util.Log.v(App.TAG, "(EMDK) Barcode manager not received.");
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager)
    {
        android.util.Log.v(App.TAG, "Status: " + "EMDK open success!");

        this.emdkManager = emdkManager;

        android.util.Log.v(App.TAG, "Status: 1");

        setupScanner();
    }

    @Override
    public void onClosed() {

        if (emdkManager != null) {
            // Remove connection listener
            if (barcodeManager != null){
                barcodeManager.removeConnectionListener(this);
                barcodeManager = null;
            }

            // Release all the resources
            emdkManager.release();
            emdkManager = null;
        }
        android.util.Log.v(App.TAG, "Status: " + "EMDK closed unexpectedly! Please close and restart the application.");
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {

        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList<ScanData> scanData = scanDataCollection.getScanData();
            for(ScanData data : scanData) {

                String dataString =  data.getData();

                android.util.Log.v(App.TAG, "Received scan data ["+dataString+"]");
                //new AsyncDataUpdate().execute(dataString);

                LinkService.startProcessBarcode(this, dataString);

                if("444444".equals(dataString)) {
                    Intent i = new Intent(this, Home.class);
                    i.setAction(Intent.ACTION_VIEW);
                    i.putExtra(Intent.EXTRA_TEXT, getString(R.string.label_logout));
                    startActivity(i);
                }else if(!dataString.startsWith("X:")) {
                    Intent i = new Intent(this, Home.class);
                    i.setAction(Intent.ACTION_VIEW);
                    i.putExtra(Intent.EXTRA_TEXT, dataString);
                    startActivity(i);
                }
            }
        }
    }

    @Override
    public void onStatus(StatusData statusData) {

        ScannerStates state = statusData.getState();
        latestScannerState = state;
        String statusString = "<NONE>";
        switch(state) {
            case IDLE:
                statusString = statusData.getFriendlyName()+" is enabled and idle ("+stop+")...";
                if(stop){
                    stop = false;
                }else {
                    try {
                        // An attempt to use the scanner continuously and rapidly (with a delay < 100 ms between scans)
                        // may cause the scanner to pause momentarily before resuming the scanning.
                        // Hence add some delay (>= 100ms) before submitting the next read.
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        scanner.read();
                    } catch (ScannerException e) {
                        statusString = "[Exception] " + e.getMessage();
                        //msgHandler.sendEmptyMessageDelayed(App.MSG_START_SCAN, 100);
                    }
                }
                break;
            case WAITING:
                statusString = "Scanner is waiting for trigger press...";
                break;
            case SCANNING:
                statusString = "Scanning...";
                break;
            case DISABLED:
                statusString = statusData.getFriendlyName()+" is disabled.";
                break;
            case ERROR:
                statusString = "An error has occurred.";
                break;
            default:
                break;
        }
        android.util.Log.v(App.TAG, "Processed: "+statusString);
    }

    private void setTrigger() {

        if (scanner == null) {
            initScanner();
        }

        if (scanner != null) {
            scanner.triggerType = TriggerType.SOFT_ALWAYS;
        }
    }

    private void setDecoders() {

        if (scanner == null) {
            initScanner();
        }

        if ((scanner != null) && (scanner.isEnabled())) {
            try {

                ScannerConfig config = scanner.getConfig();

                // Set EAN8
                config.decoderParams.ean8.enabled = true;

                // Set EAN13
                config.decoderParams.ean13.enabled = true;

                // Set Code39
                config.decoderParams.code39.enabled = true;

                //Set Code128
                config.decoderParams.code128.enabled = true;

                scanner.setConfig(config);
            } catch (ScannerException e) {

                android.util.Log.v(App.TAG,"Status: " + e.getMessage());
            }
        }
    }


    private void startScan()
    {
        if(scanner == null) {
            initScanner();
        }

        if (scanner != null) {
            try {
                if(scanner.isEnabled())
                {
                    if(latestScannerState != null && (latestScannerState == ScannerStates.IDLE || latestScannerState == ScannerStates.WAITING))
                    {
                        // Submit a new read.
                        scanner.read();
                    }else{
                        msgHandler.sendEmptyMessageDelayed(App.MSG_START_SCAN, 100);
                    }
                    /*
                    if (checkBoxContinuous.isChecked())
                        bContinuousMode = true;
                    else
                        bContinuousMode = false;

                    new AsyncUiControlUpdate().execute(false);
                    */
                }
                else
                {
                    android.util.Log.v(App.TAG,"Status: Scanner is not enabled");
                }

            } catch (ScannerException e) {
                android.util.Log.v(App.TAG,"Status: " + e.getMessage());
                //msgHandler.sendEmptyMessageDelayed(App.MSG_START_SCAN, 1000);
            }
        }else{
            msgHandler.sendEmptyMessageDelayed(App.MSG_START_SCAN, 1000);
            android.util.Log.v(App.TAG,"Status: scanner not yet available");
        }
    }

    private void stopScan() {

        if (scanner != null) {

            try {

                // Reset continuous flag
                //bContinuousMode = false;

                stop = true;

                // Cancel the pending read.
                scanner.cancelRead();

                //new AsyncUiControlUpdate().execute(true);

            } catch (ScannerException e) {

                android.util.Log.v(App.TAG,"Status: " + e.getMessage());
            }
        }
    }

    private void initScanner()
    {
        if (scanner == null && barcodeManager != null) {

            if ((deviceList != null) && (deviceList.size() != 0)) {
                scanner = barcodeManager.getDevice(defaultScanner);
            }
            else {
                android.util.Log.v(App.TAG,"Status: " + "Failed to get the specified scanner device! Please close and restart the application.");
                return;
            }

            if (scanner != null) {

                scanner.addDataListener(this);
                scanner.addStatusListener(this);

                try {
                    scanner.enable();
                } catch (ScannerException e) {

                    android.util.Log.v(App.TAG,"Status: " + e.getMessage());
                }
            }else{
                android.util.Log.v(App.TAG,"Status: " + "Failed to initialize the scanner device.");
            }
        }else{
            android.util.Log.v(App.TAG,"Barcode Manager not available.");
            if(emdkManager != null) setupScanner();
        }
    }

    private void deInitScanner()
    {
        if (scanner != null) {

            try {

                scanner.cancelRead();
                scanner.disable();

            } catch (Exception e) {

                android.util.Log.v(App.TAG, "Status: " + e.getMessage());
            }

            try {
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);

            } catch (Exception e) {

                android.util.Log.v(App.TAG,"Status: " + e.getMessage());
            }

            try{
                scanner.release();
            } catch (Exception e) {

                android.util.Log.v(App.TAG,"Status: " + e.getMessage());
            }

            scanner = null;
        }
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, ConnectionState connectionState)
    {
        String status;
        String scannerName = "";

        String statusExtScanner = connectionState.toString();
        String scannerNameExtScanner = scannerInfo.getFriendlyName();

        if (deviceList.size() != 0) {
            //scannerName = deviceList.get(scannerIndex).getFriendlyName();
        }

        if (scannerName.equalsIgnoreCase(scannerNameExtScanner))
        {
            switch(connectionState) {
                case CONNECTED:
                    deInitScanner();
                    initScanner();
                    setTrigger();
                    setDecoders();
                    break;
                case DISCONNECTED:
                    deInitScanner();
                    //new AsyncUiControlUpdate().execute(true);
                    break;
            }

            status = scannerNameExtScanner + ":" + statusExtScanner;
            //new AsyncStatusUpdate().execute(status);
        } else {
            //status =  statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            //new AsyncStatusUpdate().execute(status);
        }
    }

    private void enumerateScannerDevices() {

        if (barcodeManager != null) {

            friendlyNameList = new ArrayList<String>();

            deviceList = barcodeManager.getSupportedDevicesInfo();

            if ((deviceList != null) && (deviceList.size() != 0)) {
                Iterator<ScannerInfo> it = deviceList.iterator();
                while(it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    friendlyNameList.add(scnInfo.getFriendlyName());
                    if(scnInfo.isDefaultScanner()) {
                        defaultScanner = scnInfo;
                    }
                }
            }
            else {
                android.util.Log.v(App.TAG,"Status: " + "Failed to get the list of supported scanner devices! Please close and restart the application.");
            }
        }
    }

}
