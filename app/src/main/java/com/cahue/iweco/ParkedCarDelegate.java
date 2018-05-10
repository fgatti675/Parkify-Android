package com.cahue.iweco;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cahue.iweco.model.Car;
import com.cahue.iweco.parkedcar.CarDetailsFragment;
import com.cahue.iweco.util.ColorUtil;
import com.cahue.iweco.util.GMapV2Direction;
import com.cahue.iweco.util.Tracking;
import com.google.android.gms.maps.CameraUpdate;
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
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.maps.android.ui.IconGenerator;

/**
 * Created by francesco on 27.10.2014.
 */
public class ParkedCarDelegate extends AbstractMarkerDelegate implements CameraUpdateRequester {

    public static final String FRAGMENT_TAG = "PARKED_CAR_DELEGATE";
    private static final String TAG = ParkedCarDelegate.class.getSimpleName();
    private static final int MAX_DIRECTIONS_DISTANCE = 2000;

    private static final String ARG_CAR_ID = "car";

    @NonNull
    private String carId;

    @Nullable
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
    private FirebaseFirestore firebaseFirestore;
    private ListenerRegistration carListener;

    @NonNull
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
    @NonNull
    public static ParkedCarDelegate newInstance(String carId) {
        ParkedCarDelegate fragment = new ParkedCarDelegate();
        Bundle args = new Bundle();
        args.putString(ARG_CAR_ID, carId);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onAttach(@NonNull Activity context) {
        super.onAttach(context);

        try {
            this.carSelectedListener = (OnCarClickedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + OnCarClickedListener.class.getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        iconGenerator = new IconGenerator(getActivity());
        directionsDelegate = new DirectionsDelegate();

        this.carId = getArguments().getString(ARG_CAR_ID);

        firebaseFirestore = FirebaseFirestore.getInstance();
        carListener = firebaseFirestore.collection("cars")
                .document(carId)
                .addSnapshotListener((snapshot, e) -> {
                    if(snapshot == null) return; // this may happen on logout
                    update(Car.fromFirestore(snapshot), true);
                    if (isActive) activate();
                    else Log.v(TAG, "car update: not active");
                });

        Log.i(TAG, "onCreate " + carId);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isActive) activate();
        else Log.v(TAG, "onResume: not active");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        carListener.remove();
    }

    public void update(Car car, boolean resetDirections) {

        this.car = car;

        if (car == null || !isMapReady() || !isResumed()) return;

        if (car.location == null) {
            clear();
            return;
        }

        setUpColors();

        directionsDelegate.setColor(markerColor);
        if (resetDirections)
            directionsDelegate.hide(true);

        doDraw();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        directionsDelegate.setMap(map);
        update(car, true);
        if (isActive)
            activate();
        else Log.v(TAG, "onMapReady: not active");
    }


    public void doDraw() {

        Log.i(TAG, "Drawing parked car components");

        clear();
        drawCar();

        updateCameraIfFollowing();
    }

    private void clear() {
        if (carMarker != null) carMarker.remove();
        if (accuracyCircle != null) accuracyCircle.remove();
        directionsDelegate.hide(false);
    }


    private void setUpColors() {

        markerColor = car.isOther() ?
                getActivity().getResources().getColor(R.color.silver)
                : (car.color == null ? getResources().getColor(R.color.theme_accent) : car.color);
        lightColor = ColorUtil.getSemiTransparent(getResources().getColor(R.color.silver));

        if (car.color == null) {
            iconGenerator.setTextAppearance(getActivity(), com.google.maps.android.R.style.amu_Bubble_TextAppearance_Light);
            iconGenerator.setColor(markerColor);
            lightColor = ColorUtil.getSemiTransparent(markerColor);
        } else {
            iconGenerator.setColor(markerColor);
            boolean brightColor = ColorUtil.isBrightColor(markerColor);
            iconGenerator.setTextAppearance(getActivity(),
                    brightColor ?
                            com.google.maps.android.R.style.amu_Bubble_TextAppearance_Dark :
                            com.google.maps.android.R.style.amu_Bubble_TextAppearance_Light);

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
        isActive = false;
        clear();
    }


    private void drawDirections() {
        if (!isActive) return;

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
        updateCameraIfFollowing();
    }

    @Override
    protected void onActiveStatusChanged(boolean active) {
        if (active) {
            drawDirections();
        } else {
            directionsDelegate.hide(true);
        }
    }

    @Override
    public float getDirectionsMaxDistance() {
        return MAX_DIRECTIONS_DISTANCE;
    }

    @Override
    public void setCameraFollowing(boolean following) {

        Log.d(TAG, "Setting camera following mode to " + following);

        this.following = following;

        updateCameraIfFollowing();

    }

    @Override
    public void onMapResized() {
        updateCameraIfFollowing();
    }

    private boolean updateCameraIfFollowing() {

        if (!isMapReady() || !isResumed()) return false;

        if (following) {
            return zoomToSeeBoth();
        }

        return false;
    }

    /**
     * This method zooms to see both user and the car.
     */
    private boolean zoomToSeeBoth() {

        if (delegateManager == null)
            return false;

        if (!isAdded()) return false;

        Log.v(TAG, "zoomToSeeBoth");

        if (car.location == null || userLocation == null) return false;

        if (car.location.distanceTo(userLocation) < 30) {
            zoomToCar();
        } else {

            LatLng carPosition = getCarLatLng();
            LatLng userPosition = getUserLatLng();

            LatLngBounds.Builder builder = new LatLngBounds.Builder()
                    .include(carPosition)
                    .include(userPosition);

            for (LatLng latLng : directionsDelegate.getDirectionPoints())
                builder.include(latLng);

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(builder.build(), 120);
            delegateManager.doCameraUpdate(cameraUpdate, this);
        }
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
                            .zoom(18f)
                            .build()),
                    this);
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        if (marker.equals(carMarker)) {

            carSelectedListener.onCarSelected(car);

            activate();

            Tracking.sendEvent(Tracking.CATEGORY_MAP, Tracking.ACTION_CAR_SELECTED, Tracking.LABEL_SELECTED_FROM_MARKER);

            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
            Bundle bundle = new Bundle();
            bundle.putString("car", car.id);
            firebaseAnalytics.logEvent("car_clicked_marker", bundle);

            return true;
        } else {
            setCameraFollowing(false);
        }
        return false;
    }

    public void activate() {
        isActive = true;

        if (car == null) {
            return;
        }

        Log.d(TAG, "Activating ParkedCarDelegate " + car);

        drawDirections();
        setCameraFollowing(true);
        detailsViewManager.setDetailsFragment(this, CarDetailsFragment.newInstance(carId));
    }

    public boolean isFollowing() {
        return following;
    }

}
