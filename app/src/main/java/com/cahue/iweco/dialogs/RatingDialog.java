package com.cahue.iweco.dialogs;

/**
 * Get some nice ratings
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.R;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Tracking;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Date;

public class RatingDialog extends DialogFragment {

    public static boolean shouldBeShown(Context context) {

        if (isDialogAccepted(context))
            return false;

        Date lastDisplayed = getRateDialogLastDisplayed(context);
        if (lastDisplayed == null) {
            setRateDialogLastDisplayed(context, new Date());
            return false;
        }

        return (System.currentTimeMillis() - lastDisplayed.getTime()) > (getNextShowIntervalDays(context) * 24 * 60 * 60 * 1000);
    }

    private static boolean isDialogAccepted(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PreferencesUtil.PREF_RATED_DIALOG_ACCEPTED, false);
    }

    private static void setRatedDialogAccepted(Context context, boolean accepted) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PreferencesUtil.PREF_RATED_DIALOG_ACCEPTED, accepted).apply();
    }

    private static Date getRateDialogLastDisplayed(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains(PreferencesUtil.PREF_RATED_DIALOG_SHOWN_DATE))
            return null;
        return new Date(prefs.getLong(PreferencesUtil.PREF_RATED_DIALOG_SHOWN_DATE, 0));
    }

    private static void setRateDialogLastDisplayed(Context context, @NonNull Date date) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(PreferencesUtil.PREF_RATED_DIALOG_SHOWN_DATE, date.getTime()).apply();
    }

    private static int getNextShowIntervalDays(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(PreferencesUtil.PREF_RATED_DIALOG_NEXT_INTERVAL_DAYS, 6);
    }

    private static void setNextShowIntervalDays(Context context, int days) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(PreferencesUtil.PREF_RATED_DIALOG_NEXT_INTERVAL_DAYS, days).apply();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
        builder.setMessage(R.string.rating_message)
                .setIcon(R.drawable.ic_logo_heart)
                .setTitle(R.string.like_question)
                .setPositiveButton(R.string.rate, (dialog, id) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
                    startActivity(intent);
                    setRatedDialogAccepted(getActivity(), true);
                    Tracking.sendEvent(Tracking.CATEGORY_RATING_DIALOG, Tracking.ACTION_ACCEPT);
                    Bundle bundle = new Bundle();
                    firebaseAnalytics.logEvent("rating_dialog_accept", bundle);
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    int currentInterval = getNextShowIntervalDays(getActivity());
                    setNextShowIntervalDays(getActivity(), currentInterval + 3);
                    Tracking.sendEvent(Tracking.CATEGORY_RATING_DIALOG, Tracking.ACTION_DISMISS);
                    Bundle bundle = new Bundle();
                    firebaseAnalytics.logEvent("rating_dialog_cancel", bundle);
                });

        setRateDialogLastDisplayed(getActivity(), new Date());

        // Create the AlertDialog object and return it
        return builder.create();
    }
}