/**
 * This is a Digital Audio Signal Processing Class
 */
package com.example.thirdearoftruth.audio;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.thirdearoftruth.R;
import com.example.thirdearoftruth.activities.DetectionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;

import static com.example.thirdearoftruth.notifications.InAppNotification.CHANNEL_ID;

/**
 * @author dermotbrennan
 *
 *
 * A background service that handles the detection of Acoustic Events/sounds in the user's listening
 * environment and calls instances of the Runnable class, Recognition Event to process the audiostream,
 * extract important audio information and compare it to the Acoustic Events/Sounds saved in the Firebase
 * Realtime Database by the user (or the default sounds added upon registration if they have not added any).
 *
 * On each execution of the VolumeProcessor's process() method, the relative power of the audio input
 * is added to an arraylist. A timertask with a period and delay specified by the user runs constantly
 * and executes a threshold calculation using the values in the aforementioned list and clears the list
 * to allow another execution of this calculation after the next interval.
 */
public class DetectionService extends Service {

    // declare variables
    private static final String TAG = "DETECTION_DISPATCHER";

    /**
     * VolleyRequestQueue passed to each instance of the RecognitionEvent class allowing push notifications
     * to be sent to any device this user has logged into and Selected 'Receiving' on.
     */
    private RequestQueue mRequestQueue;

    /**
     * The current logged in user
     */
    FirebaseUser mUser;


    // Audio variables
    /**
     * The primary TarsosDSP class for processing an audioInputStream. Reads the audio into a buffer with a
     * size/length specified before running at the specified Sampling rate and wraps it in an AudioEvent
     * class to extract commonly used audio data from it e.g. RMS, dbSPL and the number of samples, bytes and seconds
     * processed so far in the stream. Shifts each portion of the buffer to allow the stream to be read continuously.
     */
    private AudioDispatcher detectionDispatcher;

    /**
     * An atomic boolean that denotes whether a detection is currently happening or not. If the threshold
     * is exceeded and this is currently false, the start of the sound has just happened so it is set to true.
     * At this point, a new instance of RecognitionEvent will be called and started.
     *
     * If the value is true and the threshold is still exceeded, the sound is still occurring.
     * if the value is true and the threshold is no longer exceeded, the release phase of the sound is happening
     * and once the nearRelease variable reaches the specified limit, this will be set to false as the sound has ended.
     *
     */
    private AtomicBoolean detectionStarted;

    /**
     * The limit to which the currentRMS can be below the threshold while the detectedEvent AtomicBoolean
     * is still true. When nearRelease reaches this limit, detectedEvent will be set to false.
     */
    private static final int RELEASE = 10;
    /**
     * The integer value that increments once the currentRMS falls below the threshold while the
     * detectedEvent AtomicBoolean is still true.
     *
     * The purpose of these 2 variables is to prevent sharp cutoffs of the end of the AcousticEvent
     * a single event with multiple steep transients from being interpreted as separate events requiring
     * recognition and separate RecognitionEvent instances.
     */
    private static int nearRelease;

    /**
     * Thread to which the detectionDispatcher is passed in order to start it.
     */
    Thread detectionThread;

    /**
     * Thread to which the recognition event is passed in order to run while detectedEvent is true.
     * Where the bulk of the DSP will be performed on the audio data to extract features from the
     * Acoustic event/sound currently being detected and compare them to those of the user's saved
     * events to attempt to recognize it.
     */
    private static Thread mfccThread;


    /**
     * The class that handles the MFCC extraction concurrently during detection (while detectedEvent
     * is true). Once the stop() method is called, the recognition process occurs during which the
     * user's saved Acoustic Events/ sounds are read from Firebase RealTime Database and DTW is performed
     * on their MFCC matrices and the matrix of the event that has just been detected. The cost of the
     * distance for each is established and if they are below a specified cost limit, they are put into a
     * results set.
     *
     * If the results set is not empty, the AcousticEvent with the lowest distance cost is selected as
     * the best possible match and a notification is sent to the user's device on which they are subscribed to
     * push notifications.
     *
     * A new instance of this class are called every time detectedEvent becomes true (every time the
     * threshold is exceeded) to treat each detection as a separate event.
     */
    private static MFCCRecognitionEvent MFCCRecognitionEvent;


    // Threshold variables
    /**
     * The timer that is scheduled to run repeatedly and executes the Threshold calculation and setting
     * method after a Time interval specified by the user.
     */
    Timer timer;

    /**
     * The current relative power of this portion of the audio buffer. Acquired by performing the
     * Root Mean Square calculation on the buffer and is stored in the Tarsos DSP AudioEvent class.
     * Measured against the threshold after every calculation and is added to the list of RMS values
     * in order to establish a new threshold based on the changing, ambient noise level of the
     * listening environment.
     */
    private double currentRMS;

    /**
     * The value that the currentRMS must exceed in order to determine that an Acoustic Event has either
     * just begun, is in the middle of happening or
     */
    private double volumeThreshold;

    /**
     * The list to which all currentRMS values are added during the specified time period (interval).
     * After the timer executes the threshold calculation method on these values, the list is cleared
     * and begins to refill with new values for the next time interval.
     */
    ArrayList<Double> rmsValues;

    /**
     * The time period after which the threshold calculation will be executed. Initial value is specified
     * by the user in the DetectionActivity and can be updated/changed using the seekbar which broadcasts
     * the new value to this service. The Timertask will adjust how long it waits before executing accordingly.
     */
    private long thresholdInterval;


    // Logcat printing variables
    String rmsString;
    String threshString;

    // METHODS

    /**
     * Prepares variables of the Service for the detection process
     */
    @Override
    public void onCreate() {
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mRequestQueue = Volley.newRequestQueue(this);

        int sampleRate = 44100;
        int audioBufferSize = 2048;
        int bufferOverlap = 0;

        detectionStarted = new AtomicBoolean(false);
        nearRelease = 0;
        rmsValues = new ArrayList<>();

        // Starting value of the volumeThreshold
        volumeThreshold = 0.0029043591183558017; //0.005457633058228808

        // setup the dispatcher with the Android system's audioRecord in place of the JVM's AudioInputStream
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

            TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(sampleRate, 16,1, true, false);

            TarsosDSPAudioInputStream audioStream = new AndroidAudioInputStream(audioInputStream, format);

            audioInputStream.startRecording();

            detectionDispatcher = new AudioDispatcher(audioStream,audioBufferSize,bufferOverlap);
        }else{
            throw new IllegalArgumentException("Buffer size too small should be at least " + (minAudioBufferSize *2));
        }

        // Define the detection conditions in a new, anonymous AudioProcessor
        AudioProcessor volumeProcessor = new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                currentRMS = audioEvent.getRMS();
                rmsValues.add(currentRMS);
                rmsString = Double.toString(currentRMS);
                threshString = Double.toString(volumeThreshold);

                // if a sound is not detected and rms is greater than thresh - START EVENT
                if ((currentRMS > volumeThreshold) && (detectionStarted.get() == false)) {
                    detectionStarted.set(true);
                    MFCCRecognitionEvent = new MFCCRecognitionEvent(audioEvent, mUser, mRequestQueue);
                    mfccThread = new Thread(MFCCRecognitionEvent);
                    mfccThread.start();

                    Log.d(TAG, "START- Time :"+System.currentTimeMillis()+" Detection: "
                            +detectionStarted.get() + " OVER THRESHOLD: "+threshString
                            +"\t Current RMS: "+rmsString);

                } else if ((currentRMS > volumeThreshold) && (detectionStarted.get() == true)) { // DURING EVENT

                    nearRelease = 0;
                    MFCCRecognitionEvent.setAudioEvent(audioEvent);
                    mfccThread.interrupt();

                } else if ((currentRMS <= volumeThreshold) && (nearRelease >= RELEASE)) { // END EVENT

                    detectionStarted.set(false);
                    nearRelease = 0;
                    MFCCRecognitionEvent.stop();

                    Log.i("END DETECTION", " END- Time : "
                            +System.currentTimeMillis()+" Detection: " + detectionStarted.get());

                } else if ((detectionStarted.get() == true) && currentRMS <= volumeThreshold) {
                    // increment if this condition is true
                    nearRelease += 1;
                    MFCCRecognitionEvent.setAudioEvent(audioEvent);
                    mfccThread.interrupt();

                } else {
                    // Print RMS
                    Log.i(TAG, "Current RMS: " + rmsString);

                } // end if

                return true;

            } //  end process Method

            @Override
            public void processingFinished() {
                Log.i(TAG, "Processing finished");
            }
        }; // end AudioProcessor declaration

        detectionDispatcher.addAudioProcessor(volumeProcessor);
        detectionThread = new Thread(detectionDispatcher, "Detection AudioDispatcher");

        // Handle broadcasts from the DetectionActivity to change the threshold calculation interval
        // The user may wish to extend or reduce the time taken to calculate the threshold while the
        // detectionService is listening
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("interval_change"));


    } // end onCreate




    /**
     * Receive broadcasts from the DetectionActivity and set the Threshold calculation interval for
     * the timertask to the new value.
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent != null) {
                int calcInterval = intent.getIntExtra("new interval", 10);
                Log.i("RECEIVER", "Received integer value from broadcast :"+calcInterval);
                setThresholdInterval(calcInterval);
            } else{
                Log.i("RECEIVER", "Could not receive broadcast");
            }

        }
    }; // end Broadcast receiver




    /**
     * Starts the Detection Service when startService() is called in the DetectionActivity.
     * A broadcast manager and receiver listen for changes in the threshold interval set by the user
     * in the DetectionActivity and calls the ThresholdInterval's setter method.
     *
     * The timertask adjusts the time taken to gather threshold values accordingly.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // set the initial threshold calculation interval
        int interval = intent.getIntExtra("threshold interval", 10);
        setThresholdInterval(interval);


        // display a persistent notification so the user knows this device is detecting sounds and
        // sending notifications to any other devices they are logged in as Receivers
        Intent notificationIntent = new Intent(this, DetectionActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0 ,
                notificationIntent,
                0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Detecting")
                .setContentText("Listening for sounds to identify")
                .setSmallIcon(R.drawable.sound_notification)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);



        // Begin the listening and detection...
        detectionThread.start();

        // log the time started
        Log.i(TAG, "Detection Started : "+System.currentTimeMillis());



        // Timertask that executes the calculateThreshold method periodically based on the time
        // interval specified by the user in the DetectionActivity
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                volumeThreshold = calculateThreshold(rmsValues);

                Log.d(TAG, "Threshold set to : " + volumeThreshold);
                rmsValues.clear();
                // change interval to the value set by the user
            }
        }, thresholdInterval, thresholdInterval);

        Toast.makeText(DetectionService.this, "Listening for sounds...",
                Toast.LENGTH_SHORT).show();


        // DO NOT restart the service without user input first if it stops unexpectedly
        return START_NOT_STICKY;


    } // end onStartCommand




    /**
     * When stopService() is called in the DetectionActivity, the audioRecord supplying the
     * stream is closed, the dispatcher releases its assets and stops processing, thereby allowing
     * its processing chain to end correctly.
     *
     * If a recognition event is currently taking place, it ends and the regularly scheduled
     * volume threshold calculation Timer is cancelled
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // unregister the receiver as the service is about to stop
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        Log.i(TAG, "Detection is finished");
        timer.cancel();
        if (MFCCRecognitionEvent != null) {
            MFCCRecognitionEvent.stop();
        }
        releaseDispatcher();


    } // end onDestroy


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * Stops the detection dispatcher by checking its internal flag and assigns it a null value
     */
    private void releaseDispatcher() {
        if (!detectionDispatcher.isStopped()) {
            detectionDispatcher.stop();
        } // end inner if
        detectionDispatcher = null;
    } // end release Dispatcher


    /**
     * The period of time in milliseconds after which the threshold will be calculated
     *
     * @return
     */
    public long getThresholdInterval() {
        return thresholdInterval;
    }

    /**
     * The period of time in milliseconds
     *
     * @param interval
     */
    public void setThresholdInterval(int interval) {
        this.thresholdInterval = interval*1000;
        Log.d("SERVICE_THRESHOLD", "Interval has been set at "+interval+". Threshold will be calculated repeatedly after this many seconds.");
    }



    /**
     * Retrieves the Max and Min values of the collected RMS values of the input audio signal.
     * Calculates the difference between them and returns the minimum value+ 20% of the difference.
     * <p>
     * The percentage of the difference added to the minimum RMS may need to change based on
     * the performance of the system.
     *
     * @param rmsValues the ArrayList containing RMS values gathered from readDispatcher
     * @return a Double representing the new average loudness/ambience of the user's listening environment
     */
    public static Double calculateThreshold(ArrayList<Double> rmsValues) {
        double minRMS = Collections.min(rmsValues);
        double maxRMS = Collections.max(rmsValues);

        double difference = maxRMS - minRMS;

        double result = minRMS + (difference * 0.2);
        Log.d(TAG, "New Threshold is : " + result);
        return result;

    } // end calculate threshold


} // end detection service
