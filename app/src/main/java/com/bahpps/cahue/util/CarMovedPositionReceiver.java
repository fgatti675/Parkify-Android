package com.bahpps.cahue.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * This class is in charge of receiving location updates, and store it as the cars position. It is in charge of deciding
 * the most accurate position (where we think the car is), based on wifi and gps position providers.
 *
 * @author Francesco
 */
public class CarMovedPositionReceiver extends LocationPoller {


    private final static String TAG = "CarMovedPositionReceiver";
    private final static String URL = "http://glossy-radio.appspot.com/spots";

    @Override
    public void onLocationPolled(Context context, final Location location) {

        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] objects) {
                try {

                    Log.i(TAG, "Posting users location");

                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(URL);
                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");

                    String json = getJSON(location);
                    Log.i(TAG, "Posting\n" + json);
                    httpPost.setEntity(new StringEntity(json));

                    HttpResponse response = httpclient.execute(httpPost);
                    StatusLine statusLine = response.getStatusLine();

                    Log.i(TAG, "Post result: " + statusLine.getStatusCode());
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        Log.i(TAG, "Post result: " + EntityUtils.toString(response.getEntity()));
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
                }

                return null;
            }
        }.execute();

    }


    private static String getJSON(Location location) {
        return String.format(Locale.ENGLISH,
                "{\n" +
                        "    \"latitude\" : \"%f\",\n" +
                        "    \"longitude\": \"%f\"\n" +
                        "}",
                location.getLatitude(),
                location.getLongitude());
    }

}
