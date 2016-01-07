package com.cahue.iweco;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by francesco on 28.10.2014.
 */
public abstract class AbstractMarkerDelegate extends Fragment implements CameraUpdateRequester, LocationListener {

    protected Location userLocation;
    protected DetailsViewManager detailsViewManager;
    protected DelegateManager delegateManager;
    // too far from the car to calculate directions
    private boolean tooFar = false;
    private GoogleMap mMap;

    public abstract void doDraw();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            this.delegateManager = (DelegateManager) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + DelegateManager.class.getName());
        }

        try {
            this.detailsViewManager = (DetailsViewManager) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + DetailsViewManager.class.getName());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        delegateManager.registerCameraUpdateRequester(this);
        delegateManager.registerDelegate(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        delegateManager = null;
        detailsViewManager = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        delegateManager.unregisterCameraUpdateRequester(this);
        delegateManager.unregisterDelegate(this);
        mMap = null;
    }

    /**
     * Called when the map is ready to be used
     *
     * @param mMap
     */
    protected void onMapReady(GoogleMap mMap) {

    }

    /**
     * Called when the details view is closed
     */
    public void onDetailsClosed() {

    }

    /**
     * Called when a marker is clicked
     *
     * @param marker
     * @return true if the delegate has consumed the event
     */
    public abstract boolean onMarkerClick(Marker marker);

    @Override
    public final void onLocationChanged(Location userLocation) {
        this.userLocation = userLocation;
        onUserLocationChanged(userLocation);
    }

    protected abstract void onUserLocationChanged(Location userLocation);

    protected LatLng getUserLatLng() {
        if (userLocation == null) return null;
        return new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
    }

    protected final void updateTooFar(LatLng firstLocation, LatLng secondLocation) {

        if (firstLocation == null || secondLocation == null) {
            return;
        }

        float distances[] = new float[3];
        Location.distanceBetween(
                firstLocation.latitude,
                firstLocation.longitude,
                secondLocation.latitude,
                secondLocation.longitude,
                distances);

        tooFar = distances[0] > getDirectionsMaxDistance();
    }

    public float getDirectionsMaxDistance() {
        return -1;
    }

    public boolean isTooFar() {
        return tooFar;
    }

    protected GoogleMap getMap() {
        if (mMap == null) {
            Fragment mapFragment = getFragmentManager().findFragmentById(R.id.map);
            if (mapFragment != null)
                setMap(((MapFragment) mapFragment).getMap());
        }
        return mMap;
    }

    /**
     * @param mMap
     */
    public final void setMap(GoogleMap mMap) {
        this.mMap = mMap;
        onMapReady(mMap);
    }
}
