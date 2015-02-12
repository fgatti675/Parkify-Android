package com.bahpps.cahue.login;

import android.util.Log;

import com.bahpps.cahue.cars.Car;

import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by francesco on 22.01.2015.
 */
public class LoginResultBean {

    String email;

    String googleId;

    Set<Car> cars = new HashSet<>();

    String refreshToken;

    String authToken;

    public static LoginResultBean fromJSON(JSONObject json) {
        try {
            LoginResultBean loginResultBean = new LoginResultBean();

            loginResultBean.authToken = json.getString("authToken");
            loginResultBean.refreshToken = json.getString("refreshToken");
            loginResultBean.email = json.getJSONObject("user").getJSONObject("googleUser").getString("email");
            loginResultBean.googleId = json.getJSONObject("user").getJSONObject("googleUser").getString("googleId");

            if (json.has("cars")) {
                JSONArray carsArray = json.getJSONArray("cars");
                loginResultBean.cars = Car.fromJSONArray(carsArray);
            }

            return loginResultBean;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
