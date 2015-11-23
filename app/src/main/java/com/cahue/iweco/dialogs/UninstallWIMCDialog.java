package com.cahue.iweco.dialogs;

/**
 * Created by Francesco on 07/06/2015.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.cahue.iweco.R;
import com.cahue.iweco.util.PreferencesUtil;


public class UninstallWIMCDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.uninstall_WIMC)
                .setPositiveButton(R.string.uninstall, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Uri packageUri = Uri.parse("package:com.whereismycar");
                        Intent uninstallIntent =
                                new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
                        startActivity(uninstallIntent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        PreferencesUtil.setWIMCUninstallDialogShown(getActivity(), true);

        // Create the AlertDialog object and return it
        return builder.create();
    }
}