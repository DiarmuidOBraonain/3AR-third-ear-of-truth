package com.example.thirdearoftruth.activities;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.writer.WriterProcessor;

/**
 * @author dermotbrennan
 *
 * The Activity responsible for the naming, recording and extraction of MFCCs for a new Acoustic Event
 * to be added by the User. After entering a name for this new Event, the user is prompted to click
 * the Record button which begins the recording of a wav file (to allow the User to hear the sound for
 * verification purposes) and the collection of the RMS of the signal over a period of 10 seconds.
 *
 * A countdown timer executes the CalculateTheshold method after the 10 seconds and the user is prompted
 * to begin making their sound. When an acoustic Event has been established, they are prompted to
 * click the stop button to move to the ConfirmEventActivity where they can upload or discard the
 * AcousticEvent as they see fit.
 */
public class CreateEventActivity extends AppCompatActivity {

    // declare constants
    private static final String TAG = "CREATE";
    private static final String RECORD_TAG = "RECORDING THREAD";

    private static final String WAV_FILES_DIR = "3AR";
    private static final int MINIMUM_EVENT_NAME_LENGTH = 3;
    /**
     * Threshold constant: high starting value to ensure no mfccs are gathered until after 10
     * seconds have elapsed and a threshold based on the user's environment has been established
     */
    private static final double STARTING_RMS_THRESHOLD = 1000.000000;

    // nearRelease increments when the system knows an event has occurred and the threshold is no
    // longer exceeded. Results in a less sharp cutoff of gathered audio data (mfccs)
    private static final int RELEASE = 10;
    private static int nearRelease;

    // firebase variables
    FirebaseUser mUser;
    DatabaseReference reference;
    String mUserId, mUserName;

    // Audio Capture variables
    int sampleRate;
    int audioBufferSize;
    int bufferOverlap;

    AudioDispatcher recordDispatcher;
    WriterProcessor writerProcessor;
    MFCC mfcc;
    AudioProcessor detectorProcessor;
    TarsosDSPAudioFormat tarsosDSPAudioFormat;
    File wavFile, wavFileDir;
    String fileName;

    // Threshold variables
    private AtomicBoolean detectionStarted;
    private static double volumeThreshold;
    private static double currentRMS;
    ArrayList<Double> rmsValues;


    // mfcc vars
    /**
     * The MFCC array as returned by the TarsosDSP MFCC AudioProcessor
     */
    private static float[] mfccsFloats;
    /**
     * The MFCC array converted to doubles
     */
    private static double[] mfccsAsDoubles;
    /**
     * The MFCC doubles boxed into a wrapped Double list
     */
    private static ArrayList<Double> mfccWrapperList;
    /**
     * The arrayList to hold each List of Wrapped Doubles of the MFCC. This will be unboxed to a
     * primitive 2d array of double [][] during the recognition phase.
     *
     * This conversion is necessary because Firebase Realtime database has NO NATIVE SUPPORT FOR
     * ARRAYS
     */
    ArrayList<ArrayList<Double>> recordedEventMfccList;
    int mfccCount;

    // AcousticEvent parameter variables
    String eventName;
    private long startTime;
    private long endTime;
    private double duration;

    // UI variables
    TextView indicator;
    EditText newAcousticEventName;
    Button btnRecord, btnStop;
    private Chronometer chronometer;
    long timerOffset;
    private boolean timerRunning;


    // Logcat printing variables
    String threshString;
    String rmsString;
    String mfccString;

    // Methods

    /**
     * Prepare the UI and associate any relevant components with variables.
     * Retrieve User information from Firebase Authentication
     *
     * Assign values to variables
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        // prepare the ui and buttons/textviews/edittext
        setupUiElements();
        initialiseButtons();

        // Create/ Assign the directory that the wave audio file will be recorded and stored in for playback
        wavFileDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+WAV_FILES_DIR);
        if(!wavFileDir.exists()){
            wavFileDir.mkdirs();
        }


        // connect to firebase and get user's details
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mUser != null) {
            mUserId = mUser.getUid();
        }
        reference = FirebaseDatabase.getInstance().getReference("Users").child(mUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                assert user != null;
                mUserName = user.getUsername();
            } // end onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            } // end onCancelled
        });

        // assign audio format variables
        sampleRate = 44100;
        audioBufferSize = 2048;
        bufferOverlap = 0;


        // assign starting threshold and detecttionStarted,  initialise lists of rms values and mfccs
        volumeThreshold = STARTING_RMS_THRESHOLD;
        nearRelease = 0;
        rmsValues = new ArrayList<>();
        recordedEventMfccList = new ArrayList<>();
        detectionStarted = new AtomicBoolean(false);


    } // end onCreate


    /**
     * Call the relevant support action bar and start the background animation
     */
    public void setupUiElements(){
        // animate the background
        RelativeLayout myLayout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) myLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        // setup the toolbar- handle the recording being interrupted if the back button is clicked.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.showOverflowMenu();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add A New Sound");

        // set up the onscreen timer
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("%s");
        chronometer.setBase(SystemClock.elapsedRealtime());


        // assign vars to ids
        indicator = findViewById(R.id.indicator);
        newAcousticEventName = findViewById(R.id.event_name);

        // make sure the user inputs text that
        newAcousticEventName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no work to be done here
            } // end beforeTextChanged
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().equals("")) {
                    btnRecord.setEnabled(false);

                } else if (s.toString().length() < MINIMUM_EVENT_NAME_LENGTH) {
                    Log.d(TAG, "Minimum event name length not reached, unable to record");
                    btnRecord.setEnabled(false);

                } else {
                    indicator.setText(R.string.indicator2);
                    btnRecord.setEnabled(true);

                } // end if
            } // end onTextChanged
            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length() < MINIMUM_EVENT_NAME_LENGTH) {
                    Toast.makeText(CreateEventActivity.this, "Name is too short! ", Toast.LENGTH_SHORT).show();
                }
            }// end aftertextchanged
        });


    } // end setup UI elements



    /**
     * Assign the buttons to variables and determine their onclick methods.
     */
    public void initialiseButtons(){
        btnRecord = findViewById(R.id.btn_record);
        btnRecord.setEnabled(false);
        btnStop = findViewById(R.id.btn_stop);
        btnStop.setEnabled(false);  // disable Stop button until the recording/timer has started and 10 seconds have passed



        // button onclick behaviour
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // log button click time for NFR test
                Log.d(TAG, "Record Button Clicked : "+System.currentTimeMillis());

                newAcousticEventName.setEnabled(false); //disable the edittext. MAY NEED TO REMOVE THIS!
                eventName = newAcousticEventName.getText().toString(); // get the name of the event that the user has chosen

                fileName = eventName + " " + System.currentTimeMillis()/1000 +".wav"; // get the name of the event and create the wav file using it
                wavFile = new File(wavFileDir, fileName);


                startRecording(); // start recording the stream and getting RMS values to calculate threshold
                chronometer.setBase(SystemClock.elapsedRealtime() - timerOffset);
                chronometer.start(); // start the timer
                timerRunning = true; // change the flag

                Toast.makeText(CreateEventActivity.this,
                        "Calibrating. Please allow 10 seconds of silence...",
                        Toast.LENGTH_LONG).show();

                //Set Countdown Timer for 10 seconds
                new CountDownTimer(10000, 10) {
                    public void onTick(long millisUntilFinished) {
                        indicator.setText(R.string.indicator3);
                    }
                    @Override
                    public void onFinish() {

                        volumeThreshold = calculateThreshold(rmsValues);
                        Log.i(TAG, "New threshold calculated as "+volumeThreshold);
                        Toast.makeText(CreateEventActivity.this,
                                "Make a sound and hit STOP",
                                Toast.LENGTH_SHORT).show();
                        indicator.setText(R.string.indicator4);

                    } // end countdown onFinish
                }.start();


            } // end record button onClick
        }); // end BUTTON RECORD ONCLICK


        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //log time when button is clicked
                Log.d(TAG, "Stop Button Clicked : "+System.currentTimeMillis());

                // stop the recording and release assets
                stopRecording();
                if(!recordedEventMfccList.isEmpty()) {

                    Intent confirmIntent = new Intent(CreateEventActivity.this,
                            ConfirmEventActivity.class);
                    // pass Acoustic Event parameters to the next activity
                    confirmIntent.putExtra("eventName", eventName);
                    confirmIntent.putExtra("duration", duration);
                    confirmIntent.putExtra("data", new Gson().toJson(recordedEventMfccList) );
                    confirmIntent.putExtra("mfccCount", mfccCount);
                    confirmIntent.putExtra("wav file name", fileName);

                    // move on to confirmation
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(confirmIntent);
                    finish();
                }
            } // end onClick
        }); // end BUTTON STOP ONCLICK listener



    } // end initialise buttons





    /**
     * starts the recordDispatcher
     */
    public void startRecording() {
        if (!timerRunning) {
            timerRunning = true;
            btnRecord.setEnabled(false);

            // STEP 1: setup the dispatcher with the Android system's audioRecord in place of the JVM's AudioInputStream
            // STEP 2: record the sound and measure the RMS
            // STEP 3: calculate environment Threshold
            // STEP 4: add MFCCs as an ArrayList<Double> to a List if this new Threshold is exceeded
            int minAudioBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT);
            int minAudioBufferSizeInSamples =  minAudioBufferSize/2;

            if(minAudioBufferSizeInSamples <= audioBufferSize ){
                AudioRecord audioInputStream = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, sampleRate,
                        android.media.AudioFormat.CHANNEL_IN_MONO,
                        android.media.AudioFormat.ENCODING_PCM_16BIT,
                        audioBufferSize * 2);

                tarsosDSPAudioFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);

                TarsosDSPAudioInputStream audioStream = new AndroidAudioInputStream(audioInputStream, tarsosDSPAudioFormat);
                //start recording ! Opens the stream.
                audioInputStream.startRecording();
                recordDispatcher = new AudioDispatcher(audioStream, audioBufferSize, 0);
            }else{
                throw new IllegalArgumentException("Buffer size too small should be at least " + (minAudioBufferSize *2));
            }

            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(wavFile, "rw");
                writerProcessor = new WriterProcessor(tarsosDSPAudioFormat, randomAccessFile);
                mfcc = new MFCC(audioBufferSize, sampleRate, 13, 20, 133.33f, 8000f);
                detectorProcessor = new AudioProcessor() {
                    @Override
                    public void processingFinished() {
                        Log.d(TAG, "recording ended");
                    }

                    @Override
                    public boolean process(AudioEvent audioEvent) {
                        // assign the RMS and MFCC using this frame block of the buffer
                        currentRMS = audioEvent.getRMS();
                        // add to list
                        rmsValues.add(currentRMS);

                        // extract MFCC and convert
                        mfccsFloats = mfcc.getMFCC();
                        mfccsAsDoubles = convertFloatsToDoubles(mfccsFloats);
                        mfccWrapperList = new ArrayList<Double>(mfccsAsDoubles.length);
                        for(double d : mfccsAsDoubles) {
                            mfccWrapperList.add(Double.valueOf(d));
                        }


                        //Strings for Log messages
                        threshString = Double.toString(volumeThreshold);
                        rmsString = Double.toString(currentRMS);
                        mfccString = Arrays.toString(mfccsAsDoubles);

                        // if a sound is not detected and rms is greater than thresh - START EVENT

                        if ((currentRMS > volumeThreshold)&& (detectionStarted.get()==false)) {

                            detectionStarted.set(true);
                            startTime = System.currentTimeMillis(); // get the start time here
                            recordedEventMfccList.add(mfccWrapperList);

                            Log.d(RECORD_TAG, "START- Detection: " + detectionStarted.get() +
                                    " OVER THRESHOLD: " + threshString + "\t Current RMS: " +
                                    rmsString +" Mfcc: "+mfccString);

                        } else if((currentRMS > volumeThreshold) && (detectionStarted.get()==true)) { // DURING

                            recordedEventMfccList.add(mfccWrapperList);
                            nearRelease=0;
                            Log.i("DURING", "Detection : "+detectionStarted.get()
                                    +" Mfcc : "+mfccString);

                        } else if((currentRMS <= volumeThreshold) && (nearRelease >= RELEASE)) { // END

                            detectionStarted.set(false);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // enable button when event ends
                                    btnStop.setEnabled(true);
                                    Log.i("END","Button stop is now enabled");
                                    indicator.setText(R.string.indicator5);
                                }
                            });

                            nearRelease=0;
                            Log.i("END DETECTION", " END- Detection: " + detectionStarted.get());

                        }else if((detectionStarted.get()==true) && currentRMS <= volumeThreshold){ // RELEASE

                            nearRelease+=1;
                            recordedEventMfccList.add(mfccWrapperList);
                            Log.i("DIPPED BELOW", "Near Release : "+nearRelease+
                                    " Detection : "+detectionStarted.get()+" Mfcc"+mfccString);
                        } else{

                            Log.i(RECORD_TAG,"No detection yet");
                        }
                        return true;
                    }
                };

                recordDispatcher.addAudioProcessor(writerProcessor);
                recordDispatcher.addAudioProcessor(mfcc);
                recordDispatcher.addAudioProcessor(detectorProcessor);

                Thread recordThread = new Thread(recordDispatcher, "Record Audio Thread");
                recordThread.start();

                // log time at which the recording actually started
                Log.d(TAG, "Recording/Extraction START : "+System.currentTimeMillis());
                Log.d(TAG, "Recording thread started. Audio will now be written to a file");

            } catch (IOException e) {
                Log.e(RECORD_TAG, e.getMessage());
            }

        } // end if
    } // end startRecording




    /**
     * Stops the recordingDispatcher and the timer/chronometer
     */
    public void stopRecording() {
        if (timerRunning) {
            chronometer.stop();

            releaseRecordDispatcher();

            // measure the duration of the event
            endTime = System.currentTimeMillis(); // get the end time here

            long elapsedTime = endTime - startTime;
            duration = (double) elapsedTime /1000.0;


            mfccCount = recordedEventMfccList.size(); // check the size of the list of mfccs
            Toast.makeText(CreateEventActivity.this, "Recording Ended", Toast.LENGTH_SHORT).show();
            chronometer.setBase(SystemClock.elapsedRealtime());
            timerRunning = false; // stop the timer

        } // end if
    } // end stop recording



    /**
     * Stops the dispatcher responsible for the recording of the audio as a wav file
     */
    public void releaseRecordDispatcher() {
        // end the reading
        if (recordDispatcher != null) {
            if (!recordDispatcher.isStopped()) {
                recordDispatcher.stop();

                //log time recording stopped for NFR test
                Log.d(TAG, "Recording/Extraction END : "+System.currentTimeMillis());

            } // end inner if
        } // end outer if

    } // end release record



    /**
     * Retrieves the Max and Min values of the collected RMS values of the input audio signal.
     * Calculates the difference between them and returns the minimum value+ 20% of the difference.
     *
     * The percentage of the difference added to the minimum RMS may need to change based on
     * the performance of the system.
     *
     * @param rmsValues the ArrayList containing RMS values gathered from readDispatcher
     * @return a Double representing the new average loudness/ambience
     */
    public static Double calculateThreshold(ArrayList<Double> rmsValues) {
        double minRMS = Collections.min(rmsValues);
        double maxRMS = Collections.max(rmsValues);

        double difference = maxRMS - minRMS;

        return minRMS+(difference*0.2);
        //return maxRMS;
    }

    /**
     * Converts the mfcc float array to doubles in order to be sent through the DTW
     * class
     *
     * @param input an array of floats representing the mfcc output
     * @return a double array of mfccs
     */
    public static double[] convertFloatsToDoubles(float[] input) {
        if (input == null) {
            return null; // Or throw an exception
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i];
        }
        return output;
    }




    /**
     * Take the user back to the main menu, but this functionality is only permitted BEFORE the
     * record button has been clicked to prevent crashing.
     */
    @Override
    public void onBackPressed() {
        //if(shouldAllowBack){
            super.onBackPressed();
            if(timerRunning){
                releaseRecordDispatcher();
                File cancelFile = new File(wavFileDir, fileName);
                if (cancelFile.exists()) {
                    cancelFile.delete();
                } // end inner if
            } // end outer if
    } // end onBackPressed



}