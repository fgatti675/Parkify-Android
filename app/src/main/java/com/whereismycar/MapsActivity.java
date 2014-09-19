package com.whereismycar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.whereismycar.util.BluetoothDetector;
import com.whereismycar.util.CarLocationManager;
import com.whereismycar.util.GMapV2Direction;
import com.whereismycar.util.Util;
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

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Calendar;

public class MapsActivity extends Activity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMapLongClickListener {

    protected static final String TAG = "Maps";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(2000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

    private GoogleApiClient googleApiClient;

    private Marker carMarker;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    private SharedPreferences prefs;

    private Button linkButton;

    private IconGenerator iconFactory;

    private static boolean firstRun = true;

    /**
     * Directions delegate
     */
    private GMapV2Direction md;

    /**
     * Actual lines representing the directions
     */
    private Polyline directions;
    /**
     * If we get a new car position while we are using the app, we update the map
     */
    private final BroadcastReceiver carPosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Location location = (Location) intent.getExtras().get(CarLocationManager.INTENT_POSITION);
            if (location != null) {
                Log.i(TAG, "Location received: " + location);
                setCar(location);
                addDirections();
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        setUpMapIfNeeded();
        setUpLocationClientIfNeeded();

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

        // try to reuse map
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


        // show help dialog only on first run of the app
        boolean dialogShown = prefs.getBoolean(Util.PREF_DIALOG_SHOWN, false);
        if (!dialogShown) {
            showHelpDialog();
        }


        // we add the car on the stored position
        Location carLocation = CarLocationManager.getStoredLocation(this);
        setCar(carLocation);


    }

    private void addDirections() {
        final LatLng carPosition = getCarPosition();

        if (carPosition == null) {
            return;
        }

        Log.i(TAG, "addDirections");

        new AsyncTask<Object, Object, Document>() {
            @Override
            protected Document doInBackground(Object[] objects) {

                Document doc = md.getDocument(getUserPosition(), carPosition, GMapV2Direction.MODE_WALKING);

                return doc;
            }

            @Override
            protected void onPostExecute(Document doc) {
                ArrayList<LatLng> directionPoint = md.getDirection(doc);
                PolylineOptions rectLine = new PolylineOptions().width(3).color(Color.rgb(242, 69, 54));

                for (int i = 0; i < directionPoint.size(); i++) {
                    rectLine.add(directionPoint.get(i));
                }
                directions = mMap.addPolyline(rectLine);
            }
        }.execute();

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
        unregisterReceiver(carPosReceiver);
        if (googleApiClient != null) {
            googleApiClient.disconnect();
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
        mMap.setOnMapLongClickListener(this);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
    }

    /**
     * Displays the car in the map
     */
    private void setCar(Location carLocation) {

        // remove previous
        if (carMarker != null) {
            mMap.clear();
        }

        if (carLocation == null) return;

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
            default:
                return super.onOptionsItemSelected(item);
        }
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

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                REQUEST,
                this);

        // call convenience method that zooms map on our location only on starting the app

        if (prefs.getLong(Util.PREF_CAR_TIME, 0) == 0) {
            zoomToMyLocation();
        } else {
            zoomToSeeBoth();
        }

        addDirections();

        firstRun = false;
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

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
        if (mMap.getCameraPosition().zoom > 15.5f)
            zoomToCar();

    }


    private void zoomToMyLocation() {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(getUserPosition())
                .zoom(15.5f)
                .build()), null);
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

    private LatLng getUserPosition() {
        Location userLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        return new LatLng(userLastLocation.getLatitude(), userLastLocation.getLongitude());
    }

    private LatLng getCarPosition() {
        if (carMarker == null) return null;
        return carMarker.getPosition();
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
