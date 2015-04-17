package com.cahue.iweco;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
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

import com.android.vending.billing.IInAppBillingService;
import com.cahue.iweco.activityRecognition.ActivityRecognitionUtil;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarManagerActivity;
import com.cahue.iweco.cars.CarManagerFragment;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.EditCarDialog;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.debug.DebugActivity;
import com.cahue.iweco.locationServices.CarMovedService;
import com.cahue.iweco.locationServices.LocationPollerService;
import com.cahue.iweco.locationServices.ParkedCarService;
import com.cahue.iweco.login.AuthUtils;
import com.cahue.iweco.login.LoginActivity;
import com.cahue.iweco.parkedCar.CarDetailsFragment;
import com.cahue.iweco.parkedCar.ParkedCarDelegate;
import com.cahue.iweco.parkedCar.SetCarPositionDialog;
import com.cahue.iweco.spots.ParkingSpot;
import com.cahue.iweco.spots.ParkingSpotSender;
import com.cahue.iweco.spots.SpotDetailsFragment;
import com.cahue.iweco.spots.SpotsDelegate;
import com.cahue.iweco.tutorial.TutorialActivity;
import com.cahue.iweco.util.Util;
import com.google.android.gms.location.DetectedActivity;
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
import com.melnykov.fab.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends BaseActivity
        implements
        LocationListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        SpotsDelegate.SpotSelectedListener,
        CarDetailsFragment.OnCarPositionDeletedListener,
        SetCarPositionDialog.Callbacks,
        CarManagerFragment.Callbacks,
        EditCarDialog.CarEditedListener,
        CameraManager,
        OnMapReadyCallback,
        CameraUpdateRequester,
        OnCarClickedListener,
        Navigation {

    protected static final String TAG = "Maps";

    static final String DETAILS_FRAGMENT_TAG = "DETAILS_FRAGMENT";

    static final int REQUEST_ON_PURCHASE = 1001;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    List<AbstractMarkerDelegate> delegates = new ArrayList();

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private AccountManager mAccountManager;

    private CarDatabase carDatabase;

    private Toolbar mToolbar;

    private FloatingActionButton myLocationButton;
    private CardView detailsContainer;
    private DetailsFragment detailsFragment;
    private boolean detailsDisplayed = false;

    private IInAppBillingService iInAppBillingService;

    private boolean cameraFollowing;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Currently recognized activity type (what the user is doing)
     */
//    private DetectedActivity activityType;

    /**
     * If we get a new car position while we are using the app, we update the map
     */
    private final BroadcastReceiver activityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

//            activityType = (DetectedActivity) intent.getExtras().get(ActivityRecognitionUtil.INTENT_EXTRA_ACTIVITY);

        }
    };

    /**
     * Current logged user
     */
    private Account mAccount;

    /**
     * If we get a new car position while we are using the app, we update the map
     */
    private final BroadcastReceiver carUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String carId = intent.getExtras().getString(Constants.INTENT_CAR_EXTRA_ID);
            if (carId != null) {
                Log.i(TAG, "Car update received: " + carId);
                getParkedCarDelegate(carId).update();
            }

        }
    };

    private ServiceConnection mBillingServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            iInAppBillingService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iInAppBillingService = IInAppBillingService.Stub.asInterface(service);
        }
    };

//    private PendingIntent pActivityRecognitionIntent;

    @Override
    protected void onPlusClientSignIn() {

        LocationServices.FusedLocationApi.requestLocationUpdates(getGoogleApiClient(),
                REQUEST,
                this);

//        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
//        pActivityRecognitionIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(getGoogleApiClient(), 5000, pActivityRecognitionIntent);

    }

    @Override
    protected void onPlusClientSignOut() {
        goToLogin();
    }

    public void goToLogin() {
        if (!isFinishing()) {

            if (!isSkippedLogin())
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
    protected void onConnectingStatusChange(boolean connecting) {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        carDatabase = CarDatabase.getInstance(this);

        mAccountManager = AccountManager.get(this);
        final Account[] availableAccounts = mAccountManager.getAccountsByType(getString(R.string.account_type));

        if (availableAccounts.length == 0 && !isSkippedLogin()) {
            goToLogin();
            return;
        }
        // There should be just one account
        else if (availableAccounts.length > 1) {
            Log.w(TAG, "Multiple accounts found");
        }

        if (availableAccounts.length > 0)
            mAccount = availableAccounts[0];

        // show help dialog only on first run of the app
        if (!Util.isTutorialShown(this)) {
            goToTutorial();
        }

        setContentView(R.layout.activity_main);

//        activityType = ActivityRecognitionUtil.getLastDetectedActivity(this);

        if (BuildConfig.DEBUG) {
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

        /**
         * Bind service used for donations
         */
        bindBillingService();

    }

    private void setUpNavigationDrawer() {

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setRetainInstance(true);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                mToolbar);

    }

    private boolean mapInitialised = false;

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
            CameraUpdate update = Util.getLastCameraPosition(this);
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

    private void showDetails() {

        detailsContainer.setVisibility(View.VISIBLE);

        detailsContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                final int height = detailsContainer.getMeasuredHeight();

                TranslateAnimation animation = new TranslateAnimation(0, 0, detailsDisplayed ? 0 : height, 0);
                int mediumAnimTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
                animation.setDuration(mediumAnimTime);
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
                        setMapPadding(height);
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

    private void setMapPadding(int bottomPadding) {
        mMap.setPadding(0, Util.getActionBarSize(MapsActivity.this), 0, bottomPadding);
    }

    private void hideDetails() {

        if (!detailsDisplayed) return;

        detailsDisplayed = false;

        setMapPadding(0);

        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, detailsContainer.getHeight());
        int mediumAnimTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        animation.setDuration(mediumAnimTime);
        animation.setInterpolator(MapsActivity.this, R.anim.my_decelerate_interpolator);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setMapPadding(0);
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
    public void onBackPressed() {

        if (mNavigationDrawerFragment.isDrawerOpen()) {
            mNavigationDrawerFragment.closeDrawers();
            return;
        }

        if (detailsDisplayed) {
            for (AbstractMarkerDelegate delegate : delegates) delegate.onDetailsClosed();
            hideDetails();
        } else {
            super.onBackPressed();
        }
    }


    @Override
    protected void onResume() {

        super.onResume();

        // when our activity resumes, we want to register for car updates
        registerReceiver(carUpdateReceiver, new IntentFilter(Constants.INTENT_CAR_UPDATE));

//        List<String> carIds = carDatabase.getCarIds();
//        for (String id : carIds) {
//            getParkedCarDelegate(id).update();
//        }

        setInitialCamera();

    }

    private void bindBillingService() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mBillingServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // element purchase
        if (requestCode == REQUEST_ON_PURCHASE) {

            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    Util.createUpperToast(this, getString(R.string.thanks), Toast.LENGTH_LONG); // do string
                } catch (JSONException e) {
                    Util.createUpperToast(this, "Failed to parse purchase data.", Toast.LENGTH_LONG);
                    e.printStackTrace();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSignInRequired() {
        Log.d(TAG, "onSignInRequired");
        goToLogin();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (iInAppBillingService != null) {
            unbindService(mBillingServiceConn);
        }

        saveMapCameraPosition();
    }

    private void saveMapCameraPosition() {
        if (mMap != null) {
            Util.saveCameraPosition(this, mMap.getCameraPosition());
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

    @Override
    protected boolean autoConnect() {
        return true;
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

    public void goToTutorial() {
        startActivity(new Intent(this, TutorialActivity.class));
        Util.setTutorialShown(this, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (getGoogleApiClient() != null && getGoogleApiClient().isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(getGoogleApiClient(), this);
//            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(getGoogleApiClient(), pActivityRecognitionIntent);
        }
        unregisterReceiver(carUpdateReceiver);
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
        mMap.setPadding(0, Util.getActionBarSize(this), 0, 0);
    }


    private void setUpMapListeners() {
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(this);
    }


    public void openDonationDialog() {
        DonateDialog dialog = new DonateDialog();
        dialog.setIInAppBillingService(iInAppBillingService);
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


    private boolean initialCameraSet = false;

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
                onCarClicked(car.id);
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

        SetCarPositionDialog dialog = SetCarPositionDialog.newInstance(location);
        dialog.show(getFragmentManager(), "SetCarPositionDialog");

        hideDetails();

    }

    private LatLng getUserLatLng() {
        Location userLastLocation = getUserLocation();
        if (userLastLocation == null) return null;
        return new LatLng(userLastLocation.getLatitude(), userLastLocation.getLongitude());
    }

    private Location getUserLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(getGoogleApiClient());
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        if (mMap == null) return;

        if (cameraUpdateRequester != this)
            setCameraFollowing(false);

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onCameraChange(cameraPosition, cameraUpdateRequester);
        }

        if (detailsFragment != null && detailsFragment.isResumed())
            detailsFragment.onCameraUpdate();

        cameraUpdateRequester = null;

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onMarkerClick(marker);
        }
        return false;
    }


    @Override
    public void onMapClick(LatLng point) {

    }


    private void goToDebug() {
        startActivity(new Intent(this, DebugActivity.class));
    }


    @Override
    public void onSpotClicked(ParkingSpot spot) {
        setDetailsFragment(SpotDetailsFragment.newInstance(spot, getUserLocation()));
    }


    /**
     * Set the details fragment
     *
     * @param fragment
     */
    private void setDetailsFragment(DetailsFragment fragment) {

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
        getParkedCarDelegate(carId).setFollowing(true);
        setDetailsFragment(CarDetailsFragment.newInstance(carId)); // TODO
    }


    private CameraUpdateRequester cameraUpdateRequester;

    @Override
    public void onCameraUpdateRequest(CameraUpdate cameraUpdate, final CameraUpdateRequester cameraUpdateRequester) {

        if (mMap == null) return;

        mMap.animateCamera(cameraUpdate,
                new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        MapsActivity.this.cameraUpdateRequester = cameraUpdateRequester;
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    private void setCameraFollowing(boolean cameraFollowing) {
        this.cameraFollowing = cameraFollowing;
        if (cameraFollowing) {
            zoomToMyLocation();
            myLocationButton.setImageResource(R.drawable.ic_action_maps_my_location_accent);
        } else {
            myLocationButton.setImageResource(R.drawable.ic_action_maps_my_location);
        }

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
        onCarClicked(carId);
    }

    @Override
    public void devicesBeingLoaded(boolean loading) {
        // Do nothing
    }

    @Override
    public void onCarEdited(Car car, boolean newCar) {
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {
    }

    /**
     * Only for debugging
     */
    private void setDebugConfig() {

        View debugLayout = findViewById(R.id.debug_layout);
        if (debugLayout == null) return;
        debugLayout.setVisibility(View.VISIBLE);

        Button refresh = (Button) findViewById(R.id.refresh);
        refresh.setVisibility(View.VISIBLE);
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

        Button carParked = (Button) findViewById(R.id.park);
        carParked.setVisibility(View.VISIBLE);
        carParked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, ParkedCarService.class);
                intent.putExtra(LocationPollerService.EXTRA_CAR, carDatabase.retrieveCars(false).iterator().next().id);
                startService(intent);
            }
        });

        Button carMoved = (Button) findViewById(R.id.driveOff);
        carMoved.setVisibility(View.VISIBLE);
        carMoved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, CarMovedService.class);
                Car car = carDatabase.retrieveCars(false).iterator().next();
                intent.putExtra(LocationPollerService.EXTRA_CAR, car.id);
                startService(intent);
            }
        });

        Button approachingCar = (Button) findViewById(R.id.approaching);
        approachingCar.setVisibility(View.VISIBLE);
        approachingCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Car car = carDatabase.retrieveCars(false).iterator().next();
                ParkingSpotSender.doPostSpotLocation(MapsActivity.this, car.location, true, car);
            }
        });
    }

}
