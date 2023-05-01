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
package kn.uni.voronoitreemap.core;

import kn.uni.voronoitreemap.datastructure.OpenList;
import kn.uni.voronoitreemap.diagram.PowerDiagram;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Rectangle2D;
import kn.uni.voronoitreemap.j2d.Site;

import java.util.Random;

/**
 * Core class for generating Voronoi Treemaps. position and weight of sites is
 * changed on each iteration to get the wanted area for a cell.
 *
 * @author Arlind Nocaj
 */
public class VoronoiCore {
	/**
	 * core variables
	 */

	private boolean firstIteration = true;
	private static final double nearlyOne = 0.999;

	/**
	 * Settings for the Core
	 */
	VoroSettings settings = new VoroSettings();

	protected PolygonSimple clipPolygon;
	protected OpenList sites;
	protected PowerDiagram diagram;
	private int currentIteration;
	protected double currentAreaError = 1.0;

	private Point2D center;
	private double scale;
	private double currentErrorMax;

	/**
	 * The resulting Voronoi cells are clipped with this polygon
	 *
	 * @param polygon clipping polygon
	 */
	public void setClipPolygon(PolygonSimple polygon) {
		clipPolygon = polygon;
		if (diagram != null)
			diagram.setClipPoly(polygon);
	}

	/**
	 * Returns clipping polygon
	 *
	 * @return
	 */
	public PolygonSimple getClipPolygon() {
		return clipPolygon;
	}

	/**
	 * Sets a rectangle as clipping polygon.
	 *
	 * @param rectangle
	 */
	public void setClipPolygon(Rectangle2D rectangle) {
		PolygonSimple poly = new PolygonSimple();
		poly.add(rectangle.getMinX(), rectangle.getMinY());
		poly.add(rectangle.getMaxX(), rectangle.getMinY());
		poly.add(rectangle.getMaxX(), rectangle.getMaxY());
		poly.add(rectangle.getMinX(), rectangle.getMaxY());
		setClipPolygon(poly);
	}

	private void init() {
		diagram = new PowerDiagram();
	}

	public VoronoiCore() {
		sites = new OpenList();
		init();
	}

	public VoronoiCore(Rectangle2D rectangle) {
		this();
		setClipPolygon(rectangle);
	}

	public VoronoiCore(PolygonSimple clipPolygon) {
		this();
		setClipPolygon(clipPolygon);
	}

	public VoronoiCore(OpenList sites, PolygonSimple clipPolygon) {
		this();
		this.sites = sites;
		setClipPolygon(clipPolygon);
	}

	/**
	 * Adds a site to the Voronoi diagram.
	 *
	 * @param site
	 */
	public void addSite(Site site) {
		sites.add(site);
	}

	public void iterateSimple() {
		// if(currentIteration<=settings.maxIterat){
		moveSites(sites);
		checkPointsInPolygon(sites);
		// }

		// voroDiagram();//does not seem to be necessary
		// fixNoPolygonSites();

		// adapt weights
		adaptWeightsSimple(sites);
		voroDiagram();

		// fixNoPolygonSites();
		// fixWeightsIfDominated(sites);
		currentAreaError = computeAreaError(sites);
		currentErrorMax = computeMaxError(sites);
		currentIteration++;
	}

	private void fixNoPolygonSites() {
		for (Site a : sites) {
			if (a.getPolygon() == null) {
				fixWeightsIfDominated(sites);
				voroDiagram();
				break;
			}
		}
	}

	public boolean checkBadResult(OpenList sites) {
		for (Site a : sites) {
			if (a.getPolygon() == null)
				return true;

			// assert clipPolygon.contains(a.getPolygon().getCentroid());

		}

		return false;
	}

	private void checkPointsInPolygon(OpenList sites) {
		boolean outside = false;
		for (int i = 0; i < sites.size; i++) {
			Site point = sites.array[i];
			if (!clipPolygon.contains(point.x, point.y)) {
				outside = true;
				Point2D p = clipPolygon.getInnerPoint();
				point.setXY(p.x, p.y);
			}
		}
		if (outside)
			fixWeightsIfDominated(sites);
	}

	private double computeAreaError(OpenList sites) {
		double completeArea = clipPolygon.getArea();
		double errorArea = 0;
		for (int z = 0; z < sites.size; z++) {
			Site point = sites.array[z];
			PolygonSimple poly = point.getPolygon();
			double currentArea = (poly == null) ? 0.0 : poly.getArea();
			double wantedArea = completeArea * point.getPercentage();
			errorArea += Math.abs(wantedArea - currentArea)
						 / (completeArea * 2.0);
		}
		return errorArea;
	}

	private double computeMaxError(OpenList sites2) {
		double completeArea = clipPolygon.getArea();
		double maxError = 0;
		for (int z = 0; z < sites.size; z++) {
			Site point = sites.array[z];
			PolygonSimple poly = point.getPolygon();
			double currentArea = (poly == null) ? 0.0 : poly.getArea();
			double wantedArea = completeArea * point.getPercentage();
			double error = Math.abs(wantedArea - currentArea) / (wantedArea);
			maxError = Math.max(error, maxError);
		}
		return maxError;
	}

	private void moveSites(OpenList sites) {
		for (Site point : sites) {
			PolygonSimple poly = point.getPolygon();
			if (poly != null) {
				Point2D centroid = poly.getCentroid();
				double centroidX = centroid.getX();
				double centroidY = centroid.getY();
				if (clipPolygon.contains(centroidX, centroidY))
					point.setXY(centroidX, centroidY);
			}
		}
	}

	private void adjustWeightsToBePositive(OpenList sites) {
		double minWeight = 0;
		for (int z = 0; z < sites.size; z++) {
			Site s = sites.array[z];
			if (s.getWeight() < minWeight)
				minWeight = s.getWeight();
		}

		for (int z = 0; z < sites.size; z++) {
			Site s = sites.array[z];
			double w = s.getWeight();
			if (Double.isNaN(w))
				w = 0.0001;
			w -= minWeight;
			if (w < 0.0001)
				w = 0.0001;
			s.setWeight(w);
		}

	}

	private void adaptWeightsSimple(OpenList sites) {
		Site[] array = sites.array;
		int size = sites.size;
		Random rand = new Random(5);
		double averageDistance = getGlobalAvgNeighbourDistance(sites);
		// double averageWeight=getAvgWeight(sites);
		// averageDistance+=averageWeight;
		double error = computeAreaError(sites);
		for (int z = 0; z < size; z++) {
			Site point = array[z];
			PolygonSimple poly = point.getPolygon();

			// if(poly==null)
			// System.err.println(point.getWeight()+"\t"+error);
			double completeArea = clipPolygon.getArea();
			double currentArea = (poly == null) ? 0.0 : poly.getArea();
			double wantedArea = completeArea * point.getPercentage();

			double increase = wantedArea / currentArea;
			if (currentArea == 0.0)
				increase = 2.0;

			double weight = point.getWeight();

			double step = 0;
			double errorTransform = (-(error - 1) * (error - 1) + 1);

			// errorTransform=error;
			// errorTransform=Math.max(errorTransform, settings.errorThreshold);
			// if(currentIteration>settings.maxIterat)
			// errorTransform*=rand.nextDouble();

			step = 1.0 * averageDistance * errorTransform;
			// step=2*averageDistance*error;
			double epsilon = 0.01;
			if (increase < (1.0 - epsilon))
				weight -= step;
			else if (increase > (1.0 + epsilon))
				weight += step;
			point.setWeight(weight);

			// debug purpose
			point.setLastIncrease(increase);

		}
	}

	private void fixWeightsIfDominated(OpenList sites) {

		for (Site s : sites) {
			double weight = s.getWeight();
			if (Double.isNaN(weight)) {
				s.setWeight(0.00000000001);
			}
		}

		for (Site s : sites) {
			for (Site q : sites) {
				if (s != q) {
					double distance = s.distance(q) * nearlyOne;
					if (Math.sqrt(s.getWeight()) >= distance) {
						double weight = distance * distance;
						q.setWeight(weight);
					}
				}
			}
		}
	}

	private void fixWeightsIfDominated2(OpenList sites) {

		// get all nearest neighbors
		OpenList copy = sites.cloneWithZeroWeights();
		for (Site s : sites)
			if (Double.isNaN(s.getWeight()))
				System.err.println(s);
		PowerDiagram diagram = new PowerDiagram(sites, sites.getBoundsPolygon(20));
		diagram.computeDiagram();

		// set pointer to original site
		for (int z = 0; z < sites.size; z++)
			copy.array[z].setData(sites.array[z]);

		for (int z = 0; z < copy.size; z++) {
			Site pointCopy = copy.array[z];
			Site point = sites.array[z];
			if (pointCopy.getNeighbours() != null)
				for (Site neighbor : pointCopy.getNeighbours()) {
					Site original = (Site) neighbor.getData();
					if (original.getPolygon() == null) {
						double dist = pointCopy.distance(neighbor) * nearlyOne;
						if (Math.sqrt(pointCopy.getWeight()) > dist) {
							double weight = dist * dist;
							point.setWeight(weight);
						}
					}
				}

		}

	}

	/**
	 * Computes the minimal distance to the voronoi Diagram neighbours
	 */
	private double getMinNeighbourDistance(Site point) {
		double minDistance = Double.MAX_VALUE;
		for (Site neighbour : point.getNeighbours()) {
			double distance = neighbour.distance(point);
			if (distance < minDistance) {
				minDistance = distance;
			}
		}
		return minDistance;
	}

	private double getAvgNeighbourDistance(Site point) {
		double avg = 0;
		for (Site neighbour : point.getNeighbours()) {
			double distance = neighbour.distance(point);
			avg += distance;
		}
		avg /= point.getNeighbours().size();
		return avg;
	}

	private double getAvgWeight(OpenList sites) {
		double avg = 0;
		int num = sites.size;
		for (Site point : sites)
			avg += point.getWeight();
		avg /= num;
		return avg;
	}

	private double getGlobalAvgNeighbourDistance(OpenList sites) {
		double avg = 0;
		int num = 0;
		for (Site point : sites)
			if (point.getNeighbours() != null)
				for (Site neighbour : point.getNeighbours()) {
					double distance = neighbour.distance(point);
					avg += distance;
					num++;
				}
		avg /= num;
		return avg;
	}

	private double getMinNeighbourDistanceOld(Site point) {
		double minDistance = Double.MAX_VALUE;

		for (Site neighbour : point.getOldNeighbors()) {
			double distance = neighbour.distance(point);
			if (distance < minDistance) {
				minDistance = distance;
			}
		}
		return minDistance;
	}

	/**
	 * Computes the diagram and sets the results
	 */
	public synchronized void voroDiagram() {
		boolean worked = false;
		while (!worked) {
			try {
				PowerDiagram diagram = new PowerDiagram();
				diagram.setSites(sites);
				diagram.setClipPoly(clipPolygon);
				diagram.computeDiagram();
				worked = true;
			} catch (Exception e) {

				System.out.println("Error on computing power diagram, fixing by randomization");
				// e.printStackTrace();

				randomizePoints(sites);
				adjustWeightsToBePositive(sites);
				fixWeightsIfDominated(sites);
			}
		}
	}

	public void printCoreCode() {
		printPolygonCode(clipPolygon);

		System.err.println("OpenList list=new OpenList(" + sites.size + ");");
		for (int i = 0; i < sites.size; i++) {
			Site s = sites.array[i];
			String line = "list.add(new Site(" + s.x + "," + s.y + ","
						  + s.getWeight() + "));";
			System.err.println(line);
		}

	}

	public static void printPolygonCode(PolygonSimple poly) {
		double[] x = poly.getXPoints();
		double[] y = poly.getYPoints();
		System.err.println("PolygonSimple poly=new PolygonSimple();");

		for (int i = 0; i < poly.getNumPoints(); i++) {
			String line = "poly.add(" + x[i] + "," + y[i] + ");";
			System.err.println(line);
		}
	}

	private void randomizePoints(OpenList sites) {

		// double dist=getGlobalAvgNeighbourDistance(sites);
		for (int i = 0; i < sites.size; i++) {
			Site point = sites.array[i];
			if (!clipPolygon.contains(point.x, point.y)) {
				Point2D p = clipPolygon.getInnerPoint();
				point.setXY(p.x, p.y);
				continue;
			}
			// double x=0;
			// double y=0;
			// do{
			// x=rand.nextDouble()-dist*1E-6;
			// y=rand.nextDouble()-dist*1E-6;
			// }while(!clipPolygon.contains(point.x+x,point.y+y));
			// point.setXY(x, y);

		}
	}

	/**
	 * Computes the ordinary diagram and sets the results
	 */
	synchronized protected void voroOrdinaryDiagram(OpenList sites) {

		diagram.setSites(sites);
		diagram.setClipPoly(clipPolygon);
		try {
			diagram.computeDiagram();
		} catch (Exception e) {
			System.err.println("Error on computing power diagram");
			e.printStackTrace();
		}
	}

	public void doIterate() {
		if (sites.size <= 1) {
			sites.array[0].setPolygon(clipPolygon.clone());
			return;
		}

		shiftAndScaleZeroCenter();

		// solveDuplicates(this.sites);
		currentIteration = 0;
		currentAreaError = 1.0;

		checkPointsInPolygon(sites);
		if (firstIteration) {
			firstIteration = false;
			voroDiagram();
		}

		boolean badResult = true;
		while (true) {
			iterateSimple();
			badResult = checkBadResult(sites);

			if (!badResult) {
				if (settings.cancelAreaError
					&& currentAreaError < settings.errorThreshold
					&& (!settings.cancelOnLocalError || currentErrorMax < settings.errorThreshold))
					break;

				if (settings.cancelMaxIterat
					&& currentIteration > settings.maxIterat)
					break;
			}

			// System.err.println("Iter: " + currentIteration
			// + "\t AreaError: \t" + lastAreaError);
		}

		transformBackFromZero();

		System.err.println("Iteration: " + currentIteration + "\t AreaError: \t" + currentAreaError);
		System.err.println("Iteration: " + currentIteration + "\t MaxError: \t" + currentErrorMax);

		// now its finished so give the cells a hint
		for (Site site : sites) {
			PolygonSimple poly = site.getPolygon();
			if (site.cellObject != null) {
				site.cellObject.setVoroPolygon(poly);
				site.cellObject.doFinalWork();
			}
		}
	}

	/**
	 * Scaling and Shifting allows for higher geometry precision
	 */
	private void shiftAndScaleZeroCenter() {

		PolygonSimple poly = clipPolygon;
		this.center = poly.getCentroid();
		Rectangle2D bounds = poly.getBounds2D();
		double width = Math.max(bounds.getWidth(), bounds.getHeight());
		double goalWidth = 500.0;

		this.scale = goalWidth / width;

		poly.translate(-center.x, -center.y);
		poly.scale(scale);

		PolygonSimple copy = poly.getOriginalPolygon();
		if (copy != null) {
			copy.translate(-center.x, -center.y);
			copy.scale(scale);
		}

		setClipPolygon(poly);

		for (Site s : sites) {
			double a = s.getX();
			double b = s.getY();

			a -= center.x;
			b -= center.y;

			a *= scale;
			b *= scale;
			s.setX(a);
			s.setY(b);
		}
	}

	private void transformBackFromZero() {

		clipPolygon.scale(1 / scale);
		clipPolygon.translate(center.x, center.y);

		for (Site s : sites) {
			double a = s.getX();
			double b = s.getY();
			a /= scale;
			b /= scale;

			a += center.x;
			b += center.y;

			s.setX(a);
			s.setY(b);

			PolygonSimple poly = s.getPolygon();
			poly.scale(1 / scale);
			poly.translate(center.x, center.y);
			s.setPolygon(poly);

			PolygonSimple copy = poly.getOriginalPolygon();
			if (copy != null) {
				copy.scale(1 / scale);
				copy.translate(center.x, center.y);
			}

			s.setWeight(s.getWeight() / (scale * scale));
		}

		scale = 1.0;
		center.x = 0;
		center.y = 0;
	}

	public void setSites(OpenList sites) {
		this.sites = sites;
	}

	public OpenList getSites() {
		return sites;
	}

	public static void normalizeSites(OpenList sites) {
		double sum = 0;
		Site[] array = sites.array;
		int size = sites.size;
		for (int z = 0; z < size; z++) {
			Site s = array[z];
			sum += s.getPercentage();
		}
		for (int z = 0; z < size; z++) {
			Site s = array[z];
			s.setPercentage(s.getPercentage() / sum);
		}

	}

	public void setSettings(VoroSettings coreSettings) {
		this.settings = coreSettings;
	}

}
