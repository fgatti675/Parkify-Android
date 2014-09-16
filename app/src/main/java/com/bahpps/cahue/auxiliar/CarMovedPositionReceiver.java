package com.bahpps.cahue.auxiliar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.bahpps.cahue.location.LocationPoller;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * This class is in charge of receiving location updates, and store it as the cars position. It is in charge of deciding
 * the most accurate position (where we think the car is), based on wifi and gps position providers.
 *
 * @author Francesco
 */
public class CarMovedPositionReceiver extends BroadcastReceiver {

    private final static String URL = "http://glossy-radio.appspot.com/spots";


    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            Location lastCarLocation = CarLocationManager.getStoredLocation(context);

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setEntity(new StringEntity(getJSON(lastCarLocation)));

            HttpResponse response = httpclient.execute(httpPost);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
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
    }

    private String getJSON(Location location) {
        return String.format(Locale.ENGLISH,
                "{\n" +
                        "    \"latitude\" : \"%d\",\n" +
                        "    \"longitude\": \"%d\"\n" +
                        "}",
                location.getLatitude(),
                location.getLongitude());
    }

}
