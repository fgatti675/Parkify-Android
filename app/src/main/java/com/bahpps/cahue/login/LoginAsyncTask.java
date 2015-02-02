package com.bahpps.cahue.login;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.bahpps.cahue.Endpoints;
import com.bahpps.cahue.cars.Car;
import com.bahpps.cahue.spots.ParkingSpot;
import com.bahpps.cahue.util.CommUtil;
import com.bahpps.cahue.util.Util;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.ParseException;

/**
 * Created by francesco on 22.01.2015.
 */
public class LoginAsyncTask extends AsyncTask<Void, Void, LoginResultBean> {

    public interface LoginListener {

        public void onBackEndLogin(LoginResultBean loginResult);

        public void onLoginError();
    }


    private static final String TAG = LoginAsyncTask.class.getSimpleName();

    boolean error = false;

    private final LoginListener loginResultListener;

    private final String registrationId;
    private final String authToken;
    private Context context;

    public LoginAsyncTask(String gcmId, String authToken, LoginListener loginResultListener, Context context) {
        this.registrationId = gcmId;
        this.authToken = authToken;
        this.loginResultListener = loginResultListener;
        this.context = context;
    }

    @Override
    protected LoginResultBean doInBackground(Void... voids) {

        HttpResponse response = null;

        try {

            if (registrationId == null || authToken == null) {
                throw new IllegalStateException("No device registration ID or OAuth token while trying to register");
            }

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority(Endpoints.BASE_URL)
                    .appendPath(Endpoints.USERS_PATH)
                    .appendPath(Endpoints.CREATE_USER_GOOGLE_PATH);

            HttpPost httpPost = CommUtil.createHttpPost(context, builder.build().toString());
            String json = createRegistrationJSON(registrationId, authToken);
            Log.d(TAG, json);
            httpPost.setEntity(new StringEntity(json));
            HttpClient httpclient = new DefaultHttpClient();

            response = httpclient.execute(httpPost);
            StatusLine statusLine = response.getStatusLine();

            Log.i(TAG, "Post result: " + statusLine.getStatusCode());
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

                try {
                    LoginResultBean loginResultBean = new LoginResultBean();

                    JSONObject jsonResult = new JSONObject(EntityUtils.toString(response.getEntity()));

                    loginResultBean.authToken = jsonResult.getString("authToken");
                    loginResultBean.email = jsonResult.getString("email");
                    loginResultBean.googleId = jsonResult.getString("googleId");

                    JSONArray carsArray = jsonResult.getJSONArray("cars");
                    for (int i = 0; i < carsArray.length(); i++) {

                        JSONObject carJSON = carsArray.getJSONObject(i);

                        Car car = Car.fromJSON(carJSON);
                        loginResultBean.cars.add(car);
                    }
                    //TODO: cars

                    Log.i(TAG, "Post result: " + jsonResult);

                    return loginResultBean;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {

                //Closes the connection.
                if (response != null && response.getEntity() != null) {
                    response.getEntity().getContent().close();
                    Log.e(TAG, statusLine.getReasonPhrase());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        error = true;

        return null;
    }

    @Override
    protected void onPostExecute(LoginResultBean response) {

        if (error) {
            // error
            Log.d(TAG, "Login error");
            loginResultListener.onLoginError();
            return;
        }

        Log.d(TAG, "Login ok");
        loginResultListener.onBackEndLogin(response);

    }

    private static String createRegistrationJSON(String regId, String authToken) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("deviceRegId", regId);
            obj.put("googleAuthToken", authToken);
            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
