package com.cahue.iweco.activityrecognition;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.OnCarClickedListener;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.util.Tracking;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Delegate to show a marker where a car might be parked, based on activity recognition
 */
public class PossibleParkedCarDelegate extends AbstractMarkerDelegate implements OnCarClickedListener, PossibleSetCarDetailsFragment.OnPossibleSpotDeletedListener {

    public static final String FRAGMENT_TAG = "POSSIBLE_PARKED_CAR_DELEGATE";

    private static final String ARG_SPOT = "spot";
    private static final String ARG_RECENCY = "recency";

    @Retention(SOURCE)
    @IntDef({RECENT_0, RECENT_1, RECENT_2, RECENT_3, RECENT_4})
    public @interface Recency {
    }

    public static final int RECENT_0 = 0;
    public static final int RECENT_1 = 1;
    public static final int RECENT_2 = 2;
    public static final int RECENT_3 = 3;
    public static final int RECENT_4 = 4;

    @Recency
    private int recency;

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
    public static PossibleParkedCarDelegate newInstance(ParkingSpot spot, @Recency int recency) {
        PossibleParkedCarDelegate fragment = new PossibleParkedCarDelegate();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SPOT, spot);
        args.putInt(ARG_RECENCY, recency);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = CarDatabase.getInstance();
        this.spot = getArguments().getParcelable(ARG_SPOT);
        //noinspection WrongConstant
        this.recency = getArguments().getInt(ARG_RECENCY);

        iconGenerator = new IconGenerator(getActivity());
        iconGenerator.setColor(getActivity().getResources().getColor(R.color.theme_primary));
        iconGenerator.setContentRotation(-90);
        switch (recency) {
            case RECENT_0:
                iconGenerator.setTextAppearance(getActivity(), R.style.Marker_PossibleCar);
            case RECENT_1:
                iconGenerator.setTextAppearance(getActivity(), R.style.Marker_PossibleCar12);
                break;
            case RECENT_2:
            case RECENT_3:
            case RECENT_4:
                iconGenerator.setTextAppearance(getActivity(), R.style.Marker_PossibleCar8);
                break;
        }
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
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon("Possible parked car")))
                .anchor(iconGenerator.getAnchorU(), iconGenerator.getAnchorV()));

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

            Tracking.sendEvent(Tracking.CATEGORY_MAP, Tracking.ACTION_POSSIBLE_CAR_SELECTED, Tracking.LABEL_SELECTED_FROM_MARKER);
            return true;
        }
        return false;
    }

    @Override
    protected void onUserLocationChanged(Location userLocation) {
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
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
        database.removeParkingSpot(getActivity(), spot);
        if (marker != null)
            marker.remove();
        detailsViewManager.hideDetails();
    }
}
