package com.bahpps.cahue.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Francesco on 31/01/2015.
 */
public class AuthUtils {

    public static final String PREF_USER_LOGGED_IN = "PREF_USER_LOGGED_IN";
    public static final String PREF_OAUTH_TOKEN = "PREF_OAUTH_TOKEN";
    public static final String PREF_REFRESH_TOKEN = "PREF_REFRESH_TOKEN";

    public static void saveOAuthToken(Context context, String token) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_OAUTH_TOKEN, token).apply();
    }

    public static String getOauthToken(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_OAUTH_TOKEN, null);
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_USER_LOGGED_IN, false);
    }

    public static void setIsLoggedIn(Context context, boolean loggedIn) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_USER_LOGGED_IN, loggedIn).apply();
    }

    public static void saveRefreshToken(Context context, String refreshToken) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_REFRESH_TOKEN, refreshToken).apply();
    }
}
