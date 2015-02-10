package com.bahpps.cahue.cars;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.bahpps.cahue.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by francesco on 28.11.2014.
 */
public class Car implements Parcelable {

    public static Car fromJSON(JSONObject carJSON) {
        try {
            Car car = new Car();
            car.id = carJSON.getString("id");
            car.name = carJSON.getString("name");
            car.btAddress = carJSON.getString("btAddress");

            if (carJSON.has("color")) {
                car.color = carJSON.getInt("color");
            }

            if(carJSON.has("latitude") && carJSON.has("longitude")) {
                Location location = new Location("JSON");
                location.setLatitude(carJSON.getDouble("latitude"));
                location.setLongitude(carJSON.getDouble("longitude"));
                location.setAccuracy((float) carJSON.getDouble("accuracy"));
                car.location = location;
                if(carJSON.has("time")){
                    car.time = Util.DATE_FORMAT.parse(carJSON.getString("time"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("name", name);
            if(btAddress != null) jsonObject.put("btAddress", btAddress);
            if(color != null) jsonObject.put("color", color);
            if(location != null){
                jsonObject.put("latitude", location.getLatitude());
                jsonObject.put("longitude", location.getLongitude());
                jsonObject.put("accuracy", location.getAccuracy());
            }
            jsonObject.put("time", Util.DATE_FORMAT.format(time));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static final String TABLE_NAME = "cars";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_BT_ADDRESS = "bt_address";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ACCURACY = "accuracy";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_COLOR = "color";

    public String id;

    public String name;

    public String btAddress;

    public Location location;

    public Date time = new Date();

    public Integer color;

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

    public Car(Parcel parcel) {
        id = parcel.readString();
        name = parcel.readString();
        btAddress = parcel.readString();
        location = parcel.readParcelable(Location.class.getClassLoader());
        time = (Date) parcel.readSerializable();
        color = (Integer) parcel.readValue(Integer.class.getClassLoader());
    }

    public Car() {

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
        parcel.writeParcelable(location, i);
        parcel.writeSerializable(time);
        parcel.writeValue(color);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Car car = (Car) o;

        if (!btAddress.equals(car.btAddress)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return btAddress.hashCode();
    }

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
}
