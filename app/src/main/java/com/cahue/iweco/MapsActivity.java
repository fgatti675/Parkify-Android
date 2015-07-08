package com.cahue.iweco;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cahue.iweco.activityRecognition.ParkedCarRequestedService;
import com.cahue.iweco.auth.Authenticator;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarManagerActivity;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.locationServices.CarMovedService;
import com.cahue.iweco.login.AuthUtils;
import com.cahue.iweco.login.LoginActivity;
import com.cahue.iweco.login.LoginType;
import com.cahue.iweco.parkedCar.CarDetailsFragment;
import com.cahue.iweco.parkedCar.ParkedCarService;
import com.cahue.iweco.setCarLocation.SetCarLocationDelegate;
import com.cahue.iweco.setCarLocation.SetCarPositionDialog;
import com.cahue.iweco.spots.ParkingSpot;
import com.cahue.iweco.spots.ParkingSpotSender;
import com.cahue.iweco.tutorial.TutorialActivity;
import com.cahue.iweco.util.FacebookAppInvitesDialog;
import com.cahue.iweco.util.IwecoPromoDialog;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.UninstallWIMCDialog;
import com.cahue.iweco.util.Util;
import com.facebook.login.LoginManager;
import com.facebook.share.widget.AppInviteDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.plus.Plus;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MapsActivity extends AppCompatActivity
        implements
        LocationListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        SpotsDelegate.SpotSelectedListener,
        CarDetailsFragment.OnCarPositionDeletedListener,
        SetCarPositionDialog.Callbacks,
        CameraManager,
        OnMapReadyCallback,
        CameraUpdateRequester,
        OnCarClickedListener,
        Navigation,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DetailsViewManager {

    protected static final String TAG = MapsActivity.class.getSimpleName();

    static final String DETAILS_FRAGMENT_TAG = "DETAILS_FRAGMENT";
    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(2000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    /**
     * If we get a new car position while we are using the app, we update the map
     */
    private final BroadcastReceiver carUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String carId = intent.getExtras().getString(Constants.INTENT_CAR_EXTRA_ID);
            if (carId != null) {
                Log.i(TAG, "Car update received: " + carId);
                getParkedCarDelegate(carId).update(true);
            }

        }
    };
    List<AbstractMarkerDelegate> delegates = new ArrayList();
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    // This is the helper object that connects to Google Play Services.
    private GoogleApiClient mGoogleApiClient;
    private AccountManager mAccountManager;
    private CarDatabase carDatabase;
    private Toolbar mToolbar;
    private FloatingActionButton myLocationButton;
    private CardView detailsContainer;
    private DetailsFragment detailsFragment;
    private boolean detailsDisplayed = false;
    private boolean cameraFollowing;

    /**
     * Currently recognized activity type (what the user is doing)
     */
//    private DetectedActivity activityType;
    /**
     * The user didn't log in, but we still love him
     */
    private boolean mSkippedLogin;

    private LoginType loginType;
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    /**
     * Current logged user
     */
    private Account mAccount;
    private boolean mapInitialised = false;
    private boolean initialCameraSet = false;
    /**
     * Last component to request a camera update
     */
    private CameraUpdateRequester lastCameraUpdateRequester;
    /**
     *
     */
    private Set<CameraUpdateRequester> cameraUpdateRequesterList = new LinkedHashSet<>();

    public void goToLogin() {
        if (!isFinishing()) {

            if (!mSkippedLogin)
                carDatabase.clearCars();

            AuthUtils.setSkippedLogin(this, false);

            clearAccounts();
            Log.d(TAG, "goToLogin");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void clearAccounts() {
        for (Account account : mAccountManager.getAccountsByType(getString(R.string.account_type)))
            mAccountManager.removeAccount(account, null, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mSkippedLogin = AuthUtils.isSkippedLogin(this);

        /**
         * Bind service used for donations
         */
        setUpBillingFragment();

        carDatabase = CarDatabase.getInstance(this);

        mAccountManager = AccountManager.get(this);
        final Account[] availableAccounts = mAccountManager.getAccountsByType(getString(R.string.account_type));

        if (availableAccounts.length == 0 && !mSkippedLogin) {
            goToLogin();
            return;
        }
        // There should be just one account
        else if (availableAccounts.length > 1) {
            Log.w(TAG, "Multiple accounts found");
        }

        loginType = LoginType.Google;

        if (availableAccounts.length > 0) {
            mAccount = availableAccounts[0];

            String userId = mAccountManager.getUserData(mAccount, Authenticator.USER_ID);
            ((IwecoApp) getApplication()).setTrackerUserId(userId);

            String typeString = mAccountManager.getUserData(mAccount, Authenticator.LOGIN_TYPE);
            if (typeString != null)
                loginType = LoginType.valueOf(typeString);
        }

        // Create a GoogleApiClient instance
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this);

        if (!mSkippedLogin && loginType == LoginType.Google) {
            builder.addApi(Plus.API)
                    .addScope(Plus.SCOPE_PLUS_LOGIN)
                    .addScope(Plus.SCOPE_PLUS_PROFILE)
                    .addScope(new Scope("https://www.googleapis.com/auth/userinfo.email"));
        }

        mGoogleApiClient = builder.build();

        // show help dialog only on first run of the app
        if (!PreferencesUtil.isTutorialShown(this)) {
            goToTutorial();
        }

        setContentView(R.layout.activity_main);

        if (BuildConfig.DEBUG) {
            setDebugConfig();
            setDebugConfig();
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        ViewCompat.setElevation(mToolbar, getResources().getDimension(R.dimen.elevation));
        if ("wimc".equals(BuildConfig.FLAVOR)) {
            mToolbar.removeView(findViewById(R.id.logo));
            mToolbar.setTitle(getString(R.string.app_name));
        }


        setUpNavigationDrawer();

        myLocationButton = (FloatingActionButton) findViewById(R.id.my_location);
        ViewCompat.setElevation(myLocationButton, getResources().getDimension(R.dimen.elevation));
        myLocationButton.setBackgroundTintList(getResources().getColorStateList(R.color.button_states));
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCameraFollowing(true);
            }
        });

        /**
         * Try to reuse map
         */
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        /**
         * There is no saved instance so we create a few things
         */
        if (savedInstanceState == null) {

            // First incarnation of this activity.
            mapFragment.setRetainInstance(true);

        }

        /**
         *
         */
        else {

            initialCameraSet = savedInstanceState.getBoolean("initialCameraSet");
            mapInitialised = savedInstanceState.getBoolean("mapInitialised");
            detailsDisplayed = savedInstanceState.getBoolean("detailsDisplayed");
            cameraFollowing = savedInstanceState.getBoolean("cameraFollowing");

        }

        /**
         * Add delegates
         */
        if (!"wimc".equals(BuildConfig.FLAVOR))
            delegates.add(getSpotsDelegate());

        delegates.add(getSetCarLocationDelegate());

        List<String> carIds = carDatabase.getCarIds();
        for (String id : carIds) {
            delegates.add(getParkedCarDelegate(id));
        }

        /**
         * Details
         */
        detailsFragment = (DetailsFragment) getFragmentManager().findFragmentByTag(DETAILS_FRAGMENT_TAG);
        detailsContainer = (CardView) findViewById(R.id.marker_details_container);
        detailsContainer.setVisibility(detailsDisplayed ? View.VISIBLE : View.INVISIBLE);


        registerCameraUpdater(this);

        checkIweco();
        checkWIMC();

        showFacebookAppInvite();

        handleIntent();

    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra(Constants.INTENT_CAR_EXTRA_UPDATE_REQUEST)) {
            Location location = intent.getParcelableExtra(Constants.INTENT_CAR_EXTRA_LOCATION);
            Date time = new Date(intent.getLongExtra(Constants.INTENT_CAR_EXTRA_TIME, System.currentTimeMillis()));
            String address = intent.getStringExtra(Constants.INTENT_CAR_EXTRA_ADDRESS);
            getSetCarLocationDelegate().setRequestLocation(location, time, address);
        }
    }

    public void checkIweco() {
        if ("wimc".equals(BuildConfig.FLAVOR)) {

            if (Util.isPackageInstalled("com.cahue.iweco", this)) {
                if (!PreferencesUtil.isWIMCUninstallDialogShown(this)) {
                    UninstallWIMCDialog dialog = new UninstallWIMCDialog();
                    dialog.show(getFragmentManager(), "UninstallWIMCDialog");
                }
            } else {
                if (IwecoPromoDialog.shouldBeShown(this)) {
                    IwecoPromoDialog dialog = new IwecoPromoDialog();
                    dialog.show(getFragmentManager(), "IwecoPromoDialog");
                }
            }

        }
    }

    private void checkWIMC() {
        if ("iweco".equals(BuildConfig.FLAVOR)
                && Util.isPackageInstalled("com.whereismycar", this)
                && !PreferencesUtil.isWIMCUninstallDialogShown(this)) {
            UninstallWIMCDialog dialog = new UninstallWIMCDialog();
            dialog.show(getFragmentManager(), "UninstallWIMCDialog");
        }
    }

    private void setUpBillingFragment() {
        Log.d(TAG, "Creating new BillingFragment");
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        BillingFragment spotsDelegate = BillingFragment.newInstance();
        spotsDelegate.setRetainInstance(true);
        transaction.add(spotsDelegate, BillingFragment.FRAGMENT_TAG);
        transaction.commit();
    }

    private void setUpNavigationDrawer() {

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setRetainInstance(true);

        // Set up the drawer.
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            mNavigationDrawerFragment.setUpDrawer(
                    R.id.navigation_drawer,
                    drawerLayout,
                    mToolbar);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG, "onMapReady");

        mMap = googleMap;

        setUpMap();
        setUpMapListeners();

        /**
         * Initial zoom
         */
        if (!mapInitialised) {
            CameraUpdate update = PreferencesUtil.getLastCameraPosition(this);
            if (update != null)
                mMap.moveCamera(update);
            mapInitialised = true;
        }

        /**
         * Do everything again
         */
        mMap.clear();

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onMapReady(mMap);
        }


        if (detailsDisplayed) showDetails();
        else hideDetails();

    }

    private SpotsDelegate getSpotsDelegate() {
        SpotsDelegate spotsDelegate = (SpotsDelegate) getFragmentManager().findFragmentByTag(SpotsDelegate.FRAGMENT_TAG);
        if (spotsDelegate == null) {
            Log.d(TAG, "Creating new ParkedCarDelegate");
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            spotsDelegate = SpotsDelegate.newInstance();
            spotsDelegate.setRetainInstance(true);
            transaction.add(spotsDelegate, SpotsDelegate.FRAGMENT_TAG);
            transaction.commit();
        }
        return spotsDelegate;
    }

    /**
     * @param carId
     * @return
     */
    private ParkedCarDelegate getParkedCarDelegate(String carId) {
        ParkedCarDelegate parkedCarDelegate = (ParkedCarDelegate) getFragmentManager().findFragmentByTag(carId);
        if (parkedCarDelegate == null) {
            Log.d(TAG, "Creating new ParkedCarDelegate");
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            parkedCarDelegate = ParkedCarDelegate.newInstance(carId);
            parkedCarDelegate.setRetainInstance(true);
            transaction.add(parkedCarDelegate, carId);
            transaction.commit();
        }
        return parkedCarDelegate;
    }

    /**
     * @return
     */
    private SetCarLocationDelegate getSetCarLocationDelegate() {
        SetCarLocationDelegate setCarLocationDelegate = (SetCarLocationDelegate) getFragmentManager().findFragmentByTag(SetCarLocationDelegate.FRAGMENT_TAG);
        if (setCarLocationDelegate == null) {
            Log.d(TAG, "Creating new SetCarLocationDelegate");
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            setCarLocationDelegate = SetCarLocationDelegate.newInstance();
            setCarLocationDelegate.setRetainInstance(true);
            transaction.add(setCarLocationDelegate, SetCarLocationDelegate.FRAGMENT_TAG);
            transaction.commit();
        }
        return setCarLocationDelegate;
    }

    private void showDetails() {

        detailsContainer.setVisibility(View.VISIBLE);

        detailsContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                final int height = detailsContainer.getMeasuredHeight();

                TranslateAnimation animation = new TranslateAnimation(0, 0, detailsDisplayed ? 0 : height, 0);
                int mediumAnimTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
                animation.setDuration(mediumAnimTime);

                setMapPaddingAndNotify(height);

                animation.setInterpolator(MapsActivity.this, R.anim.my_decelerate_interpolator);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) myLocationButton.getLayoutParams();
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                        myLocationButton.setLayoutParams(params); //causes layout update
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                detailsContainer.startAnimation(animation);
                myLocationButton.startAnimation(animation);
                detailsDisplayed = true;

                detailsContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });


    }

    private void setMapPaddingAndNotify(int bottomPadding) {
        mMap.setPadding(0, Util.getActionBarSize(MapsActivity.this), 0, bottomPadding);
        for (CameraUpdateRequester requester : cameraUpdateRequesterList)
            requester.onMapResized();
    }

    @Override
    public void hideDetails() {

        if (!detailsDisplayed) return;

        detailsDisplayed = false;

        setMapPaddingAndNotify(0);

        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, detailsContainer.getHeight());
        int mediumAnimTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        animation.setDuration(mediumAnimTime);
        animation.setInterpolator(MapsActivity.this, R.anim.my_decelerate_interpolator);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setMapPaddingAndNotify(0);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                detailsContainer.setVisibility(View.INVISIBLE);
                detailsContainer.removeAllViews();

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) myLocationButton.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                myLocationButton.setLayoutParams(params); //causes layout update

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        detailsContainer.startAnimation(animation);
        myLocationButton.startAnimation(animation);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                REQUEST,
                this);
        Log.w(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.w(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.w(TAG, "onConnectionFailed");
        goToLogin();
    }

    @Override
    public void onBackPressed() {

        if (mNavigationDrawerFragment.isDrawerOpen()) {
            mNavigationDrawerFragment.closeDrawers();
            return;
        }

        if (detailsDisplayed) {
            for (AbstractMarkerDelegate delegate : delegates) {
                delegate.onDetailsClosed();
                delegate.setCameraFollowing(false);
            }
            hideDetails();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        // when our activity resumes, we want to register for car updates
        registerReceiver(carUpdateReceiver, new IntentFilter(Constants.INTENT_CAR_UPDATED));

        setInitialCamera();

        alertIfNoCars();

    }

    private void alertIfNoCars() {

        if (carDatabase.isEmpty()) {
            Snackbar.make(findViewById(R.id.main_container), R.string.nocars, Snackbar.LENGTH_LONG)
                    .setAction(R.string.create, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            goToCarManager();
                        }
                    })
                    .show();
        }


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BillingFragment billingFragment = (BillingFragment) getFragmentManager().findFragmentByTag(BillingFragment.FRAGMENT_TAG);
        billingFragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        saveMapCameraPosition();
    }

    private void saveMapCameraPosition() {
        if (mMap != null) {
            PreferencesUtil.saveCameraPosition(this, mMap.getCameraPosition());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean("initialCameraSet", initialCameraSet);
        savedInstanceState.putBoolean("mapInitialised", mapInitialised);
        savedInstanceState.putBoolean("detailsDisplayed", detailsDisplayed);
        savedInstanceState.putBoolean("cameraFollowing", cameraFollowing);

    }

    /**
     * Checks whether the device currently has a network connection
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    /**
     * Sign out the user (so they can switch to another account).
     */
    public void signOut() {

        // We only want to sign out if we're connected.
        if (mGoogleApiClient.isConnected()) {

            // Clear the default account in order to allow the user to potentially choose a
            // different account from the account chooser.
            if (!mSkippedLogin && loginType == LoginType.Google) {
                Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
            }
        }

        // Facebook disconnect
        final LoginManager loginManager = LoginManager.getInstance();
        loginManager.logOut();

        ((IwecoApp) getApplication()).setTrackerUserId(null);

        PreferencesUtil.clear(this);
        AuthUtils.clearLoggedUserDetails(this);

        Log.v(TAG, "Sign out successful!");

        goToLogin();
    }

    public void goToTutorial() {
        startActivity(new Intent(this, TutorialActivity.class));
        PreferencesUtil.setTutorialShown(this, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        unregisterReceiver(carUpdateReceiver);

        saveMapCameraPosition();
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
    }

    private void setUpMapListeners() {
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(this);
    }

    public void openShareDialog() {
        AppInviteDialog.show(this, FacebookAppInvitesDialog.getFacebookAppInvites(this));
    }

    public void openDonationDialog() {
        DonateDialog dialog = new DonateDialog();
        dialog.show(getFragmentManager(), "DonateDialog");
    }

    /**
     * Method used to start the pairing activity
     */
    @Override
    public void goToCarManager() {
        startActivity(new Intent(MapsActivity.this, CarManagerActivity.class));
    }

    /**
     * Implementation of {@link LocationListener}.
     */
    @Override
    public void onLocationChanged(Location location) {

        Log.v(TAG, "New location");

        if (mMap == null) return;

        if (cameraFollowing)
            zoomToMyLocation();

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onLocationChanged(location);
        }

        if (detailsFragment != null)
            detailsFragment.setUserLocation(location);

        /**
         * Set initial zoom level
         */
        setInitialCamera();
    }

    private void setInitialCamera() {

        if (initialCameraSet || isFinishing()) return;

        LatLng userPosition = getUserLatLng();
        if (userPosition != null) {

            final List<Car> parkedCars = carDatabase.retrieveCars(true);

            List<Car> closeCars = new ArrayList<>();
            for (Car car : parkedCars) {
                ParkedCarDelegate parkedCarDelegate = getParkedCarDelegate(car.id);
                parkedCarDelegate.onLocationChanged(getUserLocation());
                if (!parkedCarDelegate.isTooFar()) {
                    closeCars.add(car);
                }
            }

            // One parked car
            if (closeCars.size() == 1) {
                Car car = closeCars.get(0);
                getParkedCarDelegate(car.id).onCarClicked();
            }
            // zoom to user otherwise
            else {
                setCameraFollowing(cameraFollowing);
            }

            initialCameraSet = true;
        }

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d(TAG, "Long tap event " + latLng.latitude + " " + latLng.longitude);

        Location location = new Location(Util.TAPPED_PROVIDER);
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        location.setAccuracy(10);

        getSetCarLocationDelegate().setRequestLocation(location, new Date(), null);
//        SetCarPositionDialog dialog = SetCarPositionDialog.newInstance(location);
//        dialog.show(getFragmentManager(), "SetCarPositionDialog");


    }

    private LatLng getUserLatLng() {
        Location userLastLocation = getUserLocation();
        if (userLastLocation == null) return null;
        return new LatLng(userLastLocation.getLatitude(), userLastLocation.getLongitude());
    }

    private Location getUserLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    /**
     * This is called when the user moves the camera but also when it is triggered programatically.
     *
     * @param cameraPosition
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        if (mMap == null) return;

        if (lastCameraUpdateRequester != this)
            setCameraFollowing(false);

        for (AbstractMarkerDelegate delegate : delegates) {
            if (lastCameraUpdateRequester != delegate) {
                delegate.setCameraFollowing(false);
                delegate.onCameraChange(cameraPosition, lastCameraUpdateRequester);
            }
        }

        if (detailsFragment != null && detailsFragment.isResumed())
            detailsFragment.onCameraUpdate();

        lastCameraUpdateRequester = null;

        Log.v(TAG, "onCameraChange");

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        boolean consumeEvent = false;
        for (AbstractMarkerDelegate delegate : delegates) {
            if(delegate.onMarkerClick(marker))
                consumeEvent = true;
        }
        return consumeEvent;
    }

    @Override
    public void onMapClick(LatLng point) {

    }


    @Override
    public void onSpotClicked(ParkingSpot spot) {
    }

    @Override
    public DetailsFragment getDetailsFragment() {
        return detailsFragment;
    }

    /**
     * Set the details fragment
     *
     * @param fragment
     */
    @Override
    public void setDetailsFragment(DetailsFragment fragment) {

        if (isFinishing()) return;

        FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();

        if (detailsFragment != null)
            fragTransaction.remove(detailsFragment);

        detailsFragment = fragment;
        detailsFragment.setRetainInstance(true);
        detailsFragment.setUserLocation(getUserLocation());

        fragTransaction.add(detailsContainer.getId(), detailsFragment, DETAILS_FRAGMENT_TAG);
        fragTransaction.commitAllowingStateLoss();

        showDetails();
    }

    @Override
    public void onCarRemoved(String carId) {
        hideDetails();
    }

    @Override
    public void onCarClicked(String carId) {
    }

    @Override
    public void registerCameraUpdater(CameraUpdateRequester cameraUpdateRequester) {
        cameraUpdateRequesterList.add(cameraUpdateRequester);
    }

    @Override
    public void onCameraUpdateRequest(CameraUpdate cameraUpdate, final CameraUpdateRequester cameraUpdateRequester) {

        if (mMap == null) return;

        if (!cameraUpdateRequesterList.contains(cameraUpdateRequester))
            throw new IllegalStateException("Register this camera updater before requesting camera updates");

        // tell the rest not to perform more updates
        for (CameraUpdateRequester requester : cameraUpdateRequesterList)
            if (requester != cameraUpdateRequester) requester.setCameraFollowing(false);

        // perform animation
        mMap.animateCamera(cameraUpdate,
                new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        MapsActivity.this.lastCameraUpdateRequester = cameraUpdateRequester;
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    public void setCameraFollowing(boolean cameraFollowing) {
        this.cameraFollowing = cameraFollowing;
        if (cameraFollowing) {
            zoomToMyLocation();
            myLocationButton.setImageResource(R.drawable.crosshairs_gps_accent);
        } else {
            myLocationButton.setImageResource(R.drawable.crosshairs_gps);
        }

    }

    @Override
    public void onMapResized() {
        if (cameraFollowing) zoomToMyLocation();
    }

    private void zoomToMyLocation() {

        Log.d(TAG, "zoomToMyLocation");

        LatLng userPosition = getUserLatLng();
        if (userPosition == null) return;

        float zoom = Math.max(mMap.getCameraPosition().zoom, 15);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .zoom(zoom)
                .target(userPosition)
                .build());

        onCameraUpdateRequest(cameraUpdate, this);
    }

    /**
     * Called when the user sets the car manually
     */
    @Override
    public void onCarPositionUpdate(String carId) {
        getParkedCarDelegate(carId).onCarClicked();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {
    }

    private void showFacebookAppInvite() {

        if (!PreferencesUtil.isFacebookInvitesShown(this)
                && System.currentTimeMillis() - AuthUtils.getLoginDate(this) > 24 * 60 * 60 * 1000
                && AppInviteDialog.canShow()) {
            FacebookAppInvitesDialog dialog = new FacebookAppInvitesDialog();
            dialog.show(getFragmentManager(), "FacebookAppInvitesDialog");
            PreferencesUtil.setFacebookInvitesShown(this, true);
        }
    }

    /**
     * Only for debugging
     */
    private void setDebugConfig() {

        View debugLayout = findViewById(R.id.debug_layout);
        if (debugLayout == null) return;
        debugLayout.setVisibility(View.VISIBLE);


        Button refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!"wimc".equals(BuildConfig.FLAVOR))
                    getSpotsDelegate().queryCameraView();
                if (mAccount != null)
                    CarsSync.TriggerRefresh(MapsActivity.this, mAccount);
                else
                    Toast.makeText(MapsActivity.this, "Not logged in, so cannot perform refresh", Toast.LENGTH_SHORT);
            }
        });

        Button actRecog = (Button) findViewById(R.id.act_recog);
        actRecog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, ParkedCarRequestedService.class);
                startService(intent);
            }
        });

        Button carParked = (Button) findViewById(R.id.park);
        carParked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, ParkedCarService.class);
                intent.putExtra(Constants.INTENT_CAR_EXTRA_ID, carDatabase.retrieveCars(false).iterator().next().id);
                startService(intent);
            }
        });

        Button carMoved = (Button) findViewById(R.id.driveOff);
        carMoved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, CarMovedService.class);
                Car car = carDatabase.retrieveCars(false).iterator().next();
                intent.putExtra(Constants.INTENT_CAR_EXTRA_ID, car.id);
                startService(intent);
            }
        });

        Button approachingCar = (Button) findViewById(R.id.approaching);
        approachingCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Car car = carDatabase.retrieveCars(false).iterator().next();
                ParkingSpotSender.doPostSpotLocation(MapsActivity.this, car.location, true, car);
            }
        });
    }

}
