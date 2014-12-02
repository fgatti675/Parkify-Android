package com.bahpps.cahue;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends ActionBarActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        Toolbar.OnMenuItemClickListener,
        SpotsDelegate.SpotSelectedListener,
        ParkedCarDelegate.CarSelectedListener,
        CarDetailsFragment.OnCarPositionDeletedListener,
        CameraUpdateListener {

    protected static final String TAG = "Maps";

    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";

    static final String DETAILS_FRAGMENT_TAG = "DETAILS_FRAGMENT";

    static final int REQUEST_CODE_PICK_ACCOUNT = 1;
    static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 2;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 3;
    static final int REQUEST_ON_PURCHASE = 1001;

    private static final String PREF_USER_EMAIL = "pref_user_email";

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

    private String accountName;
    private String authToken;

    private GoogleApiClient googleApiClient;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private SharedPreferences prefs;

    private View detailsContainer;
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
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        ViewCompat.setElevation(toolbar, 8);

        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(this);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            noBluetooth(this);
        } else if (!mBluetoothAdapter.isEnabled()) {
            noBluetooth(this);
        }

        /**
         * Try to reuse map
         */
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);

        parkedCarDelegateMap = new HashMap<Car, ParkedCarDelegate>();

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

        if (detailsDisplayed) detailsContainer.setVisibility(View.VISIBLE);

        /**
         * Preferences
         */
        prefs = Util.getSharedPreferences(this);

        // show help dialog only on first run of the app
        boolean dialogShown = prefs.getBoolean(Util.PREF_DIALOG_SHOWN, false);
        if (!dialogShown) {
            showHelpDialog();
        }

        bindBillingService();

        setUpUserAccount();

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
     *
     * @param car
     * @return
     */
    private ParkedCarDelegate getParkedCarDelegate(Car car) {
        ParkedCarDelegate parkedCarDelegate = parkedCarDelegateMap.get(car);
        if (parkedCarDelegate == null) {
            parkedCarDelegate = new ParkedCarDelegate();
            parkedCarDelegate.init(this, this, car, mMap, null, this);
            parkedCarDelegateMap.put(car, parkedCarDelegate);
        }
        return parkedCarDelegate;
    }

    private void showDetails() {

        if (detailsDisplayed) return;

        Log.i(TAG, "DETAILS HEIGHT " + detailsContainer.getHeight());
        detailsDisplayed = true;
        TranslateAnimation animation = new TranslateAnimation(0, 0, detailsContainer.getHeight(), 0);
        animation.setDuration(300);
        animation.setInterpolator(this, R.anim.my_decelerate_interpolator);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                detailsContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mMap.setPadding(0, Util.getActionBarSize(this), 0, detailsContainer.getHeight());
        detailsContainer.startAnimation(animation);

    }

    private void hideDetails() {

        if (!detailsDisplayed) return;

        detailsDisplayed = false;
        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, detailsContainer.getHeight());
        animation.setDuration(300);
        animation.setInterpolator(this, R.anim.my_decelerate_interpolator);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                detailsContainer.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mMap.setPadding(0, Util.getActionBarSize(this), 0, 0);
        detailsContainer.startAnimation(animation);
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

        setUpMapIfNeeded();
        setUpLocationClientIfNeeded();

        // when our activity resumes, we want to register for location updates
        registerReceiver(carPosReceiver, new IntentFilter(CarLocationManager.INTENT));

        googleApiClient.connect();

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onResume();
        }

        drawIfNecessary();

    }

    private void setUpUserAccount() {
        accountName = prefs.getString(PREF_USER_EMAIL, null);
        if (accountName == null) {
            try {
                Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                        new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
                startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
            } catch (ActivityNotFoundException e) {
                // TODO
            }
        } else {
            requestOauthToken();
        }
    }

    private void requestOauthToken() {


        Log.i(TAG, "requestOauthToken");

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                try {
                    authToken = GoogleAuthUtil.getToken(MapsActivity.this, accountName, SCOPE);
                    Log.d(TAG, authToken);
                    onAuthCompleted(authToken);
                } catch (UserRecoverableAuthException userRecoverableException) {
                    // GooglePlayServices.apk is either old, disabled, or not present
                    // so we need to show the user some UI in the activity to recover.
                    handleException(userRecoverableException);
                    Log.d(TAG, "Auth token: " + authToken);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GoogleAuthException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

    }

    private void onAuthCompleted(String authToken) {

    }

    private void bindBillingService() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Receiving a result from the AccountPicker
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                prefs.edit().putString(PREF_USER_EMAIL, accountName).apply();
                Log.i(TAG, "Users email:" + accountName);

                requestOauthToken();
            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, R.string.pick_account, Toast.LENGTH_SHORT).show();
            }
        } else if ((requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR)
                && resultCode == RESULT_OK) {
            // Receiving a result that follows a GoogleAuthException, try auth again
            setUpUserAccount();
        }


        // element purchase
        else if (requestCode == REQUEST_ON_PURCHASE) {

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
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    /**
     * This method is a hook for background threads and async tasks that need to provide the
     * user a response UI when an exception occurs.
     */
    public void handleException(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException) e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                            MapsActivity.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException) e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }


    private void showHelpDialog() {
        InfoDialog dialog = new InfoDialog();
        dialog.show(getFragmentManager(), "InfoDialogFragment");
        prefs.edit().putBoolean(Util.PREF_DIALOG_SHOWN, true).apply();
    }


    @Override
    protected void onPause() {
        super.onPause();

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onPause();
        }

        if (googleApiClient != null) {
            googleApiClient.disconnect();
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
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpLocationClientIfNeeded() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
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
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            noBluetooth(this);
        } else if (!mBluetoothAdapter.isEnabled()) {
            noBluetooth(this);
        } else {
            startActivityForResult(new Intent(MapsActivity.this, DeviceListActivity.class), 0);
        }
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


    /**
     * Callback called when connected to GCore. Implementation of {@link GooglePlayServicesClient.ConnectionCallbacks}.
     */
    @Override
    public void onConnected(Bundle connectionHint) {

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                REQUEST,
                this);
        // call convenience method that zooms map on our location only on starting the app
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
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d(TAG, "Long tap event " + latLng.latitude + " " + latLng.longitude);

        Location location = new Location(Util.TAPPED_PROVIDER);
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        location.setAccuracy(10);

        SetCarPositionDialog dialog = new SetCarPositionDialog();
        dialog.init(location);
        dialog.show(getFragmentManager(), "SetCarPositionDialog");

    }

    private LatLng getUserLatLng() {
        Location userLastLocation = getUserLocation();
        if (userLastLocation == null) return null;
        return new LatLng(userLastLocation.getLatitude(), userLastLocation.getLongitude());
    }

    private Location getUserLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onCameraChange(cameraPosition, justFinishedAnimating);
        }

        if(detailsFragment != null)
            detailsFragment.onCameraUpdate(justFinishedAnimating);

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
//        parkedCarDelegateMap.setModeFree();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

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

    /**
     * Shows a toast in case no BT is detected
     */
    public static void noBluetooth(Context context) {
        Util.createUpperToast(context, context.getString(R.string.bt_not_available), Toast.LENGTH_LONG);
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
    public void onFollowingClicked(Car car) {
        getParkedCarDelegate(car).setFollowing();
    }

    @Override
    public void onCarPositionDeleted(Car car) {
        getParkedCarDelegate(car).removeCar();
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

        LatLng userPosition = getUserLatLng();
        if (userPosition == null) return;
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .target(userPosition)
                        .zoom(15.5f)
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
