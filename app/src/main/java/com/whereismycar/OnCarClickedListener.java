package com.whereismycar;

import com.whereismycar.model.Car;

/**
 * Callbacks interface that all activities using this fragment must implement.
 */
public interface OnCarClickedListener {
    /**
     * Called when a car is selected in an UI component
     */
    void onCarSelected(Car car);
}
