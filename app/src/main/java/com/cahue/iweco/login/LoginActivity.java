package com.cahue.iweco.login;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
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
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import me.drakeet.support.toast.ToastCompat;

/**
 * A login screen that offers login via Google+
 */
public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

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
                Log.d(TAG, facebookToken);
                firebaseAuthWithFacebook(loginResult.getAccessToken());
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
                goToNextActivity();
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
//        tutorialShown = false;
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
                GoogleSignInAccount googleAccessToken = result.getSignInAccount();

                firebaseAuthWithGoogle(googleAccessToken);

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
                            goToNextActivity();
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
                            goToNextActivity();

                        } else {
                            Log.e(TAG, "signInWithCredential:error " + task.getException());

                            currentUser.delete();
                            firebaseAuth.signOut();
                            firebaseLogin(credential);
                        }
                    });

        }
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

                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    onError();
                });

        migratePreviousAnonymousCars(firebaseUser.getUid());
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


}



