package com.whereismycar.locationservices;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.whereismycar.Constants;

import static com.whereismycar.Constants.EXTRA_CAR_ID;

/**
 * Meant to start the {@link GeofenceCarService} some time after parking the car
 */
public class DelayedGeofenceJob extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        Intent intent = new Intent(this, GeofenceCarService.class);
        intent.putExtra(EXTRA_CAR_ID, jobParameters.getExtras().getString(Constants.EXTRA_CAR_ID));
        ContextCompat.startForegroundService(this, intent);

        jobFinished(jobParameters, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
