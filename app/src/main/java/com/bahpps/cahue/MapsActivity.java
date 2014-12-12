package com.bahpps.cahue;

import android.app.FragmentTransaction;
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
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ScrollView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.bahpps.cahue.parkedCar.Car;
import com.bahpps.cahue.parkedCar.CarLocationManager;
import com.bahpps.cahue.parkedCar.ParkedCarDelegate;
import com.bahpps.cahue.spots.ParkingSpot;
import com.bahpps.cahue.spots.SpotsDelegate;
import com.bahpps.cahue.util.Util;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends BaseActivity
        implements
        LocationListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        Toolbar.OnMenuItemClickListener,
        SpotsDelegate.SpotSelectedListener,
        ParkedCarDelegate.CarSelectedListener,
        CarDetailsFragment.OnCarPositionDeletedListener,
        CameraUpdateListener, OnMapReadyCallback {

    protected static final String TAG = "Maps";

    static final String DETAILS_FRAGMENT_TAG = "DETAILS_FRAGMENT";

    static final int REQUEST_ON_PURCHASE = 1001;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    List<AbstractMarkerDelegate> delegates = new ArrayList();
    private SpotsDelegate spotsDelegate;
    private Map<Car, ParkedCarDelegate> parkedCarDelegateMap;

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


    private View myLocationButton;
    private ScrollView detailsContainer;
    private DetailsFragment detailsFragment;
    private boolean detailsDisplayed = false;


    /**
     * If we get a new car position while we are using the app, we update the map
     */
    private final BroadcastReceiver carPosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Car car = (Car) intent.getExtras().get(CarLocationManager.INTENT_POSITION);
            if (car != null) {
                Log.i(TAG, "Location received: " + car);
                getParkedCarDelegate(car).setCarLocation(car);
            }

        }
    };

    private IInAppBillingService iInAppBillingService;

    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            iInAppBillingService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iInAppBillingService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    @Override
    protected void onPlusClientSignIn() {
        LocationServices.FusedLocationApi.requestLocationUpdates(getGoogleApiClient(),
                REQUEST,
                this);

        // call convenience method that zooms map on our location only on starting the app
        setInitialCamera();

    }

    @Override
    protected void onPlusClientSignOut() {
        goToLogin();
    }

    private void goToLogin() {
        if (!isFinishing()) {
            Log.d(TAG, "goToLogin");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    protected void onConnectingStatusChange(boolean connecting) {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        ViewCompat.setElevation(toolbar, 8);

        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(this);

        myLocationButton = findViewById(R.id.my_location);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomToMyLocation();
            }
        });

        /**
         * Try to reuse map
         */
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);
        parkedCarDelegateMap = new HashMap<>();

        /**
         * There is no saved instance so we create a few things
         */
        if (savedInstanceState == null) {

            // First incarnation of this activity.
            mapFragment.setRetainInstance(true);

            spotsDelegate = new SpotsDelegate();

            List<Car> cars = CarLocationManager.getAvailableCars(this);
            for (Car car : cars) {
                ParkedCarDelegate parkedCarDelegate = new ParkedCarDelegate();
                parkedCarDelegateMap.put(car, parkedCarDelegate);
            }
        }

        /**
         *
         */
        else {
            // Reincarnated activity. The obtained map is the same map instance in the previous
            // activity life cycle. There is no need to reinitialize it.
            mMap = mapFragment.getMap();

            /**
             * Do everything again
             */
            mMap.clear();

            spotsDelegate = savedInstanceState.getParcelable("spotsDelegate");
            spotsDelegate.markAsDirty();

            List<ParkedCarDelegate> parkedCarDelegates = savedInstanceState.getParcelableArrayList("parkedCarDelegates");
            for (Parcelable parcelable : parkedCarDelegates) {
                ParkedCarDelegate delegate = (ParkedCarDelegate) parcelable;
                delegate.markAsDirty();
                parkedCarDelegateMap.put(delegate.getCar(), delegate);
            }

            initialCameraSet = savedInstanceState.getBoolean("initialCameraSet");
            detailsDisplayed = savedInstanceState.getBoolean("detailsDisplayed");

        }

        /**
         * Details
         */
        detailsContainer = (ScrollView) findViewById(R.id.marker_details_container);
        ViewCompat.setElevation(detailsContainer, 8);

        detailsFragment = (DetailsFragment) getFragmentManager().findFragmentByTag(DETAILS_FRAGMENT_TAG);

        detailsContainer.setVisibility(detailsDisplayed ? View.VISIBLE : View.INVISIBLE);


        // show help dialog only on first run of the app
        if (!Util.isDialogShown(this)) {
            showHelpDialog();
        }

        bindBillingService();

        setUpMapIfNeeded();
        setUpMapListeners();

        /**
         * Init spots delegate
         */
        spotsDelegate.init(this, mMap, this);
        delegates.add(spotsDelegate);

        /**
         * Init the parked car delegates
         */
        for (Car car : parkedCarDelegateMap.keySet()) {
            ParkedCarDelegate parkedCarDelegate = parkedCarDelegateMap.get(car);
            parkedCarDelegate.init(this, this, car, mMap, null, this);
            delegates.add(parkedCarDelegate);
        }

    }


    /**
     * @param car
     * @return
     */
    private ParkedCarDelegate getParkedCarDelegate(Car car) {
        ParkedCarDelegate parkedCarDelegate = parkedCarDelegateMap.get(car);
        if (parkedCarDelegate == null) {
            Log.d(TAG, "Creating new ParkedCarDelegate");
            parkedCarDelegate = new ParkedCarDelegate();
            parkedCarDelegate.init(this, this, car, mMap, null, this);
            parkedCarDelegateMap.put(car, parkedCarDelegate);
        }
        return parkedCarDelegate;
    }

    private void showDetails() {

        detailsContainer.setVisibility(View.VISIBLE);

        detailsContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                final int height = detailsContainer.getHeight();

                if (detailsDisplayed) {
                    setMapPadding(height);
                } else {
                    detailsDisplayed = true;
                    TranslateAnimation animation = new TranslateAnimation(0, 0, height, 0);
                    int mediumAnimTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
                    animation.setDuration(mediumAnimTime);
                    animation.setInterpolator(MapsActivity.this, R.anim.my_decelerate_interpolator);
                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
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
                }
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
//        setUpMap();
        setUpMapIfNeeded();

        // when our activity resumes, we want to register for location updates
        registerReceiver(carPosReceiver, new IntentFilter(CarLocationManager.INTENT));

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onResume();
        }

        drawIfNecessary();

    }

    private void bindBillingService() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
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
        goToLogin();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (iInAppBillingService != null) {
            unbindService(mServiceConn);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putParcelable("spotsDelegate", spotsDelegate);
        savedInstanceState.putParcelableArrayList("parkedCarDelegates", new ArrayList(parkedCarDelegateMap.values()));
        savedInstanceState.putBoolean("initialCameraSet", initialCameraSet);
        savedInstanceState.putBoolean("detailsDisplayed", detailsDisplayed);

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

    private void showHelpDialog() {
        InfoDialog dialog = new InfoDialog();
        dialog.show(getFragmentManager(), "InfoDialogFragment");
        Util.setDialogShown(this, true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onPause();
        }

        unregisterReceiver(carPosReceiver);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            Log.d(TAG, "Map was set up");

            // Try to obtain the map from the SupportMapFragment.
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            setUpMap();
        }
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
        mMap.setPadding(0, Util.getActionBarSize(this), 0, 0);
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                drawIfNecessary();
            }
        });
    }


    private void setUpMapListeners() {
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(this);
    }


    private void openDonationDialog() {
        DonateDialog dialog = new DonateDialog();
        dialog.setIInAppBillingService(iInAppBillingService);
        dialog.show(getFragmentManager(), "DonateDialog");
    }

    /**
     * Method used to start the pairing activity
     */
    protected void startDeviceSelection() {
        startActivityForResult(new Intent(MapsActivity.this, DeviceSelectionActivity.class), 0);
    }

    /**
     * Implementation of {@link LocationListener}.
     */
    @Override
    public void onLocationChanged(Location location) {

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

        if (initialCameraSet) return;

        LatLng userPosition = getUserLatLng();
        if (userPosition != null && !initialCameraSet) {

            CameraUpdate update = CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(userPosition)
                    .zoom(SpotsDelegate.MAX_ZOOM)
                    .build());
            mMap.moveCamera(update);

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(userPosition)
                    .zoom(15.5F)
                    .build()));

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
        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onCameraChange(cameraPosition, justFinishedAnimating);
        }

        if (detailsFragment != null)
            detailsFragment.onCameraUpdate(justFinishedAnimating);

        justFinishedAnimating = false;

        drawIfNecessary();
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

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        // Handle presses on the action bar items
        switch (menuItem.getItemId()) {
            case R.id.action_link_device:
                startDeviceSelection();
                return true;
            case R.id.action_display_help:
                showHelpDialog();
                return true;
            case R.id.action_donate:
                openDonationDialog();
                return true;
            case R.id.action_disconnect:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }

    }


    /**
     * Redraw delegates if they need it
     */
    public void drawIfNecessary() {

        for (AbstractMarkerDelegate delegate : delegates) {
            if (delegate.isNeedsRedraw()) {
                Log.i(TAG, delegate.getClass().getSimpleName() + " is being redrawn because ");
                delegate.doDraw();
                delegate.setNeedsRedraw(false);
            }
        }

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
        FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();

        if (detailsFragment != null)
            fragTransaction.remove(detailsFragment);

        detailsFragment = fragment;
        detailsFragment.setRetainInstance(true);
        detailsFragment.setUserLocation(getUserLocation());

        fragTransaction.add(detailsContainer.getId(), detailsFragment, DETAILS_FRAGMENT_TAG);
        fragTransaction.commit();

        showDetails();
    }

    @Override
    public void onCarPositionDeleted(Car car) {
        hideDetails();
    }


    @Override
    public void onCarClicked(Car car) {
        setDetailsFragment(CarDetailsFragment.newInstance(car, getParkedCarDelegate(car)));
    }


    private boolean justFinishedAnimating = false;

    @Override
    public void onCameraUpdateRequest(CameraUpdate cameraUpdate) {
        mMap.animateCamera(cameraUpdate, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                justFinishedAnimating = true;
            }

            @Override
            public void onCancel() {
            }
        });
    }

    private void zoomToMyLocation() {

        Log.d(TAG, "zoomToMyLocation");

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onZoomToMyLocation();
        }

        LatLng userPosition = getUserLatLng();
        if (userPosition == null) return;
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .zoom(Math.max(mMap.getCameraPosition().zoom, 14))
                        .target(userPosition)
                        .build()),
                new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        justFinishedAnimating = true;
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}
