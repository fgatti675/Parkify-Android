package com.cahue.iweco;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarViewHolder;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.util.DividerItemDecoration;
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
    private OnCarClickedListener mCallbacks;

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

    private BroadcastReceiver carUpdatedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            cars = CarDatabase.getInstance(getActivity()).retrieveCars(false);
            adapter.notifyDataSetChanged();
        }

    };

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cars = CarDatabase.getInstance(getActivity()).retrieveCars(false);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
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
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

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
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(carUpdatedReceiver, new IntentFilter(Constants.INTENT_CAR_UPDATE));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(carUpdatedReceiver);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (OnCarClickedListener) activity;
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
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
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
        public static final int CAR_MANAGER_TYPE = 1;
        public static final int DONATE_TYPE = 2;
        public static final int HELP_TYPE = 3;
        public static final int SIGN_OUT_TYPE = 4;

        @Override
        public int getItemViewType(int position) {

            if (position < cars.size())
                return CAR_TYPE;
            else if (position == cars.size())
                return CAR_MANAGER_TYPE;
            else if (position == cars.size() + 1)
                return DONATE_TYPE;
            else if (position == cars.size() + 2)
                return HELP_TYPE;
            else if (position == cars.size() + 3)
                return SIGN_OUT_TYPE;
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
                        inflate(R.layout.layout_car_details,
                                viewGroup,
                                false);

                return new CarViewHolder(itemView);
            } else if (viewType == CAR_MANAGER_TYPE
                    || viewType == DONATE_TYPE
                    || viewType == HELP_TYPE
                    || viewType == SIGN_OUT_TYPE) {

                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.layout_item_drawer_navigation,
                                viewGroup,
                                false);
                return new MenuViewHolder(itemView);
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

                final Car car = cars.get(position);
                carViewHolder.bind(getActivity(), car, mLastUserLocation, BluetoothAdapter.getDefaultAdapter());

                /**
                 * Set up click listener
                 */
                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCallbacks.onCarClicked(car.id);
                        mDrawerLayout.closeDrawers();
                    }
                };
                carViewHolder.cardView.setOnClickListener(clickListener);

                /**
                 * Set up toolbar
                 */
                carViewHolder.toolbar.setOnClickListener(clickListener);

            } else if (viewType == CAR_MANAGER_TYPE
                    || viewType == DONATE_TYPE
                    || viewType == HELP_TYPE
                    || viewType == SIGN_OUT_TYPE) {

                MenuViewHolder menuViewHolder = (MenuViewHolder) viewHolder;
                if (viewType == CAR_MANAGER_TYPE) {
                    bindCarManager(menuViewHolder);
                } else if (viewType == DONATE_TYPE) {
                    bindDonate(menuViewHolder);
                } else if (viewType == HELP_TYPE) {
                    bindHelp(menuViewHolder);
                } else if (viewType == SIGN_OUT_TYPE) {
                    bindSignOut(menuViewHolder);
                }
            }

        }

        private void bindSignOut(MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.disconnect);
            menuViewHolder.icon.setImageResource(R.drawable.ic_logout_grey600_24dp);
        }

        private void bindHelp(MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.help);
            menuViewHolder.icon.setImageResource(R.drawable.ic_help_circle_grey600_24dp);
        }

        private void bindDonate(MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.donate);
            menuViewHolder.icon.setImageResource(R.drawable.ic_heart_grey600_24dp);
        }

        private void bindCarManager(MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.cars);
            menuViewHolder.icon.setImageResource(R.drawable.ic_car_grey600_24dp);
        }

        @Override
        public int getItemCount() {
            return cars.size() + 4;
        }

        public class MenuViewHolder extends RecyclerView.ViewHolder {

            public ImageView icon;
            public TextView title;

            public MenuViewHolder(View itemView) {
                super(itemView);
                icon = (ImageView) itemView.findViewById(R.id.icon);
                title = (TextView) itemView.findViewById(R.id.title);
            }
        }
    }



}
