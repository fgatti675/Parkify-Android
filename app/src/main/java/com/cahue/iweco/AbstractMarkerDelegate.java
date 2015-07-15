package com.cahue.iweco;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;

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
    protected CameraManager cameraManager;
    // too far from the car to calculate directions
    private boolean tooFar = false;
    private GoogleMap mMap;

    public abstract void doDraw();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            this.cameraManager = (CameraManager) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + CameraManager.class.getName());
        }

        try {
            this.detailsViewManager = (DetailsViewManager) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + DetailsViewManager.class.getName());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        cameraManager.registerCameraUpdater(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        cameraManager.unregisterCameraUpdater(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMap = null;
    }

    /**
     * Called when the map is ready to be used
     *
     * @param mMap
     */
    public void onMapReady(GoogleMap mMap) {
        this.mMap = mMap;
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
            onMapReady(((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap());
        }
        return mMap;
    }
}
