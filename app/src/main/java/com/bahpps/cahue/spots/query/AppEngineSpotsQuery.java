package com.bahpps.cahue.spots.query;

import android.net.Uri;
import android.util.Log;

import com.bahpps.cahue.Endpoints;
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
import java.net.URI;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by francesco on 11.12.2014.
 */
public class AppEngineSpotsQuery extends ParkingSpotsQuery {

    private static final String TAG = AppEngineSpotsQuery.class.getSimpleName();

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public AppEngineSpotsQuery(ParkingSpotsUpdateListener listener) {
        super(listener);
    }

    @Override
    protected Set<ParkingSpot> doInBackground(Void... voids) {

        if (mode == Mode.viewPort) {

            if (latLngBounds == null)
                throw new IllegalStateException("There must be a latLngBound set as a viewport to build the SQL query.");


            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority(Endpoints.BASE_URL)
                    .appendPath(Endpoints.SPOTS_PATH)
                    .appendPath(Endpoints.RETRIEVE_AREA)
                    .appendQueryParameter("swLat", Double.toString(latLngBounds.southwest.latitude))
                    .appendQueryParameter("swLong", Double.toString(latLngBounds.southwest.longitude))
                    .appendQueryParameter("neLat", Double.toString(latLngBounds.northeast.latitude))
                    .appendQueryParameter("neLong", Double.toString(latLngBounds.northeast.longitude));

            String url = builder.build().toString();
            Log.i(TAG, "Query area : ");

            return parseResult(query(url));
        } else if (mode == Mode.closestSpots) {

            if (center == null || limit == null)
                throw new IllegalStateException("There must be a center and a limit in the number of spots set to build the SQL query.");

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority(Endpoints.BASE_URL)
                    .appendPath(Endpoints.SPOTS_PATH)
                    .appendPath(Endpoints.RETRIEVE_NEAREST)
                    .appendQueryParameter("lat", Double.toString(center.latitude))
                    .appendQueryParameter("long", Double.toString(center.longitude))
                    .appendQueryParameter("count", Integer.toString(limit));

            String url = builder.build().toString();
            Log.i(TAG, "Query nearest : ");

            return parseResult(query(url));
        } else {
            throw new IllegalStateException("Did you introduce a new mode?");
        }
    }

    private JSONObject query(String url) {
        try {

            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");

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

    private Set<ParkingSpot> parseResult(JSONObject result) {
        Set<ParkingSpot> spots = new HashSet<ParkingSpot>();

        if (result != null) {
            try {

                JSONArray spotsArray = result.getJSONArray("spots");
                for (int i = 0; i < spotsArray.length(); i++) {

                    JSONObject entry = spotsArray.getJSONObject(i);

                    ParkingSpot spot = new ParkingSpot(
                            entry.getLong("id"),
                            new LatLng(entry.getDouble("latitude"), entry.getDouble("longitude")),
                            dateFormat.parse(entry.getString("time"))
                    );
                    spots.add(spot);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return spots;
    }
}
