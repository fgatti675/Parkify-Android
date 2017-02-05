package com.cahue.iweco.util;

/**
 * Created by Francesco on 07/06/2015.
 */

import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;

import static com.facebook.GraphRequest.TAG;


public class FacebookAppInvitesDialog  {

    public static void showAppInviteDialog(@NonNull final MapsActivity activity) {

        // Facebook callback registration
        CallbackManager mFacebookCallbackManager = CallbackManager.Factory.create();
        activity.setFacebookCallbackManager(mFacebookCallbackManager);

        AppInviteDialog appInviteDialog = new AppInviteDialog(activity);

        String appLinkUrl = activity.getString(R.string.facebook_app_link);

        AppInviteContent appInviteContent = new AppInviteContent.Builder()
                .setApplinkUrl(appLinkUrl)
                .build();

        appInviteDialog.registerCallback(mFacebookCallbackManager,
                new FacebookCallback<AppInviteDialog.Result>() {
                    @Override
                    public void onSuccess(@NonNull AppInviteDialog.Result result) {
                        Log.d(TAG, "onSuccess: ");
                        PreferencesUtil.setAdsRemoved(activity, true);
                        activity.sendBroadcast(new Intent(Constants.INTENT_ADS_REMOVED));
                        for (String s : result.getData().keySet()) {
                            Object o = result.getData().get(s);
                            Log.d(TAG, "onSuccess: " + o);
                        }
                        Tracking.sendEvent(Tracking.CATEGORY_FACEBOOK_INVITE, Tracking.ACTION_SUCCESS);
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "onCancel: ");
                        Tracking.sendEvent(Tracking.CATEGORY_FACEBOOK_INVITE, Tracking.ACTION_CANCELLED);
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(TAG, "onError: ");
                        Tracking.sendEvent(Tracking.CATEGORY_FACEBOOK_INVITE, Tracking.ACTION_ERROR);
                    }
                });

        appInviteDialog.show(appInviteContent);
    }


}