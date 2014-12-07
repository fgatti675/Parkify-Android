package com.bahpps.cahue.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bahpps.cahue.R;

import java.util.HashSet;
import java.util.Set;

public class Util {

    /*
     * Shared preferences constants
     */
    public static final String PREF_DIALOG_SHOWN = "PREF_DIALOG_SHOWN";

    public static final String TAPPED_PROVIDER = "Tapped";
    public static final String CAR_OTHER_ID = "Id";

    /**
     * Method for printing our fancy custom Toast
     *
     * @param context
     * @param string  Content of the toast
     * @param length  One from Toast.LENGTH_LONG or Toast.LENGTH_SHORT
     */
    public static void createUpperToast(Context context, String string, int length) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.custom_toast, null);
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


    public static final String PREF_BT_DEVICE_ADDRESSES = "PREF_BT_DEVICE_ADDRESSES";

    public static Set<String> getPairedDevices(Context context) {
        SharedPreferences prefs = Util.getSharedPreferences(context);
        HashSet result = new HashSet();
        result.addAll(prefs.getStringSet(PREF_BT_DEVICE_ADDRESSES, new HashSet<String>()));
        return result;
    }

    public static void setPairedDevices(Context context, Set<String> selectedDeviceAddresses) {
        SharedPreferences prefs = Util.getSharedPreferences(context);
        prefs.edit().putStringSet(PREF_BT_DEVICE_ADDRESSES, selectedDeviceAddresses).apply();
    }


    public static final String PREF_OAUTH_TOKEN = "PREF_OAUTH_TOKEN";

    public static void saveOAuthToken(Context context, String token){
        SharedPreferences prefs = Util.getSharedPreferences(context);
        prefs.edit().putString(PREF_OAUTH_TOKEN, token).apply();
    }

    public static String getOauthToken(Context context){
        SharedPreferences prefs = Util.getSharedPreferences(context);
        return prefs.getString(PREF_OAUTH_TOKEN, null);
    }

}
