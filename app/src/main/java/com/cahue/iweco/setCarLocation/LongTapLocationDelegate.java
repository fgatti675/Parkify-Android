package com.cahue.iweco.setCarLocation;

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
 * Created by Francesco on 06/07/2015.
 */
public class LongTapLocationDelegate extends AbstractMarkerDelegate implements OnCarClickedListener {

    public static final String FRAGMENT_TAG = "LONG_TAP_LOCATION_DELEGATE";

    ParkingSpot spot;

    Marker marker;
    boolean activated = false;
    private IconGenerator iconGenerator;

    public static LongTapLocationDelegate newInstance() {
        LongTapLocationDelegate fragment = new LongTapLocationDelegate();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        iconGenerator = new IconGenerator(getActivity());
        LayoutInflater myInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = myInflater.inflate(R.layout.marker_long_tap_view, null, false);
        iconGenerator.setContentView(contentView);
        iconGenerator.setBackground(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        doDraw();
    }

    @Override
    protected void onMapReady(GoogleMap mMap) {
        super.onMapReady(mMap);
        doDraw();
    }

    @Override
    public void doDraw() {

        if (!isMapReady() || !isResumed() || !activated) return;

        clearMarker();

        marker = getMap().addMarker(new MarkerOptions()
                .position(spot.getLatLng())
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon()))
                .anchor(0.5F, 0.5F));

        centerCameraOnMarker();
    }

    private void centerCameraOnMarker() {

        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .zoom(15)
                .target(spot.getLatLng())
                .build());

        delegateManager.doCameraUpdate(cameraUpdate, this);
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
            deactivate();
        }
    }

    private boolean isDisplayed() {
        DetailsFragment detailsFragment = detailsViewManager.getDetailsFragment();
        return detailsFragment != null && detailsFragment instanceof LongTapSetCarDetailsFragment;
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
        deactivate();
    }

    private void deactivate() {
        activated = false;
        clearMarker();
        if (isDisplayed())
            detailsViewManager.hideDetails();
    }

    public void activate(ParkingSpot spot) {
        activated = true;
        this.spot = spot;
        detailsViewManager.setDetailsFragment(LongTapSetCarDetailsFragment.newInstance(spot));
        doDraw();
    }

    @Override
    public void onCarSelected(Car car) {
        deactivate();

        CarsSync.updateCarFromPossibleSpot(CarDatabase.getInstance(getActivity()), getActivity(), car, spot);

        detailsViewManager.hideDetails();
    }
}
