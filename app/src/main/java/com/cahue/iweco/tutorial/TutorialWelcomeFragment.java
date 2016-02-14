package com.cahue.iweco.tutorial;


import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
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

    public TutorialWelcomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TutorialWelcome.
     */
    @NonNull
    public static TutorialWelcomeFragment newInstance() {
        TutorialWelcomeFragment fragment = new TutorialWelcomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_tutorial_welcome, container, false);
        AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
        alpha.setDuration(400);
        alpha.setFillAfter(true);
        view.findViewById(R.id.textView).startAnimation(alpha);
        final View logo = view.findViewById(R.id.logo);
        logo.setVisibility(View.INVISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
                alpha.setDuration(800);
                alpha.setFillAfter(true);
                logo.setVisibility(View.VISIBLE);
                logo.startAnimation(alpha);
            }
        }, 400);
        return view;
    }

}
