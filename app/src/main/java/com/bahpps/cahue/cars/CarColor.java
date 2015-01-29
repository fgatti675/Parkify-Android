package com.bahpps.cahue.cars;

import com.bahpps.cahue.R;

/**
 * Created by francesco on 29.01.2015.
 */
public enum CarColor {

    WHITE(R.string.car_white, R.color.car_white),
    BLACK(R.string.car_black, R.color.car_black),
    SILVER(R.string.car_silver, R.color.car_silver),
    GRAY(R.string.car_gray, R.color.car_gray),
    RED(R.string.car_red, R.color.car_red),
    BLUE(R.string.car_blue, R.color.car_blue),
    BROWN(R.string.car_brown, R.color.car_brown),
    GREEN(R.string.car_green, R.color.car_green),
    OLIVE(R.string.car_olive, R.color.car_olive),
    YELLOW(R.string.car_yellow, R.color.car_yellow),
    GOLD(R.string.car_gold, R.color.car_gold),
    ORANGE(R.string.car_orange, R.color.car_orange),
    PINK(R.string.car_pink, R.color.car_pink);

    final int nameId;
    final int colorId;

    CarColor(int nameId, int colorId) {
        this.nameId = nameId;
        this.colorId = colorId;
    }
}
