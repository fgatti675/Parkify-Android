package com.cahue.iweco;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Tracking;
import com.cahue.iweco.util.Util;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by francesco on 21.04.2015.
 */
public class BillingFragment extends Fragment {

    public static final String FRAGMENT_TAG = "BILLING_DELEGATE";

    public static final int REQUEST_ON_PURCHASE = 1001;

    private static final String TAG = BillingFragment.class.getSimpleName();

    private static final String PRODUCT_DONATION_1_ADMINISTERED = "donate_1_administered";
    private static final String PRODUCT_DONATION_2_ADMINISTERED = "donate_2_administered";
    private static final String PRODUCT_DONATION_5_ADMINISTERED = "donate_5_administered";

    private IInAppBillingService iInAppBillingService;

    @Nullable
    private OnBillingReadyListener onBillingReadyListener;

    private final ServiceConnection mBillingServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            iInAppBillingService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iInAppBillingService = IInAppBillingService.Stub.asInterface(service);

            /**
             * Tell everyone the billing service is ready
             */
            if (getActivity() != null) {
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(Constants.INTENT_BILLING_READY));
                onBillingReadyListener.onBillingReady(BillingFragment.this);
            }
        }
    };

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        getActivity().bindService(serviceIntent, mBillingServiceConn, Context.BIND_AUTO_CREATE);

        if (activity instanceof OnBillingReadyListener)
            this.onBillingReadyListener = (OnBillingReadyListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mBillingServiceConn != null) {
            getActivity().unbindService(mBillingServiceConn);
        }
        onBillingReadyListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Nullable
    public Bundle queryAvailableItems() {


        Log.d(TAG, "Querying products");

        try {
            ArrayList<String> skuList = new ArrayList<>();
            if (BuildConfig.DEBUG) {
                skuList.add("android.test.purchased");
                skuList.add("android.test.canceled");
                skuList.add("android.test.item_unavailable");
            }
            skuList.add(PRODUCT_DONATION_1_ADMINISTERED);
            skuList.add(PRODUCT_DONATION_2_ADMINISTERED);
            skuList.add(PRODUCT_DONATION_5_ADMINISTERED);
            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
            return iInAppBillingService.getSkuDetails(3, BuildConfig.APPLICATION_ID, "inapp", querySkus);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isBillingServiceReady() {
        return iInAppBillingService != null;
    }

    public interface PurchasesCheckListener {
        void onPurchaseCheck(List<String> purchaseDataList);
    }

    public static class CheckPurchaseAsyncTask extends AsyncTask<Void, Void, List<String>> {
        IInAppBillingService iInAppBillingService;
        PurchasesCheckListener listener;

        public CheckPurchaseAsyncTask(IInAppBillingService iInAppBillingService, PurchasesCheckListener listener) {
            this.iInAppBillingService = iInAppBillingService;
            this.listener = listener;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {

            try {
                Bundle ownedItems = iInAppBillingService.getPurchases(3, BuildConfig.APPLICATION_ID, "inapp", null);
                int response = ownedItems.getInt("RESPONSE_CODE");
                if (response == 0) {
                    List<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                    Log.d(TAG, "Purchased items: " + purchaseDataList.toString());
                    return purchaseDataList;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<String> purchaseDataList) {
            super.onPostExecute(purchaseDataList);
            listener.onPurchaseCheck(purchaseDataList);
        }
    }


    public void checkPurchases(PurchasesCheckListener listener) {
        new CheckPurchaseAsyncTask(iInAppBillingService, listener).execute();
    }


    public void doPurchase(String sku) {
        try {

            Bundle buyIntentBundle = iInAppBillingService.getBuyIntent(3,
                    BuildConfig.APPLICATION_ID,
                    sku,
                    "inapp",
                    UUID.randomUUID().toString());

            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
            if (pendingIntent == null) {
                Util.showBlueToast(getActivity(), getString(R.string.purchase_account_error), Toast.LENGTH_LONG);
                Tracking.sendEvent(Tracking.CATEGORY_DONATION_DIALOG, Tracking.ACTION_PURCHASE_ERROR);
                Bundle bundle = new Bundle();
                bundle.putString("sku", sku);
                firebaseAnalytics.logEvent("donation_error", bundle);
            } else {
                Bundle bundle = new Bundle();
                bundle.putString("sku", sku);
                firebaseAnalytics.logEvent("donation_intent", bundle);
                getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                        1001, new Intent(), 0, 0, 0);
            }

        } catch (RemoteException | IntentSender.SendIntentException e) {
            e.printStackTrace();
            Tracking.sendException(Tracking.CATEGORY_DONATION_DIALOG, e.getMessage(), false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());

        // element purchase
        if (requestCode == REQUEST_ON_PURCHASE) {

            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == Activity.RESULT_OK) {

                if (purchaseData != null) {
                    try {

                        JSONObject jo = new JSONObject(purchaseData);
                        String sku = jo.getString("productId");

                        Tracking.sendEvent(Tracking.CATEGORY_DONATION_DIALOG, Tracking.ACTION_PURCHASE_SUCCESSFUL, sku);

                        Bundle bundle = new Bundle();
                        bundle.putString("sku", sku);
                        firebaseAnalytics.logEvent("donation_successful", bundle);

                    } catch (JSONException e) {
                    }

                } else {
                    Tracking.sendEvent(Tracking.CATEGORY_DONATION_DIALOG, Tracking.ACTION_PURCHASE_SUCCESSFUL);
                    Bundle bundle = new Bundle();
                    firebaseAnalytics.logEvent("donation_successful", bundle);
                }

                Util.showBlueToast(getActivity(), R.string.thanks, Toast.LENGTH_LONG); // do string

                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(Constants.INTENT_ADS_REMOVED));
                PreferencesUtil.setAdsRemoved(getActivity(), true);

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Bundle bundle = new Bundle();
                firebaseAnalytics.logEvent("donation_cancelled", bundle);
                Tracking.sendEvent(Tracking.CATEGORY_DONATION_DIALOG, Tracking.ACTION_PURCHASE_CANCELLED);
            }
        }

    }

    public interface OnBillingReadyListener {
        void onBillingReady(BillingFragment billingFragment);
    }
}
