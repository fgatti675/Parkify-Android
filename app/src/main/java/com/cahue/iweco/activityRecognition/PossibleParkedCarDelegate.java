package com.cahue.iweco.activityrecognition;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;

import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.CameraUpdateRequester;
import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.OnCarClickedListener;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

/**
 * Delegate to show a marker where a car might be parked, based on activity recognition
 */
public class PossibleParkedCarDelegate extends AbstractMarkerDelegate implements OnCarClickedListener, PossibleSetCarDetailsFragment.OnPossibleSpotDeletedListener {

    public static final String FRAGMENT_TAG = "POSSIBLE_PARKED_CAR_DELEGATE";

    private static final String ARG_SPOT = "spot";

    @Nullable
    private ParkingSpot spot;

    @Nullable
    private Marker marker;

    private IconGenerator iconGenerator;
    private boolean following;
    private CarDatabase database;

    @NonNull
    public static String getFragmentTag(@NonNull ParkingSpot spot) {
        return FRAGMENT_TAG + "." + spot.time.getTime();
    }

    @NonNull
    public static PossibleParkedCarDelegate newInstance(ParkingSpot spot) {
        PossibleParkedCarDelegate fragment = new PossibleParkedCarDelegate();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SPOT, spot);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = CarDatabase.getInstance(getActivity());
        this.spot = getArguments().getParcelable(ARG_SPOT);
        iconGenerator = new IconGenerator(getActivity());
        LayoutInflater myInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = myInflater.inflate(R.layout.marker_possible_view, null, false);
        iconGenerator.setBackground(null);
        iconGenerator.setContentView(contentView);
    }

    @Override
    protected void onMapReady(GoogleMap map) {
        super.onMapReady(map);
        doDraw();
    }

    public void doDraw() {

        if (!isMapReady() || !isResumed()) return;

        clearMarker();

        marker = getMap().addMarker(new MarkerOptions()
                .position(spot.getLatLng())
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon())));

    }

    private void centerCameraOnMarker() {

        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .zoom(17)
                .target(spot.getLatLng())
                .build());

        delegateManager.doCameraUpdate(cameraUpdate, this);
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        if (marker.equals(this.marker)) {
            activate();
            return true;
        }
        return false;
    }

    @Override
    protected void onUserLocationChanged(Location userLocation) {
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {
    }

    private boolean isDisplayed() {
        DetailsFragment detailsFragment = detailsViewManager.getDetailsFragment();
        return detailsFragment != null && detailsFragment instanceof PossibleSetCarDetailsFragment;
    }


    private void clearMarker() {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
    }

    @Override
    public void setCameraFollowing(boolean following) {
        this.following = following;
        if (following) centerCameraOnMarker();
    }

    @Override
    public void onMapResized() {
        if (isDisplayed() && following)
            centerCameraOnMarker();
    }

    @Override
    public void onCarSelected(@NonNull Car car) {
        clearMarker();
        CarsSync.updateCarFromPossibleSpot(database, getActivity(), car, spot);
        detailsViewManager.hideDetails();
    }

    public void activate() {
        isActive = true;
        centerCameraOnMarker();
        detailsViewManager.setDetailsFragment(this, PossibleSetCarDetailsFragment.newInstance(spot, getTag()));
        setCameraFollowing(true);
    }

    @Override
    public void onPossibleSpotDeleted(@NonNull ParkingSpot spot) {
        database.removeParkingSpot(spot);
        if (marker != null)
            marker.remove();
        detailsViewManager.hideDetails();
    }
}
