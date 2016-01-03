package com.cahue.iweco;

import android.app.Fragment;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cahue.iweco.cars.CarManagerActivity;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NoCarsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NoCarsFragment extends DetailsFragment {


    public NoCarsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MarkerDetailsFragment.
     */
    public static NoCarsFragment newInstance() {
        NoCarsFragment fragment = new NoCarsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.layout_no_cars, container, false);
        view.findViewById(R.id.add_car).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), CarManagerActivity.class));
            }
        });
        return view;
    }


    @Override
    public void setUserLocation(Location userLocation) {

    }
}
