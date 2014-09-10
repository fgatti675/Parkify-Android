package com.bahpps.cahue;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bahpps.cahue.auxiliar.Util;
import com.bahpps.cahue.location.LocationPoller;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Calendar;

public class MapsActivity extends FragmentActivity
        implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMyLocationButtonClickListener {

    protected static final String TAG = "Maps";
    private static final int INFO_DIALOG = 0;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private LocationClient mLocationClient;

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private Marker carMarker;


    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private SharedPreferences prefs;

    private Button linkButton;

    // TODO: remove static variable?
    private static boolean firstRun = true;

    private GestureDetector mGestureDetector;

    /**
     * If we get a new car position while we are using the app, we update the map
     */
    private final BroadcastReceiver carPosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Location loc = (Location) intent.getExtras().get(LocationPoller.EXTRA_LOCATION);
            if (loc != null) {
                Log.i(TAG, "Location received: " + loc);
                addCar(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy());
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setUpMapIfNeeded();
        setUpLocationClientIfNeeded();
        mLocationClient.connect();

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Util.noBluetooth(this);
        } else if (!mBluetoothAdapter.isEnabled()) {
            Util.noBluetooth(this);
        }


        // button for moving to the cars position
        ImageButton carButton = (ImageButton) findViewById(R.id.carButton);
        carButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
//                zoomToCar();
            }

        });

        // button for linking a BT device
        linkButton = (Button) findViewById(R.id.linkButton);
        linkButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                startDeviceSelection();
            }

        });


//        mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
//
//            @Override
//            public void onLongPress(MotionEvent e) {
//                setCarPosition(e);
//            }
//
//            @Override
//            public boolean onDoubleTap(MotionEvent e) {
//                setCarPosition(e);
//                return false;
//            }
//
//            /*
//             * On double or long tap, we set the car positions manually (asking first
//             */
//            private void setCarPosition(MotionEvent e) {
//                Location tappedCarLocation;
//
//                Log.d(TAG, "Double Tap event " + (int) e.getX() + " " + (int) e.getY());
//                GeoPoint gp = mapView.getProjection().fromPixels((int) e.getX(), (int) e.getY());
//
//                Log.d(TAG, "Double Tap event " + gp.getLatitudeE6() + " " + gp.getLongitudeE6());
//                tappedCarLocation = new Location("Tapped");
//                tappedCarLocation.setLatitude(gp.getLatitudeE6() / 1E6);
//                tappedCarLocation.setLongitude(gp.getLongitudeE6() / 1E6);
//                tappedCarLocation.setAccuracy(Util.DEFAULT_ACCURACY);
//                Log.d(TAG,
//                        "Double Tap event " + tappedCarLocation.getLatitude() + " " + tappedCarLocation.getLongitude());
//
//                Intent intent = new Intent(MapsActivity.this, SetCarPositionActivity.class);
//                intent.putExtra(Util.EXTRA_LOCATION, tappedCarLocation);
//
//                startActivityForResult(intent, 0);
//            }
//
//        });

//        // call convenience method that zooms map on our location only on starting the app
//        if (firstRun) {
//            myLocationOverlay.runOnFirstFix(new Runnable() {
//
//                public void run() {
//                    if (prefs.getLong(Util.PREF_CAR_TIME, 0) == 0)
//                        zoomToMyLocation();
//                    else
//                        zoomToSeeBoth();
//                }
//
//            });
//            firstRun = false;
//        }

        // we add the car
        prefs = getSharedPreferences("MAPS", Context.MODE_WORLD_READABLE);

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
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
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
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                mMap.getUiSettings().setZoomControlsEnabled(false);
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
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    /**
     * Displays the car in the map
     *
     * @param latitude as an int in 1E6
     * @param longitude as an int in 1E6
     */
    private void addCar(double latitude, double longitude, float accuracy) {
//        if (latitude != 0 && longitude != 0) {
//
//            Log.i(TAG, "Car location: " + latitude + " " + longitude);
//
//            Location loc = new Location("");
//            loc.setLatitude(latitude);
//            loc.setLongitude(longitude);
//            loc.setAccuracy(accuracy);
//            // loc = new GeoPoint(40347990, -3821760);
//
//            if (listOfOverlays.contains(carOverlay)) {
//                listOfOverlays.remove(carOverlay);
//            }
//            LatLng g = new LatLng((int) (latitude * 1E6), (int) (longitude * 1E6));
//            this.myLocationOverlay.setCarPosition(g);
//            carOverlay.setLocation(loc);
//
//            listOfOverlays.add(carOverlay);
//
//        } else {
//            Log.i(TAG, "No car location available");
//        }
//
//        if (listOfOverlays.contains(myLocationOverlay))
//            listOfOverlays.remove(myLocationOverlay);
//
//        listOfOverlays.add(myLocationOverlay);
//        mapView.postInvalidate();
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
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    /**
     * This method shows the Toast when the car icon is pressed, telling the user the parking time
     *
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


}
