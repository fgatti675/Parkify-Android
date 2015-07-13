package com.cahue.iweco.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by Francesco on 04/06/2015.
 */
public class PreferencesUtil {

    /*
     * Shared preferences constants
     */
    public static final String PREF_TUTORIAL_DIALOG_SHOWN = "PREF_DIALOG_SHOWN";
    public static final String PREF_FACEBOOK_INVITES_DIALOG_SHOWN = "PREF_FACEBOOK_INVITES_DIALOG_SHOWN";
    public static final String PREF_UNINSTALL_WIMC_SHOWN = "PREF_UNINSTALL_WIMC_SHOWN";
    public static final String PREF_IWECO_PROMO_DATE = "PREF_IWECO_PROMO_DATE";
    public static final String PREF_CAMERA_ZOOM = "PREF_CAMERA_ZOOM";
    public static final String PREF_CAMERA_LAT = "PREF_CAMERA_LAT";
    public static final String PREF_CAMERA_LONG = "PREF_CAMERA_LONG";


    public static final String PREF_USE_MILES = "PREF_USE_MILES";
    public static final String PREF_MOVEMENT_RECOGNITION = "PREF_MOVEMENT_RECOGNITION";
    public static final String PREF_BT_ON_ENTER_VEHICLE = "PREF_BT_ON_ENTER_VEHICLE";
    public static final String PREF_BT_OFF_LEAVE_VEHICLE = "PREF_BT_OFF_LEAVE_VEHICLE";

    public static boolean isTutorialShown(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_TUTORIAL_DIALOG_SHOWN, false);
    }

    public static void setTutorialShown(Context context, boolean shown) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_TUTORIAL_DIALOG_SHOWN, shown).apply();
    }

    public static boolean isFacebookInvitesShown(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_FACEBOOK_INVITES_DIALOG_SHOWN, false);
    }

    public static void setFacebookInvitesShown(Context context, boolean shown) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_FACEBOOK_INVITES_DIALOG_SHOWN, shown).apply();
    }


    public static void saveCameraPosition(Context context, CameraPosition cameraPosition) {
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
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(latLng)
                .zoom(prefs.getFloat(PREF_CAMERA_ZOOM, 12))
                .build());

        return update;
    }

    public static boolean isWIMCUninstallDialogShown(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_UNINSTALL_WIMC_SHOWN, false);
    }

    public static void setWIMCUninstallDialogShown(Context context, boolean shown) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_UNINSTALL_WIMC_SHOWN, shown).apply();
    }

    public static void clear(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .remove(PREF_IWECO_PROMO_DATE)
                .remove(PREF_FACEBOOK_INVITES_DIALOG_SHOWN)
                .remove(PREF_UNINSTALL_WIMC_SHOWN)
                .remove(PREF_TUTORIAL_DIALOG_SHOWN)
                .apply();
    }

    public static void setIwecoPromoDialogShown(Context context, Date date) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(PREF_IWECO_PROMO_DATE, date.getTime()).apply();
    }

    public static Date getIwecoPromoDialogShown(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(!prefs.contains(PREF_IWECO_PROMO_DATE))
            return  null;
        return new Date(prefs.getLong(PREF_IWECO_PROMO_DATE, 0));
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
        return prefs.getBoolean(PREF_MOVEMENT_RECOGNITION, false);
    }
    public static boolean isBtOnEnteringVehicleEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_BT_ON_ENTER_VEHICLE, false);
    }
    public static boolean isBtOffLeavingVehicleEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_BT_OFF_LEAVE_VEHICLE, false);
    }
}
