package com.bahpps.cahue.spots;

import android.os.Parcel;
import android.os.Parcelable;

import com.bahpps.cahue.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Date;

/**
 * Created by Francesco on 11/10/2014.
 */
public class ParkingSpot implements Parcelable {

    private static long GREEN_TIME_THRESHOLD_MS = 10 * 60 * 1000;
    private static long YELLOW_TIME_THRESHOLD_MS = 45 * 60 * 1000;
    private static long ORANGE_TIME_THRESHOLD_MS = 120 * 60 * 1000;

    public final String id;

    public final LatLng position;

    public final Date time;


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

    public ParkingSpot(Parcel parcel) {
        id = parcel.readString();
        position = parcel.readParcelable(LatLng.class.getClassLoader());
        time = (Date) parcel.readSerializable();
    }

    public ParkingSpot(String id, LatLng location, Date time) {
        this.id = id;
        this.position = location;
        this.time = time;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeParcelable(position, i);
        parcel.writeSerializable(time);
    }

    @Override
    public String toString() {
        return "ParkingSpot{" +
                "id='" + id + '\'' +
                ", position=" + position +
                ", time=" + time +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParkingSpot that = (ParkingSpot) o;

        if (!id.equals(that.id)) return false;
        if (!position.equals(that.position)) return false;
        if (!time.equals(that.time)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + position.hashCode();
        result = 31 * result + time.hashCode();
        return result;
    }

    /**
     * Get the marker time based on the time it was created
     *
     * @return
     */
    public Type getMarkerType() {
        long timeSinceSpotWasFree_ms = System.currentTimeMillis() - time.getTime();
        if (timeSinceSpotWasFree_ms < GREEN_TIME_THRESHOLD_MS)
            return Type.green;
        else if (timeSinceSpotWasFree_ms < YELLOW_TIME_THRESHOLD_MS)
            return Type.yellow;
        else if (timeSinceSpotWasFree_ms < ORANGE_TIME_THRESHOLD_MS)
            return Type.orange;
        else
            return Type.red;
    }

    /**
     * Created by Francesco on 19.11.2014.
     */
    public static enum Type {

        red(0.02F, R.color.marker_red),
        orange(0.04F, R.color.marker_orange),
        yellow(0.03F, R.color.marker_yellow),
        green(0.06F, R.color.marker_green);

        /**
         * Difference in alpha values per frame when the marker is included in the map
         */
        public final float dAlpha;

        /**
         * Resource color representing
         */
        public final int colorId;

        Type(float dAlpha, int colorId) {
            this.dAlpha = dAlpha;
            this.colorId = colorId;
        }

    }
}
