package com.whereismycar.login;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.whereismycar.BuildConfig;

/**
 * Created by francesco on 19.01.2015.
 */
public class GCMUtil {

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String GCM_PREFS = "GCM_PREFS";
    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    static final String SENDER_ID = "582791978228";
    private static final String TAG = GCMUtil.class.getSimpleName();
    private static final String PROPERTY_APP_VERSION = "appVersion";

    /**
     * Gets the current registration ID for application on GCM service.
     * <p/>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    @Nullable
    public static String getRegistrationId(@NonNull Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = BuildConfig.VERSION_CODE;
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed."); // TODO: do something?
//            return "";
        }
        return registrationId;
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId   registration ID
     */
    public static void storeRegistrationId(@NonNull Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = BuildConfig.VERSION_CODE;
        Log.i(TAG, "Saving regId on app version " + appVersion);
        prefs.edit()
                .putString(PROPERTY_REG_ID, regId)
                .putInt(PROPERTY_APP_VERSION, appVersion)
                .apply();
    }


    /**
     * @return Application's {@code SharedPreferences}.
     */
    public static SharedPreferences getGCMPreferences(@NonNull Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(GCM_PREFS, Context.MODE_PRIVATE);
    }
}
