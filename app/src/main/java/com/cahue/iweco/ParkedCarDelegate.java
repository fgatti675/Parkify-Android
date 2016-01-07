package com.cahue.iweco;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.parkedCar.CarDetailsFragment;
import com.cahue.iweco.util.ColorUtil;
import com.cahue.iweco.util.GMapV2Direction;
import com.cahue.iweco.util.Tracking;
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
import com.google.maps.android.ui.IconGenerator;

/**
 * Created by francesco on 27.10.2014.
 */
public class ParkedCarDelegate extends AbstractMarkerDelegate implements CameraUpdateRequester {

    public static final String FRAGMENT_TAG = "PARKED_CAR_DELEGATE";
    private static final String TAG = ParkedCarDelegate.class.getSimpleName();
    private static final int MAX_DIRECTIONS_DISTANCE = 2000;

    private static final String ARG_CAR_ID = "car";
    private String carId;
    private Car car;
    private boolean following = false;
    private IconGenerator iconGenerator;
    private OnCarClickedListener carSelectedListener;

    private Integer markerColor;
    private int lightColor;
    /**
     * Map components
     */
    private Marker carMarker;
    private Circle accuracyCircle;
    private DirectionsDelegate directionsDelegate;

    public static String getFragmentTag(String carId) {
        return FRAGMENT_TAG + "." + carId;
    }

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
            this.carSelectedListener = (OnCarClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + OnCarClickedListener.class.getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        iconGenerator = new IconGenerator(getActivity());
        directionsDelegate = new DirectionsDelegate();

        this.carId = getArguments().getString(ARG_CAR_ID);
        Log.i(TAG, "onCreate " + carId);
    }

    @Override
    public void onResume() {
        super.onResume();
        update(true);
    }

    public void update(boolean resetDirections) {

        this.car = CarDatabase.getInstance(getActivity()).findCar(carId);

        if (getMap() == null || !isResumed()) return;

        if (car == null || car.location == null) {
            clear();
            return;
        }

        setUpColors();

        directionsDelegate.setColor(lightColor);
        if (resetDirections)
            directionsDelegate.hide(true);

        doDraw();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        directionsDelegate.setMap(map);
        update(true);
    }


    public void doDraw() {

        Log.i(TAG, "Drawing parked car components");

        clear();
        drawCar();
        drawDirections();

        updateCameraIfFollowing();
    }

    private void clear() {
        if (carMarker != null) carMarker.remove();
        if (accuracyCircle != null) accuracyCircle.remove();
        directionsDelegate.hide(false);
    }


    private void setUpColors() {


        markerColor = car.isOther() ? getActivity().getResources().getColor(R.color.silver) : car.color;
        lightColor = ColorUtil.getSemiTransparent(getResources().getColor(R.color.silver));

        if (markerColor == null) {
            iconGenerator.setTextAppearance(getActivity(), com.google.maps.android.R.style.Bubble_TextAppearance_Light);
            markerColor = getResources().getColor(R.color.theme_accent);
            iconGenerator.setColor(markerColor);
            lightColor = ColorUtil.getSemiTransparent(markerColor);
        } else {
            iconGenerator.setColor(markerColor);
            boolean brightColor = ColorUtil.isBrightColor(markerColor);
            iconGenerator.setTextAppearance(getActivity(),
                    brightColor ?
                            com.google.maps.android.R.style.Bubble_TextAppearance_Dark :
                            com.google.maps.android.R.style.Bubble_TextAppearance_Light);

            if (markerColor != getResources().getColor(R.color.white))
                lightColor = brightColor ? markerColor : ColorUtil.getSemiTransparent(markerColor);
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

        String name = car.isOther() ?
                getActivity().getResources().getText(R.string.other).toString() :
                (car.name != null ? car.name : getActivity().getResources().getText(R.string.car).toString());

        carMarker = getMap().addMarker(new MarkerOptions()
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

        accuracyCircle = getMap().addCircle(circleOptions);

    }


    public void removeCar() {
        clear();
    }


    private void drawDirections() {

        final LatLng carPosition = getCarLatLng();
        final LatLng userPosition = getUserLatLng();

        if (carPosition == null || userPosition == null) {
            return;
        }

        updateTooFar(carPosition, userPosition);

        // don't set if they are too far
        if (isTooFar()) return;

        directionsDelegate.drawDirections(userPosition, carPosition, GMapV2Direction.MODE_WALKING);
    }


    private LatLng getCarLatLng() {
        if (car.location == null) return null;
        return new LatLng(car.location.getLatitude(), car.location.getLongitude());
    }

    @Override
    protected void onUserLocationChanged(Location userLocation) {
        drawDirections();
        updateCameraIfFollowing();
    }

    @Override
    public float getDirectionsMaxDistance() {
        return MAX_DIRECTIONS_DISTANCE;
    }

    @Override
    public void setCameraFollowing(boolean following) {

        Log.v(TAG, "Setting camera following mode to " + following);

        this.following = following;

        updateCameraIfFollowing();

    }

    @Override
    public void onMapResized() {
        updateCameraIfFollowing();
    }


    public boolean updateCameraIfFollowing() {

        if (getMap() == null || !isResumed()) return false;

        if (following) {
            return zoomToSeeBoth();
        }

        return false;
    }

    /**
     * This method zooms to see both user and the car.
     */
    protected boolean zoomToSeeBoth() {

        if (delegateManager == null)
            return false;

        if (!isAdded()) return false;

        Log.v(TAG, "zoomToSeeBoth");

        LatLng carPosition = getCarLatLng();
        LatLng userPosition = getUserLatLng();

        if (carPosition == null || userPosition == null) return false;

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(carPosition)
                .include(userPosition);

        for (LatLng latLng : directionsDelegate.getDirectionPoints())
            builder.include(latLng);

        delegateManager.doCameraUpdate(CameraUpdateFactory.newLatLngBounds(builder.build(), 100), this);

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
            delegateManager.doCameraUpdate(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
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

            carSelectedListener.onCarSelected(car);

            activate();

            Tracking.sendEvent(Tracking.CATEGORY_MAP, Tracking.ACTION_CAR_SELECTED, Tracking.LABEL_SELECTED_FROM_MARKER);

            return true;
        } else {
            setCameraFollowing(false);
        }
        return false;
    }

    public void activate() {
        setCameraFollowing(true);
        detailsViewManager.setDetailsFragment(CarDetailsFragment.newInstance(carId));
    }

    public boolean isFollowing() {
        return following;
    }

}
