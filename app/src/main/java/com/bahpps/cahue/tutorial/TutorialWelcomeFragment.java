package com.bahpps.cahue.tutorial;


import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;

import com.bahpps.cahue.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TutorialWelcomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TutorialWelcomeFragment extends Fragment {


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TutorialWelcome.
     */
    public static TutorialWelcomeFragment newInstance() {
        TutorialWelcomeFragment fragment = new TutorialWelcomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public TutorialWelcomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tutorial_welcome, container, false);
        AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
        alpha.setDuration(1200);
        alpha.setFillAfter(true);
        view.findViewById(R.id.textView).startAnimation(alpha);
        return view;
    }

}
