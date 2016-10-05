package com.cahue.iweco.model;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cahue.iweco.R;
import com.cahue.iweco.util.Util;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by Francesco on 11/10/2014.
 */
public class ParkingSpot implements Parcelable {

    public static final Parcelable.Creator<ParkingSpot> CREATOR =
            new Parcelable.Creator<ParkingSpot>() {
                @NonNull
                @Override
                public ParkingSpot createFromParcel(@NonNull Parcel parcel) {
                    return new ParkingSpot(parcel);
                }

                @NonNull
                @Override
                public ParkingSpot[] newArray(int size) {
                    return new ParkingSpot[size];
                }
            };
    private static final long GREEN_TIME_THRESHOLD_MS = 5 * 60 * 1000;
    private static final long YELLOW_TIME_THRESHOLD_MS = 10 * 60 * 1000;

    @Nullable
    public final Long id;
    public final Location location;
    public final Date time;

    public final boolean future;

    @Nullable
    public String address;
    private LatLng latLng;

    public ParkingSpot(@NonNull Parcel parcel) {

        Long parcelid = parcel.readLong();
        if (parcelid == -1) parcelid = null;

        id = parcelid;
        location = parcel.readParcelable(Location.class.getClassLoader());
        address = parcel.readString();
        time = (Date) parcel.readSerializable();
        future = parcel.readByte() != 0;
    }

    public ParkingSpot(@Nullable Long id, Location location, @Nullable String address, Date time, boolean future) {
        this.id = id;
        this.location = location;
        this.address = address;
        this.time = time;
        this.future = future;
    }

    @NonNull
    public static ParkingSpot fromJSON(@NonNull JSONObject jsonObject) throws JSONException {
        try {
            Location location = new Location("Json");
            location.setLatitude(jsonObject.getDouble("latitude"));
            location.setLongitude(jsonObject.getDouble("longitude"));
            location.setAccuracy(Float.parseFloat(jsonObject.getString("accuracy")));
            return new ParkingSpot(
                    jsonObject.getLong("id"),
                    location,
                    null,
                    Util.DATE_FORMAT.parse(jsonObject.getString("time")),
                    jsonObject.optBoolean("future"));
        } catch (ParseException e) {
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
        parcel.writeLong(id == null ? -1 : id);
        parcel.writeParcelable(location, i);
        parcel.writeString(address);
        parcel.writeSerializable(time);
        parcel.writeByte((byte) (future ? 1 : 0));
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
                "id='" + id + '\'' +
                ", location=" + location +
                ", time=" + time +
                ", future=" + future +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParkingSpot that = (ParkingSpot) o;

        if (!id.equals(that.id)) return false;
        if (!location.equals(that.location)) return false;
        return time.equals(that.time);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + location.hashCode();
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
