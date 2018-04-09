package com.cahue.iweco.cars.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.model.PossibleSpot;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cahue.iweco.model.PossibleSpot.NOT_SO_RECENT;
import static com.cahue.iweco.model.PossibleSpot.RECENT;

/**
 * Created by Francesco on 22/01/2015.
 */
public class CarDatabase {


    public interface CarUpdateListener {
        void onCarUpdated(Car car);

        void onCarUpdateError();
    }

    public interface CarsRetrieveListener {
        void onCarsRetrieved(List<Car> cars);

        void onCarsRetrievedError();
    }

    public static final String TABLE_CARS = "cars";
    public static final String TABLE_POSSIBLE_SPOTS = "possible_spots";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_FIRESTORE_ID = "firestore_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_BT_ADDRESS = "bt_address";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ACCURACY = "accuracy";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_COLOR = "color";

    public static final String[] CAR_PROJECTION = new String[]{
            COLUMN_ID,
            COLUMN_FIRESTORE_ID,
            COLUMN_NAME,
            COLUMN_BT_ADDRESS,
            COLUMN_COLOR,
            COLUMN_LATITUDE,
            COLUMN_LONGITUDE,
            COLUMN_ACCURACY,
            COLUMN_TIME,
            COLUMN_ADDRESS,
    };

    private static final String[] SPOT_PROJECTION = new String[]{
            COLUMN_LATITUDE,
            COLUMN_LONGITUDE,
            COLUMN_ACCURACY,
            COLUMN_TIME,
            COLUMN_ADDRESS
    };
    private static final int MAX_POSSIBLE_SPOTS = 5;
    private static final String TAG = CarDatabase.class.getSimpleName();
    private static CarDatabase mInstance;

    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public static CarDatabase getInstance() {
        if (mInstance == null) {
            mInstance = new CarDatabase();
        }
        return mInstance;
    }


    /**
     * Update the spot ID of a parked car
     *
     * @param carId
     * @param address
     */
    public void updateAddress(@NonNull String carId, @NonNull String address) {

        Map<String, Object> fsCar = new HashMap<>();
        fsCar.put("parked_at.address", address);
        firestore
                .collection("cars")
                .document(carId)
                .update(fsCar)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Car address updated");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Migration error", e);
                });

    }

    /**
     * Persist date about a car, location included
     *
     * @param car
     */
    public void updateCar(@NonNull Car car, CarUpdateListener listener) {

        Map<String, Object> fsCar = car.toFireStoreMap(FirebaseAuth.getInstance().getCurrentUser().getUid());
        firestore
                .collection("cars")
                .document(car.id)
                .update(fsCar)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Car saved");
                    if (listener != null)
                        listener.onCarUpdated(car);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Migration error", e);
                    if (listener != null)
                        listener.onCarUpdateError();
                });

    }


    /**
     * Persist car location
     */
    public void updateCarLocation(String carId, Location location, String address, Date time, String source) {
        updateCarLocation(carId, location, address, time, source, null);
    }

    public void updateCarLocation(String carId, Location location, String address, Date time, String source, CarUpdateListener carUpdateListener) {

        Map<String, Object> fsCar = new HashMap<>();
        fsCar.put("parked_at", Car.toFirestoreParkingEvent(location, time, address, source));
        firestore
                .collection("cars")
                .document(carId)
                .update(fsCar)
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("cars").document(carId).get().addOnCompleteListener(o -> {
                        if (o.isSuccessful()) {
                            Car car = Car.fromFirestore(o.getResult());
                            if (carUpdateListener != null)
                                carUpdateListener.onCarUpdated(car);
                        } else {
                            if (carUpdateListener != null)
                                carUpdateListener.onCarUpdateError();
                        }

                    });
                    Log.d(TAG, "Car address updated : " + aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Car address error", e);
                    if (carUpdateListener != null)
                        carUpdateListener.onCarUpdateError();
                });

    }

    /**
     * Persist date about a car, location included
     *
     * @param car
     */
    public void createCar(@NonNull Car car, CarUpdateListener listener) {

        Map<String, Object> fsCar = car.toFireStoreMap(FirebaseAuth.getInstance().getCurrentUser().getUid());
        firestore
                .collection("cars")
                .add(fsCar)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Car saved");
                    car.id = documentReference.getId();
                    if (listener != null)
                        listener.onCarUpdated(car);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Migration error", e);
                    if (listener != null)
                        listener.onCarUpdateError();
                });


    }

    /**
     * Update a car location from a possible AR spot
     */
    public void updateCarFromArSpot(Context context, String carId, @NonNull PossibleSpot spot) {

        Log.i(TAG, "Updating car " + carId + " " + spot);

        updateCarLocation(carId, spot.location, spot.address, spot.time, "ar_possible_spot");

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            database.delete(TABLE_POSSIBLE_SPOTS, COLUMN_TIME + " = '" + spot.time.getTime() + "'", null);
        } finally {
            database.close();
            carDatabaseHelper.close();
        }

    }


    public void retrieveCars(CarsRetrieveListener listener) {
        firestore.collection("cars").whereEqualTo("owner", FirebaseAuth.getInstance().getCurrentUser().getUid()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                    List<Car> cars = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : documents) {
                        Car car = Car.fromFirestore(documentSnapshot);
                        cars.add(car);
                    }
                    listener.onCarsRetrieved(cars);

                })
                .addOnFailureListener(f -> listener.onCarsRetrievedError());
    }


    public void removeCarLocation(String carId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("parked_at", FieldValue.delete());
        firestore.collection("cars").document(carId).update(updates);

//        final DocumentReference carDocRef = firestore.collection("cars").document(carId);
//        firestore.runTransaction(new Transaction.Function<Double>() {
//            @Override
//            public Double apply(Transaction transaction) throws FirebaseFirestoreException {
//                DocumentSnapshot snapshot = transaction.get(carDocRef);
//                HashMap<String, Object> currentlyParkedAt = (HashMap<String, Object>) snapshot.get("parked_at");
//                if (newPopulation <= 1000000) {
//                    transaction.update(carDocRef, "parked_at", newPopulation);
//                    return newPopulation;
//                } else {
//                    throw new FirebaseFirestoreException("Population too high",
//                            FirebaseFirestoreException.Code.ABORTED);
//                }
//            }
//        }).addOnSuccessListener(new OnSuccessListener<Double>() {
//            @Override
//            public void onSuccess(Double result) {
//                Log.d(TAG, "Transaction success: " + result);
//            }
//        })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.w(TAG, "Transaction failure.", e);
//                    }
//                });

    }


    public void deleteCar(Context context, @NonNull Car car) {

        firestore
                .collection("cars")
                .document(car.id)
                .delete();

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();
        try {
            database.delete(TABLE_CARS, "id = '" + car.legacy_id + "'", null);
        } finally {
            database.close();
            carDatabaseHelper.close();
        }
    }

    public void addPossibleParkingSpot(Context context, @NonNull PossibleSpot spot) {
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            ContentValues values = createSpotContentValues(spot);

            Cursor cursor = database.query(TABLE_POSSIBLE_SPOTS,
                    SPOT_PROJECTION,
                    null,
                    null, null, null,
                    COLUMN_TIME + " DESC");

            List<ParkingSpot> previousSpots = new ArrayList<>();
            while (cursor.moveToNext()) {
                previousSpots.add(cursorToSpot(cursor));
            }
            cursor.close();

            database.insertWithOnConflict(TABLE_POSSIBLE_SPOTS, COLUMN_TIME, values, SQLiteDatabase.CONFLICT_REPLACE);

            // remove spots that are too close
            for (ParkingSpot prevSpot : previousSpots) {
                float distances[] = new float[3];
                Location.distanceBetween(
                        spot.getLatLng().latitude,
                        spot.getLatLng().longitude,
                        prevSpot.getLatLng().latitude,
                        prevSpot.getLatLng().longitude,
                        distances);

                if (distances[0] < 25)
                    database.delete(TABLE_POSSIBLE_SPOTS, COLUMN_TIME + " = '" + prevSpot.getTime().getTime() + "'", null);
            }

            // remove stale spots
            if (previousSpots.size() > MAX_POSSIBLE_SPOTS - 1) {
                for (ParkingSpot prevSpot : previousSpots.subList(MAX_POSSIBLE_SPOTS - 1, previousSpots.size())) {
                    database.delete(TABLE_POSSIBLE_SPOTS, COLUMN_TIME + " = '" + prevSpot.getTime().getTime() + "'", null);
                }
            }

        } finally {
            database.close();
            carDatabaseHelper.close();
        }
    }


    /**
     * Create ContentValues from Car
     *
     * @param spot
     * @return
     */
    @NonNull
    private ContentValues createSpotContentValues(@NonNull PossibleSpot spot) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, spot.location.getLatitude());
        values.put(COLUMN_LONGITUDE, spot.location.getLongitude());
        values.put(COLUMN_ACCURACY, spot.location.getAccuracy());
        values.put(COLUMN_TIME, spot.time != null ? spot.time.getTime() : null);
        values.put(COLUMN_ADDRESS, spot.address);

        return values;
    }

    /**
     * Get a Collection of possible parking spots
     *
     * @return
     */
    @NonNull
    public List<PossibleSpot> retrievePossibleParkingSpots(Context context) {
        List<PossibleSpot> spots = new ArrayList<>();

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {

            Cursor cursor = database.query(TABLE_POSSIBLE_SPOTS,
                    SPOT_PROJECTION,
                    null,
                    null, null, null,
                    COLUMN_TIME + " DESC");

            int i = 0;
            while (cursor.moveToNext()) {
                spots.add(cursorToPossibleSpot(cursor, i++));
            }
            cursor.close();

        } finally {
            database.close();
            carDatabaseHelper.close();
        }

        Log.d(TAG, "Retrieved cars from DB: " + spots);

        return spots;
    }

    /**
     * Persist date about a car, location included
     *
     * @param spot
     */
    public void removeParkingSpot(Context context, @NonNull PossibleSpot spot) {

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            database.delete(TABLE_POSSIBLE_SPOTS, COLUMN_TIME + " = '" + spot.time.getTime() + "'", null);
        } finally {
            database.close();
            carDatabaseHelper.close();
        }

    }

    @NonNull
    private ParkingSpot cursorToSpot(@NonNull Cursor cursor) {

        double latitude = cursor.getDouble(0);
        double longitude = cursor.getDouble(1);
        float accuracy = cursor.getFloat(2);
        Location location = new Location("db");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        Date time = new Date(cursor.getLong(3));
        String address = cursor.isNull(4) ? null : cursor.getString(4);

        return new ParkingSpot(null, location, address, time, false);
    }

    @NonNull
    private PossibleSpot cursorToPossibleSpot(@NonNull Cursor cursor, int order) {

        double latitude = cursor.getDouble(0);
        double longitude = cursor.getDouble(1);
        float accuracy = cursor.getFloat(2);
        Location location = new Location("db");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        Date time = new Date(cursor.getLong(3));
        String address = cursor.isNull(4) ? null : cursor.getString(4);

        return new PossibleSpot(location, address, time, false, order < 2 ? RECENT : NOT_SO_RECENT);
    }


}