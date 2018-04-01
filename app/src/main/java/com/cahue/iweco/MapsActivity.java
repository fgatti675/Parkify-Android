package com.cahue.iweco;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.cahue.iweco.activityrecognition.ActivityRecognitionService;
import com.cahue.iweco.activityrecognition.PossibleParkedCarDelegate;
import com.cahue.iweco.auth.Authenticator;
import com.cahue.iweco.cars.CarManagerActivity;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.dialogs.DonateDialog;
import com.cahue.iweco.dialogs.RatingDialog;
import com.cahue.iweco.locationservices.CarMovedReceiver;
import com.cahue.iweco.locationservices.LocationUpdatesHelper;
import com.cahue.iweco.locationservices.ParkedCarReceiver;
import com.cahue.iweco.locationservices.PossibleParkedCarReceiver;
import com.cahue.iweco.login.AuthUtils;
import com.cahue.iweco.login.LoginActivity;
import com.cahue.iweco.login.LoginType;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.model.PossibleSpot;
import com.cahue.iweco.parkedcar.CarDetailsFragment;
import com.cahue.iweco.places.PlacesDelegate;
import com.cahue.iweco.setcarlocation.LongTapLocationDelegate;
import com.cahue.iweco.spots.ParkingSpotSender;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Tracking;
import com.cahue.iweco.util.Util;
import com.facebook.CallbackManager;
import com.facebook.ads.Ad;
import com.facebook.ads.AdChoicesView;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.NativeAd;
import com.facebook.login.LoginManager;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bolts.AppLinks;

import static android.content.Intent.ACTION_VIEW;
import static com.cahue.iweco.util.NotificationChannelsUtils.DEBUG_CHANNEL_ID;

public class MapsActivity extends AppCompatActivity
        implements
        LocationListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        CarDetailsFragment.OnCarPositionDeletedListener,
        CameraUpdateRequester,
        DelegateManager,
        Navigation,
        OnMapReadyCallback,
        OnCarClickedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DetailsViewManager,
        BillingFragment.OnBillingReadyListener,
        AdListener {

    protected static final String TAG = MapsActivity.class.getSimpleName();

    static final String DETAILS_FRAGMENT_TAG = "DETAILS_FRAGMENT";

    private static final int REQUEST_PERMISSIONS_ACCESS_FINE_LOCATION = 10;

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

    private MapFragment mapFragment;

    /**
     * This is the helper object that connects to Google Play Services.
     */
    private GoogleApiClient mGoogleApiClient;
    private AccountManager mAccountManager;

    private DetailsFragment detailsFragment;

    private CarDatabase carDatabase;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private View drawerToggle;
    private FloatingActionButton myLocationButton;

    private Button noCarsButton;

    private CardView cardDetailsContainer;

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

    private boolean locationPermissionCurrentlyRequested = false;

    private int navBarHeight = 0;
    private int statusBarHeight = 0;
    private RelativeLayout detailsContainer;
    private DrawerLayout drawerLayout;

    private ViewGroup nativeAdContainer;

    @Nullable
    private BroadcastReceiver newPurchaseReceiver;

    private AdChoicesView adChoicesView;
    private NativeAd facebookNativeAd;

    private RelativeLayout mainContainer;

    private FrameLayout nativeExpressAbMobContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        carDatabase = CarDatabase.getInstance();

        mSkippedLogin = AuthUtils.isSkippedLogin(this);

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

        /**
         * Bind service used for donations
         */
        initBillingFragment();

        loginType = null;

        if (availableAccounts.length > 0) {
            mAccount = availableAccounts[0];

            String userId = mAccountManager.getUserData(mAccount, Authenticator.USER_ID);
            Tracking.setTrackerUserId(userId);

            String typeString = mAccountManager.getUserData(mAccount, Authenticator.LOGIN_TYPE);
            if (typeString != null)
                loginType = LoginType.valueOf(typeString);
        }

        /**
         * Create a GoogleApiClient instance
         */
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

        drawerToggle = findViewById(R.id.navigation_drawer_toggle);
        if (drawerToggle != null)
            drawerToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.openDrawer(GravityCompat.START);
                    Tracking.sendEvent(Tracking.CATEGORY_MAP, Tracking.ACTION_NAVIGATION_TOGGLE);
                }
            });

        mainContainer = findViewById(R.id.main_container);
        detailsContainer = findViewById(R.id.details_container);

        /**
         * If translucent bars, apply the proper margins
         */
        Resources resources = getResources();

        int statusBarResId = resources.getIdentifier("status_bar_height", "dimen", "android");
        statusBarHeight = statusBarResId > 0 ? resources.getDimensionPixelSize(statusBarResId) : 0;

        if (resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int navBarResId = resources.getIdentifier("navigation_bar_height_landscape", "dimen", "android");
            int navBarLandscapeHeight = navBarResId > 0 ? resources.getDimensionPixelSize(navBarResId) : 0;
            mainContainer.setPadding(0, statusBarHeight, navBarLandscapeHeight, 0);
        } else {
            int navBarResId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            navBarHeight = navBarResId > 0 ? resources.getDimensionPixelSize(navBarResId) : 0;
            mainContainer.setPadding(0, statusBarHeight, 0, navBarHeight);
        }

        /**
         * Navigation drawer
         */
        setUpNavigationDrawer();

        myLocationButton = findViewById(R.id.my_location);
        myLocationButton.setBackgroundTintList(getResources().getColorStateList(R.color.button_states));
        myLocationButton.setOnClickListener(view -> setCameraFollowing(true));

        if (BuildConfig.DEBUG) {
            myLocationButton.setOnLongClickListener(v -> {
                setDebugConfig();
                return false;
            });
        }

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
        cardDetailsContainer = findViewById(R.id.card_details_container);
        cardDetailsContainer.setVisibility(detailsDisplayed ? View.VISIBLE : View.INVISIBLE);

        noCarsButton = findViewById(R.id.no_cars_button);
        noCarsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToCarManager();
            }
        });

        checkLocationPermission();


        nativeExpressAbMobContainer = findViewById(R.id.ad_mob_container);
        nativeAdContainer = findViewById(R.id.navite_ad_container);
        nativeExpressAbMobContainer.setVisibility(View.GONE);
        nativeAdContainer.setVisibility(View.GONE);

        /*
         * Start activity recognition service (if enabled)
         */
        ActivityRecognitionService.startCheckingActivityRecognition(this);

        if (BuildConfig.DEBUG) {
            long[] pattern = {0, 110, 1000};
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder mBuilder =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, DEBUG_CHANNEL_ID) : new Notification.Builder(this))
                            .setVibrate(pattern)
                            .setSmallIcon(R.drawable.crosshairs_gps)
                            .setContentTitle("DEBUG " )
                            .setContentText("test");

            int id = (int) (Math.random() * 10000);
            mNotifyMgr.notify("" + id, id, mBuilder.build());
        }

    }

    public void goToLogin() {
        if (!isFinishing()) {

            if (!mSkippedLogin)
                carDatabase.clearCars(this);

            AuthUtils.setSkippedLogin(this, false);

            clearAccounts();
            Log.d(TAG, "goToLogin");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    private void clearAccounts() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
        for (Account account : mAccountManager.getAccountsByType(getString(R.string.account_type)))
            mAccountManager.removeAccount(account, null, null);
//        }
    }


    @Override
    public void onBillingReady(BillingFragment billingFragment) {
        if (PreferencesUtil.isAdsRemoved(this)) return;

        if (carDatabase.isEmptyOfCars(this)) return;

        checkForPurchases(billingFragment);
    }

    private void checkForPurchases(final BillingFragment billingFragment) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                /**
                 * Check if the user has purchases. If there is an error we don't display just in case
                 */
                boolean displayAd = false;
                Bundle ownedItems = billingFragment.getPurchases();
                int response = ownedItems.getInt("RESPONSE_CODE");
                if (response == 0) {
                    ArrayList<?> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                    Log.d(TAG, "Purchased items: " + purchaseDataList.toString());
                    displayAd = purchaseDataList.isEmpty();
                }
                PreferencesUtil.setPurchasesCheked(MapsActivity.this, true);
                return displayAd;
            }

            @Override
            protected void onPostExecute(Boolean displayAd) {
                Log.d(TAG, "Display ads returned " + displayAd);

                if (displayAd) {

                    newPurchaseReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            nativeAdContainer.setVisibility(View.GONE);
                            nativeExpressAbMobContainer.setVisibility(View.GONE);
                            facebookNativeAd.destroy();
                        }
                    };
                    LocalBroadcastManager.getInstance(MapsActivity.this).registerReceiver(newPurchaseReceiver, new IntentFilter(Constants.INTENT_ADS_REMOVED));

                    setUpFacebookAd();
//                    setUpAdMobAdNativeExpress();

                } else {
                    PreferencesUtil.setAdsRemoved(MapsActivity.this, true);
                }

            }
        }.execute();
    }

    private void setUpFacebookAd() {

        Log.d(TAG, "setUpFacebookAd");
        facebookNativeAd = new NativeAd(MapsActivity.this, getString(R.string.facebook_maps_placement_id));
        facebookNativeAd.setAdListener(MapsActivity.this);
        facebookNativeAd.loadAd();
    }


    /**
     * On FB ad loaded
     *
     * @param ad
     */
    @Override
    public void onAdLoaded(Ad ad) {

        Log.d(TAG, "onAdLoaded: ");

        // Downloading and setting the ad icon.
        final NativeAd.Image adIcon = facebookNativeAd.getAdIcon();

        // Create native UI using the ad metadata.
        final ImageView nativeAdIcon = nativeAdContainer.findViewById(R.id.native_ad_icon);
        final TextView nativeAdTitle = nativeAdContainer.findViewById(R.id.native_ad_title);
        final TextView nativeAdBody = nativeAdContainer.findViewById(R.id.native_ad_body);
        nativeAdBody.setSelected(true);
        final Button nativeAdCallToAction = nativeAdContainer.findViewById(R.id.native_ad_call_to_action);
        final ViewGroup adChoicesWrap = nativeAdContainer.findViewById(R.id.ad_choices_wrap);

        RequestQueue requestQueue = ParkifyApp.getParkifyApp().getRequestQueue();
        if (adIcon != null) {
            ImageRequest adPicture = new ImageRequest(adIcon.getUrl(), new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap response) {
                    nativeAdIcon.setImageBitmap(response);
                    bindAdView(nativeAdCallToAction, nativeAdTitle, nativeAdBody, adChoicesWrap);

                }
            }, 0, 0, ImageView.ScaleType.CENTER, Bitmap.Config.RGB_565, null);

            Cache.Entry entry = new Cache.Entry();
            entry.ttl = 24 * 60 * 60 * 1000;
            adPicture.setCacheEntry(entry);
            requestQueue.add(adPicture);
        } else {
            Log.w(TAG, "adview without adIcon");
            bindAdView(nativeAdCallToAction, nativeAdTitle, nativeAdBody, adChoicesWrap);
        }

    }

    /**
     * On FB ad error
     *
     * @param ad
     */
    @Override
    public void onError(Ad ad, AdError adError) {
        Log.d(TAG, "onAdError: ");
        setUpAdMobAdNativeExpress();
    }


    private void setUpAdMobAdNativeExpress() {
        nativeAdContainer.setVisibility(View.GONE);
        nativeExpressAbMobContainer.setVisibility(View.VISIBLE);

        nativeExpressAbMobContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Display display = getWindowManager().getDefaultDisplay();
                DisplayMetrics outMetrics = new DisplayMetrics();
                display.getMetrics(outMetrics);

                nativeExpressAbMobContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                NativeExpressAdView nativeExpressAdView = new NativeExpressAdView(MapsActivity.this);
                int width = (int) (nativeExpressAbMobContainer.getWidth() / outMetrics.density);
                nativeExpressAdView.setAdSize(new AdSize(width, 80));
                nativeExpressAdView.setAdUnitId("ca-app-pub-7749631063131885/9014982450");
                nativeExpressAdView.setAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                    }

                    @Override
                    public void onAdFailedToLoad(int i) {
                        super.onAdFailedToLoad(i);
                    }

                    @Override
                    public void onAdLeftApplication() {
                        super.onAdLeftApplication();
                    }

                    @Override
                    public void onAdOpened() {
                        super.onAdOpened();
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                    }
                });

                AdRequest request = new AdRequest.Builder()
                        .addTestDevice("978CFEF27A71410CEFEED093CC2599DA")
                        .setLocation(getUserLocation()).build();
                nativeExpressAdView.loadAd(request);

                nativeExpressAbMobContainer.removeAllViews();
                nativeExpressAbMobContainer.addView(nativeExpressAdView);
            }
        });

    }

    private void setUpAdMobAdNative() {
        // Create native UI using the ad metadata.
        final ImageView nativeAdIcon = nativeAdContainer.findViewById(R.id.native_ad_icon);
        final TextView nativeAdTitle = nativeAdContainer.findViewById(R.id.native_ad_title);
        final TextView nativeAdBody = nativeAdContainer.findViewById(R.id.native_ad_body);
        nativeAdBody.setSelected(true);
        final Button nativeAdCallToAction = nativeAdContainer.findViewById(R.id.native_ad_call_to_action);
        final ViewGroup adChoicesWrap = nativeAdContainer.findViewById(R.id.ad_choices_wrap);
        nativeAdContainer.setVisibility(View.VISIBLE);

        AdLoader adLoader = new AdLoader.Builder(MapsActivity.this, "ca-app-pub-3940256099942544/3986624511")
                .forAppInstallAd(new NativeAppInstallAd.OnAppInstallAdLoadedListener() {
                    @Override
                    public void onAppInstallAdLoaded(NativeAppInstallAd appInstallAd) {
                        nativeAdIcon.setImageDrawable(appInstallAd.getIcon().getDrawable());
                        nativeAdTitle.setText(appInstallAd.getHeadline());
                        nativeAdBody.setText(appInstallAd.getBody());
                        nativeAdCallToAction.setText(appInstallAd.getCallToAction());
                        nativeAdBody.setVisibility(nativeAdTitle.getLineCount() == 1 ? View.VISIBLE : View.GONE);
                    }
                })
                .forContentAd(new NativeContentAd.OnContentAdLoadedListener() {
                    @Override
                    public void onContentAdLoaded(NativeContentAd contentAd) {
                        nativeAdIcon.setImageDrawable(contentAd.getLogo().getDrawable());
                        nativeAdTitle.setText(contentAd.getHeadline());
                        nativeAdBody.setText(contentAd.getBody());
                        nativeAdCallToAction.setText(contentAd.getCallToAction());
                        nativeAdBody.setVisibility(nativeAdTitle.getLineCount() == 1 ? View.VISIBLE : View.GONE);
                    }
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // Handle the failure by logging, altering the UI, etc.
                        Log.d(TAG, "onAdFailedToLoad: " + errorCode);
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build())
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public void bindAdView(Button nativeAdCallToAction, final TextView nativeAdTitle, final TextView nativeAdBody, ViewGroup adChoicesWrap) {
        nativeAdContainer.setVisibility(View.VISIBLE);

        facebookNativeAd.unregisterView();

        // Setting the Text.
        nativeAdCallToAction.setText(facebookNativeAd.getAdCallToAction());
        nativeAdTitle.setText(facebookNativeAd.getAdTitle());
        nativeAdBody.setText(facebookNativeAd.getAdBody());

        nativeAdTitle.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                nativeAdTitle.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                nativeAdBody.setVisibility(nativeAdTitle.getLineCount() == 1 ? View.VISIBLE : View.GONE);
            }
        });

        // Add adChoices icon
        if (adChoicesView == null) {
            adChoicesView = new AdChoicesView(MapsActivity.this, facebookNativeAd, true);
            adChoicesView.setGravity(Gravity.TOP | Gravity.END);
            adChoicesWrap.addView(adChoicesView);
        }

        View adContainer = nativeAdContainer.findViewById(R.id.navite_ad_container);
        facebookNativeAd.registerViewForInteraction(adContainer);

        setMapPadding();
    }

    @Override
    public void onAdClicked(Ad ad) {
        Log.d(TAG, "onAdClicked: ");
        Tracking.sendEvent(Tracking.CATEGORY_ADVERTISING, Tracking.ACTION_AD_CLICKED, "Facebook");
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        Log.d(TAG, "onLoggingImpression: ");
    }


    @Override
    protected void onStart() {
        super.onStart();

        long initTime = System.currentTimeMillis();

        registerCameraUpdateRequester(this);

        mGoogleApiClient.connect();

        Log.d("App speed", "On start init time : " + (System.currentTimeMillis() - initTime));

    }

    @Override
    protected void onResume() {

        super.onResume();

        long initTime = System.currentTimeMillis();

        Tracking.sendView(Tracking.CATEGORY_MAP);

        delegates.clear();

        /*
         * Add delegates
         */
        delegates.add(initSpotsDelegate());

        delegates.add(initPlacesDelegate());

        for (PossibleSpot spot : carDatabase.retrievePossibleParkingSpots(this))
            delegates.add(initPossibleParkedCarDelegate(spot));

        for (String id : carDatabase.getCarIds(this, true))
            delegates.add(initParkedCarDelegate(id));

        /*
         * Show some dialogs in case the user is bored
         */
        checkRatingDialogShown();

        // when our activity resumes, we want to register for car updates
        LocalBroadcastManager.getInstance(this).registerReceiver(carUpdateReceiver, new IntentFilter(Constants.INTENT_CAR_UPDATED));

        setInitialCamera();

        showOnLongClickToast();

        noCarsButton.setVisibility(carDatabase.isEmptyOfCars(this) ? View.VISIBLE : View.GONE);

        mapFragment.getMapAsync(this);

        Uri targetUrl = AppLinks.getTargetUrlFromInboundIntent(this, getIntent());
        if (targetUrl != null) {
            Log.i("Activity", "App Link Target URL: " + targetUrl.toString());
        }

        Log.d("App speed", "On resume init time : " + (System.currentTimeMillis() - initTime));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent: ");
        handleIntent(intent);
    }

    private void handleIntent(@Nullable Intent intent) {
        if (intent != null && intent.getAction() != null) {
            initialCameraSet = true;
            if (intent.getAction().equals(Constants.ACTION_POSSIBLE_PARKED_CAR)) {

                PossibleSpot possibleSpot = intent.getParcelableExtra(Constants.EXTRA_SPOT);
                initPossibleParkedCarDelegate(possibleSpot).activate();

                NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);
                mNotifyMgr.cancel(PossibleParkedCarReceiver.NOTIFICATION_ID);
                intent.setAction(null);
            } else if (intent.getAction().equals(ACTION_VIEW)) {

                String carId = intent.getStringExtra(Constants.EXTRA_CAR_ID);
                ParkedCarDelegate parkedCarDelegate = initParkedCarDelegate(carId);
                parkedCarDelegate.activate();

                NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);
                mNotifyMgr.cancel(carId, ParkedCarReceiver.NOTIFICATION_ID);
                intent.setAction(null);
            }
        }
    }

    public void checkRatingDialogShown() {
        if (RatingDialog.shouldBeShown(this)) {
            RatingDialog dialog = new RatingDialog();
            dialog.show(getFragmentManager(), "RatingDialog");
        }
    }

    private void setUpNavigationDrawer() {

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUserLocation(getUserLocation());
        mNavigationDrawerFragment.setTopMargin(statusBarHeight);
        mNavigationDrawerFragment.setBottomMargin(navBarHeight);

        // Set up the drawer.
        drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            mNavigationDrawerFragment.setUpDrawer(
                    R.id.navigation_drawer,
                    drawerLayout);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        long initTime = System.currentTimeMillis();

        Log.d(TAG, "onMapReady");

        mMap = googleMap;

        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));

        setUpMapUserLocation();
        setUpMap();
        setMapPadding();

        /*
         * Initial zoom
         */
        if (!mapInitialised) {
            CameraUpdate update = PreferencesUtil.getLastCameraPosition(this);
            if (update != null)
                mMap.moveCamera(update);
            mapInitialised = true;
        }

        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.setMap(mMap);
        }

        detailsContainer.setVisibility(View.VISIBLE);
        if (drawerLayout != null)
            drawerToggle.setVisibility(View.VISIBLE);

        if (detailsDisplayed) showDetails();
        else hideDetails();

        Log.d("App speed", "On map ready init time : " + (System.currentTimeMillis() - initTime));

        handleIntent(getIntent());

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


    private BillingFragment initBillingFragment() {
        BillingFragment billingFragment = (BillingFragment) getFragmentManager().findFragmentByTag(BillingFragment.FRAGMENT_TAG);
        if (billingFragment == null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            billingFragment = BillingFragment.newInstance();
            billingFragment.setRetainInstance(true);
            transaction.add(billingFragment, BillingFragment.FRAGMENT_TAG);
            transaction.commit();
        }
        return billingFragment;
    }


    @NonNull
    private PlacesDelegate initPlacesDelegate() {
        PlacesDelegate placesDelegate = (PlacesDelegate) getFragmentManager().findFragmentByTag(PlacesDelegate.FRAGMENT_TAG);
        if (placesDelegate == null) {
            Log.d(TAG, "Creating new ParkedCarDelegate");
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            placesDelegate = PlacesDelegate.newInstance();
            placesDelegate.setRetainInstance(true);
            transaction.add(placesDelegate, PlacesDelegate.FRAGMENT_TAG);
            transaction.commit();
        }
        return placesDelegate;
    }

    @NonNull
    private SpotsDelegate initSpotsDelegate() {
        SpotsDelegate spotsDelegate = (SpotsDelegate) getFragmentManager().findFragmentByTag(SpotsDelegate.FRAGMENT_TAG);
        if (spotsDelegate == null) {
            Log.d(TAG, "Creating new SpotsDelegate");
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
            getFragmentManager().executePendingTransactions();
        }
        return parkedCarDelegate;
    }


    /**
     * @return
     */
    @NonNull
    private PossibleParkedCarDelegate initPossibleParkedCarDelegate(@NonNull PossibleSpot spot) {
        PossibleParkedCarDelegate possibleParkedCarDelegate = getPossibleParkingDelegate(spot);
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

    private PossibleParkedCarDelegate getPossibleParkingDelegate(@NonNull PossibleSpot spot) {
        return (PossibleParkedCarDelegate) getFragmentManager().findFragmentByTag(PossibleParkedCarDelegate.getFragmentTag(spot));
    }

    /**
     * @return
     */
    @NonNull
    private LongTapLocationDelegate initLongTapLocationDelegate() {
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

    private void setMapPadding() {
        if (mMap == null) return;
        mainContainer.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int adHeight = nativeAdContainer == null ? 0 : nativeAdContainer.getMeasuredHeight();
            mMap.setPadding(0, statusBarHeight + adHeight, 0, (detailsDisplayed ? cardDetailsContainer.getMeasuredHeight() : 0) + navBarHeight);
        });
    }

    private void showDetails() {

        cardDetailsContainer.setVisibility(View.VISIBLE);

        cardDetailsContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                // should the location button be animated too
                final boolean moveLocationButton = getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;

                final int height = cardDetailsContainer.getMeasuredHeight();

                setMapPadding();
                for (CameraUpdateRequester requester : cameraUpdateRequesterList)
                    requester.onMapResized();

                AnimationSet animationSet = new AnimationSet(true);

                DecelerateInterpolator interpolator = new DecelerateInterpolator();
                animationSet.setInterpolator(interpolator);

                int durationMillis = getResources().getInteger(android.R.integer.config_mediumAnimTime);
                animationSet.setDuration(durationMillis);

                TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, detailsDisplayed ? 0 : height, 0);
                translateAnimation.setInterpolator(interpolator);
                translateAnimation.setDuration(durationMillis);

                if (!detailsDisplayed) {
                    AlphaAnimation alphaAnimation = new AlphaAnimation(0F, 1F);
                    animationSet.addAnimation(alphaAnimation);
                }

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

        // should the location button be animated too
        final boolean moveLocationButton = getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;

        detailsDisplayed = false;

        AnimationSet animationSet = new AnimationSet(true);

        AccelerateInterpolator interpolator = new AccelerateInterpolator();
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
                setMapPadding();
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
        if (PreferencesUtil.isLongClickToastShown(this) || carDatabase.isEmptyOfCars(this))
            return;

        Util.showBlueToast(this, R.string.long_click_instructions, Toast.LENGTH_LONG);

        PreferencesUtil.setLongClickToastShown(this, true);
    }


    @Override
    protected void onStop() {
        unregisterCameraUpdateRequester(this);
        mGoogleApiClient.disconnect();
        if (newPurchaseReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(newPurchaseReceiver);
            newPurchaseReceiver = null;
        }
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
                delegate.setActive(false);
                delegate.setCameraFollowing(false);
            }

            hideDetails();

        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BillingFragment.REQUEST_ON_PURCHASE) {
            BillingFragment billingFragment = (BillingFragment) getFragmentManager().findFragmentByTag(BillingFragment.FRAGMENT_TAG);
            billingFragment.onActivityResult(requestCode, resultCode, data);
        }

        // FB
        if (mFacebookCallbackManager != null)
            mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);

    }

    public void setFacebookCallbackManager(CallbackManager facebookCallbackManager) {
        this.mFacebookCallbackManager = facebookCallbackManager;
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
    public void signOutAndGoToLoginScreen(boolean resetPreferences) {

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

        if (resetPreferences)
            PreferencesUtil.clear(this);

        AuthUtils.clearLoggedUserDetails(this);

        Log.v(TAG, "Sign out successful!");

        goToLogin();
    }

    @Override
    public void goToPreferences() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            if (isLocationPermissionGranted())
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(carUpdateReceiver);

        saveMapCameraPosition();
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
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
        mMap.setOnCameraMoveStartedListener(this);
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
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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

            final List<Car> parkedCars = carDatabase.retrieveParkedCars(this);

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

        LongTapLocationDelegate longTapLocationDelegate = initLongTapLocationDelegate();
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
     * This is called when the user moves the camera but also when it is triggered programmatically.
     */
    @Override
    public void onCameraMoveStarted(int reason) {

        if (mMap == null) return;

        for (CameraUpdateRequester requester : cameraUpdateRequesterList) {

            if (reason == REASON_GESTURE)
                requester.setCameraFollowing(false);

            requester.onCameraChange(mMap.getCameraPosition());

        }

        if (detailsFragment != null && detailsFragment.isResumed())
            detailsFragment.onCameraUpdate();


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
    public void setDetailsFragment(AbstractMarkerDelegate caller, DetailsFragment fragment) {

        if (isFinishing()) return;

        for (AbstractMarkerDelegate delegate : delegates) {
            if (delegate != caller)
                delegate.setActive(false);
        }

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
        mMap.animateCamera(cameraUpdate);

        for (CameraUpdateRequester registered : cameraUpdateRequesterList) {

            if (cameraUpdateRequester != registered) {
                registered.setCameraFollowing(false);
                registered.onCameraChange(mMap.getCameraPosition());
            }
        }
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
    public void onCameraChange(CameraPosition cameraPosition) {
    }

    /**
     * Only for debugging
     */
    private void setDebugConfig() {

        View debugLayout = findViewById(R.id.debug_layout);
        if (debugLayout == null) return;
        debugLayout.setVisibility(View.VISIBLE);


        Button refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener(v -> {
            initSpotsDelegate().queryCameraView();
            if (mAccount != null)
                CarsSync.TriggerRefresh(MapsActivity.this, mAccount);
            else
                Toast.makeText(MapsActivity.this, "Not logged in, so cannot perform refresh", Toast.LENGTH_SHORT).show();
        });

        Button startActRecog = findViewById(R.id.start_act_rec);
        startActRecog.setOnClickListener(v -> {
            ActivityRecognitionService.startActivityRecognition(MapsActivity.this);
        });

        Button actRecDetected = findViewById(R.id.act_recog);
        actRecDetected.setOnClickListener(v -> {
            List<Car> cars = carDatabase.retrieveCars(MapsActivity.this, false);
            if (cars.isEmpty()) return;
            // start the CarMovedReceiver
            LocationUpdatesHelper helper = new LocationUpdatesHelper(MapsActivity.this, PossibleParkedCarReceiver.ACTION);
            helper.startLocationUpdates(null);
        });

        Button carParked = findViewById(R.id.park);
        carParked.setOnClickListener(v -> {
            List<Car> cars = carDatabase.retrieveCars(MapsActivity.this, false);
            if (cars.isEmpty()) return;
            // start the CarMovedReceiver
            LocationUpdatesHelper helper = new LocationUpdatesHelper(MapsActivity.this, ParkedCarReceiver.ACTION);
            Bundle extras = new Bundle();
            extras.putString(Constants.EXTRA_CAR_ID, cars.iterator().next().id);
            helper.startLocationUpdates(extras);
        });

        Button carMoved = findViewById(R.id.driveOff);
        carMoved.setOnClickListener(v -> {
            List<Car> cars = carDatabase.retrieveCars(MapsActivity.this, false);
            if (cars.isEmpty()) return;
            // start the CarMovedReceiver
            LocationUpdatesHelper helper = new LocationUpdatesHelper(MapsActivity.this, CarMovedReceiver.ACTION);
            Bundle extras = new Bundle();
            extras.putString(Constants.EXTRA_CAR_ID, cars.iterator().next().id);
            helper.startLocationUpdates(extras);
        });

        Button approachingCar = findViewById(R.id.approaching);
        approachingCar.setOnClickListener(v -> {
            List<Car> cars = carDatabase.retrieveCars(MapsActivity.this, false);
            if (cars.isEmpty()) return;
            Car car = cars.iterator().next();
            ParkingSpotSender.doPostSpotLocation(MapsActivity.this, car.location, true, car);
        });
    }

    @Override
    public void onCarSelected(Car car) {

    }

}
