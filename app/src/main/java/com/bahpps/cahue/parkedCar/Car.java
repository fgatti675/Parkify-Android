package com.bahpps.cahue.parkedCar;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by francesco on 28.11.2014.
 */
public class Car implements Parcelable{

    public String id;

    public String name;

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
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Car{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", time=" + time +
                '}';
    }
}
