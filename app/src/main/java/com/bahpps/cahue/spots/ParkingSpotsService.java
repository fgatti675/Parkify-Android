package com.bahpps.cahue.spots;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.model.Sqlresponse;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Service in charge of querying the Fusion Table including the parking spots.
 * The result is accessed via a listener
 */
public class ParkingSpotsService extends AsyncTask<Void, Void, List<ParkingSpot>> {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static{
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private static final String TAG = "ParkingSpotsService";
    private static final String BROWSER_KEY = "AIzaSyB5ukn97hu5159E3cBuUvLvXKV7IsgQG38"; // Using a browser key instead of an Android key for some stupid reason

    private static final String APPLICATION_NAME = "Cahue";
    private static final String TABLE_NAME = "Spots";
    private static final String TABLE_ID = "1KdObSc-BOSKNnH9zyei7WG--X1w4AyomUj-pB7Ii";

    private Fusiontables fusiontables;
    private LatLngBounds latLngBounds;
    private ParkingSpotsUpdateListener listener;

    /**
     * Create a new service. {@code #execute()} must be called afterwards
     * @param latLngBounds
     * @param listener
     */
    public ParkingSpotsService(LatLngBounds latLngBounds, ParkingSpotsUpdateListener listener) {
        this.latLngBounds = latLngBounds;
        this.listener = listener;

        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        fusiontables = new Fusiontables.Builder(httpTransport, jsonFactory, null)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    @Override
    protected List<ParkingSpot> doInBackground(Void... voids) {

        Log.i(TAG, "Retrieving parking spots " + latLngBounds);
        List<ParkingSpot> spots = new ArrayList<ParkingSpot>();
        try {

            String sqlString = String.format(
                    Locale.ENGLISH,
                    "SELECT * FROM %s WHERE ST_INTERSECTS(Location, RECTANGLE(LATLNG(%f, %f), LATLNG(%f, %f)))",
                    getTableId(),
                    latLngBounds.southwest.latitude,
                    latLngBounds.southwest.longitude,
                    latLngBounds.northeast.latitude,
                    latLngBounds.northeast.longitude
            );

            Fusiontables.Query.Sql sql = fusiontables.query().sql(sqlString).setKey(BROWSER_KEY);
            Sqlresponse spotsResponse = sql.execute();
            Log.d(TAG, spotsResponse.toPrettyString());

            List<ArrayList> rows = (List<ArrayList>) spotsResponse.get("rows");
            if (rows != null) {
                for (ArrayList<String> element : rows) {

                    try {
                        ParkingSpot spot = new ParkingSpot();
                        spot.id = element.get(0);
                        spot.time = dateFormat.parse(element.get(1));
                        String[] tokens = element.get(2).split(", ");
                        spot.position = new LatLng(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]));
                        spots.add(spot);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return spots;
    }

    @Override
    protected void onPostExecute(List<ParkingSpot> parkingSpots) {

        Log.d(TAG, parkingSpots.toString());
        super.onPostExecute(parkingSpots);
        listener.onLocationsUpdate(parkingSpots);
    }

    /**
     * Table the query will be run against.
     * Can be overridden for testing purposes.
     *
     * @return
     */
    public String getTableId() {
        return TABLE_ID;
    }

    /**
     * Components that use this service must implement a listener using this interface to get the
     * parking locations
     */
    public interface ParkingSpotsUpdateListener {
        void onLocationsUpdate(List<ParkingSpot> parkingSpots);
    }
}
