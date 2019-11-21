package com.whereismycar;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdIconView;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeBannerAd;
import com.facebook.login.LoginManager;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.whereismycar.activityrecognition.ActivityRecognitionService;
import com.whereismycar.activityrecognition.PossibleParkedCarDelegate;
import com.whereismycar.cars.CarManagerActivity;
import com.whereismycar.cars.database.CarDatabase;
import com.whereismycar.dialogs.DonateDialog;
import com.whereismycar.dialogs.RatingDialog;
import com.whereismycar.locationservices.CarMovedService;
import com.whereismycar.locationservices.ParkedCarService;
import com.whereismycar.locationservices.PossibleParkedCarService;
import com.whereismycar.login.AuthUtils;
import com.whereismycar.login.LoginActivity;
import com.whereismycar.model.Car;
import com.whereismycar.model.ParkingSpot;
import com.whereismycar.model.PossibleSpot;
import com.whereismycar.parkedcar.CarDetailsFragment;
import com.whereismycar.places.PlacesDelegate;
import com.whereismycar.setcarlocation.LongTapLocationDelegate;
import com.whereismycar.util.FetchAddressDelegate;
import com.whereismycar.util.PreferencesUtil;
import com.whereismycar.util.Tracking;
import com.whereismycar.util.Util;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import bolts.AppLinks;

import static android.content.Intent.ACTION_VIEW;
import static com.whereismycar.Constants.EXTRA_CAR_ID;

public class MapsActivity extends AppCompatActivity
        implements
        LocationListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraIdleListener,
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
        BillingFragment.OnBillingReadyListener {

    protected static final String TAG = MapsActivity.class.getSimpleName();

    static final String DETAILS_FRAGMENT_TAG = "DETAILS_FRAGMENT";

    private static final int REQUEST_PERMISSIONS_ACCESS_FINE_LOCATION = 10;


    private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAnalytics firebaseAnalytics;

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(3000)         // 3 seconds
            .setFastestInterval(32)    // 32ms = 30fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


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
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;


    private boolean mapInitialised = false;
    private boolean initialCameraSet = false;

    private boolean locationPermissionCurrentlyRequested = false;

    private int navBarHeight = 0;
    private int statusBarHeight = 0;
    private RelativeLayout detailsContainer;
    private DrawerLayout drawerLayout;

    private NativeAdLayout facebookNativeAdContainer;
    private com.google.android.gms.ads.formats.UnifiedNativeAdView adMobView;

    @Nullable
    private BroadcastReceiver newPurchaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideAds();
        }
    };

    private NativeBannerAd facebookNativeAd;

    private RelativeLayout mainContainer;

    private ListenerRegistration carListener;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        printFacebookHash();

        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, "ca-app-pub-7749631063131885~9161270854");

        carDatabase = CarDatabase.getInstance();

        mAccountManager = AccountManager.get(this);

        currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        /*
         * Bind service used for donations
         */
        initBillingFragment();

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Tracking.setTrackerUserId(currentUser.getUid());
        /*
         * Create a GoogleApiClient instance
         */
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(LocationServices.API)
                .addApiIfAvailable(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this);

        mGoogleApiClient = builder.build();

        drawerToggle = findViewById(R.id.navigation_drawer_toggle);
        if (drawerToggle != null)
            drawerToggle.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        mainContainer = findViewById(R.id.main_container);
        detailsContainer = findViewById(R.id.details_container);

        /*
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

        /*
         * Navigation drawer
         */
        setUpNavigationDrawer();

        myLocationButton = findViewById(R.id.my_location);
        myLocationButton.setBackgroundTintList(getResources().getColorStateList(R.color.button_states));
        myLocationButton.setOnClickListener(view -> {
            setCameraFollowing(true);
            Bundle bundle = new Bundle();
            firebaseAnalytics.logEvent("my_location_clicked", bundle);
        });

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
        noCarsButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            firebaseAnalytics.logEvent("no_cars_button_clicked", bundle);
            goToCarManager();
        });

        checkLocationPermission();

        facebookNativeAdContainer = findViewById(R.id.native_ad_container);
        if (facebookNativeAdContainer != null)
            facebookNativeAdContainer.setVisibility(View.GONE);

        adMobView = findViewById(R.id.adMobView);
        if (adMobView != null)
            adMobView.setVisibility(View.GONE);

        /*
         * Start activity recognition service (if enabled)
         */
        ActivityRecognitionService.startCheckingActivityRecognition(this);


    }

    private void printFacebookHash() {

        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.whereismycar",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {

        }
    }


    public void goToLogin() {
        if (!isFinishing()) {

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

        if (isFinishing()) return;

        db.collection("cars")
                .whereEqualTo("owner", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty())
                        checkForPurchases(billingFragment);
                });
    }

    private void checkForPurchases(final BillingFragment billingFragment) {

        if (isFinishing()) return;

        boolean hasActiveSubscription = billingFragment.hasActiveSubscription();

        boolean shouldDisplayAd = !hasActiveSubscription || BuildConfig.DEBUG_ADS;

        if (shouldDisplayAd) {
            firebaseAnalytics.setUserProperty("paying_user", "false");

            FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

            HashMap<String, Object> defaults = new HashMap<>();
            defaults.put("default_ad_provider", "facebook");
            defaults.put("dadaki_ad_header", "Dadaki: Lo mejor de Amazon");
            defaults.put("dadaki_ad_body", "La mejor selección diaria de productos de diseño de Amazon España");
            firebaseRemoteConfig.setDefaults(defaults);

            if (firebaseRemoteConfig.getString("default_ad_provider").equals("facebook")) {
                setUpFacebookAd();
            } else if (firebaseRemoteConfig.getString("default_ad_provider").equals("admob")) {
                setUpAdMobNative();
            }

        } else {
            firebaseAnalytics.setUserProperty("paying_user", "true");
        }


    }

    private void setUpDadakiAd() {

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        firebaseAnalytics.logEvent("dadaki_banner_displayed", new Bundle());

        facebookNativeAdContainer.setVisibility(View.VISIBLE);
        adMobView.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(MapsActivity.this);
        LinearLayout adView = (LinearLayout) inflater.inflate(R.layout.native_app_maps_ad, facebookNativeAdContainer, false);
        facebookNativeAdContainer.addView(adView);

        final AdIconView facebookNativeAdIcon = adView.findViewById(R.id.facebook_native_ad_icon);
        facebookNativeAdIcon.setVisibility(View.GONE);
        final ImageView nativeAdIcon = adView.findViewById(R.id.ad_app_icon);
        nativeAdIcon.setVisibility(View.VISIBLE);

        final TextView nativeAdTitle = adView.findViewById(R.id.native_ad_title);
        final TextView nativeAdBody = adView.findViewById(R.id.native_ad_body);
        nativeAdBody.setSingleLine(true);
        nativeAdBody.setSelected(true);
        nativeAdBody.setVisibility(View.VISIBLE);
        final Button nativeAdCallToAction = adView.findViewById(R.id.native_ad_call_to_action);

        nativeAdIcon.setImageResource(R.drawable.dadaki_icon);

        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        nativeAdTitle.setText(firebaseRemoteConfig.getString("dadaki_ad_header"));
        nativeAdBody.setText(firebaseRemoteConfig.getString("dadaki_ad_body"));
        nativeAdCallToAction.setText("Instalar");
        adView.setVisibility(View.VISIBLE);
        View.OnClickListener onClickListener = v -> {
            firebaseAnalytics.logEvent("dadaki_ad_clicked", new Bundle());
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=co.dadaki&utm_source=Parkify")));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=co.dadaki&utm_source=Parkify")));
            }
        };
        adView.setOnClickListener(onClickListener);
        nativeAdCallToAction.setOnClickListener(onClickListener);

    }

    private void hideAds() {
        if (facebookNativeAdContainer == null) return;
        if (adMobView == null) return;
        facebookNativeAdContainer.setVisibility(View.GONE);
        adMobView.setVisibility(View.GONE);
        facebookNativeAd.destroy();
    }

    private void setUpFacebookAd() {
        if (adMobView == null) return;

        adMobView.setVisibility(View.GONE);

        Log.d(TAG, "setUpFacebookAd");
        facebookNativeAd = new NativeBannerAd(MapsActivity.this, getString(R.string.facebook_maps_placement_id));
        facebookNativeAd.setAdListener(new NativeAdListener() {

            /**
             * On FB ad loaded
             *
             * @param ad
             */
            @Override
            public void onAdLoaded(Ad ad) {

                facebookNativeAd.unregisterView();

                facebookNativeAdContainer.setVisibility(View.VISIBLE);
                facebookNativeAdContainer.removeAllViews();
                adMobView.setVisibility(View.GONE);

                LayoutInflater inflater = LayoutInflater.from(MapsActivity.this);
                LinearLayout adView = (LinearLayout) inflater.inflate(R.layout.native_app_maps_ad, facebookNativeAdContainer, false);
                facebookNativeAdContainer.addView(adView);

                AdOptionsView adOptionsView = new AdOptionsView(
                        MapsActivity.this,
                        facebookNativeAd,
                        facebookNativeAdContainer,
                        AdOptionsView.Orientation.HORIZONTAL,
                        20);

                FrameLayout mAdChoicesContainer = adView.findViewById(R.id.ad_choices_container);
                mAdChoicesContainer.removeAllViews();
                mAdChoicesContainer.addView(adOptionsView);

                final AdIconView facebookNativeAdIcon = adView.findViewById(R.id.facebook_native_ad_icon);
                facebookNativeAdIcon.setVisibility(View.VISIBLE);
                final ImageView nativeAdIcon = adView.findViewById(R.id.ad_app_icon);
                nativeAdIcon.setVisibility(View.GONE);

                final TextView nativeAdTitle = adView.findViewById(R.id.native_ad_title);
                final TextView nativeAdBody = adView.findViewById(R.id.native_ad_body);
                nativeAdBody.setSingleLine(true);
                nativeAdBody.setSelected(true);
                nativeAdBody.setVisibility(View.VISIBLE);
                final Button nativeAdCallToAction = adView.findViewById(R.id.native_ad_call_to_action);

                FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
                nativeAdTitle.setText(firebaseRemoteConfig.getString("dadaki_ad_header"));
                nativeAdBody.setText(firebaseRemoteConfig.getString("dadaki_ad_body"));
                nativeAdCallToAction.setText("Instalar");
                adView.setVisibility(View.VISIBLE);

                nativeAdCallToAction.setText(facebookNativeAd.getAdCallToAction());
                nativeAdTitle.setText(facebookNativeAd.getAdvertiserName());
                nativeAdBody.setText(facebookNativeAd.getAdBodyText());

                List<View> clickableViews = new ArrayList<>();
//                clickableViews.add(facebookNativeAdIcon);
//                clickableViews.add(nativeAdTitle);
//                clickableViews.add(nativeAdBody);
                clickableViews.add(nativeAdCallToAction);

                setMapPadding();

                facebookNativeAd.registerViewForInteraction(facebookNativeAdContainer, facebookNativeAdIcon, clickableViews);

            }

            @Override
            public void onAdClicked(Ad ad) {
                Log.d(TAG, "onAdClicked: ");
                Tracking.sendEvent(Tracking.CATEGORY_ADVERTISING, Tracking.ACTION_AD_CLICKED, "Facebook");
                FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());
                Bundle bundle = new Bundle();
                firebaseAnalytics.logEvent("fb_ad_clicked", bundle);
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                Log.d(TAG, "onLoggingImpression: ");
                Tracking.sendEvent(Tracking.CATEGORY_ADVERTISING, Tracking.ACTION_AD_IMPRESSION, "Facebook");
                FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());
                Bundle bundle = new Bundle();
                firebaseAnalytics.logEvent("fb_ad_impression", bundle);
            }


            @Override
            public void onMediaDownloaded(Ad ad) {

            }

            /**
             * On FB ad error
             *
             * @param ad
             */
            @Override
            public void onError(Ad ad, AdError adError) {
                Log.d(TAG, "onAdError: " + adError.getErrorMessage());
                setUpAdMobNative();
            }
        });
        facebookNativeAd.loadAd();
    }


    private void setUpAdMobNative() {

        Locale locale = getResources().getConfiguration().locale;
        if (locale.toString().equals("es_ES")) {
            setUpDadakiAd();
            return;
        }

        if (facebookNativeAdContainer == null) return;
        if (adMobView == null) return;

        facebookNativeAdContainer.setVisibility(View.GONE);

        @SuppressLint("StaticFieldLeak") AdLoader adLoader = new AdLoader.Builder(MapsActivity.this, "ca-app-pub-7749631063131885/1189588351")
                .forUnifiedNativeAd(unifiedNativeAd -> {

                    adMobView.setNativeAd(unifiedNativeAd);
                    adMobView.setVisibility(View.VISIBLE);

                    final AdIconView facebookNativeAdIcon = adMobView.findViewById(R.id.facebook_native_ad_icon);
                    facebookNativeAdIcon.setVisibility(View.GONE);

                    FrameLayout mAdChoicesContainer = adMobView.findViewById(R.id.ad_choices_container);
                    mAdChoicesContainer.removeAllViews();

                    final ImageView nativeAdIcon = adMobView.findViewById(R.id.ad_app_icon);
                    nativeAdIcon.setVisibility(View.VISIBLE);

                    final TextView nativeAdTitle = adMobView.findViewById(R.id.native_ad_title);
                    final TextView nativeAdBody = adMobView.findViewById(R.id.native_ad_body);
                    nativeAdBody.setSelected(true);
                    nativeAdBody.setVisibility(View.VISIBLE);
                    final Button nativeAdCallToAction = adMobView.findViewById(R.id.native_ad_call_to_action);

                    nativeAdTitle.setText(unifiedNativeAd.getHeadline());
                    adMobView.setHeadlineView(nativeAdTitle);
                    nativeAdBody.setText(unifiedNativeAd.getBody());
                    adMobView.setBodyView(nativeAdBody);
                    nativeAdCallToAction.setText(unifiedNativeAd.getCallToAction());
                    adMobView.setCallToActionView(nativeAdCallToAction);

                    if (unifiedNativeAd.getIcon() != null) {
                        AsyncTask<Void, Void, Bitmap> asyncTask = new AsyncTask<Void, Void, Bitmap>() {
                            @Override
                            protected Bitmap doInBackground(Void[] objects) {
                                try {
                                    URL url = new URL(unifiedNativeAd.getIcon().getUri().toString());
                                    Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                    return bmp;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            }

                            @Override
                            protected void onPostExecute(Bitmap bitmap) {
                                super.onPostExecute(bitmap);
                                nativeAdIcon.setImageBitmap(bitmap);
                            }
                        };
                        asyncTask.execute();
                    }
                    adMobView.setIconView(nativeAdIcon);

                    setMapPadding();

                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        Log.d(TAG, "admob onAdFailedToLoad: " + errorCode);
                        adMobView.setVisibility(View.GONE);
                        setUpFacebookAd();
                    }

                    @Override
                    public void onAdClicked() {
                        Tracking.sendEvent(Tracking.CATEGORY_ADVERTISING, Tracking.ACTION_AD_CLICKED, "AdMob");
                        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());
                        Bundle bundle = new Bundle();
                        firebaseAnalytics.logEvent("admob_ad_clicked", bundle);
                    }

                    @Override
                    public void onAdImpression() {
                        Tracking.sendEvent(Tracking.CATEGORY_ADVERTISING, Tracking.ACTION_AD_IMPRESSION, "AdMob");
                        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());
                        Bundle bundle = new Bundle();
                        firebaseAnalytics.logEvent("admob_ad_impression", bundle);
                    }

                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build())
                .build();
        adLoader.loadAds(new AdRequest.Builder().addTestDevice("1A8F30F81D60C9F244F7AB1F7C359F24").build(), 3);

    }

    @Override
    protected void onStart() {
        super.onStart();

        long initTime = System.currentTimeMillis();

        registerCameraUpdateRequester(this);

        LocalBroadcastManager.getInstance(MapsActivity.this).registerReceiver(newPurchaseReceiver, new IntentFilter(Constants.INTENT_ADS_REMOVED));

        mGoogleApiClient.connect();

        Log.d("App speed", "On start init time : " + (System.currentTimeMillis() - initTime));

    }

    @Override
    protected void onResume() {

        super.onResume();

        long initTime = System.currentTimeMillis();

        Tracking.sendView(Tracking.CATEGORY_MAP);

        delegates.clear();

        carListener = db.collection("cars")
                .whereEqualTo("owner", currentUser.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null) return;

                    boolean hasCarWithBt = false;
                    for (DocumentSnapshot documentSnapshot : snapshot.getDocuments()) {
                        Car car = Car.fromFirestore(documentSnapshot);
                        if (car.location != null && car.address == null) {
                            Log.d(TAG, "Car fetched with no address");
                            updateCarLocationAddress(car);
                        }
                        if (car.btAddress != null) hasCarWithBt = true;
                        ParkedCarDelegate parkedCarDelegate = initParkedCarDelegate(car.id);
                        if (mMap != null)
                            parkedCarDelegate.setMap(mMap);
                        parkedCarDelegate.update(car, false);
                        delegates.add(parkedCarDelegate);
                    }

                    firebaseAnalytics.setUserProperty("has_car_with_bt", String.valueOf(hasCarWithBt));

                    noCarsButton.setVisibility(snapshot.isEmpty() ? View.VISIBLE : View.GONE);
                });

        /*
         * Add delegates
         */
//        delegates.add(initSpotsDelegate());
//
//        delegates.add(initPlacesDelegate());

        for (PossibleSpot spot : carDatabase.retrievePossibleParkingSpots(this))
            delegates.add(initPossibleParkedCarDelegate(spot));


        /*
         * Show some dialogs in case the user is bored
         */
        checkRatingDialogShown();

//        // when our activity resumes, we want to register for car updates
//        LocalBroadcastManager.getInstance(this).registerReceiver(carUpdateReceiver, new IntentFilter(Constants.INTENT_CAR_UPDATED));

        setInitialCamera();


        db.collection("cars")
                .whereEqualTo("owner", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (PreferencesUtil.isLongClickToastShown(this) || queryDocumentSnapshots.isEmpty())
                        return;

                    if (!isFinishing())
                        Util.showToast(this, R.string.long_click_instructions, Toast.LENGTH_LONG);

                    PreferencesUtil.setLongClickToastShown(this, true);
                });

        mapFragment.getMapAsync(this);

        Uri targetUrl = AppLinks.getTargetUrlFromInboundIntent(this, getIntent());
        if (targetUrl != null) {
            Log.i("Activity", "App Link Target URL: " + targetUrl.toString());
        }

        Log.d("App speed", "On resume init time : " + (System.currentTimeMillis() - initTime));
    }

    private void updateCarLocationAddress(Car car) {
        FetchAddressDelegate fetchAddressDelegate = new FetchAddressDelegate();
        fetchAddressDelegate.fetch(this, car.location, new FetchAddressDelegate.Callbacks() {
            @Override
            public void onAddressFetched(String address) {
                carDatabase.updateAddress(car.id, address);
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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
                mNotifyMgr.cancel(PossibleParkedCarService.NOTIFICATION_ID);
                intent.setAction(null);
            } else if (intent.getAction().equals(ACTION_VIEW)) {

                String carId = intent.getStringExtra(Constants.EXTRA_CAR_ID);
                ParkedCarDelegate parkedCarDelegate = initParkedCarDelegate(carId);
                parkedCarDelegate.activate();

                NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);
                mNotifyMgr.cancel(carId, ParkedCarService.NOTIFICATION_ID);
                intent.setAction(null);
            }
        }
    }

    public void checkRatingDialogShown() {
        if (RatingDialog.shouldBeShown(this)) {
            if (isFinishing()) return;
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
                    if (!isFinishing())
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
            transaction.commitAllowingStateLoss();
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
            transaction.commitAllowingStateLoss();
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
            transaction.commitAllowingStateLoss();
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
            transaction.commitAllowingStateLoss();
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
            transaction.commitAllowingStateLoss();
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
            transaction.commitAllowingStateLoss();
            getFragmentManager().executePendingTransactions();
        }
        return longTapLocationDelegate;
    }

    private void setMapPadding() {
        if (mMap == null) return;
        mainContainer.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int adHeight = facebookNativeAdContainer == null ? 0 : facebookNativeAdContainer.getMeasuredHeight();
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


    @Override
    protected void onStop() {
        unregisterCameraUpdateRequester(this);
        mGoogleApiClient.disconnect();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(newPurchaseReceiver);
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
     * Sign out the user (so they can switch to another account).
     */
    public void signOutAndGoToLoginScreen(boolean resetPreferences) {

        if (!currentUser.isAnonymous())
            firebaseAuth.signOut();

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

        carListener.remove();

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
        mMap.setOnCameraIdleListener(this);
    }


    public void openDonationDialog() {
        if (isFinishing()) return;
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

            carDatabase.retrieveCars(new CarDatabase.CarsRetrieveListener() {
                @Override
                public void onCarsRetrieved(List<Car> cars) {

                    List<Car> closeCars = new ArrayList<>();
                    for (Car car : cars) {
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

                @Override
                public void onCarsRetrievedError() {

                }
            });

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

        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent("map_long_click", bundle);

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

        }

        if (detailsFragment != null && detailsFragment.isResumed())
            detailsFragment.onCameraUpdate();


        Log.v(TAG, "onCameraMoveStarted");
    }


    @Override
    public void onCameraIdle() {
        if (mMap == null) return;

        for (CameraUpdateRequester requester : cameraUpdateRequesterList) {
            requester.onCameraChange(mMap.getCameraPosition());
        }

        Log.v(TAG, "onCameraMove");
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
        });

        Button startActRecog = findViewById(R.id.start_act_rec);
        startActRecog.setOnClickListener(v -> {
            ActivityRecognitionService.startActivityRecognition(MapsActivity.this);
        });

        Button actRecDetected = findViewById(R.id.act_recog);
        actRecDetected.setOnClickListener(v -> carDatabase.retrieveCars(new CarDatabase.CarsRetrieveListener() {
            @Override
            public void onCarsRetrieved(List<Car> cars) {
                if (cars.isEmpty()) return;
                // start the CarMovedService

                Intent intent = new Intent(MapsActivity.this, PossibleParkedCarService.class);
                ContextCompat.startForegroundService(MapsActivity.this, intent);

            }

            @Override
            public void onCarsRetrievedError() {

            }
        }));

        Button carParked = findViewById(R.id.park);
        carParked.setOnClickListener(v -> {
            carDatabase.retrieveCars(new CarDatabase.CarsRetrieveListener() {
                @Override
                public void onCarsRetrieved(List<Car> cars) {
                    if (cars.isEmpty()) return;

                    Intent intent = new Intent(MapsActivity.this, ParkedCarService.class);
                    intent.putExtra(EXTRA_CAR_ID, cars.iterator().next().id);
                    ContextCompat.startForegroundService(MapsActivity.this, intent);

                }

                @Override
                public void onCarsRetrievedError() {

                }
            });
        });

        Button carMoved = findViewById(R.id.driveOff);
        carMoved.setOnClickListener(v -> {
            carDatabase.retrieveCars(new CarDatabase.CarsRetrieveListener() {
                @Override
                public void onCarsRetrieved(List<Car> cars) {
                    if (cars.isEmpty()) return;

                    Intent intent = new Intent(MapsActivity.this, CarMovedService.class);
                    intent.putExtra(EXTRA_CAR_ID, cars.iterator().next().id);
                    ContextCompat.startForegroundService(MapsActivity.this, intent);
                }

                @Override
                public void onCarsRetrievedError() {

                }
            });
        });

        Button approachingCar = findViewById(R.id.approaching);
        approachingCar.setOnClickListener(v -> {
            carDatabase.retrieveCars(new CarDatabase.CarsRetrieveListener() {
                @Override
                public void onCarsRetrieved(List<Car> cars) {
                    if (cars.isEmpty()) return;
                    Car car = cars.iterator().next();
                    // TODO: about to move car
                }

                @Override
                public void onCarsRetrievedError() {

                }
            });
        });
    }

    @Override
    public void onCarSelected(Car car) {

    }

}
