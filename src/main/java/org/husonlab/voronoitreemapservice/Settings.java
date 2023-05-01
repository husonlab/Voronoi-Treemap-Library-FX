/*
 * Settings.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.husonlab.voronoitreemapservice;

import kn.uni.voronoitreemap.core.VoroSettings;

/**
 * algorithm settings
 * using kn.uni.voronoitreemap by Arlind Nocaj
 * Daniel Huson, 5.2023
 */
public class Settings extends VoroSettings {
	private double polygonScaleFactor = 1.0;
	private long seed = 666;
	private int numberOfThreads = 4;

	public Settings() {
		super();
	}

	/**
	 * constructor
	 *
	 * @param numberOfThreads number of threads to use
	 */
	public Settings(int numberOfThreads) {
		super();
		setNumberOfThreads(numberOfThreads);
	}

	/**
	 * constructor
	 *
	 * @param numberOfThreads number of threads to use
	 * @param seed            random generator seed
	 */
	public Settings(int numberOfThreads, long seed) {
		super();
		setNumberOfThreads(numberOfThreads);
		setSeed(seed);
	}

	/**
	 * constructor
	 *
	 * @param numberOfThreads    number of threads to use
	 * @param seed               random generator seed
	 * @param polygonScaleFactor scale factor to shrink polygons
	 */
	public Settings(int numberOfThreads, long seed, double polygonScaleFactor) {
		super();
		setNumberOfThreads(numberOfThreads);
		setSeed(seed);
		setPolygonScaleFactor(polygonScaleFactor);
	}

	public double getPolygonScaleFactor() {
		return polygonScaleFactor;
	}

	public void setPolygonScaleFactor(double polygonScaleFactor) {
		this.polygonScaleFactor = polygonScaleFactor;
	}

	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}
}
