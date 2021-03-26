/**
 * This is a Digital Audio Signal Processing Class
 */
package com.example.thirdearoftruth.audio;

import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.thirdearoftruth.marytts.DTW;
import com.example.thirdearoftruth.models.AcousticEvent;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.mfcc.MFCC;

/**
 * @author dermotbrennan
 *
 *
 * This class handles the feature extraction and Recognition process when a sound is detected in the
 * Detection Service class using the Tarsos DSP AudioDispatcher, AudioProcessor and AudioEvent classes.
 *
 * This class runs concurrently with the Dispatcher in the Detection Service and shares the audioEvent
 * currently being processed by its AudioProcessor chain until it determines that the current RMS (power/volume) has
 * both fallen back below the specified threshold and has done so for a long enough period of time to
 * safely assume the Acoustic Event has finished.
 *
 * Extends Detection as this is expected to be 1 of potentially many Recognition behaviours, while
 * this one focuses solely on extraction of MFCCs as the acoustic feature and Dynamic Time Warping
 * as the Recognition methodology
 */
public class MFCCRecognitionEvent extends Detection implements Runnable {

    private static final String TAG = "RECOGNITION_EVENT";
    private static boolean detectedEvent;


    /**
     * the current block of frames/ the buffer to be processed
     */
    AudioEvent audioEvent;


    /**
     * the results of the DTW comparisons stored with their corresponding Acoustic
     * Events
     */
    private Map<AcousticEvent, Double> results;

    /**
     * the arrayList of double[] to add each MFCC to. A 2d array suitable
     * for DTW will be formed from it in the getResult() method
     */
    ArrayList<double[]> mfccList;

    /**
     * The mfccList converted to a 2D array suitable for Dynamic Time Warping
     */
    private double[][] detectedEventMfccs;

    /**
     * the Tarsos DSP MFCC audioprocessor that produces an mfcc for each
     * block of frames fed into it
     */
    private MFCC mfcc;


    /**
     * The MFCC produced by the MFCC processor
     */
    float[] mfccs;

    /**
     * the MFCC converted to a double[] to be added to the list
     */
    double[] mfccsAsDoubles;


    /**
     * The volley requestQueue to which the notification holding the result of this recognition process
     * will be sent to
     */
    private RequestQueue mRequestQueue;

    /**
     * The Firebase Messaging push notification URL to connect to Cloud Messaging and
     * handle the sending of push notifications/ topic notifications
     */
    private String notificationUrl;

    /**
     * The currently logged in user to which notifications will be sent
     */
    FirebaseUser mUser;

    /**
     * The reference to the Firebase Realtime Database containing all the known acoustic events for this user
     */
    DatabaseReference mDatabaseReference;

    /**
     * A list of the user's known acoustic events read from the Firebase Realtime Database
     *
     */
    List<AcousticEvent> acousticEventList;

    /**
     * Constructor to which the first audioEvent must be passed when this runnable is called in the
     * Detection service. Other The requestQueue and current logged-in user are passed from the service as
     * well to alleviate any unnecessary repetition of code.
     *
     */
    public MFCCRecognitionEvent(AudioEvent audioEvent, FirebaseUser firebaseUser, RequestQueue requestQueue) {
        // the current audioEvent, firebaseUser and Volley requestQueue are inherited from the detection
        super(audioEvent, firebaseUser, requestQueue);

        this.audioEvent = super.getAudioEvent();

        // URL used to connect to Cloud Messaging
        notificationUrl = "https://fcm.googleapis.com/fcm/send";

        detectedEvent = true;

        acousticEventList = new ArrayList<>();

        results = new HashMap<>();

        mfccList = new ArrayList<double[]>();

        mfcc = new MFCC(2048, 44100, 13, 20, 133.33f, 8000f);


    } // end constructor



    // getters and setters

    /**
     * @return the audioEvent
     */
    public AudioEvent getAudioEvent() {
        return audioEvent;
    }



    /**
     * Retrieve the new mfcc feature vector as a 2d array
     * @return detectedEventMfccs, the 2d array
     */
    public double[][] getDetectedEventMfccs() {
        return detectedEventMfccs;
    }

    /**
     * convert the List of double[] representing the mfccs of this detected Acoustic Event into
     * a 2d array of type double [][].
     *
     *
     * The newly detected event can now be passed into the MaryTTS Dynamic Time Warping class with each
     * Acoustic Event stored in the database by the user to establish a potential match.
     *
     *
     * @param mfccList
     */
    public void setDetectedEventMfccs(ArrayList<double[]> mfccList) {

        double[][] mfcc2DArray = new double[mfccList.size()][];
        for (int i = 0; i < mfccList.size(); i++) {
            mfcc2DArray[i] = mfccList.get(i);
        }

        this.detectedEventMfccs = mfcc2DArray;

    } // end setDetectedEventMfccs method


    // Methods

    /**
     * This method handles the processing and collection of MFCCs from the audioEvent (current portion
     * of the audio buffer from the stream being shared between the detection service and this runnable).
     *
     * While running, each time the audioEvent is set during the processing in the Detection Service,
     * the MFCC processes the float buffer within the audioEvent before being interrupted. It will sleep until
     * the audioEvent is set again which happens roughly every 46 milliseconds and is virtually unnoticeable.
     *
     * This continues while the AtomicBoolean detectedEvent is still true, and when the stop() method is called
     * in the Detecttion Service this AtomicBoolean is set to false, thereby ending this loop and allowing the
     * Recognition calculation to occur.
     *
     */
    @Override
    public void run() {
        boolean paused;

        while(detectedEvent==true) {
            String threadName = Thread.currentThread().getName();
            mfcc.process(audioEvent);
            mfccs = mfcc.getMFCC();
            mfccsAsDoubles = convertFloatsToDoubles(mfccs);
            mfccList.add(mfccsAsDoubles);
            Log.d(TAG,threadName+" "+ Arrays.toString(mfccsAsDoubles));
            paused = true;

            while(paused) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    paused = false;
                }
            }
        } // end while
    } // end run


    /**
     * Called when the detectedEvent atomicboolean in the main
     * detection service becomes false.
     *
     * Create a method where the list is added to dtw
     */
    public void stop() {

        detectedEvent = false;
        audioEvent = null;
        Log.d(TAG,"List of mfccs created : "+mfccList.size());
        setDetectedEventMfccs(mfccList);

        identifyAcousticEvent();

    } // end stop method


    /**
     * The recognition process is initiated from here.
     * The Acoustic Event's stored by the user in the Firebase Realtime Database are read and adapted
     * into AcousticEvent objects to hold the relevant audio data and information to relay back to the
     * user.
     *
     * Each acousticEvent stored by the user is compared to the new sound using their mfccs
     * stored in feature vectors/matrices/2d arrays. The distance between them is established and
     * set as the "Cost", after which the AcousticEvent with the minimum cost is established.
     *
     * If the cost of the Distance between this final Acoustic Event and the newly detected event is
     * small enough to be deemed a probable match, a notification is sent to the user informing them
     * that the sound has occurred in their listening environment.
     *
     */
    public void identifyAcousticEvent() {

        mUser = super.getmFirebaseUser();

        Log.d(TAG, "DTW Recognition Process START : "+System.currentTimeMillis());

        mDatabaseReference = FirebaseDatabase.getInstance().getReference("AcousticEvents").child(mUser.getUid());
        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                acousticEventList.clear();
                results.clear();

                for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                    // create the acoustic event object from the snapshot
                    final AcousticEvent acousticEvent = dataSnapshot.getValue(AcousticEvent.class);

                    assert acousticEvent != null;
                    acousticEvent.setMfccMatrix(acousticEvent.getMfccDoubleList());


                    // perform Dynamic Time Warping to compare the newly detected event against this Known Event
                    DTW dynamicTimeWarp = new DTW(acousticEvent.getMfccMatrix(), detectedEventMfccs);

                    Double cost = dynamicTimeWarp.getCost();
                    Double maxCost = acousticEvent.getMaxCost();
                    Log.d("KNOWN_EVENT", acousticEvent.name+" Maximum Acceptable Cost :"+maxCost+" distance :"+ cost);


                    // Compare cost doubles to determine if the cost was less than the maximum
                    // acceptable cost. If so, add event to results

                    int comparison = Double.compare(cost, maxCost);

                    if(comparison > 0 ){
                        Log.d("KNOWN_EVENT", cost+
                                " cost is greater than event's max acceptable cost: "+maxCost);
                    } else if (comparison < 0){
                        Log.d("KNOWN_EVENT", cost+
                                " cost is less than event's max acceptable cost :"+maxCost);
                        results.put(acousticEvent, cost);
                    }



                } // end dataSnapshot for-Loop


                if(results.isEmpty()){
                    // if none were close enough, send a default notification
                    sendNotification("Unknown Sound");
                } else {
                    Double shortestDistance = Collections.min(results.values());
                    Log.d("KNOWN_EVENTS", "Shortest Distance was : " + shortestDistance);


                    //Match shortest distance to the event that produced it
                    for (AcousticEvent event : results.keySet()) {
                        if (results.get(event).equals(shortestDistance)) {

                            sendNotification(event.getName());

                            Log.d(TAG, event.getName() + " was the event closest to this one");
                            Log.d(TAG, "DTW Recognition Process END : "+System.currentTimeMillis());

                        }
                    }

                }

            } // end onDataChange

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("KNOWN_EVENTS", error.getMessage().toString());
            } // end on cancelled
        }); // end databaseReferenceValueEventListener




    } // end identifyAcousticEvent




    /**
     * converts the mfcc float array to doubles in order to be sent through the DTW
     * class
     *
     * @param input
     * @return a double array of mfccs
     */
    public static double[] convertFloatsToDoubles(float[] input) {
        if (input == null) {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i];
        }
        return output;
    } // end convertFloatsToDoubles




    /**
     * When the detected event is identified using DTW to establish the known event with the distance in
     * MFCC vectors closest to this one, the name of the sound is passed into this method and the notification
     * is sent to all user's subscribed to the topic.
     *
     * The topic in this case is the current user's id so the only receiver's of this notification will be the
     * devices on which the user with this ID has logged into and therefore subscribed to the topic.
     *
     * @param soundName
     */
    public void sendNotification(String soundName){
        mRequestQueue = super.getmRequestQueue();

        String body;
        String time = String.valueOf(Calendar.getInstance().getTime());

        // Check if there was a match to the detected acoustic event or not

        if(soundName.equals("Unknown Sound")){
            body = " was detected, please investigate";
        } else{
            body = " was heard in your home at "+time;
        }

        // jsonObject
        JSONObject mainObject = new JSONObject();

        try {
            // current user's user id is the topic to send notifications to subscribed devices
            mainObject.put("to", "/topics/" + mUser.getUid());
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("title", soundName+" Detected");
            notificationObject.put("body", soundName+body);


            mainObject.put("notification", notificationObject);


            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, notificationUrl, mainObject,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // run when successful

                            Log.d("NOTIFICATION", "Notification sent to user : "+System.currentTimeMillis());
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // run on error
                    Log.e("NOTIFICATION ERROR", error.getMessage().toString());
                }
            }){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {

                    Map<String,String> header = new HashMap<>();
                    header.put("Content-type", "application/json");
                    header.put("authorization", "key=AAAAajXxFaU:APA91bHXDHy0XumDr6FNo8LCsqc8YyR8Hop_JsiqaBKbAECR8KPlnr0x0g5opfyjsc5kXRCWLrePsFLgXUMVF_WDp43tbw8N7-IUT-4XuY5lMJ2fcTDCg8tjRL9WhP75x3E8c7Hfsewb");

                    return header;
                }
            };

            mRequestQueue.add(request);
        }catch (JSONException e) {
            e.printStackTrace();
        } // end try-catch block


    } // end sendNotification method





} // end subClass
