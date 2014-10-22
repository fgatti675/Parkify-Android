package com.bahpps.cahue.spots;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by Francesco on 11/10/2014.
 */
public class ParkingSpot implements Parcelable {

    String id;

    LatLng location;

    Date time;

    public static final Parcelable.Creator<ParkingSpot> CREATOR =
            new Parcelable.Creator<ParkingSpot>() {
                @Override
                public ParkingSpot createFromParcel(Parcel parcel) {
                    return new ParkingSpot(parcel);
                }

                @Override
                public ParkingSpot[] newArray(int size) {
                    return new ParkingSpot[size];
                }
            };

    public ParkingSpot() {
    }

    public ParkingSpot(Parcel parcel) {
        id = parcel.readString();
        location = parcel.readParcelable(LatLng.class.getClassLoader());
        time = (Date) parcel.readSerializable();
    }

    public ParkingSpot(String id, LatLng location, Date time) {
        this.id = id;
        this.location = location;
        this.time = time;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeParcelable(location, 0);
        parcel.writeSerializable(time);
    }

    @Override
    public String toString() {
        return "ParkingSpot{" +
                "id='" + id + '\'' +
                ", location=" + location +
                ", time=" + time +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParkingSpot that = (ParkingSpot) o;

        if (!id.equals(that.id)) return false;
        if (!location.equals(that.location)) return false;
        if (!time.equals(that.time)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + location.hashCode();
        result = 31 * result + time.hashCode();
        return result;
    }
}
