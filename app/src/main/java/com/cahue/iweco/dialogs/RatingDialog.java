package com.cahue.iweco.dialogs;

/**
 * Get some nice ratings
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
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Tracking;

import java.util.Date;

public class RatingDialog extends DialogFragment {

    public static boolean shouldBeShown(Context context) {

        if (isDialogShown(context))
            return false;

        Date lastDisplayed = getRateDialogLastDisplayed(context);
        if (lastDisplayed == null) {
            setRateDialogDate(context, new Date());
            return false;
        }

        return (System.currentTimeMillis() - lastDisplayed.getTime()) > 3 * 24 * 60 * 60 * 1000;
    }

    public static boolean isDialogShown(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PreferencesUtil.PREF_RATED_DIALOG_ACCEPTED, false);
    }

    public static void setRatedDialogShown(Context context, boolean shown) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PreferencesUtil.PREF_RATED_DIALOG_SHOWN, shown).apply();
    }

    public static Date getRateDialogLastDisplayed(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains(PreferencesUtil.PREF_RATED_DIALOG_SHOWN_DATE))
            return null;
        return new Date(prefs.getLong(PreferencesUtil.PREF_RATED_DIALOG_SHOWN_DATE, 0));
    }

    public static void setRateDialogDate(Context context, Date date) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(PreferencesUtil.PREF_RATED_DIALOG_SHOWN_DATE, date.getTime()).apply();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.rating_message)
                .setIcon(R.drawable.weco_logo_small)
                .setTitle(R.string.like_question)
                .setPositiveButton(R.string.rate, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
                        startActivity(intent);
                        setRatedDialogShown(getActivity(), true);
                        Tracking.sendEvent(Tracking.CATEGORY_RATING_DIALOG, Tracking.ACTION_ACCEPT);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setRatedDialogShown(getActivity(), true);
                        Tracking.sendEvent(Tracking.CATEGORY_RATING_DIALOG, Tracking.ACTION_DISMISS);
                    }
                });

        setRateDialogDate(getActivity(), new Date());

        // Create the AlertDialog object and return it
        return builder.create();
    }
}