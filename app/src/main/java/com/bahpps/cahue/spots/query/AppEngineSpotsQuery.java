package com.bahpps.cahue.spots.query;

import android.util.Log;

import com.bahpps.cahue.spots.ParkingSpot;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Set;

/**
 * Created by francesco on 11.12.2014.
 */
public class AppEngineSpotsQuery extends ParkingSpotsQuery {
    public AppEngineSpotsQuery(ParkingSpotsUpdateListener listener) {
        super(listener);
    }

    @Override
    protected Set<ParkingSpot> doInBackground(Void... voids) {

        if (mode == Mode.viewPort) {

            if (latLngBounds == null)
                throw new IllegalStateException("There must be a latLngBound set as a viewport to build the SQL query.");

            return String.format(
                    Locale.ENGLISH,
                    "SELECT created_at, id, the_geom FROM %s " +
                            "WHERE the_geom && ST_MakeEnvelope(%f, %f, %f, %f, 4326)",
                    getTableId(),
                    latLngBounds.southwest.longitude,
                    latLngBounds.southwest.latitude,
                    latLngBounds.northeast.longitude,
                    latLngBounds.northeast.latitude
            );
        } else if (mode == Mode.closestSpots) {

            if (center == null || limit == null)
                throw new IllegalStateException("There must be a center and a limit in the number of spots set to build the SQL query.");

            return String.format(
                    Locale.ENGLISH,
                    "SELECT created_at, id, the_geom FROM %s " +
                            "ORDER BY the_geom <-> ST_SetSRID(ST_Point(%f, %f),4326) " +
                            "LIMIT %d",
                    getTableId(),
                    center.longitude,
                    center.latitude,
                    limit.intValue()
            );
        } else {
            throw new IllegalStateException("Did you introduce a new mode?");
        }
        return null;
    }

    private Set<ParkingSpot> query(String url){
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
    }
}
