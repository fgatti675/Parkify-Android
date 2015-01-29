package com.bahpps.cahue.cars;


import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bahpps.cahue.R;
import com.bahpps.cahue.util.DividerItemDecoration;

import java.util.ArrayList;
import java.util.HashSet;
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

    private LinearLayoutManager layoutManager;
    private RecyclerViewCarsAdapter adapter;

    /**
     * Device selection
     */
    private BluetoothAdapter mBtAdapter;

    private Button enableBTButton;

    private Set<String> selectedDeviceAddresses;

    private DeviceSelectionLoadingListener mListener;
    private RecyclerView recyclerView;

    /**
     * Cars that are being edited
     */
    private Set<Car> carsBeingEdited;

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_car_manager, container, false);

        /**
         * RecyclerView
         */
        recyclerView = (RecyclerView) view.findViewById(R.id.cardList);

        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        carDatabase = new CarDatabase(getActivity());

        selectedDeviceAddresses = carDatabase.getPairedBTAddresses();

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        devices = new ArrayList<>();
        cars = carDatabase.retrieveCars(false);
        carsBeingEdited = new HashSet<>();

        setBondedDevices();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
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
                mListener.devicesBeingLoaded(false);

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
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {

        if (mBtAdapter.isEnabled() && !mBtAdapter.isDiscovering()) {
            Log.d(TAG, "doDiscovery");

            // Indicate scanning in the title
            mListener.devicesBeingLoaded(true);

            // Request discover from BluetoothAdapter
            mBtAdapter.startDiscovery();
        }
    }

    public boolean areDevicesBeingLoaded() {
        return mBtAdapter != null && mBtAdapter.isDiscovering();
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
                carViewHolder.bind(car, carsBeingEdited.contains(car));

            }

            /**
             * Device
             */
            else if (position > cars.size()) {

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
            return cars.size() + 1 + devices.size();
        }
    }


    private void setBondedDevices() {

        if (!devices.isEmpty()) {
            devices.clear();
            adapter.notifyDataSetChanged();
        }

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

        doDiscovery();
        updateEnableBTButton();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (DeviceSelectionLoadingListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DeviceSelectionLoadingListener");
        }

        // Register for broadcasts when a device is discovered
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
        mListener = null;

        // Unregister broadcast listeners
        getActivity().unregisterReceiver(mReceiver);
    }


    private void onDeviceSelected(BluetoothDevice device) {

        /**
         * Create a car
         */
        Car car = new Car();
        car.id = UUID.randomUUID().toString();
        car.btAddress = device.getAddress();
        car.name = device.getName();

        carsBeingEdited.add(car);

        cars.add(car);

        /**
         * Car's position is simple
         */
        int position = cars.size() - 1;
        adapter.notifyItemInserted(position);
        int deviceIndex = devices.indexOf(device);
        devices.remove(deviceIndex);
        adapter.notifyItemRemoved(cars.size() + 1 + deviceIndex);
        layoutManager.scrollToPosition(position);
    }


    private void addDevice(BluetoothDevice device) {
        if (!devices.contains(device)) {
            devices.add(device);
            adapter.notifyItemInserted(cars.size() + devices.size());
        }
    }

    public interface DeviceSelectionLoadingListener {

        public void devicesBeingLoaded(boolean loading);

    }

    public final static class CarViewHolder extends RecyclerView.ViewHolder {

        private Car car;

        private Toolbar toolbar;
        private LinearLayout infoLayout;
        private TextView name;
        private TextView time;
        private EditText nameEdit;

        public CarViewHolder(View itemView) {
            super(itemView);

            toolbar = (Toolbar) itemView.findViewById(R.id.car_toolbar);
            infoLayout = (LinearLayout) itemView.findViewById(R.id.infoLayout);
            name = (TextView) itemView.findViewById(R.id.name);
            time = (TextView) itemView.findViewById(R.id.time);
            nameEdit = (EditText) itemView.findViewById(R.id.name_edit);
        }

        public void bind(final Car car, final boolean editMode) {

            this.car = car;

            /**
             * Is this car being edited
             */
            if (editMode) {

                infoLayout.setVisibility(View.GONE);
                nameEdit.setVisibility(View.VISIBLE);
                nameEdit.setText(car.name);
                nameEdit.requestFocus();

                toolbar.getMenu().clear();
                toolbar.inflateMenu(R.menu.car_edit_menu);
                toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.action_clear:
                                bind(car, false);
                                return true;
                            case R.id.action_save:
                                car.name = nameEdit.getText().toString();
                                bind(car, false);
                                return true;
                        }
                        return false;
                    }
                });
            }

            /**
             * Otherwise just show info
             */
            else {
                infoLayout.setVisibility(View.VISIBLE);
                nameEdit.setVisibility(View.GONE);

                name.setText(car.name);
                if (car.time != null)
                    time.setText(DateUtils.getRelativeTimeSpanString(car.time.getTime()));

                toolbar.getMenu().clear();
                toolbar.inflateMenu(R.menu.car_list_menu);
                toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.action_edit:
                                bind(car, true);
                                return true;
                        }
                        return false;
                    }
                });
            }

        }
    }

    public final static class DeviceViewHolder extends RecyclerView.ViewHolder {

        private View layout;
        private TextView title;

        public DeviceViewHolder(View view) {
            super(view);
            layout = view.findViewById(R.id.device_item);
            title = (TextView) view.findViewById(R.id.device);
        }
    }

    public final static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(View view) {
            super(view);
        }
    }
}
