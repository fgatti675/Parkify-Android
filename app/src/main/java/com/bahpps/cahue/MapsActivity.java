package com.bahpps.cahue;

import android.accounts.AccountManager;
import android.app.Activity;
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
import com.google.android.gms.common.AccountPicker;
import com.bahpps.cahue.util.BluetoothDetector;
import com.bahpps.cahue.util.CarLocationManager;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MapsActivity extends Activity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {




    protected static final String TAG = "Maps";

    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";


    private static final int REQUEST_CODE_EMAIL = 1;

    private static final String PREF_USER_EMAIL = "pref_user_email";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private ParkedCarDelegate parkedCarDelegate;


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

    private Button linkButton;

    private ImageButton carButton;


    /**
     * If we get a new car position while we are using the app, we update the map
     */
    private final BroadcastReceiver carPosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Location location = (Location) intent.getExtras().get(CarLocationManager.INTENT_POSITION);
            if (location != null) {
                Log.i(TAG, "Location received: " + location);
                parkedCarDelegate.setCarLocation(location);
                parkedCarDelegate.draw();
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
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            iInAppBillingService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Util.noBluetooth(this);
        } else if (!mBluetoothAdapter.isEnabled()) {
            Util.noBluetooth(this);
        }


        /**
         * Car button for indicating the camera mode
         */
        carButton = (ImageButton) findViewById(R.id.carButton);
        carButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                parkedCarDelegate.setUpOrChangeMode();
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
            setUpMap();
        }

        /**
         * Restore parkedCarDelegate if saved
         */
        if (savedInstanceState != null) {
            parkedCarDelegate = savedInstanceState.getParcelable("parkedCarDelegate");
        } else {
            parkedCarDelegate = new ParkedCarDelegate();
        }

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

        setUserAccount();

    }

    private void setUserAccount() {
        accountName = prefs.getString(PREF_USER_EMAIL, null);
        if (accountName == null) {
            try {
                Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                        new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
                startActivityForResult(intent, REQUEST_CODE_EMAIL);
            } catch (ActivityNotFoundException e) {
                // TODO
            }
        } else {
//            requestOauthToken();
        }
    }

    private void requestOauthToken() {
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                try {
                    authToken = GoogleAuthUtil.getToken(MapsActivity.this, accountName, SCOPE);
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

    private void bindBillingService() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EMAIL && resultCode == RESULT_OK) {
            accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            prefs.edit().putString(PREF_USER_EMAIL, accountName).apply();
            Log.i(TAG, "Users email:" + accountName);

//            requestOauthToken();
        }

        // element purchse
        else if (requestCode == 1001) {

            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    Util.createToast(this, getString(R.string.thanks), Toast.LENGTH_LONG); // do string
                } catch (JSONException e) {
                    Util.createToast(this, "Failed to parse purchase data.", Toast.LENGTH_LONG);
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
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putParcelable("parkedCarDelegate", parkedCarDelegate);
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

        parkedCarDelegate.init(this, mMap, carButton);
        parkedCarDelegate.setCarLocationIfNull(CarLocationManager.getStoredLocation(this));
        parkedCarDelegate.draw();

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
        parkedCarDelegate.removeCar();
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

        parkedCarDelegate.setUserLocation(location);
        parkedCarDelegate.updateCameraIfFollowing();


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
        LatLng userPosition = getUserLatLng();
        if (userPosition != null)
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(userPosition)
                    .zoom(15.5f)
                    .build()));

        parkedCarDelegate.setUpOrChangeMode();


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
        dialog.setLocation(location);
        dialog.show(getFragmentManager(), "SetCarPositionDialog");

    }

    private LatLng getUserLatLng() {
        Location userLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (userLastLocation == null) return null;
        return new LatLng(userLastLocation.getLatitude(), userLastLocation.getLongitude());
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return parkedCarDelegate.onMarkerClick(marker);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        parkedCarDelegate.onCameraChange(cameraPosition);
    }



    @Override
    public void onMapClick(LatLng point) {
        parkedCarDelegate.setModeFree();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
