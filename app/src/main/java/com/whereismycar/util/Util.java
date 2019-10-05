package com.whereismycar.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import androidx.annotation.NonNull;
import android.util.TypedValue;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;


public class Util {

    public static final String TAPPED_PROVIDER = "Tapped";

    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        Util.DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static void showToast(@NonNull Context context, int resId, int length) {
        showToast(context, context.getString(resId), length);
    }

    /**
     * Method for printing our fancy custom Toast
     *
     * @param context
     * @param string  Content of the toast
     * @param length  One from Toast.LENGTH_LONG or Toast.LENGTH_SHORT
     */
    public static void showToast(@NonNull Context context, String string, int length) {
        Toast toast = Toast.makeText(context, string, length);
        toast.show();
    }


    public static float dpToPx(@NonNull Context context, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static boolean isImperialMetricsLocale(@NonNull Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        return locale.getCountry().equals("US");
    }

    public static boolean isPackageInstalled(String packageName, @NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}
