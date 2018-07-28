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
        LocationUpdatesService.startLocationUpdate(this,
                GeofenceCarReceiver.ACTION,
                jobParameters.getExtras().getString(Constants.EXTRA_CAR_ID));

        jobFinished(jobParameters, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
