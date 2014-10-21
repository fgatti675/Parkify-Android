package com.bahpps.cahue;

import android.test.ActivityTestCase;
import android.util.Log;

import com.bahpps.cahue.spots.ParkingSpot;
import com.bahpps.cahue.spots.ParkingSpotsService;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ActivityTestCase {


    /**
     * This demonstrates how to test AsyncTasks in android JUnit. Below I used
     * an in line implementation of a asyncTask, but in real life you would want
     * to replace that with some task in your application.
     *
     * @throws Throwable
     */
    public void testSomeAsyncTask() throws Throwable {

        // create  a signal to let us know when our task is done.
        final CountDownLatch signal = new CountDownLatch(1);

        Log.i("Test", "Testing fusion tables query");

//        LatLngBounds latLngBounds = LatLngBounds.builder() // Alcorcon
//                .include(new LatLng(40.350358, -3.821536))
//                .include(new LatLng(40.346335, -3.817309))
//                .build();

        LatLngBounds latLngBounds = LatLngBounds.builder() // Munich
                .include(new LatLng(48.132219,11.561716))
                .include(new LatLng(48.123913,11.553905))
                .build();

        final ParkingSpotsService parkingSpotsService = new ParkingSpotsService(latLngBounds, new ParkingSpotsService.ParkingSpotsUpdateListener() {
            @Override
            public void onLocationsUpdate(List<ParkingSpot> parkingSpots) {
                signal.countDown();
            }
        });

        // Execute the async task on the UI thread! THIS IS KEY!
        runTestOnUiThread(new Runnable() {

            @Override
            public void run() {
                parkingSpotsService.execute();
            }
        });

    /* The testing thread will wait here until the UI thread releases it
     * above with the countDown() or 30 seconds passes and it times out.
     */
        signal.await(60, TimeUnit.SECONDS);

        // The task is done, and now you can assert some things!
        assertTrue("Happiness", true);
    }
}