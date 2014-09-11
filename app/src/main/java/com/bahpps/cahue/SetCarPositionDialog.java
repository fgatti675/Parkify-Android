package com.bahpps.cahue;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.bahpps.cahue.auxiliar.Util;


/**
 * This class is used as a dialog to ask the user if he is sure to store the location in
 * the indicated place.
 * @author Francesco
 *
 */
public class SetCarPositionDialog extends DialogFragment  {

    Location location;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setIcon(R.drawable.common_signin_btn_icon_dark)
                .setTitle(R.string.car_dialog_text)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.w("CAR_DIALOG", location.toString());
                        // If ok, we just send and intent and leave the location receivers to do all the work
                        Intent intent = new Intent(Util.INTENT_NEW_CAR_POS);
                        intent.putExtra(Util.EXTRA_LOCATION, location);
                        getActivity().sendBroadcast(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
