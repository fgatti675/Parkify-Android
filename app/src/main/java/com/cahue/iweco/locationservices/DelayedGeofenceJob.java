package com.cahue.iweco.locationservices;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Bundle;

import com.cahue.iweco.Constants;

/**
 * Meant to start the {@link GeofenceCarReceiver} some time after parking the car
 */
public class DelayedGeofenceJob extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        LocationUpdatesHelper helper = new LocationUpdatesHelper(this, GeofenceCarReceiver.ACTION);
        Bundle extras = new Bundle();
        extras.putString(Constants.EXTRA_CAR_ID, jobParameters.getExtras().getString(Constants.EXTRA_CAR_ID));
        helper.startLocationUpdates(extras);

        jobFinished(jobParameters, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
