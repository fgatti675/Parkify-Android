package com.cahue.iweco.parkedCar;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.CameraUpdateRequester;
import com.cahue.iweco.CameraManager;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.util.ColorUtil;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.util.GMapV2Direction;
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
public class ParkedCarDelegate extends AbstractMarkerDelegate implements CameraUpdateRequester {

    /**
     * Interface for components listening for the marker click event in the map
     */
    public interface CarSelectedListener {
        public void onCarClicked(String carId);
    }

    private static final String TAG = ParkedCarDelegate.class.getSimpleName();

    private static final String ARG_CAR_ID = "car";
    private static final int MAX_DIRECTIONS_DISTANCE = 2000;
    private static final int DIRECTIONS_EXPIRY = 30000;

    private String carId;
    private Car car;

    // too far from the car to calculate directions
    private boolean tooFar = false;

    private boolean following = false;

    private IconGenerator iconGenerator;

    private CameraManager cameraManager;
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


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param carId
     * @return A new instance of fragment CarDetailsFragment.
     */
    public static ParkedCarDelegate newInstance(String carId) {
        ParkedCarDelegate fragment = new ParkedCarDelegate();
        Bundle args = new Bundle();
        args.putString(ARG_CAR_ID, carId);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            this.cameraManager = (CameraManager) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + CameraManager.class.getName());
        }

        try {
            this.carSelectedListener = (CarSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + CarSelectedListener.class.getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        directionPoints = new ArrayList<LatLng>();
        iconGenerator = new IconGenerator(getActivity());
        directionsDelegate = new GMapV2Direction();

        this.carId = getArguments().getString(ARG_CAR_ID);
        Log.i(TAG, "onCreate " + carId);
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
        Log.i(TAG, "c " + carId);
    }

    public void update() {

        this.car = CarDatabase.getInstance(getActivity()).find(carId);

        if (!isResumed()) return;

        directionPoints.clear();

        if (car == null || car.location == null) {
            clear();
            return;
        }

        fetchDirections(true);
        doDraw();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.mMap = map;
        update();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mMap = null;
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


    private void setUpColors() {

        lightColor = ColorUtil.getSemiTransparent(getResources().getColor(R.color.car_silver));

        if (car.color == null) {
            iconGenerator.setTextAppearance(getActivity(), com.google.maps.android.R.style.Bubble_TextAppearance_Light);
            int color = getResources().getColor(R.color.theme_accent);
            iconGenerator.setColor(color);
            lightColor = ColorUtil.getSemiTransparent(color);
        } else {
            iconGenerator.setColor(car.color);
            boolean brightColor = ColorUtil.isBrightColor(car.color);
            iconGenerator.setTextAppearance(getActivity(),
                    brightColor ?
                            com.google.maps.android.R.style.Bubble_TextAppearance_Dark :
                            com.google.maps.android.R.style.Bubble_TextAppearance_Light);

            if (car.color != getResources().getColor(R.color.car_white))
                lightColor = brightColor ? car.color : ColorUtil.getSemiTransparent(car.color);
        }
    }

    /**
     * Displays the car in the map
     */
    private void drawCar() {

        if (car == null) {
            Log.e(TAG, "Car is null");
            return;
        }

        if (car.location == null) {
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


    public Car getCar() {
        return car;
    }

    public void removeCar() {
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


    @Override
    public void onLocationChanged(Location userLocation) {
        this.userLocation = userLocation;

        if (lastDirectionsUpdate == null || System.currentTimeMillis() - lastDirectionsUpdate.getTime() > DIRECTIONS_EXPIRY)
            fetchDirections(false);

        updateCameraIfFollowing();
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

        if (mMap == null || !isResumed()) return false;

        if (following) {
            return zoomToSeeBoth();
        }

        return false;
    }

    /**
     * This method zooms to see both user and the car.
     */
    protected boolean zoomToSeeBoth() {

        if (cameraManager == null)
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

        cameraManager.onCameraUpdateRequest(CameraUpdateFactory.newLatLngBounds(builder.build(), 100), this);

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
            cameraManager.onCameraUpdateRequest(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                            .target(loc)
                            .zoom(15.5f)
                            .build()),
                    this);
        }
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {
        if (requester != this)
            following = false;
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.equals(carMarker)) {
            carSelectedListener.onCarClicked(carId);
            setFollowing(true);
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
