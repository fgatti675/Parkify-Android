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
                .inflate(R.layout.color_spinner_row, parent, false);

        CarColor color = getItem(position);

        TextView name = (TextView) row.findViewById(R.id.name);
        ImageView image = (ImageView) row.findViewById(R.id.image);

        name.setText(context.getResources().getString(color.nameId));

        GradientDrawable gradientDrawable = (GradientDrawable) image.getDrawable();
        gradientDrawable.setColor(context.getResources().getColor(color.colorId));

        return row;
    }

    @Override
    public int getCount() {
        return colors.size();
    }

    @Override
    public CarColor getItem(int position) {
        return colors.get(position);
    }

    @Override
    public long getItemId(int position) {
        return context.getResources().getColor(colors.get(position).colorId);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getDropDownView(position, convertView, parent);
    }

}
