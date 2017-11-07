package com.aware.plugin.esm.scheduler;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.util.Calendar;

public class Plugin extends Aware_Plugin {

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::" + getResources().getString(R.string.app_name);

        //Add permissions you need (Android M+).
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_CALENDAR);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_ESM_SCHEDULER, true);

            Cursor calendars = getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                    new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.NAME},
                    CalendarContract.Calendars.NAME + " LIKE 'AWARE%'",
                    null, null);
            if (calendars != null && calendars.moveToFirst()) {
                Cursor allesms = getContentResolver().query(CalendarContract.Events.CONTENT_URI,
                        null,
                        CalendarContract.Events.CALENDAR_ID + "=" + calendars.getInt(calendars.getColumnIndex(CalendarContract.Calendars._ID)) + " AND " +
                                CalendarContract.Events.TITLE + " LIKE 'ESM%'",
                        null,
                        CalendarContract.Events.DTSTART + " ASC");
                if (allesms != null && allesms.moveToFirst()) {
                    do {

                        Calendar now = Calendar.getInstance();
                        Calendar endToday = Calendar.getInstance();
                        endToday.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 23, 59, 59);

                        Uri.Builder uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
                        ContentUris.appendId(uriBuilder, now.getTimeInMillis());
                        ContentUris.appendId(uriBuilder, endToday.getTimeInMillis());

                        String[] fields = new String[]{
                                CalendarContract.Instances._ID,
                                CalendarContract.Instances.TITLE,
                                CalendarContract.Instances.DESCRIPTION,
                                CalendarContract.Instances.BEGIN
                        };

                        Cursor instances = getContentResolver().query(uriBuilder.build(), fields, CalendarContract.Instances.EVENT_ID + "=" + allesms.getInt(allesms.getColumnIndex(CalendarContract.Events._ID)), null, null );
                        if (instances != null && instances.moveToFirst()) {
                            do {
                                try {
                                    Scheduler.Schedule esm = Scheduler.getSchedule(getApplicationContext(), instances.getString(instances.getColumnIndex(CalendarContract.Instances.TITLE)));
                                    if (esm == null) { //not set yet
                                        Calendar timer = Calendar.getInstance();
                                        timer.setTimeInMillis(instances.getLong(instances.getColumnIndex(CalendarContract.Instances.BEGIN)));

                                        esm = new Scheduler.Schedule(instances.getString(instances.getColumnIndex(CalendarContract.Instances.TITLE)));
                                        esm.setActionType(Scheduler.ACTION_TYPE_BROADCAST);
                                        esm.setActionIntentAction(ESM.ACTION_AWARE_QUEUE_ESM);
                                        esm.addActionExtra(ESM.EXTRA_ESM, cleanString(instances.getString(instances.getColumnIndex(CalendarContract.Instances.DESCRIPTION))));
                                        esm.setTimer(timer);

                                        Scheduler.saveSchedule(getApplication(), esm);
                                    } else { //check if time or the ESM has changed since the last time we scheduled it
                                        if (esm.getTimer() != instances.getLong(instances.getColumnIndex(CalendarContract.Instances.BEGIN))) {
                                            Calendar newTimer = Calendar.getInstance();
                                            newTimer.setTimeInMillis(instances.getLong(instances.getColumnIndex(CalendarContract.Instances.BEGIN)));
                                            esm.setTimer(newTimer);
                                        }
                                        if (!esm.getActionExtras().getString(0).equalsIgnoreCase(cleanString(instances.getString(instances.getColumnIndex(CalendarContract.Instances.DESCRIPTION))))) {
                                            esm.addActionExtra(ESM.EXTRA_ESM, cleanString(instances.getString(instances.getColumnIndex(CalendarContract.Instances.DESCRIPTION))));
                                        }
                                        Scheduler.saveSchedule(getApplicationContext(), esm);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } while (instances.moveToNext());
                        }
                        if (instances != null && ! instances.isClosed()) instances.close();

                    } while (allesms.moveToNext());
                }
                if (allesms != null && ! allesms.isClosed()) allesms.close();
            }
            if (calendars != null && !calendars.isClosed()) calendars.close();

            //Initialise AWARE instance in plugin
            Aware.startAWARE(this);
        }

        return START_STICKY;
    }

    private String cleanString(String badString) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(badString, Html.FROM_HTML_MODE_LEGACY).toString().replace("\"","\'");
        } else return Html.fromHtml(badString).toString().replace("\"","\'");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_ESM_SCHEDULER, false);

        //Stop AWARE instance in plugin
        Aware.stopAWARE(this);
    }
}
