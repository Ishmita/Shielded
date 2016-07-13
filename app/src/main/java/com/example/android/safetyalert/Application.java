package com.example.android.safetyalert;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Ishmita on 11-07-2016.
 */
public class Application extends android.app.Application {

    public static final String MY_PREFS_NAME = "MyPrefsFile";
    String id;
    private static final String TAG = "Application";
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String restoredText = prefs.getString("id", null);
        if (restoredText != null) {
            id = prefs.getString("id", "No id");//"No id" is the default value.
            Log.d(TAG, "id saved in shared prefs: " + id);
        }

    }
}
