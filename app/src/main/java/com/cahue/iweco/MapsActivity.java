package com.cahue.iweco;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cahue.iweco.activityRecognition.ActivityRecognitionService;
import com.cahue.iweco.activityRecognition.PossibleParkedCarDelegate;
import com.cahue.iweco.activityRecognition.PossibleParkedCarService;
import com.cahue.iweco.auth.Authenticator;
import com.cahue.iweco.cars.CarManagerActivity;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.dialogs.DonateDialog;
import com.cahue.iweco.dialogs.RatingDialog;
import com.cahue.iweco.locationServices.CarMovedService;
import com.cahue.iweco.login.AuthUtils;
import com.cahue.iweco.login.LoginActivity;
import com.cahue.iweco.login.LoginType;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.parkedCar.CarDetailsFragment;
import com.cahue.iweco.parkedCar.ParkedCarService;
import com.cahue.iweco.setCarLocation.LongTapLocationDelegate;
import com.cahue.iweco.spots.ParkingSpotSender;
import com.cahue.iweco.tutorial.TutorialActivity;
import com.cahue.iweco.util.FacebookAppInvitesDialog;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Tracking;
import com.cahue.iweco.util.Util;
import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import com.facebook.share.widget.AppInviteDialog;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bolts.AppLinks;

public class MapsActivity extends AppCompatActivity
        implements
        LocationListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        CarDetailsFragment.OnCarPositionDeletedListener,
        CameraUpdateRequester,
        DelegateManager,
        Navigation,
        OnMapReadyCallback,
        OnCarClickedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DetailsViewManager {

    protected static final String TAG = MapsActivity.class.getSimpleName();

    static final String DETAILS_FRAGMENT_TAG = "DETAILS_FRAGMENT";

    private static final int REQUEST_PERMISSIONS_ACCESS_FINE_LOCATION = 10;

    private static final int REQUEST_CODE_START_TUTORIAL = 2345;

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(3000)         // 3 seconds
            .setFastestInterval(32)    // 32ms = 30fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    /**
     * If we get a new car position while we are using the app, we update the map
     */
    @Nullable
    private final BroadcastReceiver carUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String carId = intent.getExtras().getString(Constants.EXTRA_CAR_ID);
            if (carId != null) {
                Log.i(TAG, "Car update received: " + carId);
                initParkedCarDelegate(carId).update(true);
            }
        }
    };

    @NonNull
    private final Set<AbstractMarkerDelegate> delegates = new HashSet<>();

    /**
     * Set of components that can move the camera in the map
     */
    @NonNull
    private final Set<CameraUpdateRequester> cameraUpdateRequesterList = new HashSet<>();

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    // This is the helper object that connects to Google Play Services.
    private GoogleApiClient mGoogleApiClient;
    private AccountManager mAccountManager;

    private CarDatabase carDatabase;
    private Toolbar mToolbar;
    private FloatingActionButton myLocationButton;

    private CardView cardDetailsContainer;

    private DetailsFragment detailsFragment;

    private boolean detailsDisplayed = false;

    private boolean cameraFollowing;

    /**
     * The user didn't log in, but we still love him
     */
    private boolean mSkippedLogin;

    @Nullable
    private LoginType loginType;

    private CallbackManager mFacebookCallbackManager;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Current logged user
     */
    private Account mAccount;
    private boolean mapInitialised = false;
    private boolean initialCameraSet = false;

    /**
     * Last component to request a camera update
     */
    @Nullable
    private CameraUpdateRequester lastCameraUpdateRequester;

    private MapFragment mapFragment;

    private boolean locationPermissionCurrentlyRequested = false;

    private int navBarHeight = 0;

    public void goToLogin() {
        if (!isFinishing()) {

            if (!mSkippedLogin)
                carDatabase.clearCars();

            AuthUtils.setSkippedLogin(this, false);

            clearAccounts();
            Log.d(TAG, "goToLogin");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void clearAccounts() {
        for (Account account : mAccountManager.getAccountsByType(getString(R.string.account_type)))
            mAccountManager.removeAccount(account, null, null);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mSkippedLogin = AuthUtils.isSkippedLogin(this);

        // TODO: remove
        if (AppturboUnlockTools.isAppturboUnlockable(this)) {
            sendBroadcast(new Intent(Constants.INTENT_ADS_REMOVED));
            PreferencesUtil.setAdsRemoved(MapsActivity.this, true);
        }

        /**
         * Bind service used for donations
         */
        setUpBillingFragment();

        carDatabase = CarDatabase.getInstance(this);

        mAccountManager = AccountManager.get(this);
        final Account[] availableAccounts = mAccountManager.getAccountsByType(getString(R.string.account_type));

        if (availableAccounts.length == 0 && !mSkippedLogin) {
            goToLogin();
            return;
        }
        // There should be just one account
        else if (availableAccounts.length > 1) {
            Log.w(TAG, "Multiple accounts found");
        }

        loginType = null;

        if (availableAccounts.length > 0) {
            mAccount = availableAccounts[0];

            String userId = mAccountManager.getUserData(mAccount, Authenticator.USER_ID);
            Tracking.setTrackerUserId(userId);

            String typeString = mAccountManager.getUserData(mAccount, Authenticator.LOGIN_TYPE);
            if (typeString != null)
                loginType = LoginType.valueOf(typeString);
        }

        // Create a GoogleApiClient instance
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(LocationServices.API)
                .addApiIfAvailable(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this);

        if (!mSkippedLogin && loginType == LoginType.Google) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            builder.addApi(Auth.GOOGLE_SIGN_IN_API, gso);
        }

        mGoogleApiClient = builder.build();

        setContentView(R.layout.activity_main);

        if (BuildConfig.DEBUG) {
            setDebugConfig();
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        ViewCompat.setElevation(mToolbar, getResources().getDimension(R.dimen.elevation));


        /**
         * If translucent bars, apply the proper margins
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Resources resources = getResources();

            RelativeLayout detailsContainer = (RelativeLayout) findViewById(R.id.details_container);
            if (resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                int navBarResId = resources.getIdentifier("navigation_bar_height_landscape", "dimen", "android");
                int navBarLandscapeHeight = navBarResId > 0 ? resources.getDimensionPixelSize(navBarResId) : 0;
                detailsContainer.setPadding(0, 0, navBarLandscapeHeight, 0);
            } else {
                int navBarResId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
                navBarHeight = navBarResId > 0 ? resources.getDimensionPixelSize(navBarResId) : 0;
                detailsContainer.setPadding(0, 0, 0, navBarHeight);
            }

            int statusBarResId = resources.getIdentifier("status_bar_height", "dimen", "android");
            int statusBarHeight = statusBarResId > 0 ? resources.getDimensionPixelSize(statusBarResId) : 0;
            ViewGroup mainContainer = (ViewGroup) findViewById(R.id.main_container);
            mainContainer.setPadding(0, statusBarHeight, 0, 0);
        }

        /**
         * Navigation drawer
         */
        setUpNavigationDrawer();

        myLocationButton = (FloatingActionButton) findViewById(R.id.my_location);
        ViewCompat.setElevation(myLocationButton, getResources().getDimension(R.dimen.elevation));
        myLocationButton.setBackgroundTintList(getResources().getColorStateList(R.color.button_states));
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCameraFollowing(true);
            }
        });

        /**
         * Try to reuse map
         */
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);

        /**
         * There is no saved instance so we create a few things
         */
        if (savedInstanceState == null) {
            // First incarnation of this activity.
            mapFragment.setRetainInstance(true);
        }
        /**
         *
         */
        else {
            initialCameraSet = savedInstanceState.getBoolean("initialCameraSet");
            mapInitialised = savedInstanceState.getBoolean("mapInitialised");
            detailsDisplayed = savedInstanceState.getBoolean("detailsDisplayed");
            cameraFollowing = savedInstanceState.getBoolean("cameraFollowing");
        }

        /**
         * Details
         */
        detailsFragment = (DetailsFragment) getFragmentManager().findFragmentByTag(DETAILS_FRAGMENT_TAG);
        cardDetailsContainer = (CardView) findViewById(R.id.card_details_container);
        cardDetailsContainer.setVisibility(detailsDisplayed ? View.VISIBLE : View.INVISIBLE);


        // Facebook callback registration
        mFacebookCallbackManager = CallbackManager.Factory.create();

        // show help dialog only on first run of the app
        if (!PreferencesUtil.isTutorialShown(this)) {
            goToTutorial();
            return;
        }

        checkLocationPermission();

    }

    @Override
    protected void onStart() {
        super.onStart();

        registerCameraUpdateRequester(this);

        handleIntent(getIntent());

        mGoogleApiClient.connect();

    }

    @Override
    protected void onResume() {

        super.onResume();

        Tracking.sendView(Tracking.CATEGORY_MAP);

        delegates.clear();

        /**
         * Add delegates
         */
        delegates.add(initSpotsDelegate());

        for (ParkingSpot spot : carDatabase.retrievePossibleParkingSpots())
            delegates.add(initPossibleParkedCarDelegate(spot));

        for (String id : carDatabase.getCarIds(true))
            delegates.add(initParkedCarDelegate(id));

        /**
         * Show some dialogs in case the user is bored
         */
        checkRatingDialogShown();
        showFacebookAppInvite();

        /**
         * If BT is not enabled, start activity recognition service (if enabled)
         */
        ActivityRecognitionService.startIfNoBT(this);

        /**
         * Set no cars details if database is empty
         */
        if (carDatabase.isEmptyOfCars()) {
            setNoCars();
        } else {
            if (detailsFragment instanceof NoCarsFragment) hideDetails();
        }

        // when our activity resumes, we want to register for car updates
        registerReceiver(carUpdateReceiver, new IntentFilter(Constants.INTENT_CAR_UPDATED));

        setInitialCamera();

        showOnLongClickToast();

        mapFragment.getMapAsync(this);

        Uri targetUrl = AppLinks.getTargetUrlFromInboundIntent(this, getIntent());
        if (targetUrl != null) {
            Log.i("Activity", "App Link Target URL: " + targetUrl.toString());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(@Nullable Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(Constants.ACTION_POSSIBLE_PARKED_CAR)) {
            initialCameraSet = true;

            ParkingSpot possibleSpot = intent.getParcelableExtra(Constants.EXTRA_SPOT);
            initPossibleParkedCarDelegate(possibleSpot).activate();

            NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);
            mNotifyMgr.cancel(PossibleParkedCarService.NOTIFICATION_ID);
            intent.setAction(null);
        }
    }

    public void checkRatingDialogShown() {
        if (RatingDialog.shouldBeShown(this)) {
            RatingDialog dialog = new RatingDialog();
            dialog.show(getFragmentManager(), "RatingDialog");
        }
    }

    private void setUpBillingFragment() {
        Log.d(TAG, "Creating new BillingFragment");
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        BillingFragment spotsDelegate = BillingFragment.newInstance();
        spotsDelegate.setRetainInstance(true);
        transaction.add(spotsDelegate, BillingFragment.FRAGMENT_TAG);
        transaction.commit();
    }

    private void setUpNavigationDrawer() {

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUserLocation(getUserLocation());
        mNavigationDrawerFragment.setBottomMargin(navBarHeight);

        // Set up the drawer.
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            mNavigationDrawerFragment.setUpDrawer(
                    R.id.navigation_drawer,
                    drawerLayout,
                    mToolbar);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG, "onMapReady");

        mMap = googleMap;

        setUpMapUserLocation();
        setUpMap();
        setMapPadding(0);

        /**
         * Initial zoom
         */
        if (!mapInitialised) {
            CameraUpdate update = PreferencesUtil.getLastCameraPosition(this);
            if (update != null)
                mMap.moveCamera(update);
            mapInitialised = true;
        }

        /**
         * Do everything again
         */
        mMap.clear();

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.setMap(mMap);
        }


        if (detailsDisplayed) showDetails();
        else hideDetails();

    }

    private void checkLocationPermission() {

        if (isLocationPermissionGranted()) return;

        if (!locationPermissionCurrentlyRequested) {
            Log.i(TAG, "Requesting location permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_ACCESS_FINE_LOCATION);
            locationPermissionCurrentlyRequested = true;
        }
    }

    private boolean isLocationPermissionGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setUpMapUserLocation();
                } else {
                    Toast.makeText(this, R.string.permission_error, Toast.LENGTH_SHORT).show();
                }
                locationPermissionCurrentlyRequested = false;
            }
        }
    }

    private void setUpMapUserLocation() {

        if (mMap == null) return;

        // permission not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;

        if (!mGoogleApiClient.isConnected()) return;

        mMap.setMyLocationEnabled(true);

        // Connected to Google Play services!
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                REQUEST,
                this);
    }

    @NonNull
    private SpotsDelegate initSpotsDelegate() {
        SpotsDelegate spotsDelegate = (SpotsDelegate) getFragmentManager().findFragmentByTag(SpotsDelegate.FRAGMENT_TAG);
        if (spotsDelegate == null) {
            Log.d(TAG, "Creating new ParkedCarDelegate");
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            spotsDelegate = SpotsDelegate.newInstance();
            spotsDelegate.setRetainInstance(true);
            transaction.add(spotsDelegate, SpotsDelegate.FRAGMENT_TAG);
            transaction.commit();
        }
        return spotsDelegate;
    }

    /**
     * @param carId
     * @return
     */
    @NonNull
    private ParkedCarDelegate initParkedCarDelegate(String carId) {
        ParkedCarDelegate parkedCarDelegate = (ParkedCarDelegate) getFragmentManager().findFragmentByTag(ParkedCarDelegate.getFragmentTag(carId));
        if (parkedCarDelegate == null) {
            Log.d(TAG, "Creating new ParkedCarDelegate");
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            parkedCarDelegate = ParkedCarDelegate.newInstance(carId);
            parkedCarDelegate.setRetainInstance(true);
            transaction.add(parkedCarDelegate, ParkedCarDelegate.getFragmentTag(carId));
            transaction.commit();
        }
        return parkedCarDelegate;
    }


    /**
     * @return
     */
    @NonNull
    private PossibleParkedCarDelegate initPossibleParkedCarDelegate(@NonNull ParkingSpot spot) {
        PossibleParkedCarDelegate possibleParkedCarDelegate =
                (PossibleParkedCarDelegate) getFragmentManager().findFragmentByTag(PossibleParkedCarDelegate.getFragmentTag(spot));
        if (possibleParkedCarDelegate == null) {
            Log.d(TAG, "Creating new PossibleParkedCarDelegate: " + spot.toString());
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            possibleParkedCarDelegate = PossibleParkedCarDelegate.newInstance(spot);
            possibleParkedCarDelegate.setRetainInstance(true);
            transaction.add(possibleParkedCarDelegate, PossibleParkedCarDelegate.getFragmentTag(spot));
            transaction.commit();
            getFragmentManager().executePendingTransactions();
        }
        return possibleParkedCarDelegate;
    }

    /**
     * @return
     */
    @NonNull
    private LongTapLocationDelegate getLongTapLocationDelegate() {
        LongTapLocationDelegate longTapLocationDelegate = (LongTapLocationDelegate) getFragmentManager().findFragmentByTag(LongTapLocationDelegate.FRAGMENT_TAG);
        if (longTapLocationDelegate == null) {
            Log.d(TAG, "Creating new PossibleParkedCarDelegate: ");
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            longTapLocationDelegate = LongTapLocationDelegate.newInstance();
            longTapLocationDelegate.setRetainInstance(true);
            transaction.add(longTapLocationDelegate, LongTapLocationDelegate.FRAGMENT_TAG);
            transaction.commit();
            getFragmentManager().executePendingTransactions();
        }
        return longTapLocationDelegate;
    }

    private void setMapPadding(int bottomPadding) {
        if (mMap == null) return;
        mMap.setPadding(0, 0, 0, bottomPadding + navBarHeight);
    }

    private void showDetails() {

        cardDetailsContainer.setVisibility(View.VISIBLE);

        cardDetailsContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                // should the location button be animated too
                final boolean moveLocationButton = getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;

                final int height = cardDetailsContainer.getMeasuredHeight();

                setMapPadding(height);
                for (CameraUpdateRequester requester : cameraUpdateRequesterList)
                    requester.onMapResized();

                AnimationSet animationSet = new AnimationSet(true);

                AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
                animationSet.setInterpolator(interpolator);

                int durationMillis = getResources().getInteger(android.R.integer.config_mediumAnimTime);
                animationSet.setDuration(durationMillis);

                TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, detailsDisplayed ? 0 : height, 0);
                translateAnimation.setInterpolator(interpolator);
                translateAnimation.setDuration(durationMillis);

                AlphaAnimation alphaAnimation = new AlphaAnimation(0F, 1F);
                animationSet.addAnimation(alphaAnimation);

                animationSet.addAnimation(translateAnimation);
                animationSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        if (moveLocationButton) {
                            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) myLocationButton.getLayoutParams();
                            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                            myLocationButton.setLayoutParams(params); //causes layout update
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                cardDetailsContainer.startAnimation(animationSet);
                if (moveLocationButton) myLocationButton.startAnimation(translateAnimation);

                detailsDisplayed = true;

                cardDetailsContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });


    }


    @Override
    public void hideDetails() {

        if (!detailsDisplayed) return;

        if (carDatabase.isEmptyOfCars()) {
            setNoCars();
            return;
        }

        // should the location button be animated toov
        final boolean moveLocationButton = getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;

        detailsDisplayed = false;

        setMapPadding(0);

        AnimationSet animationSet = new AnimationSet(true);

        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        animationSet.setInterpolator(interpolator);
        int mediumAnimTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        animationSet.setDuration(mediumAnimTime);

        AlphaAnimation alphaAnimation = new AlphaAnimation(1F, 0F);
        animationSet.addAnimation(alphaAnimation);

        TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0, cardDetailsContainer.getHeight());
        translateAnimation.setDuration(mediumAnimTime);
        translateAnimation.setInterpolator(interpolator);
        animationSet.addAnimation(translateAnimation);

        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setMapPadding(0);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cardDetailsContainer.setVisibility(View.INVISIBLE);
                cardDetailsContainer.removeAllViews();

                if (moveLocationButton) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) myLocationButton.getLayoutParams();
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    myLocationButton.setLayoutParams(params); //causes layout update
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        cardDetailsContainer.startAnimation(animationSet);
        if (moveLocationButton) myLocationButton.startAnimation(translateAnimation);
    }

    private void showOnLongClickToast() {
        if (PreferencesUtil.isLongClickToastShown(this) || carDatabase.isEmptyOfCars())
            return;

        Util.createUpperToast(this, R.string.long_click_instructions, Toast.LENGTH_LONG);

        PreferencesUtil.setLongClickToastShown(this, true);
    }


    @Override
    protected void onStop() {
        unregisterCameraUpdateRequester(this);
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected");
        setUpMapUserLocation();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.w(TAG, "onConnectionFailed");
        goToLogin();
    }

    @Override
    public void onBackPressed() {

        if (mNavigationDrawerFragment.isDrawerOpen()) {
            mNavigationDrawerFragment.closeDrawers();
            return;
        }

        if (detailsDisplayed) {
            for (AbstractMarkerDelegate delegate : delegates) {
                delegate.onDetailsClosed();
                delegate.setCameraFollowing(false);
            }

            if (detailsFragment instanceof NoCarsFragment) {
                super.onBackPressed();
            } else {
                hideDetails();
            }
        } else {
            super.onBackPressed();
        }
    }


    private void setNoCars() {
        setDetailsFragment(NoCarsFragment.newInstance());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_START_TUTORIAL) {
            checkLocationPermission();
        } else if (requestCode == BillingFragment.REQUEST_ON_PURCHASE) {
            BillingFragment billingFragment = (BillingFragment) getFragmentManager().findFragmentByTag(BillingFragment.FRAGMENT_TAG);
            billingFragment.onActivityResult(requestCode, resultCode, data);
        }

        // FB
        if (mFacebookCallbackManager != null)
            mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);

    }

    public CallbackManager getFacebookCallbackManager() {
        return mFacebookCallbackManager;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        saveMapCameraPosition();
    }

    private void saveMapCameraPosition() {
        if (mMap != null) {
            PreferencesUtil.saveCameraPosition(this, mMap.getCameraPosition());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean("initialCameraSet", initialCameraSet);
        savedInstanceState.putBoolean("mapInitialised", mapInitialised);
        savedInstanceState.putBoolean("detailsDisplayed", detailsDisplayed);
        savedInstanceState.putBoolean("cameraFollowing", cameraFollowing);

    }

    /**
     * Checks whether the device currently has a network connection
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Sign out the user (so they can switch to another account).
     */
    public void signOut() {

        // We only want to sign out if we're connected.
        if (mGoogleApiClient.isConnected() && loginType == LoginType.Google) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    Log.d(TAG, "Google Signed out!");
                }
            });
        }

        // Facebook disconnect
        final LoginManager loginManager = LoginManager.getInstance();
        loginManager.logOut();

        Tracking.setTrackerUserId(null);

        PreferencesUtil.clear(this);
        AuthUtils.clearLoggedUserDetails(this);

        Log.v(TAG, "Sign out successful!");

        goToLogin();
    }

    @Override
    public void goToPreferences() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void goToTutorial() {
        startActivityForResult(new Intent(this, TutorialActivity.class), REQUEST_CODE_START_TUTORIAL);
        // animation
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
        PreferencesUtil.setTutorialShown(this, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            if (isLocationPermissionGranted())
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        unregisterReceiver(carUpdateReceiver);

        saveMapCameraPosition();
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(this);
    }


    public void openShareDialog() {
        FacebookAppInvitesDialog.showAppInviteDialog(this);
    }

    public void openDonationDialog() {
        DonateDialog dialog = new DonateDialog();
        dialog.show(getFragmentManager(), "DonateDialog");
    }

    /**
     * Method used to start the pairing activity
     */
    @Override
    public void goToCarManager() {
        startActivity(new Intent(MapsActivity.this, CarManagerActivity.class));
    }

    /**
     * Implementation of {@link LocationListener}.
     */
    @Override
    public void onLocationChanged(Location location) {

        Log.v(TAG, "New location");

        if (mMap == null) return;

        if (cameraFollowing)
            zoomToMyLocation();

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onLocationChanged(location);
        }

        if (detailsFragment != null)
            detailsFragment.setUserLocation(location);

        mNavigationDrawerFragment.setUserLocation(location);

        /**
         * Set initial zoom level
         */
        setInitialCamera();
    }

    private void setInitialCamera() {

        if (initialCameraSet || isFinishing()) return;

        LatLng userPosition = getUserLatLng();
        if (userPosition != null) {

            final List<Car> parkedCars = carDatabase.retrieveParkedCars();

            List<Car> closeCars = new ArrayList<>();
            for (Car car : parkedCars) {
                ParkedCarDelegate parkedCarDelegate = initParkedCarDelegate(car.id);
                parkedCarDelegate.onLocationChanged(getUserLocation());
                if (!parkedCarDelegate.isTooFar()) {
                    closeCars.add(car);
                }
            }

            // One parked car
            if (closeCars.size() == 1) {
                Car car = closeCars.get(0);
                initParkedCarDelegate(car.id).activate();
            }
            // zoom to user otherwise
            else {
                setCameraFollowing(true);
            }

            initialCameraSet = true;
        }

    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        Log.d(TAG, "Long tap event " + latLng.latitude + " " + latLng.longitude);

        Location location = new Location(Util.TAPPED_PROVIDER);
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        location.setAccuracy(10);

        ParkingSpot spot = new ParkingSpot(null, location, null, new Date(), false);

        LongTapLocationDelegate longTapLocationDelegate = getLongTapLocationDelegate();
        longTapLocationDelegate.setMap(mMap);
        longTapLocationDelegate.activate(spot);

    }

    private LatLng getUserLatLng() {
        Location userLastLocation = getUserLocation();
        if (userLastLocation == null) return null;
        return new LatLng(userLastLocation.getLatitude(), userLastLocation.getLongitude());
    }

    private Location getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    /**
     * This is called when the user moves the camera but also when it is triggered programatically.
     *
     * @param cameraPosition
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        if (mMap == null) return;

        if (lastCameraUpdateRequester != this)
            setCameraFollowing(false);

        for (CameraUpdateRequester requester : cameraUpdateRequesterList) {
            if (lastCameraUpdateRequester != requester) {
                requester.setCameraFollowing(false);
                requester.onCameraChange(cameraPosition, lastCameraUpdateRequester);
            }
        }

        if (detailsFragment != null && detailsFragment.isResumed())
            detailsFragment.onCameraUpdate();

        lastCameraUpdateRequester = null;

        Log.v(TAG, "onCameraChange");

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        for (AbstractMarkerDelegate delegate : delegates) {
            if (delegate.onMarkerClick(marker))
                return true;
        }
        return false;
    }

    @Override
    public void onMapClick(LatLng point) {
    }


    @Override
    public DetailsFragment getDetailsFragment() {
        return detailsFragment;
    }

    /**
     * Set the details fragment
     *
     * @param fragment
     */
    @Override
    public void setDetailsFragment(DetailsFragment fragment) {

        if (isFinishing()) return;

        FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();

        if (detailsFragment != null)
            fragTransaction.remove(detailsFragment);

        detailsFragment = fragment;
        detailsFragment.setRetainInstance(true);
        detailsFragment.setUserLocation(getUserLocation());

        fragTransaction.add(cardDetailsContainer.getId(), detailsFragment, DETAILS_FRAGMENT_TAG);
        fragTransaction.commitAllowingStateLoss();

        showDetails();
    }

    @Override
    public void onCarRemoved(String carId) {
        hideDetails();
    }

    @Override
    public void registerDelegate(AbstractMarkerDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void unregisterDelegate(AbstractMarkerDelegate delegate) {
        delegates.remove(delegate);
    }

    @Override
    public void registerCameraUpdateRequester(CameraUpdateRequester cameraUpdateRequester) {
        cameraUpdateRequesterList.add(cameraUpdateRequester);
    }

    @Override
    public void unregisterCameraUpdateRequester(CameraUpdateRequester cameraUpdateRequester) {
        cameraUpdateRequesterList.remove(cameraUpdateRequester);
    }

    @Override
    public void doCameraUpdate(@NonNull CameraUpdate cameraUpdate, final CameraUpdateRequester cameraUpdateRequester) {

        if (mMap == null) return;

        if (!cameraUpdateRequesterList.contains(cameraUpdateRequester))
            throw new IllegalStateException("Register this camera updater before requesting camera updates");

        // tell the rest not to perform more updates
        for (CameraUpdateRequester requester : cameraUpdateRequesterList)
            if (requester != cameraUpdateRequester) requester.setCameraFollowing(false);

        // perform animation
        mMap.animateCamera(cameraUpdate,
                new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        MapsActivity.this.lastCameraUpdateRequester = cameraUpdateRequester;
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    public void setCameraFollowing(boolean cameraFollowing) {
        this.cameraFollowing = cameraFollowing;
        if (cameraFollowing) {
            zoomToMyLocation();
            myLocationButton.setImageResource(R.drawable.crosshairs_gps_accent);
        } else {
            myLocationButton.setImageResource(R.drawable.crosshairs_gps);
        }

    }

    @Override
    public void onMapResized() {
        if (cameraFollowing) zoomToMyLocation();
    }

    private void zoomToMyLocation() {

        Log.d(TAG, "zoomToMyLocation");

        LatLng userPosition = getUserLatLng();
        if (userPosition == null || mMap == null) return;

        float zoom = Math.max(mMap.getCameraPosition().zoom, 15);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .zoom(zoom)
                .target(userPosition)
                .build());

        doCameraUpdate(cameraUpdate, this);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {
    }

    private void showFacebookAppInvite() {

        if (!PreferencesUtil.isFacebookInvitesShown(this)
                && System.currentTimeMillis() - AuthUtils.getLoginDate(this) > 24 * 60 * 60 * 1000
                && AppInviteDialog.canShow()) {
            FacebookAppInvitesDialog dialog = new FacebookAppInvitesDialog();
            dialog.show(getFragmentManager(), "FacebookAppInvitesDialog");
            PreferencesUtil.setFacebookInvitesShown(this, true);
        }
    }

    /**
     * Only for debugging
     */
    private void setDebugConfig() {

        View debugLayout = findViewById(R.id.debug_layout);
        if (debugLayout == null) return;
        debugLayout.setVisibility(View.VISIBLE);


        Button refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initSpotsDelegate().queryCameraView();
                if (mAccount != null)
                    CarsSync.TriggerRefresh(MapsActivity.this, mAccount);
                else
                    Toast.makeText(MapsActivity.this, "Not logged in, so cannot perform refresh", Toast.LENGTH_SHORT).show();
            }
        });

        Button actRecog = (Button) findViewById(R.id.act_recog);
        actRecog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, PossibleParkedCarService.class);
                startService(intent);
            }
        });

        Button carParked = (Button) findViewById(R.id.park);
        carParked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, ParkedCarService.class);
                intent.putExtra(Constants.EXTRA_CAR_ID, carDatabase.retrieveCars(true).iterator().next().id);
                startService(intent);
            }
        });

        Button carMoved = (Button) findViewById(R.id.driveOff);
        carMoved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, CarMovedService.class);
                List<Car> cars = carDatabase.retrieveCars(true);
                if (cars.isEmpty()) return;
                Car car = cars.iterator().next();
                intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
                startService(intent);
            }
        });

        Button approachingCar = (Button) findViewById(R.id.approaching);
        approachingCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Car> cars = carDatabase.retrieveCars(true);
                if (cars.isEmpty()) return;
                Car car = cars.iterator().next();
                ParkingSpotSender.doPostSpotLocation(MapsActivity.this, car.location, true, car);
            }
        });
    }

    @Override
    public void onCarSelected(Car car) {

    }
}
