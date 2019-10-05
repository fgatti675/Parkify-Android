package com.whereismycar;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.whereismycar.util.Tracking;
import com.whereismycar.util.Util;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by francesco on 21.04.2015.
 */
public class BillingFragment extends Fragment implements PurchasesUpdatedListener {

    public static final String FRAGMENT_TAG = "BILLING_DELEGATE";

    public static final int REQUEST_ON_PURCHASE = 1001;

    private static final String TAG = BillingFragment.class.getSimpleName();

    private static final String DONATE_2_YEARLY = "donate_2_yearly";
    private static final String DONATE_5_YEARLY = "donate_5_yearly";
    private static final String DONATE_10_YEARLY = "donate_10_yearly";
    private BillingClient billingClient;
    private OnBillingReadyListener onBillingReadyListener;

    private String error;

    @NonNull
    public static BillingFragment newInstance() {
        BillingFragment fragment = new BillingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context activity) {

        super.onAttach(activity);
        billingClient = BillingClient.newBuilder(activity).enablePendingPurchases().setListener(this).build();

        if (getActivity() instanceof OnBillingReadyListener)
            this.onBillingReadyListener = (OnBillingReadyListener) activity;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                Log.i(TAG, "onBillingSetupFinished");
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    onBillingReadyListener.onBillingReady(BillingFragment.this);
                }
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                    error = billingResult.getDebugMessage();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.e(TAG, "onBillingServiceDisconnected");
            }
        });

    }

    @Override
    public void onDetach() {
        super.onDetach();
        onBillingReadyListener = null;
        billingClient.endConnection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Nullable
    public void queryAvailableItems(SkuDetailsResponseListener listener) {

        Log.d(TAG, "Querying products");

        ArrayList<String> skuList = new ArrayList<>();
        if (BuildConfig.DEBUG) {
            skuList.add("android.test.purchased");
            skuList.add("android.test.canceled");
            skuList.add("android.test.item_unavailable");
        }
        skuList.add(DONATE_2_YEARLY);
        skuList.add(DONATE_5_YEARLY);
        skuList.add(DONATE_10_YEARLY);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
        billingClient.querySkuDetailsAsync(params.build(),
                listener);

    }


    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());

        // element purchase
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

            purchases.forEach(purchase -> {
                handlePurchase(purchase);
                Bundle bundle = new Bundle();
                bundle.putString("sku", purchase.getSku());
                firebaseAnalytics.logEvent("donation_successful", bundle);

                saveOnFirebase(purchase.getSku());
            });

            if (isAdded())
                Util.showToast(getActivity(), R.string.thanks, Toast.LENGTH_LONG); // do string

            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(Constants.INTENT_ADS_REMOVED));


        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Bundle bundle = new Bundle();
            firebaseAnalytics.logEvent("donation_cancelled", bundle);
            Tracking.sendEvent(Tracking.CATEGORY_DONATION_DIALOG, Tracking.ACTION_PURCHASE_CANCELLED);

        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
                || billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {

            if (isAdded())
                Util.showToast(getActivity(), getString(R.string.purchase_account_error), Toast.LENGTH_LONG);
            Tracking.sendEvent(Tracking.CATEGORY_DONATION_DIALOG, Tracking.ACTION_PURCHASE_ERROR);
            Bundle bundle = new Bundle();
            firebaseAnalytics.logEvent("donation_error", bundle);
        }
    }

    void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user.

            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {

                });
            }
        }
    }

    public boolean hasActiveSubscription() {
        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS);
        purchasesResult.getPurchasesList().forEach(this::handlePurchase);
        boolean hasActiveSubscriptions = purchasesResult.getPurchasesList().stream()
                .anyMatch(purchase -> purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED);
        Purchase.PurchasesResult oldPurchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        oldPurchasesResult.getPurchasesList().forEach(this::handlePurchase);
        long oneYearAgo = new Date().getTime() - (365 * 24 * 60 * 60 * 1000);
        boolean hasOldValidPurchases = purchasesResult.getPurchasesList().stream()
                .anyMatch(purchase -> purchase.getPurchaseTime() > oneYearAgo);
        return hasActiveSubscriptions || hasOldValidPurchases;
    }


    public void doPurchase(SkuDetails skuDetails) {

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
        Bundle bundle = new Bundle();
        bundle.putString("sku", skuDetails.getSku());
        firebaseAnalytics.logEvent("donation_intent", bundle);

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        billingClient.launchBillingFlow(getActivity(), flowParams);
    }

    private void saveOnFirebase(String sku) {
        Map<String, Object> purchase = new HashMap<>();
        purchase.put("sku", sku);
        purchase.put("date", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("users")
                .document(FirebaseAuth.getInstance().getUid())
                .collection("purchases")
                .add(purchase)
                .addOnFailureListener(Crashlytics::logException);
    }

    public interface OnBillingReadyListener {
        void onBillingReady(BillingFragment billingFragment);
    }
}
