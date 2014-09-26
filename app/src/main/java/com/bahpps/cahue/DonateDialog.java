package com.bahpps.cahue;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.auth.GoogleAuthUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * This class is used as a dialog to ask the user if he is sure to store the location in
 * the indicated place.
 *
 * @author Francesco
 */
public class DonateDialog extends DialogFragment {

    private static String PRODUCT_DONATION_1 = "donation_1";
    private static String PRODUCT_DONATION_2 = "donation_2";
    private static String PRODUCT_DONATION_5 = "donation_5";

    private final static String TAG = "DonateDialog";

    IInAppBillingService mService;

    private Bundle queryPurchaseItems() {

        Log.d(TAG, "Querying products");

        try {
            ArrayList<String> skuList = new ArrayList<String>();
            skuList.add(PRODUCT_DONATION_1);
            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
            return mService.getSkuDetails(3, getActivity().getPackageName(), "inapp", querySkus);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setIInAppBillingService(IInAppBillingService mService) {
        this.mService = mService;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        final LinearLayout view = (LinearLayout) inflater.inflate(R.layout.donate_dialog, null);

        final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radiogroup);
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        final TextView errorTextView = (TextView) view.findViewById(R.id.error_text);

        new AsyncTask<Void, Void, Bundle>() {
            @Override
            protected Bundle doInBackground(Void... voids) {
                return queryPurchaseItems();
            }

            @Override
            protected void onPostExecute(Bundle skuDetails) {

                int response = skuDetails.getInt("RESPONSE_CODE");

                progressBar.setVisibility(View.GONE);

                if (response == 0) {
                    radioGroup.setVisibility(View.VISIBLE);
                    errorTextView.setVisibility(View.GONE);
                    try {
                        ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");

                        for (String thisResponse : responseList) {

                            JSONObject object = null;
                            object = new JSONObject(thisResponse);
                            String sku = object.getString("productId");
                            String price = object.getString("price");

                            RadioButton radioButton = new RadioButton(getActivity());
                            if ("donation_1".equals(sku))
                                radioButton.setText(R.string.donation_1);
                            else if ("donation_2".equals(sku))
                                radioButton.setText(R.string.donation_2);
                            else if ("donation_5".equals(sku))
                                radioButton.setText(R.string.donation_5);
                            else
                                radioButton.setText(sku);

                            radioButton.setTag(sku);

                            radioGroup.addView(radioButton);
                        }
                    } catch (JSONException e) {
                        errorTextView.setVisibility(View.VISIBLE);
                        errorTextView.setText("ERROR. Json Exception...");
                        e.printStackTrace();
                    }
                } else {
                    radioGroup.setVisibility(View.GONE);
                    errorTextView.setVisibility(View.VISIBLE);
                    errorTextView.setText("ERROR. Response: " + response);
                }

            }
        }.execute();


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Use the Builder class for convenient dialog construction new AlertDialog.Builder()
        builder
                .setTitle(R.string.donate)
                .setView(view)
                .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radiogroup);
                                int checkedRadioButton = 0;
                                try {
                                    checkedRadioButton = radioGroup.getCheckedRadioButtonId();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                View selectedRadioButton = radioGroup.findViewById(checkedRadioButton);
                                String selectedTag = (String) selectedRadioButton.getTag();
                                int i = 0;
//                                switch (checkedRadioButton) {
//                                    case R.id.a2s:
//                                        datasource.updateIcon(i, itemid);
//                                        break;
//                                    case R.id.android:
//                                        i = 1;
//                                        datasource.updateIcon(i, itemid);
//                                        break;
//                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Canceled.
                            }
                        });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void doPurchase(String sku){
        try {

            Bundle buyIntentBundle = mService.getBuyIntent(3,
                    getActivity().getPackageName(),
                    sku,
                    "inapp",
                    "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");


            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                    1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

}
