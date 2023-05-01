/*
 * PolygonUtilities.java Copyright (C) 2023 Daniel H. Huson
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

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import kn.uni.voronoitreemap.j2d.PolygonSimple;

/**
 * some simple polygon utilities
 * Daniel Huson, 5.2023
 */
public class PolygonUtilities {
	/**
	 * convert simple polygon to polygon
	 *
	 * @param polygonSimple simple polygon
	 * @return polygon
	 */
	public static Polygon polygon(PolygonSimple polygonSimple) {
		var points = new double[2 * polygonSimple.getNumPoints()];
		var xMin = Double.MAX_VALUE;
		var yMin = Double.MAX_VALUE;
		for (var i = 0; i < polygonSimple.getNumPoints(); i++) {
			var x = polygonSimple.getXPoints()[i];
			xMin = Math.min(xMin, x);
			points[2 * i] = x;
			var y = polygonSimple.getYPoints()[i];
			yMin = Math.min(yMin, y);
			points[2 * i + 1] = y;
		}
		for (var i = 0; i < polygonSimple.getNumPoints(); i++) {
			points[2 * i] -= xMin;
			points[2 * i + 1] -= yMin;
		}
		var polygon = new Polygon(points);
		polygon.setTranslateX(xMin);
		polygon.setTranslateY(yMin);
		return polygon;
	}

	/**
	 * computes the center of a polygon
	 *
	 * @param polygon the polyon
	 * @return its center
	 */
	public static Point2D computeCenter(Polygon polygon) {
		var points = polygon.getPoints();
		var n = points.size() / 2;
		if (n > 0) {
			var x = 0.0;
			var y = 0.0;
			for (var i = 0; i < n; i++) {
				x += points.get(2 * i);
				y += points.get(2 * i + 1);
			}
			return new Point2D(x / n, y / n);
		} else
			return new Point2D(0, 0);
	}

	/**
	 * computes a simple n-gon of the given radius
	 *
	 * @param radius radius
	 * @param n      number of corners
	 * @return polygon
	 */
	public static PolygonSimple simpleNGon(double radius, int n) {
		var polygon = new PolygonSimple();
		var delta = 2 * Math.PI / n;
		for (var i = 0; i < n; i++) {
			polygon.add(Math.cos((i - 0.5) * delta) * radius, Math.sin((i - 0.5) * delta) * radius);
		}
		return polygon;
	}
}
