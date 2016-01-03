package com.cahue.iweco.setCarLocation;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.util.FetchAddressIntentService;
import com.cahue.iweco.util.PreferencesUtil;

import java.util.List;

/**
 * Created by Francesco on 06/07/2015.
 */
public class SetCarDetailsFragment extends DetailsFragment {

    private static final String ARG_SPOT = "arg_spot";

    private ParkingSpot spot;

    private CarDatabase carDatabase;

    private TextView distanceView;
    private TextView timeAgoView;
    private TextView addressView;

    private Location userLocation;

    private CarSelectedListener carSelectedListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SetCarDetailsFragment.
     */
    public static DetailsFragment newInstance(ParkingSpot spot) {
        DetailsFragment fragment = new SetCarDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SPOT, spot);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        carDatabase = CarDatabase.getInstance(getActivity());

        spot = getArguments().getParcelable(ARG_SPOT);
        if (spot.address == null)
            fetchAddress();

        try {
            this.carSelectedListener = (CarSelectedListener) getFragmentManager().findFragmentByTag(LongTapLocationDelegate.getFragmentTag(spot));
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement " + CarSelectedListener.class.getName());
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_set_car_details, container, false);
        if (spot != null) {

            // Set time ago
            timeAgoView = (TextView) view.findViewById(R.id.time);
            updateTimeAgo();

            // Update distance
            distanceView = (TextView) view.findViewById(R.id.distance);
            updateDistance();

            // Update addressView
            addressView = (TextView) view.findViewById(R.id.address);
            updateAddress();

            GridView buttonsLayout = (GridView) view.findViewById(R.id.car_buttons);
            List<Car> cars = carDatabase.retrieveCars(false);

            if (!cars.isEmpty())
                buttonsLayout.setAdapter(new CarButtonAdapter(cars));

            Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
            view.startAnimation(fadeInAnimation);
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
        Intent fetchAddressIntent = new Intent(getActivity(), FetchAddressIntentService.class);
        fetchAddressIntent.putExtra(FetchAddressIntentService.RECEIVER, new AddressResultReceiver());
        fetchAddressIntent.putExtra(FetchAddressIntentService.LOCATION_DATA_EXTRA, spot.location);
        getActivity().startService(fetchAddressIntent);
    }

    public interface CarSelectedListener {
        void onCarButtonClicked(String carId);
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver() {
            super(new Handler());
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultCode != FetchAddressIntentService.SUCCESS_RESULT)
                return;

            spot.address = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);

            updateAddress();
        }
    }

    public class CarButtonAdapter extends BaseAdapter {

        private List<Car> cars;

        public CarButtonAdapter(List<Car> cars) {
            this.cars = cars;
        }

        public int getCount() {
            return cars.size();
        }

        public Object getItem(int position) {
            return cars.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        // create a new Button for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            Button button;
            final Car car = cars.get(position);
            if (convertView == null) {
                button = (Button) LayoutInflater.from(parent.getContext()).
                        inflate(R.layout.button_borderless,
                                parent,
                                false);
//                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_car_grey600_24dp, 0, 0, 0);
            } else {
                button = (Button) convertView;
            }
            button.setText(car.name);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CarsSync.updateCarFromPossibleSpot(carDatabase, getActivity(), car, spot);
                    carSelectedListener.onCarButtonClicked(car.id);
                }
            });
            return button;
        }
    }
}
