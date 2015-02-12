package com.bahpps.cahue.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;

import com.bahpps.cahue.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by francesco on 11.02.2015.
 */
public class CarImages {

    private static Map<Integer, Integer> colorImageMap;

    public static Integer getImageResourceId(int color, Context context){
        /**
         * Build images map if not there
         */
        if (colorImageMap == null) {
            colorImageMap = new HashMap<>();
            Resources resources = context.getApplicationContext().getResources();
            int[] colors = resources.getIntArray(R.array.rainbow_colors);
            TypedArray carImages = resources.obtainTypedArray(R.array.rainbow_cars);
            for (int i = 0; i < colors.length; i++) {
                colorImageMap.put(colors[i], carImages.getResourceId(i, -1));
            }
        }
        Integer drawableId = colorImageMap.get(color);
        return drawableId == null ? R.drawable.ic_car_silver : drawableId;
    }
}
