package com.cahue.iweco.util;

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

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.R;

import java.util.Date;


public class RatingDialog extends DialogFragment {


    public static boolean shouldBeShown(Context context) {

        if(isDialogShown(context))
            return false;

        Date lastDisplayed = getRateDialogDate(context);
        if (lastDisplayed == null) {
            setRateDialogDate(context, new Date());
            return false;
        }

        return (new Date().getTime() - lastDisplayed.getTime()) > 4 * 24 * 60 * 60;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.rating_message)
                .setIcon(R.drawable.iweco_logo_small)
                .setTitle(R.string.like_question)
                .setPositiveButton(R.string.rate, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
                        startActivity(intent);
                        setRatedDialogShown(getActivity(), true);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setRatedDialogShown(getActivity(), true);
                    }
                });

        setRateDialogDate(getActivity(), new Date());

        // Create the AlertDialog object and return it
        return builder.create();
    }

    public static boolean isDialogShown(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PreferencesUtil.PREF_RATED_DIALOG_ACCEPTED, true);
    }

    public static void setRatedDialogShown(Context context, boolean shown) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PreferencesUtil.PREF_RATE_DIALOG_SHOWN, shown).apply();
    }



    public static Date getRateDialogDate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(!prefs.contains(PreferencesUtil.PREF_RATE_DIALOG_SHOWN))
            return  null;
        return new Date(prefs.getLong(PreferencesUtil.PREF_RATE_DIALOG_SHOWN, 0));
    }

    public static void setRateDialogDate(Context context, Date date) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(PreferencesUtil.PREF_RATE_DIALOG_SHOWN, date.getTime()).apply();
    }
}