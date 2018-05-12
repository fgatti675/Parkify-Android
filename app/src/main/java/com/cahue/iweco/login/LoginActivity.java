package com.cahue.iweco.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.cahue.iweco.auth.Authenticator;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.tutorial.TutorialActivity;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Tracking;
import com.cahue.iweco.util.Util;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.drakeet.support.toast.ToastCompat;

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

    private GoogleCloudMessaging gcm;

    private AccountManager mAccountManager;

    // This is the helper object that connects to Google Play Services.
    private GoogleApiClient mGoogleApiClient;

    private CallbackManager mFacebookCallbackManager;

    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private FirebaseAnalytics firebaseAnalytics;

    private List<Car> legacyCars;

    private AccessToken facebookAccessToken;
    private GoogleSignInAccount googleAccessToken;
    private String previousAnonymousUid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        gcm = GoogleCloudMessaging.getInstance(this);
        mAccountManager = AccountManager.get(this);

        setContentView(R.layout.activity_login);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.default_web_client_id))
                .requestIdToken("582791978228-1djp2v9ot1s14c5rmrm70hc19u916g6r.apps.googleusercontent.com")
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mButtonsLayout = findViewById(R.id.buttons);

        // Find the Google+ sign in button.
        Button googleSignIn = findViewById(R.id.plus_sign_in_button);
        mProgressView = findViewById(R.id.login_progress);

        if (checkPlayServices()) {
            // Set a listener to connect the user when the G+ button is clicked.
            googleSignIn.setOnClickListener(view -> googleLogin());
        } else {
            googleSignIn.setVisibility(View.GONE);
            return;
        }

        Button mFacebookLoginButton = findViewById(R.id.facebook_login_button);

        // Facebook callbacks registration
        mFacebookCallbackManager = CallbackManager.Factory.create();

        final LoginManager loginManager = LoginManager.getInstance();
        mFacebookLoginButton.setOnClickListener(v -> {

            // set default miles use
            PreferencesUtil.setUseMiles(this, Util.isImperialMetricsLocale(this));
            loginManager.logInWithReadPermissions(LoginActivity.this, Arrays.asList("public_profile", "user_friends", "email"));
            setLoading(true);
        });

        loginManager.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(@NonNull LoginResult loginResult) {
                AccessToken facebookAccessToken = loginResult.getAccessToken();
                String facebookToken = facebookAccessToken.getToken();
                onAuthTokenSet(facebookToken, LoginType.Facebook);
                Log.d(TAG, facebookToken);
                LoginActivity.this.facebookAccessToken = loginResult.getAccessToken();
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel");
                setLoading(false);
            }

            @Override
            public void onError(@NonNull FacebookException exception) {
                Log.d(TAG, exception.toString());
                setLoading(false);
            }
        });

        Button noSingIn = findViewById(R.id.no_sign_in);
        noSingIn.setOnClickListener(v -> onLoginSkipped());

        findViewById(R.id.terms_of_use).setOnClickListener(v -> {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://glossy-radio.appspot.com/terms_of_use.txt"));
            startActivity(myIntent);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Tracking.sendView(Tracking.CATEGORY_LOGIN);
    }

    public void onLoginSkipped() {

        Tracking.sendEvent(Tracking.CATEGORY_LOGIN, Tracking.ACTION_SKIP_LOGIN);

        firebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent("skip_login", bundle);

        if (firebaseAuth.getCurrentUser() != null) goToNextActivity();

        setLoading(true);

        firebaseAuth.signInAnonymously().addOnCompleteListener(this, task -> {

            if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential:success");
                legacyCars = carDatabase.retrieveCarsDatabase(LoginActivity.this, false);
                migrateToFirestore();
            } else {
                // If sign in fails, display a message to the user.
                Log.w(TAG, "signInWithCredential:failure", task.getException());
                onError();
            }
        });
    }

    public void goToNextActivity() {
        if (isFinishing()) return;
        boolean tutorialShown = PreferencesUtil.isTutorialShown(this);
        Intent intent = new Intent(this, tutorialShown ? MapsActivity.class : TutorialActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void googleLogin() {

        setLoading(true);

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);

        // set default miles use
        PreferencesUtil.setUseMiles(this, Util.isImperialMetricsLocale(this));

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Signed in successfully, show authenticated UI.
                googleAccessToken = result.getSignInAccount();


                requestGoogleOauthToken(googleAccessToken.getEmail());

                String pictureURL = googleAccessToken.getPhotoUrl() != null ? googleAccessToken.getPhotoUrl().toString() : null;
                AuthUtils.setLoggedUserDetails(this, googleAccessToken.getDisplayName(), googleAccessToken.getEmail(), pictureURL);

                Log.w(TAG, "Name: " + googleAccessToken.getDisplayName() + ", email: " + googleAccessToken.getEmail()
                        + ", Image: " + pictureURL);

            } else {
                Log.e(TAG, "google sign in error: " + result.getStatus());
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
        if (!isFinishing())
            Util.showBlueToast(this, R.string.gcm_error, Toast.LENGTH_SHORT);
    }

    @Override
    public void onBackEndLogin(@NonNull LoginResultBean loginResult, @NonNull LoginType type) {

        legacyCars = new ArrayList<>(loginResult.cars);

        Tracking.sendEvent(Tracking.CATEGORY_LOGIN, Tracking.ACTION_DO_LOGIN, type == LoginType.Facebook ? Tracking.LABEL_FACEBOOK_LOGIN : Tracking.LABEL_GOOGLE_LOGIN);

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());
        Bundle fbBundle = new Bundle();
        fbBundle.putString("type", type == LoginType.Facebook ? "facebook" : "google");
        firebaseAnalytics.logEvent("login", fbBundle);


        String accountName = !TextUtils.isEmpty(loginResult.email) ? loginResult.email : loginResult.userId;

        final Account account = new Account(accountName, getString(R.string.account_type));

        String authToken = loginResult.authToken;
        String refreshToken = loginResult.refreshToken;
        String authTokenType = getString(R.string.account_type);

        // Creating the account on the device and setting the auth token we got
        // (Not setting the auth token will cause another call to the server to authenticate the user)
        Bundle bundle = new Bundle();
        bundle.putString(Authenticator.USER_ID, loginResult.userId);
        bundle.putString(Authenticator.LOGIN_TYPE, type.toString());
        bundle.putLong(Authenticator.LOGIN_DATE, System.currentTimeMillis());

        mAccountManager.addAccountExplicitly(account, refreshToken, bundle);
        mAccountManager.setAuthToken(account, authTokenType, authToken);

        if (type == LoginType.Google)
            firebaseAuthWithGoogle(googleAccessToken);
        else
            firebaseAuthWithFacebook(facebookAccessToken);

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

            @Nullable
            @Override
            protected String doInBackground(Void[] objects) {
                String mGoogleAuthToken = null;
                try {
                    mGoogleAuthToken = GoogleAuthUtil.getToken(LoginActivity.this, email, SCOPE);
                } catch (UserRecoverableAuthException userRecoverableException) {
                    // GooglePlayServices.apk is either old, disabled, or not present
                    // so we need to show the user some UI in the activity to recover.
                } catch (@NonNull IOException | GoogleAuthException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Google oAuth token: " + mGoogleAuthToken);
                return mGoogleAuthToken;
            }

            @Override
            protected void onPostExecute(@Nullable String authToken) {
                if (authToken == null) {
                    onTokenRetrieveError();
                } else {
                    onAuthTokenSet(authToken, LoginType.Google);
                }
            }

        }.execute();


    }


    protected void onAuthTokenSet(final String authToken, final LoginType type) {
        new AsyncTask<Void, Void, String>() {

            @Nullable
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
            protected void onPostExecute(@Nullable String regId) {
                if (regId != null) onGCMRegIdSet(regId, authToken, type);
                else onGCMError(authToken);
            }
        }.execute();
    }


    private void onTokenRetrieveError() {
        if (!isFinishing())
            Util.showBlueToast(this, "Error Google auth", Toast.LENGTH_SHORT);
        setLoading(false);
        mGoogleApiClient.disconnect();
    }


    @Override
    public void onLoginError(String authToken, LoginType type) {
        setLoading(false);
        onError();
    }

    private void onError() {
        AuthUtils.clearLoggedUserDetails(this);
        if (!isFinishing())
            ToastCompat.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show();
        setLoading(false);
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseLogin(credential);
    }


    private void firebaseAuthWithFacebook(AccessToken token) {
        Log.d(TAG, "firebaseAuthWithFacebook:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseLogin(credential);
    }

    AuthCredential facebookPendingCredential = null;

    private void firebaseLogin(AuthCredential credential) {

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null)
            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (facebookPendingCredential == null) {
                                saveFirebaseUser(user);
                            } else {
                                user.linkWithCredential(facebookPendingCredential).addOnCompleteListener(task1 -> saveFirebaseUser(user));
                            }
                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                setLoading(false);
                                facebookPendingCredential = credential;
                                if (!isFinishing())
                                    ToastCompat.makeText(this, "Please use Google login instead", Toast.LENGTH_SHORT).show();
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithCredential:failure", task.getException());
                                onError();
                            }
                        }
                    });
        else { // previous anonymous user existed

            previousAnonymousUid = currentUser.getUid();

            currentUser.linkWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (facebookPendingCredential == null) {
                                saveFirebaseUser(user);
                            } else {
                                user.linkWithCredential(facebookPendingCredential).addOnCompleteListener(task1 -> saveFirebaseUser(user));
                            }


                        } else {
                            Log.e(TAG, "signInWithCredential:error " + task.getException());

                            currentUser.delete();
                            firebaseAuth.signOut();
                            firebaseLogin(credential);
                        }
                    });

        }
    }

    private void migratePreviousAnonymousCars(String newUid) {

        FirebaseFirestore.getInstance().collection("cars").whereEqualTo("owner", previousAnonymousUid).get().addOnCompleteListener(t -> {
            QuerySnapshot carsSnapshot = t.getResult();
            Map<String, Object> ownerUpdate = new HashMap<>();
            ownerUpdate.put("owner", newUid);
            for (DocumentSnapshot snapshot : carsSnapshot.getDocuments()) {
                snapshot.getReference().update(ownerUpdate);
            }
        });

    }

    private void saveFirebaseUser(FirebaseUser firebaseUser) {

        Map<String, Object> user = new HashMap<>();
        user.put("email", firebaseUser.getEmail());
        user.put("name", firebaseUser.getDisplayName());
        if (firebaseUser.getPhotoUrl() != null)
            user.put("photo_url", firebaseUser.getPhotoUrl().toString());

        db.collection("users")
                .document(firebaseUser.getUid())
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User saved");
                    migrateToFirestore();

                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    onError();
                });

        migratePreviousAnonymousCars(firebaseUser.getUid());
    }

    CarDatabase carDatabase = CarDatabase.getInstance();

    public void migrateToFirestore() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        if (legacyCars == null || legacyCars.isEmpty()) {
            goToNextActivity();
            return;
        }

        firestore.collection("cars").whereEqualTo("owner", FirebaseAuth.getInstance().getCurrentUser().getUid()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();

                    Map<String, String> legacyIdsMap = new HashMap<>();
                    for (DocumentSnapshot documentSnapshot : documents) {
                        legacyIdsMap.put((String) documentSnapshot.get("legacy_id"), documentSnapshot.getId());
                    }

                    for (Car car : legacyCars) {
                        if (legacyIdsMap.containsKey(car.legacy_id)) {
                            car.id = legacyIdsMap.get(car.legacy_id);
                            carDatabase.updateCar(car, new CarDatabase.CarUpdateListener() {
                                @Override
                                public void onCarUpdated(Car car) {
                                    goToNextActivity();
                                }

                                @Override
                                public void onCarUpdateError() {
                                    onError();
                                }
                            });
                        } else {
                            carDatabase.createCar(car, new CarDatabase.CarUpdateListener() {
                                @Override
                                public void onCarUpdated(Car car) {
                                    onMigratedCarCreated(car);
                                }

                                @Override
                                public void onCarUpdateError() {
                                    onError();
                                }
                            });
                        }
                    }


                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore migration error: " + e);
                    goToNextActivity();
                });
    }

    private void onMigratedCarCreated(Car car) {
        carDatabase.updateCarLocation(car.id, car.location, car.address, car.time, null, new CarDatabase.CarUpdateListener() {
            @Override
            public void onCarUpdated(Car car) {
                goToNextActivity();
            }

            @Override
            public void onCarUpdateError() {

            }
        });
    }
}



