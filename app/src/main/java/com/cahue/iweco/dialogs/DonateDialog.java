package com.cahue.iweco.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.cahue.iweco.BillingFragment;
import com.cahue.iweco.R;

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
                                billingFragment.doPurchase(sku);
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
        final TextView errorTextView = (TextView) view.findViewById(R.id.text);

        /**
         * AsyncTask population the donation options
         */
        new AsyncTask<Void, Void, Bundle>() {
            @Override
            protected Bundle doInBackground(Void... voids) {
                return billingFragment.queryAvailableItems();
            }

            @Override
            protected void onPostExecute(@NonNull Bundle skuDetails) {

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


}
