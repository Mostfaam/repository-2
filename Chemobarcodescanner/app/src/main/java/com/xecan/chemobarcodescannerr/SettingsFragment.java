package com.xecan.chemobarcodescannerr;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Patterns;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

/*
 * Created by femi on 4/9/18.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {
    private SharedPreferences preferences;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // Load the Preferences from the XML file
        addPreferencesFromResource(R.xml.settings);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // put the version information into the screen
        Preference p = getPreferenceScreen().findPreference(getString(R.string.pref_key_version));
        if(p != null){
            Activity a = getActivity();
            try {
                PackageInfo pInfo = a.getPackageManager().getPackageInfo(a.getPackageName(), 0);
                p.setSummary("Version "+ pInfo.versionName+" ("+pInfo.versionCode+")");
            }catch(Exception e){
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //unregister the preferenceChange listener
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        getPreferenceScreen().findPreference(getString(R.string.pref_key_database_ip)).setOnPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister the preference change listener
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }


    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getActivity().getString(R.string.pref_key_database_ip))) {
            String value = preferences.getString(key, "");

            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
            App.notifyInfo(lbm, String.format(getActivity().getString(R.string.message_testing_db_connection), value));
            LinkService.startTestDB(getActivity());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        if (preference.getKey().equals(getActivity().getString(R.string.pref_key_database_ip))) {
            // check that the new value properly parses as an IP
            if(!Patterns.IP_ADDRESS.matcher(String.valueOf(newValue)).matches())
            {
                LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
                App.notifyError(lbm, String.format(getActivity().getString(R.string.message_invalid_ip_address), String.valueOf(newValue)));
                LinkService.startTestDB(getActivity());
                return false;
            }
        }
        return true;
    }
}