package com.whereismycar.util;

import android.graphics.Color;

/**
 * Created by Francesco on 08/03/2015.
 */
public class ColorUtil {

    public static boolean isBrightColor(int color) {
        if (android.R.color.transparent == color)
            return true;

        boolean rtnValue = false;

        int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};

        int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1]
                * rgb[1] * .691 + rgb[2] * rgb[2] * .068);

        // color is light
        if (brightness >= 220) {
            rtnValue = true;
        }

        return rtnValue;
    }


    public static int getSemiTransparent(int color) {
        return Color.argb(100, Color.red(color), Color.green(color), Color.blue(color));
    }

}
