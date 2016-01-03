package com.cahue.iweco.spots;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;

import com.cahue.iweco.R;
import com.cahue.iweco.model.ParkingSpot;
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
    private static BitmapDescriptor redBitmap;
    private static BitmapDescriptor futureBitmap;

    public static MarkerOptions getMarker(ParkingSpot spot, Context context) {

        if (greenIconGenerator == null) {

            LayoutInflater myInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            greenIconGenerator = new IconGenerator(context.getApplicationContext());
            greenIconGenerator.setContentView(myInflater.inflate(R.layout.marker_green_view, null, false));
            greenIconGenerator.setBackground(null);

            yellowIconGenerator = new IconGenerator(context.getApplicationContext());
            yellowIconGenerator.setContentView(myInflater.inflate(R.layout.marker_orange_view, null, false));
            yellowIconGenerator.setBackground(null);

        }

        if (spot.future)
            return getFutureMarker(spot.getLatLng());

        long timeSinceSpotWasFree = System.currentTimeMillis() - spot.time.getTime();
        ParkingSpot.Type markerType = spot.getMarkerType();
        if (markerType == ParkingSpot.Type.green)
            return getGreenMarker(timeSinceSpotWasFree, spot.getLatLng());
        else if (markerType == ParkingSpot.Type.yellow)
            return getYellowMarker(timeSinceSpotWasFree, spot.getLatLng());
        else if (markerType == ParkingSpot.Type.red)
            return getRedMarker(context, spot.getLatLng());

        throw new IllegalStateException("Add spot type to " + MarkerFactory.class.getName());

    }

    private static MarkerOptions getGreenMarker(long timeSinceSpotWasFree, LatLng position) {
        return new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(greenIconGenerator.makeIcon(getMarkerText(timeSinceSpotWasFree))))
                .anchor(MarkerFactory.greenIconGenerator.getAnchorU(), MarkerFactory.greenIconGenerator.getAnchorV());
    }

    private static MarkerOptions getYellowMarker(long timeSinceSpotWasFree, LatLng position) {
        return new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(yellowIconGenerator.makeIcon(getMarkerText(timeSinceSpotWasFree))))
                .anchor(MarkerFactory.yellowIconGenerator.getAnchorU(), MarkerFactory.yellowIconGenerator.getAnchorV());
    }

    private static String getMarkerText(long ms) {
        return String.format("%d'", ms / 60000);
    }

    private static MarkerOptions getRedMarker(Context context, LatLng position) {

        if (redBitmap == null) {

            final float scale = context.getResources().getDisplayMetrics().density;
            Bitmap bitmap = Bitmap.createBitmap((int) (16 * scale + 0.5f), (int) (22 * scale + 0.5f), Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);

            Drawable drawable = context.getResources().getDrawable(R.drawable.marker_red);
            drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            drawable.draw(canvas);
            redBitmap = BitmapDescriptorFactory.fromBitmap(bitmap);

        }

        return new MarkerOptions()
                .flat(false)
                .position(position)
                .icon(redBitmap)
                .anchor(0.5F, 1F);
    }

    private static MarkerOptions getFutureMarker(LatLng position) {

        if (futureBitmap == null) {
            futureBitmap = BitmapDescriptorFactory.fromResource(R.drawable.marker_future);
        }

        return new MarkerOptions()
                .flat(false)
                .position(position)
                .icon(futureBitmap)
                .anchor(0.5F, 1F);
    }


}
