package com.cahue.iweco;

import android.os.AsyncTask;
import android.util.Log;

import com.cahue.iweco.util.GMapV2Direction;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Francesco on 27/05/2015.
 */
public class DirectionsDelegate {

    private static final String TAG = DirectionsDelegate.class.getSimpleName();

    private static final int DIRECTIONS_EXPIRY = 15000;

    private Polyline directionsPolyline;

    /**
     * Actual lines representing the directions PolyLine
     */
    private List<LatLng> directionPoints;

    /**
     * Task for loading directions
     */
    private AsyncTask<Object, Object, Document> directionsAsyncTask;

    /**
     * Directions delegate
     */
    private GMapV2Direction gMapV2Direction;

    public boolean displayed = false;

    private Date lastDirectionsUpdate;
    private GoogleMap mMap;
    private int color;

    public DirectionsDelegate() {
        directionPoints = new ArrayList<LatLng>();
        gMapV2Direction = new GMapV2Direction();
    }

    public void hide(boolean reset) {
        Log.d(TAG, "Hiding directions");
        if(reset)
            reset();
        displayed = false;
        clear();
    }

    private void clear() {
        if (directionsPolyline != null) directionsPolyline.remove();
    }

    private void doDraw() {

        clear();

        if (!displayed)
            return;

        if (directionPoints.isEmpty())
            return;

        Log.d(TAG, "Drawing directions");

        PolylineOptions rectLine = new PolylineOptions().width(10).color(color);

        for (int i = 0; i < directionPoints.size(); i++) {
            rectLine.add(directionPoints.get(i));
        }

        directionsPolyline = mMap.addPolyline(rectLine);

    }

    private void reset() {
        lastDirectionsUpdate = null;
    }

    /**
     * Fetch directions with the indicated parameters and draw to the map when ready
     *
     * @param from
     * @param to
     * @param mode
     */
    public void drawDirections(final LatLng from, final LatLng to, final String mode) {

        displayed = true;

        if (lastDirectionsUpdate != null && System.currentTimeMillis() - lastDirectionsUpdate.getTime() < DIRECTIONS_EXPIRY) {
            return;
        }

        // if there were results before we display them while new ones come
        doDraw();

        /**
         * Cancel if something is going on
         */
        if (directionsAsyncTask != null && directionsAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            directionsAsyncTask.cancel(true);
        }

        Log.d(TAG, "Fetching directions");

        directionsAsyncTask = new AsyncTask<Object, Object, Document>() {

            @Override
            protected Document doInBackground(Object[] objects) {
                Log.d(TAG, "Fetching directions");
                Document doc = gMapV2Direction.getDocument(from, to, mode);
                return doc;
            }

            @Override
            protected void onPostExecute(Document doc) {
                lastDirectionsUpdate = new Date();
                directionPoints.clear();
                directionPoints.addAll(gMapV2Direction.getDirection(doc));
                doDraw();
            }

        };
        directionsAsyncTask.execute();
    }

    public void setMap(GoogleMap map) {
        this.mMap = map;
    }

    public List<LatLng> getDirectionPoints() {
        return directionPoints;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
