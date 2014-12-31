package com.bahpps.cahue;

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
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.bahpps.cahue.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;


/**
 * This class is used as a dialog to ask the user if he is sure to store the location in
 * the indicated place.
 *
 * @author Francesco
 */
public class DonateDialog extends DialogFragment {

    @Deprecated
    private static String PRODUCT_DONATION_1 = "donate_1";
    @Deprecated
    private static String PRODUCT_DONATION_2 = "donate_2";
    @Deprecated
    private static String PRODUCT_DONATION_5 = "donate_5";

    private static String PRODUCT_DONATION_1_ADMINISTERED = "donate_1_administered";
    private static String PRODUCT_DONATION_2_ADMINISTERED = "donate_2_administered";
    private static String PRODUCT_DONATION_5_ADMINISTERED = "donate_5_administered";

    private final static String TAG = "DonateDialog";

    IInAppBillingService mService;

    private Bundle queryPurchaseItems() {

        Log.d(TAG, "Querying products");

        try {
            ArrayList<String> skuList = new ArrayList<String>();
//            skuList.add("android.test.purchased");
            skuList.add(PRODUCT_DONATION_1_ADMINISTERED);
            skuList.add(PRODUCT_DONATION_2_ADMINISTERED);
            skuList.add(PRODUCT_DONATION_5_ADMINISTERED);
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

        final LinearLayout view = (LinearLayout) inflater.inflate(R.layout.dialog_donate, null);

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
                                String sku = (String) selectedRadioButton.getTag();
                                doPurchase(sku);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Canceled.
                            }
                        });


        // Create the AlertDialog object and return it
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });


        final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radiogroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        });

        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        final TextView errorTextView = (TextView) view.findViewById(R.id.error_text);

        /**
         * AsyncTask population the donation options
         */
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
                            String title = object.getString("title");
                            String price = object.getString("price");

                            RadioButton radioButton = new RadioButton(getActivity());
                            radioButton.setText(title);

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


        return dialog;
    }

    private void doPurchase(String sku) {
        try {

            Bundle buyIntentBundle = mService.getBuyIntent(3,
                    getActivity().getPackageName(),
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
