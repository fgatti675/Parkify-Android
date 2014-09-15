package com.bahpps.cahue;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.bahpps.cahue.auxiliar.Util;

/**
 * Fancy dialog we used as information
 *
 * @author Francesco
 */
public class InfoDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String message = String.format(getString(R.string.info_text), getString(R.string.app_name));

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setIcon(R.drawable.ic_action_help)
                .setTitle(R.string.help)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null);
        // Create the AlertDialog object and return it
        return builder.create();

    }
}