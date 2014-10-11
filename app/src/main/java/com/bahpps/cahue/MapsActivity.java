package com.bahpps.cahue;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.bahpps.cahue.util.BluetoothDetector;
import com.bahpps.cahue.util.CarLocationManager;
import com.bahpps.cahue.util.GMapV2Direction;
import com.bahpps.cahue.util.Util;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MapsActivity extends Activity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, ParkingSpotsService.ParkingSpotsUpdateListener {

    /**
     * Camera mode
     */
    private enum Mode {
        FREE, FOLLOWING;
    }

    private Mode mode;

    protected static final String TAG = "Maps";

    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";

    static final int REQUEST_CODE_PICK_ACCOUNT = 0;
    static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 2;
    static final int REQUEST_ON_PURCHASE = 1001;

    private static final String PREF_USER_EMAIL = "pref_user_email";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

    private String accountName;
    private String authToken;

    private GoogleApiClient googleApiClient;

    private Marker carMarker;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private SharedPreferences prefs;

    private Button linkButton;

    private IconGenerator iconFactory;

    private ImageButton carButton;

    private boolean justFinishedAnimating = false;

    /**
     * Directions delegate
     */
    private GMapV2Direction md;

    /**
     * Actual lines representing the directionsPolyLine
     */
    private Polyline directionsPolyLine;
    private ArrayList<LatLng> directionPoint;

    /**
     * If we get a new car position while we are using the app, we update the map
     */
    private final BroadcastReceiver carPosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Location location = (Location) intent.getExtras().get(CarLocationManager.INTENT_POSITION);
            if (location != null) {
                Log.i(TAG, "Location received: " + location);
                mMap.clear();
                setCar(location);
                drawDirections();
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

        setContentView(R.layout.main);

        setUpLocationClientIfNeeded();
        setUpMapIfNeeded();


        iconFactory = new IconGenerator(this);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Util.noBluetooth(this);
        } else if (!mBluetoothAdapter.isEnabled()) {
            Util.noBluetooth(this);
        }

        if (md == null)
            md = new GMapV2Direction();

        /**
         * Try to reuse map
         */
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapview);

        if (savedInstanceState == null) {
            // First incarnation of this activity.
            mapFragment.setRetainInstance(true);
        } else {
            // Reincarnated activity. The obtained map is the same map instance in the previous
            // activity life cycle. There is no need to reinitialize it.
            mMap = mapFragment.getMap();
            mMap.clear();
        }

        /**
         * Restore mode if saved
         */
        if (savedInstanceState != null) {
            mode = (Mode) savedInstanceState.getSerializable("mode");
            directionPoint = (ArrayList) savedInstanceState.getSerializable("directionPoint");
        }

        /**
         * Car button for indicating the camera mode
         */
        carButton = (ImageButton) findViewById(R.id.carButton);
        carButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mode == Mode.FREE) setMode(Mode.FOLLOWING);
                else if (mode == Mode.FOLLOWING) setMode(Mode.FREE);
            }
        });

        // button for linking a BT device
        linkButton = (Button) findViewById(R.id.linkButton);
        linkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startDeviceSelection();
            }
        });

        /**
         * Preferences
         */
        prefs = Util.getSharedPreferences(this);

        // show help dialog only on first run of the app
        boolean dialogShown = prefs.getBoolean(Util.PREF_DIALOG_SHOWN, false);
        if (!dialogShown) {
            showHelpDialog();
        }

        Location carLocation = CarLocationManager.getStoredLocation(this);
        setCar(carLocation);

        bindBillingService();

        setUserAccount();

    }

    private void setUserAccount() {
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
                } catch (UserRecoverableAuthException userRecoverableException) {
                    // GooglePlayServices.apk is either old, disabled, or not present
                    // so we need to show the user some UI in the activity to recover.
                    handleException(userRecoverableException);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GoogleAuthException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

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
        }

        else if ((requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR)
                && resultCode == RESULT_OK) {
            // Receiving a result that follows a GoogleAuthException, try auth again
            setUserAccount();
        }


        // element purchse
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

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putSerializable("mode", mode);
        savedInstanceState.putSerializable("directionPoint", directionPoint);
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

    private void drawDirections() {

        Log.i(TAG, "drawDirections");

        final LatLng carPosition = getCarPosition();

        if (directionsPolyLine != null)
            directionsPolyLine.remove();

        if (carPosition == null || getUserPosition() == null) {
            return;
        }

        new AsyncTask<Object, Object, Document>() {

            @Override
            protected Document doInBackground(Object[] objects) {
                Document doc = md.getDocument(getUserPosition(), carPosition, GMapV2Direction.MODE_WALKING);
                return doc;
            }

            @Override
            protected void onPostExecute(Document doc) {
                directionPoint = md.getDirection(doc);
                PolylineOptions rectLine = new PolylineOptions().width(10).color(Color.argb(85, 242, 69, 54));

                for (int i = 0; i < directionPoint.size(); i++) {
                    rectLine.add(directionPoint.get(i));
                }
                directionsPolyLine = mMap.addPolyline(rectLine);

                updateCameraIfFollowing();
            }

        }.execute();

    }

    private void setMode(Mode mode) {

        Log.i(TAG, "Setting mode to " + mode);

        this.mode = mode;

        updateCameraIfFollowing();

        if (mode == Mode.FOLLOWING) {
            carButton.setImageResource(R.drawable.ic_icon_car_red);
        } else if (mode == Mode.FREE) {
            carButton.setImageResource(R.drawable.ic_icon_car);
        }

    }

    private void showHelpDialog() {
        InfoDialog dialog = new InfoDialog();
        dialog.show(getFragmentManager(), "InfoDialogFragment");
        prefs.edit().putBoolean(Util.PREF_DIALOG_SHOWN, true).apply();
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setUpLocationClientIfNeeded();
        googleApiClient.connect();

        // when our activity resumes, we want to register for location updates
        registerReceiver(carPosReceiver, new IntentFilter(CarLocationManager.INTENT));

        // bt adress on the linked device
        String btAddress = prefs.getString(BluetoothDetector.PREF_BT_DEVICE_ADDRESS, "");

        // we hide the linkbutton if the app is linked
        if (!btAddress.equals("")) {
            linkButton.setVisibility(View.GONE);
        } else {
            linkButton.setVisibility(View.VISIBLE);
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
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
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapview))
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
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(this);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.setPadding(0, Util.getActionBarSize(this), 0, 0);
        mMap.setOnMarkerClickListener(this);
    }

    /**
     * Displays the car in the map
     */
    private void setCar(Location carLocation) {

        // remove previous
        if (carMarker != null) {
            carMarker.remove();
        }

        if (carLocation == null) {
            // remove directionsPolyLine if there too
            if (directionsPolyLine != null) {
                directionsPolyLine.remove();
                directionsPolyLine = null;
            }
            carMarker = null;
            return;
        }

        // TODO accuracy

        double latitude = carLocation.getLatitude();
        double longitude = carLocation.getLongitude();

        if (latitude != 0 && longitude != 0) {

            Log.i(TAG, "Setting car in map: " + latitude + " " + longitude);

            iconFactory.setContentRotation(-90);
            iconFactory.setStyle(IconGenerator.STYLE_RED);

            // Uses a colored icon.
            carMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .snippet("")
                    .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(getResources().getText(R.string.car).toString().toUpperCase())))
                    .anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV()));


        } else {
            Log.i(TAG, "No car location available");
        }

    }

    /**
     * Show menu method
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // inflate from xml
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_link_device:
                startDeviceSelection();
                return true;
            case R.id.action_display_help:
                showHelpDialog();
                return true;
            case R.id.action_remove_car:
                removeCar();
                return true;
            case R.id.action_donate:
                openDonationDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openDonationDialog() {
        DonateDialog dialog = new DonateDialog();
        dialog.setIInAppBillingService(iInAppBillingService);
        dialog.show(getFragmentManager(), "DonateDialog");
    }

    private void removeCar() {
        CarLocationManager.removeStoredLocation(this);
        setCar(null);
    }

    /**
     * Method used to start the pairing activity
     */
    protected void startDeviceSelection() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Util.noBluetooth(this);
        } else if (!mBluetoothAdapter.isEnabled()) {
            Util.noBluetooth(this);
        } else {
            startActivityForResult(new Intent(MapsActivity.this, DeviceListActivity.class), 0);
        }
    }

    /**
     * Implementation of {@link LocationListener}.
     */
    @Override
    public void onLocationChanged(Location location) {

        updateCameraIfFollowing();

        if (getCarPosition() != null && directionsPolyLine == null) {
            drawDirections();
        }

    }

    private void updateCameraIfFollowing() {

        if (mode == Mode.FOLLOWING) {
            if (getCarPosition() != null)
                zoomToSeeBoth();
            else
                zoomToMyLocation();
        }
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
        LatLng userPosition = getUserPosition();
        if (userPosition != null)
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(userPosition)
                    .zoom(15.5f)
                    .build()));

        if (mode == null)
            setMode(Mode.FOLLOWING);
        else
            setMode(mode);

        queryParkingLocations();


    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    /**
     * This method shows the Toast when the car icon is pressed, telling the user the parking time
     */
    private void showCarTimeToast() {
        String toastMsg = getString(R.string.car_was_here);

        long timeDiff = Calendar.getInstance().getTimeInMillis() - prefs.getLong(Util.PREF_CAR_TIME, 0);

        String time = "";

        long seconds = timeDiff / 1000;
        if (seconds < 60) {
            time = seconds + " " + getString(R.string.seconds);
        } else {
            long minutos = timeDiff / (60 * 1000);
            if (minutos < 60) {
                time = minutos
                        + (minutos > 1 ? " " + getString(R.string.minutes) : " "
                        + getString(R.string.minute));
            } else {
                long hours = timeDiff / (60 * 60 * 1000);
                if (hours < 24) {
                    time = hours
                            + (hours > 1 ? " " + getString(R.string.hours) : " "
                            + getString(R.string.hour));
                } else {
                    long days = timeDiff / (24 * 60 * 60 * 1000);
                    time = days
                            + (days > 1 ? " " + getString(R.string.days) : " "
                            + getString(R.string.day));
                }
            }
        }

        toastMsg = String.format(toastMsg, time);

        Util.createUpperToast(this, toastMsg, Toast.LENGTH_SHORT);

    }


    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d(TAG, "Long tap event " + latLng.latitude + " " + latLng.longitude);

        Location location = new Location(Util.TAPPED_PROVIDER);
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);

        SetCarPositionDialog dialog = new SetCarPositionDialog();
        dialog.setLocation(location);
        dialog.show(getFragmentManager(), "SetCarPositionDialog");

    }

    /**
     * This method zooms to see both user and the car.
     */
    protected void zoomToSeeBoth() {

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(getCarPosition())
                .include(getUserPosition());

        if (directionsPolyLine != null) {
            for (LatLng latLng : directionsPolyLine.getPoints())
                builder.include(latLng);
        }


        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150), new GoogleMap.CancelableCallback() {
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
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .target(getUserPosition())
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


    /**
     * This method zooms to the car's location.
     */
    private void zoomToCar() {

        if (carMarker == null) return;

        LatLng loc = getCarPosition();

        if (loc != null) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(loc)
                    .zoom(15.5f)
                    .build()), null);

            showCarTimeToast();
        } else {
            Util.createUpperToast(this, getString(R.string.car_not_found), Toast.LENGTH_SHORT);
        }
    }

    private LatLng getUserPosition() {
        Location userLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (userLastLocation == null) return null;
        return new LatLng(userLastLocation.getLatitude(), userLastLocation.getLongitude());
    }

    private LatLng getCarPosition() {
        if (carMarker == null) return null;
        return carMarker.getPosition();
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if (!justFinishedAnimating) setMode(Mode.FREE);
        justFinishedAnimating = false;
    }

    private void queryParkingLocations() {

        new ParkingSpotsService(mMap.getProjection().getVisibleRegion().latLngBounds, this).execute();
    }


    @Override
    public void onLocationsUpdate(List<ParkingSpot> parkingSpots) {
        Log.d(TAG, parkingSpots.toString());
    }



    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.equals(carMarker)) {
            showCarTimeToast();
        }
        return false;
    }


    @Override
    public void onMapClick(LatLng point) {
        setMode(Mode.FREE);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
