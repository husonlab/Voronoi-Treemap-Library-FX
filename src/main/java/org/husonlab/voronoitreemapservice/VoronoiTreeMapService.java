/*
 * VoronoiTreeMapService.java Copyright (C) 2023 Daniel H. Huson
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

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import kn.uni.voronoitreemap.j2d.PolygonSimple;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * service for computing a voronoi tree map
 * using kn.uni.voronoitreemap by Arlind Nocaj
 * Daniel Huson, 5.2023
 */
public class VoronoiTreeMapService<T> extends Service<Integer> {
	private Settings settings;
	private T rootNode;
	private Function<T, Collection<T>> childrenFunction;
	private Function<T, Double> weightFunction;
	private PolygonSimple rootPolygon;
	private BiConsumer<T, PolygonSimple> resultConsumer;

	/**
	 * constructor
	 */
	public VoronoiTreeMapService() {
		this(new Settings());
	}

	/**
	 * constructor
	 *
	 * @param settings the algorithm settings
	 */
	public VoronoiTreeMapService(Settings settings) {
		this.settings = settings;
	}

	/**
	 * set the task to perform
	 *
	 * @param rootNode         root node
	 * @param childrenFunction get children
	 * @param weightFunction   get weight, leaves should not have zero weight
	 * @param rootPolygon      the root polygon to draw the map in
	 * @param resultConsumer   this is called on each computed polygon in the JavaFX thread
	 */
	public void setTask(T rootNode, Function<T, Collection<T>> childrenFunction, Function<T, Double> weightFunction,
						PolygonSimple rootPolygon, BiConsumer<T, PolygonSimple> resultConsumer) {
		this.rootNode = rootNode;
		this.childrenFunction = childrenFunction;
		this.weightFunction = weightFunction;
		this.rootPolygon = rootPolygon;
		this.resultConsumer = resultConsumer;
	}

	@Override
	protected Task<Integer> createTask() {
		return new Task<>() {
			@Override
			protected Integer call() throws Exception {
				if (settings == null || rootNode == null || childrenFunction == null || weightFunction == null || rootPolygon == null || resultConsumer == null)
					throw new Exception("VoronoiTreeMapService: not initialized");
				return VoronoiTreeMapComputation.run(settings, rootNode, childrenFunction, weightFunction, rootPolygon, (v, p) -> Platform.runLater(() -> resultConsumer.accept(v, p)));
			}
		};
	}
}
