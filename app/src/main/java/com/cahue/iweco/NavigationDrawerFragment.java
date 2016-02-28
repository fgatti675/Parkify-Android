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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cahue.iweco.cars.CarViewHolder;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.login.AuthUtils;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.LoadImageTask;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Tracking;
import com.cahue.iweco.util.Util;
import com.facebook.share.widget.AppInviteDialog;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeAppInstallAdView;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.google.android.gms.ads.formats.NativeContentAdView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment implements NativeAppInstallAd.OnAppInstallAdLoadedListener, NativeContentAd.OnContentAdLoadedListener {

    private static final boolean ADS_ENABLED = true;

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
    private View mFragmentContainerView;

    private View userDetailsView;
    private ImageView userImage;
    private TextView usernameTextView;
    private TextView emailTextView;
    @NonNull
    private final BroadcastReceiver userInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setUpUserDetails();
        }
    };

    private View currentAdView;
    private NativeContentAdView nativeContentAdView;
    private NativeAppInstallAdView nativeAppInstallAdView;
    /**
     * Are ads currently displayed
     */
    private boolean adsDisplayed = false;
    @NonNull
    private final BroadcastReceiver newPurchaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (adsDisplayed) {
                adsDisplayed = false;
                adapter.setUpElements();
                adapter.notifyItemRemoved(0);
            }
        }
    };
    @NonNull
    private final BroadcastReceiver carUpdatedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            retrieveCarsFromDB();

            adapter.setUpElements();
            adapter.notifyDataSetChanged();
        }

    };
    private int bottomMargin;
    private BillingFragment billingFragment;

    @Nullable
    private BroadcastReceiver billingReadyReceiver;

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

        RelativeLayout mDrawerListView = (RelativeLayout) inflater.inflate(
                R.layout.fragment_navigation_drawer,
                container,
                false);

        userDetailsView = mDrawerListView.findViewById(R.id.user_details);

        userImage = (ImageView) mDrawerListView.findViewById(R.id.profile_image);
        usernameTextView = (TextView) mDrawerListView.findViewById(R.id.username);
        emailTextView = (TextView) mDrawerListView.findViewById(R.id.email);

        /**
         * RecyclerView
         */
        RecyclerView recyclerView = (RecyclerView) mDrawerListView.findViewById(R.id.recycler_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);


        adapter = new RecyclerViewDrawerAdapter();
        adapter.setUpElements();
        recyclerView.setAdapter(adapter);

        // this call is actually only necessary with custom ItemAnimators
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        nativeContentAdView = (NativeContentAdView) inflater.inflate(R.layout.native_content_ad_view, container, false);
        nativeAppInstallAdView = (NativeAppInstallAdView) inflater.inflate(R.layout.native_app_install_ad_view, container, false);

        return mDrawerListView;
    }

    @Override
    public void onStart() {
        super.onStart();

        retrieveCarsFromDB();

        adapter.setUpElements();
        adapter.notifyDataSetChanged();

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
            Log.d(TAG, "Waiting for billing service");
            billingReadyReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Billing ready");
                    setUpAd();
                }
            };
            getActivity().registerReceiver(billingReadyReceiver, new IntentFilter(Constants.INTENT_BILLING_READY));
        }

        getActivity().registerReceiver(newPurchaseReceiver, new IntentFilter(Constants.INTENT_ADS_REMOVED));
        getActivity().registerReceiver(userInfoReceiver, new IntentFilter(Constants.INTENT_USER_INFO_UPDATE));

    }

    private void retrieveCarsFromDB() {
        cars = CarDatabase.getInstance(getActivity()).retrieveCars(true);

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
        if (billingReadyReceiver != null)
            getActivity().unregisterReceiver(billingReadyReceiver);
        billingReadyReceiver = null;
        getActivity().unregisterReceiver(newPurchaseReceiver);
        getActivity().unregisterReceiver(userInfoReceiver);
    }

    private void setUpAd() {

        if (!ADS_ENABLED) return;

        if (PreferencesUtil.isAdsRemoved(getActivity())) return;

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                /**
                 * Check if the user has purchases. If there is an error we don't display just in case
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
                    // Ad request
                    AdRequest adRequest = new AdRequest.Builder().setLocation(mLastUserLocation).build();

                    AdLoader adLoader = new AdLoader.Builder(getActivity(), "ca-app-pub-3940256099942544/2247696110") // TODO: replace with out ad unit id. This is test
                            .forAppInstallAd(NavigationDrawerFragment.this)
                            .forContentAd(NavigationDrawerFragment.this)
                            .withAdListener(new AdListener() {
                                @Override
                                public void onAdFailedToLoad(int errorCode) {
                                    // Handle the failure by logging, altering the UI, etc.
                                }
                            })
                            .withNativeAdOptions(new NativeAdOptions.Builder().build())
                            .build();
                    adLoader.loadAd(adRequest);

                    if (!adsDisplayed) {
                    }
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

        userDetailsView.setVisibility(skippedLogin ? View.GONE : View.VISIBLE);

        if (skippedLogin)
            return;

        String loggedUsername = AuthUtils.getLoggedUsername(getActivity());

        // in case it is being loaded in the background
        if (loggedUsername == null)
            return;

        usernameTextView.setText(loggedUsername);
        emailTextView.setText(AuthUtils.getEmail(getActivity()));
        String profilePicURL = AuthUtils.getProfilePicURL(getActivity());
        if (profilePicURL != null)
            new LoadImageTask(userImage).execute(profilePicURL);

    }

    public void setBottomMargin(int bottomMargin) {
        this.bottomMargin = bottomMargin;
    }

    @Override
    public void onAppInstallAdLoaded(NativeAppInstallAd nativeAppInstallAd) {
        // Show the app install ad.
        currentAdView = nativeAppInstallAdView;

        ImageView logo = (ImageView) nativeAppInstallAdView.findViewById(R.id.icon);
        logo.setImageDrawable(nativeAppInstallAd.getIcon().getDrawable());
        nativeAppInstallAdView.setIconView(logo);

        Button callToActionButton = (Button) nativeAppInstallAdView.findViewById(R.id.call_to_action);
        callToActionButton.setText(nativeAppInstallAd.getCallToAction());
        nativeAppInstallAdView.setCallToActionView(callToActionButton);

        TextView headlineView = (TextView) nativeAppInstallAdView.findViewById(R.id.headline);
        headlineView.setText(nativeAppInstallAd.getHeadline());
        nativeAppInstallAdView.setHeadlineView(headlineView);

        adsDisplayed = true;
        adapter.setUpElements();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onContentAdLoaded(NativeContentAd nativeContentAd) {

        // Show the content ad.
        currentAdView = nativeContentAdView;

        ImageView logo = (ImageView) nativeContentAdView.findViewById(R.id.logo);
        logo.setImageDrawable(nativeContentAd.getImages().get(0).getDrawable());
        nativeContentAdView.setLogoView(logo);

        Button callToActionButton = (Button) nativeContentAdView.findViewById(R.id.call_to_action);
        callToActionButton.setText(nativeContentAd.getCallToAction());
        nativeContentAdView.setCallToActionView(callToActionButton);

        TextView headlineView = (TextView) nativeContentAdView.findViewById(R.id.headline);
        headlineView.setText(nativeContentAd.getHeadline());
        nativeContentAdView.setHeadlineView(headlineView);

        adsDisplayed = true;
        adapter.setUpElements();
        adapter.notifyDataSetChanged();
    }

    public class RecyclerViewDrawerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int AD_TYPE = -1;
        public static final int CAR_TYPE = 0;
        public static final int CAR_MANAGER_TYPE = 1;
        public static final int SHARE_TYPE = 2;
        public static final int DONATE_TYPE = 3;
        public static final int PREFERENCES_TYPE = 4;
        public static final int HELP_TYPE = 5;
        public static final int SIGN_OUT_TYPE = 6;

        // each entry represents an item in the drawer
        private List<Integer> itemTypes;

        public void setUpElements() {

            int totalElements = cars.size() + 5;

            if (adsDisplayed) totalElements++;
            if (AppInviteDialog.canShow()) totalElements++;

            itemTypes = new ArrayList(totalElements);

            if (adsDisplayed)
                itemTypes.add(AD_TYPE);

            for (int i = 0; i < cars.size(); i++) {
                itemTypes.add(CAR_TYPE);
            }

            itemTypes.add(CAR_MANAGER_TYPE);
            if (AppInviteDialog.canShow())
                itemTypes.add(SHARE_TYPE);
            itemTypes.add(DONATE_TYPE);
            itemTypes.add(PREFERENCES_TYPE);
            itemTypes.add(HELP_TYPE);
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
             * Ad
             */
            if (viewType == AD_TYPE) {
                return new AdViewHolder(currentAdView);
            }
            /**
             * Car
             */
            else if (viewType == CAR_TYPE) {
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

            throw new IllegalStateException("New type added to the recycler view but no view holder associated : " + viewType);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

            int viewType = getItemViewType(position);

            /**
             * Ad
             */
            if (viewType == AD_TYPE) {

            }
            /**
             * Car
             */
            else if (viewType == CAR_TYPE) {

                CarViewHolder carViewHolder = (CarViewHolder) viewHolder;

                final Car car = cars.get(position - (adsDisplayed ? 1 : 0));
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
                            Util.createUpperToast(getActivity(), R.string.position_not_set, Toast.LENGTH_SHORT);
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
            } else if (viewType == SHARE_TYPE) {
                bindShare((MenuViewHolder) viewHolder);
            } else if (viewType == DONATE_TYPE) {
                bindDonate((MenuViewHolder) viewHolder);
            } else if (viewType == PREFERENCES_TYPE) {
                bindHelp((MenuViewHolder) viewHolder);
            } else if (viewType == HELP_TYPE) {
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

        private void bindShare(@NonNull MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.share);
            menuViewHolder.subtitle.setText(R.string.and_remove_ads);
            menuViewHolder.subtitle.setVisibility(View.VISIBLE);
            menuViewHolder.icon.setImageResource(R.drawable.ic_facebook);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.openShareDialog();
                    Tracking.sendEvent(Tracking.CATEGORY_NAVIGATION_DRAWER, Tracking.ACTION_FACEBOOK_INVITE_CLICK);
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

        private void bindHelp(@NonNull MenuViewHolder menuViewHolder) {
            menuViewHolder.title.setText(R.string.help);
            menuViewHolder.icon.setImageResource(R.drawable.ic_help_circle);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.goToTutorial();
                    Tracking.sendEvent(Tracking.CATEGORY_NAVIGATION_DRAWER, Tracking.ACTION_HELP_CLICK);
                }
            });
        }

        private void bindSignOut(@NonNull MenuViewHolder menuViewHolder) {
            menuViewHolder.itemView.setPadding(0, 0, 0, bottomMargin);
            menuViewHolder.title.setText(skippedLogin ? R.string.sign_in : R.string.disconnect);
            menuViewHolder.icon.setImageResource(R.drawable.ic_logout_grey600_24dp);
            menuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigation.signOut();
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

        public class AdViewHolder extends RecyclerView.ViewHolder {
            public AdViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }


}
