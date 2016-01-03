package com.cahue.iweco.setCarLocation;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.CameraUpdateRequester;
import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.R;
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
public class PossibleParkedCarDelegate extends AbstractMarkerDelegate implements SetCarDetailsFragment.CarSelectedListener {

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
    public void onResume() {
        super.onResume();
//        doDraw();
    }

    @Override
    protected void onMapReady(GoogleMap mMap) {
        super.onMapReady(mMap);
        doDraw();
    }

    @Override
    public void doDraw() {
        clearMarker();

        marker = getMap().addMarker(new MarkerOptions()
                .position(spot.getLatLng())
                .snippet("")
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon("?")))
                .anchor(iconGenerator.getAnchorU(), iconGenerator.getAnchorV()));

    }

    private void centerCameraOnMarker() {

        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .zoom(15)
                .target(spot.getLatLng())
                .build());

        cameraManager.onCameraUpdateRequest(cameraUpdate, this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker == this.marker) {
            detailsViewManager.setDetailsFragment(SetCarDetailsFragment.newInstance(spot));
            setCameraFollowing(true);
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
        return detailsFragment != null && detailsFragment instanceof SetCarDetailsFragment;
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
    public void onCarButtonClicked(String carId) {
        clearMarker();
        detailsViewManager.hideDetails();
    }
}
