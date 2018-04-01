package com.cahue.iweco.model;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cahue.iweco.R;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.util.Date;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by Francesco on 11/10/2014.
 */
public class PossibleSpot implements Parcelable {


    public static final int RECENT = 0;
    public static final int NOT_SO_RECENT = 1;

    @Retention(SOURCE)
    @IntDef({RECENT, NOT_SO_RECENT})
    public @interface Recency {
    }


    public static final Creator<PossibleSpot> CREATOR =
            new Creator<PossibleSpot>() {
                @NonNull
                @Override
                public PossibleSpot createFromParcel(@NonNull Parcel parcel) {
                    return new PossibleSpot(parcel);
                }

                @NonNull
                @Override
                public PossibleSpot[] newArray(int size) {
                    return new PossibleSpot[size];
                }
            };
    private static final long GREEN_TIME_THRESHOLD_MS = 5 * 60 * 1000;
    private static final long YELLOW_TIME_THRESHOLD_MS = 10 * 60 * 1000;

    public final Location location;
    public final Date time;
    public final int recency;

    public final boolean future;

    @Nullable
    public String address;
    private LatLng latLng;

    public PossibleSpot(@NonNull Parcel parcel) {
        location = parcel.readParcelable(Location.class.getClassLoader());
        address = parcel.readString();
        time = (Date) parcel.readSerializable();
        future = parcel.readByte() != 0;
        recency = parcel.readInt();
    }

    public PossibleSpot(Location location, @Nullable String address, Date time, boolean future, int recency) {
        this.location = location;
        this.address = address;
        this.time = time;
        this.future = future;
        this.recency = recency;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeParcelable(location, i);
        parcel.writeString(address);
        parcel.writeSerializable(time);
        parcel.writeByte((byte) (future ? 1 : 0));
        parcel.writeInt(recency);
    }

    @Nullable
    public JSONObject toJSON(@NonNull Car car) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("car", car.id);
            if (car.spotId != null)
                obj.put("id", car.spotId);
            obj.put("latitude", location.getLatitude());
            obj.put("longitude", location.getLongitude());
            obj.put("accuracy", location.getAccuracy());
            obj.put("future", future);
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ParkingSpot{" +
                ", location=" + location +
                ", time=" + time +
                ", future=" + future +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PossibleSpot that = (PossibleSpot) o;

        if (!location.equals(that.location)) return false;
        return time.equals(that.time);

    }

    public int getRecency() {
        return recency;
    }

    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + time.hashCode();
        return result;
    }

    /**
     * Get the marker time based on the time it was created
     *
     * @return
     */
    @NonNull
    public Type getMarkerType() {
        long timeSinceSpotWasFree_ms = System.currentTimeMillis() - time.getTime();
        if (timeSinceSpotWasFree_ms < GREEN_TIME_THRESHOLD_MS)
            return Type.green;
        else if (timeSinceSpotWasFree_ms < YELLOW_TIME_THRESHOLD_MS)
            return Type.yellow;
        else
            return Type.red;
    }

    public LatLng getLatLng() {
        if (latLng == null) latLng = new LatLng(location.getLatitude(), location.getLongitude());
        return latLng;
    }

    public Date getTime() {
        return time;
    }

    /**
     * Created by Francesco on 19.11.2014.
     */
    public enum Type {

        red(0.02F, R.color.marker_red),
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
