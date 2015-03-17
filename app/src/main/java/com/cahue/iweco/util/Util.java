package com.cahue.iweco.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cahue.iweco.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class Util {

    /*
     * Shared preferences constants
     */
    public static final String PREF_DIALOG_SHOWN = "PREF_DIALOG_SHOWN";
    public static final String PREF_CAMERA_ZOOM = "PREF_CAMERA_ZOOM";
    public static final String PREF_CAMERA_LAT = "PREF_CAMERA_LAT";
    public static final String PREF_CAMERA_LONG = "PREF_CAMERA_LONG";

    public static final String TAPPED_PROVIDER = "Tapped";

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        Util.DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }



    /**
     * Method for printing our fancy custom Toast
     *
     * @param context
     * @param string  Content of the toast
     * @param length  One from Toast.LENGTH_LONG or Toast.LENGTH_SHORT
     */
    public static void createUpperToast(Context context, String string, int length) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.toast_custom, null);
        TextView text = (TextView) layout.findViewById(R.id.error_text);
        text.setText(string);

        Toast toast = new Toast(context.getApplicationContext());

        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, getActionBarSize(context));
        toast.setDuration(length);
        toast.setView(layout);
        toast.show();
    }

    public static int getActionBarSize(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int actionBarSize = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        return actionBarSize;
    }


    /**
     * It returns the only shared preferences file we will be using in the app.
     *
     * @param context
     * @return
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static float dpToPx(Context context, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }


    public static boolean isTutorialShown(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getBoolean(PREF_DIALOG_SHOWN, false);
    }

    public static void setTutorialShown(Context context, boolean shown) {
        SharedPreferences prefs = getSharedPreferences(context);
        prefs.edit().putBoolean(PREF_DIALOG_SHOWN, shown).apply();
    }

    public static void saveCameraPosition(Context context, CameraPosition cameraPosition) {
        SharedPreferences prefs = getSharedPreferences(context);
        prefs.edit()
                .putFloat(PREF_CAMERA_ZOOM, cameraPosition.zoom)
                .putInt(PREF_CAMERA_LAT, (int) (cameraPosition.target.latitude * 10E6))
                .putInt(PREF_CAMERA_LONG, (int) (cameraPosition.target.longitude * 10E6))
                .apply();
    }

    public static CameraUpdate getLastCameraPosition(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);

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


}
