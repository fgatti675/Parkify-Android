package com.cahue.iweco.util;

/**
 * Created by Francesco on 07/06/2015.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;


public class FacebookAppInvitesDialog extends DialogFragment {

    private static final String TAG = FacebookAppInvitesDialog.class.getSimpleName();

    public static void showAppInviteDialog(final MapsActivity activity) {

        AppInviteDialog appInviteDialog = new AppInviteDialog(activity);

        String appLinkUrl = activity.getString(R.string.facebook_app_link);

        AppInviteContent appInviteContent = new AppInviteContent.Builder()
                .setApplinkUrl(appLinkUrl)
                .build();

        appInviteDialog.registerCallback(activity.getFacebookCallbackManager(),
                new FacebookCallback<AppInviteDialog.Result>() {
                    @Override
                    public void onSuccess(AppInviteDialog.Result result) {
                        Log.d(TAG, "onSuccess: ");
                        PreferencesUtil.setAdsRemoved(activity, true);
                        activity.sendBroadcast(new Intent(Constants.INTENT_ADS_REMOVED));
                        for (String s : result.getData().keySet()) {
                            Object o = result.getData().get(s);
                            Log.d(TAG, "onSuccess: " + o);
                        }
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "onCancel: ");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(TAG, "onError: ");
                    }
                });

        appInviteDialog.show(appInviteContent);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.share_promo)
                .setTitle(R.string.share)
                .setIcon(R.drawable.weco_logo_small)
                .setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showAppInviteDialog((MapsActivity) getActivity());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }

}