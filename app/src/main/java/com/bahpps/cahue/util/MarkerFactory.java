package com.bahpps.cahue.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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

    public static BitmapDescriptor getMarkerBitmap(ParkingSpot spot, Context context) {

        BitmapDescriptor descriptor = typeBitmapDescriptorMap.get(spot.getMarkerType());
        if(descriptor == null){
            descriptor = createMarkerBitmap(context, spot.getMarkerType().colorId);
            typeBitmapDescriptorMap.put(spot.getMarkerType(), descriptor);
        }
        return descriptor;

    }

    private static BitmapDescriptor createMarkerBitmap(Context context, int colorId) {
        int diameter = context.getResources().getDimensionPixelSize(R.dimen.marker_diameter);
        Bitmap mDotMarkerBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mDotMarkerBitmap);
        GradientDrawable drawable = (GradientDrawable) context.getResources().getDrawable(R.drawable.marker);
        drawable.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
        drawable.setColor(context.getResources().getColor(colorId));
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap);
    }
}
