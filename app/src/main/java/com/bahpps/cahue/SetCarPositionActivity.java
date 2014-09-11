package com.bahpps.cahue;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.bahpps.cahue.auxiliar.Util;


/**
 * This class is used as a dialog to ask the user if he is sure to store the location in
 * the indicated place.
 * @author Francesco
 *
 */
public class SetCarPositionActivity extends Activity implements OnClickListener {

	Button okClose;
	Button cancelClose;

    Location loc;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		/** It will hide the title */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.set_car_position_dialog);

		okClose = (Button) findViewById(R.id.okCloseButton);
		okClose.setOnClickListener(this);

		cancelClose = (Button) findViewById(R.id.cancelCloseButton);
		cancelClose.setOnClickListener(this);

		loc = (Location) getIntent().getExtras().get(Util.EXTRA_LOCATION);
	}

	public void onClick(View button) {

		if (button == okClose) {
			Log.w("CAR_DIALOG", loc.toString());
			// If ok, we just send and intent and leave the location receivers to do all the work
			Intent intent = new Intent(Util.INTENT_NEW_CAR_POS);
			intent.putExtra(Util.EXTRA_LOCATION, loc);
			sendBroadcast(intent);
			finish();
		}

        else if (button == cancelClose) {
			finish();
		}

	}

}
