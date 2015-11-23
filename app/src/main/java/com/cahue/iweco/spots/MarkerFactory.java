package com.cahue.iweco.spots;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;

import com.cahue.iweco.R;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

/**
 * Created by Francesco on 04/11/2014.
 */
public class MarkerFactory {


    private static IconGenerator greenIconGenerator;
    private static IconGenerator yellowIconGenerator;
    private static IconGenerator selectedIconGenerator;

    public static MarkerOptions getMarker(ParkingSpot spot, Context context, boolean selected) {

        if (greenIconGenerator == null) {

            int bigSide = (int) context.getResources().getDimension(R.dimen.marker_padding_big_side);
            int bigTop = (int) context.getResources().getDimension(R.dimen.marker_padding_big_top);

            int mediumSide = (int) context.getResources().getDimension(R.dimen.marker_padding_big_side);
            int mediumTop = (int) context.getResources().getDimension(R.dimen.marker_padding_big_top);

            greenIconGenerator = new IconGenerator(context.getApplicationContext());
            greenIconGenerator.setContentPadding(bigSide, bigTop, bigSide, 0);
            greenIconGenerator.setTextAppearance(R.style.Marker_Green);
            greenIconGenerator.setBackground(context.getResources().getDrawable(R.drawable.map_marker_green));

            yellowIconGenerator = new IconGenerator(context.getApplicationContext());
            yellowIconGenerator.setContentPadding(mediumSide, mediumTop, mediumSide, 0);
            yellowIconGenerator.setTextAppearance(R.style.Marker_Orange);
            yellowIconGenerator.setBackground(context.getResources().getDrawable(R.drawable.map_marker_orange));

            selectedIconGenerator = new IconGenerator(context.getApplicationContext());
            selectedIconGenerator.setContentPadding(bigSide, bigTop, bigSide, 0);
            selectedIconGenerator.setTextAppearance(R.style.Marker_Selected);
            selectedIconGenerator.setBackground(context.getResources().getDrawable(R.drawable.map_marker_selected));
        }

        if(spot.future)
            return getFutureMarker(context, spot.position, selected);

        long timeSinceSpotWasFree = System.currentTimeMillis() - spot.time.getTime();
        ParkingSpot.Type markerType = spot.getMarkerType();
        if (markerType == ParkingSpot.Type.green)
            return getGreenMarker(timeSinceSpotWasFree, spot.position, selected);
        else if (markerType == ParkingSpot.Type.yellow)
            return getYellowMarker(timeSinceSpotWasFree, spot.position, selected);
        else if (markerType == ParkingSpot.Type.red)
            return getRedMarker(context, spot.position, selected);

        throw new IllegalStateException("Add spot type to " + MarkerFactory.class.getName());

    }

    private static BitmapDescriptor futureBitmap;
    private static BitmapDescriptor futureBitmapSelected;

    private static MarkerOptions getFutureMarker(Context context, LatLng position, boolean selected) {

        BitmapDescriptor bitmap;
        if (futureBitmap == null) {
            futureBitmap = createFutureBitmap(context, false);
        }
        if (futureBitmapSelected == null) {
            futureBitmapSelected = createFutureBitmap(context, true);
        }

        if (selected)
            bitmap = futureBitmapSelected;
        else
            bitmap = futureBitmap;

        return new MarkerOptions()
                .flat(false)
                .position(position)
                .icon(bitmap)
                .anchor(0.5F, 1F);
    }

    private static MarkerOptions getGreenMarker(long timeSinceSpotWasFree, LatLng position, boolean selected) {
        if (selected)
            return getSelectedMarker(timeSinceSpotWasFree, position);

        return new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(greenIconGenerator.makeIcon(getMarkerText(timeSinceSpotWasFree))))
                .anchor(MarkerFactory.greenIconGenerator.getAnchorU(), MarkerFactory.greenIconGenerator.getAnchorV());
    }

    private static MarkerOptions getYellowMarker(long timeSinceSpotWasFree, LatLng position, boolean selected) {
        if (selected)
            return getSelectedMarker(timeSinceSpotWasFree, position);

        return new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(yellowIconGenerator.makeIcon(getMarkerText(timeSinceSpotWasFree))))
                .anchor(MarkerFactory.yellowIconGenerator.getAnchorU(), MarkerFactory.yellowIconGenerator.getAnchorV());
    }


    private static MarkerOptions getSelectedMarker(long timeSinceSpotWasFree, LatLng position) {
        return new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(selectedIconGenerator.makeIcon(getMarkerText(timeSinceSpotWasFree))))
                .anchor(MarkerFactory.selectedIconGenerator.getAnchorU(), MarkerFactory.selectedIconGenerator.getAnchorV());
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

    private static BitmapDescriptor createFutureBitmap(Context context, boolean selected) {
        return BitmapDescriptorFactory.fromResource(R.drawable.map_marker_future);
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
