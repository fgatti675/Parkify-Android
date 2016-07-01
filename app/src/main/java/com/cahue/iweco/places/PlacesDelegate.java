package com.cahue.iweco.places;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.CameraUpdateRequester;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;

import java.util.Iterator;

/**
 * Created by f.gatti.gomez on 31/05/16.
 */
public class PlacesDelegate extends AbstractMarkerDelegate implements GoogleApiClient.ConnectionCallbacks {


    private GoogleApiClient mGoogleApiClient;

    private AutocompletePredictionBuffer predictionBuffer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GoogleApiClient instance
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(getActivity())
                .addApiIfAvailable(LocationServices.API)
                .addApiIfAvailable(ActivityRecognition.API)
                .addConnectionCallbacks(this);

        mGoogleApiClient = builder.build();
    }

    @Override
    public void onStart() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();

    }

    @Override
    public void doDraw() {
        if (predictionBuffer == null) return;
        Iterator<AutocompletePrediction> iterator = predictionBuffer.singleRefIterator();
        while(iterator.hasNext()){
            AutocompletePrediction prediction = iterator.next();
            prediction.ge
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    protected void onUserLocationChanged(Location userLocation) {
        makePlacesRequest();
    }

    private void makePlacesRequest() {
        if (mGoogleApiClient.isConnected() && isMapReady()) {
            AutocompleteFilter autocompleteFilter = new AutocompleteFilter.Builder().setTypeFilter(AutocompleteFilter.TYPE_FILTER_ESTABLISHMENT).build();
            PendingResult<AutocompletePredictionBuffer> result =
                    Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient, "", getViewPortBounds(), autocompleteFilter);
            result.setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
                @Override
                public void onResult(@NonNull AutocompletePredictionBuffer autocompletePredictions) {
                    predictionBuffer = autocompletePredictions;
                    doDraw();
                }
            });
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {
        makePlacesRequest();
        doDraw();
    }

    @Override
    public void setCameraFollowing(boolean following) {

    }

    @Override
    public void onMapResized() {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        makePlacesRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
