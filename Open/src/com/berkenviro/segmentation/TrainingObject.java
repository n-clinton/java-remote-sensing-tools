/*
 *  Copyright (C) 2011  Nicholas Clinton
 *	All rights reserved.  
 *
 *	Redistribution and use in source and binary forms, with or without modification, 
 *	are permitted provided that the following conditions are met:
 *
 *	1. Redistributions of source code must retain the above copyright notice, 
 *	this list of conditions and the following disclaimer.  
 *	2. Redistributions in binary form must reproduce the above copyright notice, 
 *	this list of conditions and the following disclaimer in the documentation 
 *	and/or other materials provided with the distribution. 
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 *	AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 *	THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 *	PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
 *	BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 *	DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 *	THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN 
 *	IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.berkenviro.segmentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

//import org.geotools.feature.Feature;
import org.apache.commons.math.stat.StatUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 *	@author Nicholas Clinton
 *	
 */
public class TrainingObject {

	int id;
	private Geometry myPoly;
	private ArrayList intersectedSegments;
	/*
	 * Any yStar segment intersects the training object and one of the following:
	 * 1. has its center in the training object
	 * 2. has the training object center in it
	 * 3. has 50% of its area intersecting the training object
	 * 4. intersects 50% of the training object area
	 */
	private ArrayList yStarSegments;
	// these are only special because I already set them in the constructor
	private double avgOverSeg;
	private double avgUnderSeg;
	/*
	 * Lucieer and Stein 2002.
	 * AFI and Db
	 * Added 3/25/8
	 */
	private Segment lrgstIntrsctr;
	private double lrgstArea;
	
	/*
	 * Updated to GeoTools 2.7, 20100615
	 * 
	 */
	public TrainingObject(SimpleFeature feature, FeatureCollection segmentation) {
	//public TrainingObject(Feature feature, FeatureCollection segmentation) {
		
		// set up instance variables
		String idString = feature.getID();
		id = Integer.parseInt(idString.substring(idString.indexOf(".")+1));
		myPoly = (Geometry)feature.getDefaultGeometry();
		intersectedSegments = new ArrayList();
		yStarSegments = new ArrayList();
		avgOverSeg = 0;
		avgUnderSeg = 0;
		/*
		 * Lucieer and Stein
		 */
		lrgstIntrsctr = null;
		lrgstArea = 0;
		
		//Iterator iter = segmentation.iterator();
		FeatureIterator<SimpleFeature> iterator =  segmentation.features();
		try {
			
			//Feature f;
			Geometry geo;
			Segment s;
			// iteration overy feature in the segmentation shapefile
			while (iterator.hasNext()) {

				//f = (Feature)iter.next();
				SimpleFeature f = iterator.next();
				geo = (Geometry)f.getDefaultGeometry();
				
				// 05/06/08 debug
				/*
				if (!geo.isValid()) {
					System.out.println("Invalid Geometry from a "+geo.getClass().getName());
					System.out.println(geo.toText());
					if (geo instanceof MultiPolygon) {
						MultiPolygon mp = (MultiPolygon) geo;
						for (int g=0; g<mp.getNumGeometries(); g++) {
							System.out.println("Geometry "+g+": "+mp.getGeometryN(g).toString());
						}
					}
				}
				*/
				
				
				if ((geo instanceof Polygon) || (geo instanceof MultiPolygon)) {
					if (myPoly.intersects(geo)) {
						
						// instantiate a segment relative to this training object
						s = new Segment(geo, myPoly);
						intersectedSegments.add(s);
						
						/*
						 * logical tests for yStar membership and stats calculated
						 */ 
						if  (isYstar(s)){
							yStarSegments.add(s);
							// compute the instance variable stats from these segments (divide later)
							avgOverSeg += s.getOverSegmentation();
							avgUnderSeg += s.getUnderSegmentation();
						}
						
						/*
						 * Lucieer and Stein
						 */
						double intrsctdA = myPoly.intersection(geo).getArea();
						if (intrsctdA > lrgstArea) {
							lrgstIntrsctr = s;
							lrgstArea = intrsctdA;
						}
						
					}
				}
				else {
					System.err.println("The segment is not a polygon!!");
				}
			}
			// finish off the instance variables
			avgOverSeg = avgOverSeg/yStarSegments.size();
			avgUnderSeg = avgUnderSeg/yStarSegments.size();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			segmentation.close(iterator);
		}
		
	}
	
	/*
	 * AFI from Lucieer and Stein
	 */
	public double getAFI() {
		//if (myPoly != null && lrgstIntrsctr != null) {
			return (myPoly.getArea() - lrgstIntrsctr.getArea()) / myPoly.getArea();
		//}
		//else {
			//return Double.MIN_VALUE;
		//}
	}
	
	/*
	 * Lucieer and Stein; A modified Db.  
	 * Uses vertices rather than pixels.  Returns the average distance 
	 * between the vertices of the training shape and the vertices of the 
	 * yStar segments only.  This mod intended to eliminate the disproportionate
	 * effect of a very large segment intersecting with a small intersection (e.g. sliver).
	 */
	public double getModDb() {
		int numVerts = myPoly.getNumPoints();
		// these are the vertices in the boundary
		Coordinate[] vertices = myPoly.getCoordinates();
		// check
		if (vertices.length != numVerts) {
			System.err.println("Whoops! The vertices don't match!");
		}
		
		// iterate over the vertices
		double cumDist = 0;
		int count = 0;
		for (int v=0; v<vertices.length; v++) {			
			// iterate over the y* segments
			Iterator iter = yStarSegments.iterator();
			while (iter.hasNext()) {
				Segment seg = (Segment) iter.next();
				Coordinate[] segVerts = seg.getCoords();
				double[] dist = new double[segVerts.length];
				// iterate over the coordinates of the intersected shape
				for (int s=0; s<segVerts.length; s++) {
					dist[s] = segVerts[s].distance(vertices[v]);
				} // end seg verts
				//System.out.println("weka.core.Utils.kthSmallestValue(dist, 1)"+weka.core.Utils.kthSmallestValue(dist, 1));
				//cumDist+=weka.core.Utils.kthSmallestValue(dist, 1);
				Arrays.sort(dist);
				cumDist+=dist[0];
				count++;
			} // end segs
		} // end myPoly verts
		
		return cumDist/count;
		
	}
	
	/*
	 * Oversegmentation from Lucieer and Stein
	 */
	public boolean isOverSegmented() {
		return ((getAFI() > 0.0) && (lrgstArea/this.getArea() < 1.0));
	}
	
	/*
	 * Undersegmentation from Lucieer and Stein
	 */
	public boolean isUnderSegmented() {
		return ((getAFI() < 0.0) && (lrgstArea/this.getArea() >= 1.0));
	}
	
	/*
	 * Moller et al. RPso
	 * This method must be called prior to iterating over the segments to get a weighted average.
	 * 7/6/08 modified to use Y* segments
	 */
	public double getAvgRPso() {
		// first, iterate over the segments, find max RPsub
		double maxRPsub = 0.0;
		//Iterator iter = intersectedSegments.iterator();
		Iterator iter = yStarSegments.iterator();
		while (iter.hasNext()) {
			Segment s = (Segment) iter.next();
			if (s.getRPsub() > maxRPsub) {
				maxRPsub = s.getRPsub();
			}
		}
		// Now, set the RPso of the segements
		double cumRPso = 0;
		//iter = intersectedSegments.iterator();
		iter = yStarSegments.iterator();
		while (iter.hasNext()) {
			Segment s = (Segment) iter.next();
			s.setRPso(s.getRPsub()/maxRPsub);
			cumRPso += s.getRPso();
		}
		//return cumRPso/intersectedSegments.size();
		return cumRPso/yStarSegments.size();
	}
	
	/*
	 * Moller et al. metric averages for this training object.
	 * Returns {AvgRPso, AvgRPsub, AvgRAsuper, AvgRAsub}
	 * 7/6/08 modified to use Y* segments
	 * 7/12/08 modified for the RA metrics to be non-Y*, the RP metric to be Y*
	 */
	public double[] getMollerAvgs() {
		double cumRPsub = 0.0;
		double cumRAsuper = 0.0;
		double cumRAsub = 0.0;
		Iterator iter = intersectedSegments.iterator();
		//Iterator iter = yStarSegments.iterator();
		while (iter.hasNext()) {
			Segment s = (Segment) iter.next();
			if (isYstar(s)) {
				cumRPsub += s.getRPsub();
			}
			cumRAsuper += s.getRAsuper();
			cumRAsub += s.getRAsub();
		}
		/*
		return new double[] {getAvgRPso(),
							 cumRPsub/intersectedSegments.size(),
							 cumRAsuper/intersectedSegments.size(),
							 cumRAsub/intersectedSegments.size()
							 };
							 */
		return new double[] {getAvgRPso(),
				 cumRPsub/yStarSegments.size(),
				 cumRAsuper/intersectedSegments.size(),
				 cumRAsub/intersectedSegments.size()};
	}
	
	
	/*
	 * Zhan et al. stats for this training object.
	 * Returns {AvgSimSize, SDSimSize, AvgQLoq, SDQLoc}
	 * 7/6/08 modified to use Y* segments
	 */
	public double[] getZhanStats(){
		/*
		double[] simSizes = new double[intersectedSegments.size()];
		double[] qLoqs = new double[intersectedSegments.size()];
		for (int s=0; s<intersectedSegments.size(); s++){
			Segment seg = (Segment) intersectedSegments.get(s);
			simSizes[s] = seg.getSimSize();
			qLoqs[s] = seg.getQLoq();
		}
		*/
		double[] simSizes = new double[yStarSegments.size()];
		double[] qLoqs = new double[yStarSegments.size()];
		for (int s=0; s<yStarSegments.size(); s++){
			Segment seg = (Segment) yStarSegments.get(s);
			simSizes[s] = seg.getSimSize();
			qLoqs[s] = seg.getQLoq();
		}
		return new double[] {StatUtils.mean(simSizes),
							 Math.sqrt(StatUtils.variance(simSizes)),
							 StatUtils.mean(qLoqs),
							 Math.sqrt(StatUtils.variance(qLoqs))
							 };
	}
	

	/*
	 * Yang et al.  underMerging and overMerging.  Returns {underMerging, overMerging).
	 * Should be implemented in the constructor of the Segment.
	 * 7/6/08 modified to use Y* segments
	 */
	public double[] getYangStats() {
		double underMerge = 0;
		double overMerge = 0;
		//Iterator iter = intersectedSegments.iterator();
		Iterator iter = yStarSegments.iterator();
		while (iter.hasNext()) {
			Segment s = (Segment) iter.next();
			underMerge+=(s.getArea() - s.intersectionArea(myPoly))/myPoly.getArea();
			overMerge+=(myPoly.getArea() - s.intersectionArea(myPoly))/myPoly.getArea();
		}
		return new double[] {underMerge, overMerge};
	}
	
	/*
	 * Weidner
	 */
	public double getWeidner() {
		double qr = 0;

		Iterator iter = yStarSegments.iterator();
		while (iter.hasNext()) {
			Segment s = (Segment) iter.next();
			qr += s.getQR();
		}
		return qr/yStarSegments.size();
	}
	
	
	
	/*
	 * Does the segment meet the yStar criteria?
	 */
	public boolean isYstar(Segment s) {
		return (this.isPointIn(s.getCenter()) || 
				s.centerIsIn(myPoly.getCentroid()) ||
				s.intersectionArea(myPoly)/this.getArea() > 0.5 ||
				s.getProportionIntersected(myPoly) > 0.5);
	}
	
	/*
	 * Does the TrainingObject contain a Point?
	 */
	public boolean isPointIn(Point p) {
		return myPoly.contains(p);
	}
	
	/*
	 * Encapsulate.
	 */
	public double getArea() {
		return myPoly.getArea();
	}
	
	/*
	 * Return the number of intersected shapes.
	 */
	public int getNumIntersected() {
		return intersectedSegments.size();
	}
	
	/*
	 * The weight is the number of segments in Ystar.
	 */
	public int getWeight() {
		return this.yStarSegments.size();
	}
	
	/*
	 * Return the total area of the intersected shapes.
	 */
	public double getAreaOfIntersected() {
		double intersectedArea = 0;
		Segment s;
		Iterator iter = intersectedSegments.iterator();
		while (iter.hasNext()) {
			s = (Segment) iter.next();
			intersectedArea += s.getArea();
		}
		return intersectedArea;
	}
	
	/*
	 * 
	 */
	public double getProportionIntersected(Polygon p) {
		return myPoly.intersection(p).getArea()/this.getArea();
	}
	
	/*
	 * 
	 */
	public int getNumCentersWithin() {
		int numCenters = 0;
		Segment s;
		Point center;
		Iterator iter = intersectedSegments.iterator();
		while (iter.hasNext()) {
			s = (Segment) iter.next();
			center = s.getCenter();
			if (myPoly.contains(center)) {
				numCenters++;
			}
		}
		
		return numCenters;
	}
	
	/*
	 * return the average indices for this training object
	 */
	public double[] getSegmentationAverages() {
		double[] returnA = {this.avgOverSeg, this.avgUnderSeg};
		return returnA;
	}
	
	
	// WEIGHTED AVERAGING BY NUMBER OF *YSTAR* SEGMENTS
	/*
	 * Return a double[][] where 
	 * segmentationStats[0][] = overSegmentation
	 * segmentationStats[1][] = underSegmentation
	 * and length segmentationStats[i][] = yStarSegments.size()
	 */
	public double[][] getSegmentationStats() {
		
		double[][] segmentationStats = new double[2][yStarSegments.size()];
		Segment s;

		// iterate over the Ystar list
		for (int i=0; i<yStarSegments.size(); i++) {
			s = (Segment) yStarSegments.get(i); 
			segmentationStats[0][i] = s.getOverSegmentation();
			segmentationStats[1][i] = s.getUnderSegmentation();
			// TODO: Add other metrics in here, make the return array larger??
		}
		
		return segmentationStats;
		
	}
	
	/*
	 * Changed to Y* Segments 7/6/08
	 * WEIGHTED AVERAGING BY NUMBER OF Y* SEGMENTS
	 */
	/*
	 * Return a double[][] where 
	 * segmentationStats[0][] = RAsub
	 * segmentationStats[1][] = RAsuper
	 * segmentationStats[2][] = RPsub
	 * segmentationStats[3][] = RPso
	 * segmentationStats[4][] = QLoq
	 * segmentationStats[5][] = SimSize
	 * 
	 * calculate sd's later
	 * 
	 * and length segmentationStats[i][] = intersectedSegments.size()
	 */
	public double[][] getSegmentationStats2() {
		
		double[][] segmentationStats = new double[7][yStarSegments.size()];
		Segment s;

		// iterate over the intersectedSegments
		for (int i=0; i<yStarSegments.size(); i++) {
			s = (Segment) yStarSegments.get(i);
			segmentationStats[0][i] = s.getRAsub();
			segmentationStats[1][i] = s.getRAsuper();
			segmentationStats[2][i] = s.getRPsub();
			segmentationStats[3][i] = s.getRPso();
			segmentationStats[4][i] = s.getQLoq();
			segmentationStats[5][i] = s.getSimSize();
			segmentationStats[6][i] = s.getQR();
		}
		
		return segmentationStats;
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
