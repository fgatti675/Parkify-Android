package com.cahue.iweco.setCarLocation;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;

import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.CameraUpdateRequester;
import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

import java.util.Date;

/**
 * Created by Francesco on 06/07/2015.
 */
public class SetCarLocationDelegate extends AbstractMarkerDelegate implements SetCarDetailsFragment.CarSelectedListener {

    public static final String FRAGMENT_TAG = "SET_CAR_LOCATION_DELEGATE";

    Location requestLocation;

    Marker marker;

    private IconGenerator iconGenerator;


    public static SetCarLocationDelegate newInstance() {
        SetCarLocationDelegate fragment = new SetCarLocationDelegate();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        iconGenerator = new IconGenerator(getActivity());
        iconGenerator.setTextAppearance(getActivity(), com.google.maps.android.R.style.Bubble_TextAppearance_Light);
        iconGenerator.setTextAppearance(R.style.SetCarPositionMarkerStyle);
        int color = getResources().getColor(R.color.theme_accent);
        iconGenerator.setColor(color);
    }

    @Override
    public void doDraw() {
        clearMarker();

        LatLng latLng = new LatLng(requestLocation.getLatitude(), requestLocation.getLongitude());

        marker = getMap().addMarker(new MarkerOptions()
                .position(latLng)
                .snippet("")
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon("?")))
                .anchor(iconGenerator.getAnchorU(), iconGenerator.getAnchorV()));

        centerCameraOnMarker();
    }

    private void centerCameraOnMarker() {

        LatLng latLng = new LatLng(requestLocation.getLatitude(), requestLocation.getLongitude());

        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .zoom(15)
                .target(latLng)
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

    public void setRequestLocation(Location requestLocation, Date time, String address) {
        this.requestLocation = requestLocation;
        detailsViewManager.setDetailsFragment(SetCarDetailsFragment.newInstance(requestLocation, time, address));
        doDraw();
    }

    @Override
    public void onCarButtonClicked(String carId) {
        clearMarker();
        detailsViewManager.hideDetails();
    }
}
