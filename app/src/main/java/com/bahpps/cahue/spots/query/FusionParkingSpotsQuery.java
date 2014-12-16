package com.bahpps.cahue.spots.query;

import android.content.Context;
import android.util.Log;

import com.bahpps.cahue.spots.ParkingSpot;
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Service in charge of querying the Fusion Table including the parking spots.
 * The result is accessed via a listener
 */
public class FusionParkingSpotsQuery extends ParkingSpotsQuery {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final String TAG = "ParkingSpotsQuery";
    private static final String BROWSER_KEY = "AIzaSyB5ukn97hu5159E3cBuUvLvXKV7IsgQG38"; // Using a browser key instead of an Android key for some stupid reason

    private static final String APPLICATION_NAME = "Cahue";
    private static final String TABLE_ID = "1KdObSc-BOSKNnH9zyei7WG--X1w4AyomUj-pB7Ii";

    private Fusiontables fusiontables;
    private LatLngBounds latLngBounds;

    /**
     * Create a new service. {@code #execute()} must be called afterwards
     *
     * @param latLngBounds
     * @param listener
     */
    public FusionParkingSpotsQuery(Context context, LatLngBounds latLngBounds, ParkingSpotsUpdateListener listener) {
        super(context, listener);
        this.latLngBounds = latLngBounds;

        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        fusiontables = new Fusiontables.Builder(httpTransport, jsonFactory, null)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    @Override
    protected Set<ParkingSpot> doInBackground(Void... voids) {

        Log.i(TAG, "Retrieving parking spots " + latLngBounds);
        Set<ParkingSpot> spots = new HashSet<ParkingSpot>();
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
                        String[] positionArray = element.get(2).split(", ");
                        LatLng position = new LatLng(Double.parseDouble(positionArray[0]), Double.parseDouble(positionArray[1]));
                        Date time = dateFormat.parse(element.get(1));
                        Long id = Long.parseLong(element.get(0));
                        ParkingSpot spot = new ParkingSpot(id, position, time);
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


    /**
     * Table the query will be run against.
     * Can be overridden for testing purposes.
     *
     * @return
     */
    public String getTableId() {
        return TABLE_ID;
    }

}
