package com.bahpps.cahue;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

@Deprecated
public class MainActivity extends MapActivity {

	protected static final String TAG = "Main";

	private static final int INFO_DIALOG = 0;

	// Map objects
	private MapView mapView;
	private FixedMyLocationOverlay myLocationOverlay;
	private List<Overlay> listOfOverlays;
	private CarOverlay carOverlay;

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;

	private SharedPreferences prefs;

	private Button linkButton;

	// TODO: remove static variable?
	private static boolean firstRun = true;

	private GestureDetector mGestureDetector;

	/**
	 * If we get a new car position while we are running the app, we update the map
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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// main.xml contains a MapView
		setContentView(R.layout.main);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Util.noBluetooth(this);
		} else if (!mBluetoothAdapter.isEnabled()) {
			Util.noBluetooth(this);
		}

		// button for moving to the users position
		ImageButton userButton = (ImageButton) findViewById(R.id.userButton);
		userButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				zoomToMyLocation();
			}

		});

		// button for moving to the cars position
		ImageButton carButton = (ImageButton) findViewById(R.id.carButton);
		carButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				zoomToCar();
			}

		});

		// button for linking a BT device
		linkButton = (Button) findViewById(R.id.linkButton);
		linkButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				startDeviceSelection();
			}

		});

		// extract MapView from layout
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(false);
		mapView.setOnTouchListener(new View.OnTouchListener() {
			// we bypass the touch event
			public boolean onTouch(View v, MotionEvent event) {
				return mGestureDetector.onTouchEvent(event);
			}
		});

		mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {

			@Override
			public void onLongPress(MotionEvent e) {
				setCarPosition(e);
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				setCarPosition(e);
				return false;
			}

			/*
			 * On double or long tap, we set the car positions manually (asking first
			 */
			private void setCarPosition(MotionEvent e) {
				Location tappedCarLocation;

				Log.d(TAG, "Double Tap event " + (int) e.getX() + " " + (int) e.getY());
				GeoPoint gp = mapView.getProjection().fromPixels((int) e.getX(), (int) e.getY());

				Log.d(TAG, "Double Tap event " + gp.getLatitudeE6() + " " + gp.getLongitudeE6());
				tappedCarLocation = new Location("Tapped");
				tappedCarLocation.setLatitude(gp.getLatitudeE6() / 1E6);
				tappedCarLocation.setLongitude(gp.getLongitudeE6() / 1E6);
				tappedCarLocation.setAccuracy(Util.DEFAULT_ACCURACY);
				Log.d(TAG,
						"Double Tap event " + tappedCarLocation.getLatitude() + " " + tappedCarLocation.getLongitude());

				Intent intent = new Intent(MainActivity.this, SetCarPositionActivity.class);
				intent.putExtra(Util.EXTRA_LOCATION, tappedCarLocation);

				startActivityForResult(intent, 0);
			}

		});

		listOfOverlays = mapView.getOverlays();

		// create an overlay that shows our current location
		myLocationOverlay = new FixedMyLocationOverlay(this, mapView);

		// mapView.getOverlays().add(new MapGestureDetectorOverlay(this));

		// add this overlay to the MapView and refresh it
		carOverlay = new CarOverlay(this);

		// call convenience method that zooms map on our location only on starting the app
		if (firstRun) {
			myLocationOverlay.runOnFirstFix(new Runnable() {

				public void run() {
					if (prefs.getLong(Util.PREF_CAR_TIME, 0) == 0)
						zoomToMyLocation();
					else
						zoomToSeeBoth();
				}

			});
			firstRun = false;
		}

		// we add the car
		prefs = Util.getSharedPreferences(this);

		// show help dialong only on first run of the app
		boolean dialogShown = prefs.getBoolean(Util.PREF_DIALOG_SHOWN, false);
		if (!dialogShown) {
			showDialog(INFO_DIALOG);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// when our activity resumes, we want to register for location updates
		registerReceiver(carPosReceiver, new IntentFilter(Util.INTENT_NEW_CAR_POS));

		// and enable positioning and compass
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.enableCompass();

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
		addCar(latitude, longitude, accuracy);

	}

	@Override
	protected void onPause() {
		super.onPause();
		// when our activity pauses, we want to remove listening for location updates
		myLocationOverlay.disableMyLocation();
		myLocationOverlay.disableCompass();
		unregisterReceiver(carPosReceiver);
	}

	/**
	 * This method zooms to the user's location with a zoom level of 18.
	 */
	private void zoomToMyLocation() {
		GeoPoint myLocationGeoPoint = myLocationOverlay.getMyLocation();
		if (myLocationGeoPoint != null) {
			mapView.getController().animateTo(myLocationGeoPoint);
			mapView.getController().setZoom(18);
		} else {
			Location loc = myLocationOverlay.getLastFix();
			if (loc != null) {
				myLocationGeoPoint = new GeoPoint((int) (loc.getLatitude() * 1E6), (int) (loc.getLongitude() * 1E6));

				mapView.getController().animateTo(myLocationGeoPoint);
				mapView.getController().setZoom(10);
			} else {
				Util.createToast(this, getString(R.string.position_waiting), Toast.LENGTH_SHORT);
			}
		}
	}

	/**
	 * This method zooms to see both user and the car.
	 */
	protected void zoomToSeeBoth() {
		int minLat = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLon = Integer.MIN_VALUE;

		ArrayList<GeoPoint> items = new ArrayList<GeoPoint>();
		Location carLoc = this.carOverlay.getLocation();
		if (carLoc != null) {
			items.add(myLocationOverlay.getMyLocation());
			GeoPoint carLocationGeoPoint = new GeoPoint((int) (carLoc.getLatitude() * 1E6),
					(int) (carLoc.getLongitude() * 1E6));
			items.add(carLocationGeoPoint);
		} else {
			zoomToMyLocation();
		}
		for (GeoPoint item : items)
		{
			int lat = item.getLatitudeE6();
			int lon = item.getLongitudeE6();

			maxLat = Math.max(lat, maxLat);
			minLat = Math.min(lat, minLat);
			maxLon = Math.max(lon, maxLon);
			minLon = Math.min(lon, minLon);
		}
		double fitFactor = 1.5;
		mapView.getController().zoomToSpan((int) (Math.abs(maxLat - minLat) * fitFactor),
				(int) (Math.abs(maxLon - minLon) * fitFactor));
		mapView.getController().animateTo(new GeoPoint((maxLat + minLat) / 2,
				(maxLon + minLon) / 2));
	}

	/**
	 * This method zooms to the car's location.
	 */
	private void zoomToCar() {
		Location loc = carOverlay.getLocation();
		if (loc != null) {
			GeoPoint carLocationGeoPoint = new GeoPoint((int) (loc.getLatitude() * 1E6),
					(int) (loc.getLongitude() * 1E6));
			mapView.getController().animateTo(carLocationGeoPoint);
			mapView.getController().setZoom(18);

			Util.showCarTimeToast(this);
		} else {
			Util.createToast(this, getString(R.string.car_not_found), Toast.LENGTH_SHORT);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			String name = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_NAME);

			String msg = String.format(getString(R.string.bt_linked_to), name);
			Util.createToast(this, msg, Toast.LENGTH_LONG);

		}
	}

	/**
	 * Displays the car in the map
	 * 
	 * @param latitude as an int in 1E6
	 * @param longitude as an int in 1E6
	 */
	private void addCar(double latitude, double longitude, float accuracy) {
		if (latitude != 0 && longitude != 0) {

			Log.d("Main", "Car location: " + latitude + " " + longitude);

			Location loc = new Location("");
			loc.setLatitude(latitude);
			loc.setLongitude(longitude);
			loc.setAccuracy(accuracy);
			// loc = new GeoPoint(40347990, -3821760);

			if (listOfOverlays.contains(carOverlay)) {
				listOfOverlays.remove(carOverlay);
			}
			GeoPoint g = new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
			this.myLocationOverlay.setCarPosition(g);
			carOverlay.setLocation(loc);

			listOfOverlays.add(carOverlay);

			myLocationOverlay.enableCompass();

		} else {
			Log.w("Main", "No car location available");
			myLocationOverlay.disableCompass();
		}

		if (listOfOverlays.contains(myLocationOverlay))
			listOfOverlays.remove(myLocationOverlay);

		listOfOverlays.add(myLocationOverlay);
		mapView.postInvalidate();
	}

	/**
	 * Show menu method
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// inflate from xml
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		menu.getItem(0).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				startDeviceSelection();
				return false;
			}
		});
		
		menu.getItem(1).setOnMenuItemClickListener(new OnMenuItemClickListener() {
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
			startActivityForResult(new Intent(MainActivity.this, DeviceListActivity.class), 0);
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case INFO_DIALOG:
			Dialog d = new InfoDialog(this);
			prefs.edit().putBoolean(Util.PREF_DIALOG_SHOWN, true).commit();
			return d;
		}

		return null;
	}

}