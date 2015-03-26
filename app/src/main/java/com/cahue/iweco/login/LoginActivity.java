package com.cahue.iweco.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.cahue.iweco.BaseActivity;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.auth.Authenticator;
import com.cahue.iweco.cars.database.CarsProvider;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import java.io.IOException;
import java.util.List;


/**
 * A login screen that offers login via Google+
 */
public class LoginActivity extends BaseActivity implements LoginAsyncTask.LoginListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private final static String SCOPE = "oauth2:https://www.googleapis.com/auth/plus.login https://www.googleapis.com/auth/userinfo.email";


    // UI references.
    private View mProgressView;
    private View mButtonsLayout;

    private Button mPlusSignInButton;
    private GoogleCloudMessaging gcm;
    private CarDatabase database;
    private AccountManager mAccountManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mButtonsLayout = findViewById(R.id.buttons);

        database = CarDatabase.getInstance(this);

        mAccountManager = AccountManager.get(this);

        // Find the Google+ sign in button.
        mPlusSignInButton = (Button) findViewById(R.id.plus_sign_in_button);
        mProgressView = findViewById(R.id.login_progress);

        if (checkPlayServices()) {
            // Set a listener to connect the user when the G+ button is clicked.
            mPlusSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    login();
                }
            });
        } else {
            // Don't offer G+ sign in if the app's version is too low to support Google Play
            // Services.
            // TODO: set error message
            mPlusSignInButton.setVisibility(View.GONE);
            return;
        }
        Button noSingIn = (Button) findViewById(R.id.no_sign_in);
        noSingIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthUtils.setSkippedLogin(LoginActivity.this, true);
                goToMaps();
            }
        });

    }

    private void goToMaps() {
        startActivity(new Intent(this, MapsActivity.class));
        finish();
    }

    private void login() {
        setLoading(true);
        reconnect();
        signIn();
    }

    @Override
    protected void onSignInRequired() {
        signIn();
    }

    protected void onGoogleAuthTokenSet(final String authToken) {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                try {
                    return getGCMRegId();
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onGCMError();
                        }
                    });
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String regId) {
                if (regId != null) onGCMRegIdSet(regId, authToken);
            }
        }.execute();
    }

    private void onGCMRegIdSet(String gcmRegId, String authToken) {
        new LoginAsyncTask(gcmRegId, authToken, this, this).execute();
    }

    private void onGCMError() {
        setLoading(false);
        Toast.makeText(this, R.string.gcm_error, Toast.LENGTH_SHORT).show();
    }

    private String getGCMRegId() throws IOException {

        gcm = GoogleCloudMessaging.getInstance(this);

        String regId = GCMUtil.getRegistrationId(this);

        if (regId.isEmpty()) {
            regId = gcm.register(GCMUtil.SENDER_ID); // TODO: this can crash
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

        mButtonsLayout.setVisibility(!show ? View.VISIBLE : View.INVISIBLE);
        mButtonsLayout.animate().setDuration(animTime).alpha(!show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mButtonsLayout.setVisibility(!show ? View.VISIBLE : View.INVISIBLE);
                    }
                });
    }

    private void requestOauthToken(final String email) {

        Log.i(TAG, "requestOauthToken");

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void[] objects) {
                String mGoogleAuthToken = null;
                try {
                    mGoogleAuthToken = GoogleAuthUtil.getToken(LoginActivity.this, email, SCOPE);
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
                    onTokenRetrieveError();
                } else {
                    onGoogleAuthTokenSet(authToken);
                }
            }

        }.execute();


    }

    private void onTokenRetrieveError() {
        Toast.makeText(this, "Error Google auth", Toast.LENGTH_SHORT).show();
        setLoading(false);
    }

    @Override
    protected boolean autoConnect() {
        return false;
    }

    @Override
    protected void onPlusClientSignIn() {
        Log.d(TAG, "onPlusClientSignIn");
        setLoading(true);
        String email = Plus.AccountApi.getAccountName(getGoogleApiClient());
        requestOauthToken(email);
    }

    @Override
    protected void onConnectingStatusChange(boolean connecting) {
//        if (!isFinishing())
//            setLoading(connecting);
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

        /**
         * Maybe there was some data there already due to that the app was being used
         * without signing in
         */
        List<Car> cars = database.retrieveCars(false);
        for(Car car: cars)
            CarsSync.postCar(car, this, CarDatabase.getInstance(this));

        database.save(loginResult.cars);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(AccountManager.KEY_ACCOUNT_NAME, loginResult.email);
        resultIntent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
        resultIntent.putExtra(AccountManager.KEY_AUTHTOKEN, loginResult.authToken);
        resultIntent.putExtra(AccountManager.KEY_PASSWORD, loginResult.refreshToken);

        finishLogin(resultIntent, loginResult.userId);

        goToMaps();
    }

    private void finishLogin(Intent intent, String userId) {

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        String authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String refreshToken = intent.getStringExtra(AccountManager.KEY_PASSWORD);
        String authTokenType = Authenticator.ACCOUNT_TYPE;

        // Creating the account on the device and setting the auth token we got
        // (Not setting the auth token will cause another call to the server to authenticate the user)
        Bundle bundle = new Bundle();
        bundle.putString(Authenticator.USER_ID, userId);
        mAccountManager.addAccountExplicitly(account, refreshToken, bundle);
        mAccountManager.setAuthToken(account, authTokenType, authToken);

        // Inform the system that this account supports sync
        ContentResolver.setIsSyncable(account, CarsProvider.CONTENT_AUTHORITY, 1);
        // Inform the system that this account is eligible for auto sync when the network is up
        ContentResolver.setSyncAutomatically(account, CarsProvider.CONTENT_AUTHORITY, true);
        // Recommend a schedule for automatic synchronization. The system may modify this based
        // on other scheduled syncs and network utilization.
//        ContentResolver.addPeriodicSync(
//                account, CarsProvider.CONTENT_AUTHORITY, new Bundle(), CarsProvider.SYNC_FREQUENCY);

        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onLoginError() {
        setLoading(false);
        Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show();
    }


    // Profile pic image size in pixels
    private final static int PROFILE_PIC_SIZE = 400;

    /**
     * Fetching user's information name, mLoggedEmail, profile pic
     */
    @Deprecated
    private void getProfileInformation() {
        try {
            String email = Plus.AccountApi.getAccountName(getGoogleApiClient());

            if (Plus.PeopleApi.getCurrentPerson(getGoogleApiClient()) != null) {


                Person currentPerson = Plus.PeopleApi
                        .getCurrentPerson(getGoogleApiClient());
                String personName = currentPerson.getDisplayName();
                String personPhotoUrl = currentPerson.getImage().getUrl();
                String personGooglePlusProfile = currentPerson.getUrl();

                Log.e(TAG, "Name: " + personName + ", plusProfile: "
                        + personGooglePlusProfile + ", email: " + email
                        + ", Image: " + personPhotoUrl);

//                txtName.setText(personName);
//                txtEmail.setText(email);

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



