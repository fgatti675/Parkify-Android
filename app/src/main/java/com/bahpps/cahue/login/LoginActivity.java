package com.bahpps.cahue.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.bahpps.cahue.BaseActivity;
import com.bahpps.cahue.MapsActivity;
import com.bahpps.cahue.R;
import com.bahpps.cahue.util.Util;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;


/**
 * A login screen that offers login via Google+
 */
public class LoginActivity extends BaseActivity implements LoginAsyncTask.LoginListener {

    private static final String TAG = LoginActivity.class.getSimpleName();


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

    @Override
    protected void onAuthTokenSet(String authToken) {
        setLoading(true);
        String gcmRegId = getGCMRegId();
        new LoginAsyncTask(gcmRegId, authToken, this, this).execute();
    }

    private String getGCMRegId() {

        gcm = GoogleCloudMessaging.getInstance(this);

        String regId = GCMUtil.getRegistrationId(this);

        if (regId.isEmpty()) {

            try {
                regId = gcm.register(GCMUtil.SENDER_ID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Device registered, registration ID: " + regId);

            // Persist the regID - no need to register again.
            GCMUtil.storeRegistrationId(this, regId);
        }

        return regId;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    public void setLoading(final boolean show) {
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
    }

    @Override
    protected void onConnectingStatusChange(boolean connecting) {
        if (!isFinishing())
            setLoading(connecting);
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


    @Override
    public void onBackEndLogin(LoginResultBean loginResult) {
        Util.setIsLoggedIn(this, true);
        startActivity(new Intent(this, MapsActivity.class));
        finish();
    }

    @Override
    public void onLoginError() {
        setLoading(false);
        // TODO: error toast
    }
}



