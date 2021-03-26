package com.example.thirdearoftruth.audio;

import android.util.Log;

import com.example.thirdearoftruth.marytts.DTW;
import com.example.thirdearoftruth.models.AcousticEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * @author dermotbrennan
 * <p>
 * This class is responsible for the handling of the results of Dynamic Time Warping
 * on the MFCCs of the known Acoustic Events and the MFCCs of the detected event.
 * <p>
 * The known AcousticEvents from the Firebase Realtime Databse and the MFCC double[][] of the newly
 * detected event are passed into the matchmaker and the makeMatch() method is invoked.
 *
 * The event with the lowest cost is added to the bestMatch HashMap and can be called when
 * passing the name and other stored details of the event into the notification builder.
 * <p>
 * Results of the DTW can be viewed by calling the results HashMap<AcousticEvent, Double>
 * if the comparisons to all known events need to be viewed.
 */
public class AcousticEventDtwCostIdentifier implements AcousticEventIdentifier {

    // variables
    private static final String TAG = "COST_IDENTIFIER";
    // instance variables
    /**
     * the results of the DTW comparisons stored with their corresponding Acoustic
     * Events
     */
    private Map<AcousticEvent, Double> results;

    /**
     * the known AcousticEvents stored by the user in the database.
     * Mfccs to be compared with the mfccs of the new event
     */
    private ArrayList<AcousticEvent> knownEvents;

    /**
     * the mfcc 2d array of the detected acoustic event to be compared with the
     * mfccs of the
     */
    private double[][] detectedEventMFCCs;


    /**
     * the acousticEvent and score that is lowest out of all those in the knownEvents
     * list indicating that the new event is most likely to be this sound
     */
    private Map<AcousticEvent, Double> bestMatch;


    // constructors

    /**
     * Default constructor that simply prepares the HashMaps for entries.
     */
    public AcousticEventDtwCostIdentifier() {
        // initialise the results map and bestMatch map
        this.results = new HashMap<AcousticEvent, Double>();
        this.bestMatch = new HashMap<AcousticEvent, Double>();

    }

    /**
     * Constructor that takes an arraylist of known acousticEvents (created from database
     * references) and the mfcc double[][] as arguments to be compared through Dynamic
     * Time Warping.
     *
     * @param knownEvents
     * @param detectedEvent
     */
    public AcousticEventDtwCostIdentifier(ArrayList<AcousticEvent> knownEvents, double[][] detectedEvent) {
        this.knownEvents = knownEvents;
        this.detectedEventMFCCs = detectedEvent;

        // initialise the results map and bestMatch map
        this.results = new HashMap<AcousticEvent, Double>();
        this.bestMatch = new HashMap<AcousticEvent, Double>();

    }


    // getters and setters

    /**
     * @return the results
     */
    public Map<AcousticEvent, Double> getResults() {
        return results;
    }


    /**
     * @param results the results to set
     */
    public void setResults(Map<AcousticEvent, Double> results) {
        this.results = results;
    }


    /**
     * @return the knownEvents
     */
    public ArrayList<AcousticEvent> getKnownEvents() {
        return knownEvents;
    }


    /**
     * @param knownEvents the knownEvents to set
     */
    public void setKnownEvents(ArrayList<AcousticEvent> knownEvents) {
        this.knownEvents = knownEvents;
    }


    /**
     * @return the detectedEventMFCCs
     */
    public double[][] getDetectedEventMFCCs() {
        return detectedEventMFCCs;
    }


    /**
     * @param detectedEventMFCCs the detectedEventMFCCs to set
     */
    public void setDetectedEventMFCCs(double[][] detectedEventMFCCs) {
        this.detectedEventMFCCs = detectedEventMFCCs;
    }


    /**
     * Returns the final results set: the AcousticEvent itself with its stored data and
     * * a double signifying the cost determined by the MaryTTS DTW class
     *
     * @return the bestMatch
     */
    public Map<AcousticEvent, Double> getBestMatch() {
        return bestMatch;
    }


    /**
     * Sets the final results set. (Not currently used)
     *
     * @param bestMatch the bestMatch to set
     */
    public void setBestMatch(Map<AcousticEvent, Double> bestMatch) {
        this.bestMatch = bestMatch;
    }

    // methods

    /**
     * When this method is invoked, the Dynamic Time Warping algorithm is applied to
     * the double[][] mfccs of the AcousticEvents and the mfccs double[][] of the newly
     * detected event and the costs are mapped to their corresponding AcousticEvents in
     * the results HashMap.
     * <p>
     * The lowest cost is calculated and is mapped to the corresponding AcousticEvent(s)
     * in the bestMatch HashMap which can be called by the Activity that requires it.
     */
    @Override
    public void calculateMatch() {

        // call the DTW
        // Calculates distance using Euclidean distance function.
        // New DTWCostIdentifier could be chained with this using a different distance function
        DTW dynamicTimeWarp;

        // for each acoustic event, take its MFCC 2d array and compare to
        // the detected event
        for (AcousticEvent knownEvent : knownEvents) {
            // signal is each knownEvent in the array, reference is the
            dynamicTimeWarp = new DTW(knownEvent.getMfccMatrix(), detectedEventMFCCs);

            // take the distance/cost between it's mfcc array and that of the detected event
            //  add them to the results map IF the distance is lower than the specified
            Double cost = Double.valueOf(dynamicTimeWarp.getCost());

            Log.d(TAG, "Distance for "+knownEvent.getName()+" was: "+cost);
            if(cost <= knownEvent.maxCost) {
                // add to the results hashmap
                results.put(knownEvent, cost);
            } // end distance checking if

        } // end results Map enhanced for

        // calculate the lowest value in the map
        Double shortestDistance = Collections.min(results.values());
        Log.d(TAG, "Shortest Distance was : "+shortestDistance);
        // now find the event corresponding to the lowest value
        // and add it to the bestMatch map
        // this event is the closest to the detected event
        for (AcousticEvent event : results.keySet()) {
            if (results.get(event).equals(shortestDistance)) {
                Log.d(TAG, event.getName()+" was added to the bestMatch map");
                bestMatch.put(event, shortestDistance);
            } // end if
        }// end for

    } // end calculateMatch() method




}// end class
