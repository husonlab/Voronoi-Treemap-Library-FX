/*
 * VoronoiTreeMapComputation.java Copyright (C) 2023 Daniel H. Huson
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


import kn.uni.voronoitreemap.j2d.PolygonSimple;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * computes the Voronoi map
 * using kn.uni.voronoitreemap by Arlind Nocaj
 * Daniel Huson, 4.2023
 */
public class VoronoiTreeMapComputation {

	/**
	 * run the Voronoi tree map computation
	 *
	 * @param settings         algorithm settings
	 * @param rootNode         the root node
	 * @param childrenFunction gets children of a node
	 * @param weightFunction   gets the weight of a node, leaves should have non-zero weight
	 * @param rootPolygon      the root polygon to draw the map into
	 * @param resultConsumer   is called when polygon for node has been computed
	 * @param <T>              the node type
	 * @return number of nodes processed
	 */
	public static <T> int run(Settings settings, T rootNode, Function<T, Collection<T>> childrenFunction, Function<T, Double> weightFunction, PolygonSimple rootPolygon, BiConsumer<T, PolygonSimple> resultConsumer) {
		var executorService = Executors.newFixedThreadPool(settings.getNumberOfThreads());
		var totalJobs = count(rootNode, childrenFunction);
		var countdownLatch = new CountDownLatch(totalJobs);

		var areaMap = computeDesiredAreaMap(rootNode, childrenFunction, weightFunction);

		executorService.submit(createRunnable(settings, executorService, rootNode, childrenFunction, areaMap::get, rootPolygon, 0, countdownLatch, resultConsumer));

		try {
			countdownLatch.await();
		} catch (InterruptedException ignored) {
		}
		executorService.shutdownNow();
		return totalJobs;
	}

	/**
	 * creates the runnable task
	 */
	private static <T> Runnable createRunnable(Settings settings, ExecutorService executorService, T node, Function<T, Collection<T>> childrenFunction, Function<T, Double> areaFunction, PolygonSimple polygonSimple, int level, CountDownLatch countDown, BiConsumer<T, PolygonSimple> consumeResult) {
		return () -> {
			try {
				// run
				var compute = new ChildrenMapComputation(settings);
				var childSites = compute.run(node, childrenFunction, areaFunction, polygonSimple);
				if (childSites != null) {
					for (var site : childSites) {
						if (site != null) {
							var child = (T) site.getData();
							synchronized (consumeResult) {
								var polygon = site.getPolygon();
								polygon.setLevel(level);
								consumeResult.accept(child, site.getPolygon());
							}
							executorService.submit(createRunnable(settings, executorService, child, childrenFunction, areaFunction, site.getPolygon(), level + 1, countDown, consumeResult));
						}
					}
				}
				countDown.countDown();
			} catch (Exception e) {
				System.err.println(e.getMessage());
				while (countDown.getCount() > 0)
					countDown.countDown();
			}
		};
	}

	/**
	 * counts the number of nodes in subtree rooted at v
	 */
	private static <T> int count(T v, Function<T, Collection<T>> getChildren) {
		var count = 1;
		for (var c : getChildren.apply(v)) {
			count += count(c, getChildren);
		}
		return count;
	}

	/**
	 * computes the desired area map, for each node v, the total weight in the subtree rooted at v
	 */
	private static <T> Map<T, Double> computeDesiredAreaMap(T rootNode, Function<T, Collection<T>> getChildren, Function<T, Double> getWeight) {
		var nodeAreaMap = new HashMap<T, Double>();
		computeDesiredAreaMapRec(rootNode, getChildren, getWeight, nodeAreaMap);
		return nodeAreaMap;
	}

	/**
	 * recursively does the work
	 */
	private static <T> double computeDesiredAreaMapRec(T v, Function<T, Collection<T>> getChildren, Function<T, Double> getWeight, HashMap<T, Double> nodeAreaMap) {
		var weight = 0.0;
		for (var c : getChildren.apply(v)) {
			weight += computeDesiredAreaMapRec(c, getChildren, getWeight, nodeAreaMap);
		}
		var add = getWeight.apply(v);
		if (add != null && add > 0)
			weight += add;
		nodeAreaMap.put(v, weight);
		return weight;
	}
}