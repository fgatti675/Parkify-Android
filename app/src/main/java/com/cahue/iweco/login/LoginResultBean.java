package com.cahue.iweco.login;

import com.cahue.iweco.cars.Car;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by francesco on 22.01.2015.
 */
public class LoginResultBean {

    String userId;

    String email;

    String googleId;

    String facebookId;

    Set<Car> cars = new HashSet<>();

    String refreshToken;

    String authToken;

    public static LoginResultBean fromJSON(JSONObject json) {
        try {
            LoginResultBean loginResultBean = new LoginResultBean();

            loginResultBean.authToken = json.getString("authToken");
            loginResultBean.refreshToken = json.getString("refreshToken");
            JSONObject userObject = json.getJSONObject("user");
            loginResultBean.userId = userObject.getString("id");

            if (userObject.has("googleUser")) {
                loginResultBean.email = userObject.getJSONObject("googleUser").getString("email");
                loginResultBean.googleId = userObject.getJSONObject("googleUser").getString("googleId");
            }

            if (userObject.has("facebookUser")) {
                loginResultBean.email = userObject.getJSONObject("facebookUser").getString("email");
                loginResultBean.facebookId = userObject.getJSONObject("facebookUser").getString("facebookId");
            }

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
