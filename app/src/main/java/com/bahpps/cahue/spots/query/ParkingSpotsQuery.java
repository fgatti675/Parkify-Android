package com.bahpps.cahue.spots.query;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by francesco on 13.11.2014.
 */
public abstract class ParkingSpotsQuery extends AsyncTask<Void, Void, Set<ParkingSpot>> {


    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String TAG = NearestSpotsQuery.class.getSimpleName();

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    private Context context;
    protected ParkingSpotsUpdateListener listener;


    public ParkingSpotsQuery(Context context, ParkingSpotsUpdateListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Set<ParkingSpot> parkingSpots) {

        Log.d("ParkingSpotsQuery", parkingSpots.toString());
        super.onPostExecute(parkingSpots);
        listener.onSpotsUpdate(this, parkingSpots);
    }

    protected JSONObject query(String url) {
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
                listener.onServerError(this, statusLine.getStatusCode(), statusLine.getReasonPhrase());
                throw new IOException(statusLine.getReasonPhrase());
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {

            // now this is ugly
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onIOError();
                    }
                });
            }
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected Set<ParkingSpot> parseResult(JSONObject result) {
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

    /**
     * Components that use this service must implement a listener using this interface to get the
     * parking locations
     */
    public interface ParkingSpotsUpdateListener {
        void onSpotsUpdate(ParkingSpotsQuery query, Set<ParkingSpot> parkingSpots);

        void onServerError(ParkingSpotsQuery query, int statusCode, String reasonPhrase);

        void onIOError();
    }
}
