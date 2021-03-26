/**
 * This is an Android User Interface activity
 */
package com.example.thirdearoftruth.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.models.AcousticEvent;
import com.example.thirdearoftruth.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;

/**
 * @author dermotbrennan
 *
 * The activity in which the user can play back the sound they just recorded to check if it was correct
 * and confirm that they wish to save it in the database to be used in recognition, or cancel after
 * which the wav file of the recording is discarded.
 */
public class ConfirmEventActivity extends AppCompatActivity {

    // constant variables
    public static final String TAG = "CONFIRM";

    /**
     * During recognition, Dynamic Time Warping is performed using stored Acoustic Event's
     * mfcc feature vector and the feature vector of the new acoustic event and a cost of the distance
     * between them is established.
     * In order to establish a result, the acoustic event's cost must be the lowest. By having a limit
     * such as this, if the cost of this Acoustic Event is above it it ought not be considered in the results.
     *
     * For future development, it is useful to have a value to update after successful identifications
     * such as this one in order to increase probability of future accurate identifications of this
     * Acoustic Event.
     */
    public static final double DEFAULT_MAX_COST = 300.0;

    /**
     * The directory of the stored wav file recording of the acoustic event the user just recorded
     */
    private static final String WAV_FILES_DIR = "3AR";

    // UI variables
    TextView soundName;
    ImageButton playButton;
    Button btnConfirm, btnCancel;

    // Firebase variables
    FirebaseUser mUser;
    DatabaseReference reference, eventDatabaseRef;
    String mUserId, mUserName;

    // file variables
    String eventName, wavFileName;
    File wavFile, wavFileDir;

    // Acoustic Event Variables
    int mListSize;
    double eventDuration;
    ArrayList<ArrayList<Double>> eventMfccList;
    String mfccListSize;

    // Audio Playback
    AudioDispatcher dispatcher;
    AudioProcessor playerProcessor;
    TarsosDSPAudioFormat tarsosDSPAudioFormat;


    // METHODS

    /**
     * Prepares the UI and associates any relevant components with variables.
     * Acoustic Event data created in the previous Activity is retrieved from the Intent, the path
     * of the wav file recording is called and the Firebase Authentication credentials are retrieved.
     *
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_event);
        setUpToolbarAndBackground();

        // connect to firebase and get user's details
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mUser != null) {
            mUserId = mUser.getUid();
        }
        reference = FirebaseDatabase.getInstance().getReference("Users").child(mUserId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                assert user != null;
                mUserName = user.getUsername();
            } // end onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, error.getMessage());
            } // end onCancelled
        });

        // prepare database references for the Acoustic Event to be uploaded
        eventDatabaseRef = FirebaseDatabase.getInstance().getReference("AcousticEvents").child(mUserId);

        // Retrieve the event name, duration and mfcc list size of the event/sound from intent
        Intent eventCreate = getIntent();
        eventDuration = eventCreate.getDoubleExtra("duration", 0.0);
        eventName = eventCreate.getStringExtra("eventName");

        String extra = eventCreate.getStringExtra("data");
        eventMfccList = new Gson().fromJson(extra, new TypeToken<ArrayList<ArrayList<Double>>>() {
        }.getType()); // get the recorded mfccs from the last activity

        mListSize = eventCreate.getIntExtra("mfccCount", 0);
        mfccListSize = String.valueOf(mListSize); // change to string to allow storage in database (Firebase does not store ints)

        // get the name of the recorded wav file and its directory for playback purposes
        wavFileName = eventCreate.getStringExtra("wav file name");
        wavFileDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + WAV_FILES_DIR);
        wavFile = new File(wavFileDir, wavFileName);


        initialiseTextViews();
        initialiseButtons();



    } // end onCreate


    /**
     * Creates a new AcousticEvent object and pushes it to the Firebase Realtime Database.
     * The Acoustic Event is associated with this user through the use of their Firebase Authentication
     * User ID as the parent node, with this event's push ID as the child node of the parent.
     */
    private void uploadAcousticEvent() {

        String eventId = eventDatabaseRef.push().getKey();
        DatabaseReference finalReference = eventDatabaseRef.child(eventId);

        // create the Acoustic event and upload to Realtime Database
        AcousticEvent acousticEvent = new AcousticEvent(eventId, eventName, eventDuration,
                eventMfccList, mfccListSize, DEFAULT_MAX_COST, false);

        finalReference.setValue(acousticEvent);
        Log.d(TAG, "Upload Completed : "+System.currentTimeMillis());
        Log.d(TAG, "New AcousticEvent uploaded to Firebase");

    } // end upload file



    /**
     * Prepare the Animated Background and Toolbar features
     */
    public void setUpToolbarAndBackground() {
        //  animate the background
        RelativeLayout linearLayout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) linearLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        // setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.showOverflowMenu();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle("Confirm New Sound");

    } // end toolbar and background

    /**
     * Set up the text views that rely on variables passed to this intent
     */
    public void initialiseTextViews() {
        soundName = findViewById(R.id.sound_name);
        soundName.setText(eventName);

    } // end textview setup


    /**
     * Initialise and assign the onclick behaviour of all buttons in this activity
     */
    public void initialiseButtons(){
        // Associate variables with buttons in layout file
        playButton = findViewById(R.id.btn_play);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnCancel = findViewById(R.id.btn_cancel);


        //play button
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // log time when button was clicked for NFR test
                Log.d(TAG, "Play Button Clicked : "+System.currentTimeMillis());
                playAudio();
            }
        });

        // confirmation button uploads the mfccfile to storage, creates a child reference in
        // RealTime Database of the acoustic event and returns to the main activity.
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "Confirm Button Clicked : "+System.currentTimeMillis());
                // upload the acoustic event to database
                uploadAcousticEvent();

                // dispose of the file as it will not be used again
                File wavFile = new File(wavFileDir, wavFileName);
                if (wavFile.exists()) {
                    wavFile.delete();
                }

                // confirm that the sound was added and remind the user to add the sound again!
                AlertDialog.Builder builder = new AlertDialog.Builder(ConfirmEventActivity.this);
                builder.setTitle("Sound Added!");
                builder.setMessage("This sound was successfully saved to your library. " +
                        "Record it again from different positions in your home to make sure 3AR can recognise it!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mainIntent = new Intent(ConfirmEventActivity.this, MainActivity.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mainIntent);
                    } // end onSuccess
                });
                builder.create().show(); // finally show the dialog to the user

            }
        });


        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // delete the wav File as it will not be used again
                // log button click time for NFR
                Log.d(TAG, "Cancel Button Clicked : "+System.currentTimeMillis());
                File wavFile = new File(wavFileDir, wavFileName);
                if (wavFile.exists()) {
                    wavFile.delete();
                }

                //head back to main
                Toast.makeText(ConfirmEventActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                Intent mainIntent = new Intent(ConfirmEventActivity.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainIntent);

                //log the time taken to cancel for NFR
                Log.d(TAG, "Cancel Completed : "+System.currentTimeMillis());
            }
        });


    } // end initialiseButtons method



    /**
     * Takes the wav file recorded by the User and plays it back to them when clicked.
     * Restarts audio playback if clicked more than once.
     *
     * Be aware that it plays back the 10 seconds of ambience while the noise threshold was established
     * in the previous activity because the audiorecord object cannot be instantiated twice.
     */
    public void playAudio() {
        try {
            releaseDispatcher();
            tarsosDSPAudioFormat = new TarsosDSPAudioFormat(44100, 16, 1, true, false);
            FileInputStream fileInputStream = new FileInputStream(wavFile);
            dispatcher = new AudioDispatcher(new UniversalAudioInputStream(fileInputStream, tarsosDSPAudioFormat), 2048, 0);
            playerProcessor = new AndroidAudioPlayer(tarsosDSPAudioFormat, 2048, 0);
            dispatcher.addAudioProcessor(playerProcessor);

            Thread playingThread = new Thread(dispatcher, "Playing Audio Thread");
            playingThread.start();
            // log time for NFR test
            Log.d(TAG, "Playback of Recorded Audio START : "+System.currentTimeMillis());
            Toast.makeText(ConfirmEventActivity.this, "Playing back recorded sound...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            String error = e.getMessage();
            assert error != null;
            Log.e(TAG, error);
        }
    } // end play audio method

    /**
     * Stops the dispatcher responsible for playing audio by checking its internal isStopped flag
     */
    public void releaseDispatcher() {
        if (dispatcher != null) {
            if (!dispatcher.isStopped()) {
                dispatcher.stop();
            } // end inner if
            dispatcher = null;
        } // end outer if
    } // end release


    /**
     * Method that ensures playback of the recording ends and the AudioDispatcher's assets are released
     */
    @Override
    protected void onStop() {
        super.onStop();
        releaseDispatcher();
    }





} // end activity