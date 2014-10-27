package com.bahpps.cahue;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bahpps.cahue.util.CarLocationManager;
import com.bahpps.cahue.util.GMapV2Direction;
import com.bahpps.cahue.util.Util;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by francesco on 27.10.2014.
 */
public class ParkedCarDelegate implements Parcelable {

    private static final String TAG = "ParkedCarDelegate";

    private static final int LIGHT_RED = Color.argb(85, 242, 69, 54);

    private static final int MAX_DIRECTIONS_DISTANCE = 5000;

    /**
     * Camera mode
     */
    private enum Mode {
        FREE, FOLLOWING
    }

    private Mode mode;

    private IconGenerator iconFactory;

    private GoogleMap mMap;
    private ImageButton carButton;

    private Location carLocation;
    private Location userLocation;
    private Marker carMarker;

    /**
     * Actual lines representing the directions PolyLine
     */
    private Polyline directionsPolyLine;
    private List<LatLng> directionPoints;
    private boolean directionsDisplayed = false;

    private AsyncTask<Object, Object, Document> directionsAsyncTask;

    /**
     * Directions delegate
     */
    private GMapV2Direction directionsDelegate;
    private Context mContext;

    private boolean justFinishedAnimating = false;

    public static final Parcelable.Creator<ParkedCarDelegate> CREATOR =
            new Parcelable.Creator<ParkedCarDelegate>() {
                @Override
                public ParkedCarDelegate createFromParcel(Parcel parcel) {
                    return new ParkedCarDelegate(parcel);
                }

                @Override
                public ParkedCarDelegate[] newArray(int size) {
                    return new ParkedCarDelegate[size];
                }
            };

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(mode);
        parcel.writeParcelable(carLocation, 0);
        parcel.writeParcelable(userLocation, 0);

        LatLng[] directionsArray = new LatLng[directionPoints.size()];
        parcel.writeParcelableArray(directionPoints.toArray(directionsArray), 0);
    }

    public ParkedCarDelegate() {
        directionPoints = new ArrayList<LatLng>();
    }

    public ParkedCarDelegate(Parcel parcel) {
        mode = (Mode) parcel.readSerializable();
        carLocation = parcel.readParcelable(ParkedCarDelegate.class.getClassLoader());
        userLocation = parcel.readParcelable(ParkedCarDelegate.class.getClassLoader());

        LatLng[] directionsArray = (LatLng[]) parcel.readParcelableArray(ParkedCarDelegate.class.getClassLoader());
        directionPoints = Arrays.asList(directionsArray);
    }

    public void init(Context context, GoogleMap map, ImageButton carButton) {
        mContext = context;
        this.mMap = map;
        this.carButton = carButton;
        iconFactory = new IconGenerator(context);
        directionsDelegate = new GMapV2Direction();
    }

    public void setCarLocationIfNull(Location carLocation) {
        if (this.carLocation == null)
            setCarLocation(carLocation);
    }

    public void setCarLocation(Location carLocation) {
        this.carLocation = carLocation;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;

        if (!directionsDisplayed)
            drawDirections();
    }

    public void draw(){
        drawCar();
        drawDirections();
    }

    /**
     * Displays the car in the map
     */
    private void drawCar() {

        if (carLocation == null) {
            return;
        } else {
            if (carMarker != null) carMarker.remove();
            if (directionsPolyLine != null) directionsPolyLine.remove();
        }

        Log.i(TAG, "Setting car in map: " + carLocation);

        iconFactory.setContentRotation(-90);
        iconFactory.setStyle(IconGenerator.STYLE_RED);

        // Uses a colored icon.
        LatLng carLatLng = getCarLatLng();

        carMarker = mMap.addMarker(new MarkerOptions()
                .position(carLatLng)
                .snippet("")
                .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(mContext.getResources().getText(R.string.car).toString().toUpperCase())))
                .anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV()));

        CircleOptions circleOptions = new CircleOptions()
                .center(carLatLng)   //set center
                .radius(carLocation.getAccuracy())   //set radius in meters
                .fillColor(LIGHT_RED)
                .strokeColor(LIGHT_RED)
                .strokeWidth(0);

        Circle accuracyCircle = mMap.addCircle(circleOptions);


    }

    public void removeCar() {
        carLocation = null;
        CarLocationManager.removeStoredLocation(mContext);
        drawCar();
    }

    private void drawDirections() {

        final LatLng carPosition = getCarLatLng();
        final LatLng userPosition = getUserLatLng();

        if (directionsPolyLine != null) {
            directionsPolyLine.remove();
            directionsDisplayed = false;
        }

        if (carPosition == null || userPosition == null) {
            return;
        }

        Log.i(TAG, "Drawing directions");

        // don't set if they are too far
        float distances[] = new float[3];
        Location.distanceBetween(
                carPosition.latitude,
                carPosition.longitude,
                userPosition.latitude,
                userPosition.longitude,
                distances);
        if (distances[0] > MAX_DIRECTIONS_DISTANCE) {
            return;
        }

        /**
         * Cancel if something is going on
         */
        if(directionsAsyncTask != null && directionsAsyncTask.getStatus() != AsyncTask.Status.FINISHED)
            directionsAsyncTask.cancel(true);

        directionsAsyncTask = new AsyncTask<Object, Object, Document>() {

            @Override
            protected Document doInBackground(Object[] objects) {
                Document doc = directionsDelegate.getDocument(userPosition, carPosition, GMapV2Direction.MODE_WALKING);
                return doc;
            }

            @Override
            protected void onPostExecute(Document doc) {
                directionPoints = directionsDelegate.getDirection(doc);
                PolylineOptions rectLine = new PolylineOptions().width(10).color(LIGHT_RED);

                for (int i = 0; i < directionPoints.size(); i++) {
                    rectLine.add(directionPoints.get(i));
                }
                directionsPolyLine = mMap.addPolyline(rectLine);
                directionsDisplayed = true;

                updateCameraIfFollowing();
            }

        };
        directionsAsyncTask.execute();

    }

    private LatLng getCarLatLng() {
        if (carLocation == null) return null;
        return new LatLng(carLocation.getLatitude(), carLocation.getLongitude());
    }

    /**
     * This method shows the Toast when the car icon is pressed, telling the user the parking time
     */
    private void showCarTimeToast() {
        String toastMsg = mContext.getString(R.string.car_was_here);

        long timeDiff = Calendar.getInstance().getTimeInMillis() - CarLocationManager.getParkingTime(mContext);

        String time = "";

        long seconds = timeDiff / 1000;
        if (seconds < 60) {
            time = seconds + " " + mContext.getString(R.string.seconds);
        } else {
            long minutes = timeDiff / (60 * 1000);
            if (minutes < 60) {
                time = minutes
                        + (minutes > 1 ? " " + mContext.getString(R.string.minutes) : " "
                        + mContext.getString(R.string.minute));
            } else {
                long hours = timeDiff / (60 * 60 * 1000);
                if (hours < 24) {
                    time = hours
                            + (hours > 1 ? " " + mContext.getString(R.string.hours) : " "
                            + mContext.getString(R.string.hour));
                } else {
                    long days = timeDiff / (24 * 60 * 60 * 1000);
                    time = days
                            + (days > 1 ? " " + mContext.getString(R.string.days) : " "
                            + mContext.getString(R.string.day));
                }
            }
        }

        toastMsg = String.format(toastMsg, time);

        Util.createToast(mContext, toastMsg, Toast.LENGTH_SHORT);

    }


    public void changeMode() {
        if (mode == null || mode == Mode.FREE) setMode(Mode.FOLLOWING);
        else if (mode == Mode.FOLLOWING) setMode(Mode.FREE);
    }

    private void setMode(Mode mode) {

        Log.i(TAG, "Setting mode to " + mode);

        this.mode = mode;

        updateCameraIfFollowing();

        if (mode == Mode.FOLLOWING) {
            carButton.setImageResource(R.drawable.ic_icon_car_red);
        } else if (mode == Mode.FREE) {
            carButton.setImageResource(R.drawable.ic_icon_car);
        }

    }


    private LatLng getUserLatLng() {
        if (userLocation == null) return null;
        return new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
    }


    public boolean updateCameraIfFollowing() {

        if (mode == Mode.FOLLOWING) {
            return zoomToSeeBoth();
        }
        return false;
    }

    /**
     * This method zooms to see both user and the car.
     */
    protected boolean zoomToSeeBoth() {

        LatLng carPosition = getCarLatLng();
        LatLng userPosition = getUserLatLng();

        if (carPosition == null || userPosition == null) return false;

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(carPosition)
                .include(userPosition);

        if (directionsPolyLine != null) {
            for (LatLng latLng : directionsPolyLine.getPoints())
                builder.include(latLng);
        }


        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150), new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                justFinishedAnimating = true;
            }

            @Override
            public void onCancel() {
            }
        });
        return true;
    }

    /**
     * This method zooms to the car's location.
     */
    private void zoomToCar() {

        Log.d(TAG, "zoomToCar");

        if (carLocation == null) return;

        LatLng loc = getCarLatLng();

        if (loc != null) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(loc)
                    .zoom(15.5f)
                    .build()), null);

            showCarTimeToast();
        }
    }

    private void zoomToMyLocation() {

        Log.d(TAG, "zoomToMyLocation");

        LatLng userPosition = getUserLatLng();
        if (userPosition == null) return;
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .target(userPosition)
                        .zoom(15.5f)
                        .build()),
                new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        justFinishedAnimating = true;
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    public void onCameraChange(CameraPosition cameraPosition) {
        if (!justFinishedAnimating) setMode(Mode.FREE);
        justFinishedAnimating = false;
    }

    public void setModeFree() {
        setMode(Mode.FREE);
    }

    public boolean onMarkerClick(Marker marker) {
        if (marker.equals(carMarker)) {
            showCarTimeToast();
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
