package com.cahue.iweco.places;

import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.R;
import com.cahue.iweco.SpotsDelegate;
import com.cahue.iweco.util.PreferencesUtil;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlaceDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlaceDetailsFragment extends DetailsFragment implements Toolbar.OnMenuItemClickListener {

    // the fragment initialization parameters
    private static final String ARG_PLACE = "ARG_PLACE";
    private static final String ARG_USER_LOCATION = "ARG_USER_LOCATION";

    @Nullable
    private Location userLocation;
    @Nullable
    private Place place;

    private TextView title;
    private TextView address;
    private TextView distance;

    private SpotsDelegate spotsDelegate;

    public PlaceDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param place Parameter 1.
     * @return A new instance of fragment MarkerDetailsFragment.
     */
    @NonNull
    public static DetailsFragment newInstance(Place place, Location userLocation) {
        DetailsFragment fragment = new PlaceDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PLACE, place);
        args.putParcelable(ARG_USER_LOCATION, userLocation);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCameraUpdate() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            place = getArguments().getParcelable(ARG_PLACE);
            userLocation = getArguments().getParcelable(ARG_USER_LOCATION);
        }

        spotsDelegate = (SpotsDelegate) getFragmentManager().findFragmentByTag(SpotsDelegate.FRAGMENT_TAG);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_place_details, container, false);
        // Set time ago
        title = (TextView) view.findViewById(R.id.name);
        address = (TextView) view.findViewById(R.id.address);
        distance = (TextView) view.findViewById(R.id.distance);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
        view.startAnimation(fadeInAnimation);

        if (place != null) {
            title.setText(place.name);
            address.setText(place.address);
            updateDistance();
        }

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        updateDistance();
    }

    @Override
    public void setUserLocation(@Nullable Location userLocation) {
        this.userLocation = userLocation;
        View view = getView();
        if (view != null) {
            updateDistance();
        }
    }

    @Override
    public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
        // Handle presses on the action bar items
        switch (menuItem.getItemId()) {
            case R.id.action_follow:
                spotsDelegate.setCameraFollowing(true);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void updateDistance() {
        if (userLocation != null) {
            float distanceM = place.location.distanceTo(userLocation);

            if (PreferencesUtil.isUseMiles(getActivity())) {
                distance.setText(String.format("%.1f miles", distanceM / 1609.34));
            } else {
                distance.setText(String.format("%.1f km", distanceM / 1000));
            }
        }
    }

}
