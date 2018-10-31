package com.cahue.iweco.cars;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cahue.iweco.R;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.ColorUtil;
import com.cahue.iweco.util.PreferencesUtil;

/**
 * Created by Francesco on 08/03/2015.
 */
public class CarViewHolder extends RecyclerView.ViewHolder {

    public final View cardView;
    public final Toolbar toolbar;
    public final TextView linkedDevice;
    public final TextView name;
    public final TextView time;
    public final TextView distance;
    public final TextView address;
    public final ImageView carImage;

    public CarViewHolder(@NonNull View itemView) {

        super(itemView);

        this.cardView = itemView;

        carImage = itemView.findViewById(R.id.car_image);
        toolbar = itemView.findViewById(R.id.car_toolbar);
        linkedDevice = itemView.findViewById(R.id.linked_device);
        name = itemView.findViewById(R.id.name);
        time = itemView.findViewById(R.id.time);
        distance = itemView.findViewById(R.id.distance);
        address = itemView.findViewById(R.id.address);

    }

    public void bind(@NonNull Context context, @NonNull final Car car, Location userLastLocation, @Nullable BluetoothAdapter btAdapter) {

        if (car.isOther()) {
            name.setText(context.getResources().getText(R.string.other));
        } else {
            name.setText(car.name);
        }

        updateTime(car);

        int color = car.isOther() ?
                context.getResources().getColor(R.color.silver) :
                (car.color != null ?
                        car.color :
                        context.getResources().getColor(R.color.theme_accent));

        carImage.setBackgroundColor(color);

        if (ColorUtil.isBrightColor(color)) {
            carImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_car_grey600_24dp));
        } else {
            carImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_car_white_24dp));
        }

        if (car.btAddress != null && btAdapter != null && btAdapter.isEnabled()) {
            for (BluetoothDevice device : btAdapter.getBondedDevices()) {
                if (device.getAddress().equals(car.btAddress)) {
                    String deviceName = device.getName();
                    if (car.name == null || !car.name.equals(deviceName) || deviceName.isEmpty()) {
                        linkedDevice.setText(deviceName);
                        linkedDevice.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            }
        }

        updateDistance(context, userLastLocation, car.location);

        updateAddress(car);

    }

    private void updateAddress(@NonNull Car car) {
        if (car.address != null) {
            address.setText(car.address);
            address.setVisibility(View.VISIBLE);
        } else if (car.location == null) {
            address.setText(R.string.position_not_set);
            address.setVisibility(View.VISIBLE);
        } else {
            address.setVisibility(View.GONE);
        }
    }

    private void updateTime(@NonNull Car car) {
        if (car.location != null && car.time != null) {
            time.setText(DateUtils.getRelativeTimeSpanString(car.time.getTime()));
            time.setVisibility(View.VISIBLE);
        } else {
            time.setVisibility(View.GONE);
        }
    }

    public void updateDistance(Context context, @Nullable Location userLocation, @Nullable Location carLocation) {
        if (userLocation != null && carLocation != null) {
            float distanceM = carLocation.distanceTo(userLocation);
            if (PreferencesUtil.isUseMiles(context)) {
                distance.setText(String.format("%.1f miles", distanceM / 1609.34));
            } else {
                distance.setText(String.format("%.1f km", distanceM / 1000));
            }
            distance.setVisibility(View.VISIBLE);
        } else {
            distance.setVisibility(View.GONE);
        }
    }

}
