package com.cahue.iweco;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by francesco on 28.10.2014.
 */
public abstract class AbstractMarkerDelegate extends Fragment implements CameraUpdateRequester, LocationListener {

    protected boolean isActive = false;

    protected Location userLocation;

    @NonNull
    protected DetailsViewManager detailsViewManager;

    @NonNull
    protected DelegateManager delegateManager;

    // too far from the car to calculate directions
    private boolean tooFar = false;

    @Nullable
    private GoogleMap mMap;

    @Override
    public void onAttach(@NonNull Activity context) {
        super.onAttach(context);

        try {
            this.delegateManager = (DelegateManager) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + DelegateManager.class.getName());
        }

        try {
            this.detailsViewManager = (DetailsViewManager) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement " + DetailsViewManager.class.getName());
        }
        delegateManager.registerCameraUpdateRequester(this);
        delegateManager.registerDelegate(this);
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

    @Nullable
    protected LatLng getUserLatLng() {
        if (userLocation == null) return null;
        return new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
    }

    protected final void updateTooFar(@Nullable LatLng firstLocation, @Nullable LatLng secondLocation) {

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

    protected boolean isMapReady() {
        return mMap != null;
    }

    @Nullable
    protected GoogleMap getMap() {
        return mMap;
    }

    /**
     * @param mMap
     */
    public final void setMap(GoogleMap mMap) {
        this.mMap = mMap;
        onMapReady(mMap);
    }

    protected LatLngBounds getViewPortBounds() {
        return getMap().getProjection().getVisibleRegion().latLngBounds;
    }

    public void setActive(boolean active) {
        isActive = active;
        onActiveStatusChanged(active);
    }

    protected void onActiveStatusChanged(boolean active) {

    }
}
