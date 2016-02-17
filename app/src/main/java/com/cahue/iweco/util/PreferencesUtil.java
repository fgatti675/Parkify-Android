package com.cahue.iweco.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Francesco on 04/06/2015.
 */
public class PreferencesUtil {

    /*
     * Shared preferences constants
     */
    public static final String PREF_LONG_CLICK_SHOWN = "PREF_LONG_CLICK_SHOWN";
    public static final String PREF_RATED_DIALOG_ACCEPTED = "PREF_RATED_DIALOG_ACCEPTED";
    public static final String PREF_RATED_DIALOG_SHOWN_DATE = "PREF_RATED_DIALOG_SHOWN_DATE";
    public static final String PREF_RATED_DIALOG_NEXT_INTERVAL_DAYS = "PREF_RATED_DIALOG_NEXT_INTERVAL_DAYS";
    public static final String PREF_TUTORIAL_SHOWN = "PREF_DIALOG_SHOWN";
    public static final String PREF_FACEBOOK_INVITES_DIALOG_SHOWN = "PREF_FACEBOOK_INVITES_DIALOG_SHOWN";
    public static final String PREF_UNINSTALL_WIMC_SHOWN = "PREF_UNINSTALL_WIMC_SHOWN";
    public static final String PREF_IWECO_PROMO_DATE = "PREF_IWECO_PROMO_DATE";
    public static final String PREF_CAMERA_ZOOM = "PREF_CAMERA_ZOOM";
    public static final String PREF_CAMERA_LAT = "PREF_CAMERA_LAT";
    public static final String PREF_CAMERA_LONG = "PREF_CAMERA_LONG";

    public static final String PREF_USE_MILES = "PREF_USE_MILES";
    public static final String PREF_MOVEMENT_RECOGNITION = "PREF_MOVEMENT_RECOGNITION";
    public static final String PREF_MOVEMENT_RECOGNITION_NOTIFICATION = "PREF_MOVEMENT_RECOGNITION_NOTIFICATION";
    public static final String PREF_BT_ON_ENTER_VEHICLE = "PREF_BT_ON_ENTER_VEHICLE";

    public static final String PREF_REMOVE_ADS = "PREF_REMOVE_ADS";



    public static boolean isLongClickToastShown(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_LONG_CLICK_SHOWN, true);
    }

    public static void setLongClickToastShown(Context context, boolean shown) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_LONG_CLICK_SHOWN, shown).apply();
    }

    public static boolean isTutorialShown(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_TUTORIAL_SHOWN, false);
    }

    public static void setTutorialShown(Context context, boolean shown) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_TUTORIAL_SHOWN, shown).apply();
    }

    public static boolean isFacebookInvitesShown(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_FACEBOOK_INVITES_DIALOG_SHOWN, false);
    }

    public static void setFacebookInvitesShown(Context context, boolean shown) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_FACEBOOK_INVITES_DIALOG_SHOWN, shown).apply();
    }


    public static void saveCameraPosition(Context context, @NonNull CameraPosition cameraPosition) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putFloat(PREF_CAMERA_ZOOM, cameraPosition.zoom)
                .putInt(PREF_CAMERA_LAT, (int) (cameraPosition.target.latitude * 10E6))
                .putInt(PREF_CAMERA_LONG, (int) (cameraPosition.target.longitude * 10E6))
                .apply();
    }

    public static CameraUpdate getLastCameraPosition(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if(!prefs.contains(PREF_CAMERA_LAT) || !prefs.contains(PREF_CAMERA_LONG))
            return null;

        LatLng latLng = new LatLng(
                (double) prefs.getInt(PREF_CAMERA_LAT, 0) / 10E6,
                (double) prefs.getInt(PREF_CAMERA_LONG, 0) / 10E6);

        return CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(latLng)
                .zoom(prefs.getFloat(PREF_CAMERA_ZOOM, 12))
                .build());
    }

    public static boolean isWIMCUninstallDialogShown(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_UNINSTALL_WIMC_SHOWN, false);
    }

    public static void setWIMCUninstallDialogShown(Context context, boolean shown) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_UNINSTALL_WIMC_SHOWN, shown).apply();
    }

    public static boolean isAdsRemoved(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_REMOVE_ADS, false);
    }

    public static void setAdsRemoved(Context context, boolean removed) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_REMOVE_ADS, removed).apply();
    }

    public static void clear(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .remove(PREF_IWECO_PROMO_DATE)
                .remove(PREF_FACEBOOK_INVITES_DIALOG_SHOWN)
                .remove(PREF_UNINSTALL_WIMC_SHOWN)
                .remove(PREF_TUTORIAL_SHOWN)
                .remove(PREF_LONG_CLICK_SHOWN)
                .apply();
    }



    public static boolean isUseMiles(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_USE_MILES, false);
    }

    public static void setUseMiles(Context context, boolean use) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_USE_MILES, use).apply();
    }

    public static boolean isMovementRecognitionEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_MOVEMENT_RECOGNITION, true);
    }

    public static boolean isMovementRecognitionNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_MOVEMENT_RECOGNITION_NOTIFICATION, true);
    }

    public static boolean isBtOnEnteringVehicleEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_BT_ON_ENTER_VEHICLE, false);
    }
}
