/*
 * ChildrenMapComputation.java Copyright (C) 2023 Daniel H. Huson
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

import kn.uni.voronoitreemap.core.VoronoiCore;
import kn.uni.voronoitreemap.datastructure.OpenList;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;

import java.util.Collection;
import java.util.Random;
import java.util.function.Function;

/**
 * compute a Voronoi map for the children of a node,
 * using kn.uni.voronoitreemap by Arlind Nocaj
 * Daniel Huson, 4.2023
 */
public class ChildrenMapComputation {
	private final Random random;
	private final Settings settings;

	/**
	 * constructor
	 *
	 * @param settings settings
	 */
	public ChildrenMapComputation(Settings settings) {
		this.settings = settings;
		random = new Random(settings.getSeed());
	}

	/**
	 * does the computation
	 *
	 * @param node             the current tree node
	 * @param childrenFunction the children function
	 * @param areaFunction     the desired area function
	 * @param polygon          the polygon to fit the map into
	 */
	public <T> OpenList run(T node, Function<T, Collection<T>> childrenFunction, Function<T, Double> areaFunction, PolygonSimple polygon) {
		if (childrenFunction.apply(node) == null)
			return null;
		var numberChildren = childrenFunction.apply(node).size();
		if (numberChildren == 0)
			return null;

		// this is important:
		polygon = new PolygonSimple(polygon);

		var voronoiCore = new VoronoiCore();
		voronoiCore.setSettings(settings);
		voronoiCore.setClipPolygon(polygon);

		// add each child as a site
		var totalArea = childrenFunction.apply(node).stream().mapToDouble(areaFunction::apply).sum();

		for (var child : childrenFunction.apply(node)) {
			var point = polygon.getRelativePosition(polygon.getRandomInnerPoint(random));
			var site = new Site(point.getX(), point.getY());
			if (totalArea == 0)
				site.setPercentage(1.0 / numberChildren);
			else
				site.setPercentage(areaFunction.apply(child) / totalArea);
			site.setData(child);
			voronoiCore.addSite(site);
		}

		voronoiCore.doIterate();

		var sites = voronoiCore.getSites();
		if (settings.getPolygonScaleFactor() > 0 && settings.getPolygonScaleFactor() != 1.0) {
			for (var i = 0; i < sites.size; i++) {
				sites.array[i].getPolygon().shrinkForBorder(settings.getPolygonScaleFactor());
			}
		}
		return sites;
	}
}
