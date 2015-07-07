package com.cahue.iweco.setCarLocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.cars.CarManagerActivity;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.locationServices.GeofenceCarService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * This class is used as a dialog to ask the user if he is sure to store the location in
 * the indicated place.
 *
 * @author Francesco
 */
@Deprecated
public class SetCarPositionDialog extends DialogFragment {

    private Callbacks mCallbacks;

    public interface Callbacks{
        /**
         * Called when the user sets the car manually
         */
        void onCarPositionUpdate(String carId);
    }

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if(!(activity instanceof Callbacks)) {
            throw new IllegalStateException(activity.getClass().getSimpleName() + " must implement " + Callbacks.class.getName());
        }

        mCallbacks = (Callbacks) activity;
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

        CarDatabase carDatabase = CarDatabase.getInstance(getActivity());
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

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.what_car_is_parked)
                .setSingleChoiceItems(optionsArray, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selected = cars.get(i);
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        onCarSet(selected);
                    }
                })
                .setNegativeButton(R.string.cancel, null);


        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void onCarSet(Car car) {
        car.spotId = null;
        car.location = location;
        car.address = null;
        car.time = new Date();

        Log.i(TAG, car.toString());

        // If ok, we just send and intent and leave the location receivers to do all the work
        CarDatabase carDatabase = CarDatabase.getInstance(getActivity());
        CarsSync.storeCar(carDatabase, getActivity(), car);

        mCallbacks.onCarPositionUpdate(car.id);

        /**
         * In debug mode we set a geofence
         */
        if (BuildConfig.DEBUG) {
            Intent intent = new Intent(getActivity(), GeofenceCarService.class);
            intent.putExtra(Constants.INTENT_CAR_EXTRA_ID, car.id);
            getActivity().startService(intent);
        }
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
        return builder.create();
    }
}
