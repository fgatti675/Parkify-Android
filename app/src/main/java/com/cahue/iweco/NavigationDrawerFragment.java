package com.cahue.iweco;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.cahue.iweco.cars.CarViewHolder;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.login.AuthUtils;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.Tracking;
import com.cahue.iweco.util.Util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    private static final String TAG = NavigationDrawerFragment.class.getSimpleName();

    private boolean skippedLogin;
    private List<Car> cars;
    private Location mLastUserLocation;
    private Navigation navigation;

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    @Nullable
    private OnCarClickedListener mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;

    private RecyclerViewDrawerAdapter adapter;
    @NonNull
    private final BroadcastReceiver carUpdatedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            retrieveCarsFromDB();

            adapter.setUpElements();
            adapter.notifyDataSetChanged();
        }

    };
    private View mFragmentContainerView;
    private View userDetailsView;
    private ImageView userImage;
    private TextView usernameTextView;
    private TextView emailTextView;
    private Button signInButton;
    @NonNull
    private final BroadcastReceiver userInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setUpUserDetails();
        }
    };
    private int bottomMargin;
    private int topMargin;

    private RelativeLayout mRootView;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        skippedLogin = AuthUtils.isSkippedLogin(getActivity());

        retrieveCarsFromDB();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = (RelativeLayout) inflater.inflate(
                R.layout.fragment_navigation_drawer,
                container,
                false);

        userDetailsView = mRootView.findViewById(R.id.user_details);

        userImage = (ImageView) mRootView.findViewById(R.id.profile_image);
        usernameTextView = (TextView) mRootView.findViewById(R.id.username);
        emailTextView = (TextView) mRootView.findViewById(R.id.email);

        signInButton = (Button) mRootView.findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigation.signOutAndGoToLoginScreen(true);
            }
        });

        /**
         * RecyclerView
         */
        RecyclerView recyclerView = (RecyclerView) mRootView.findViewById(R.id.recycler_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);


        adapter = new RecyclerViewDrawerAdapter();
        adapter.setUpElements();
        recyclerView.setAdapter(adapter);

        // this call is actually only necessary with custom ItemAnimators
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return mRootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        retrieveCarsFromDB();

        adapter.setUpElements();
        adapter.notifyDataSetChanged();

        getActivity().registerReceiver(userInfoReceiver, new IntentFilter(Constants.INTENT_USER_INFO_UPDATE));

    }

    private void retrieveCarsFromDB() {
        cars = CarDatabase.getInstance().retrieveCars(getActivity(), true);

        // hide 'Other' car if not parked
        Iterator<Car> iterator = cars.iterator();
        while (iterator.hasNext()) {
            Car car = iterator.next();
            if (car.isOther() && car.location == null) iterator.remove();
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(userInfoReceiver);
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUpDrawer(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                null,
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                Tracking.sendView(Tracking.CATEGORY_MAP);

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                if (!isAdded()) {
                    return;
                }

                Tracking.sendView(Tracking.CATEGORY_NAVIGATION_DRAWER);

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
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

        getActivity().registerReceiver(carUpdatedReceiver, new IntentFilter(Constants.INTENT_CAR_UPDATED));
        getActivity().registerReceiver(carUpdatedReceiver, new IntentFilter(Constants.INTENT_ADDRESS_UPDATE));

        setUpUserDetails();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(carUpdatedReceiver);
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        try {
            mCallbacks = (OnCarClickedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement OnCarClickedListener.");
        }

        try {
            navigation = (Navigation) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement Navigation.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        navigation = null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
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

    public void setUserLocation(Location userLocation) {
        this.mLastUserLocation = userLocation;
    }


    private void setUpUserDetails() {

        if (skippedLogin) {
            userDetailsView.setVisibility(View.GONE);
            signInButton.setVisibility(View.VISIBLE);
        } else {
            userDetailsView.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.GONE);
            String loggedUsername = AuthUtils.getLoggedUsername(getActivity());

            // in case it is being loaded in the background
            if (loggedUsername == null)
                return;

            usernameTextView.setText(loggedUsername);
            emailTextView.setText(AuthUtils.getEmail(getActivity()));
            String profilePicURL = AuthUtils.getProfilePicURL(getActivity());


            if (profilePicURL != null) {
                RequestQueue requestQueue = ParkifyApp.getParkifyApp().getRequestQueue();
                ImageRequest profilePicRequest = new ImageRequest(profilePicURL, new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        userImage.setImageBitmap(response);
                    }
                }, 0, 0, ImageView.ScaleType.CENTER, Bitmap.Config.RGB_565, null);
                Cache.Entry entry = new Cache.Entry();
                entry.ttl = 24 * 60 * 60 * 1000;
                profilePicRequest.setCacheEntry(entry);
                requestQueue.add(profilePicRequest);
            }

        }
    }

    public void setTopMargin(int topMargin) {
        this.topMargin = topMargin;
        mRootView.setPadding(0, topMargin, 0, 0);
    }


    public void setBottomMargin(int bottomMargin) {
        this.bottomMargin = bottomMargin;
    }

    public class RecyclerViewDrawerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int CAR_TYPE = 0;
        public static final int CAR_MANAGER_TYPE = 1;
        public static final int DONATE_TYPE = 2;
        public static final int PREFERENCES_TYPE = 3;
        public static final int SIGN_OUT_TYPE = 4;

        // each entry represents an item in the drawer
        private List<Integer> itemTypes;

        public void setUpElements() {

            itemTypes = new ArrayList<>();

            for (int i = 0; i < cars.size(); i++) {
                itemTypes.add(CAR_TYPE);
            }

            itemTypes.add(CAR_MANAGER_TYPE);
            itemTypes.add(DONATE_TYPE);
            itemTypes.add(PREFERENCES_TYPE);
            if (!skippedLogin)
                itemTypes.add(SIGN_OUT_TYPE);
        }

        @Override
        public int getItemViewType(int position) {
            return itemTypes.get(position);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

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
                    || viewType == PREFERENCES_TYPE
                    || viewType == SIGN_OUT_TYPE) {

                View itemView = LayoutInflater.from(viewGroup.getContext()).
                        inflate(R.layout.layout_item_drawer_navigation,
                                viewGroup,
                                false);


                return new MenuViewHolder(itemView);
            }

            throw new IllegalStateException("New type added to the recycler view but no view holder associated : " + viewType);
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
                        if (car.location != null) {

                            Tracking.sendEvent(Tracking.CATEGORY_NAVIGATION_DRAWER, Tracking.ACTION_CAR_SELECTED, Tracking.LABEL_SELECTED_FROM_DRAWER);
                            ((ParkedCarDelegate) getFragmentManager().findFragmentByTag(ParkedCarDelegate.getFragmentTag(car.id))).activate();

                            mCallbacks.onCarSelected(car);

                            if (mDrawerLayout != null)
                                mDrawerLayout.closeDrawers();

                        } else {
                            Util.showBlueToast(getActivity(), R.string.position_not_set, Toast.LENGTH_SHORT);
                        }
                    }
                };
                carViewHolder.cardView.setOnClickListener(clickListener);

                /**
                 * Set up toolbar
                 */
                carViewHolder.toolbar.setOnClickListener(clickListener);

            } else if (viewType == CAR_MANAGER_TYPE) {
                bindCarManager((MenuViewHolder) viewHolder);
            } else if (viewType == DONATE_TYPE) {
                bindDonate((MenuViewHolder) viewHolder);
            } else if (viewType == PREFERENCES_TYPE) {
                bindPreferences((MenuViewHolder) viewHolder);
            } else if (viewType == SIGN_OUT_TYPE) {
                bindSignOut((MenuViewHolder) viewHolder);
            }

        }

        private void bindCarManager(@NonNull MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.edit_cars);
            menuViewHolder.icon.setImageResource(R.drawable.ic_edit_primary_blue_24dp);
            menuViewHolder.divider.setVisibility(View.VISIBLE);

            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.goToCarManager();
                    Tracking.sendEvent(Tracking.CATEGORY_NAVIGATION_DRAWER, Tracking.ACTION_CAR_MANAGER_CLICK);
                }
            });
        }

        private void bindDonate(@NonNull MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.donate);
            menuViewHolder.icon.setImageResource(R.drawable.ic_favorite_24dp);
            menuViewHolder.subtitle.setText(R.string.and_remove_ads);
            menuViewHolder.subtitle.setVisibility(View.VISIBLE);
            menuViewHolder.divider.setVisibility(View.VISIBLE);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.openDonationDialog();
                    Tracking.sendEvent(Tracking.CATEGORY_NAVIGATION_DRAWER, Tracking.ACTION_DONATION_CLICK);
                }
            });
        }

        private void bindPreferences(@NonNull MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.preferences);
            menuViewHolder.icon.setImageResource(R.drawable.ic_settings_24dp);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.goToPreferences();
                    Tracking.sendEvent(Tracking.CATEGORY_NAVIGATION_DRAWER, Tracking.ACTION_SETTINGS_CLICK);
                }
            });
        }

        private void bindSignOut(@NonNull MenuViewHolder menuViewHolder) {
            menuViewHolder.itemView.setPadding(0, 0, 0, bottomMargin);
            menuViewHolder.title.setText(R.string.disconnect);
            menuViewHolder.icon.setImageResource(R.drawable.ic_logout_grey600_24dp);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.signOutAndGoToLoginScreen(true);
                    Tracking.sendEvent(Tracking.CATEGORY_NAVIGATION_DRAWER, Tracking.ACTION_SIGN_OUT);
                }
            });
        }

        @Override
        public int getItemCount() {
            return itemTypes.size();
        }

        public class MenuViewHolder extends RecyclerView.ViewHolder {

            public final ImageView icon;
            public final TextView title;
            public final TextView subtitle;
            public final View divider;
            public final View itemView;

            public MenuViewHolder(@NonNull View itemView) {
                super(itemView);
                this.itemView = itemView;
                icon = (ImageView) itemView.findViewById(R.id.icon);
                title = (TextView) itemView.findViewById(R.id.title);
                subtitle = (TextView) itemView.findViewById(R.id.subtitle);
                divider = itemView.findViewById(R.id.divider);
            }
        }

    }


}
