/**
 * Copyright 2004-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.example.thirdearoftruth.marytts;

import com.example.thirdearoftruth.marytts.MathUtils;

/**
 * @author Marc Schr&ouml;der
 */
public class GaussWindow extends Window {
    public static final double DEFAULT_SIGMA = 100;
    protected double sigma;
    protected double sigmasquare;

    /**
     * Crate a Gauss window with the given length and a default sigma.
     *
     * @param length
     *            the length of the window, in samples (must be an odd number)
     */
    public GaussWindow(int length) {
        this(length, DEFAULT_SIGMA, 1.);
    }

    /**
     * Crate a Gauss window with the given length and a default sigma, and apply a prescaling factor to each sample in the window.
     *
     * @param length
     *            the length of the window, in samples (must be an odd number)
     * @param prescalingFactor
     *            prescaling factor
     */
    public GaussWindow(int length, double prescalingFactor) {
        this(length, DEFAULT_SIGMA, prescalingFactor);
    }

    /**
     * Create a Gauss window with the given length and sigma.
     *
     * @param length
     *            the length of the window, in samples (should be an odd number)
     * @param sigma
     *            the sigma coefficient in the Gauss curve. A good starting point is 100.
     * @param prescalingFactor
     *            prescaling factor
     */
    public GaussWindow(int length, double sigma, double prescalingFactor) {
        window = new double[length];
        this.sigma = sigma;
        this.sigmasquare = sigma * sigma;
        this.prescalingFactor = prescalingFactor;
        initialise();
    }

    protected void initialise() {
        int mid = window.length / 2 + 1;
        for (int i = 0; i < window.length; i++) {
            int dist = i - mid;
            window[i] = Math.exp(-0.5 * dist * dist / sigmasquare);
        }
    }

    public String toString() {
        return "Gauss window";
    }
}
