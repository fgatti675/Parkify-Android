package com.bahpps.cahue.spots;

import android.util.Log;

import com.cartodb.CartoDBClientIF;
import com.cartodb.CartoDBException;
import com.cartodb.impl.ApiKeyCartoDBClient;
import com.cartodb.model.CartoDBResponse;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Service in charge of querying the Fusion Table including the parking spots.
 * The result is accessed via a listener
 */
public class CartoDBParkingSpotsQuery extends ParkingSpotsQuery {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private static final String TAG = "ParkingSpotsQuery";
    private static final String API_KEY = "3037e96df92be2c06ee3d1d1e15c089157c33419"; // Using a browser key instead of an Android key for some stupid reason

    private static final String ACCOUNT_NAME = "Cahue";
    private static final String TABLE_NAME = "spots";

    CartoDBClientIF cartoDBClient;

    /**
     * Create a new service. {@code #execute()} must be called afterwards
     *
     * @param latLngBounds
     * @param listener
     */
    public CartoDBParkingSpotsQuery(LatLngBounds latLngBounds, ParkingSpotsUpdateListener listener) {
        super(latLngBounds, listener);

        try {
            cartoDBClient = new ApiKeyCartoDBClient(ACCOUNT_NAME, API_KEY);
        } catch (CartoDBException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Set<ParkingSpot> doInBackground(Void... voids) {

        Log.i(TAG, "Retrieving parking spots " + latLngBounds);
        Set<ParkingSpot> spots = new HashSet<ParkingSpot>();

        String sqlString = String.format(
                Locale.ENGLISH,
                "SELECT * FROM %s WHERE the_geom && ST_MakeEnvelope(%f, %f, %f, %f, 4326)",
                getTableId(),
                latLngBounds.southwest.longitude,
                latLngBounds.southwest.latitude,
                latLngBounds.northeast.longitude,
                latLngBounds.northeast.latitude
        );

        // get rows as a Map
        CartoDBResponse<Map<String, Object>> res = null;
        try {
            res = cartoDBClient.request("select * from mytable limit 1");
        } catch (CartoDBException e) {
            e.printStackTrace();
        }
//            System.out.print(res.getRows().get(0).get("cartodb_id"));


        if (res != null) {
            for (Map<String, Object> row : res.getRows()) {

                System.out.println(row);
//                    try {
//                        String[] positionArray = element.get(2).split(", ");
//                        LatLng position = new LatLng(Double.parseDouble(row.get("")), Double.parseDouble(positionArray[1]));
//                        Date time = dateFormat.parse(element.get(1));
//                        ParkingSpot spot = new ParkingSpot(element.get(0), position, time);
//                        spots.add(spot);
//                    } catch (ParseException e) {
//                        e.printStackTrace();
//                    }
            }
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
        return TABLE_NAME;
    }

}
