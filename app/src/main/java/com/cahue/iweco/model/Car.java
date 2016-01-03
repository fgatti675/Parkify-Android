package com.cahue.iweco.model;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.cahue.iweco.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by francesco on 28.11.2014.
 */
public class Car implements Parcelable {

    public static final Parcelable.Creator<Car> CREATOR =
            new Parcelable.Creator<Car>() {
                @Override
                public Car createFromParcel(Parcel parcel) {
                    return new Car(parcel);
                }

                @Override
                public Car[] newArray(int size) {
                    return new Car[size];
                }
            };
    public String id;
    public String name;
    public String btAddress;
    public Long spotId;
    public Location location;
    public Date time;
    public Integer color;
    public String address;

    public Car(Parcel parcel) {
        id = parcel.readString();
        name = parcel.readString();
        btAddress = parcel.readString();
        spotId = (Long) parcel.readValue(Long.class.getClassLoader());
        location = parcel.readParcelable(Location.class.getClassLoader());
        time = (Date) parcel.readSerializable();
        color = (Integer) parcel.readValue(Integer.class.getClassLoader());
        address = parcel.readString();
    }

    public Car() {

    }

    public static Set<Car> fromJSONArray(JSONArray carsArray) {
        try {
            Set<Car> cars = new HashSet<>();
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

    public static Car fromJSON(JSONObject carJSON) {
        try {
            Car car = new Car();
            car.id = carJSON.getString("id");

            if (carJSON.has("name")) {
                car.name = carJSON.getString("name");
            }

            if (carJSON.has("btAddress")) {
                car.btAddress = carJSON.getString("btAddress");
            }

            if (carJSON.has("color")) {
                car.color = carJSON.getInt("color");
            }

            if (carJSON.has("spotId")) {
                car.spotId = carJSON.getLong("spotId");
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
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(btAddress);
        parcel.writeValue(spotId);
        parcel.writeParcelable(location, i);
        parcel.writeSerializable(time);
        parcel.writeValue(color);
        parcel.writeString(address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Car car = (Car) o;

        return id.equals(car.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Car{" +
                "id='" + id + '\'' +
                ", btAddress='" + btAddress + '\'' +
                ", name='" + name + '\'' +
                ", spotId=" + spotId +
                ", location=" + location +
                ", time=" + time +
                '}';
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("name", name);
            if (btAddress != null) jsonObject.put("btAddress", btAddress);
            if (color != null) jsonObject.put("color", color);
            if (spotId != null) jsonObject.put("spotId", spotId);
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
}
