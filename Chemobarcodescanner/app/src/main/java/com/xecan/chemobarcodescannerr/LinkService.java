package com.xecan.chemobarcodescannerr;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.app.Application;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.preference.PreferenceManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

import javax.sql.DataSource;

import java.util.Properties;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class LinkService extends IntentService
{
    private static String last;
    private static Long lastTime;

    private Properties props;

    private DataSource ds;
    private QueryRunner q;
    private ScalarHandler<Object> sh;
    private ArrayHandler ah;
    private ArrayListHandler alh;
    private MapHandler mh;
    private MapListHandler mlh;

    private String patientPrefix, nursePrefix, pcaPrefix;
    private String machines;

    public String versionInfo;
    public LinkService() {
        super("LinkService");
    }

    private static Pattern configPattern = Pattern.compile("X\\:(\\d)\\:(.*)");

    private String dburl;

    @Override
    public void onCreate()
    {
        super.onCreate();

        q = new QueryRunner();
        sh = new ScalarHandler<Object>();
        ah = new ArrayHandler();
        alh = new ArrayListHandler();
        mh = new MapHandler();
        mlh = new MapListHandler();

        props = new Properties();

        try {
            Class.forName("com.mysql.jdbc.Driver");
            props.load(getResources().openRawResource(R.raw.hibernate));
            patientPrefix = (String)props.get("hibernate.patient.prefix");
            nursePrefix = (String)props.get("hibernate.nurse.prefix");
            pcaPrefix = (String)props.get("hibernate.PCA.prefix");
            machines = (String)props.get("hibernate.machines");
        }catch(Exception e){
            App.notifyError(LocalBroadcastManager.getInstance(this), String.format(getString(R.string.message_processing_error), "JDBC Unavailable"));
        }

        try {
            Application a = getApplication();
            PackageInfo pInfo = a.getPackageManager().getPackageInfo(a.getPackageName(), 0);
            versionInfo = pInfo.versionName+"/"+pInfo.versionCode;
        }catch(Exception e){
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        dburl = "jdbc:mysql://"+prefs.getString(getString(R.string.pref_key_database_ip), "192.168.1.166")+":3306/edx0?autoReconnect=true&useSSL=false";
    }

    public Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(dburl,"edx_dba","edx");
    }

    public String getPropertyFromFile( String theKey, String theDefault)
    {
        String retVal = props.getProperty(theKey);
        if(retVal == null) return theDefault;
        return retVal;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null){
            if(intent.getIntExtra(Intent.EXTRA_INTENT, 0) == App.MSG_CHECK_DATABASE){
                checkDatabase(intent);
                return;
            }

            if(intent.getIntExtra(Intent.EXTRA_INTENT, 0) == App.MSG_PROCESS_BARCODE) {
                processBarcode(intent);
                return;
            }
        }
    }

    String getExamName(Connection conn, String readerSN) throws Exception
    {
        return String.valueOf(q.query(conn, "SELECT examName FROM examrooms WHERE id = ?", sh, readerSN));
    }

    String getRFIDFromScheduler(Connection conn, String tagID) throws Exception
    {
        Object RFID = q.query(conn, "SELECT RFID from apptscheduler where patID1 = RFID and date(apptDate) = current_date order by RFID", sh);
        if(RFID == null || String.valueOf(RFID).trim().length()==0) {
            String rfid = String.valueOf(getRFIDWriteSEQ(conn));
            return getSixDigitID(rfid);
        }
        return String.valueOf(RFID).trim();
    }

    int getRFIDWriteSEQ(Connection conn) throws Exception
    {
        Integer transNo = (Integer)q.query(conn, "select logical_id from logical_id_seq order by logical_id desc", sh);
        if(transNo != null){
            q.update(conn, "INSERT INTO logical_id_seq VALUES()");
        }
        return transNo;
    }

    private static String getSixDigitID(String inNum)
    {
        if(inNum.length() == 5)
            return "B" + inNum;
        else if (inNum.length() == 4)
            return "BB" + inNum;
        else if (inNum.length() == 3)
            return "BBB" + inNum;
        else if (inNum.length() == 2)
            return "BBBB" + inNum;
        else if (inNum.length() >= 6){
            inNum= "B"+inNum.substring(inNum.length()-5, inNum.length());
            //   inNum= "B"+inNum.substring(inNum.length()-4, inNum.length()) + inNum.substring(5,6);
        }
        return inNum;
    }

    String updateSchedulerWithPatID(Connection conn, String RFID, String patID) throws Exception
    {
        q.update(conn, "Update apptscheduler set PatID1 = ?, firstName= ?, lastName= ?, apptLocation= ?, EMRID= ?, Doc= ?, description= ? where RFID= ?", patID, "F.N. "+patID, "L.N. "+patID, "ApptLoc "+patID, patID, "ApptDoc "+patID, "apptDesc "+patID, RFID);
        return "FirstName " + patID;
    }

    String getPatRFIDByPatID(Connection conn, String patID) throws Exception
    {
        return String.valueOf(q.query(conn, "select RFID from apptscheduler where patID1= ? and date(apptDate) = current_date", sh, patID));
    }

    String buildTagID(String prefix, String suffix)
    {
        int i = 24 - patientPrefix.length() - suffix.length();
        StringBuilder sb = new StringBuilder(26);
        // 2019/05/23 Requested by byang: remove 0x
        //sb.append("0x").append(prefix);
        sb.append(prefix);
        while(i > 0){
            sb.append("0");
            --i;
        }
        sb.append(suffix);
        return sb.toString();
    }

    void playSuccessTone(ToneGenerator toneG)
    {
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    void playErrorTone(ToneGenerator toneG)
    {
        toneG.startTone(ToneGenerator.TONE_SUP_PIP, 200);
    }

    void checkDatabase(Intent intent)
    {
        // do a simple thing
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String ip = prefs.getString(getString(R.string.pref_key_database_ip), "192.168.1.166");
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        try {
            Object valid = q.query(getConnection(), "SELECT 1", sh);
            if(valid != null){
                App.notifySuccess(lbm, String.format(getString(R.string.message_db_connection_test_success), ip));
                return;
            }
            App.notifyError(lbm, String.format(getString(R.string.message_db_connection_test_failed), ip));
        }catch(Exception e){
            App.notifyError(lbm, String.format(getString(R.string.message_db_connection_test_failed), ip));
            android.util.Log.v(App.TAG, "Exception checking database connection:"+e.getMessage(), e);
        }
    }

    void processBarcode(Intent intent)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String query = intent.getStringExtra(Intent.EXTRA_TEXT);

        ToneGenerator toneG = null;

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

        boolean silence = false;
        if(query.equals(last) && System.currentTimeMillis() - lastTime < 10000) {
            silence = true;
        }else {
            toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            lastTime = System.currentTimeMillis();
        }
        last = query;

        // using okhttp
        try {
            if("444444".equals(query)) {
                // send the logout
                if(!silence) {
                    if(toneG != null) playSuccessTone(toneG);
                    try{ Thread.sleep(300); }catch(Exception e){}
                    if(toneG != null) playSuccessTone(toneG);
                }

                // 2019/05/23 Requested by byang: remove 0x
                //String RFID = "0x000000000209000000444444";
                String RFID = "000000000209000000444444";
                OkHttpClient client = new OkHttpClient();

                // and run the send
                String posturl = prefs.getString("PostURL", null);
                if(posturl != null) {
                    HttpUrl url = HttpUrl.parse(posturl).newBuilder().addQueryParameter("inputData", RFID).addQueryParameter("v", versionInfo).addQueryParameter("type", "barcode").build();
                    android.util.Log.v(App.TAG, "Using RFID: ["+RFID+"] with url ["+url.toString()+"]");

                    Request req = new Request.Builder().url(url).build();
                    Response res = client.newCall(req).execute();
                    try {
                        if (res.isSuccessful()) {
                            //String resp = res.body().string();
                            App.notifySuccess(lbm, "Logged out");
                        } else {
                            // error: should show it in the main screen
                            App.notifyError(lbm, String.format(getString(R.string.message_processing_error), res.message()));
                        }
                    } finally {
                        try {
                            res.body().close();
                        } catch (Exception e1) {
                        }
                    }
                }
            }else if(query.startsWith("N")){
                if(!silence) {
                    if(toneG != null) playSuccessTone(toneG);
                    try{ Thread.sleep(300); }catch(Exception e){}
                    if(toneG != null) playSuccessTone(toneG);
                }

                String RFID = query.substring(1);
                RFID = buildTagID(nursePrefix, RFID);
                OkHttpClient client = new OkHttpClient();

                // and run the send
                String posturl = prefs.getString("PostURL", null);
                if(posturl != null) {
                    HttpUrl url = HttpUrl.parse(posturl).newBuilder().addQueryParameter("inputData", RFID).addQueryParameter("type", "barcode").addQueryParameter("v", versionInfo).build();
                    android.util.Log.v(App.TAG, "Using RFID: ["+RFID+"] with url ["+url.toString()+"]");

                    Request req = new Request.Builder().url(url).build();
                    Response res = client.newCall(req).execute();
                    try {
                        if (res.isSuccessful()) {
                            //String resp = res.body().string();
                            App.notifySuccess(lbm, "Nurse logged in.");
                        } else {
                            // error: should show it in the main screen
                            App.notifyError(lbm, String.format(getString(R.string.message_processing_error), res.message()));
                        }
                    } finally {
                        try {
                            res.body().close();
                        } catch (Exception e1) {
                        }
                    }
                }
            }else if(query.startsWith("PC")){
                if(!silence) {
                    if(toneG != null) playSuccessTone(toneG);
                    try{ Thread.sleep(300); }catch(Exception e){}
                    if(toneG != null) playSuccessTone(toneG);
                }

                String RFID = query.substring(3);
                RFID = buildTagID(pcaPrefix, RFID);
                OkHttpClient client = new OkHttpClient();

                // and run the send
                String posturl = prefs.getString("PostURL", null);
                if(posturl != null) {
                    HttpUrl url = HttpUrl.parse(posturl).newBuilder().addQueryParameter("type", "barcode").addQueryParameter("inputData", RFID).addQueryParameter("v", versionInfo).build();
                    android.util.Log.v(App.TAG, "Using RFID: ["+RFID+"] with url ["+url.toString()+"]");

                    Request req = new Request.Builder().url(url).build();
                    Response res = client.newCall(req).execute();
                    try {
                        if (res.isSuccessful()) {
                            //String resp = res.body().string();
                            App.notifySuccess(lbm, "Nurse logged in.");
                        } else {
                            // error: should show it in the main screen
                            App.notifyError(lbm, String.format(getString(R.string.message_processing_error), res.message()));
                        }
                    } finally {
                        try {
                            res.body().close();
                        } catch (Exception e1) {
                        }
                    }
                }
            }else if (query.startsWith("AC")) {
                if(!silence) {
                    if(toneG != null) playSuccessTone(toneG);
                    try{ Thread.sleep(300); }catch(Exception e){}
                    if(toneG != null) playSuccessTone(toneG);
                }

                Connection conn = getConnection();
                String RFID = getPatRFIDFromRawScanID(conn, query);
                RFID = buildTagID(patientPrefix, RFID);

                android.util.Log.v(App.TAG, "Using RFID bin: ["+RFID+"]");

                OkHttpClient client = new OkHttpClient();

                // and run the send
                String posturl = prefs.getString("PostURL", null);
                android.util.Log.v(App.TAG, "Using posturl: ["+posturl+"]");
                String readerSN = prefs.getString("readerSN", null);
                android.util.Log.v(App.TAG, "Using readerSN: ["+readerSN+"]");
                String acctNum = prefs.getString("acctNum", null);
                android.util.Log.v(App.TAG, "Using acctNum: ["+acctNum+"]");
                // http://192.168.1.100:12001/readerAgent/report?type=barcode&id=123456&readerSN=0000001001&acctNum=1002800003
                /**Map vars = prefs.getAll();
                 Iterator it = vars.keySet().iterator();
                 while(it.hasNext()) {
                 android.util.Log.v(App.TAG, " prefs keys: [" + it.next() + " - " + prefs.getString((String)it.next(), null)+ "]");
                 }*/
                if(posturl != null) {
                    HttpUrl url = HttpUrl.parse(posturl).newBuilder().addQueryParameter("type", "barcode").addQueryParameter("id", RFID).addQueryParameter("readerSN", readerSN).addQueryParameter("acctNum", acctNum).addQueryParameter("v", versionInfo).build();
                    android.util.Log.v(App.TAG, " http req url="+url.toString());
                    //android.util.Log.v(App.TAG, "Using RFID: ["+RFID+"] with url ["+url.toString()+"]");

                    Request req = new Request.Builder().url(url).build();
                    android.util.Log.v(App.TAG, " http request req="+req.toString());
                    Response res = client.newCall(req).execute();
                    try {
                        if (res.isSuccessful()) {
                            //String resp = res.body().string();
                            App.notifySuccess(lbm, "Patient logged in.");
                        } else {
                            // error: should show it in the main screen
                            App.notifyError(lbm, String.format(getString(R.string.message_processing_error), res.message()));
                        }
                    } finally {
                        try {
                            res.body().close();
                        } catch (Exception e1) {
                        }
                    }
                }
                /*
                tagID = getPatRFIDByPatID(conn, patID);
                RFID = buildTagID(patientPrefix, tagID);

                String rfidTagID = RFID.substring(RFID.length()-6, RFID.length());
                */
                String rfidTagID = RFID;
                /*
                String[] pinfo = getPatInfoFromAppScheduler(conn, rfidTagID);
                if (pinfo == null) {
                    pinfo = getPatInfoFromTrackingRecords(conn, rfidTagID);
                }
                if (pinfo != null) {
                    // firstname, lastname, patient ID, attending doc, notes, appt date
                    String patientName = pinfo[1] + ", " + pinfo[0].substring(0, 1);
                    Patient p = new Patient();
                    p.setName(patientName);
                    p.setAttDoctorName(pinfo[3]);
                    p.setRFID(rfidTagID);
                    p.setAppTime(pinfo[5]);
                    p.setFirstInTime(null);
                    p.setLastRecordTime(null);
                    p.setID(pinfo[2]);

                    // new patient
                    Date now = new Date();
                    p.setFirstInTime(now);
                    p.setFirstScanTime(now);
                    p.setColor(RFIDConsts.StaffInStatusColor);
                    p.setStatus(RFIDConsts.patientInStatus);
                    p.setWaitTime(0);
                    updateStatus(conn, rfidTagID, readerSN, acctNum, "In", "0");
                    updateChairsWBPatient(conn, p, pinfo[4], readerSN);
                }
                //String s = String.valueOf(q.query(conn, "SELECT COUNT(*) FROM readerinfo", sh));
                App.notifySuccess(lbm, "Whiteboard updated.");
                */
            } else if (query.startsWith("X:")) {
                // config string: put it in place
                Matcher m = configPattern.matcher(query);
                if (m.matches()) {
                    if(!silence) {
                        if(toneG != null) playSuccessTone(toneG);
                    }

                    int pos = Integer.parseInt(m.group(1));
                    if(pos == 0){
                        Object[] deviceInfo = getDeviceInfo(getConnection(), m.group(2));
                        if(deviceInfo != null && deviceInfo.length == 4) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("PostURL", String.valueOf(deviceInfo[0]));
                            editor.putString("ChairName", String.valueOf(deviceInfo[1]));
                            editor.putString("readerSN", String.valueOf(deviceInfo[2]));
                            editor.putString("acctNum", String.valueOf(deviceInfo[3]));
                            android.util.Log.v(App.TAG, "Configured Post URL: " + deviceInfo[0] +" chairName "+deviceInfo[1] +" readerSN=" +deviceInfo[2] + " acctNum=" +deviceInfo[3] );
                            // merged!
                            App.notifySuccess(lbm, "Completed configuration.");//Config: " + configdata.toString());
                            App.notifyCommand(lbm, App.MSG_CONFIGURED);
                            if (!silence) {
                                if(toneG != null) playSuccessTone(toneG);
                                try {
                                    Thread.sleep(300);
                                } catch (Exception e) {
                                }
                                if(toneG != null) playSuccessTone(toneG);
                                try {
                                    Thread.sleep(300);
                                } catch (Exception e) {
                                }
                                if(toneG != null) playSuccessTone(toneG);
                            }
                            editor.apply();
                        }else{
                            App.notifyError(lbm, String.format(getString(R.string.message_scanner_id_not_found), m.group(2), prefs.getString(getString(R.string.pref_key_database_ip), "192.168.1.166")));
                            if(toneG != null) playErrorTone(toneG);
                        }
                    }
                }
            }else if("063435890714".equals(query)){
                OkHttpClient client = new OkHttpClient();

                // and run the send
                String posturl = prefs.getString("PostURL", null);
                if(posturl != null) {
                    HttpUrl url = HttpUrl.parse(posturl).newBuilder().addQueryParameter("tagId", query).addQueryParameter("type", "barcode").addQueryParameter("v", versionInfo).build();
                    Request req = new Request.Builder().url(url).build();
                    Response res = client.newCall(req).execute();
                    try {
                        if (res.isSuccessful()) {
                            String resp = res.body().string();
                            /*
                            // details
                            //{"success":true,"id":"2a1c6244-7b5f-4a3d-a64b-b2d92edfe4ff","key":"KJpmtMT3hsTAgTgk2skRpgguWhfjNqMn9TL4n5EUAOYBVDcC3j3P5uCCgnr4r5ad"}
                            if (details.has("success")) {
                                // extract and save the id and key
                                SharedPreferences.Editor edit = prefs.edit();
                                edit.putString(getString(R.string.pref_server_key), details.getString("token"));
                                edit.putString(getString(R.string.pref_patient_name), details.getString("name"));
                                edit.putString(getString(R.string.pref_patient_firstname), details.getString("firstname"));
                                edit.commit();

                                // notify the user that we're now connection
                                App.notifySuccess(lbm, String.format(getString(R.string.message_connected_successfully), details.getString("name")));
                            } else {
                                // error: should show it in the main screen
                                App.notifyError(lbm, String.format(getString(R.string.message_connection_error), details.getString("error")));
                            }
                            */
                        } else {
                            // error: should show it in the main screen
                            App.notifyError(lbm, String.format(getString(R.string.message_processing_error), res.message()));
                        }
                    } finally {
                        try {
                            res.body().close();
                        } catch (Exception e1) {
                        }
                    }
                }
            }else{
                App.notifyInfo(lbm, "Please repeat the last scan: the received barcode ("+query+") could not be recognized.");
                android.util.Log.v(App.TAG, "Unrecognized barcode ("+query+")");
                if(!silence) {
                    if(toneG != null) playErrorTone(toneG);
                }

            }
        /*
            OkHttpClient client = new OkHttpClient();

            // and run the send
            String url = BuildConfig.SERVER_URL + "link.form?link="+link;
            Request req = new Request.Builder().url(url).get().build();
            Response res = client.newCall(req).execute();
            try {
                if (res.isSuccessful()) {
                    String resp = res.body().string();
                    JSONObject details = (JSONObject) new JSONTokener(resp).nextValue();

                    // details
                    //{"success":true,"id":"2a1c6244-7b5f-4a3d-a64b-b2d92edfe4ff","key":"KJpmtMT3hsTAgTgk2skRpgguWhfjNqMn9TL4n5EUAOYBVDcC3j3P5uCCgnr4r5ad"}
                    if (details.has("success")) {
                        // extract and save the id and key
                        SharedPreferences.Editor edit = prefs.edit();
                        edit.putString(getString(R.string.pref_server_key), details.getString("token"));
                        edit.putString(getString(R.string.pref_patient_name), details.getString("name"));
                        edit.putString(getString(R.string.pref_patient_firstname), details.getString("firstname"));
                        edit.commit();

                        // notify the user that we're now connection
                        App.notifySuccess(lbm, String.format(getString(R.string.message_connected_successfully), details.getString("name")));
                    }else {
                        // error: should show it in the main screen
                        App.notifyError(lbm, String.format(getString(R.string.message_connection_error), details.getString("error")));
                    }
                } else {
                    // error: should show it in the main screen
                    App.notifyError(lbm, String.format(getString(R.string.message_connection_error), res.message()));
                }
            } finally {
                try {
                    res.body().close();
                } catch (Exception e1) {
                }
            }
            */
        } catch (Exception e) {
            android.util.Log.v(App.TAG, "Exception processing barcode: " + e.getMessage(), e);
            App.notifyError(lbm, String.format(getString(R.string.message_processing_error), e.getMessage()));
        } finally {
            if(toneG != null) {
                try{
                    toneG.release();
                }catch(Exception e2){

                }
            }
        }
        //App.notifyError(lbm, String.format(getString(R.string.message_processing_error), "Unimplemented"));
    }

    public static void startProcessBarcode(Context context, String query)
    {
        Intent intent = new Intent(context, LinkService.class);
        intent.putExtra(Intent.EXTRA_INTENT, App.MSG_PROCESS_BARCODE);
        intent.putExtra(Intent.EXTRA_TEXT, query);
        context.startService(intent);
    }

    public static void startTestDB(Context context)
    {
        Intent intent = new Intent(context, LinkService.class);
        intent.putExtra(Intent.EXTRA_INTENT, App.MSG_CHECK_DATABASE);
        context.startService(intent);
    }


    String[] getPatInfoFromAppScheduler(Connection conn, String RFID) throws Exception
    {
        List<Object[]> entries = q.query(conn, "select firstName, lastName, patID1, Doc, description, apptDate, apptLocation from apptscheduler where RFID = ? and date(apptDate) = current_date", alh, RFID);
        if (entries != null && !entries.isEmpty()) {
            SimpleDateFormat dateFormat1 = new SimpleDateFormat("HH:mm MMM,dd");
            String val;
            for (Object[] entry : entries) {
                if(true){ //if (machines.contains(String.valueOf(entry[6]))) {
                    String[] result = new String[6];
                    result[0] = String.valueOf(entry[0]); // firstname, lastname, patient ID, attending doc, notes, appt date
                    result[1] = String.valueOf(entry[1]); //
                    result[2] = String.valueOf(entry[2]);
                    result[3] = "...";
                    if(entry[3] != null) {
                        val = String.valueOf(entry[3]).trim();
                        if (val != "") result[3] = val;
                    }
                    result[4] = "...";
                    if(entry[4] != null){
                        val = String.valueOf(entry[4]).trim();
                        if (val != "") result[4] = val;
                    }
                    Date dt = (Date)entry[5];
                    try {
                        result[5] = dateFormat1.format(dt);
                    } catch (Exception e) {
                    }
                    return result;
                }
            }
        }
        return null;
    }

    String[] getPatInfoFromTrackingRecords(Connection conn, String RFID) throws Exception
    {
        Object[] entry = q.query(conn, "select substring_index(itemDescription,',',-1) as firstName, substring_index(itemDescription,',',1) as lastName, patID1 as pid, 'doc' as Doc, 'desc' as description, antennaID as apptDate, lotnumber as apptLocation, '0' as isOTVToday  "+
                "from productUrn, productStatus ps where ps.RFID= ? and ps.URNEPCNO = producturn.URNEPCNO "+
                "and ps.DATETIME > current_date order by ps.DATETIME desc ", ah, RFID);
        if(entry != null && entry.length > 0){
            SimpleDateFormat dateFormat1 = new SimpleDateFormat("HH:mm");
            String[] result = new String[6];
            result[0] = String.valueOf(entry[0]);
            result[1] = String.valueOf(entry[1]);
            result[2] = String.valueOf(entry[2]);
            result[3] = "...";
            result[4] = "...";
            result[5] = String.valueOf(entry[5]);
            String val = String.valueOf(entry[6]);
            boolean isWhitespace = val.matches("^\\s*$");
            if(isWhitespace){
                result[3] = val.substring(0, val.indexOf(" "));
                result[4] = val.substring(val.indexOf(" ")+1, val.length());
            }else{
                result[4] = val.trim();
            }
            return result;
        }
        return null;
    }

    Object[] getDeviceInfo(Connection conn, String scannerID) throws Exception
    {
        // return (Object[])q.query(conn, "SELECT postURL, ChairName FROM chairs WHERE scannerID = ?", ah, scannerID);
        return (Object[])q.query(conn, "SELECT postURL, ChairName, ID, acctNum FROM chairs WHERE scannerID = ?", ah, scannerID);
    }

    boolean updateExamroomWBPatient(Connection conn, Patient patient, String notes, String readerSN) throws Exception
    {
        q.update(conn, "update examrooms set PatRFID= ?, PatName=?, PatDuration=?, PatStatus=?, PatColor=?, "+
                "ApptNotes= ?, ApptTime=?, attDoc=? where id = ?", patient.getRFID(), patient.getName(), patient.getWaitTime(), patient.getStatus(), patient.getColor(), notes, patient.getAppTime(), patient.getAttDoctorName(), readerSN);
        return true;
    }

    boolean updateChairsWBPatient(Connection conn, Patient patient, String notes, String readerSN) throws Exception
    {
        int cnt = q.update(conn, "update chairs set PatRFID= ?, PatName=?, PatDuration=?, PatStatus=?, PatColor=?, "+
                "ApptNotes= ?, ApptTime=?, attDoc=? where id = ?", patient.getRFID(), patient.getName(), patient.getWaitTime(), patient.getStatus(), patient.getColor(), notes, patient.getAppTime(), patient.getAttDoctorName(), readerSN);
        return true;
    }

    void updateStatus(Connection conn, String RFID, String readerSN, String acctNum, String status, String patWaitTime) throws Exception
    {
        q.update(conn, "update PRODUCTSTATUS ST, PRODUCTEpcTags PT set ST.TagStatus= ?, PT.TagCommand= ?, PT.Duration=?,  ST.Duration=? "  +
                "where PT.readerEpcNo=? and ST.readerEpcNo=PT.readerEpcNo and PT.urnEpcNo=ST.urnEpcNo and "+
                "PT.RFID=? and ST.TagStatus not like 'C' AND ST.DATETIME > CURRENT_DATE AND ST.DATETIME < CURRENT_DATE + INTERVAL 1 DAY", status, status, patWaitTime, patWaitTime, readerSN, RFID);
    }

    String getPatRFIDFromRawScanID(Connection conn, String rawID) throws Exception
    {
        String RFID = getPatIDApptTimeTodayFromAppScheduler(conn, rawID);
        if(RFID == null || RFID.trim().length()==0) {
            RFID = getRFIDWriteSEQ(conn) + "";
            RFID = getSixDigitID(RFID);

            String cell = "cell"+rawID;
            String firstName= "First_name" + rawID;
            String lastName = rawID;
            String patientId = rawID;
            String location = "location" + rawID;
            String IDA = "IDA" + rawID;
            String notes = "notes" + rawID;
            String DrIni="DrIni"+rawID;
            String startTime="TBD";
            String status="0";
            q.update(conn,"INSERT INTO apptscheduler (PatID1, RFID, firstName, lastName, apptDate, apptLocation, phone, EMRID, Doc, description, isOTVToday, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    rawID, RFID, firstName, lastName, new java.sql.Timestamp(new Date().getTime()), location, cell, IDA, DrIni, notes, "0", status);
        }
        return RFID;
    }

    String getPatIDApptTimeTodayFromAppScheduler(Connection conn, String patID1) throws Exception
    {
        return (String) q.query(conn, "SELECT RFID FROM apptscheduler WHERE date(apptDate) = current_date AND patId1 = ?", sh, patID1);
    }
}
