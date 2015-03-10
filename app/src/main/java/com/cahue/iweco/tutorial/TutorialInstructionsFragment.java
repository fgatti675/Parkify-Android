package com.cahue.iweco.tutorial;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cahue.iweco.R;

/**
 * A simple {@link android.app.Fragment} subclass.
 * Use the {@link com.cahue.iweco.tutorial.TutorialInstructionsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TutorialInstructionsFragment extends Fragment {


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TutorialWelcome.
     */
    public static TutorialInstructionsFragment newInstance() {
        TutorialInstructionsFragment fragment = new TutorialInstructionsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public TutorialInstructionsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tutorial_instructions, container, false);
    }


}
