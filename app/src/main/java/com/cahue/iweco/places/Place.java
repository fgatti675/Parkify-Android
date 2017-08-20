package com.cahue.iweco.places;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by f.gatti.gomez on 31/07/16.
 */
public class Place implements Parcelable {

    public static final Parcelable.Creator<Place> CREATOR =
            new Parcelable.Creator<Place>() {
                @NonNull
                @Override
                public Place createFromParcel(@NonNull Parcel parcel) {
                    return new Place(parcel);
                }

                @NonNull
                @Override
                public Place[] newArray(int size) {
                    return new Place[size];
                }
            };

    String id;

    Location location;

    String name;

    String address;

    private LatLng latLng;

    public Place(String id, Location location, String name, String address) {
        this.id = id;
        this.location = location;
        this.name = name;
        this.address = address;
    }

    public Place(Parcel parcel) {
        id = parcel.readString();
        location = parcel.readParcelable(Location.class.getClassLoader());
        name = parcel.readString();
        address = parcel.readString();
    }

    @NonNull
    public static Place fromJSON(@NonNull JSONObject jsonObject) throws JSONException {
        Location location = new Location("Json");
        location.setLatitude(jsonObject.getDouble("latitude"));
        location.setLongitude(jsonObject.getDouble("longitude"));
        return new Place(
                jsonObject.getString("id"),
                location,
                jsonObject.getString("name"),
                jsonObject.getString("address"));
    }

    @Override
    public String toString() {
        return "Place{" +
                "id=" + id +
                ", location=" + location +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }

    public LatLng getLatLng() {
        if (latLng == null) latLng = new LatLng(location.getLatitude(), location.getLongitude());
        return latLng;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Place place = (Place) o;

        return id != null ? id.equals(place.id) : place.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeParcelable(location, i);
        parcel.writeString(name);
        parcel.writeString(address);
    }
}
