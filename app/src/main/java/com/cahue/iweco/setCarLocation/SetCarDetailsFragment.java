package com.cahue.iweco.setCarLocation;

import android.location.Location;
import android.os.Bundle;

import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.spots.ParkingSpot;
import com.cahue.iweco.spots.SpotDetailsFragment;

/**
 * Created by Francesco on 06/07/2015.
 */
public class SetCarDetailsFragment extends DetailsFragment{

    private static final String ARG_LOCATION = "arg_location";
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SetCarDetailsFragment.
     */
    public static DetailsFragment newInstance(Location location) {
        DetailsFragment fragment = new SetCarDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_LOCATION, location);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setUserLocation(Location userLocation) {

    }
}
