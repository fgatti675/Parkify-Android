package com.bahpps.cahue.cars;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.location.Location;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bahpps.cahue.R;
import com.bahpps.cahue.util.ColorUtil;

/**
 * Created by Francesco on 08/03/2015.
 */
public final class CarViewHolder extends RecyclerView.ViewHolder {

    public Toolbar toolbar;
    public TextView linkedDevice;
    public TextView name;
    public TextView time;
    public TextView distance;
    public TextView address;
    public ImageView carImage;

    public CarViewHolder(View itemView) {
        super(itemView);

        carImage = (ImageView) itemView.findViewById(R.id.car_image);
        toolbar = (Toolbar) itemView.findViewById(R.id.car_toolbar);
        linkedDevice = (TextView) itemView.findViewById(R.id.linked_device);
        name = (TextView) itemView.findViewById(R.id.name);
        time = (TextView) itemView.findViewById(R.id.time);
        distance = (TextView) itemView.findViewById(R.id.distance);
        address = (TextView) itemView.findViewById(R.id.address);
    }

    public void bind(Context context, final Car car, Location userLastLocation, BluetoothAdapter btAdapter) {

        name.setText(car.name);
        if (car.location != null && car.time != null) {
            time.setText(DateUtils.getRelativeTimeSpanString(car.time.getTime()));
            time.setVisibility(View.VISIBLE);
        } else {
            time.setVisibility(View.GONE);
        }

        if (car.color != null) {

            if (car.color != null)
                carImage.setBackgroundColor(car.color);
            else
                carImage.setBackgroundColor(context.getResources().getColor(R.color.theme_accent_1));

            if (car.color != null && ColorUtil.isBrightColor(car.color)) {
                carImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_car_grey600_36dp));
            } else{
                carImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_car_white_36dp));
            }
        }

        if (car.btAddress != null && btAdapter.isEnabled()) {
            linkedDevice.setVisibility(View.VISIBLE);
            for (BluetoothDevice device : btAdapter.getBondedDevices()) {
                if (device.getAddress().equals(car.btAddress)) {
                    String deviceName = device.getName();
                    if(car.name == null || !car.name.equals(deviceName)) {
                        linkedDevice.setText(deviceName);
                        linkedDevice.setVisibility(View.VISIBLE);
                        break;
                    } else{
                        linkedDevice.setVisibility(View.GONE);
                    }
                }
            }
        } else {
            linkedDevice.setVisibility(View.GONE);
        }

        updateDistance(userLastLocation, car.location);

        if (car.address != null) {
            address.setText(car.address);
            address.setVisibility(View.VISIBLE);
        } else {
            address.setVisibility(View.GONE);
        }

    }

    public void updateDistance(Location userLocation, Location carLocation) {
        if (userLocation != null && carLocation != null) {
            float distanceM = carLocation.distanceTo(userLocation);
            distance.setText(String.format("%.1f km", distanceM / 1000));
            distance.setVisibility(View.VISIBLE);
        } else {
            distance.setVisibility(View.GONE);
        }
    }

}
