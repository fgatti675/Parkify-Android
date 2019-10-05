package com.whereismycar.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.SkuDetails;
import com.whereismycar.BillingFragment;
import com.whereismycar.R;

import org.json.JSONObject;


/**
 * This class is used as a dialog to ask the user if he is sure to store the location in
 * the indicated place.
 *
 * @author Francesco
 */
public class DonateDialog extends DialogFragment {

    private final static String TAG = "DonateDialog";

    private BillingFragment billingFragment;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        billingFragment = (BillingFragment) getFragmentManager().findFragmentByTag(BillingFragment.FRAGMENT_TAG);

        LayoutInflater inflater = getActivity().getLayoutInflater();

        final LinearLayout view = (LinearLayout) inflater.inflate(R.layout.dialog_donate, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Use the Builder class for convenient dialog construction new AlertDialog.Builder()
        builder
                .setTitle(R.string.donate)
                .setView(view)
                .setPositiveButton("Ok",
                        (dialog, whichButton) -> {
                            RadioGroup radioGroup = view.findViewById(R.id.radiogroup);
                            int checkedRadioButton = 0;
                            try {
                                checkedRadioButton = radioGroup.getCheckedRadioButtonId();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            View selectedRadioButton = radioGroup.findViewById(checkedRadioButton);
                            SkuDetails sku = (SkuDetails) selectedRadioButton.getTag();
                            billingFragment.doPurchase(sku);
                        })
                .setNegativeButton("Cancel",
                        (dialog, whichButton) -> {
                            // Canceled.
                        });


        // Create the AlertDialog object and return it
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false));


        final RadioGroup radioGroup = view.findViewById(R.id.radiogroup);
        radioGroup.setOnCheckedChangeListener((radioGroup1, i) -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true));

        final ProgressBar progressBar = view.findViewById(R.id.progress_bar);
        final TextView errorTextView = view.findViewById(R.id.text);

        /*
         * AsyncTask population the donation options
         */
        if (billingFragment == null) return null;
        billingFragment.queryAvailableItems((billingResult, skuDetailsList) -> {
            if (skuDetailsList == null) return;

            int response = billingResult.getResponseCode();

            progressBar.setVisibility(View.GONE);

            if (response == BillingClient.BillingResponseCode.OK) {
                radioGroup.setVisibility(View.VISIBLE);
                errorTextView.setVisibility(View.GONE);

                for (SkuDetails skuDetail : skuDetailsList) {

                    JSONObject object = null;
                    String sku = skuDetail.getSku();
                    String title = skuDetail.getTitle();
                    String price = skuDetail.getPrice();

                    RadioButton radioButton = new RadioButton(getActivity());
                    radioButton.setText(title);

                    radioButton.setTag(skuDetail);

                    radioGroup.addView(radioButton);
                }
            } else {
                radioGroup.setVisibility(View.GONE);
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText("ERROR. Response: " + response);
            }
        });


        return dialog;
    }


}
