package com.example.thirdearoftruth.audio;

import com.android.volley.RequestQueue;
import com.google.firebase.auth.FirebaseUser;

import be.tarsos.dsp.AudioEvent;

/**
 * This is the superclass from which all Detected Acoustic Events will be processed.
 *
 * The Tarsos DSP AudioEvent containing the buffer/current block of frames is shared between the
 * AudioDispatcher responsible for detecting the Acoustic Events in DetectionService while the
 * processing and identification takes place within a class that extends this Superclass.
 */
public abstract class Detection implements Runnable {

    // Instance variables
    /**
     * the current block of frames/ the buffer to be processed
     */
    private AudioEvent audioEvent;


    /**
     * The currently logged in user to which notifications will be sent
     */
    FirebaseUser mFirebaseUser;

    /**
     * The volley requestQueue to which the notification holding the result of this recognition process
     * will be sent to
     */
    private RequestQueue mRequestQueue;



    // Constructors

    /**
     * Default constructor with no parameters
     */
    public Detection() {

    }


    /**
     *
     * @param audioEvent
     * @param mUser
     * @param mRequestQueue
     */
    public Detection(AudioEvent audioEvent, FirebaseUser mUser, RequestQueue mRequestQueue) {
        this.audioEvent = audioEvent;
        this.mFirebaseUser = mUser;
        this.mRequestQueue = mRequestQueue;
    }






    // Getters and Setters

    /**
     * Call this method in each section of the if statement block in the detection processor
     * where the audioEvent's RMS level is over the threshold
     * @return the audioEvent
     */
    public AudioEvent getAudioEvent() {
        return audioEvent;
    }

    public void setAudioEvent(AudioEvent audioEvent) {
        this.audioEvent = audioEvent;
    }


    /**
     * Retrieve the currently logged-in Firebase User
     * @return
     */
    public FirebaseUser getmFirebaseUser() {
        return mFirebaseUser;
    }

    /**
     * Set the FirebaseUser to refer to the database
     * @param mFirebaseUser
     */
    public void setmFirebaseUser(FirebaseUser mFirebaseUser) {
        this.mFirebaseUser = mFirebaseUser;
    }


    /**
     * Retrieve the Volley RequestQueue
     * @return
     */
    public RequestQueue getmRequestQueue() {
        return mRequestQueue;
    }

    /**
     * Set the Volley RequestQueue for sending push notifications
     * @param mRequestQueue
     */
    public void setmRequestQueue(RequestQueue mRequestQueue) {
        this.mRequestQueue = mRequestQueue;
    }








    // Methods
    /**
     * Process the AudioEvent from the Detection Service while a condition is true
     */
    @Override
    public void run() {

    }


    /**
     * End the detection condition and call  the identification method from here
     */
    public void stop() {

    }

    /**
     * Use the audio data gathered during the run method to recognise the
     */
    public void identifyAcousticEvent(){

    }


    /**
     * Send the notification containing the name of the identified Sound to the User via the volley
     * requestQueue, a JSON object and the MyFirebaseMessagingService class in the notifications package
     *
     */
    public void sendNotification(){

    }




} // end Detection superClass
