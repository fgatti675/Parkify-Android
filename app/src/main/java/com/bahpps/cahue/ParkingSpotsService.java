package com.bahpps.cahue;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.FusiontablesScopes;
import com.google.api.services.fusiontables.model.Sqlresponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by Francesco on 11/10/2014.
 */
public class ParkingSpotsService extends AsyncTask<Void, Void, List<ParkingSpot>> {

    private static final String TAG = "ParkingSpotsService";
    private static final String BROWSER_KEY = "AIzaSyB5ukn97hu5159E3cBuUvLvXKV7IsgQG38"; // Using a browser key instead of an Android key for some stupid reason

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

        Log.i(TAG, "Retrieving parking spots " + latLngBounds);
        List<ParkingSpot> spots = new ArrayList<ParkingSpot>();
        try {

            String sqlString = String.format(
                    Locale.ENGLISH,
                    "SELECT * FROM %s WHERE ST_INTERSECTS(Location, RECTANGLE(LATLNG(%f, %f), LATLNG(%f, %f)))",
                    TABLE_ID,
                    latLngBounds.southwest.latitude,
                    latLngBounds.southwest.longitude,
                    latLngBounds.northeast.latitude,
                    latLngBounds.northeast.longitude
            );

            Fusiontables.Query.Sql sql = fusiontables.query().sql(sqlString).setKey(BROWSER_KEY);
            Sqlresponse spotsResponse = sql.execute();

            List<ArrayList> rows = (List<ArrayList>) spotsResponse.get("rows");
            if (rows != null) {
                for (ArrayList<String> element : rows) {
                    ParkingSpot spot = new ParkingSpot();
                    spot.id = element.get(0);
                    spot.time = new Date(element.get(1));
                    String[] tokens =  element.get(2).split(",");
                    spot.location = new LatLng(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]));
                    spots.add(spot);
                }
            }

            Log.i(TAG, spotsResponse.toPrettyString());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return spots;
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
