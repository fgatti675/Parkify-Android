package com.bahpps.cahue;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.bahpps.cahue.parkedCar.Car;
import com.bahpps.cahue.parkedCar.CarLocationManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * This class is used as a dialog to ask the user if he is sure to store the location in
 * the indicated place.
 *
 * @author Francesco
 */
public class SetCarPositionDialog extends DialogFragment {

    private final static String TAG = "SetCarPositionDialog";

    private Location location;

    Car selected;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final List<Car> cars = CarLocationManager.getAvailableCars(getActivity());

        // fill option names
        ArrayList<String> options = new ArrayList();
        for (Car car : cars) {
            options.add(car.name != null ? car.name : getString(R.string.other));
        }
        String[] optionsArray = new String[options.size()];
        optionsArray = options.toArray(optionsArray);

        // TODO can crash if no devices linked
        selected = cars.get(0);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.car_dialog_text)
                .setSingleChoiceItems(optionsArray, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selected = cars.get(i);
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        selected.location = location;

                        Log.w(TAG, selected.toString());
                        // If ok, we just send and intent and leave the location receivers to do all the work
                        CarLocationManager.saveCar(getActivity(), selected);
                    }
                })
                .setNegativeButton(R.string.cancel, null);


        // Create the AlertDialog object and return it
        return builder.create();
    }

    public void init(Location location) {
        this.location = location;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            location = savedInstanceState.getParcelable("location");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("location", location);
        super.onSaveInstanceState(outState);
    }


}
