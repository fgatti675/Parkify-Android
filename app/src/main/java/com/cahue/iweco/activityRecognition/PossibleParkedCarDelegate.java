package com.cahue.iweco.activityRecognition;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
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
public class PossibleParkedCarDelegate extends AbstractMarkerDelegate implements OnCarClickedListener {

    public static final String FRAGMENT_TAG = "POSSIBLE_PARKED_CAR_DELEGATE";

    private static final String ARG_SPOT = "spot";

    ParkingSpot spot;

    Marker marker;

    private IconGenerator iconGenerator;
    private boolean following;

    public static String getFragmentTag(ParkingSpot spot) {
        return FRAGMENT_TAG + "." + spot.time.getTime();
    }

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
        this.spot = getArguments().getParcelable(ARG_SPOT);
        iconGenerator = new IconGenerator(getActivity());
        LayoutInflater myInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = myInflater.inflate(R.layout.marker_possible_view, null, false);
        iconGenerator.setBackground(null);
        iconGenerator.setContentView(contentView);
    }

    @Override
    protected void onMapReady(GoogleMap mMap) {
        super.onMapReady(mMap);
        doDraw();
    }

    @Override
    public void doDraw() {

        if (!isMapReady() || !isResumed()) return;

        clearMarker();

        marker = getMap().addMarker(new MarkerOptions()
                .position(spot.getLatLng())
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon())));

    }

    private void centerCameraOnMarker() {

        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .zoom(16)
                .target(spot.getLatLng())
                .build());

        delegateManager.doCameraUpdate(cameraUpdate, this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
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
    public void onCarSelected(Car car) {
        clearMarker();
        CarsSync.updateCarFromPossibleSpot(CarDatabase.getInstance(getActivity()), getActivity(), car, spot);
        detailsViewManager.hideDetails();
    }

    public void activate() {
        centerCameraOnMarker();
        detailsViewManager.setDetailsFragment(PossibleSetCarDetailsFragment.newInstance(spot, getTag()));
        setCameraFollowing(true);
    }
}
