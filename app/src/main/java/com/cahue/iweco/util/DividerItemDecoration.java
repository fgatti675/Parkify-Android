package com.cahue.iweco.util;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Created by francesco on 27.01.2015.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private final int dp_padding;

    public DividerItemDecoration(Context context) {
        dp_padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
    }

    @Override
    public void getItemOffsets(android.graphics.Rect outRect, android.view.View view, android.support.v7.widget.RecyclerView parent, android.support.v7.widget.RecyclerView.State state) {
        outRect.set(0, 0, 0, dp_padding);
    }
}