package com.cahue.iweco.login;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.RequestFuture;
import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.ParkifyApp;
import com.cahue.iweco.util.Requests;

import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

/**
 * Created by francesco on 22.01.2015.
 */
public class LoginAsyncTask extends AsyncTask<Void, Void, LoginResultBean> {

    private static final String TAG = LoginAsyncTask.class.getSimpleName();
    private final LoginListener loginResultListener;
    private final String registrationId;
    private final String authToken;
    private final Context context;
    private final LoginType type;

    public LoginAsyncTask(String gcmId, String authToken, LoginListener loginResultListener, Context context, LoginType type) {
        this.registrationId = gcmId;
        this.authToken = authToken;
        this.loginResultListener = loginResultListener;
        this.context = context;
        this.type = type;
    }

    @Nullable
    @Override
    protected LoginResultBean doInBackground(Void... voids) {

        if (registrationId == null || authToken == null) {
            throw new IllegalStateException("No device registration ID or OAuth token while trying to register");
        }

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(BuildConfig.BACKEND_URL)
                .appendPath("users");


        if (type == LoginType.Google) {
            builder.appendPath("registerGoogle")
                    .appendQueryParameter("deviceRegId", registrationId)
                    .appendQueryParameter("googleAuthToken", authToken);
        } else if (type == LoginType.Facebook) {
            builder.appendPath("registerFacebook")
                    .appendQueryParameter("deviceRegId", registrationId)
                    .appendQueryParameter("facebookAuthToken", authToken);
        } else {
            throw new IllegalStateException("What did you do?");
        }

        RequestQueue queue = ParkifyApp.getParkifyApp().getRequestQueue();

        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        String url = builder.build().toString();
        Log.d(TAG, url);
        JsonRequest stringRequest = new Requests.JsonPostFormRequest(
                context,
                url,
                null,
                future,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull VolleyError error) {
                        error.printStackTrace();
                        loginResultListener.onLoginError(authToken, type);
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        try {
            JSONObject jsonResult = future.get();
            LoginResultBean loginResultBean = LoginResultBean.fromJSON(jsonResult);

            Log.i(TAG, "Post result: " + jsonResult);

            return loginResultBean;
        } catch (InterruptedException | ExecutionException e) {
        }

        return null;
    }

    @Override
    protected void onPostExecute(LoginResultBean response) {
        Log.d(TAG, "Login ok");
        loginResultListener.onBackEndLogin(response, type);
    }

    public interface LoginListener {

        void onBackEndLogin(LoginResultBean loginResult, LoginType type);

        void onLoginError(String authToken, LoginType type);
    }
}
