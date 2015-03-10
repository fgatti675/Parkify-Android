package com.cahue.iweco.util;

import android.support.v7.widget.RecyclerView;

/**
 * Created by francesco on 27.01.2015.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    @Override
    public void getItemOffsets(android.graphics.Rect outRect, android.view.View view, android.support.v7.widget.RecyclerView parent, android.support.v7.widget.RecyclerView.State state) {
        outRect.set(0, 10, 0, 0);
    }
}