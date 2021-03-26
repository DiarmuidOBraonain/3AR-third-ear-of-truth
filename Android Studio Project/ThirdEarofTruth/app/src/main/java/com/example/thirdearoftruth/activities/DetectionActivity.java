/**
 * This is an Android User Interface activity
 */
package com.example.thirdearoftruth.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.audio.DetectionService;
import com.example.thirdearoftruth.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * @author dermotbrennan
 *
 * The activity that handles the detection and recognition of sounds. The user may alter the starting
 * threshold calculation interval or leave it as the default value before clicking the listen button
 * thereby starting a background service that listens for acoustic events and tries to identify them based
 * on the data stored for saved acoustic events added by the user.
 *
 * Only one instance of this activity may exist at any one time to prevent crossover in services.
 * The listen button is also disabled after being clicked to prevent a second detection service from being
 * instantiated and causing overlap in database reads and audio input.
 */
public class DetectionActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "DETECTION";

    // firebase variables
    FirebaseUser mUser;
    DatabaseReference reference;
    String mUserId, mUserName;

    // UI vars
    SeekBar seekBar;
    TextView seekBarValue;

    Button btnDetect;
    Button btnStop;
    int min;
    int max;
    int current;

    private Chronometer chronometer;
    long timerOffset;


    // instance variables

    /**
     * Set by the user using the seekbar. This value is sent to
     */
    private int thresholdInterval;

    /**
     *
     * Setup UI and User variables, Associate variables with UI elements
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        setUpToolbarAndBackground();

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

        // setup seekbar with defined range and default value to start with
        min = 5;
        max = 30;
        current = 10;

        seekBar = (SeekBar) findViewById(R.id.seekbar);
        seekBar.setMax(max - min);
        seekBar.setProgress(current - min);
        seekBarValue = (TextView) findViewById(R.id.seekbar_value);
        String startingText = "New Threshold Every " + current + " Seconds";
        seekBarValue.setText(startingText);

        setThresholdInterval(current);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                current = progress + min;
                String progressText = "New Threshold Every " + current + " seconds";
                seekBarValue.setText(progressText);
                seekBar.setProgress(current - min);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //set the new threshold interval value to  the current int
                setThresholdInterval(current);
                sendNewThresholdIntervalToService();
            }
        }); // end seek bar progress conditions

        // buttons
        btnDetect = (Button) findViewById(R.id.btn_detect);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnStop.setEnabled(false);

        btnDetect.setOnClickListener(this);
        btnStop.setOnClickListener(this);


        // set up the onscreen timer
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("%s");
        chronometer.setBase(SystemClock.elapsedRealtime());


    } // end oncreate


    /**
     * Define the onclick behaviour of the buttons
     * @param v
     */
    @Override
    public void onClick(View v) {
        if (v == btnDetect) {

            startDetecting();

            btnDetect.setEnabled(false);
            btnStop.setEnabled(true);
            chronometer.setBase(SystemClock.elapsedRealtime() - timerOffset);
            chronometer.start(); // start the timer

            Log.d(TAG, "Button start clicked");
        } else if (v == btnStop) {
            stopDetecting();

            Log.d(TAG, "Button stop clicked");
            Intent mainIntent = new Intent(DetectionActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);

        } else {
            Log.d(TAG, "Unknown view");
        }

    } // end onClick method





    /**
     * Begins the detection. An underlying detection service with a thread containing a
     * Tarsos DSP audio dispatcher object starts and detects whether or not the given threshold has
     * been exceeded
     */
    public void startDetecting() {

        Intent detectionIntent = new Intent(this, DetectionService.class);
        detectionIntent.putExtra("threshold interval", thresholdInterval);

        startService(detectionIntent);
        // start the timer to let the user know how long the detection service has been running
        chronometer.setBase(SystemClock.elapsedRealtime() - timerOffset);
        chronometer.start();


    }



    /**
     * The underlying detection service is called to stop
     */
    public void stopDetecting() {
        Intent detectionIntent = new Intent(this, DetectionService.class);
        stopService(detectionIntent);

    }




    /**
     * Prepare the Animated Background and Toolbar features
     */
    public void setUpToolbarAndBackground() {
        //  animate the background
        RelativeLayout relativeLayout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) relativeLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        // setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.showOverflowMenu();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Detect Sounds");

    } // end toolbar and background




    /**
     * Returns the threshold interval set by the user
     *
     * @return
     */
    public int getThresholdInterval() {
        return thresholdInterval;
    }




    /**
     * Set the value of the threshold interval to be converted to milliseconds in the detection service.
     * The detection service will use this interval to determine how long to gather RMS values for before
     * calculating a new volume threshold for the listening environment
     *
     * @param thresholdInterval
     */
    public void setThresholdInterval(int thresholdInterval) {
        this.thresholdInterval = thresholdInterval;
        Log.d(TAG, "Threshold interval set at: " + thresholdInterval);
        Toast.makeText(DetectionActivity.this, "Threshold set after " + thresholdInterval+" seconds",
                Toast.LENGTH_SHORT).show();
    }


    /**
     * Sends an updated interval to the DetectionService to change the threshold calculation interval.
     * The thresholdInterval setter will be called in the service and the value changed to this one.
     * <p>
     * The timertask will then execute its task after this new interval: the volume threshold calculation.
     */
    public void sendNewThresholdIntervalToService() {
        Log.d("SENDER", "Broadcasting new threshold interval to service: " + thresholdInterval);
        Intent intervalIntent = new Intent("interval_change");
        intervalIntent.putExtra("new interval", thresholdInterval);
        // LocalBroadcastManager to send the broadcast to the service
        LocalBroadcastManager.getInstance(this).sendBroadcast(intervalIntent);
    }


    /**
     *
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    /**
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDetecting();
    } // end onDestroy


    /**
     *
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // go back to mainActivity but stop the service first
        stopDetecting();
        Log.d(TAG, "Cancelled by Clicking Home Button. Ending detection and going back to Main");
    } // end onBackPressed


} // end activity