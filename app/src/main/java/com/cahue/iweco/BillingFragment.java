package com.cahue.iweco;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.cahue.iweco.util.Util;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by francesco on 21.04.2015.
 */
public class BillingFragment extends Fragment {

    public static final String FRAGMENT_TAG = "BILLING_DELEGATE";

    static final int REQUEST_ON_PURCHASE = 1001;

    private static final String TAG = BillingFragment.class.getSimpleName();

    private static String PRODUCT_DONATION_1_ADMINISTERED = "donate_1_administered";
    private static String PRODUCT_DONATION_2_ADMINISTERED = "donate_2_administered";
    private static String PRODUCT_DONATION_5_ADMINISTERED = "donate_5_administered";

    private String packageName;

    public static BillingFragment newInstance() {
        BillingFragment fragment = new BillingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    private IInAppBillingService iInAppBillingService;

    private ServiceConnection mBillingServiceConn = new ServiceConnection() {
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
            getActivity().sendBroadcast(new Intent(Constants.INTENT_BILLING_READY));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        packageName = activity.getPackageName();
        bindBillingService();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (iInAppBillingService != null) {
            getActivity().unbindService(mBillingServiceConn);
        }
    }

    private void bindBillingService() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        getActivity().bindService(serviceIntent, mBillingServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public Bundle queryAvailableItems() {

        Log.d(TAG, "Querying products");

        try {
            ArrayList<String> skuList = new ArrayList<String>();
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
            return iInAppBillingService.getSkuDetails(3, packageName, "inapp", querySkus);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isBillingServiceReady() {
        return iInAppBillingService != null;
    }

    public Bundle getPurchases() {
        try {
            Bundle bundle = iInAppBillingService.getPurchases(3, packageName, "inapp", null);
            return bundle;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // element purchase
        if (requestCode == REQUEST_ON_PURCHASE) {

            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == getActivity().RESULT_OK) {
//                try {
//                    JSONObject jo = new JSONObject(purchaseData);
//                    String sku = jo.getString("productId");
                Util.createUpperToast(getActivity(), R.string.thanks, Toast.LENGTH_LONG); // do string
//                } catch (JSONException e) {
//                    Util.createUpperToast(this, "Failed to parse purchase data.", Toast.LENGTH_LONG);
//                    e.printStackTrace();
//                }

                getActivity().sendBroadcast(new Intent(Constants.INTENT_NEW_PURCHASE));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void doPurchase(String sku) {
        try {

            Bundle buyIntentBundle = iInAppBillingService.getBuyIntent(3,
                    packageName,
                    sku,
                    "inapp",
                    UUID.randomUUID().toString());

            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            if (pendingIntent == null) {
                Util.createUpperToast(getActivity(), getString(R.string.purchase_account_error), Toast.LENGTH_LONG);
            } else {
                getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                        1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                        Integer.valueOf(0));
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }
}
