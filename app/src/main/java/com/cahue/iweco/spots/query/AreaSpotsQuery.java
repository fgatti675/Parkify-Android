package com.cahue.iweco.spots.query;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cahue.iweco.R;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Created by francesco on 11.12.2014.
 */
public class AreaSpotsQuery extends ParkingSpotsQuery {

    private static final String TAG = AreaSpotsQuery.class.getSimpleName();

    private final Context context;
    protected LatLngBounds latLngBounds;

    public AreaSpotsQuery(Context context,LatLngBounds latLngBounds, ParkingSpotsUpdateListener listener) {
        super(context, listener);
        this.context = context;
        this.latLngBounds = latLngBounds;
    }

    @Override
    protected Uri getRequestUri() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(context.getResources().getString(R.string.baseURL))
                .appendPath(context.getResources().getString(R.string.spotsPath))
                .appendQueryParameter("swLat", Double.toString(latLngBounds.southwest.latitude))
                .appendQueryParameter("swLong", Double.toString(latLngBounds.southwest.longitude))
                .appendQueryParameter("neLat", Double.toString(latLngBounds.northeast.latitude))
                .appendQueryParameter("neLong", Double.toString(latLngBounds.northeast.longitude));
        return builder.build();
    }
}
