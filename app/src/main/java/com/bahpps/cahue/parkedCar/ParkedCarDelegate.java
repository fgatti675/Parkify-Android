package com.bahpps.cahue.parkedCar;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageButton;

import com.bahpps.cahue.AbstractMarkerDelegate;
import com.bahpps.cahue.CameraUpdateListener;
import com.bahpps.cahue.R;
import com.bahpps.cahue.util.GMapV2Direction;
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
import java.util.Date;
import java.util.List;

/**
 * Created by francesco on 27.10.2014.
 */
public class ParkedCarDelegate extends AbstractMarkerDelegate implements Parcelable {

    private static final String TAG = "ParkedCarDelegate";

    private static final int LIGHT_RED = Color.argb(85, 242, 69, 54);

    private static final int MAX_DIRECTIONS_DISTANCE = 5000;

    private Car car;

    /**
     * Camera mode
     */
    public enum Mode {
        FREE, FOLLOWING
    }

    private Mode mode = Mode.FREE;

    private IconGenerator iconFactory;

    private CameraUpdateListener cameraUpdateListener;
    private GoogleMap mMap;
    private ImageButton carButton;
    private CarSelectedListener carSelectedListener;

    private Location userLocation;

    /**
     * Map components
     */
    private Marker carMarker;
    private Circle accuracyCircle;
    private Polyline directionsPolyline;

    /**
     * Actual lines representing the directions PolyLine
     */
    private List<LatLng> directionPoints;

    private AsyncTask<Object, Object, Document> directionsAsyncTask;

    /**
     * Directions delegate
     */
    private GMapV2Direction directionsDelegate;
    private Context mContext;

    private Date lastDirectionsUpdate;

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
        parcel.writeParcelable(car, 0);
        parcel.writeParcelable(userLocation, 0);
        parcel.writeTypedList(directionPoints);
        parcel.writeSerializable(lastDirectionsUpdate);
    }

    public ParkedCarDelegate() {
        directionPoints = new ArrayList<LatLng>();
    }

    public ParkedCarDelegate(Parcel parcel) {
        mode = (Mode) parcel.readSerializable();
        car = parcel.readParcelable(Car.class.getClassLoader());
        userLocation = parcel.readParcelable(ParkedCarDelegate.class.getClassLoader());
        parcel.readTypedList(directionPoints, LatLng.CREATOR);
        lastDirectionsUpdate = (Date) parcel.readSerializable();
    }

    public void init(Context context, CameraUpdateListener cameraUpdateListener, Car car, GoogleMap map, ImageButton carButton, CarSelectedListener carSelectedListener) {
        mContext = context;
        this.cameraUpdateListener = cameraUpdateListener;
        this.mMap = map;
        this.car = car;
        this.carButton = carButton;
        this.carSelectedListener = carSelectedListener;
        iconFactory = new IconGenerator(context);
        directionsDelegate = new GMapV2Direction();

        buttonUpdate();
    }

    public void setCarLocation(Car car) {
        this.car = car;
        directionPoints.clear();
        fetchDirections(true);
        doDraw();
    }

    public void onLocationChanged(Location userLocation) {
        this.userLocation = userLocation;

        if (lastDirectionsUpdate == null || System.currentTimeMillis() - lastDirectionsUpdate.getTime() > 30000)
            fetchDirections(false);

        updateCameraIfFollowing();

    }

    public void doDraw() {
        Log.i(TAG, "Drawing parked car components");
        clear();
        drawCar();
        drawDirections();
        updateCameraIfFollowing();
    }

    private void clear() {
        if (carMarker != null) carMarker.remove();
        if (accuracyCircle != null) accuracyCircle.remove();
        if (directionsPolyline != null) directionsPolyline.remove();
    }

    /**
     * Displays the car in the map
     */
    private void drawCar() {

        if (car == null || car.location == null) {
            return;
        }

        Log.i(TAG, "Setting car in map: " + car);

        iconFactory.setContentRotation(-90);
        iconFactory.setStyle(IconGenerator.STYLE_RED);

        // Uses a colored icon.
        LatLng carLatLng = getCarLatLng();

        String name = car.name;
        if (name == null)
            name = mContext.getResources().getText(R.string.car).toString();

        carMarker = mMap.addMarker(new MarkerOptions()
                .position(carLatLng)
                .snippet("")
                .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(name.toUpperCase())))
                .anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV()));

        CircleOptions circleOptions = new CircleOptions()
                .center(carLatLng)   //set center
                .radius(car.location.getAccuracy())   //set radius in meters
                .fillColor(LIGHT_RED)
                .strokeColor(LIGHT_RED)
                .strokeWidth(0);

        accuracyCircle = mMap.addCircle(circleOptions);

    }


    public Car getCar() {
        return car;
    }

    public void removeCar() {
        CarLocationManager.removeStoredLocation(mContext, car.id);
        car.location = null;
        clear();
    }

    private void drawDirections() {

        Log.d(TAG, "Drawing directions");

        PolylineOptions rectLine = new PolylineOptions().width(10).color(LIGHT_RED);

        for (int i = 0; i < directionPoints.size(); i++) {
            rectLine.add(directionPoints.get(i));
        }

        directionsPolyline = mMap.addPolyline(rectLine);

    }

    private void fetchDirections(boolean restart) {

        final LatLng carPosition = getCarLatLng();
        final LatLng userPosition = getUserLatLng();

        directionPoints.clear();

        if (carPosition == null || userPosition == null) {
            return;
        }

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
        if (directionsAsyncTask != null && directionsAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            if (restart)
                directionsAsyncTask.cancel(true);
            else
                return;
        }

        Log.d(TAG, "Fetching directions");

        directionsAsyncTask = new AsyncTask<Object, Object, Document>() {

            @Override
            protected Document doInBackground(Object[] objects) {
                Document doc = directionsDelegate.getDocument(userPosition, carPosition, GMapV2Direction.MODE_WALKING);
                return doc;
            }

            @Override
            protected void onPostExecute(Document doc) {
                lastDirectionsUpdate = new Date();
                directionPoints.clear();
                directionPoints.addAll(directionsDelegate.getDirection(doc));
                doDraw();
            }

        };
        directionsAsyncTask.execute();
    }

    private LatLng getCarLatLng() {
        if (car.location == null) return null;
        return new LatLng(car.location.getLatitude(), car.location.getLongitude());
    }

    public void changeMode() {
        if (mode == null || mode == Mode.FREE) setMode(Mode.FOLLOWING);
        else if (mode == Mode.FOLLOWING) setMode(Mode.FREE);
    }

    public void setFollowing() {
        setMode(Mode.FOLLOWING);
    }

    private void setMode(Mode mode) {

        if (this.mode != mode)
            Log.i(TAG, "Setting camera mode to " + mode);

        this.mode = mode;

        updateCameraIfFollowing();

        buttonUpdate();

    }

    private void buttonUpdate() {
        if (carButton == null)
            return;
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

        Log.v(TAG, "zoomToSeeBoth");

        LatLng carPosition = getCarLatLng();
        LatLng userPosition = getUserLatLng();

        if (carPosition == null || userPosition == null) return false;

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(carPosition)
                .include(userPosition);

        for (LatLng latLng : directionPoints)
            builder.include(latLng);

        cameraUpdateListener.onCameraUpdateRequest(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));

        return true;
    }

    /**
     * This method zooms to the car's location.
     */
    private void zoomToCar() {

        Log.d(TAG, "zoomToCar");

        if (car == null) return;

        LatLng loc = getCarLatLng();

        if (loc != null) {
            cameraUpdateListener.onCameraUpdateRequest(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(loc)
                    .zoom(15.5f)
                    .build()));
        }
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition, boolean justFinishedAnimating) {
        if (!justFinishedAnimating)
            setMode(Mode.FREE);
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.equals(carMarker)) {
            carSelectedListener.onCarClicked(car);
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public interface CarSelectedListener {
        public void onCarClicked(Car car);
    }

}
