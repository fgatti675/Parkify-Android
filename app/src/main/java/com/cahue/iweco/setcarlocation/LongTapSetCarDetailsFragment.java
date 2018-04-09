package com.cahue.iweco.setcarlocation;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridView;
import android.widget.TextView;

import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.OnCarClickedListener;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.util.CarButtonAdapter;
import com.cahue.iweco.util.FetchAddressDelegate;
import com.cahue.iweco.util.PreferencesUtil;

import java.util.List;

/**
 * Created by Francesco on 06/07/2015.
 */
public class LongTapSetCarDetailsFragment extends DetailsFragment {

    private static final String ARG_SPOT = "arg_spot";
    private static final String TAG = LongTapSetCarDetailsFragment.class.getSimpleName();

    @Nullable
    private ParkingSpot spot;

    private CarDatabase carDatabase;

    private TextView distanceView;
    private TextView addressView;

    private Location userLocation;

    private OnCarClickedListener carSelectedListener;
    private View mainView;
    private GridView buttonsLayout;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LongTapSetCarDetailsFragment.
     */
    @NonNull
    public static DetailsFragment newInstance(ParkingSpot spot) {
        DetailsFragment fragment = new LongTapSetCarDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SPOT, spot);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        carDatabase = CarDatabase.getInstance();

        spot = getArguments().getParcelable(ARG_SPOT);
        if (spot.address == null)
            fetchAddress();

        try {
            this.carSelectedListener = (OnCarClickedListener) getFragmentManager().findFragmentByTag(LongTapLocationDelegate.FRAGMENT_TAG);
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement " + OnCarClickedListener.class.getName());
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mainView = inflater.inflate(R.layout.layout_long_tap_set_car_details, container, false);
        if (spot != null) {

            // Update distance
            distanceView = mainView.findViewById(R.id.distance);

            // Update addressView
            addressView = mainView.findViewById(R.id.address);

            buttonsLayout = mainView.findViewById(R.id.car_buttons);


        }
        return mainView;
    }

    @Override
    public void onResume() {
        super.onResume();
        carDatabase.retrieveCars(new CarDatabase.CarsRetrieveListener() {
            @Override
            public void onCarsRetrieved(List<Car> cars) {
                int numCars = cars.size();
                int numColumns;
                if (numCars < 4) numColumns = numCars;
                else if (numCars % 3 == 0) numColumns = 3;
                else if (numCars % 2 == 0) numColumns = 2;
                else numColumns = 3;
                buttonsLayout.setNumColumns(numColumns);

                if (!cars.isEmpty())
                    buttonsLayout.setAdapter(new CarButtonAdapter(carSelectedListener, cars));

                Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
                mainView.startAnimation(fadeInAnimation);

            }

            @Override
            public void onCarsRetrievedError() {
                Log.e(TAG, "Error populating car buttons");
            }
        });

        updateDistance();
        updateAddress();
    }

    @Override
    public void setUserLocation(Location userLocation) {

        this.userLocation = userLocation;
        View view = getView();
        if (view != null) {
            updateDistance();
        }
    }

    private void updateDistance() {
        if (userLocation != null) {
            float distanceM = spot.location.distanceTo(userLocation);

            if (PreferencesUtil.isUseMiles(getActivity())) {
                distanceView.setText(String.format("%.1f miles", distanceM / 1609.34));
            } else {
                distanceView.setText(String.format("%.1f km", distanceM / 1000));
            }
        }
    }

    private void updateAddress() {
        if (spot.address != null && addressView != null) {
            addressView.setText(spot.address);
        }
    }

    /**
     * Fetch address for the spot
     */
    private void fetchAddress() {

        FetchAddressDelegate fetchAddressDelegate = new FetchAddressDelegate();
        fetchAddressDelegate.fetch(getActivity(), spot.location, new FetchAddressDelegate.Callbacks() {
            @Override
            public void onAddressFetched(String address) {
                spot.address = address;
                updateAddress();
            }

            @Override
            public void onError(String error) {
            }
        });
    }

}
