package com.cahue.iweco.tutorial;


import android.app.Fragment;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link android.app.Fragment} subclass.
 * Use the {@link TutorialInstructionsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TutorialInstructionsFragment extends Fragment {

    public static final String TYPE_SPOTS = "SPOTS";
    public static final String TYPE_PARKING = "PARKING";
    private static final String ARG_LAYOUT_ID = "ARG_LAYOUT_ID";
    private static final String ARG_TYPE = "ARG_TYPE";

    @LayoutRes
    private int layoutId;

    @NonNull
    private String type;

    public TutorialInstructionsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TutorialWelcome.
     */
    @NonNull
    public static TutorialInstructionsFragment newInstance(@LayoutRes int layoutId, String type) {
        TutorialInstructionsFragment fragment = new TutorialInstructionsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_ID, layoutId);
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            layoutId = getArguments().getInt(ARG_LAYOUT_ID);
            type = getArguments().getString(ARG_TYPE);
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(layoutId, container, false);
    }

    @NonNull
    public String getType() {
        return type;
    }
}
