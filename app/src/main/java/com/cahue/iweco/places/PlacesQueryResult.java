package com.cahue.iweco.places;

import android.location.Location;
import android.support.annotation.NonNull;

import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Set;

/**
 * Created by f.gatti.gomez on 07/08/16.
 */
public class PlacesQueryResult {

    /**
     * The results included are not complete
     */
    public boolean moreResults = false;

    /**
     * Spots
     */
    public Set<Place> places;


    @NonNull
    public static ParkingSpot fromJSON(@NonNull JSONObject jsonObject) throws JSONException {
        try {
            Location location = new Location("Json");
            location.setLatitude(jsonObject.getDouble("latitude"));
            location.setLongitude(jsonObject.getDouble("longitude"));
            location.setAccuracy(Float.parseFloat(jsonObject.getString("accuracy")));
            return new ParkingSpot(
                    jsonObject.getLong("id"),
                    location,
                    null,
                    Util.DATE_FORMAT.parse(jsonObject.getString("time")),
                    jsonObject.optBoolean("future"));
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
