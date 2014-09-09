package com.bahpps.cahue;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/**
 * Fancy dialog we used as information
 * @author Francesco
 *
 */
public class InfoDialog extends Dialog implements OnClickListener {
	Button close;

	public InfoDialog(Context context) {
		// we use our own style
		super(context, R.style.DialogStyle);
		
		// It will hide the title 
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.info_dialog);

		// getWindow().getAttributes().windowAnimations = R.style.DialogStyle;

		TextView t = (TextView) findViewById(R.id.textView1);
		String s = String.format(context.getString(R.string.info), context.getString(R.string.app_name));
		t.setText(s);

		close = (Button) findViewById(R.id.closeButton);
		close.setOnClickListener(this);
	}

	public void onClick(View v) {
		if (v == close)
			dismiss();
	}

}
