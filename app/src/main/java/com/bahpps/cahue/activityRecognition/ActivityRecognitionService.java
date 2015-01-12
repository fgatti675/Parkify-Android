package com.bahpps.cahue.activityRecognition;

import com.bahpps.cahue.R;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class ActivityRecognitionService extends IntentService	 {

    int id = 0;

	private String TAG = this.getClass().getSimpleName();
	public ActivityRecognitionService() {
		super("My Activity Recognition Service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if(ActivityRecognitionResult.hasResult(intent)){
			ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            String type = getType(result.getMostProbableActivity().getType());
            Log.i(TAG, type +"\t" + result.getMostProbableActivity().getConfidence());
			Intent i = new Intent("com.kpbird.myactivityrecognition.ACTIVITY_RECOGNITION_DATA");
			i.putExtra("Activity", type);
			i.putExtra("Confidence", result.getMostProbableActivity().getConfidence());
			sendBroadcast(i);

            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_navigation_cancel)
                            .setContentTitle(type)
                            .setContentText(result.getMostProbableActivity().getConfidence() + "");

            mNotifyMgr.notify("MY TAG" , id, mBuilder.build());
            id++;
		}
	}
	
	private String getType(int type){
		if(type == DetectedActivity.UNKNOWN)
			return "Unknown";
		else if(type == DetectedActivity.IN_VEHICLE)
			return "In Vehicle";
		else if(type == DetectedActivity.ON_BICYCLE)
			return "On Bicycle";
		else if(type == DetectedActivity.ON_FOOT)
			return "On Foot";
		else if(type == DetectedActivity.STILL)
			return "Still";
		else if(type == DetectedActivity.TILTING)
			return "Tilting";
		else
			return "";
	}

}
