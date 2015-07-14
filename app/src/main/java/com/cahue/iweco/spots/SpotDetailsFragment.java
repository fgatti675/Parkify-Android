package com.cahue.iweco.spots;

import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.R;
import com.cahue.iweco.SpotsDelegate;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Util;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SpotDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SpotDetailsFragment extends DetailsFragment implements Toolbar.OnMenuItemClickListener {

    // the fragment initialization parameters
    private static final String ARG_SPOT = "arg_spot";
    private static final String ARG_LOCATION = "arg_location";

    private Location userLocation;
    private ParkingSpot spot;

    private TextView title;
    private TextView timeAgo;
    private TextView distance;
    private ImageView rectangleImage;

    private SpotsDelegate spotsDelegate;
    private Toolbar toolbar;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param spot Parameter 1.
     * @return A new instance of fragment MarkerDetailsFragment.
     */
    public static DetailsFragment newInstance(ParkingSpot spot, Location userLocation) {
        DetailsFragment fragment = new SpotDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SPOT, spot);
        args.putParcelable(ARG_LOCATION, userLocation);
        fragment.setArguments(args);
        return fragment;
    }

    public SpotDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCameraUpdate() {
        updateFollowButtonState();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            spot = getArguments().getParcelable(ARG_SPOT);
            userLocation = getArguments().getParcelable(ARG_LOCATION);
        }

        spotsDelegate = (SpotsDelegate) getFragmentManager().findFragmentByTag(SpotsDelegate.FRAGMENT_TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_spot_details, container, false);
        if (spot != null) {
//
            toolbar = (Toolbar) view.findViewById(R.id.spot_toolbar);
            toolbar.inflateMenu(R.menu.spot_menu);
            toolbar.setOnMenuItemClickListener(this);

            // Set time ago
            title = (TextView) view.findViewById(R.id.spot_title);
            timeAgo = (TextView) view.findViewById(R.id.time);
            updateTimeAgo();

            // Update distance
            distance = (TextView) view.findViewById(R.id.distance_time);

            // Set rectangle color
            rectangleImage = (ImageView) view.findViewById(R.id.spot_image);

            Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
            view.startAnimation(fadeInAnimation);
        }
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        updateDistance();
        updateImage();
        updateTimeAgo();
        updateFollowButtonState();
    }

    @Override
    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
        View view = getView();
        if (view != null) {
            updateDistance();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        // Handle presses on the action bar items
        switch (menuItem.getItemId()) {
            case R.id.action_follow:
                spotsDelegate.setCameraFollowing(true);
                updateFollowButtonState();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void updateImage() {
        if (spot.future) {
            rectangleImage.setImageResource(R.drawable.map_marker_future);
        } else {
            GradientDrawable gradientDrawable = (GradientDrawable) rectangleImage.getDrawable();
            gradientDrawable.setStroke((int) Util.dpToPx(getActivity(), 8), getResources().getColor(spot.getMarkerType().colorId));
        }
    }

    private void updateDistance() {
        if (userLocation != null) {
            Location spotLocation = new Location("spot");
            spotLocation.setLatitude(spot.position.latitude);
            spotLocation.setLongitude(spot.position.longitude);
            float distanceM = spotLocation.distanceTo(userLocation);

            if (PreferencesUtil.isUseMiles(getActivity())) {
                distance.setText(String.format("%.1f miles", distanceM / 1609.34));
            } else {
                distance.setText(String.format("%.1f km", distanceM / 1000));
            }
        }
    }

    private void updateTimeAgo() {
        if (spot.future) {
            title.setText(R.string.aboutToLeave);
            timeAgo.setVisibility(View.GONE);
        } else {
            title.setText(R.string.free_spot);
            timeAgo.setText(DateUtils.getRelativeTimeSpanString(spot.time.getTime()));
            timeAgo.setVisibility(View.VISIBLE);
        }
    }

    private void updateFollowButtonState() {

        boolean selected = spotsDelegate.isFollowing();

        MenuItem item = toolbar.getMenu().findItem(R.id.action_follow);
        item.setIcon(getResources().getDrawable(
                selected
                        ? R.drawable.ic_action_maps_navigation_accent
                        : R.drawable.ic_action_maps_navigation));

    }

}
