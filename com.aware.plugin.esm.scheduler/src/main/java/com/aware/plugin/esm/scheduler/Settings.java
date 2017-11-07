package com.aware.plugin.esm.scheduler;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.EditText;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_ESM_SCHEDULER = "status_plugin_esm_scheduler";
    public static final String CALENDAR_URL = "calendar_url";

    //Plugin settings UI elements
    private static CheckBoxPreference status;
    private static EditTextPreference calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_ESM_SCHEDULER);
        if( Aware.getSetting(this, STATUS_PLUGIN_ESM_SCHEDULER).length() == 0 ) {
            Aware.setSetting( this, STATUS_PLUGIN_ESM_SCHEDULER, true ); //by default, the setting is true on install
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_ESM_SCHEDULER).equals("true"));

        calendar = (EditTextPreference) findPreference(CALENDAR_URL);
        if (Aware.getSetting(this, CALENDAR_URL).length() > 0) {
            calendar.setText(Aware.getSetting(this, CALENDAR_URL));
            calendar.setSummary(Aware.getSetting(this, CALENDAR_URL));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = findPreference(key);
        if( setting.getKey().equals(STATUS_PLUGIN_ESM_SCHEDULER) ) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            status.setChecked(sharedPreferences.getBoolean(key, false));
        }
        if (setting.getKey().equals(CALENDAR_URL)) {
            Aware.setSetting(this, key, sharedPreferences.getString(key, ""));
            calendar.setText(sharedPreferences.getString(key, ""));
            calendar.setSummary(sharedPreferences.getString(key, ""));
        }
        if (Aware.getSetting(this, STATUS_PLUGIN_ESM_SCHEDULER).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.esm.scheduler");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.esm.scheduler");
        }
    }
}
