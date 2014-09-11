package com.bahpps.cahue;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bahpps.cahue.auxiliar.Util;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
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

import java.util.Calendar;

public class MapsActivity extends Activity
        implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMapLongClickListener {

    protected static final String TAG = "Maps";
    private static final int INFO_DIALOG = 0;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.


    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private LocationClient mLocationClient;
    private Marker carMarker;


    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private SharedPreferences prefs;

    private Button linkButton;

    // TODO: remove static variable?
    private static boolean firstRun = true;


    /**
     * If we get a new car position while we are using the app, we update the map
     */
    private final BroadcastReceiver carPosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Location location = (Location) intent.getExtras().get(Util.EXTRA_LOCATION);
            if (location != null) {
                Log.i(TAG, "Location received: " + location);
                setCar(location.getLatitude(), location.getLongitude(), location.getAccuracy());
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setUpMapIfNeeded();
        setUpLocationClientIfNeeded();

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Util.noBluetooth(this);
        } else if (!mBluetoothAdapter.isEnabled()) {
            Util.noBluetooth(this);
        }

        // try to reuse map
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapview);

        if (savedInstanceState == null) {
            // First incarnation of this activity.
            mapFragment.setRetainInstance(true);
        } else {
            // Reincarnated activity. The obtained map is the same map instance in the previous
            // activity life cycle. There is no need to reinitialize it.
            mMap = mapFragment.getMap();
        }

        // button for moving to the car's position
        ImageButton carButton = (ImageButton) findViewById(R.id.carButton);
        carButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                zoomToCar();
            }
        });

        // button for moving to the car's position
        ImageButton userButton = (ImageButton) findViewById(R.id.userButton);
        userButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                zoomToMyLocation();
            }
        });

        // button for linking a BT device
        linkButton = (Button) findViewById(R.id.linkButton);
        linkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startDeviceSelection();
            }
        });

        prefs = Util.getSharedPreferences(this);

        // we add the car

        // show help dialog only on first run of the app
        boolean dialogShown = prefs.getBoolean(Util.PREF_DIALOG_SHOWN, false);
        if (!dialogShown) {
            showDialog(INFO_DIALOG);
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setUpLocationClientIfNeeded();
        mLocationClient.connect();

        // when our activity resumes, we want to register for location updates
        registerReceiver(carPosReceiver, new IntentFilter(Util.INTENT_NEW_CAR_POS));

        // bt adress on the linked device
        String btAddress = prefs.getString(Util.PREF_BT_DEVICE_ADDRESS, "");

        // we hide the linkbutton if the app is linked
        if (!btAddress.equals("")) {
            linkButton.setVisibility(View.GONE);
        } else {
            linkButton.setVisibility(View.VISIBLE);
        }

        double latitude = (prefs.getInt(Util.PREF_CAR_LATITUDE, 0)) / 1E6;
        double longitude = (prefs.getInt(Util.PREF_CAR_LONGITUDE, 0)) / 1E6;
        float accuracy = (float) ((prefs.getInt(Util.PREF_CAR_ACCURACY, 0)) / 1E6);

        // we add the car on the stored position
        setCar(latitude, longitude, accuracy);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(carPosReceiver);
        if (mLocationClient != null) {
            mLocationClient.disconnect();
        }
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
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(
                    getApplicationContext(),
                    this,  // ConnectionCallbacks
                    this); // OnConnectionFailedListener
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setOnMapLongClickListener(this);
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
    }

    /**
     * Displays the car in the map
     *
     * @param latitude  as an int in 1E6
     * @param longitude as an int in 1E6
     */
    private void setCar(double latitude, double longitude, float accuracy) {

        // remove previous
        if (carMarker != null) {
            carMarker.remove();
        }

        // TODO accuracy

        if (latitude != 0 && longitude != 0) {

            Log.i(TAG, "Car location: " + latitude + " " + longitude);

            // location = new GeoPoint(40347990, -3821760);

            // Uses a colored icon.
            carMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .title("My car")
                    .snippet("Population: 2,074,200")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car_marker)));


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
        inflater.inflate(R.menu.menu, menu);

        menu.getItem(0).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                startDeviceSelection();
                return false;
            }
        });

        menu.getItem(1).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                showDialog(0);
                return false;
            }
        });

        return true;
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
    }

    /**
     * Callback called when connected to GCore. Implementation of {@link GooglePlayServicesClient.ConnectionCallbacks}.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        mLocationClient.requestLocationUpdates(
                REQUEST,
                this);  // LocationListener

        // call convenience method that zooms map on our location only on starting the app
        if (firstRun) {

            if (prefs.getLong(Util.PREF_CAR_TIME, 0) == 0) {
                zoomToMyLocation();
            } else {
                zoomToSeeBoth();
            }

            firstRun = false;
        }
    }

    /**
     * Callback called when disconnected from GCore. Implementation of {@link GooglePlayServicesClient.ConnectionCallbacks}.
     */
    @Override
    public void onDisconnected() {
        // Do nothing
    }

    /**
     * Implementation of {@link GooglePlayServicesClient.OnConnectionFailedListener}.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Do nothing
    }

    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
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

        Util.createToast(this, toastMsg, Toast.LENGTH_SHORT);

    }


    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d(TAG, "Long tap event " + latLng.latitude + " " + latLng.longitude);

        Location location = new Location(Util.TAPPED_PROVIDER);
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);

        SetCarPositionDialog dialog = new SetCarPositionDialog();
        dialog.setLocation(location);
        dialog.show(getFragmentManager(), "NoticeDialogFragment");

    }

    /**
     * This method zooms to see both user and the car.
     */
    protected void zoomToSeeBoth() {
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(getCarPosition())
                .include(getUserPosition())
                .build();

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));

    }


    private void zoomToMyLocation() {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(getUserPosition())
                .zoom(15.5f)
                .build()), null);
    }

    private LatLng getUserPosition() {
        Location userLastLocation = mLocationClient.getLastLocation();
        return new LatLng(userLastLocation.getLatitude(), userLastLocation.getLongitude());
    }

    private LatLng getCarPosition() {
        return carMarker.getPosition();
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
            Util.createToast(this, getString(R.string.car_not_found), Toast.LENGTH_SHORT);
        }
    }

}
