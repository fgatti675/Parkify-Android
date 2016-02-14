package com.cahue.iweco;

import android.location.Location;
import android.support.annotation.NonNull;
import android.test.ActivityTestCase;
import android.util.Log;

import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.spots.query.ParkingSpotsQuery;
import com.cahue.iweco.spots.query.QueryResult;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class SpotsQueryTest extends ActivityTestCase {

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

        List<ParkingSpot> expectedResult = new ArrayList<ParkingSpot>() {
            {
                Location location = new Location("");
                location.setLatitude(48.129830);
                location.setLongitude(11.559060);
                location.setAccuracy(10);
                add(new ParkingSpot(
                        6245697686863872L,
                        location,
                        null,
                        new Date(1413928884000L),
                        false));
            }
        };

        final LatLngBounds latLngBounds = LatLngBounds.builder() // Munich
                .include(new LatLng(48.132219, 11.561716))
                .include(new LatLng(48.123913, 11.553905))
                .build();

        final List<ParkingSpot> spots = new ArrayList<ParkingSpot>();

        final ParkingSpotsQuery parkingSpotsQuery = new TestSpotsQuery(
                getActivity(),
                latLngBounds,
                new ParkingSpotsQuery.ParkingSpotsUpdateListener() {

                    @Override
                    public void onSpotsUpdate(ParkingSpotsQuery query, @NonNull QueryResult result) {
                        spots.addAll(result.spots);
                        signal.countDown();
                    }

                    @Override
                    public void onServerError(ParkingSpotsQuery query, int statusCode, String reasonPhrase) {

                    }
                });

        // Execute the async task on the UI thread! THIS IS KEY!
        runTestOnUiThread(new Runnable() {

            @Override
            public void run() {
                parkingSpotsQuery.execute();
            }
        });

        /**
         *  The testing thread will wait here until the UI thread releases it
         * above with the countDown() or 30 seconds passes and it times out.
         */
        signal.await(60, TimeUnit.SECONDS);

        // The task is done, and now you can assert some things!
        assertEquals(expectedResult, spots);
    }

}