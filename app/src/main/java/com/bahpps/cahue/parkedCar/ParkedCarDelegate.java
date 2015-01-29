package com.bahpps.cahue.parkedCar;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.bahpps.cahue.AbstractMarkerDelegate;
import com.bahpps.cahue.CameraUpdateListener;
import com.bahpps.cahue.R;
import com.bahpps.cahue.cars.Car;
import com.bahpps.cahue.cars.CarDatabase;
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
    private static final int DIRECTIONS_EXPIRY = 30000;

    private Car car;

    // too far from the car to calculate directions
    private boolean tooFar = false;

    private boolean following = false;

    private IconGenerator iconGenerator;

    private CameraUpdateListener cameraUpdateListener;
    private GoogleMap mMap;
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
        super.writeToParcel(parcel, i);
        parcel.writeByte((byte) (following ? 1 : 0));
        parcel.writeByte((byte) (tooFar ? 1 : 0));
        parcel.writeParcelable(car, i);
        parcel.writeParcelable(userLocation, i);
        parcel.writeTypedList(directionPoints);
        parcel.writeSerializable(lastDirectionsUpdate);
    }

    public ParkedCarDelegate(Car car) {
        this.car = car;
        directionPoints = new ArrayList<LatLng>();
    }

    public ParkedCarDelegate(Parcel parcel) {
        super(parcel);
        following = parcel.readByte() > 0;
        tooFar = parcel.readByte() > 0;
        car = parcel.readParcelable(Car.class.getClassLoader());
        userLocation = parcel.readParcelable(ParkedCarDelegate.class.getClassLoader());
        directionPoints = new ArrayList();
        parcel.readTypedList(directionPoints, LatLng.CREATOR);
        lastDirectionsUpdate = (Date) parcel.readSerializable();
    }

    public void init(Context context, CameraUpdateListener cameraUpdateListener, CarSelectedListener carSelectedListener) {

        mContext = context;

        this.cameraUpdateListener = cameraUpdateListener;
        this.carSelectedListener = carSelectedListener;
        iconGenerator = new IconGenerator(context);
        directionsDelegate = new GMapV2Direction();

        updateCarLocation();

    }

    public void updateCarLocation() {
        directionPoints.clear();

        // TODO: not necessary to create a new instance of this
        CarDatabase carDatabase = new CarDatabase(mContext);

        this.car = carDatabase.findByBTAddress(this.car.btAddress);

        if (car == null || car.location == null) {
            return;
        }
        fetchDirections(true);
        doDraw();
    }

    @Override
    public void onMapReady(GoogleMap map) {

        this.mMap = map;

        updateCarLocation();
    }

    public void onLocationChanged(Location userLocation) {
        this.userLocation = userLocation;

        if (lastDirectionsUpdate == null || System.currentTimeMillis() - lastDirectionsUpdate.getTime() > DIRECTIONS_EXPIRY)
            fetchDirections(false);

        updateCameraIfFollowing();

    }

    public void doDraw() {
        if(mMap == null) return;
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

    @Override
    public void onZoomToMyLocation() {
        setFollowing(false);
    }

    /**
     * Displays the car in the map
     */
    private void drawCar() {

        if (car == null || car.location == null) {
            return;
        }

        Log.i(TAG, "Setting car in map: " + car);

        iconGenerator.setContentRotation(-90);
        iconGenerator.setColor(IconGenerator.STYLE_RED);

        iconGenerator.setColor(0xff0099cc); // TODO: check color can be changed like this

        // Uses a colored icon.
        LatLng carLatLng = getCarLatLng();

        String name = car.name;
        if (name == null)
            name = mContext.getResources().getText(R.string.car).toString();

        carMarker = mMap.addMarker(new MarkerOptions()
                .position(carLatLng)
                .snippet("")
                .icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon(name.toUpperCase())))
                .anchor(iconGenerator.getAnchorU(), iconGenerator.getAnchorV()));

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

        // TODO: not necessary to create a new instance of this
        CarDatabase carDatabase = new CarDatabase(mContext);
        carDatabase.removeStoredLocation(car);
        car.location = null;
        clear();
    }

    private void drawDirections() {

        if (car == null || car.location == null) {
            return;
        }

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

        updateTooFar(carPosition, userPosition);

        // don't set if they are too far
        if (isTooFar()) return;

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

    private void updateTooFar(LatLng carPosition, LatLng userPosition) {
        float distances[] = new float[3];
        Location.distanceBetween(
                carPosition.latitude,
                carPosition.longitude,
                userPosition.latitude,
                userPosition.longitude,
                distances);

        tooFar = distances[0] > MAX_DIRECTIONS_DISTANCE;
    }

    private LatLng getCarLatLng() {
        if (car.location == null) return null;
        return new LatLng(car.location.getLatitude(), car.location.getLongitude());
    }


    public void setFollowing(boolean following) {

        if (this.following != following)
            Log.i(TAG, "Setting camera following mode to " + following);

        this.following = following;

        updateCameraIfFollowing();

    }


    private LatLng getUserLatLng() {
        if (userLocation == null) return null;
        return new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
    }


    public boolean updateCameraIfFollowing() {

        if (following) {
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

        cameraUpdateListener.onCameraUpdateRequest(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

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
            following = false;
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.equals(carMarker)) {
            carSelectedListener.onCarClicked(car);
        } else {
            setFollowing(false);
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isFollowing() {
        return following;
    }

    public boolean isTooFar() {
        return tooFar;
    }

    public interface CarSelectedListener {
        public void onCarClicked(Car car);
    }

}
