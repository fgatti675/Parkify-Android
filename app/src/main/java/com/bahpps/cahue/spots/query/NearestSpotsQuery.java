package com.bahpps.cahue.spots.query;

import android.net.Uri;
import android.util.Log;

import com.bahpps.cahue.Endpoints;
import com.bahpps.cahue.spots.ParkingSpot;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Set;

/**
 * Created by francesco on 11.12.2014.
 */
public class NearestSpotsQuery extends ParkingSpotsQuery {

    private static final String TAG = NearestSpotsQuery.class.getSimpleName();

    protected LatLng center;
    protected Integer limit;


    public NearestSpotsQuery(LatLng center, Integer limit, ParkingSpotsUpdateListener listener) {
        super(listener);
        this.center = center;
        this.limit = limit;
    }

    @Override
    protected Set<ParkingSpot> doInBackground(Void... voids) {

            if (center == null || limit == null)
                throw new IllegalStateException("There must be a center and a limit in the number of spots set to build the SQL query.");

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority(Endpoints.BASE_URL)
                    .appendPath(Endpoints.SPOTS_PATH)
                    .appendPath(Endpoints.RETRIEVE_NEAREST)
                    .appendQueryParameter("lat", Double.toString(center.latitude))
                    .appendQueryParameter("long", Double.toString(center.longitude))
                    .appendQueryParameter("count", Integer.toString(limit));

            String url = builder.build().toString();
            Log.i(TAG, "Query nearest : ");

            return parseResult(query(url));
    }


}
