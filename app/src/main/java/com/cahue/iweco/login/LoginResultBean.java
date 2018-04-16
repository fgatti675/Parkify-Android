package com.cahue.iweco.login;

import android.support.annotation.NonNull;

import com.cahue.iweco.model.Car;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by francesco on 22.01.2015.
 */
public class LoginResultBean {

    String userId;

    String email;

    String googleId;

    String facebookId;

    List<Car> cars = new ArrayList<>();

    String refreshToken;

    String authToken;

    @NonNull
    public static LoginResultBean fromJSON(@NonNull JSONObject json) {
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
                JSONObject facebookUser = userObject.getJSONObject("facebookUser");
                loginResultBean.email = facebookUser.optString("email");
                loginResultBean.facebookId = facebookUser.getString("facebookId");
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
