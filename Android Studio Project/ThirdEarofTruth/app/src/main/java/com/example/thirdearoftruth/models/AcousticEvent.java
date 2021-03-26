package com.example.thirdearoftruth.models;

import android.util.Log;
import java.util.ArrayList;

/**
 * Represents an Acoustic Event known by the User that has been recorded, processed and explicitly
 * identified with a name.
 *
 * This version of an Acoustic Event is solely intended to be used in the recognition behaviour of
 * Dynamic Time Warping based only on the extracted Acoustic Features of Mel-Frequency Cepstral
 * Coefficients.
 *
 * It is expected that this class will be considered a Superclass to be extended in any future development
 * if new recognition behaviours using other Acoustic features are implemented.
 *
 */
public class AcousticEvent   {
    // instance variables - must be public and have getters and setters in order to store
    // in Realtime database

    /**
     * Unique Identifier for this particular Acoustic Event/ Sound stored by the User in the
     * Database
     */
    public String id;

    /**
     * Real world name of the Acoustic Event to allow the user to understand the source of the
     * Acoustic Event
     */
    public String name;

    /**
     * The duration of this particular Acoustic Event expressed as a double
     */
    public double duration;

    /**
     * the 2d Arraylist of wrapper doubles representing the mfccs. Convert to primitive to perform
     * DTW on this and the newly detected sound
     */
    public ArrayList<ArrayList<Double>> mfccDoubleList;

    /**
     * the size of the mfcc vector used when rebuilding the vector from a file
     */
    public String mfccListSize;

    /**
     * the maximum acceptable value for a DTW cost to be considered accurate after the recognition
     * process
     */
    public double maxCost;

    /**
     * determines whether or not this event should be shared across multiple locations and users
     */
    public boolean defaultEvent;


    /**
     * The MFCC feature vector/matrix to be used in Dynamic Time Warping distance and cost calculations
     */
    private double[][] mfccMatrix;



    // constructors
    /**
     * Default constructor
     *
     */
    public AcousticEvent(){

    }

    /**
     * Constructor with params
     * @param id
     * @param name
     * @param duration
     * @param mfccDoubleList
     * @param mfccListSize
     * @param maxCost
     * @param defaultEvent
     */
    public AcousticEvent(String id, String name, double duration, ArrayList<ArrayList<Double>> mfccDoubleList, String mfccListSize, double maxCost, boolean defaultEvent) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.mfccDoubleList = mfccDoubleList;
        this.mfccListSize = mfccListSize;
        this.maxCost = maxCost;
        this.defaultEvent = defaultEvent;

    }


    // getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public ArrayList<ArrayList<Double>> getMfccDoubleList() {
        return mfccDoubleList;
    }

    public void setMfccDoubleList(ArrayList<ArrayList<Double>> mfccDoubleList) {
        this.mfccDoubleList = mfccDoubleList;
    }

    public String getMfccListSize() {
        return mfccListSize;
    }

    public void setMfccListSize(String mfccListSize) {
        this.mfccListSize = mfccListSize;
    }

    public double getMaxCost() {
        return maxCost;
    }

    public void setMaxCost(double maxCost) {
        this.maxCost = maxCost;
    }

    public boolean isDefaultEvent() {
        return defaultEvent;
    }

    public void setDefaultEvent(boolean defaultEvent) {
        this.defaultEvent = defaultEvent;
    }



    /**
     * Feed the 2d ArrayList of wrapper Doubles in to set the primitive 2d array double[][]
     * to be used in the dynamic time warping and matching process
     *
     *
     * @param mfccDoubleList
     */
    public void setMfccMatrix(ArrayList<ArrayList<Double>> mfccDoubleList){

        double[][] mfcc2DArray = new double[mfccDoubleList.size()][];

        for(int i = 0; i < mfccDoubleList.size(); i++) {

            ArrayList<Double> row = mfccDoubleList.get(i);
            double[] copy = new double[row.size()];
            for(int j = 0; j < row.size(); j++) {
                copy[j] = row.get(j);
            }
            mfcc2DArray[i] = copy;
        }

        this.mfccMatrix = mfcc2DArray;

    }

    /**
     * Returns the mfcc matrix/vector as a 2d array suitable for use in the
     * @return the MfccMatrix as a double[][]
     */
    public double[][] getMfccMatrix() {
        return mfccMatrix;
    }
}
