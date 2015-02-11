package com.bahpps.cahue.cars;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by francesco on 11.02.2015.
 */
public class CarSyncBean {

    List<Car> cars;

    List<Car> deletedCars;

    public JSONObject toJSON() {
        try {
            JSONObject object = new JSONObject();
            JSONArray savedArray = new JSONArray();
            for (Car savedCar : cars) {
                savedArray.put(savedCar.toJSON());
            }
            object.put("cars", savedArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
