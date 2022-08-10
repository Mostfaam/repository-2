package com.xecan.chemobarcodescannerr;

import android.app.Application;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import android.os.Handler;
import android.os.Message;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.xecan.chemobarcodescannerr.adapters.ChairAdapter;

import org.apache.commons.dbutils.QueryRunner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Home extends BaseDisplayActivity implements NavigationView.OnNavigationItemSelectedListener {
    private String lastText;

    private ScheduledExecutorService exec;
    private ExecutorService exec2;

    private OkHttpClient client;

    private SharedPreferences prefs;

    private AutoResizeTextView latestTag;
    private AutoResizeTextView chairName;
    LocalBroadcastManager lbm;

    private boolean mHomeDown;
    private boolean mBackDown;
    private QueryRunner q;
    RecyclerView rvChairs;

    String RFID = "000000000209000000444444";

    ArrayList<ChairInfo> chairInfos = new ArrayList<>();
    ChairAdapter chairAdapter;
    ImageView ivLogout;
    Thread thread;
    public String versionInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lbm = LocalBroadcastManager.getInstance(this);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            Application a = getApplication();
            PackageInfo pInfo = a.getPackageManager().getPackageInfo(a.getPackageName(), 0);
            versionInfo = pInfo.versionName + "/" + pInfo.versionCode;
        } catch (Exception ignored) {
        }
        ivLogout = findViewById(R.id.ivLogout);
        q = new QueryRunner();
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.v("ttt", e.getMessage());
        }

        exec = Executors.newScheduledThreadPool(3);
        exec2 = Executors.newSingleThreadExecutor();

        client = new OkHttpClient();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        fab.setVisibility(View.GONE);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        latestTag = (AutoResizeTextView) findViewById(R.id.latestTag);
        chairName = (AutoResizeTextView) findViewById(R.id.chairName);

        chairName.setText(prefs.getString("ChairName", ""));

        if (!prefs.getString("ChairName", "").isEmpty()) {
            startThread();
            ivLogout.setVisibility(View.VISIBLE);
        } else {
            ivLogout.setVisibility(View.GONE);
        }
        ivLogout.setOnClickListener(view -> {
            if (thread != null) {
                thread.interrupt();
            }

            HttpUrl url = HttpUrl.parse("http://" + prefs.getString(getString(R.string.pref_key_database_ip), "192.168.1.166") + ":12001/readerAgent/report").newBuilder().addQueryParameter("inputData", RFID).addQueryParameter("v", versionInfo).addQueryParameter("type", "barcode").build();
            Request req = new Request.Builder().url(url).build();
            try {
                Response res = client.newCall(req).execute();

                if (res.isSuccessful()) {
                    //String resp = res.body().string();
                    App.notifySuccess(lbm, "Logged out");
                } else {
                    // error: should show it in the main screen
                    App.notifyError(lbm, String.format(getString(R.string.message_processing_error), res.message()));
                }
                res.body().close();
            } catch (IOException e) {
                e.printStackTrace();
            }


        });
        rvChairs = findViewById(R.id.rvChairs);
        chairAdapter = new ChairAdapter(this, chairInfos);
        rvChairs.setAdapter(chairAdapter);

        rvChairs.setLayoutManager(new LinearLayoutManager(this));


        // Get the intent, verify the action and get the query
        handleIntent(getIntent());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        /*
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
        */
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        /*
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            showNetworkAddressDialog();
            return true;
        }
        */

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_settings) {
            Intent i = new Intent(this, com.xecan.chemobarcodescannerr.Settings.class);
            startActivity(i);
        } else if (id == R.id.system_settings) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } else if (id == R.id.restart) {
            // stop the service, then recreate the activity (which will start the service again)
            this.stopService(new Intent(this, ScannerService.class));
            recreate();
/*
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {
*/
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
        // start the scanner
        Intent i = new Intent(this, ScannerService.class);
        i.putExtra(Intent.EXTRA_INTENT, App.MSG_START_SCAN);
        startService(i);
        */
    }

    @Override
    protected void onPause() {
        super.onPause();

        // start the scanner
        //Intent i = new Intent(this, ScannerService.class);
        //i.putExtra(Intent.EXTRA_INTENT, App.MSG_STOP_SCAN);
        //startService(i);
    }

    @Override
    protected void onStop() {
        super.onStop();

        //Intent i = new Intent(this, ScannerService.class);
        //i.putExtra(Intent.EXTRA_INTENT, App.MSG_STOP_SCAN);
        //startService(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        latestTag = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            mBackDown = mHomeDown = false;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    mBackDown = true;
                    return true;
                case KeyEvent.KEYCODE_HOME:
                    mHomeDown = true;
                    return true;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    if (!event.isCanceled()) {
                        // Do BACK behavior.
                    }
                    mBackDown = true;
                    return true;
                case KeyEvent.KEYCODE_HOME:
                    if (!event.isCanceled()) {
                        // Do HOME behavior.
                    }
                    mHomeDown = true;
                    return true;
            }
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU ||
                event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN ||
                event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
            return super.dispatchKeyEvent(event);

        return true;
    }

    void showNetworkAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(prefs.getString("NETWORK_ADDRESS", "http://192.168.1.132:8010/"));
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateNetworkAddress(input.getText().toString());
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    void updateNetworkAddress(String address) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("NETWORK_ADDRESS", address);
        edit.commit();

        Toast.makeText(this, "Network address saved.", Toast.LENGTH_LONG).show();
    }

    private static final int MSG_1 = 1;
    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case App.MSG_AWAKE:
                    clearWindowFlags();
                    break;
                case MSG_1:
                    break;

            }
        }
    };

    void clearWindowFlags() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    void processBarcode(String query) {
        android.util.Log.v(App.TAG, "Received " + query);
        LinkService.startProcessBarcode(this, query);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Close the menu
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            getWindow().closeAllPanels();
        } else {
            handleIntent(intent);
        }
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            processBarcode(query);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Snackbar snackbar = Snackbar
                    .make(rvChairs, intent.getStringExtra(Intent.EXTRA_TEXT), Snackbar.LENGTH_LONG);
            snackbar.show();
        } else if ("wakeup".equals(intent.getStringExtra(Intent.EXTRA_INTENT))) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            msgHandler.sendEmptyMessageDelayed(App.MSG_AWAKE, 2000);
        }
    }

    protected void processNotification(Intent i) {
        if (i.getIntExtra(Intent.EXTRA_INTENT, 0) == App.MSG_CONFIGURED) {
            chairName.setText(prefs.getString("ChairName", ""));
            ivLogout.setVisibility(View.VISIBLE);
            startThread();
        }
    }

    public void startThread(){

        String chairId = prefs.getString("readerSN", "");
        String acctNum = prefs.getString("acctNum", "");

        if(thread!=null){
            thread.interrupt();
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ivLogout.setVisibility(View.VISIBLE);

                try {
                    String dburl = "jdbc:mysql://" + prefs.getString(getString(R.string.pref_key_database_ip), "192.168.1.166") + ":3306/edx0?autoReconnect=true&useSSL=false";
//                    String dburl = "jdbc:mysql://" +"192.168.1.109"+ ":3306/testa";

                    Connection connection = getConnection(dburl);
                    while (true) {
                        getChair(connection, acctNum, chairId);
//                        getChair(connection, "1002800003", "0000008001");
                        Thread.sleep(3000);
                    }
                } catch (SQLException | InterruptedException e) {
                    e.printStackTrace();
                    Log.v("ttt", e.getMessage());
                }
            }
        });
        thread.start();
    }

    ArrayList<Chair> getChair(Connection conn, String acctNum, String chairId) {
        try {
            Log.v("ttt","getChair");
            ArrayList<Chair> chairs = new ArrayList<>();
            ArrayList<ChairInfo> chairInfosInside = new ArrayList<>();
            ResultSet resultSet = conn.createStatement().executeQuery("SELECT * FROM chairs WHERE ID = " + chairId + " AND AcctNum = " + acctNum);
            Log.v("ttt", resultSet.getFetchSize() + "");
            while (resultSet.next()) {
                Chair chair = new Chair();
                chair.setChairStatus(resultSet.getString("ChairStatus"));
                chair.setChairName(resultSet.getString("ChairName"));
                chair.setAcctNum(resultSet.getString("AcctNum"));
                chair.setId(resultSet.getString("ID"));
                chair.setType(resultSet.getString("Type"));
                chair.setSimDrugsJSon(resultSet.getString("simDrugsJSon"));
                chair.setVerifiedDrugsJSon(resultSet.getString("VerifiedDrugsJSon"));
                chairs.add(chair);
                try {
                    JSONArray jsonArray = new JSONArray(chair.getSimDrugsJSon());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ChairInfo chairInfo = new ChairInfo();

                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String desc = jsonObject.getString("Desc");
                        String type = jsonObject.getString("type");
                        String barcode = jsonObject.getString("Barcode");
                        String patName = jsonObject.getString("patName");
                        chairInfo.setBarcode(barcode);
                        chairInfo.setType(type);
                        chairInfo.setDesc(desc);
                        chairInfo.setPatName(patName);
                        chairInfosInside.add(chairInfo);

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chairAdapter.update(chairInfosInside);
                    Log.v("ttt", chairInfosInside.toString());
                }
            });
            Log.v("ttt", chairs.toString());
            return chairs;
        } catch (SQLException e) {
            e.printStackTrace();
            Log.v("ttt", e.getMessage());
        }
        return null;
    }

    public Connection getConnection(String url) throws SQLException {

        return DriverManager.getConnection(url, "edx_dba", "edx");
    }
}
