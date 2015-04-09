package com.cahue.iweco;

import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ImageView;

import com.cahue.iweco.login.AuthUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.plus.Plus;

import java.io.InputStream;


/**
 * A base class to wrap communication with the Google Play Services PlusClient.
 */
public abstract class BaseActivity
        extends ActionBarActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = BaseActivity.class.getSimpleName();


    // A magic number we will use to know that our sign-in error resolution activity has completed
    private final static int OUR_REQUEST_CODE = 49404;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * The user didn't log in, but we still love him
     */
    private boolean mSkippedLogin;

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

    /**
     * Called when the PlusClient is successfully connected.
     */
    protected abstract void onPlusClientSignIn();

    /**
     * Called when the PlusClient is disconnected.
     */
    protected abstract void onPlusClientSignOut();

    /**
     * Called when there is a change in connection state.  If you have "Sign in"/ "Connect",
     * "Sign out"/ "Disconnect", or "Revoke access" buttons, this lets you know when their states
     * need to be updated.
     */
    protected abstract void onConnectingStatusChange(boolean connecting);

    /**
     * Called when signing in is required
     */
    protected abstract void onSignInRequired();

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mSkippedLogin = AuthUtils.isSkippedLogin(this);

        if (checkPlayServices()) {
            if (savedInstanceState != null) {
                mIntentInProgress = savedInstanceState.getBoolean("mIntentInProgress");
            }
            Log.d(TAG, "mGoogleApiClient initialized");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean("mIntentInProgress", mIntentInProgress);

    }

    private void setUpGoogleApiClientIfNeeded() {

        if (mGoogleApiClient == null) {

            // Initialize the PlusClient connection.
            // Scopes indicate the information about the user your application will be able to access.
            GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this);

            if (!mSkippedLogin)
                builder.addApi(Plus.API)
                        .addScope(Plus.SCOPE_PLUS_LOGIN)
                        .addScope(new Scope("https://www.googleapis.com/auth/userinfo.email"));

            mGoogleApiClient = builder.build();

        }
    }

    /**
     * Try to sign in the user.
     */
    public void signIn() {

        if (!mGoogleApiClient.isConnecting()) {
            // Show the dialog as we are now signing in.
            mSigningIn = true;
            resolveSignInError();
            onConnectingStatusChange(true);
        }

    }

    protected void connect() {
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
        mGoogleApiClient.connect();
        onConnectingStatusChange(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setUpGoogleApiClientIfNeeded();
        if (autoConnect()) {
            Log.d(TAG, "mGoogleApiClient connecting");
            mGoogleApiClient.connect();
        }
    }

    /**
     * Should google play services connect automatically on activity start
     * @return
     */
    protected abstract boolean autoConnect();


    /**
     * Successfully connected
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected");
        mSigningIn = false;
        onPlusClientSignIn();
        onConnectingStatusChange(false);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Sign out the user (so they can switch to another account).
     */
    public void signOut() {

        // We only want to sign out if we're connected.
        if (mGoogleApiClient.isConnected()) {

            // Clear the default account in order to allow the user to potentially choose a
            // different account from the account chooser.
            if (!mSkippedLogin)
                Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);

            // Disconnect from Google Play Services, then reconnect in order to restart the
            // process from scratch.
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();

            Log.v(TAG, "Sign out successful!");
        }

        onConnectingStatusChange(false);
        onPlusClientSignOut();
    }


    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {

        if (requestCode == OUR_REQUEST_CODE) {
            if (responseCode != RESULT_OK) {
                mSigningIn = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
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

        if (!result.hasResolution()) {
            GooglePlayServicesUtil
                    .getErrorDialog(result.getErrorCode(), this, 0)
                    .show();
            return;
        }

        if (result.getErrorCode() == ConnectionResult.SIGN_IN_REQUIRED) {
            onSignInRequired();
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

        onConnectingStatusChange(false);
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

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }


    /**
     * Background Async task to load user profile picture from url
     */
    private class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public LoadProfileImage(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    public boolean isSkippedLogin() {
        return mSkippedLogin;
    }

}
