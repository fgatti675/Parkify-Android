package com.bahpps.cahue.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.bahpps.cahue.R;
import com.bahpps.cahue.spots.ParkingSpot;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

/**
 * Created by Francesco on 04/11/2014.
 */
public class MarkerFactory {

    private static long GREEN_TIME_THRESHOLD_MS = 5 * 60 * 1000;
    private static long YELLOW_TIME_THRESHOLD_MS = 30 * 60 * 1000;

    public static BitmapDescriptor getMarkerBitmap(ParkingSpot spot, Context context){
        long timeSinceSpotWasFree_ms
                = System.currentTimeMillis() - spot.getTime().getTime() ;

        if(timeSinceSpotWasFree_ms < GREEN_TIME_THRESHOLD_MS)
            return getGreenMarker(context);
        else if(timeSinceSpotWasFree_ms < YELLOW_TIME_THRESHOLD_MS)
            return getYellowMarker(context);
        else
            return getRedMarker(context);

    }

    private static BitmapDescriptor redMarker;
    private static BitmapDescriptor yellowMarker;
    private static BitmapDescriptor greenMarker;

    private static BitmapDescriptor getRedMarker(Context context){
        if (redMarker == null) {
            redMarker = createMarkerBitmap(context, R.drawable.red_marker);
        }
        return redMarker;
    }

    private static BitmapDescriptor getYellowMarker(Context context){
        if (yellowMarker == null) {
            yellowMarker = createMarkerBitmap(context, R.drawable.yellow_marker);
        }
        return yellowMarker;
    }

    private static BitmapDescriptor getGreenMarker(Context context){
        if (greenMarker == null) {
            greenMarker = createMarkerBitmap(context, R.drawable.green_marker);
        }
        return greenMarker;
    }

    private static BitmapDescriptor createMarkerBitmap(Context context, int markerDrawableId) {
        int diameter = context.getResources().getDimensionPixelSize(R.dimen.marker_diameter);
        Bitmap mDotMarkerBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mDotMarkerBitmap);
        Drawable drawable =  context.getResources().getDrawable(markerDrawableId);
        drawable.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap);
    }
}
