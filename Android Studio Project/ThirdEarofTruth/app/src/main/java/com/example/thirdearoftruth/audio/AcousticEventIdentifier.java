package com.example.thirdearoftruth.audio;

/**
 * @author dermotbrennan
 * A basic abstract class to be extended by new classes used for identifying AcousticEvents.
 *
 * Defines a definitive single behaviour of results calculation.
 *
 * A sub-class will utilise Dynamic Time Warping upon 2 sets of double[][] arrays consisting
 * of MFCCs of known Acoustic Events and 1 new event to produce a result that can be used to determine
 * how similar they are.
 *
 * If a collection of these AcousticEventIdentifiers could be chained together taking input from
 * AudioProcessors other than these and the results sets could
 *
 */
public interface AcousticEventIdentifier {

    /**
     * Takes multiple sets of Audio Data for comparison and produces a result to help identify the
     * new Acoustic Event detected by the system.
     */
    void calculateMatch();



}
