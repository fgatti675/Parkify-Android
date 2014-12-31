package com.bahpps.cahue.tutorial;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.bahpps.cahue.DeviceSelectionFragment;
import com.bahpps.cahue.R;

import java.util.Locale;


public class TutorialActivity extends ActionBarActivity
        implements DeviceSelectionFragment.DeviceSelectionLoadingListener, ViewPager.OnPageChangeListener {


    private static final int TOTAL_NUMBER_PAGES = 3;
    private static final String TAG = TutorialActivity.class.getSimpleName();

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    Button next;
    Button previous;
    Button ok;

    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tutorial);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        next = (Button) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
            }
        });

        previous = (Button) findViewById(R.id.previous);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
            }
        });

        ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        final LayerDrawable background = (LayerDrawable) mViewPager.getBackground();

        background.getDrawable(0).setAlpha(0); // this is the lowest drawable
        background.getDrawable(1).setAlpha(0);
        background.getDrawable(2).setAlpha(255); // this is the upper one

        mViewPager.setPageTransformer(true, new ViewPager.PageTransformer() {
            @Override
            public void transformPage(View view, float position) {

                int index = (Integer) view.getTag();
                Drawable currentDrawableInLayerDrawable;
                currentDrawableInLayerDrawable = background.getDrawable(index);

                if (position <= -1 || position >= 1) {
                    currentDrawableInLayerDrawable.setAlpha(0);
                } else if (position == 0) {
                    currentDrawableInLayerDrawable.setAlpha(255);
                } else {
                    currentDrawableInLayerDrawable.setAlpha((int) (255 - Math.abs(position * 255)));
                }

            }
        });
        mViewPager.setOffscreenPageLimit(TOTAL_NUMBER_PAGES - 1);
        mViewPager.setOnPageChangeListener(this);

    }

    @Override
    public void devicesBeingLoaded(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (position == 0) {
            previous.setAlpha(positionOffset);
        } else if (position == TOTAL_NUMBER_PAGES - 2) {
            next.setAlpha(1 - positionOffset);
            ok.setAlpha(positionOffset);
        } else if (position == TOTAL_NUMBER_PAGES - 1) {
            next.setAlpha(0);
            ok.setAlpha(1);
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (position == TOTAL_NUMBER_PAGES - 1) {
            next.setVisibility(View.GONE);
            ok.setVisibility(View.VISIBLE);
        } else {
            next.setVisibility(View.VISIBLE);
            ok.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state != ViewPager.SCROLL_STATE_IDLE) {
            next.setVisibility(View.VISIBLE);
            ok.setVisibility(View.VISIBLE);
        }
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return TutorialWelcomeFragment.newInstance();
                case 1:
                    return TutorialInstructionsFragment.newInstance();
                case 2:
                    return DeviceSelectionFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return TOTAL_NUMBER_PAGES;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            if (object instanceof TutorialWelcomeFragment) {
                view.setTag(0);
            }
            if (object instanceof TutorialInstructionsFragment) {
                view.setTag(1);
            }
            if (object instanceof DeviceSelectionFragment) {
                view.setTag(2);
            }
            return super.isViewFromObject(view, object);
        }
    }


}
