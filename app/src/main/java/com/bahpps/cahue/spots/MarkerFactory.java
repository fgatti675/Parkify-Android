package com.bahpps.cahue.spots;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;

import com.bahpps.cahue.R;
import com.bahpps.cahue.spots.ParkingSpot;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.maps.android.ui.IconGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Francesco on 04/11/2014.
 */
public class MarkerFactory {

    public static IconGenerator iconGenerator;

    private static Map<ParkingSpot.Type, BitmapDescriptor> typeBitmapDescriptorMap = new HashMap<ParkingSpot.Type, BitmapDescriptor>();
    private static Map<ParkingSpot.Type, BitmapDescriptor> selectedTypeBitmapDescriptorMap = new HashMap<ParkingSpot.Type, BitmapDescriptor>();

    public static BitmapDescriptor getMarkerBitmap(ParkingSpot.Type type, Context context, boolean selected) {

        if (iconGenerator == null) {
            iconGenerator = new IconGenerator(context.getApplicationContext());
        }

        if (selected) {

            BitmapDescriptor descriptor = selectedTypeBitmapDescriptorMap.get(type);
            if (descriptor == null) {
                descriptor = createMarkerBitmap(context, type, true);
                selectedTypeBitmapDescriptorMap.put(type, descriptor);
            }
            return descriptor;

        } else {

            BitmapDescriptor descriptor = typeBitmapDescriptorMap.get(type);
            if (descriptor == null) {
                descriptor = createMarkerBitmap(context, type, false);
                typeBitmapDescriptorMap.put(type, descriptor);
            }
            return descriptor;

        }

    }

    private static BitmapDescriptor createMarkerBitmap(Context context, ParkingSpot.Type type, boolean selected) {

        iconGenerator.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Inverse);

        if (type == ParkingSpot.Type.green) {
            iconGenerator.setBackground(context.getResources().getDrawable(R.drawable.map_marker_green));
            return BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon("T"));
        } else if (type == ParkingSpot.Type.yellow) {
            iconGenerator.setBackground(context.getResources().getDrawable(R.drawable.map_marker_yellow));
            return BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon("T"));
        }


        int diameter = context.getResources().getDimensionPixelSize(selected ? R.dimen.marker_diameter_selected : type.diameterId);
        Bitmap mDotMarkerBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(mDotMarkerBitmap);

        GradientDrawable drawable = (GradientDrawable) context.getResources().getDrawable(selected ? R.drawable.marker_selected : R.drawable.marker);
        drawable.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
        drawable.setColor(context.getResources().getColor(type.colorId));
        drawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap);
    }
}
