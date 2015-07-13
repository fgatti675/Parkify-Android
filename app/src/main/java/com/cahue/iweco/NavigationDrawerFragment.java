package com.cahue.iweco;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarViewHolder;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.login.AuthUtils;
import com.cahue.iweco.util.DividerItemDecoration;
import com.cahue.iweco.util.LoadProfileImage;
import com.facebook.share.widget.AppInviteDialog;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    private static final boolean ADS_ENABLED = false;

    private static final String TAG = NavigationDrawerFragment.class.getSimpleName();
    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private OnCarClickedListener mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private RelativeLayout mDrawerListView;

    private View mFragmentContainerView;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private RecyclerViewDrawerAdapter adapter;
    private List<Car> cars;

    private Location mLastUserLocation;

    private Navigation navigation;

    private BroadcastReceiver carUpdatedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            cars = CarDatabase.getInstance(getActivity()).retrieveCars(false);
            adapter.notifyDataSetChanged();
        }

    };
    private ImageView userImage;
    private TextView usernameTextView;
    private TextView emailTextView;
    private boolean skippedLogin;

    private AdView adView;

    private BillingFragment billingFragment;
    private BroadcastReceiver billingReadyReceiver;

    private BroadcastReceiver newPurchaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            adView.setVisibility(View.GONE);
        }
    };

    private BroadcastReceiver userInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setUpUserDetails();
        }
    };

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        skippedLogin = AuthUtils.isSkippedLogin(getActivity());
        cars = CarDatabase.getInstance(getActivity()).retrieveCars(false);


    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mDrawerListView = (RelativeLayout) inflater.inflate(
                R.layout.fragment_navigation_drawer,
                container,
                false);

        View userDetails = mDrawerListView.findViewById(R.id.user_details);

        userImage = (ImageView) mDrawerListView.findViewById(R.id.profile_image);
        usernameTextView = (TextView) mDrawerListView.findViewById(R.id.username);
        emailTextView = (TextView) mDrawerListView.findViewById(R.id.email);

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

        if (!skippedLogin)
            setUpUserDetails();

        userDetails.setVisibility(skippedLogin ? View.GONE : View.VISIBLE);

        // Create adView.
        adView = (AdView) mDrawerListView.findViewById(R.id.adView);
        adView.setVisibility(View.GONE);

        return mDrawerListView;
    }

    @Override
    public void onStart() {
        super.onStart();

        billingFragment = (BillingFragment) getFragmentManager().findFragmentByTag(BillingFragment.FRAGMENT_TAG);
        if (billingFragment == null)
            throw new RuntimeException("The billing fragment must be set");

        /**
         * Set up the ad if the billing service is ready
         */
        if (billingFragment.isBillingServiceReady()) {
            setUpAd();
        }

        /**
         * Wait for it otherwise
         */
        else {
            Log.d(TAG, "Witing billing service");
            billingReadyReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Billing ready");
                    setUpAd();
                }
            };
            getActivity().registerReceiver(billingReadyReceiver, new IntentFilter(Constants.INTENT_BILLING_READY));
        }


        getActivity().registerReceiver(newPurchaseReceiver, new IntentFilter(Constants.INTENT_NEW_PURCHASE));

        getActivity().registerReceiver(userInfoReceiver, new IntentFilter(Constants.INTENT_USER_INFO_UPDATE));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (billingReadyReceiver != null)
            getActivity().unregisterReceiver(billingReadyReceiver);
        billingReadyReceiver = null;
        getActivity().unregisterReceiver(newPurchaseReceiver);
        getActivity().unregisterReceiver(userInfoReceiver);
    }

    private void setUpAd() {

        if(!ADS_ENABLED) return;

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {

                /**
                 * Check if the user has purchases. If there is an error we don't display just in case
                 *
                 * @return
                 */
                boolean displayAd = false;
                Bundle ownedItems = billingFragment.getPurchases();
                int response = ownedItems.getInt("RESPONSE_CODE");
                if (response == 0) {
                    ArrayList<?> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                    Log.d(TAG, "Purchased items: " + purchaseDataList.toString());
                    displayAd = purchaseDataList.isEmpty();
                }

                return displayAd;
            }

            @Override
            protected void onPostExecute(Boolean displayAd) {
                Log.d(TAG, "Display ads returned " + displayAd);
                if (displayAd) {
                    adView.setVisibility(View.VISIBLE);
                    // Ad request
                    AdRequest adRequest = new AdRequest.Builder().build();
                    adView.loadAd(adRequest);
                } else {
                    adView.setVisibility(View.GONE);
                }
            }
        }.execute();

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
    public void setUpDrawer(int fragmentId, DrawerLayout drawerLayout, Toolbar mToolbar) {
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

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                if (!isAdded()) {
                    return;
                }

                Tracker tracker = ((IwecoApp) getActivity().getApplication()).getTracker();
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("UX")
                        .setAction("click")
                        .setLabel("Drawer open")
                        .build());

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
        adView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(carUpdatedReceiver);
        adView.pause();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (OnCarClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement OnCarClickedListener.");
        }

        try {
            navigation = (Navigation) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement Navigation.");
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
        adView.destroy();
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

    public void setUserLocation(Location userLocation) {
        this.mLastUserLocation = userLocation;
        recyclerView.getAdapter().notifyDataSetChanged();
    }


    public class RecyclerViewDrawerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int CAR_TYPE = 0;
        public static final int CAR_MANAGER_TYPE = 1;
        public static final int SHARE_TYPE = 2;
        public static final int DONATE_TYPE = 3;
        public static final int PREFERENCES_TYPE = 4;
        public static final int HELP_TYPE = 5;
        public static final int SIGN_OUT_TYPE = 6;

        // each entry represents an item in the drawer
        private int[] itemTypes;

        public RecyclerViewDrawerAdapter() {

            int totalElements = cars.size() + (AppInviteDialog.canShow() ? 5 : 4);

            itemTypes = new int[totalElements];

            int i = 0;
            for (;i<cars.size();) {
                itemTypes[i++] = CAR_TYPE;
            }

            itemTypes[i++] = CAR_MANAGER_TYPE;
            if (AppInviteDialog.canShow()) itemTypes[i++] = SHARE_TYPE;
            itemTypes[i++] = DONATE_TYPE;
            itemTypes[i++] = HELP_TYPE;
            itemTypes[i++] = SIGN_OUT_TYPE;
        }

        @Override
        public int getItemViewType(int position) {

            return itemTypes[position];

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
                    || viewType == SHARE_TYPE
                    || viewType == DONATE_TYPE
                    || viewType == PREFERENCES_TYPE
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
                        if (car.location != null) {

                            ((ParkedCarDelegate) getFragmentManager().findFragmentByTag(car.id)).onCarClicked();

                            mCallbacks.onCarClicked(car.id);

                            if (mDrawerLayout != null)
                                mDrawerLayout.closeDrawers();

                        } else {
                            Toast.makeText(getActivity(), R.string.position_not_set, Toast.LENGTH_SHORT).show();
                        }
                    }
                };
                carViewHolder.cardView.setOnClickListener(clickListener);

                /**
                 * Set up toolbar
                 */
                carViewHolder.toolbar.setOnClickListener(clickListener);

            } else if (viewType == CAR_MANAGER_TYPE
                    || viewType == SHARE_TYPE
                    || viewType == DONATE_TYPE
                    || viewType == PREFERENCES_TYPE
                    || viewType == HELP_TYPE
                    || viewType == SIGN_OUT_TYPE) {

                MenuViewHolder menuViewHolder = (MenuViewHolder) viewHolder;
                if (viewType == CAR_MANAGER_TYPE) {
                    bindCarManager(menuViewHolder);
                } else if (viewType == SHARE_TYPE) {
                    bindShare(menuViewHolder);
                } else if (viewType == DONATE_TYPE) {
                    bindDonate(menuViewHolder);
                } else if (viewType == PREFERENCES_TYPE) {
                    bindHelp(menuViewHolder);
                } else if (viewType == HELP_TYPE) {
                    bindPreferences(menuViewHolder);
                } else if (viewType == SIGN_OUT_TYPE) {
                    bindSignOut(menuViewHolder);
                }
            }

        }

        private void bindCarManager(MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.edit_cars);
            menuViewHolder.icon.setImageResource(R.drawable.ic_border_color_grey600_24dp);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.goToCarManager();
                }
            });
        }

        private void bindShare(MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.share);
            menuViewHolder.icon.setImageResource(R.drawable.ic_facebook_box_grey600_24dp);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.openShareDialog();
                }
            });
        }

        private void bindPreferences(MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.preferences);
            menuViewHolder.icon.setImageResource(R.drawable.ic_facebook_box_grey600_24dp);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.goToPreferences();
                }
            });
        }

        private void bindDonate(MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.donate);
            menuViewHolder.icon.setImageResource(R.drawable.ic_heart_grey600_24dp);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.openDonationDialog();
                }
            });
        }

        private void bindSignOut(MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(skippedLogin ? R.string.sign_in : R.string.disconnect);
            menuViewHolder.icon.setImageResource(R.drawable.ic_logout_grey600_24dp);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.signOut();
                }
            });
        }

        private void bindHelp(MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.help);
            menuViewHolder.icon.setImageResource(R.drawable.ic_help_circle_grey600_24dp);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.goToTutorial();
                }
            });
        }

        @Override
        public int getItemCount() {
            return itemTypes.length;
        }

        public class MenuViewHolder extends RecyclerView.ViewHolder {

            public ImageView icon;
            public TextView title;
            public View itemView;

            public MenuViewHolder(View itemView) {
                super(itemView);
                this.itemView = itemView;
                icon = (ImageView) itemView.findViewById(R.id.icon);
                title = (TextView) itemView.findViewById(R.id.title);
            }
        }
    }


    private void setUpUserDetails() {
        usernameTextView.setText(AuthUtils.getLoggedUsername(getActivity()));
        emailTextView.setText(AuthUtils.getEmail(getActivity()));
        new LoadProfileImage(userImage).execute(AuthUtils.getProfilePicURL(getActivity()));
    }


}
