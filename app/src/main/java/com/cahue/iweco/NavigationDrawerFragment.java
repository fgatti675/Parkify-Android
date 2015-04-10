package com.cahue.iweco;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarViewHolder;
import com.cahue.iweco.cars.database.CarDatabase;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks {

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private GoogleApiClient mGoogleApiClient;

    private DrawerLayout mDrawerLayout;
    private LinearLayout mDrawerListView;

    private View mFragmentContainerView;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private RecyclerViewDrawerAdapter adapter;
    private List<Car> cars;

    private Location mLastUserLocation;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cars = CarDatabase.getInstance(getActivity()).retrieveCars(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerListView = (LinearLayout) inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);

        /**
         * RecyclerView
         */
        recyclerView = (RecyclerView) mDrawerListView.findViewById(R.id.recycler_view);

        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
//        recyclerView.addItemDecoration(new FirstItemDecoration(getActivity()));

        adapter = new RecyclerViewDrawerAdapter();

        recyclerView.setAdapter(adapter);

        // this call is actually only necessary with custom ItemAnimators
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return mDrawerListView;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     * @param mToolbar
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout, Toolbar mToolbar) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                mToolbar,
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }
                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public void closeDrawers() {
        mDrawerLayout.closeDrawers();
    }


    @Override
    public void onConnected(Bundle bundle) {
        mLastUserLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    public class RecyclerViewDrawerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int CAR_TYPE = 0;
        public static final int INFO_TYPE = 1;
        public static final int BT_DEVICE_TYPE = 2;
        public static final int ADD_BUTTON_TYPE = 3;

        @Override
        public int getItemViewType(int position) {

            if (position < cars.size())
                return CAR_TYPE;
//            else if (position == cars.size())
//                return INFO_TYPE;
//            else if (position > cars.size() && position < cars.size() + 1 + devices.size())
//                return BT_DEVICE_TYPE;
//            else if (position == (cars.size() + devices.size() + 1))
//                return ADD_BUTTON_TYPE;
            else
                throw new IllegalStateException("Error in recycler view positions");

        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

            /**
             * Car
             */
            if (viewType == CAR_TYPE) {
                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.fragment_car_details,
                                viewGroup,
                                false);

                return new CarViewHolder(itemView);
            }

//            /**
//             * Information
//             */
//            else if (viewType == INFO_TYPE) {
//
//                View itemView = LayoutInflater.from(viewGroup.getContext()).
//                        inflate(R.layout.card_manager_instructions,
//                                viewGroup,
//                                false);
//
//                enableBTButton = (Button) itemView.findViewById(R.id.enable_bt);
//                enableBTButton.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
//                    }
//                });
//                updateEnableBTButton();
//
//                return new SimpleViewHolder(itemView);
//            }
//
//            /**
//             * Device selection
//             */
//            else if (viewType == BT_DEVICE_TYPE) {
//                View itemView = LayoutInflater.from(viewGroup.getContext()).
//                        inflate(R.layout.list_item_device,
//                                viewGroup,
//                                false);
//
//                return new DeviceViewHolder(itemView);
//            }
//
//            /**
//             * Add car button
//             */
//            else if (viewType == ADD_BUTTON_TYPE) {
//                View itemView = LayoutInflater.from(viewGroup.getContext()).
//                        inflate(R.layout.button_add_device,
//                                viewGroup,
//                                false);
//                itemView.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Car car = createCar();
//                        editCar(car, true);
//                    }
//                });
//
//                return new SimpleViewHolder(itemView);
//            }

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

                final Car car = cars.get(position);
                carViewHolder.bind(getActivity(), car, mLastUserLocation, BluetoothAdapter.getDefaultAdapter());

                /**
                 * Set up click listener
                 */
                carViewHolder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCallbacks.onNavigationCarClick(car.id);
                        mDrawerLayout.closeDrawers();
                    }
                });

                /**
                 * Set up toolbar
                 */
//                carViewHolder.toolbar.getMenu().clear();
//                carViewHolder.toolbar.inflateMenu(R.menu.edit_car_menu);
//                carViewHolder.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//                    @Override
//                    public boolean onMenuItemClick(MenuItem menuItem) {
//                        switch (menuItem.getItemId()) {
//                            case R.id.action_edit:
//                                editCar(car, false);
//                                return true;
//                            case R.id.action_delete:
//                                showClearDialog(car);
//                                return true;
//                        }
//                        return false;
//                    }
//                });

            }

//            /**
//             * Device
//             */
//            else if (viewType == BT_DEVICE_TYPE) {
//
//                DeviceViewHolder deviceViewHolder = (DeviceViewHolder) viewHolder;
//                final BluetoothDevice device = devices.get(position - cars.size() - 1);
//                deviceViewHolder.title.setText(device.getName());
//                deviceViewHolder.layout.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        onDeviceSelected(device);
//                    }
//                });
//
//            }
        }

        @Override
        public int getItemCount() {
            return cars.size();
        }
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationCarClick(String carId);
    }
}
