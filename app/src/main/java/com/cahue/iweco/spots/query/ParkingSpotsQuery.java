package com.cahue.iweco.spots.query;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.cahue.iweco.spots.ParkingSpot;
import com.cahue.iweco.util.Util;
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
import java.util.HashSet;
import java.util.Set;

/**
 * Created by francesco on 13.11.2014.
 */
public abstract class ParkingSpotsQuery extends AsyncTask<Void, Void, QueryResult> {


    private static final String TAG = ParkingSpotsQuery.class.getSimpleName();

    protected Context context;
    protected ParkingSpotsUpdateListener listener;


    public ParkingSpotsQuery(Context context, ParkingSpotsUpdateListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected abstract QueryResult doInBackground(Void... voids);

    @Override
    protected void onPostExecute(final QueryResult result) {

        Log.d("ParkingSpotsQuery", result.toString());
        super.onPostExecute(result);
        if(context instanceof Activity){
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onSpotsUpdate(ParkingSpotsQuery.this, result);
                }
            });
        }
    }

    protected JSONObject query(String url) {
        try {

            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");

            Log.i(TAG, "Getting\n" + url);

            final HttpResponse response = httpclient.execute(httpGet);
            final StatusLine statusLine = response.getStatusLine();

            Log.i(TAG, "Query result: " + statusLine.getStatusCode());
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                String result = EntityUtils.toString(response.getEntity());
                Log.i(TAG, "Query result: " + result);
                JSONObject json = new JSONObject(result);
                return json;
            } else {
                if(context instanceof Activity){
                    response.getEntity().getContent().close();
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Closes the connection.
                            listener.onServerError(ParkingSpotsQuery.this, statusLine.getStatusCode(), statusLine.getReasonPhrase());
                        }
                    });
                }
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

    protected QueryResult parseResult(JSONObject jsonObject) {

        QueryResult result = new QueryResult();

        Set<ParkingSpot> spots = new HashSet<>();

        result.spots = spots;

        if (jsonObject != null) {
            try {

                JSONArray spotsArray = jsonObject.getJSONArray("spots");
                for (int i = 0; i < spotsArray.length(); i++) {
                    JSONObject entry = spotsArray.getJSONObject(i);
                    ParkingSpot spot = ParkingSpot.fromJSON(entry);
                    spots.add(spot);
                }

                result.moreResults = jsonObject.getBoolean("moreResults");
                result.error = jsonObject.getBoolean("error");

            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException("Error parsing spots jsonObject", e);
            }
        }

        return result;
    }

    /**
     * Components that use this service must implement a listener using this interface to get the
     * parking locations
     */
    public interface ParkingSpotsUpdateListener {

        void onSpotsUpdate(ParkingSpotsQuery query, QueryResult result);

        void onServerError(ParkingSpotsQuery query, int statusCode, String reasonPhrase);

        void onIOError();

    }
}
