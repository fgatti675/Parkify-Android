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

import java.util.Arrays;
import java.util.List;

/**
 * Created by francesco on 29.01.2015.
 */
public class ColorSpinnerAdapter extends BaseAdapter {

    private List<CarColor> colors;

    private Context context;

    public ColorSpinnerAdapter(Context context) {
        this.context = context;

        colors = Arrays.asList(CarColor.values());
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.spinner_color_item, parent, false);

        if (position == 0) {
            TextView textView = new TextView(context);
            textView.setText(R.string.pick_color);
            return textView;
        }

        TextView name = (TextView) row.findViewById(R.id.name);
        CarColor color = getItem(position);

        ImageView image = (ImageView) row.findViewById(R.id.image);

        name.setText(context.getResources().getString(color.nameId));

        GradientDrawable gradientDrawable = (GradientDrawable) image.getDrawable();
        gradientDrawable.setColor(context.getResources().getColor(color.colorId));

        return row;
    }

    @Override
    public int getCount() {
        return colors.size() + 1;
    }

    @Override
    public CarColor getItem(int position) {

        if (position == 0) return null;
        return colors.get(position - 1);
    }

    @Override
    public long getItemId(int position) {
        return context.getResources().getColor(colors.get(position).colorId);
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
    public int getColorInPosition(int color) {
        int position = 1;
        for (CarColor carColor : colors) {

            if(color == context.getResources().getColor(carColor.colorId)){
                return position;
            }

            position++;
        }
        return -1;
    }
}
