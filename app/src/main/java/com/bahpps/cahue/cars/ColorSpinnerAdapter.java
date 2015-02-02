package com.bahpps.cahue.cars;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bahpps.cahue.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by francesco on 29.01.2015.
 */
public class ColorSpinnerAdapter extends BaseAdapter {


    private Context context;
    private final int[] colorsValues;
    private final String[] colorNames;

    public ColorSpinnerAdapter(Context context) {
        this.context = context;

        int a = context.getResources().getColor(R.color.car_blue);

        colorsValues = context.getResources().getIntArray(R.array.rainbow_colors);
        colorNames = context.getResources().getStringArray(R.array.rainbow_names);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.spinner_color_item, parent, false);

        TextView name = (TextView) row.findViewById(R.id.name);
        ImageView image = (ImageView) row.findViewById(R.id.image);

        if (position == 0) {
            name.setText(R.string.pick_color);
            GradientDrawable gradientDrawable = (GradientDrawable) image.getDrawable();
            gradientDrawable.setColor(context.getResources().getColor(R.color.transparent));
        } else {

            name.setText(colorNames[position - 1]);

            GradientDrawable gradientDrawable = (GradientDrawable) image.getDrawable();
            gradientDrawable.setColor(colorsValues[position - 1]);
        }
        return row;
    }

    @Override
    public int getCount() {
        return colorsValues.length + 1;
    }

    @Override
    public Integer getItem(int position) {

        if (position == 0) return null;
        return colorsValues[position-1];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getDropDownView(position, convertView, parent);
    }

    /**
     * Get the position of a color, based on its value (RGB)
     *
     * @param color
     */
    public int getPositionOf(Integer color) {

        if (color == null) return 0;

        int position = 1;
        for (int carColor : colorsValues) {

            if (color == carColor) {
                return position;
            }

            position++;
        }
        return 0;
    }
}
