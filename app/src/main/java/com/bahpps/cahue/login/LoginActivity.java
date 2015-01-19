package com.bahpps.cahue.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import android.content.Context;
import android.content.Intent;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.bahpps.cahue.BaseActivity;
import com.bahpps.cahue.Endpoints;
import com.bahpps.cahue.MapsActivity;
import com.bahpps.cahue.R;
import com.bahpps.cahue.util.CommUtil;
import com.bahpps.cahue.util.Util;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.gcm.GoogleCloudMessaging;

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
 * A login screen that offers login via Google+
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    static final String SENDER_ID = "582791978228";

    // UI references.
    private View mProgressView;
    private SignInButton mPlusSignInButton;

    private GoogleCloudMessaging gcm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Find the Google+ sign in button.
        mPlusSignInButton = (SignInButton) findViewById(R.id.plus_sign_in_button);
        mProgressView = findViewById(R.id.login_progress);

        if (checkPlayServices()) {
            // Set a listener to connect the user when the G+ button is clicked.
            mPlusSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    signIn();
                }
            });
        } else {
            // Don't offer G+ sign in if the app's version is too low to support Google Play
            // Services.
            // TODO: set error message
            mPlusSignInButton.setVisibility(View.GONE);
            return;
        }

    }

    @Override
    protected void onSignInRequired() {

    }

    private String getGCMRegId(){

        gcm = GoogleCloudMessaging.getInstance(this);

        String regId = GCMUtil.getRegistrationId(this);

        if (regId.isEmpty()) {

            try {
                regId = gcm.register(SENDER_ID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Device registered, registration ID: " + regId);

            // For this demo: we don't need to send it because the device
            // will send upstream messages to a server that echo back the
            // message using the 'from' address in the message.

            // Persist the regID - no need to register again.
            GCMUtil.storeRegistrationId(this, regId);
        }

        return regId;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    public void showProgress(final boolean show) {
        int animTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);

        mProgressView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        mProgressView.animate().setDuration(animTime).alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mProgressView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                    }
                });

        mPlusSignInButton.setVisibility(!show ? View.VISIBLE : View.INVISIBLE);
        mPlusSignInButton.animate().setDuration(animTime).alpha(!show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mPlusSignInButton.setVisibility(!show ? View.VISIBLE : View.INVISIBLE);
                    }
                });
    }

    @Override
    protected void onPlusClientSignIn() {
        Log.d(TAG, "onPlusClientSignIn");

        new LoginAsyncTask(this).execute();

    }

    @Override
    protected void onConnectingStatusChange(boolean connecting) {
        if (!isFinishing())
            showProgress(connecting);
    }

    @Override
    protected void onPlusClientSignOut() {
        Log.d(TAG, "onPlusClientSignOut");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public class LoginAsyncTask extends AsyncTask<Void, Void, Integer> {

        private Context context;

        public LoginAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected Integer doInBackground(Void... voids) {

            try {

                // set up a device registration ID
                String registrationId = getGCMRegId();

                Uri.Builder builder = new Uri.Builder();
                builder.scheme("https")
                        .authority(Endpoints.BASE_URL)
                        .appendPath(Endpoints.USERS_PATH)
                        .appendPath(Endpoints.CREATE_USER_GOOGLE_PATH);

                HttpPost httpPost = CommUtil.createHttpPost(context, builder.build().toString());
                httpPost.setEntity(new StringEntity(getJSON(registrationId)));
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = httpclient.execute(httpPost);
                StatusLine statusLine = response.getStatusLine();

                Log.i(TAG, "Post result: " + statusLine.getStatusCode());
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

                    Log.i(TAG, "Post result: " + EntityUtils.toString(response.getEntity()));

                } else {

                    //Closes the connection.
                    if (response != null && response.getEntity() != null) {
                        response.getEntity().getContent().close();
                        Log.e(TAG, statusLine.getReasonPhrase());
                    }
                }

                return statusLine.getStatusCode();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return -1;
        }

        @Override
        protected void onPostExecute(Integer httpResult) {
            if(httpResult == HttpStatus.SC_OK){
                onBackEndLogin();
            } else{
            }
            showProgress(false);
        }
    }

    private void onBackEndLogin() {
        Util.setIsLoggedIn(this, true);
        startActivity(new Intent(this, MapsActivity.class));
        finish();
    }

    private static String getJSON(String regId) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("deviceRegId", regId);
            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}



