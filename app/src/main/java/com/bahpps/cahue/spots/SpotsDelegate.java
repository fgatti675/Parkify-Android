package com.bahpps.cahue.spots;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Francesco on 21/10/2014.
 */
public class SpotsDelegate implements Parcelable {

    List<ParkingSpot> spots;

    public static final Parcelable.Creator<SpotsDelegate> CREATOR =
            new Parcelable.Creator<SpotsDelegate>()
            {
                @Override
                public SpotsDelegate createFromParcel(Parcel parcel)
                {
                    return new SpotsDelegate(parcel);
                }

                @Override
                public SpotsDelegate[] newArray(int size)
                {
                    return new SpotsDelegate[size];
                }
            };

    public SpotsDelegate(Parcel parcel) {
        ParkingSpot[] spotsArray = (ParkingSpot[]) parcel.readParcelableArray(SpotsDelegate.class.getClassLoader());
        spots = Arrays.asList(spotsArray);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        ParkingSpot[] spotsArray = new ParkingSpot[spots.size()];
        parcel.writeParcelableArray(spots.toArray(spotsArray), 0);
    }
}
