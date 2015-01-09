package com.bahpps.cahue.spots.query;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bahpps.cahue.Endpoints;
import com.bahpps.cahue.spots.ParkingSpot;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Set;

/**
 * Created by francesco on 11.12.2014.
 */
public class AreaSpotsQuery extends ParkingSpotsQuery {

    private static final String TAG = AreaSpotsQuery.class.getSimpleName();

    protected LatLngBounds latLngBounds;

    public AreaSpotsQuery(Context context,LatLngBounds latLngBounds, ParkingSpotsUpdateListener listener) {
        super(context, listener);
        this.latLngBounds = latLngBounds;
    }

    @Override
    protected Set<ParkingSpot> doInBackground(Void... voids) {

        if (latLngBounds == null)
            throw new IllegalStateException("There must be a latLngBound set as a viewport to build the SQL query.");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(Endpoints.BASE_URL)
                .appendPath(Endpoints.SPOTS_PATH)
                .appendPath(Endpoints.RETRIEVE_AREA)
                .appendQueryParameter("swLat", Double.toString(latLngBounds.southwest.latitude))
                .appendQueryParameter("swLong", Double.toString(latLngBounds.southwest.longitude))
                .appendQueryParameter("neLat", Double.toString(latLngBounds.northeast.latitude))
                .appendQueryParameter("neLong", Double.toString(latLngBounds.northeast.longitude));

        String url = builder.build().toString();
        Log.i(TAG, "Query area : " +  url);

        return parseResult(query(url));
    }


}
