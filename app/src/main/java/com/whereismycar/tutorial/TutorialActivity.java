package com.whereismycar.tutorial;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.legacy.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.whereismycar.MapsActivity;
import com.whereismycar.R;
import com.whereismycar.cars.CarManagerFragment;
import com.whereismycar.cars.EditCarDialog;
import com.whereismycar.model.Car;
import com.whereismycar.util.PreferencesUtil;
import com.whereismycar.util.Tracking;


public class TutorialActivity extends AppCompatActivity
        implements CarManagerFragment.Callbacks,
        EditCarDialog.CarEditedListener,
        ViewPager.OnPageChangeListener {

    private static final int TOTAL_NUMBER_PAGES = 2;
    private static final String TAG = TutorialActivity.class.getSimpleName();

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private Button next;
    private Button previous;
    private Button ok;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        PreferencesUtil.setTutorialShown(this, true);

        setContentView(R.layout.activity_tutorial);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        next = findViewById(R.id.next);
        next.setOnClickListener(v -> mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true));

        previous = findViewById(R.id.previous);
        previous.setOnClickListener(v -> mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true));

        ok = findViewById(R.id.ok);
        ok.setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progress_bar);

        final LayerDrawable background = (LayerDrawable) getWindow().getDecorView().getBackground();

        background.getDrawable(0).setAlpha(255);
        background.getDrawable(1).setAlpha(0);

        mViewPager.setPageTransformer(true, (view, position) -> {

            int index = (Integer) view.getTag();
            Drawable currentDrawableInLayerDrawable;
            currentDrawableInLayerDrawable = background.getDrawable(TOTAL_NUMBER_PAGES - 1 - index);

            if (position <= -1 || position >= 1) {
                currentDrawableInLayerDrawable.setAlpha(0);
            } else if (position == 0) {
                currentDrawableInLayerDrawable.setAlpha(255);
            } else {
                currentDrawableInLayerDrawable.setAlpha((int) (255 - Math.abs(position * 255)));
            }

        });
        mViewPager.setOffscreenPageLimit(TOTAL_NUMBER_PAGES - 1);
        mViewPager.addOnPageChangeListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        Tracking.sendView(Tracking.CATEGORY_TUTORIAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //closing transition animations
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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
        }  else if (position == TOTAL_NUMBER_PAGES - 1) {
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

    @Override
    public void onCarEdited(@NonNull Car car, boolean newCar) {
        ((CarManagerFragment) mSectionsPagerAdapter.getItem(1)).onCarEdited(car, newCar);
    }

    @Override
    public void finish() {
        Intent intent = new Intent(TutorialActivity.this, MapsActivity.class);
        startActivity(intent);
        super.finish();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        CarManagerFragment carManagerFragment;

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Nullable
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return TutorialInstructionsFragment.newInstance(R.layout.fragment_tutorial_instructions_find, TutorialInstructionsFragment.TYPE_PARKING);
                case 1:
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
        public boolean isViewFromObject(@NonNull View view, Object object) {
            if (object instanceof TutorialInstructionsFragment) {
                view.setTag(0);
            } else if (object instanceof CarManagerFragment) {
                view.setTag(1);
            }
            return super.isViewFromObject(view, object);
        }
    }
}
