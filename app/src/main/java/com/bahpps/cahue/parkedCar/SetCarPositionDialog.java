package com.bahpps.cahue.parkedCar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.bahpps.cahue.R;
import com.bahpps.cahue.cars.Car;
import com.bahpps.cahue.cars.CarDatabase;
import com.bahpps.cahue.cars.CarManagerActivity;

import java.util.ArrayList;
import java.util.List;


/**
 * This class is used as a dialog to ask the user if he is sure to store the location in
 * the indicated place.
 *
 * @author Francesco
 */
public class SetCarPositionDialog extends DialogFragment {

    private final static String TAG = SetCarPositionDialog.class.getSimpleName();
    private static final String ARG_LOCATION = "arg_location";

    private Location location;

    Car selected;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param location Parameter 1.
     * @return A new instance of fragment MarkerDetailsFragment.
     */
    public static SetCarPositionDialog newInstance(Location location) {
        SetCarPositionDialog fragment = new SetCarPositionDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_LOCATION, location);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            location = getArguments().getParcelable(ARG_LOCATION);
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // TODO: not necessary to create a new instance of this
        CarDatabase carDatabase = new CarDatabase(getActivity());
        final List<Car> cars = carDatabase.retrieveCars(false);

        // there has to be a default car
        if (cars.isEmpty()) {
            return getEmptyDialog();
        }

        // fill option names
        ArrayList<String> options = new ArrayList();
        for (Car car : cars) {
            options.add(car.name != null ? car.name : getString(R.string.other));
        }
        String[] optionsArray = new String[options.size()];
        optionsArray = options.toArray(optionsArray);

        selected = cars.get(0);

        String lastSavedId = CarManager.getLastCarSavedId(getActivity());

        int itemIndex = 0;
        boolean lastSelected = false;
        if (lastSavedId != null) {
            for (Car car : cars) {
                if (car.btAddress.equals(lastSavedId)) {
                    selected = car;
                    lastSelected = true;
                    break;
                }
                itemIndex++;
            }
        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.car_dialog_text)
                .setSingleChoiceItems(optionsArray, lastSelected ? itemIndex : 0, new DialogInterface.OnClickListener() {
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

                        CarDatabase carDatabase = new CarDatabase(getActivity());
                        carDatabase.saveCar(selected);
                    }
                })
                .setNegativeButton(R.string.cancel, null);


        // Create the AlertDialog object and return it
        return builder.create();
    }


    public Dialog getEmptyDialog() {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.no_cars)
                .setMessage(R.string.no_cars_long)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        getActivity().startActivity(new Intent(getActivity(), CarManagerActivity.class));
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        return  builder.create();
    }
}
