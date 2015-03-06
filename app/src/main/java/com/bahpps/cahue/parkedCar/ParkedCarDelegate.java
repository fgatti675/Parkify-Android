package com.bahpps.cahue.parkedCar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import com.bahpps.cahue.AbstractMarkerDelegate;
import com.bahpps.cahue.CameraUpdateListener;
import com.bahpps.cahue.R;
import com.bahpps.cahue.cars.Car;
import com.bahpps.cahue.util.FetchAddressIntentService;
import com.bahpps.cahue.cars.database.CarDatabase;
import com.bahpps.cahue.cars.CarsSync;
import com.bahpps.cahue.util.GMapV2Direction;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by francesco on 27.10.2014.
 */
public class ParkedCarDelegate extends AbstractMarkerDelegate {

    /**
     * Interface for components listening for the marker click event in the map
     */
    public interface CarSelectedListener {
        public void onCarClicked(Car car);
    }

    private static final String TAG = ParkedCarDelegate.class.getSimpleName();

    private static final String ARG_CAR = "car";
    private static final int MAX_DIRECTIONS_DISTANCE = 2000;
    private static final int DIRECTIONS_EXPIRY = 30000;

    private Car car;

    // too far from the car to calculate directions
    private boolean tooFar = false;

    private boolean following = false;

    private IconGenerator iconGenerator;

    private CameraUpdateListener cameraUpdateListener;
    private GoogleMap mMap;
    private CarSelectedListener carSelectedListener;

    private Location userLocation;

    private int lightColor;

    /**
     * Map components
     */
    private Marker carMarker;
    private Circle accuracyCircle;
    private Polyline directionsPolyline;

    /**
     * Actual lines representing the directions PolyLine
     */
    private List<LatLng> directionPoints;

    private AsyncTask<Object, Object, Document> directionsAsyncTask;

    /**
     * Directions delegate
     */
    private GMapV2Direction directionsDelegate;

    private Date lastDirectionsUpdate;
    private CarDatabase carDatabase;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CarDetailsFragment.
     */
    public static ParkedCarDelegate newInstance(Car car) {
        ParkedCarDelegate fragment = new ParkedCarDelegate();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CAR, car);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            this.cameraUpdateListener = (CameraUpdateListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement CameraUpdateListener");
        }

        try {
            this.carSelectedListener = (CarSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement CarSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        directionPoints = new ArrayList<LatLng>();
        iconGenerator = new IconGenerator(getActivity());
        directionsDelegate = new GMapV2Direction();

        carDatabase = CarDatabase.getInstance(getActivity());

        this.car = getArguments().getParcelable(ARG_CAR);
    }

    @Override
    public void onResume() {
        super.onResume();
        doDraw();
    }

    public void setCar(Car car) {

        this.car = car;

        if (!isResumed()) return;

        directionPoints.clear();

        if (car == null || car.location == null) {
            return;
        }

        fetchDirections(true);
        doDraw();
    }

    @Override
    public void onMapReady(GoogleMap map) {

        this.mMap = map;

        setCar(this.car);
    }

    public void onLocationChanged(Location userLocation) {
        this.userLocation = userLocation;

        if (lastDirectionsUpdate == null || System.currentTimeMillis() - lastDirectionsUpdate.getTime() > DIRECTIONS_EXPIRY)
            fetchDirections(false);

        updateCameraIfFollowing();

    }

    public void doDraw() {
        if (mMap == null || !isResumed()) return;

        Log.i(TAG, "Drawing parked car components");
        setUpColors();
        clear();
        drawCar();
        drawDirections();
        updateCameraIfFollowing();
    }

    private void clear() {
        if (carMarker != null) carMarker.remove();
        if (accuracyCircle != null) accuracyCircle.remove();
        if (directionsPolyline != null) directionsPolyline.remove();
    }

    @Override
    public void onZoomToMyLocation() {
        setFollowing(false);
    }

    private void setUpColors() {

        lightColor = getSemiTransparent(getResources().getColor(R.color.car_silver));

        if (car.color == null) {
            iconGenerator.setStyle(IconGenerator.STYLE_DEFAULT);
        } else {
            iconGenerator.setColor(car.color);
            boolean brightColor = isBrightColor(car.color);
            iconGenerator.setTextAppearance(getActivity(),
                    brightColor ?
                            com.google.maps.android.R.style.Bubble_TextAppearance_Dark :
                            com.google.maps.android.R.style.Bubble_TextAppearance_Light);

            if (car.color != getResources().getColor(R.color.car_white))
                lightColor = brightColor ? car.color : getSemiTransparent(car.color);
        }
    }

    /**
     * Displays the car in the map
     */
    private void drawCar() {

        if (car == null || car.location == null) {
            return;
        }

        Log.i(TAG, "Setting car in map: " + car);

        iconGenerator.setContentRotation(-90);

        // Uses a colored icon.
        LatLng carLatLng = getCarLatLng();

        String name = car.name;
        if (name == null)
            name = getActivity().getResources().getText(R.string.car).toString();

        carMarker = mMap.addMarker(new MarkerOptions()
                .position(carLatLng)
                .snippet("")
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon(name.toUpperCase())))
                .anchor(iconGenerator.getAnchorU(), iconGenerator.getAnchorV()));

        CircleOptions circleOptions = new CircleOptions()
                .center(carLatLng)   //set center
                .radius(car.location.getAccuracy())   //set radius in meters
                .fillColor(lightColor)
                .strokeColor(lightColor)
                .strokeWidth(0);

        accuracyCircle = mMap.addCircle(circleOptions);

    }

    private int getSemiTransparent(int color) {
        return Color.argb(100, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static boolean isBrightColor(int color) {
        if (android.R.color.transparent == color)
            return true;

        boolean rtnValue = false;

        int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};

        int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1]
                * rgb[1] * .691 + rgb[2] * rgb[2] * .068);

        // color is light
        if (brightness >= 200) {
            rtnValue = true;
        }

        return rtnValue;
    }


    public Car getCar() {
        return car;
    }

    public void removeCar() {
        CarsSync.clearLocation(carDatabase, getActivity(), car);
        clear();
    }

    private void drawDirections() {

        if (car == null || car.location == null) {
            return;
        }

        Log.d(TAG, "Drawing directions");

        PolylineOptions rectLine = new PolylineOptions().width(10).color(lightColor);

        for (int i = 0; i < directionPoints.size(); i++) {
            rectLine.add(directionPoints.get(i));
        }

        directionsPolyline = mMap.addPolyline(rectLine);

    }


    private void fetchDirections(boolean restart) {

        final LatLng carPosition = getCarLatLng();
        final LatLng userPosition = getUserLatLng();

        directionPoints.clear();

        if (carPosition == null || userPosition == null) {
            return;
        }

        updateTooFar(carPosition, userPosition);

        // don't set if they are too far
        if (isTooFar()) return;

        /**
         * Cancel if something is going on
         */
        if (directionsAsyncTask != null && directionsAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            if (restart)
                directionsAsyncTask.cancel(true);
            else
                return;
        }

        Log.d(TAG, "Fetching directions");

        directionsAsyncTask = new AsyncTask<Object, Object, Document>() {

            @Override
            protected Document doInBackground(Object[] objects) {
                Document doc = directionsDelegate.getDocument(userPosition, carPosition, GMapV2Direction.MODE_WALKING);
                return doc;
            }

            @Override
            protected void onPostExecute(Document doc) {
                lastDirectionsUpdate = new Date();
                directionPoints.clear();
                directionPoints.addAll(directionsDelegate.getDirection(doc));
                doDraw();
            }

        };
        directionsAsyncTask.execute();
    }

    private void updateTooFar(LatLng carPosition, LatLng userPosition) {

        if (carPosition == null || userPosition == null) {
            return;
        }

        float distances[] = new float[3];
        Location.distanceBetween(
                carPosition.latitude,
                carPosition.longitude,
                userPosition.latitude,
                userPosition.longitude,
                distances);

        tooFar = distances[0] > MAX_DIRECTIONS_DISTANCE;
    }

    private LatLng getCarLatLng() {
        if (car.location == null) return null;
        return new LatLng(car.location.getLatitude(), car.location.getLongitude());
    }


    public void setFollowing(boolean following) {

        if (this.following != following)
            Log.i(TAG, "Setting camera following mode to " + following);

        this.following = following;

        updateCameraIfFollowing();

    }


    private LatLng getUserLatLng() {
        if (userLocation == null) return null;
        return new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
    }


    public boolean updateCameraIfFollowing() {

        if (mMap == null) return false;

        if (following) {
            return zoomToSeeBoth();
        }

        return false;
    }

    /**
     * This method zooms to see both user and the car.
     */
    protected boolean zoomToSeeBoth() {

        if (cameraUpdateListener == null)
            return false;

        Log.v(TAG, "zoomToSeeBoth");

        LatLng carPosition = getCarLatLng();
        LatLng userPosition = getUserLatLng();

        if (carPosition == null || userPosition == null) return false;

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(carPosition)
                .include(userPosition);

        for (LatLng latLng : directionPoints)
            builder.include(latLng);

        cameraUpdateListener.onCameraUpdateRequest(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

        return true;
    }

    /**
     * This method zooms to the car's location.
     */
    private void zoomToCar() {

        Log.d(TAG, "zoomToCar");

        if (car == null) return;

        LatLng loc = getCarLatLng();

        if (loc != null) {
            cameraUpdateListener.onCameraUpdateRequest(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(loc)
                    .zoom(15.5f)
                    .build()));
        }
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition, boolean justFinishedAnimating) {
        if (!justFinishedAnimating)
            following = false;
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.equals(carMarker)) {
            setFollowing(true);
            carSelectedListener.onCarClicked(car);
        } else {
            setFollowing(false);
        }
        return false;
    }

    public boolean isFollowing() {
        return following;
    }

    public boolean isTooFar() {
        return tooFar;
    }
}
