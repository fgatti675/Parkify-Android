package com.cahue.iweco.tutorial;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarManagerFragment;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.EditCarDialog;

import java.util.HashMap;
import java.util.Map;


public class TutorialActivity extends AppCompatActivity
        implements CarManagerFragment.Callbacks,
        EditCarDialog.CarEditedListener,
        ViewPager.OnPageChangeListener {

    private static final int TOTAL_NUMBER_PAGES = "wimc".equals(BuildConfig.FLAVOR) ? 3 : 4;
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
                finish();
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        final LayerDrawable background = (LayerDrawable) mViewPager.getBackground();

        background.getDrawable(0).setAlpha(0); // this is the lowest drawable
        background.getDrawable(1).setAlpha(0);
        if("wimc".equals(BuildConfig.FLAVOR)){
            background.getDrawable(2).setAlpha(255); // this is the upper one
        }else {
            background.getDrawable(2).setAlpha(0);
            background.getDrawable(3).setAlpha(255); // this is the upper one
        }

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

        // animation
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

    }

    @Override
    protected void onPause() {
        super.onPause();

        //closing transition animations
        overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);
    }

    @Override
    public void devicesBeingLoaded(boolean loading) {
        Log.d(TAG, "Devices being loaded: " + loading);
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

        findViewById(R.id.progress_wrapper).setVisibility(position == TOTAL_NUMBER_PAGES - 1 ? View.VISIBLE : View.INVISIBLE);
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

        CarManagerFragment carManagerFragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return TutorialWelcomeFragment.newInstance();
                case 1:
                    return TutorialInstructionsFragment.newInstance(R.string.info_parking_text_header, R.string.info_parking_text, TutorialInstructionsFragment.TYPE_PARKING);
                case 2:
                    if(!"wimc".equals(BuildConfig.FLAVOR))
                        return TutorialInstructionsFragment.newInstance(R.string.info_spots_text_header, R.string.info_spots_text, TutorialInstructionsFragment.TYPE_SPOTS);
                case 3:
                    if (carManagerFragment == null)
                        carManagerFragment = CarManagerFragment.newInstance();
                    return carManagerFragment;
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
            } else if (object instanceof TutorialInstructionsFragment) {
                if (((TutorialInstructionsFragment) object).getType().equals(TutorialInstructionsFragment.TYPE_PARKING))
                    view.setTag(1);
                else if (((TutorialInstructionsFragment) object).getType().equals(TutorialInstructionsFragment.TYPE_SPOTS))
                    view.setTag(2);
            } else if (object instanceof CarManagerFragment) {
                if("wimc".equals(BuildConfig.FLAVOR)) {
                    view.setTag(2);
                } else{
                    view.setTag(3);
                }
            }
            return super.isViewFromObject(view, object);
        }
    }

    @Override
    public void onCarEdited(Car car, boolean newCar) {
        ((CarManagerFragment) mSectionsPagerAdapter.getItem("wimc".equals(BuildConfig.FLAVOR) ? 2 : 3)).onCarEdited(car, newCar);
    }

}
