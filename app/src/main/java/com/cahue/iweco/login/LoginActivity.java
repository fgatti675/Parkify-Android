package com.cahue.iweco.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.cahue.iweco.Constants;
import com.cahue.iweco.IwecoApp;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.cahue.iweco.auth.Authenticator;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;


/**
 * A login screen that offers login via Google+
 */
public class LoginActivity extends AppCompatActivity implements LoginAsyncTask.LoginListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private final static String SCOPE = "oauth2:https://www.googleapis.com/auth/plus.login https://www.googleapis.com/auth/plus.me https://www.googleapis.com/auth/userinfo.email";

    // A magic number we will use to know that our sign-in error resolution activity has completed
    private final static int OUR_REQUEST_CODE = 49404;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    // Profile pic image size in pixels
    private final static int PROFILE_PIC_SIZE = 200;

    // UI references.
    private View mProgressView;
    private View mButtonsLayout;
    private Button mPlusSignInButton;
    private Button mFacebookLoginButton;

    private GoogleCloudMessaging gcm;
    private CarDatabase database;

    private AccountManager mAccountManager;
    /**
     * A flag indicating that a PendingIntent is in progress and prevents us
     * from starting further intents.
     */
    private boolean mIntentInProgress;
    private boolean mSigningIn;
    // This is the helper object that connects to Google Play Services.
    private GoogleApiClient mGoogleApiClient;

    // The saved result from {@link #onConnectionFailed(ConnectionResult)}.  If a connection
    // attempt has been made, this is non-null.
    // If this IS null, then the connect method is still running.
    private ConnectionResult mConnectionResult;
    private Tracker tracker;
    private CallbackManager mFacebookCallbackManager;

    private AccessToken facebookAccessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        gcm = GoogleCloudMessaging.getInstance(this);

        if (savedInstanceState != null) {
            mIntentInProgress = savedInstanceState.getBoolean("mIntentInProgress");
        }

        setContentView(R.layout.activity_login);

        setUpGoogleApiClientIfNeeded();

        database = CarDatabase.getInstance(this);
        mAccountManager = AccountManager.get(this);

        mButtonsLayout = findViewById(R.id.buttons);

        // Find the Google+ sign in button.
        mPlusSignInButton = (Button) findViewById(R.id.plus_sign_in_button);
        mProgressView = findViewById(R.id.login_progress);

        if (checkPlayServices()) {
            // Set a listener to connect the user when the G+ button is clicked.
            mPlusSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    googleLogin();
                }
            });
        } else {
            // Don't offer G+ sign in if the app's version is too low to support Google Play
            // Services.
            // TODO: set error message
            mPlusSignInButton.setVisibility(View.GONE);
            return;
        }

        mFacebookLoginButton = (Button) findViewById(R.id.facebook_login_button);

        // Callback registration
        mFacebookCallbackManager = CallbackManager.Factory.create();

        final LoginManager loginManager = LoginManager.getInstance();
        mFacebookLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginManager.logInWithReadPermissions(LoginActivity.this, Arrays.asList("user_friends", "email"));
                setLoading(true);
            }
        });

        loginManager.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                facebookAccessToken = loginResult.getAccessToken();
                String facebookToken = facebookAccessToken.getToken();
                onAuthTokenSet(facebookToken, LoginType.Facebook);
                Log.d(TAG, facebookToken);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel");
                setLoading(false);
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d(TAG, exception.toString());
                setLoading(false);
            }
        });

        Button noSingIn = (Button) findViewById(R.id.no_sign_in);
        noSingIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthUtils.setSkippedLogin(LoginActivity.this, true);
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("UX")
                        .setAction("click")
                        .setLabel("Skip Login")
                        .build());
                goToMaps();
            }
        });

        tracker = ((IwecoApp) getApplication()).getTracker();
        tracker.setScreenName("Login");

    }

    private void setUpGoogleApiClientIfNeeded() {

        if (mGoogleApiClient == null) {

            // Initialize the PlusClient connection.
            // Scopes indicate the information about the user your application will be able to access.
            GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
//                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Plus.API)
                    .addScope(Plus.SCOPE_PLUS_LOGIN)
                    .addScope(Plus.SCOPE_PLUS_PROFILE)
                    .addScope(new Scope("https://www.googleapis.com/auth/userinfo.email"));

            mGoogleApiClient = builder.build();

        }
    }

    private void goToMaps() {
        startActivity(new Intent(this, MapsActivity.class));
        finish();
    }

    private void googleLogin() {

        setLoading(true);

        if (!mGoogleApiClient.isConnecting()) {
            // Show the dialog as we are now signing in.
            mSigningIn = true;
            resolveSignInError();
            onConnectingStatusChange(true);
        }

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        mGoogleApiClient.connect();

        onConnectingStatusChange(true);

        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("UX")
                .setAction("click")
                .setLabel("Google Login")
                .build());

    }

    protected void onAuthTokenSet(final String authToken, final LoginType type) {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                try {
                    return getGCMRegId();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String regId) {
                if (regId != null) onGCMRegIdSet(regId, authToken, type);
                else onGCMError(authToken);
            }
        }.execute();
    }

    private void onGCMRegIdSet(String gcmRegId, String authToken, LoginType type) {
        new LoginAsyncTask(gcmRegId, authToken, this, this, type).execute();
    }

    private void onGCMError(String authToken) {
        setLoading(false);
        clearGoogleToken(authToken);
        Toast.makeText(this, R.string.gcm_error, Toast.LENGTH_SHORT).show();
    }

    private void clearGoogleToken(String authToken) {
        try {
            GoogleAuthUtil.clearToken(this, authToken);
        } catch (GoogleAuthException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getGCMRegId() throws IOException {

        String gcmDeviceId = GCMUtil.getRegistrationId(this);

        if (gcmDeviceId.isEmpty()) {
            gcmDeviceId = gcm.register(GCMUtil.SENDER_ID); // TODO: this can crash
            Log.d(TAG, "Device registered, registration ID: " + gcmDeviceId);

            // Persist the regID - no need to register again.
            GCMUtil.storeRegistrationId(this, gcmDeviceId);
        }

        return gcmDeviceId;
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    protected boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    public void setLoading(final boolean show) {

        Log.i(TAG, "loading " + show);

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

    private void requestGoogleOauthToken(final String email) {

        Log.i(TAG, "requestGoogleOauthToken");

        setLoading(true);

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void[] objects) {
                String mGoogleAuthToken = null;
                try {
                    mGoogleAuthToken = GoogleAuthUtil.getToken(LoginActivity.this, email, SCOPE);
                } catch (UserRecoverableAuthException userRecoverableException) {
                    // GooglePlayServices.apk is either old, disabled, or not present
                    // so we need to show the user some UI in the activity to recover.
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
                    onAuthTokenSet(authToken, LoginType.Google);
                }
            }

        }.execute();


    }

    private void onTokenRetrieveError() {
        Toast.makeText(this, "Error Google auth", Toast.LENGTH_SHORT).show();
        setLoading(false);
    }

    protected void onPlusClientSignIn() {
        Log.d(TAG, "onPlusClientSignIn");
        String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
        requestGoogleOauthToken(email);
    }

    protected void onConnectingStatusChange(boolean connecting) {
        Log.d(TAG, "onConnectingStatusChange " + connecting);
        setLoading(connecting);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("mIntentInProgress", mIntentInProgress);
    }

    @Override
    public void onBackEndLogin(LoginResultBean loginResult, LoginType type) {

        /**
         * Maybe there was some data there already due to that the app was being used
         * without signing in
         */
        List<Car> cars = database.retrieveCars(false);
        for (Car car : cars)
            CarsSync.postCar(car, this, CarDatabase.getInstance(this));

        database.saveAndBroadcast(loginResult.cars);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(AccountManager.KEY_ACCOUNT_NAME, loginResult.email);
        resultIntent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
        resultIntent.putExtra(AccountManager.KEY_AUTHTOKEN, loginResult.authToken);
        resultIntent.putExtra(AccountManager.KEY_PASSWORD, loginResult.refreshToken);

        finishLogin(resultIntent, loginResult, type);

        goToMaps();
    }

    private void finishLogin(Intent intent, LoginResultBean loginResult, LoginType type) {

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        String authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        String refreshToken = intent.getStringExtra(AccountManager.KEY_PASSWORD);
        String authTokenType = getString(R.string.account_type);

        // Creating the account on the device and setting the auth token we got
        // (Not setting the auth token will cause another call to the server to authenticate the user)
        Bundle bundle = new Bundle();
        bundle.putString(Authenticator.USER_ID, loginResult.userId);
        bundle.putString(Authenticator.LOGIN_TYPE, type.toString());
        bundle.putLong(Authenticator.LOGIN_DATE, System.currentTimeMillis());

        AuthUtils.setLoginDate(this, System.currentTimeMillis());

        mAccountManager.addAccountExplicitly(account, refreshToken, bundle);
        mAccountManager.setAuthToken(account, authTokenType, authToken);

        // Inform the system that this account supports sync
        ContentResolver.setIsSyncable(account, getString(R.string.content_authority), 1);
        // Inform the system that this account is eligible for auto sync when the network is up
        ContentResolver.setSyncAutomatically(account, getString(R.string.content_authority), true);
        // Recommend a schedule for automatic synchronization. The system may modify this based
        // on other scheduled syncs and network utilization.
//        ContentResolver.addPeriodicSync(
//                account, CarsProvider.CONTENT_AUTHORITY, new Bundle(), CarsProvider.SYNC_FREQUENCY);

        switch (type) {
            case Facebook:
                storeFacebookProfileInformation();
                break;
            case Google:
                storeGoogleProfileInformation();
                break;
        }


        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onLoginError(String authToken) {
        setLoading(false);
        clearGoogleToken(authToken);
        Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show();
    }

    /**
     * Successfully connected
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected");
        mSigningIn = false;
        onConnectingStatusChange(false);
        onPlusClientSignIn();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {

        Log.d(TAG, "onActivityResult " + requestCode + " - " + responseCode);

        // Google
        if (requestCode == OUR_REQUEST_CODE) {
            if (responseCode != RESULT_OK) {
                mSigningIn = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }

            onConnectingStatusChange(responseCode == RESULT_OK);
        }

        // FB
        mFacebookCallbackManager.onActivityResult(requestCode, responseCode, intent);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    /**
     * Connection failed for some reason
     * Try and resolve the result.  Failure here is usually not an indication of a serious error,
     * just that the user's input is needed.
     *
     * @see #onActivityResult(int, int, Intent)
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {

        Log.d(TAG, "Connection failed: " + result);

        if (!result.hasResolution()) {
            GooglePlayServicesUtil
                    .getErrorDialog(result.getErrorCode(), this, 0)
                    .show();
            return;
        }

        if (!mIntentInProgress) {
            // Store the ConnectionResult for later usage
            mConnectionResult = result;

            if (mSigningIn) {
                // The user has already clicked 'sign-in' so we attempt to
                // resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }

    }

    /**
     * Method to resolve any sign in errors
     */
    private void resolveSignInError() {
        if (mConnectionResult != null && mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, OUR_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    /**
     * Fetching user's information name, mLoggedEmail, profile pic
     */
    private void storeGoogleProfileInformation() {
        try {
            String userEmail = Plus.AccountApi.getAccountName(mGoogleApiClient);

            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

            if (currentPerson != null) {

                String personName = currentPerson.getDisplayName();
                String personPhotoUrl = currentPerson.getImage().getUrl();
                String personGooglePlusProfile = currentPerson.getUrl();

                Log.e(TAG, "Name: " + personName + ", plusProfile: "
                        + personGooglePlusProfile + ", email: " + userEmail
                        + ", Image: " + personPhotoUrl);

                // by default the profile url gives 50x50 px image only
                // we can replace the value with whatever dimension we want by
                // replacing sz=X
                personPhotoUrl = personPhotoUrl.substring(0,
                        personPhotoUrl.length() - 2)
                        + PROFILE_PIC_SIZE;

                AuthUtils.setLoggedUserDetails(this, personName, userEmail, personPhotoUrl);


            } else {
                Log.e(TAG, "Person information is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetching user's information name, mLoggedEmail, profile pic
     */
    private void storeFacebookProfileInformation() {
        GraphRequest request = GraphRequest.newMeRequest(
                facebookAccessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {
                        Log.d(TAG, "Facebook result : " + object.toString());
                        try {
                            AuthUtils.setLoggedUserDetails(LoginActivity.this,
                                    object.getString("name"),
                                    object.getString("email"),
                                    String.format("https://graph.facebook.com/%s/picture", object.getString("id")));
                            Intent intent = new Intent(Constants.INTENT_USER_INFO_UPDATE);
                            sendBroadcast(intent);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

}



