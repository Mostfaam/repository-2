package com.xecan.chemobarcodescannerr;

import android.os.AsyncTask;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class MyTask extends AsyncTask<Void, Void, Void> {

    private String idFromServer;
    private String url="jdbc:mysql://192.168.1.108:3306/test";
    String name = "AAA";

    @Override
    protected Void doInBackground(Void... params) {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            ResultSet result = null;
            String a = "INSERT INTO user (name) VALUES('yosef')";
            Connection con = DriverManager.getConnection(url,"root","");
            Statement state = con.createStatement();
            int res = state.executeUpdate(a);
            Log.v("ttt",res+"res");
        } catch (IllegalAccessException e) {
            Log.v("ttt",e.getMessage());
        } catch (InstantiationException e) {
            e.printStackTrace();
            Log.v("ttt",e.getMessage());

        } catch (SQLException e) {
            Log.v("ttt",e.getMessage());

            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Log.v("ttt",e.getMessage());

            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute(Void result) {
        Log.v("ttt","excuted");
        super.onPostExecute(result);
    }

}
