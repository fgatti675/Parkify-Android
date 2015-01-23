package com.bahpps.cahue.parkedCar;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import java.util.Date;

/**
 * Created by francesco on 28.11.2014.
 */
public class Car implements Parcelable {

    public static final String TABLE_NAME = "cars";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_BT_ADDRESS = "bt_address";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ACCURACY = "accuracy";
    public static final String COLUMN_TIME = "time";

    public String id;

    public String name;

    public String btAddress;

    public Location location;

    public Date time;

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
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Car car = (Car) o;

        if (!id.equals(car.id)) return false;

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
