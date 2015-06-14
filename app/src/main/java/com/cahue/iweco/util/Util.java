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
import java.util.Locale;
import java.util.TimeZone;

public class Util {

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


    public static float dpToPx(Context context, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static boolean isImperialMetricsLocale(Context context){
        Locale locale = context.getResources().getConfiguration().locale;
        return locale.getCountry().equals("US");
    }

    public static boolean isPackageInstalled(String packageName, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}
