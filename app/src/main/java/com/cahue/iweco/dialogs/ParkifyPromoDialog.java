package com.cahue.iweco.dialogs;

/**
 * Created by Francesco on 07/06/2015.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.cahue.iweco.R;
import com.cahue.iweco.util.PreferencesUtil;

import java.util.Date;
import java.util.Locale;


public class ParkifyPromoDialog extends DialogFragment {


    public static boolean shouldBeShown(Context context) {

        Locale locale = context.getResources().getConfiguration().locale;
        if (!locale.getCountry().equalsIgnoreCase("ES")
                | !locale.getCountry().equalsIgnoreCase("DE")
                | !locale.getCountry().equalsIgnoreCase("AR")) return false;

        Date lastDisplayed = getIwecoPromoDialogShown(context);
        if (lastDisplayed == null) return true;

        return (new Date().getTime() - lastDisplayed.getTime()) > 2 * 24 * 60 * 60;
    }

    public static void setIwecoPromoDialogShown(Context context, Date date) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(PreferencesUtil.PREF_IWECO_PROMO_DATE, date.getTime()).apply();
    }

    public static Date getIwecoPromoDialogShown(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains(PreferencesUtil.PREF_IWECO_PROMO_DATE))
            return null;
        return new Date(prefs.getLong(PreferencesUtil.PREF_IWECO_PROMO_DATE, 0));
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.parkify_promo)
                .setIcon(R.drawable.weco_logo_small)
                .setTitle(R.string.new_app)
                .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.cahue.iweco&referrer=utm_source%3Dwhereismycar"));
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        setIwecoPromoDialogShown(getActivity(), new Date());

        // Create the AlertDialog object and return it
        return builder.create();
    }
}