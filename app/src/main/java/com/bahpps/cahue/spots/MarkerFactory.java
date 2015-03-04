package com.bahpps.cahue.spots;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;

import com.bahpps.cahue.R;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

/**
 * Created by Francesco on 04/11/2014.
 */
public class MarkerFactory {

    private static long GREEN_TIME_THRESHOLD_MS = 5 * 60 * 1000;
    private static long YELLOW_TIME_THRESHOLD_MS = 10 * 60 * 1000;

    private static IconGenerator iconGenerator;

    public static MarkerOptions getMarker(ParkingSpot spot, Context context, boolean selected) {

        if (iconGenerator == null) {
            iconGenerator = new IconGenerator(context.getApplicationContext());
        }

        long timeSinceSpotWasFree = System.currentTimeMillis() - spot.time.getTime();
        if (timeSinceSpotWasFree < GREEN_TIME_THRESHOLD_MS)
            return getGreenMarker(context, timeSinceSpotWasFree, spot.position, selected);
        else if (timeSinceSpotWasFree < YELLOW_TIME_THRESHOLD_MS)
            return getYellowMarker(context, timeSinceSpotWasFree, spot.position, selected);
        else
            return getRedMarker(context, spot.position, selected);

    }

    private static MarkerOptions getGreenMarker(Context context, long timeSinceSpotWasFree, LatLng position, boolean selected) {
        if(selected)
            return getSelectedMarker(context, timeSinceSpotWasFree, position);

        iconGenerator.setTextAppearance(R.style.MarkerStyle);
        iconGenerator.setBackground(context.getResources().getDrawable(R.drawable.map_marker_green));
        return getMarkerOptions(timeSinceSpotWasFree, position);
    }

    private static MarkerOptions getMarkerOptions(long timeSinceSpotWasFree, LatLng position) {
        return new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon(getMarkerText(timeSinceSpotWasFree))))
                .anchor(MarkerFactory.iconGenerator.getAnchorU(), MarkerFactory.iconGenerator.getAnchorV());
    }

    private static MarkerOptions getYellowMarker(Context context, long timeSinceSpotWasFree, LatLng position, boolean selected) {
        if(selected)
            return getSelectedMarker(context, timeSinceSpotWasFree, position);

        iconGenerator.setTextAppearance(R.style.MarkerStyle);
        iconGenerator.setBackground(context.getResources().getDrawable(R.drawable.map_marker_yellow));
        return getMarkerOptions(timeSinceSpotWasFree, position);
    }

    private static MarkerOptions getSelectedMarker(Context context, long timeSinceSpotWasFree, LatLng position) {
        iconGenerator.setTextAppearance(R.style.SelectedMarkerStyle);
        iconGenerator.setBackground(context.getResources().getDrawable(R.drawable.map_marker_selected));
        return getMarkerOptions(timeSinceSpotWasFree, position);
    }

    private static String getMarkerText(long ms) {
        return String.format("%d'", ms / 60000);
    }

    private static BitmapDescriptor redBitmap;
    private static BitmapDescriptor redBitmapSelected;

    private static MarkerOptions getRedMarker(Context context, LatLng position, boolean selected) {

        BitmapDescriptor bitmap;
        if (redBitmap == null) {
            redBitmap = createRedMarkerBitmap(context, false);
        }
        if (redBitmapSelected == null) {
            redBitmapSelected = createRedMarkerBitmap(context, true);
        }

        if (selected)
            bitmap = redBitmapSelected;
        else
            bitmap = redBitmap;

        return new MarkerOptions()
                .flat(true)
                .position(position)
                .icon(bitmap)
                .anchor(0.5F, 0.5F);
    }

    private static BitmapDescriptor createRedMarkerBitmap(Context context, boolean selected) {
        int diameter = context.getResources().getDimensionPixelSize(selected ? R.dimen.marker_diameter_selected : R.dimen.marker_diameter_red);
        Bitmap mDotMarkerBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(mDotMarkerBitmap);

        GradientDrawable drawable = (GradientDrawable) context.getResources().getDrawable(selected ? R.drawable.marker_red_selected : R.drawable.marker_red);
        drawable.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
        drawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap);
    }

}
