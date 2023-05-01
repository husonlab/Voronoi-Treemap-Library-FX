/*******************************************************************************
 * Copyright (c) 2013 Arlind Nocaj, University of Konstanz.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * For distributors of proprietary software, other licensing is possible on request: arlind.nocaj@gmail.com
 *
 * This work is based on the publication below, please cite on usage, e.g.,  when publishing an article.
 * Arlind Nocaj, Ulrik Brandes, "Computing Voronoi Treemaps: Faster, Simpler, and Resolution-independent", Computer Graphics Forum, vol. 31, no. 3, June 2012, pp. 855-864
 ******************************************************************************/
package kn.uni.voronoitreemap.j2d;

import kn.uni.voronoitreemap.convexClip.ConvexClip;
import kn.uni.voronoitreemap.convexClip.cVertex;
import kn.uni.voronoitreemap.convexClip.cVertexList;
import kn.uni.voronoitreemap.helper.Geometry;

import java.util.Arrays;
import java.util.Random;


/**
 * Implements a simple polygon with one continous region, by using two double arrays.
 *
 * @author Arlind Nocaj
 */
public class PolygonSimple implements Cloneable {

	/**
	 * Used for generation of a random point in a polygon.
	 */
	private Random seed = new Random(5);

	/**
	 * centroid of the polygon is stored for faster access, once it is computed
	 */
	private Point2D centroid;
	private double area = -1;
	private kn.uni.voronoitreemap.j2d.Rectangle2D bounds;

	/**
	 * Stores the orginal polygon result, without shrinking
	 */
	private PolygonSimple oldPolygon;

	private double[] x;
	/**
	 * x-coordinates
	 */
	private double[] y;
	/**
	 * y-coordinates
	 */
	private int length = 0;

	private int level;

	public PolygonSimple() {
		x = new double[16];
		y = new double[16];
	}

	/**
	 * @param numberPoints initial array size, default initial array size is 16.
	 */
	public PolygonSimple(int numberPoints) {
		if (numberPoints > 2) {
			x = new double[numberPoints];
			y = new double[numberPoints];
		} else {
			x = new double[16];
			y = new double[16];
		}
	}

	/**
	 * @param xPoints x-coordinate of the polygon points
	 * @param yPoints y-coordinate of the polygon points
	 * @param length  number of elements which should be considered from the given arrays
	 */
	public PolygonSimple(double[] xPoints, double[] yPoints, int length) {
		bounds = null;
		centroid = null;
		this.x = Arrays.copyOf(xPoints, length);
		this.y = Arrays.copyOf(yPoints, length);
		this.length = length;
	}

	/**
	 * @param xPoints x-coordinate of the polygon points
	 * @param yPoints y-coordinate of the polygon points
	 */
	public PolygonSimple(double[] xPoints, double[] yPoints) {
		int length = xPoints.length;
		bounds = null;
		centroid = null;
		this.x = Arrays.copyOf(xPoints, length);
		this.y = Arrays.copyOf(yPoints, length);
		this.length = length;
	}

	/**
	 * Replaces the pointers of the coordinate arrays to show to the given coordinate arrays.
	 */
	public PolygonSimple(PolygonSimple that) {
		bounds = null;
		centroid = null;
		this.oldPolygon = null;
		this.seed = that.seed;
		this.length = that.length;
		this.x = Arrays.copyOf(that.x, length);
		this.y = Arrays.copyOf(that.y, length);
	}

	/**
	 * tests whether the given point is contained in the polygon (linear time).
	 */

	public boolean contains(double inX, double inY) {
		boolean contains = false;
		if (bounds == null)
			getBounds();
		if (!bounds.contains(inX, inY)) {
			return false;
		}
		// Take a horizontal ray from (inX,inY) to the right.
		// If ray across the polygon edges an odd # of times, the point is
		// inside.
		for (int i = 0, j = length - 1; i < length; j = i++) {
			if ((((y[i] <= inY) && (inY < y[j]) || ((y[j] <= inY) && (inY < y[i]))) && (inX < (x[j] - x[i])
																							  * (inY - y[i]) / (y[j] - y[i]) + x[i])))
				contains = !contains;
		}
		return contains;
	}

	/**
	 * {@link #contains(double, double, double, double)}
	 */

	public boolean contains(Rectangle2D r) {
		return contains(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
	}

	/**
	 * {@link #contains(double, double)}
	 */
	public boolean contains(Point2D p) {
		return contains(p.getX(), p.getY());
	}

	/**
	 * tests whether each corner point of the given rectangle is contained in the polygon.
	 */

	public boolean contains(double x, double y, double w, double h) {
		if (bounds == null)
			getBounds2D();
		if (bounds.contains(x, y, w, h)) {
			if (contains(x, y) && contains(x + w, y) && contains(x, y + h)
				&& contains(x + w, y + h))
				return true;
			return false;
		} else
			return false;
	}

	public Rectangle2D getBounds2D() {
		bounds = null;
		if (bounds == null) {
			getBounds();
		}
		return bounds;
	}

	/**
	 * tests whether the given rectangle will intersect to the bounds of the polygon.
	 */

	public boolean intersects(Rectangle2D r) {
		if (bounds == null) {
			getBounds();
		}
		return bounds.intersects(r);
	}

	/**
	 * {@link #intersects(Rectangle2D)}
	 */

	public boolean intersects(double x, double y, double w, double h) {
		if (bounds == null) {
			getBounds();
		}
		return bounds.intersects(x, y, w, h);
	}

	/**
	 * Returns the bounding rectangle of this polygon.
	 */
	public Rectangle2D getBounds() {
		bounds = null;
		if (bounds == null) {
			double xmin = Double.MAX_VALUE;
			double ymin = Double.MAX_VALUE;
			double xmax = Double.MIN_VALUE;
			double ymax = Double.MIN_VALUE;

			for (int i = 0; i < length; i++) {
				double x = this.x[i];
				double y = this.y[i];
				if (x < xmin)
					xmin = x;
				if (x > xmax)
					xmax = x;
				if (y < ymin)
					ymin = y;
				if (y > ymax)
					ymax = y;
			}
			bounds = new Rectangle2D(xmin, ymin, (xmax - xmin), (ymax - ymin));
		}
		return bounds;
	}

	/**
	 * Return the number of points in this polygon.
	 *
	 * @return integer number of points
	 */
	public int getNumPoints() {
		return length;
	}

	/**
	 * Adds a point to the polygon. Extends the corresponding array if necessary.
	 */
	public void add(double x, double y) {
		if (this.x.length <= length) {
			double[] newX = new double[this.x.length * 2];
			double[] newY = new double[this.x.length * 2];
			System.arraycopy(this.x, 0, newX, 0, length);
			System.arraycopy(this.y, 0, newY, 0, length);
			this.x = newX;
			this.y = newY;

		}
		this.x[length] = x;
		this.y[length] = y;
		length++;

	}

	/**
	 * Scales all points by multiplying with the scalingFactor
	 *
	 * @param scalingFactor
	 */
	public void scale(double scalingFactor) {
		for (int i = 0; i < length; i++) {
			x[i] = x[i] * scalingFactor;
			y[i] = y[i] * scalingFactor;
		}
		clearCacheOnly();
	}

	/**
	 * Translates all points of the polygon by adding the values
	 *
	 * @param tx translation on x
	 * @param ty translation on y
	 */
	public void translate(double tx, double ty) {

		for (int i = 0; i < length; i++) {
			x[i] = x[i] + tx;
			y[i] = y[i] + ty;
		}
		clearCacheOnly();
	}

	public void clearCacheOnly() {
		this.centroid = null;
		this.bounds = null;
		this.area = -1;
		if (this.oldPolygon != null)
			oldPolygon.clearCacheOnly();
	}

	/**
	 * {@link #add(double, double)}
	 */
	public void add(Point2D p) {
		add(p.x, p.y);
	}


	/**
	 * Uses the linear time algorithm of O'Rourke to run the intersection of
	 * two convex polygons.
	 *
	 * @param poly
	 * @return
	 */
	public PolygonSimple convexClip(PolygonSimple poly) {
		//bounding box have to match for intersection
		if (!this.getBounds2D().intersects(poly.getBounds2D()))
			return null;
		//check if bounding box corners are in polygon: then poly is contained completely inside the outer polygon
		if (this.contains(poly.getBounds2D()))
			return poly;

		//bounding boxes intersect 

		// to vertexList
		cVertexList list1 = this.getVertexList();
		cVertexList list2 = poly.getVertexList();
		ConvexClip clipper = new ConvexClip();
//		list1.PrintVertices();
//		list2.PrintVertices();
		clipper.Start(list1, list2);
		PolygonSimple res = new PolygonSimple();
		if (clipper.inters != null && clipper.inters.n > 0) {
			cVertex node = clipper.inters.head;
			double firstX = node.v.x;
			double firstY = node.v.y;
			res.add(node.v.x, node.v.y);
			double lastX = node.v.x;
			double lastY = node.v.y;
			for (int i = 1; i < clipper.inters.n; i++) {
				node = node.next;

				if (lastX != node.v.x || lastY != node.v.y) {// do not add point
					// if its the
					// same as
					// before
					if (i != (clipper.inters.n - 1) || (node.v.x != firstX)
						|| node.v.y != firstY) {// do not add if it is the
						// end point and the same as
						// the first point
						res.add(node.v.x, node.v.y);
					}
				}

			}
			return res;
		}
		//no intersection between the two polygons, so check if one is inside the other
		if (contains(poly.x[0], poly.y[0]))
			return poly;

		// no intersection between the polygons at all
		return null;
	}

	private cVertexList getVertexList() {
		cVertexList list = new cVertexList();
		for (int i = length - 1; i >= 0; i--) {
			cVertex vertex = new cVertex(x[i], y[i]);
			list.InsertBeforeHead(vertex);
		}
		return list;
	}

	/**
	 * Returns the area of the polygon.
	 */
	public double getArea() {
		if (area > 0) {
			return area;
		}
		double area = 0;
		// we can implement it like this because the polygon is closed
		// (point2D.get(0) = point2D.get(length + 1)
		int size = length - 1;
		for (int i = 0; i < size; i++) {
			area += (x[i] * y[i + 1] - x[i + 1] * y[i]);
		}
		area += (x[size] * y[0] - x[0] * y[size]);
		this.area = Math.abs(area) * 0.5;
		return this.area;
	}

	/**
	 * For the given point, the minimal distance to the segments of the polygon
	 * is computed.
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	public double getMinDistanceToBorder(double x, double y) {
		double result = Geometry.distancePointToSegment(this.x[length - 1],
				this.y[length - 1], this.x[0], this.y[0], x, y);
		for (int i = 0; i < (length - 1); i++) {
			double distance = Geometry.distancePointToSegment(this.x[i],
					this.y[i], this.x[i + 1], this.y[i + 1], x, y);
			if (distance < result) {
				result = distance;
			}
		}
		return result;
	}

	/**
	 * Computes the centroid of a polygon.
	 *
	 * @return centroid point
	 */
	public Point2D getCentroid() {
		if (centroid == null) {
			double xv = 0;
			double yv = 0;
			double areaQuotient = getArea() * 6;
			for (int i = 0; i < length; i++) {
				double temp = (x[i] * y[(i + 1) % length] - x[(i + 1) % length]
															* y[i]);
				xv += (x[i] + x[(i + 1) % length]) * temp;
				yv += (y[i] + y[(i + 1) % length]) * temp;
			}
			xv = xv / areaQuotient;
			yv = yv / areaQuotient;
			this.centroid = new Point2D(xv, yv);
		}
		return centroid;
	}


	public PolygonSimple clone() {
		PolygonSimple p = new PolygonSimple(this.getXPoints(), this.getYPoints(), length);
		p.oldPolygon = this.oldPolygon;
		return p;
	}

	/**
	 * Default percentage can be 0.96
	 *
	 * @param percentage
	 */
	public void shrinkForBorder(double percentage) {
		oldPolygon = (PolygonSimple) this.clone();
		getCentroid();
		double cx = centroid.getX();
		double cy = centroid.getY();
		for (int i = 0; i < length; i++) {

			double deltaX = x[i] - cx;
			double deltaY = y[i] - cy;
			double xnew = cx + deltaX * percentage;
			double ynew = cy + deltaY * percentage;
			x[i] = xnew;
			y[i] = ynew;
		}

		// /**
		// * Method where you use the angle bisector of three points to shrink
		// it.
		// */
		// double[] xnew = new double[x.length];
		// double[] ynew=new double[y.length];
		//	
		// Point2D p0=null;
		// Point2D p1=new Point2D.Double(x[length-1], y[length-1]);
		// Point2D p2=new Point2D.Double(x[0],y[0]);
		// double borderWidth=10;
		// for (int i=1;i<=length;i++){
		//			
		// p0=p1;
		// p1=p2;
		// if (i==length){
		// p2=new Point2D.Double(x[0],y[0]);
		// }else{
		// p2=new Point2D.Double(x[i], y[i]);
		// }
		//			
		// double endPointX =(p0.getX()+p2.getX())/2;
		// double endPointY=(p0.getY()+p2.getY())/2;
		// double deltaX=endPointX-p1.getX();
		// double deltaY=endPointY-p1.getY();
		//			
		// double euclidLength = Math.sqrt(deltaX*deltaX+deltaY*deltaY);
		//			
		//			
		// deltaX=deltaX/euclidLength;
		// deltaY=deltaY/euclidLength;
		//			
		// deltaX=deltaX*borderWidth;
		// deltaY=deltaY*borderWidth;
		//			
		// xnew[i-1]=p1.getX()+deltaX;
		// ynew[i-1]=p1.getY()+deltaY;
		// // }
		//			
		//		
		// }
		// // xnew[length]=xnew[0];
		// // ynew[length]=ynew[0];
		//		
		// x=xnew;
		// y=ynew;

	}

	/**
	 * We get a vector which describes where the point should be relative to the
	 * center. We change the length of the vector so that the point fits in the
	 * polygon. (reimplementation needed here)
	 *
	 * @return Point which is contained by this polygon and has same direction
	 * as the given vector point
	 */
	public Point2D getRelativePosition(Point2D vector) {

		getCentroid();

		double endPointX = centroid.getX() + vector.getX();
		double endPointY = centroid.getY() + vector.getY();
		Point2D endPoint = new Point2D(endPointX, endPointY);
		if (contains(endPointX, endPointY)) {
			return new Point2D(endPointX, endPointY);
		} else {
			double endPointX2 = centroid.getX() + vector.getX() * 0.85;
			double endPointY2 = centroid.getY() + vector.getY() * 0.85;
			if (contains(endPointX2, endPointY2)) {
				return new Point2D(endPointX2, endPointY2);
			}
		}
		Point2D p1 = null;
		Point2D p2 = new Point2D(x[0], y[0]);
		Point2D result = null;
		for (int i = 1; i <= length; i++) {

			p1 = p2;
			//TODO Keine Ahnung ob richtig
			if (i == length) {
				p2 = new Point2D(0, 0);
			} else {
				p2 = new Point2D(x[i], y[i]);
			}
			Point2D intersection = getIntersection(p1, p2, centroid, endPoint);
			if (intersection != null) {

				double deltaX = intersection.getX() - centroid.getX();
				double deltaY = intersection.getY() - centroid.getY();
				double e = intersection.distance(centroid);
				double minimalDistanceToBorder = 10;
				double alpha = (e - minimalDistanceToBorder) / e;
				if (contains(centroid)) {
					// make vector smaller
					result = new Point2D(centroid.getX() + deltaX * 0.8,
							centroid.getY() + deltaY * 0.8);
				} else {
					// make vector longer
					result = new Point2D(centroid.getX() + deltaX * 1.1,
							centroid.getY() + deltaY * ((1 - alpha) + 1));
				}
				if (contains(result)) {
					return result;
				}

			}
		}
		if (result != null && contains(result))
			return result;
		else {
			// System.err.println("Innerpoint");

			return getInnerPoint();

		}
	}

	/**
	 * Returns a random point in the polygon.
	 *
	 * @return
	 */
	public Point2D getInnerPoint() {
		var b = getBounds();
		double x = -1;
		double y = -1;
		do {
			x = b.getMinX() + seed.nextDouble() * b.getWidth();
			y = b.getMinY() + seed.nextDouble() * b.getHeight();
		} while (!this.contains(x, y));

		return new Point2D(x, y);
	}

	/**
	 * intersection of two lines formed by the given points:
	 * http://paulbourke.net/geometry/lineline2d/
	 *
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param p4
	 * @return
	 */
	private Point2D getIntersection(Point2D p1, Point2D p2, Point2D p3, Point2D p4) {

		// Bounding Box test
		double x1 = 0;
		double x2 = 0;
		double y1 = 0;
		double y2 = 0;

		double x3 = 0;
		double x4 = 0;
		double y3 = 0;
		double y4 = 0;

		if (p1.getX() < p2.getX()) {
			x1 = p1.getX();
			x2 = p2.getX();
		} else {
			x1 = p2.getX();
			x2 = p1.getX();
		}
		if (p1.getY() < p2.getY()) {
			y1 = p1.getY();
			y2 = p2.getY();
		} else {
			y1 = p2.getY();
			y2 = p1.getY();
		}

		if (p3.getX() < p4.getX()) {
			x3 = p3.getX();
			x4 = p4.getX();
		} else {
			x3 = p4.getX();
			x4 = p3.getX();
		}
		if (p3.getY() < p4.getY()) {
			y3 = p3.getY();
			y4 = p4.getY();
		} else {
			y3 = p4.getY();
			y4 = p3.getY();
		}

		//FIXME bounding box intersection needs to be corrected
		if (!(x2 >= x2 && x4 >= x1 && y2 >= y3 && y4 >= y1)) {
			return null;
		}

		Point2D n1 = new Point2D(p3.getX() - p1.getX(), p3.getY()
														- p1.getY());
		Point2D n2 = new Point2D(p2.getX() - p1.getX(), p2.getY()
														- p1.getY());
		Point2D n3 = new Point2D(p4.getX() - p1.getX(), p4.getY()
														- p1.getY());
		Point2D n4 = new Point2D(p2.getX() - p1.getX(), p2.getY()
														- p1.getY());

		if (Geometry.crossProduct(n1, n2) * Geometry.crossProduct(n3, n4) >= 0) {
			return null;
		}

		double denominator = (p4.getY() - p3.getY()) * (p2.getX() - p1.getX())
							 - (p4.getX() - p3.getX()) * (p2.getY() - p1.getY());
		if (denominator == 0) {
			// return null;
			throw new RuntimeException("Lines are parallel");
		}
		double ua = (p4.getX() - p3.getX()) * (p1.getY() - p3.getY())
					- (p4.getY() - p3.getY()) * (p1.getX() - p3.getX());
		double ub = (p2.getX() - p1.getX()) * (p1.getY() - p3.getY())
					- (p2.getY() - p1.getY()) * (p1.getX() - p3.getX());
		ua = ua / denominator;
		ub = ub / denominator;

		if ((ua >= 0 && ua <= 1) && (ub >= 0 && ub <= 1)) {
			return new Point2D(p1.getX() + ua * (p2.getX() - p1.getX()),
					p1.getY() + ua * (p2.getY() - p1.getY()));
		} else {
			// no intersection of the two segments
			return null;
		}

	}

	/**
	 * Return the intersection of the segment given bei p1 and p2 and the line
	 * given by p3 and p4. intersection:
	 * http://paulbourke.net/geometry/lineline2d/
	 *
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param p4
	 * @return
	 */
	private static Point2D getIntersectionOfSegmentAndLine(Point2D p1, Point2D p2, Point2D p3, Point2D p4) {

		double denominator = (p4.getY() - p3.getY()) * (p2.getX() - p1.getX())
							 - (p4.getX() - p3.getX()) * (p2.getY() - p1.getY());
		if (denominator == 0) {
			// return null;
			throw new RuntimeException("Lines are parallel");
		}
		double ua = (p4.getX() - p3.getX()) * (p1.getY() - p3.getY())
					- (p4.getY() - p3.getY()) * (p1.getX() - p3.getX());
		double ub = (p2.getX() - p1.getX()) * (p1.getY() - p3.getY())
					- (p2.getY() - p1.getY()) * (p1.getX() - p3.getX());
		ua = ua / denominator;
		ub = ub / denominator;

		if ((ua >= 0 && ua <= 1) && ub >= 1) {
			return new Point2D(p1.getX() + ua * (p2.getX() - p1.getX()),
					p1.getY() + ua * (p2.getY() - p1.getY()));
		} else {
			// no intersection of the two segments
			return null;
		}

	}

	/**
	 * Array with x-values of the polygon points.
	 *
	 * @return
	 */
	public double[] getXPoints() {
		return x;
	}

	/**
	 * Array with y-values of the polygon points.
	 *
	 * @return
	 */
	public double[] getYPoints() {
		return y;
	}

	/**
	 * If the polygon is modified by e.g. shrinking, this method returns the original polygon. If the polyogn was not modified, it can return null.
	 *
	 * @return
	 */
	public PolygonSimple getOriginalPolygon() {
		return oldPolygon;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}
}
