package com.cahue.iweco.setCarLocation;

import android.app.Activity;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.util.FetchAddressIntentService;
import com.cahue.iweco.util.Util;

import java.util.Date;
import java.util.List;

/**
 * Created by Francesco on 06/07/2015.
 */
public class SetCarDetailsFragment extends DetailsFragment {

    private static final String ARG_LOCATION = "arg_location";
    private static final String ARG_TIME = "arg_time";
    private static final String ARG_ADDRESS = "arg_address";

    private Date time;
    private Location location;
    private String addressString;

    private CarDatabase carDatabase;

    private TextView distance;
    private TextView timeAgo;
    private TextView address;

    private Location userLocation;

    private CarSelectedListener carSelectedListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SetCarDetailsFragment.
     */
    public static DetailsFragment newInstance(Location location, Date time, String address) {
        DetailsFragment fragment = new SetCarDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_LOCATION, location);
        args.putSerializable(ARG_TIME, time);
        if (address != null)
            args.putString(ARG_ADDRESS, address);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            this.carSelectedListener = (CarSelectedListener) getFragmentManager().findFragmentByTag(SetCarLocationDelegate.FRAGMENT_TAG);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + CarSelectedListener.class.getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        carDatabase = CarDatabase.getInstance(getActivity());

        if (getArguments() != null) {
            location = getArguments().getParcelable(ARG_LOCATION);
            time = (Date) getArguments().getSerializable(ARG_TIME);
            if (getArguments().containsKey(ARG_ADDRESS))
                addressString = getArguments().getString(ARG_ADDRESS);
        }

        if (addressString == null)
            fetchAddress();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_set_car_details, container, false);
        if (location != null) {

            // Set time ago
            timeAgo = (TextView) view.findViewById(R.id.time);
            updateTimeAgo();

            // Update distance
            distance = (TextView) view.findViewById(R.id.distance);
            updateDistance();

            // Update address
            address = (TextView) view.findViewById(R.id.address);
            updateAddress();

            GridView buttonsLayout = (GridView) view.findViewById(R.id.car_buttons);
            List<Car> cars = carDatabase.retrieveCars(false);

            if(!cars.isEmpty())
                buttonsLayout.setAdapter(new ButtonAdapter(cars));

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
            float distanceM = location.distanceTo(userLocation);

            if (Util.isImperialMetricsLocale(getActivity())) {
                distance.setText(String.format("%.1f miles", distanceM / 1609.34));
            } else {
                distance.setText(String.format("%.1f km", distanceM / 1000));
            }
        }
    }

    private void updateTimeAgo() {
        timeAgo.setText(DateUtils.getRelativeTimeSpanString(time.getTime()));
    }

    private void updateAddress() {
        if (addressString != null && address != null) {
            address.setText(addressString);
        }
    }

    private void fetchAddress() {
        /**
         * Fetch address
         */
        Intent fetchAddressIntent = new Intent(getActivity(), FetchAddressIntentService.class);
        fetchAddressIntent.putExtra(FetchAddressIntentService.RECEIVER, new AddressResultReceiver());
        fetchAddressIntent.putExtra(FetchAddressIntentService.LOCATION_DATA_EXTRA, location);
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

            addressString = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);

            updateAddress();

        }
    }

    public class ButtonAdapter extends BaseAdapter {

        private List<Car> cars;

        public ButtonAdapter(List<Car> cars) {
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
                    car.location = location;
                    car.spotId = null;
                    car.address = addressString;
                    car.time = time;
                    CarsSync.storeCar(carDatabase, getActivity(), car);
                    carSelectedListener.onCarButtonClicked(car.id);
                }
            });
            return button;
        }
    }
}
