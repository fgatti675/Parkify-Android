package com.cahue.iweco.tutorial;


import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;

import com.cahue.iweco.R;

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
        final View view = inflater.inflate(R.layout.fragment_tutorial_welcome, container, false);
        AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
        alpha.setDuration(2000);
        alpha.setFillAfter(true);
        view.findViewById(R.id.textView).startAnimation(alpha);
        final View logo = view.findViewById(R.id.logo);
        logo.setVisibility(View.INVISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
                alpha.setDuration(3000);
                alpha.setFillAfter(true);
                logo.setVisibility(View.VISIBLE);
                logo.startAnimation(alpha);
            }
        }, 1000/* 1sec delay */);
        return view;
    }

}
