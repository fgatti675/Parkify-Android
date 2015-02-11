package com.bahpps.cahue.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import android.content.Intent;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.bahpps.cahue.BaseActivity;
import com.bahpps.cahue.MapsActivity;
import com.bahpps.cahue.R;
import com.bahpps.cahue.cars.CarDatabase;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import java.io.IOException;


/**
 * A login screen that offers login via Google+
 */
public class LoginActivity extends BaseActivity implements LoginAsyncTask.LoginListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private final static String SCOPE = "oauth2:https://www.googleapis.com/auth/plus.login https://www.googleapis.com/auth/userinfo.email";

    // UI references.
    private View mProgressView;
    private SignInButton mPlusSignInButton;

    private String mLoggedEmail;

    private GoogleCloudMessaging gcm;
    private CarDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        database = new CarDatabase(this);

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

    protected void onGoogleAuthTokenSet(String authToken) {
        setLoading(false);
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

    private void requestOauthToken() {

        Log.i(TAG, "requestOauthToken");

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void[] objects) {
                String mGoogleAuthToken = null;
                try {
                    mGoogleAuthToken = GoogleAuthUtil.getToken(LoginActivity.this, mLoggedEmail, SCOPE);
                } catch (UserRecoverableAuthException userRecoverableException) {
                    // GooglePlayServices.apk is either old, disabled, or not present
                    // so we need to show the user some UI in the activity to recover.
                    onSignInRequired();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GoogleAuthException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Google oAuth token: " + mGoogleAuthToken);
                return mGoogleAuthToken;
            }

            @Override
            protected void onPostExecute(String authToken) {
                if (authToken == null) {
                    // TODO: nicer
                    Toast.makeText(LoginActivity.this, "Error with Google auth", Toast.LENGTH_SHORT).show();
                    setLoading(false);
                } else {
                    onGoogleAuthTokenSet(authToken);
                }
            }

        }.execute();


    }


    @Override
    protected void onPlusClientSignIn() {
        Log.d(TAG, "onPlusClientSignIn");

        getProfileInformation();
        requestOauthToken();
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

        AuthUtils.setIsLoggedIn(this, true);

        AuthUtils.saveOAuthToken(this, loginResult.authToken);

        database.saveCars(loginResult.cars);

        startActivity(new Intent(this, MapsActivity.class));
        finish();
    }

    @Override
    public void onLoginError() {
        setLoading(false);
        // TODO: error toast
        Toast.makeText(this, "Login error", Toast.LENGTH_SHORT).show();
    }


    // Profile pic image size in pixels
    private final static int PROFILE_PIC_SIZE = 400;

    /**
     * Fetching user's information name, mLoggedEmail, profile pic
     */
    private void getProfileInformation() {
        try {
            if (Plus.PeopleApi.getCurrentPerson(getGoogleApiClient()) != null) {
                Person currentPerson = Plus.PeopleApi
                        .getCurrentPerson(getGoogleApiClient());
                String personName = currentPerson.getDisplayName();
                String personPhotoUrl = currentPerson.getImage().getUrl();
                String personGooglePlusProfile = currentPerson.getUrl();
                mLoggedEmail = Plus.AccountApi.getAccountName(getGoogleApiClient());

                Log.e(TAG, "Name: " + personName + ", plusProfile: "
                        + personGooglePlusProfile + ", mLoggedEmail: " + mLoggedEmail
                        + ", Image: " + personPhotoUrl);

//                txtName.setText(personName);
//                txtEmail.setText(mLoggedEmail);

                // by default the profile url gives 50x50 px image only
                // we can replace the value with whatever dimension we want by
                // replacing sz=X
                personPhotoUrl = personPhotoUrl.substring(0,
                        personPhotoUrl.length() - 2)
                        + PROFILE_PIC_SIZE;

//                new LoadProfileImage(imgProfilePic).execute(personPhotoUrl);

            } else {
                Toast.makeText(getApplicationContext(),
                        "Person information is null", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



