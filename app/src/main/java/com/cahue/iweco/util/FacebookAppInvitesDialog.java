package com.cahue.iweco.util;

/**
 * Created by Francesco on 07/06/2015.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.cahue.iweco.R;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;


public class FacebookAppInvitesDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.share_promo)
                .setTitle(R.string.share)
                .setIcon(R.drawable.iweco_logo_small)
                .setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String appLinkUrl, previewImageUrl;

                        appLinkUrl = getString(R.string.facebook_app_link);
                        previewImageUrl = getString(R.string.facebook_app_image_url);

                        AppInviteContent content = new AppInviteContent.Builder()
                                .setApplinkUrl(appLinkUrl)
                                .setPreviewImageUrl(previewImageUrl)
                                .build();
                        AppInviteDialog.show(getActivity(), content);
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