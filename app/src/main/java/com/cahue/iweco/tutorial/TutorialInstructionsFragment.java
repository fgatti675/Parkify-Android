package com.cahue.iweco.tutorial;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cahue.iweco.R;

/**
 * A simple {@link android.app.Fragment} subclass.
 * Use the {@link TutorialInstructionsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TutorialInstructionsFragment extends Fragment {

    private static final String ARG_TEXT_RES_ID = "ARG_TEXT_RES_ID";
    private static final String ARG_TYPE = "ARG_TYPE";

    public static final String TYPE_SPOTS = "SPOTS";

    public static final String TYPE_PARKING = "PARKING";

    private int textResId;

    private String type;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TutorialWelcome.
     */
    public static TutorialInstructionsFragment newInstance(int textResId, String type) {
        TutorialInstructionsFragment fragment = new TutorialInstructionsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TEXT_RES_ID, textResId);
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    public TutorialInstructionsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            textResId = getArguments().getInt(ARG_TEXT_RES_ID);
            type = getArguments().getString(ARG_TYPE);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tutorial_instructions, container, false);
        TextView textView = (TextView) view.findViewById(R.id.textView);
        textView.setText(textResId);
        return view;
    }

    public String getType() {
        return type;
    }
}
