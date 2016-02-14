package com.cahue.iweco.cars;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.cahue.iweco.R;
import com.cahue.iweco.activityRecognition.ActivityRecognitionService;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Tracking;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This Activity appears as a dialog. It lists any paired devices and devices detected in the area after discovery. When
 * a linkedDevice is chosen by the user, the MAC address of the linkedDevice is sent back to the parent Activity in the result
 * Intent.
 */
public class CarManagerFragment extends Fragment implements EditCarDialog.CarEditedListener, GoogleApiClient.ConnectionCallbacks {

    // Debugging
    private static final String TAG = CarManagerFragment.class.getSimpleName();

    // Member fields
    private CarDatabase carDatabase;

    private List<Car> cars;

    private List<BluetoothDevice> devices;

    private LinearLayoutManager layoutManager;
    private RecyclerViewCarsAdapter adapter;


    private GoogleApiClient mGoogleApiClient;
    private Location mLastUserLocation;
    /**
     * Device selection
     */
    private BluetoothAdapter mBtAdapter;

    private Button enableBTButton;

    @Nullable
    private Callbacks callbacks;

    private RecyclerView recyclerView;
    private Set<String> selectedDeviceAddresses;
    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();

            // When discovery finds a linkedDevice
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    addDevice(device);
                }

            }

            // When discovery is finished, remove progress bar
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                callbacks.devicesBeingLoaded(false);
            }

            // Update UI and discovery if BT state changed
            else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {

                updateEnableBTButton();
                setBondedDevices();
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (state == BluetoothAdapter.STATE_ON) {
                    doDiscovery();
                }

            }
        }
    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CarDetailsFragment.
     */
    @NonNull
    public static CarManagerFragment newInstance() {
        CarManagerFragment fragment = new CarManagerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_car_manager, container, false);

        /**
         * RecyclerView
         */
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FirstItemDecoration(getActivity()));

        adapter = new RecyclerViewCarsAdapter();

        recyclerView.setAdapter(adapter);

        // this call is actually only necessary with custom ItemAnimators
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        setBondedDevices();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        carDatabase = CarDatabase.getInstance(getActivity());

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        devices = new ArrayList<>();
        cars = carDatabase.retrieveCars(false);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        mGoogleApiClient.disconnect();
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {

        if (mBtAdapter.isEnabled() && !mBtAdapter.isDiscovering()) {
            Log.d(TAG, "doDiscovery");

            // Indicate scanning in the title
            callbacks.devicesBeingLoaded(true);

            // Request discover from BluetoothAdapter
            mBtAdapter.startDiscovery();
        }
    }

    public boolean areDevicesBeingLoaded() {
        return mBtAdapter != null && mBtAdapter.isDiscovering();
    }

    @Override
    public void onCarEdited(@NonNull Car car, boolean newCar) {

        int carPosition = 0;
        if (newCar) {
            cars.add(car);
            selectedDeviceAddresses.add(car.btAddress);

            /**
             * Car's carPosition is simple
             */
            carPosition = cars.size() + 1;
            adapter.notifyItemInserted(carPosition);

            PreferencesUtil.setLongClickToastShown(getActivity(), false);
            ActivityRecognitionService.startIfNoBT(getActivity());

        } else {
            carPosition = 1;
            for (Car existingCar : cars) {
                if (car == existingCar) break;
                carPosition++;
            }
            adapter.notifyItemChanged(carPosition);
        }

        /**
         * Find linkedDevice with the same address as the car
         */
        BluetoothDevice device = null;
        for (BluetoothDevice d : devices) {
            if (d.getAddress().equals(car.btAddress)) {
                device = d;
                break;
            }
        }

        /**
         * The linkedDevice with the address of the car should not be listed anymore
         */
        if (device != null) {
            int deviceIndex = devices.indexOf(device);
            devices.remove(deviceIndex);
            adapter.notifyItemRemoved(cars.size() + 1 + deviceIndex);
            layoutManager.scrollToPosition(carPosition);
        }

        /**
         * DB and server update
         */
        CarsSync.storeCar(carDatabase, getActivity(), car);

        Tracking.sendEvent(Tracking.CATEGORY_CAR_MANAGER, Tracking.ACTION_CAR_EDIT);

    }

    public void onCarRemoved(@NonNull Car car) {

        CarsSync.remove(getActivity(), car, CarDatabase.getInstance(getActivity()));

        int i = 0;
        for (Car c : cars) {
            if (c.equals(car)) break;
            i++;
        }
        cars.remove(i);
        adapter.notifyItemRemoved(i);

        selectedDeviceAddresses.remove(car.btAddress);
        setBondedDevices();
        adapter.notifyDataSetChanged();
    }

    private void setBondedDevices() {

        if (!devices.isEmpty()) {
            devices.clear();
            adapter.notifyDataSetChanged();
        }

        selectedDeviceAddresses = carDatabase.getPairedBTAddresses();

        // Get a set of currently paired devices
        Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : bondedDevices)
            addDevice(bluetoothDevice);
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

        doDiscovery();
        updateEnableBTButton();
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        try {
            callbacks = (Callbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DeviceSelectionLoadingListener");
        }

        try {
            callbacks = (Callbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DeviceSelectionLoadingListener");
        }

        // Register for broadcasts when a linkedDevice is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(mReceiver, filter);

        // Register for broadcasts when bt has been disconnected or disconnected
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        activity.registerReceiver(mReceiver, filter);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;

        // Unregister broadcast listeners
        getActivity().unregisterReceiver(mReceiver);
    }

    private void showClearDialog(@NonNull final Car car) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.delete_car_confirmation)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        onCarRemoved(car);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        builder.create().show();
    }

    private void onDeviceSelected(@NonNull BluetoothDevice device) {

        /**
         * Create a car
         */
        Car car = createCar();
        car.btAddress = device.getAddress();
        car.name = device.getName();

        editCar(car, true);
    }

    @NonNull
    private Car createCar() {
        Car car = new Car();
        car.id = UUID.randomUUID().toString();
        return car;
    }

    private void editCar(Car car, boolean newCar) {
        EditCarDialog.newInstance(car, newCar).show(getFragmentManager(), "EditCarDialog");
    }

    private void addDevice(@NonNull BluetoothDevice device) {
        if (!devices.contains(device) && !selectedDeviceAddresses.contains(device.getAddress())) {
            if (device.getName() != null && !device.getName().isEmpty()) {
                devices.add(device);
                adapter.notifyItemInserted(cars.size() + devices.size());
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastUserLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * Called when a car is clicked
     */
    public interface Callbacks {

        void devicesBeingLoaded(boolean loading);

    }

    public final static class DeviceViewHolder extends RecyclerView.ViewHolder {

        private View layout;
        private TextView title;

        public DeviceViewHolder(@NonNull View view) {
            super(view);
            layout = view.findViewById(R.id.device_item);
            title = (TextView) view.findViewById(R.id.device);
        }
    }

    public final static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(@NonNull View view) {
            super(view);
        }
    }

    public class RecyclerViewCarsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int INFO_TYPE = 0;
        public static final int CAR_TYPE = 1;
        public static final int BT_DEVICE_TYPE = 2;
        public static final int ADD_BUTTON_TYPE = 3;

        @Override
        public int getItemViewType(int position) {

            if (position == 0)
                return INFO_TYPE;
            else if (position < cars.size() + 1)
                return CAR_TYPE;
            else if (position > cars.size() && position < cars.size() + 1 + devices.size())
                return BT_DEVICE_TYPE;
            else if (position == (cars.size() + devices.size() + 1))
                return ADD_BUTTON_TYPE;
            else
                throw new IllegalStateException("Error in recycler view positions");

        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

            /**
             * Information
             */
            if (viewType == INFO_TYPE) {

                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.card_manager_instructions,
                                viewGroup,
                                false);

                enableBTButton = (Button) itemView.findViewById(R.id.enable_bt);
                enableBTButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBtAdapter.enable();
                        updateEnableBTButton();
                    }
                });
                updateEnableBTButton();

                return new SimpleViewHolder(itemView);
            }

            /**
             * Car
             */
            else if (viewType == CAR_TYPE) {
                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.layout_car_details,
                                viewGroup,
                                false);

                CardView cardView = new CardView(viewGroup.getContext());
                cardView.addView(itemView);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                int margin = (int) viewGroup.getContext().getResources().getDimension(R.dimen.default_padding);
                params.setMargins(margin, 0, margin, margin);
                cardView.setLayoutParams(params);
                cardView.setCardBackgroundColor(getResources().getColor(R.color.white));


                return new CarViewHolder(cardView);
            }


            /**
             * Device selection
             */
            else if (viewType == BT_DEVICE_TYPE) {
                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.list_item_device,
                                viewGroup,
                                false);

                return new DeviceViewHolder(itemView);
            }

            /**
             * Add car button
             */
            else if (viewType == ADD_BUTTON_TYPE) {
                Button itemView = (Button) LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.button_borderless,
                                viewGroup,
                                false);
                itemView.setText(R.string.add_car_no_bt);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Car car = createCar();
                        editCar(car, true);
                    }
                });

                return new SimpleViewHolder(itemView);
            }

            throw new IllegalStateException("New type added to the recycler view but no view holder associated");
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

            int viewType = getItemViewType(position);

            /**
             * Car
             */
            if (viewType == CAR_TYPE) {

                CarViewHolder carViewHolder = (CarViewHolder) viewHolder;

                final Car car = cars.get(position - 1);
                carViewHolder.bind(getActivity(), car, mLastUserLocation, mBtAdapter);


                /**
                 * Set up toolbar
                 */
                carViewHolder.toolbar.getMenu().clear();
                carViewHolder.toolbar.inflateMenu(R.menu.edit_car_menu);
                carViewHolder.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.action_edit:
                                editCar(car, false);
                                return true;
                            case R.id.action_delete:
                                showClearDialog(car);
                                return true;
                        }
                        return false;
                    }
                });

            }

            /**
             * Device
             */
            else if (viewType == BT_DEVICE_TYPE) {

                DeviceViewHolder deviceViewHolder = (DeviceViewHolder) viewHolder;
                final BluetoothDevice device = devices.get(position - cars.size() - 1);
                deviceViewHolder.title.setText(device.getName());
                deviceViewHolder.layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDeviceSelected(device);
                    }
                });

            }
        }

        @Override
        public int getItemCount() {
            return 1 + cars.size() + devices.size() + 1;
        }
    }

    class FirstItemDecoration extends RecyclerView.ItemDecoration {

        private final int initialMargin;

        public FirstItemDecoration(@NonNull Context context) {
            float dimension = context.getResources().getDimension(R.dimen.default_padding);
            initialMargin = (int) context.getResources().getDimension(R.dimen.default_padding);
        }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect, android.view.View view, @NonNull android.support.v7.widget.RecyclerView parent, android.support.v7.widget.RecyclerView.State state) {
            if (parent.getChildPosition(view) == 0)
                outRect.set(0, initialMargin, 0, 0);
        }
    }

}
