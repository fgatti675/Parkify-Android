package com.bahpps.cahue.cars;


import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bahpps.cahue.R;
import com.bahpps.cahue.util.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This Activity appears as a dialog. It lists any paired devices and devices detected in the area after discovery. When
 * a device is chosen by the user, the MAC address of the device is sent back to the parent Activity in the result
 * Intent.
 */
public class CarManagerFragment extends Fragment {

    // Debugging
    private static final String TAG = CarManagerFragment.class.getSimpleName();

    // Member fields
    private CarDatabase carDatabase;

    private List<Car> cars;

    private List<BluetoothDevice> devices;

    private RecyclerViewCarsAdapter adapter;

    /**
     * Device selection
     */
    private BluetoothAdapter mBtAdapter;

    private Button enableBTButton;

    private Set<String> selectedDeviceAddresses;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CarDetailsFragment.
     */
    public static CarManagerFragment newInstance() {
        CarManagerFragment fragment = new CarManagerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_car_manager, container, false);

        /**
         * RecyclerView
         */
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.cardList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        devices = new ArrayList<>();
        cars = carDatabase.retrieveCars(false);

        adapter = new RecyclerViewCarsAdapter();

        recyclerView.setAdapter(adapter);

        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration();
        recyclerView.addItemDecoration(itemDecoration);

        // this call is actually only necessary with custom ItemAnimators
        recyclerView.setItemAnimator(new DefaultItemAnimator());

//        recyclerView.addOnItemTouchListener(this);


        return view;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        carDatabase = new CarDatabase(getActivity());

        selectedDeviceAddresses = carDatabase.getPairedBTAddresses();

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    public class RecyclerViewCarsAdapter extends
            RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int CAR_TYPE = 0;
        public static final int INFO_TYPE = 1;
        public static final int BT_DEVICE_TYPE = 2;

        @Override
        public int getItemViewType(int position) {

            if (position < cars.size())
                return CAR_TYPE;
            else if (position == cars.size())
                return INFO_TYPE;
            else
                return BT_DEVICE_TYPE;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            if (viewType == CAR_TYPE) {
                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.fragment_car_details,
                                viewGroup,
                                false);

                return new CarViewHolder(itemView);
            }

            /**
             * Information
             */
            else if (viewType == INFO_TYPE) {

                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.fragment_device_selection,
                                viewGroup,
                                false);

                enableBTButton = (Button) itemView.findViewById(R.id.enable_bt);
                enableBTButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                    }
                });

                // Find and set up the ListView for paired devices
                updateEnableBTButton();

                return new SimpleViewHolder(itemView);
            }

            /**
             * Device selection
             */
            else if (viewType == BT_DEVICE_TYPE) {
                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.list_device_item,
                                viewGroup,
                                false);

                return new DeviceViewHolder(itemView);
            }

            throw new IllegalStateException("New type added to the recycler view but no view holder associated");
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

            /**
             * Car
             */
            if (position < cars.size()) {

                CarViewHolder carViewHolder = (CarViewHolder) viewHolder;

                Car car = cars.get(position);

                carViewHolder.name.setText(car.name);
                if (car.time != null)
                    carViewHolder.time.setText(DateUtils.getRelativeTimeSpanString(car.time.getTime()));
            }

            /**
             * Device
             */
            else if (position > cars.size()) {

                DeviceViewHolder deviceViewHolder = (DeviceViewHolder) viewHolder;
                final BluetoothDevice device = devices.get(position - cars.size() - 1);
                deviceViewHolder.button.setText(device.getName());
                deviceViewHolder.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // TODO
                        Car car = new Car();
                        car.id = UUID.randomUUID().toString();
                        car.btAddress = device.getAddress();
                        car.name = device.getName();
                        addCar(car, device);
                    }
                });

            }
        }

        @Override
        public int getItemCount() {
            return cars.size() + 1 + devices.size();
        }
    }


    private void setBondedDevices() {
        // Get a set of currently paired devices
        Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
        devices.addAll(bondedDevices);
    }

    private void updateEnableBTButton() {

        if (enableBTButton != null) {

            if (mBtAdapter.isEnabled()) {
                enableBTButton.setVisibility(View.GONE);
            } else {
                enableBTButton.setVisibility(View.VISIBLE);
            }

        }
    }


    @Override
    public void onResume() {
        super.onResume();
        setBondedDevices();
        updateEnableBTButton();
    }


    private void addCar(Car car, BluetoothDevice bluetoothDevice) {
        cars.add(car);
        adapter.notifyItemInserted(cars.size() - 1);
        int i = devices.indexOf(bluetoothDevice);
        devices.remove(i);
        adapter.notifyItemRemoved(cars.size() + 1 + i);
    }

    public interface DeviceSelectionLoadingListener {

        public void devicesBeingLoaded(boolean loading);

    }


    public final static class CarViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView time;

        public CarViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);
            time = (TextView) itemView.findViewById(R.id.time);
        }
    }

    public final static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private Button button;

        public DeviceViewHolder(View view) {
            super(view);
            button = (Button) view.findViewById(R.id.device);
        }
    }

    public final static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(View view) {
            super(view);
        }
    }
}
