package com.cahue.iweco.setCarLocation;

import android.location.Location;
import android.os.Bundle;

import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.CameraUpdateRequester;
import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.R;
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
public class SetCarLocationDelegate extends AbstractMarkerDelegate {

    public static final String FRAGMENT_TAG = "SET_CAR_LOCATION_DELEGATE";

    Location requestLocation;

    Marker marker;
    private GoogleMap mMap;


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
        int color = getResources().getColor(R.color.theme_accent);
        iconGenerator.setColor(color);
    }

    @Override
    public void doDraw() {
        clearMarker();

        LatLng latLng = new LatLng(requestLocation.getLatitude(), requestLocation.getLongitude());

        marker= mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .snippet("")
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon("?")))
                .anchor(iconGenerator.getAnchorU(), iconGenerator.getAnchorV()));
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
            DetailsFragment detailsFragment = detailsViewManager.getDetailsFragment();
            if (detailsFragment != null && detailsFragment instanceof SetCarDetailsFragment)
                detailsViewManager.hideDetails();
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.mMap = map;
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

    }

    public void setRequestLocation(Location requestLocation, Date time, String address) {
        this.requestLocation = requestLocation;
        doDraw();
        detailsViewManager.setDetailsFragment(SetCarDetailsFragment.newInstance(requestLocation, time, address));
    }
}
