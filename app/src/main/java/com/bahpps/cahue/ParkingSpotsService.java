package com.bahpps.cahue;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.model.Sqlresponse;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Francesco on 11/10/2014.
 */
public class ParkingSpotsService extends AsyncTask<Void, Void, List<ParkingSpot>> {

    private static final String TAG = "ParkingSpotsService";

    private static final String APPLICATION_NAME = "Cahue";
    private static final String TABLE_NAME = "Spots";
    private static final String TABLE_ID = "1KdObSc-BOSKNnH9zyei7WG--X1w4AyomUj-pB7Ii";

    private Fusiontables fusiontables;
    private LatLngBounds latLngBounds;
    private ParkingSpotsUpdateListener listener;

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

        try {
            String sqlString = String.format(
                    Locale.ENGLISH,
                    "SELECT * FROM %s WHERE ST_INTERSECTS(Location, RECTANGLE(LATLNG(%f, %f), LATLNG(%f, %f)))",
                    TABLE_ID,
                    latLngBounds.northeast.latitude,
                    latLngBounds.northeast.longitude,
                    latLngBounds.southwest.latitude,
                    latLngBounds.southwest.longitude
            );


            Fusiontables.Query.Sql sql = fusiontables.query().sql(sqlString).setKey("AIzaSyBgZq0EA9yoDn8L9CtkAzwa0FtUUZ_oJgY");
            Sqlresponse execute = sql.execute();

            Log.i(TAG, execute.toPrettyString());

        } catch (IllegalArgumentException e) {
            // For google-api-services-fusiontables-v1-rev1-1.7.2-beta this exception will always
            // been thrown.
            // Please see issue 545: JSON response could not be deserialized to Sqlresponse.class
            // http://code.google.com/p/google-api-java-client/issues/detail?id=545
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(List<ParkingSpot> parkingSpots) {
        super.onPostExecute(parkingSpots);
        listener.onLocationsUpdate(parkingSpots);
    }

    public interface ParkingSpotsUpdateListener {
        void onLocationsUpdate(List<ParkingSpot> parkingSpots);
    }
}
