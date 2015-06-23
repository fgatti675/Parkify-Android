package com.cahue.iweco.login;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.cahue.iweco.R;
import com.cahue.iweco.util.Requests;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by francesco on 22.01.2015.
 */
public class LoginAsyncTask extends AsyncTask<Void, Void, LoginResultBean> {

    private static final String TAG = LoginAsyncTask.class.getSimpleName();
    private final LoginListener loginResultListener;
    private final String registrationId;
    private final String authToken;
    boolean error = false;
    private Context context;
    private LoginType type;

    public LoginAsyncTask(String gcmId, String authToken, LoginListener loginResultListener, Context context, LoginType type) {
        this.registrationId = gcmId;
        this.authToken = authToken;
        this.loginResultListener = loginResultListener;
        this.context = context;
        this.type = type;
    }

    private static String createGoogleRegistrationForm(String regId, String authToken) {
        return String.format("deviceRegId=%s&googleAuthToken=%s", regId, authToken);
    }

    private static String createFacebookRegistrationForm(String regId, String authToken) {
        return String.format("deviceRegId=%s&facebookAuthToken=%s", regId, authToken);
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
                    .authority(context.getResources().getString(R.string.baseURL))
                    .appendPath(context.getResources().getString(R.string.usersPath));

            String body;

            if (type == LoginType.Google) {
                builder.appendPath(context.getResources().getString(R.string.googlePath));
                body = createGoogleRegistrationForm(registrationId, authToken);
            } else if (type == LoginType.Facebook) {
                builder.appendPath(context.getResources().getString(R.string.facebookPath));
                body = createFacebookRegistrationForm(registrationId, authToken);
            } else {
                throw new IllegalStateException("What did you do?");
            }

            HttpPost httpPost = Requests.createHttpFormPost(context, builder.build().toString());
            Log.d(TAG, body);
            httpPost.setEntity(new StringEntity(body));
            HttpClient httpclient = new DefaultHttpClient();

            response = httpclient.execute(httpPost);
            StatusLine statusLine = response.getStatusLine();

            Log.i(TAG, "Post result: " + statusLine.getStatusCode());
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

                try {

                    JSONObject jsonResult = new JSONObject(EntityUtils.toString(response.getEntity()));
                    LoginResultBean loginResultBean = LoginResultBean.fromJSON(jsonResult);

                    Log.i(TAG, "Post result: " + jsonResult);

                    return loginResultBean;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {

                // Closes the connection.
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
            loginResultListener.onLoginError(authToken);
            return;
        }

        Log.d(TAG, "Login ok");
        loginResultListener.onBackEndLogin(response);

    }

    public interface LoginListener {

        void onBackEndLogin(LoginResultBean loginResult);

        void onLoginError(String authToken);
    }
}
