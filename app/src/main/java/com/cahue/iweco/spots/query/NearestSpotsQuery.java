package com.cahue.iweco.spots.query;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cahue.iweco.R;
import com.cahue.iweco.spots.ParkingSpot;
import com.google.android.gms.maps.model.LatLng;

import java.util.Set;

/**
 * Created by francesco on 11.12.2014.
 */
public class NearestSpotsQuery extends ParkingSpotsQuery {

    private static final String TAG = NearestSpotsQuery.class.getSimpleName();

    protected LatLng center;
    protected Integer limit;


    public NearestSpotsQuery(Context context, LatLng center, Integer limit, ParkingSpotsUpdateListener listener) {
        super(context, listener);
        this.center = center;
        this.limit = limit;
    }

    @Override
    protected Set<ParkingSpot> doInBackground(Void... voids) {

        if (center == null || limit == null)
            throw new IllegalStateException("There must be a center and a limit in the number of spots set to build the SQL query.");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(context.getResources().getString(R.string.baseURL))
                .appendPath(context.getResources().getString(R.string.spotsPath))
                .appendPath(context.getResources().getString(R.string.nearestPath))
                .appendQueryParameter("lat", Double.toString(center.latitude))
                .appendQueryParameter("long", Double.toString(center.longitude))
                .appendQueryParameter("count", Integer.toString(limit));

        String url = builder.build().toString();
        Log.i(TAG, "Query nearest : " + url);

        return parseResult(query(url));
    }


}
