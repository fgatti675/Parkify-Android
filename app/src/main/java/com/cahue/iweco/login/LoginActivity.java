package com.cahue.iweco.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.cahue.iweco.Constants;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.cahue.iweco.auth.Authenticator;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Tracking;
import com.cahue.iweco.util.Util;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.myappfree.appvalidator.AppValidator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A login screen that offers login via Google+
 */
public class LoginActivity extends AppCompatActivity implements LoginAsyncTask.LoginListener, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private final static String SCOPE = "oauth2:profile email openid";

    // A magic number we will use to know that our sign-in error resolution activity has completed
    private static final int RC_SIGN_IN = 16846;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    // UI references.
    private View mProgressView;
    private View mButtonsLayout;
    private Button mPlusSignInButton;
    private Button mFacebookLoginButton;

    private GoogleCloudMessaging gcm;
    private CarDatabase database;

    private AccountManager mAccountManager;

    // This is the helper object that connects to Google Play Services.
    private GoogleApiClient mGoogleApiClient;

    private CallbackManager mFacebookCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Tracking.sendView(Tracking.CATEGORY_LOGIN);

        gcm = GoogleCloudMessaging.getInstance(this);

        setContentView(R.layout.activity_login);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

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
            mPlusSignInButton.setVisibility(View.GONE);
            return;
        }

        mFacebookLoginButton = (Button) findViewById(R.id.facebook_login_button);

        // Facebook callback registration
        mFacebookCallbackManager = CallbackManager.Factory.create();

        FacebookSdk.sdkInitialize(this);

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
                AccessToken facebookAccessToken = loginResult.getAccessToken();
                String facebookToken = facebookAccessToken.getToken();
                onAuthTokenSet(facebookToken, LoginType.Facebook);
                storeFacebookProfileInformation(facebookAccessToken);
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
                onLoginSkipped();
            }
        });


        // TODO remove
        AppValidator.isIapToUnlock(this, new AppValidator.OnAppValidatorListener() {
            @Override
            public void validated() {
                sendBroadcast(new Intent(Constants.INTENT_ADS_REMOVED));
                PreferencesUtil.setAdsRemoved(LoginActivity.this, true);
                AppValidator.showDialog(LoginActivity.this, getString(R.string.myAppFree));
            }
        });

    }

    public void onLoginSkipped() {
        database.saveCarAndBroadcast(database.generateOtherCar());
        AuthUtils.setSkippedLogin(LoginActivity.this, true);
        Tracking.sendEvent(Tracking.CATEGORY_LOGIN, Tracking.ACTION_SKIP_LOGIN);
        goToMaps();
    }

    private void goToMaps() {
        startActivity(new Intent(this, MapsActivity.class));
        finish();
    }

    private void googleLogin() {

        setLoading(true);

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    protected void onAuthTokenSet(final String authToken, final LoginType type) {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                try {
                    String gcmDeviceId = GCMUtil.getRegistrationId(LoginActivity.this);

                    if (gcmDeviceId.isEmpty()) {
                        gcmDeviceId = gcm.register(GCMUtil.SENDER_ID);
                        Log.d(TAG, "Device registered, registration ID: " + gcmDeviceId);

                        // Persist the regID - no need to register again.
                        GCMUtil.storeRegistrationId(LoginActivity.this, gcmDeviceId);
                    }

                    return gcmDeviceId;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Signed in successfully, show authenticated UI.
                GoogleSignInAccount acct = result.getSignInAccount();

                requestGoogleOauthToken(acct.getEmail());

                String pictureURL = acct.getPhotoUrl() != null ? acct.getPhotoUrl().toString() : null;
                AuthUtils.setLoggedUserDetails(this, acct.getDisplayName(), acct.getEmail(), pictureURL);

                Log.w(TAG, "Name: " + acct.getDisplayName() + ", email: " + acct.getEmail()
                        + ", Image: " + pictureURL);

            } else {
                // Signed out, show unauthenticated UI.
                setLoading(false);
            }
        }

        // FB
        if (mFacebookCallbackManager != null)
            mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void onGCMRegIdSet(String gcmRegId, String authToken, LoginType type) {
        new LoginAsyncTask(gcmRegId, authToken, this, this, type).execute();
    }

    private void onGCMError(String authToken) {
        setLoading(false);
        mGoogleApiClient.disconnect();
        clearGoogleToken(authToken);
        Util.createUpperToast(this, R.string.gcm_error, Toast.LENGTH_SHORT);
    }

    private void clearGoogleToken(final String authToken) {
        new Thread() {
            @Override
            public void run() {
                try {
                    GoogleAuthUtil.clearToken(LoginActivity.this, authToken);
                } catch (GoogleAuthException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
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
                } catch (IOException | GoogleAuthException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Google oAuth token: " + mGoogleAuthToken);
                return mGoogleAuthToken;
            }

            @Override
            protected void onPostExecute(String authToken) {
                if (authToken == null) {
                    onTokenRetrieveError();
                } else {
                    onAuthTokenSet(authToken, LoginType.Google);
                }
            }

        }.execute();


    }

    private void onTokenRetrieveError() {
        Util.createUpperToast(this, "Error Google auth", Toast.LENGTH_SHORT);
        setLoading(false);
        mGoogleApiClient.disconnect();
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

        database.clearSaveAndBroadcast(loginResult.cars);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(AccountManager.KEY_ACCOUNT_NAME, loginResult.email != null ? loginResult.email : loginResult.userId);
        resultIntent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
        resultIntent.putExtra(AccountManager.KEY_AUTHTOKEN, loginResult.authToken);
        resultIntent.putExtra(AccountManager.KEY_PASSWORD, loginResult.refreshToken);

        finishLogin(resultIntent, loginResult, type);

        Tracking.sendEvent(Tracking.CATEGORY_LOGIN, Tracking.ACTION_DO_LOGIN, type == LoginType.Facebook ? Tracking.LABEL_FACEBOOK_LOGIN : Tracking.LABEL_GOOGLE_LOGIN);

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


        // set default miles use
        PreferencesUtil.setUseMiles(this, Util.isImperialMetricsLocale(this));

        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onLoginError(String authToken, LoginType type) {
        setLoading(false);
        if (type == LoginType.Google) {
            clearGoogleToken(authToken);
        }
        AuthUtils.clearLoggedUserDetails(this);
        Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show();
    }


    /**
     * Fetching user's information name, mLoggedEmail, profile pic
     */
    private void storeFacebookProfileInformation(AccessToken facebookAccessToken) {
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

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
}



