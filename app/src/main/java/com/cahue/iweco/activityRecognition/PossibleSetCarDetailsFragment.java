package com.cahue.iweco.activityrecognition;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
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
import com.cahue.iweco.model.PossibleSpot;
import com.cahue.iweco.util.CarButtonAdapter;
import com.cahue.iweco.util.FetchAddressDelegate;
import com.cahue.iweco.util.PreferencesUtil;

import java.util.List;

/**
 * Created by Francesco on 06/07/2015.
 */
public class PossibleSetCarDetailsFragment extends DetailsFragment {

    private static final String ARG_SPOT = "arg_spot";

    // id of the fragment contructing this one
    private static final String ARG_DELEGATE_FRAGMENT_ID = "arg_delegate_fragment_id";

    @Nullable
    private PossibleSpot spot;

    private CarDatabase carDatabase;

    private Toolbar toolbar;
    private TextView distanceView;
    private TextView timeAgoView;
    private TextView addressView;

    private Location userLocation;

    private OnCarClickedListener carSelectedListener;
    private OnPossibleSpotDeletedListener possibleSpotDeletedListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LongTapSetCarDetailsFragment.
     */
    @NonNull
    public static DetailsFragment newInstance(PossibleSpot spot, String parentFragmentTag) {
        DetailsFragment fragment = new PossibleSetCarDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SPOT, spot);
        args.putString(ARG_DELEGATE_FRAGMENT_ID, parentFragmentTag);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        String parentFragmentId = getArguments().getString(ARG_DELEGATE_FRAGMENT_ID);
        Fragment fragment = getFragmentManager().findFragmentByTag(parentFragmentId);

        try {
            this.carSelectedListener = (OnCarClickedListener) fragment;
        } catch (ClassCastException e) {
            throw new ClassCastException(fragment.getClass().getName()
                    + " must implement " + OnCarClickedListener.class.getName());
        }

        try {
            this.possibleSpotDeletedListener = (OnPossibleSpotDeletedListener) fragment;
        } catch (ClassCastException e) {
            throw new ClassCastException(fragment.getClass().getName()
                    + " must implement " + OnCarClickedListener.class.getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        carDatabase = CarDatabase.getInstance();

        spot = getArguments().getParcelable(ARG_SPOT);
        if (spot.address == null)
            fetchAddress();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_possible_set_car_details, container, false);
        if (spot != null) {

            toolbar = view.findViewById(R.id.toolbar);
            toolbar.inflateMenu(R.menu.possible_spot_menu);
            toolbar.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case R.id.action_delete:
                        possibleSpotDeletedListener.onPossibleSpotDeleted(spot);
                        return true;
                    case R.id.action_delete_all:
                        possibleSpotDeletedListener.onAllPossibleSpotsDeleted();
                        return true;
                }
                return false;
            });
            // Set time ago
            timeAgoView = view.findViewById(R.id.time);
            updateTimeAgo();

            // Update distance
            distanceView = view.findViewById(R.id.distance);
            updateDistance();

            // Update addressView
            addressView = view.findViewById(R.id.address);
            updateAddress();

            GridView buttonsLayout = view.findViewById(R.id.car_buttons);
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
                    view.startAnimation(fadeInAnimation);
                }

                @Override
                public void onCarsRetrievedError() {

                }
            });
        }
        return view;
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

    private void updateTimeAgo() {
        timeAgoView.setText(DateUtils.getRelativeTimeSpanString(spot.time.getTime()));
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

    /**
     * Interface for telling the containing activity that the car position has been deleted
     */
    public interface OnPossibleSpotDeletedListener {

        void onPossibleSpotDeleted(PossibleSpot spot);
        void onAllPossibleSpotsDeleted();

    }

}
