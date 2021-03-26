package com.example.thirdearoftruth.fragments;

import android.content.Intent;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.activities.DetectionActivity;
import com.example.thirdearoftruth.activities.MainActivity;
import com.example.thirdearoftruth.activities.CreateEventActivity;


/**
 * @author dermotbrennan
 *
 * This fragment serves as the home screen from which all other activities are navigated to.
 * If the user has opted to listen/detect and add, the 2 buttons leading to the associated actvities
 * to perform these tasks are shown.
 *
 * If the user has opted to simply receive notifications, they are simply presented with a greeting.
 * Future developments would see the addition of a timeline of all detections or a list of notifications
 * that have yet to be dismissed/ dealt with e.g. verifications of sounds detected or the adding of
 * a detected sound that does not exist in the database but was heard and recorded.
 */
public class HomeFragment extends Fragment {
    // declare variables
    private static final String TAG = "PROFILE_FRAGMENT";
    private static final String INTENT_CODE = "Main";

    Button btnRecord, btnDetect;
    TextView tvWelcome, tvTitle, tvSubtitle, tvMessage, tvMessagePt2;

    // value passed from main activity (from login)
    boolean receiving;


    // Methods

    /**
     * Associate the UI components with variables.
     * Retrieve the configuration boolean from the Bundle to determine whether the Buttons to Add
     * Acoustic Events and Detect Acoustic Events should be displayed or a Welcome Message.
     *
     * Define the onclick behaviour of the buttons- navigate to the relevant activity.
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // associate vars with ids
        tvWelcome = view.findViewById(R.id.welcome);
        tvTitle = view.findViewById(R.id.main_title);
        tvSubtitle = view.findViewById(R.id.subtitle);
        tvMessage = view.findViewById(R.id.message);
        tvMessagePt2 = view.findViewById(R.id.message2);
        btnRecord = (Button) view.findViewById(R.id.btn_record);
        btnDetect = (Button) view.findViewById(R.id.btn_detect);

        // get the boolean from the main
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            receiving  = bundle.getBoolean("receiving");
        }

        // replace buttons with a greeting if the user has opted to simply receive notifications
        if(receiving){
            tvMessage.setVisibility(View.VISIBLE);
            tvMessagePt2.setVisibility(View.VISIBLE);
            btnDetect.setVisibility(View.GONE);
            btnRecord.setVisibility(View.GONE);
        } else{
            tvMessage.setVisibility(View.GONE);
            tvMessagePt2.setVisibility(View.GONE);
            btnDetect.setVisibility(View.VISIBLE);
            btnRecord.setVisibility(View.VISIBLE);
        }


        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // add a string to the intent to let the next activity know that this was the source
                Intent createIntent = new Intent(getContext(), CreateEventActivity.class);
                createIntent.putExtra("parentName", INTENT_CODE);
                Log.d(TAG, "Add Sound Button Clicked. Going to CreateEventActivity");
                ((MainActivity) getActivity()).startActivity(createIntent);
            }
        });
        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Detection Button clicked", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Detect Sounds Button Clicked. Going to DetectionActivity");
                Intent detectIntent = new Intent(getContext(), DetectionActivity.class);
                ((MainActivity) getActivity()).startActivity(detectIntent);

            }
        });

        return view;


    } // end oncreate view



} // end fragment