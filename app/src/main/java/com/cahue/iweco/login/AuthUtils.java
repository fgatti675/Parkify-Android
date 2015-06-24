package com.cahue.iweco.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Francesco on 27/02/2015.
 */
public class AuthUtils {

    public static final String PREF_SKIPPED_LOGGED_IN = "PREF_SKIPPED_LOGGED_IN";
    public static final String PREF_USER_NAME = "PREF_USER_NAME";
    public static final String PREF_USER_EMAIL = "PREF_USER_EMAIL";
    public static final String PREF_USER_PICTURE_URL = "PREF_USER_PICTURE_URL";

    public static void setSkippedLogin(Context context, boolean skipped) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_SKIPPED_LOGGED_IN, skipped).apply();
    }

    public static boolean isSkippedLogin(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_SKIPPED_LOGGED_IN, false);
    }

    public static void clearLoggedUserDetails(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .remove(PREF_USER_NAME)
                .remove(PREF_USER_EMAIL)
                .remove(PREF_USER_PICTURE_URL)
                .apply();

    }

    public static void setLoggedUserDetails(Context context, String username, String email, String pictureURL){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putString(PREF_USER_NAME, username)
                .putString(PREF_USER_EMAIL, email)
                .putString(PREF_USER_PICTURE_URL, pictureURL)
                .apply();
    }

    public static String getLoggedUsername(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_USER_NAME, null);
    }

    public static String getEmail(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_USER_EMAIL, null);
    }

    public static String getProfilePicURL(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_USER_PICTURE_URL, null);
    }

}
