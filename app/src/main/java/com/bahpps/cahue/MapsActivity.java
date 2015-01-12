package com.bahpps.cahue;

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
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.bahpps.cahue.activityRecognition.ActivityRecognitionService;
import com.bahpps.cahue.parkedCar.Car;
import com.bahpps.cahue.parkedCar.CarLocationManager;
import com.bahpps.cahue.parkedCar.ParkedCarDelegate;
import com.bahpps.cahue.spots.ParkingSpot;
import com.bahpps.cahue.spots.SpotsDelegate;
import com.bahpps.cahue.tutorial.TutorialActivity;
import com.bahpps.cahue.util.Util;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
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
        CameraUpdateListener,
        OnMapReadyCallback {


    protected static final String TAG = "Maps";

    static final String DETAILS_FRAGMENT_TAG = "DETAILS_FRAGMENT";

    static final int REQUEST_ON_PURCHASE = 1001;
    private static final float INITIAL_ZOOM = 12F;

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

    private IInAppBillingService iInAppBillingService;

    /**
     * If we get a new car position while we are using the app, we update the map
     */
    private final BroadcastReceiver carPosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Car car = (Car) intent.getExtras().get(CarLocationManager.INTENT_POSITION);
            if (car != null) {
                Log.i(TAG, "Location received: " + car);
                getParkedCarDelegate(car).updateCarLocation();
            }

        }
    };

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
    private PendingIntent pIntent;

    @Override
    protected void onPlusClientSignIn() {
        LocationServices.FusedLocationApi.requestLocationUpdates(getGoogleApiClient(),
                REQUEST,
                this);

        Intent intent = new Intent(this, ActivityRecognitionService.class);
        pIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(getGoogleApiClient(), 2000, pIntent);


    }

    @Override
    protected void onPlusClientSignOut() {
        goToLogin();
    }

    private void goToLogin() {
        if (!isFinishing()) {
            Util.setIsLoggedIn(this, false);
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

        if (!Util.isLoggedIn(this)) {
            goToLogin();
            return;
        }

        // show help dialog only on first run of the app
        if (!Util.isTutorialShown(this)) {
            goToTutorial();
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        ViewCompat.setElevation(toolbar, 8);

        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(this);

        myLocationButton = findViewById(R.id.my_location);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomToMyLocation(false);
            }
        });

        /**
         * Try to reuse map
         */
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        parkedCarDelegateMap = new HashMap<>();

        /**
         * There is no saved instance so we create a few things
         */
        if (savedInstanceState == null) {

            // First incarnation of this activity.
            mapFragment.setRetainInstance(true);

            spotsDelegate = new SpotsDelegate();

            List<Car> cars = CarLocationManager.getAvailableCars(this, false);
            for (Car car : cars) {
                ParkedCarDelegate parkedCarDelegate = new ParkedCarDelegate(car);
                parkedCarDelegateMap.put(car, parkedCarDelegate);
            }
        }

        /**
         *
         */
        else {

            spotsDelegate = savedInstanceState.getParcelable("spotsDelegate");
            spotsDelegate.markAsDirty();

            List<ParkedCarDelegate> parkedCarDelegates = savedInstanceState.getParcelableArrayList("parkedCarDelegates");
            for (Parcelable parcelable : parkedCarDelegates) {
                ParkedCarDelegate delegate = (ParkedCarDelegate) parcelable;
                delegate.markAsDirty();
                parkedCarDelegateMap.put(delegate.getCar(), delegate);
            }

            initialCameraSet = savedInstanceState.getBoolean("initialCameraSet");
            mapInitialised = savedInstanceState.getBoolean("mapInitialised");
            detailsDisplayed = savedInstanceState.getBoolean("detailsDisplayed");

        }

        /**
         * Details
         */
        detailsContainer = (ScrollView) findViewById(R.id.marker_details_container);
        ViewCompat.setElevation(detailsContainer, 8);

        detailsFragment = (DetailsFragment) getFragmentManager().findFragmentByTag(DETAILS_FRAGMENT_TAG);

        detailsContainer.setVisibility(detailsDisplayed ? View.VISIBLE : View.INVISIBLE);

        bindBillingService();

        /**
         * Add delegates
         */

        delegates.add(spotsDelegate);

        for (ParkedCarDelegate parkedCarDelegate : parkedCarDelegateMap.values()) {
            delegates.add(parkedCarDelegate);
        }

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


    /**
     * @param car
     * @return
     */
    private ParkedCarDelegate getParkedCarDelegate(Car car) {
        ParkedCarDelegate parkedCarDelegate = parkedCarDelegateMap.get(car);
        if (parkedCarDelegate == null) {
            Log.d(TAG, "Creating new ParkedCarDelegate");
            parkedCarDelegate = new ParkedCarDelegate(car);
            parkedCarDelegate.init(this, this, this);
            parkedCarDelegate.onMapReady(mMap);
            parkedCarDelegateMap.put(car, parkedCarDelegate);
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
                        params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
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

        /**
         * Init delegates
         */
        spotsDelegate.init(this, this);

        for (ParkedCarDelegate parkedCarDelegate : parkedCarDelegateMap.values()) {
            parkedCarDelegate.init(this, this, this);
        }

        // when our activity resumes, we want to register for location updates
        registerReceiver(carPosReceiver, new IntentFilter(CarLocationManager.INTENT));

        setInitialCamera();


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
        Log.d(TAG, "onSignInRequired");
        goToLogin();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (iInAppBillingService != null) {
            unbindService(mServiceConn);
        }

        if (getGoogleApiClient().isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(getGoogleApiClient(), this);
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(getGoogleApiClient(), pIntent);
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

        savedInstanceState.putParcelable("spotsDelegate", spotsDelegate);
        savedInstanceState.putParcelableArrayList("parkedCarDelegates", new ArrayList(parkedCarDelegateMap.values()));
        savedInstanceState.putBoolean("initialCameraSet", initialCameraSet);
        savedInstanceState.putBoolean("mapInitialised", mapInitialised);
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

    private void goToTutorial() {
        startActivity(new Intent(this, TutorialActivity.class));
        Util.setTutorialShown(this, true);
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

        if (mMap == null) return;

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

            final List<Car> parkedCars = CarLocationManager.getAvailableCars(this, true);

            // One parked car
            if (parkedCars.size() == 1) {

                Car car = parkedCars.get(0);
                ParkedCarDelegate parkedCarDelegate = getParkedCarDelegate(car);
                parkedCarDelegate.onLocationChanged(getUserLocation());
                if (!parkedCarDelegate.isTooFar())
                    onCarClicked(car);
                else
                    zoomToMyLocation(true);

            }

            // zoom to user otherwise
            else {
                zoomToMyLocation(true);
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

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onCameraChange(cameraPosition, justFinishedAnimating);
        }

        if (detailsFragment != null && detailsFragment.isResumed())
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
                goToTutorial();
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
        setDetailsFragment(CarDetailsFragment.newInstance(car, getParkedCarDelegate(car), true));
    }


    private boolean justFinishedAnimating = false;

    @Override
    public void onCameraUpdateRequest(CameraUpdate cameraUpdate) {

        if (mMap == null) return;

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

    private void zoomToMyLocation(boolean resetZoom) {

        Log.d(TAG, "zoomToMyLocation");

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onZoomToMyLocation();
        }

        LatLng userPosition = getUserLatLng();
        if (userPosition == null) return;

        float zoom = 17F;
        if (!resetZoom)
            zoom = Math.max(mMap.getCameraPosition().zoom, 14);

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .zoom(zoom)
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
}
