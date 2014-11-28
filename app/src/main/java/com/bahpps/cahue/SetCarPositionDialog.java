package com.bahpps.cahue;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bahpps.cahue.parkedCar.Car;
import com.bahpps.cahue.parkedCar.CarLocationManager;
import com.bahpps.cahue.util.Util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * This class is used as a dialog to ask the user if he is sure to store the location in
 * the indicated place.
 *
 * @author Francesco
 */
public class SetCarPositionDialog extends DialogFragment {
    private final static String TAG = "SetCarPositionDialog";

    private Location location;

    List<String> paredIds;

    Set<BluetoothDevice> bondedBTDevices;

    String selectedId;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // bonded BT devices to the phone
        bondedBTDevices = mBtAdapter.getBondedDevices();

        // BT addresses linked to the application
        paredIds = new ArrayList(Util.getPairedDevices(getActivity()));

        // fill option names
        ArrayList<String> options = new ArrayList();
        for(String id: paredIds) {
            String name = getLinkedBTName(id);
            if(name != null)
                options.add(name);
        }
        String[] optionsArray = new String[options.size()];
        optionsArray = options.toArray(optionsArray);


        // TODO can crash if no devices linked
        selectedId = paredIds.get(0);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setIcon(R.drawable.ic_action_help)
                .setTitle(R.string.car_dialog_text)
                .setSingleChoiceItems(optionsArray, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedId = paredIds.get(i);
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        Log.w(TAG, location.toString());

                        Car car = new Car();
                        car.location = location;
                        car.id = selectedId;
                        car.time = new Date();

                        // If ok, we just send and intent and leave the location receivers to do all the work
                        CarLocationManager.saveLocation(getActivity(), car);
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

    private String getLinkedBTName(String carId) {
        String title = null;
        for (BluetoothDevice device : bondedBTDevices) {
            if (carId.equals(device.getAddress())) {
                return device.getName();
            }
        }
        return null;
    }

}
