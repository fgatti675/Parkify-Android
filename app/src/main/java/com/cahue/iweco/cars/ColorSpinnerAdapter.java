package com.cahue.iweco.cars;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cahue.iweco.R;


/**
 * Created by francesco on 29.01.2015.
 */
public class ColorSpinnerAdapter extends BaseAdapter {


    private final int[] colorsValues;
    private final String[] colorNames;
    private final Context context;

    public ColorSpinnerAdapter(@NonNull Context context) {
        this.context = context;

        colorsValues = context.getResources().getIntArray(R.array.rainbow_colors);
        colorNames = context.getResources().getStringArray(R.array.rainbow_names);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {

        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.spinner_color_item, parent, false);

        TextView name = (TextView) row.findViewById(R.id.name);
        ImageView image = (ImageView) row.findViewById(R.id.image);

        if (position == 0) {
            name.setText(R.string.pick_color);
            GradientDrawable gradientDrawable = (GradientDrawable) image.getDrawable();
            gradientDrawable.setColor(context.getResources().getColor(android.R.color.transparent));
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

    @Nullable
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
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return getDropDownView(position, convertView, parent);
    }

    /**
     * Get the position of a color, based on its value (RGB)
     *
     * @param color
     */
    public int getPositionOf(@Nullable Integer color) {

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
