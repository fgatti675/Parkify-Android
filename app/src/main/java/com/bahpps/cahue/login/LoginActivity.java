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

import com.bahpps.cahue.BaseActivity;
import com.bahpps.cahue.Endpoints;
import com.bahpps.cahue.MapsActivity;
import com.bahpps.cahue.R;
import com.bahpps.cahue.util.CommUtil;
import com.bahpps.cahue.util.Util;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;


/**
 * A login screen that offers login via Google+
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    // UI references.
    private View mProgressView;
    private SignInButton mPlusSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Find the Google+ sign in button.
        mPlusSignInButton = (SignInButton) findViewById(R.id.plus_sign_in_button);
        if (supportsGooglePlayServices()) {
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

        mProgressView = findViewById(R.id.login_progress);
    }

    @Override
    protected void onSignInRequired() {

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
        Util.setIsLoggedIn(this, true);
        startActivity(new Intent(this, MapsActivity.class));
        finish();
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

    /**
     * Check if the device supports Google Play Services.  It's best
     * practice to check first rather than handling this as an error case.
     *
     * @return whether the device supports Google Play Services
     */
    private boolean supportsGooglePlayServices() {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) ==
                ConnectionResult.SUCCESS;
    }


    public class LoginAsyncTask extends AsyncTask<Void, Void, Integer> {


        private Context context;

        public LoginAsyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected Integer doInBackground(Void... voids) {

            try {
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("https")
                        .authority(Endpoints.BASE_URL)
                        .appendPath(Endpoints.USERS_PATH)
                        .appendPath(Endpoints.CREATE_USER_GOOGLE_PATH);

                HttpPost httpPost = CommUtil.createHttpPost(context, builder.build().toString());

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


    }
}



