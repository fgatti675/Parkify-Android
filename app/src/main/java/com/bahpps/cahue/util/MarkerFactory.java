package com.bahpps.cahue.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;

import com.bahpps.cahue.R;
import com.bahpps.cahue.spots.ParkingSpot;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Francesco on 04/11/2014.
 */
public class MarkerFactory {

    private static Map<ParkingSpot.Type, BitmapDescriptor> typeBitmapDescriptorMap = new HashMap<ParkingSpot.Type, BitmapDescriptor>();
    private static Map<ParkingSpot.Type, BitmapDescriptor> selectedTypeBitmapDescriptorMap = new HashMap<ParkingSpot.Type, BitmapDescriptor>();

    public static BitmapDescriptor getMarkerBitmap(ParkingSpot spot, Context context, boolean selected) {

        if(selected)
            return getSelectedMarkerBitmap(spot, context);

        BitmapDescriptor descriptor = typeBitmapDescriptorMap.get(spot.getMarkerType());
        if (descriptor == null) {
            descriptor = createMarkerBitmap(context, spot.getMarkerType().colorId, false);
            typeBitmapDescriptorMap.put(spot.getMarkerType(), descriptor);
        }
        return descriptor;

    }

    private static BitmapDescriptor getSelectedMarkerBitmap(ParkingSpot spot, Context context) {
        BitmapDescriptor descriptor = selectedTypeBitmapDescriptorMap.get(spot.getMarkerType());
        if (descriptor == null) {
            descriptor = createMarkerBitmap(context, spot.getMarkerType().colorId, true);
            selectedTypeBitmapDescriptorMap.put(spot.getMarkerType(), descriptor);
        }
        return descriptor;

    }

    private static BitmapDescriptor createMarkerBitmap(Context context, int colorId, boolean selected) {

        int diameter = context.getResources().getDimensionPixelSize(selected ? R.dimen.marker_diameter_selected : R.dimen.marker_diameter);
        Bitmap mDotMarkerBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(mDotMarkerBitmap);

        GradientDrawable drawable = (GradientDrawable) context.getResources().getDrawable(selected ? R.drawable.marker_selected : R.drawable.marker);
        drawable.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
        drawable.setColor(context.getResources().getColor(colorId));
        drawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap);
    }
}