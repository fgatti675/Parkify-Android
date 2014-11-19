package com.bahpps.cahue.spots.query;

import android.util.Log;

import com.bahpps.cahue.spots.ParkingSpot;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Service in charge of querying the Fusion Table including the parking spots.
 * The result is accessed via a listener
 */
public class CartoDBParkingSpotsQuery extends ParkingSpotsQuery {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    private static final String TAG = "ParkingSpotsQuery";
    private static final String API_KEY = "3037e96df92be2c06ee3d1d1e15c089157c33419";

    private static final String ACCOUNT_NAME = "cahue";
    private static final String TABLE_NAME = "spots";
    private static final String URL = "http://" + ACCOUNT_NAME + ".cartodb.com/api/v2/sql?format=GeoJSON";

    public CartoDBParkingSpotsQuery(ParkingSpotsUpdateListener listener) {
        super(listener);
    }


    @Override
    protected Set<ParkingSpot> doInBackground(Void... voids) {

        Set<ParkingSpot> spots = new HashSet<ParkingSpot>();

        Log.i(TAG, "Retrieving parking spots " + latLngBounds);
        String sqlString = buildSQL();

        JSONObject json = doQuery(sqlString);
        if(json == null)
            listener.onError(this);
        try {
            JSONArray rows = json.getJSONArray("features");
            for (int i = 0; i < rows.length(); i++) {

                JSONObject entry = rows.getJSONObject(i);

                JSONObject properties = entry.getJSONObject("properties");
                String id = properties.getString("id");
                Date date = dateFormat.parse(properties.getString("created_at"));

                JSONObject geometry = entry.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                double latitude = coordinates.getDouble(1);
                double longitude = coordinates.getDouble(0);

                ParkingSpot spot = new ParkingSpot(id, new LatLng(latitude, longitude), date);
                if(mode == Mode.closestSpots) spot.setClosest(true);
                spots.add(spot);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }


        return spots;
    }

    private String buildSQL() {

        if (mode == Mode.viewPort) {

            if (latLngBounds == null)
                throw new IllegalStateException("There must be a latLngBound set as a viewport to build the SQL query.");

            return String.format(
                    Locale.ENGLISH,
                    "SELECT * FROM %s " +
                            "WHERE the_geom && ST_MakeEnvelope(%f, %f, %f, %f, 4326)",
                    getTableId(),
                    latLngBounds.southwest.longitude,
                    latLngBounds.southwest.latitude,
                    latLngBounds.northeast.longitude,
                    latLngBounds.northeast.latitude
            );
        }

        else if (mode == Mode.closestSpots) {

            if (center == null || limit == null)
                throw new IllegalStateException("There must be a center and a limit in the number of spots set to build the SQL query.");

            return String.format(
                    Locale.ENGLISH,
                    "SELECT * FROM %s " +
                            "ORDER BY the_geom <-> ST_SetSRID(ST_Point(%f, %f),4326) " +
                            "LIMIT %d",
                    getTableId(),
                    center.longitude,
                    center.latitude,
                    limit.intValue()
            );
        }

        else {
            throw new IllegalStateException("Did you introduce a new mode?");
        }
    }

    private JSONObject doQuery(String sql) {
        try {

            String url = URL + "&q=" + URLEncoder.encode(sql, "ISO-8859-1");

            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");
            httpGet.setHeader("Content-type", "application/json");

            Log.i(TAG, "Getting\n" + url);

            HttpResponse response = httpclient.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();

            Log.i(TAG, "Query result: " + statusLine.getStatusCode());
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                String result = EntityUtils.toString(response.getEntity());
                Log.i(TAG, "Query result: " + result);
                JSONObject json = new JSONObject(result);
                return json;
            } else {
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
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
