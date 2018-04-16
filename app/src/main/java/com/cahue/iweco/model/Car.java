package com.cahue.iweco.model;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cahue.iweco.util.Util;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by francesco on 28.11.2014.
 */
public class Car implements Parcelable {

    public final static String OTHER_ID = "OTHER";

    public static final Parcelable.Creator<Car> CREATOR =
            new Parcelable.Creator<Car>() {
                @NonNull
                @Override
                public Car createFromParcel(@NonNull Parcel parcel) {
                    return new Car(parcel);
                }

                @NonNull
                @Override
                public Car[] newArray(int size) {
                    return new Car[size];
                }
            };

    public String id;
    public String legacy_id;
    public String name;
    public String btAddress;
    @Nullable
    public Location location;
    @Nullable
    public Date time;
    @Nullable
    public Integer color;
    @Nullable
    public String address;


    private Car(@NonNull Parcel parcel) {
        id = parcel.readString();
        legacy_id = parcel.readString();
        name = parcel.readString();
        btAddress = parcel.readString();
        location = parcel.readParcelable(Location.class.getClassLoader());
        time = (Date) parcel.readSerializable();
        color = (Integer) parcel.readValue(Integer.class.getClassLoader());
        address = parcel.readString();
    }

    public Car() {

    }

    @NonNull
    public static List<Car> fromJSONArray(@NonNull JSONArray carsArray) {
        try {
            List<Car> cars = new ArrayList<>();
            for (int i = 0; i < carsArray.length(); i++) {

                JSONObject carJSON = carsArray.getJSONObject(i);

                Car car = Car.fromJSON(carJSON);
                cars.add(car);
            }
            return cars;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static Car fromJSON(@NonNull JSONObject carJSON) {
        try {
            Car car = new Car();

            car.legacy_id = carJSON.getString("id");

            if (carJSON.has("name")) {
                car.name = carJSON.getString("name");
            }

            if (carJSON.has("btAddress")) {
                car.btAddress = carJSON.getString("btAddress");
            }

            if (carJSON.has("color")) {
                car.color = carJSON.getInt("color");
            }

            if (carJSON.has("latitude") && carJSON.has("longitude")) {
                Location location = new Location("JSON");
                location.setLatitude(carJSON.getDouble("latitude"));
                location.setLongitude(carJSON.getDouble("longitude"));
                location.setAccuracy((float) carJSON.getDouble("accuracy"));
                car.location = location;
                if (carJSON.has("time")) {
                    car.time = Util.DATE_FORMAT.parse(carJSON.getString("time"));
                }
            }
            return car;
        } catch (@NonNull JSONException | ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(legacy_id);
        parcel.writeString(name);
        parcel.writeString(btAddress);
        parcel.writeParcelable(location, i);
        parcel.writeSerializable(time);
        parcel.writeValue(color);
        parcel.writeString(address);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Car car = (Car) o;

        return id.equals(car.id);

    }
//
//    @Override
//    public int hashCode() {
//        return legacy_id.hashCode();
//    }

    @NonNull
    @Override
    public String toString() {
        return "Car{" +
                "id='" + id + '\'' +
                ", btAddress='" + btAddress + '\'' +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", time=" + time +
                '}';
    }

    @NonNull
    @Deprecated
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", legacy_id);
            jsonObject.put("name", name);
            if (btAddress != null) jsonObject.put("btAddress", btAddress);
            if (color != null) jsonObject.put("color", color);
            if (location != null) {
                jsonObject.put("latitude", location.getLatitude());
                jsonObject.put("longitude", location.getLongitude());
                jsonObject.put("accuracy", location.getAccuracy());
            }
            if (time != null)
                jsonObject.put("time", Util.DATE_FORMAT.format(time));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public boolean isOther() {
        return legacy_id.equals(OTHER_ID);
    }


    public Map<String, Object> toFireStoreMap(String ownerId) {

        HashMap<String, Object> map = new HashMap<>();

        map.put("legacy_id", legacy_id);
        map.put("name", name);
        map.put("owner", ownerId);
        map.put("bt_address", btAddress);
        map.put("color", color);

//        HashMap<String, Object> parking = toFirestoreParkingEvent(location, time, address, source);
//        map.put("parked_at", parking);

        return map;

    }

    @NonNull
    public static HashMap<String, Object> toFirestoreParkingEvent(Location location, Date time, String address, String source) {
        HashMap<String, Object> parking = new HashMap<>();
        parking.put("location", location != null ? new GeoPoint(location.getLatitude(), location.getLongitude()) : null);
        parking.put("accuracy", location != null ? location.getAccuracy() : null);
        parking.put("time", time);
        parking.put("address", address);
        parking.put("source", source);
        return parking;
    }


    public static Car fromFirestore(DocumentSnapshot documentSnapshot) {
        Car car = new Car();
        car.id = documentSnapshot.getId();
        car.legacy_id = (String) documentSnapshot.get("legacy_id");
        car.name = (String) documentSnapshot.get("name");
        car.btAddress = (String) documentSnapshot.get("bt_address");
        Long color = (Long) documentSnapshot.get("color");
        if (color != null)
            car.color = color.intValue();

        Map<String, Object> parkedAt = (Map<String, Object>) documentSnapshot.get("parked_at");
        if (parkedAt != null) {
            GeoPoint point = (GeoPoint) parkedAt.get("location");
            if (point != null) {
                Location location = new Location("");
                location.setLatitude(point.getLatitude());
                location.setLongitude(point.getLongitude());
                location.setAccuracy(((Double) parkedAt.get("accuracy")).floatValue());
                car.location = location;
                car.address = (String) parkedAt.get("address");
                car.time = (Date) parkedAt.get("time");
            }
        }

        return car;
    }
}
