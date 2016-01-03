package com.cahue.iweco.setCarLocation;

import android.location.Location;
import android.os.Bundle;

import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.CameraUpdateRequester;
import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.R;
import com.cahue.iweco.model.ParkingSpot;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

/**
 * Created by Francesco on 06/07/2015.
 */
public class LongTapLocationDelegate extends AbstractMarkerDelegate implements SetCarDetailsFragment.CarSelectedListener {

    private static final String FRAGMENT_TAG = "LONG_TAP_LOCATION_DELEGATE";

    private static final String ARG_SPOT = "spot";

    ParkingSpot spot;

    Marker marker;

    private IconGenerator iconGenerator;

    public static String getFragmentTag(ParkingSpot spot) {
        return FRAGMENT_TAG + "." + spot.time;
    }

    public static LongTapLocationDelegate newInstance(ParkingSpot spot) {
        LongTapLocationDelegate fragment = new LongTapLocationDelegate();
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
        iconGenerator.setTextAppearance(getActivity(), com.google.maps.android.R.style.Bubble_TextAppearance_Light);
        iconGenerator.setTextAppearance(R.style.Marker_SetCar);
        int color = getResources().getColor(R.color.theme_accent);
        iconGenerator.setColor(color);
    }

    @Override
    public void doDraw() {
        clearMarker();

        marker = getMap().addMarker(new MarkerOptions()
                .position(spot.getLatLng())
                .snippet("")
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon("?")))
                .anchor(iconGenerator.getAnchorU(), iconGenerator.getAnchorV()));

        centerCameraOnMarker();
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
        return false;
    }

    @Override
    protected void onUserLocationChanged(Location userLocation) {

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {
        if (requester != this) {
            clearMarker();
            if (isDisplayed())
                detailsViewManager.hideDetails();
        }
    }

    private boolean isDisplayed() {

        DetailsFragment detailsFragment = detailsViewManager.getDetailsFragment();
        return detailsFragment != null && detailsFragment instanceof SetCarDetailsFragment;
    }


    private void clearMarker() {
        if (marker != null) {
            marker.remove();
        }
    }

    @Override
    public void setCameraFollowing(boolean following) {

    }

    @Override
    public void onMapResized() {
        if (isDisplayed())
            centerCameraOnMarker();
    }

    @Override
    public void onDetailsClosed() {
        clearMarker();
    }

    public void setRequestLocation() {
        detailsViewManager.setDetailsFragment(SetCarDetailsFragment.newInstance(spot));
        doDraw();
    }

    @Override
    public void onCarButtonClicked(String carId) {
        clearMarker();
        detailsViewManager.hideDetails();
    }
}
