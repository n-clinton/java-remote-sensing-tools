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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas Clinton
 *	
 */
public class Segment {

	private Geometry segShape;
	private double overSegmentation;
	private double underSegmentation;
	/*
	 * Moller et al.
	 */
	private double RAsub;  // the training object is the super object **metric
	private double RAsuper; // this.segShape is the super object **metric
	private double RPsub; // relative position to the training object ****Zhan et al. metric
	private double RPso; // this relies on the training object to determine and set **metric
	/*
	 * Zhan et al.
	 */
	private double qLoq; // = RPsub "Relative Position" of centroids
	private double simSize; // new one.  see constructor.
	
	/*
	 * Weidner
	 */
	private double qr;
	
	/*
	 * Constructor.  Simply set the Geometry variable.
	 */
	public Segment(Geometry p, Geometry ref) {
		segShape = p;
		//overSegmentation = 1.0 - intersectionArea(ref)/ref.getArea();
		//underSegmentation = 1.0 - intersectionArea(ref)/p.getArea();
		double isectA = intersectionArea(ref);
		
		// Moller et al.
		RAsub = isectA/ref.getArea();
		RAsuper = isectA/p.getArea();
		RPsub = p.getCentroid().distance(ref.getCentroid());
		RPso = -9999; // set in the training object
		
		// Observe:
		overSegmentation = 1.0 - RAsub;
		underSegmentation = 1.0 - RAsuper;
		
		// Zhan et al. ratio
		simSize = (Math.min(p.getArea(), ref.getArea())) / 
				  (Math.max(p.getArea(), ref.getArea()));
		
		// Weidner
		qr = 1 - isectA/(segShape.union(ref).getArea());
	}
	
	/*
	 * Encapsulate.
	 */
	public double getOverSegmentation() {
		return overSegmentation;
	}
	
	/*
	 * Encapsulate.
	 */
	public double getUnderSegmentation() {
		return underSegmentation;
	}
	
	/*
	 * Encapsulate.
	 */
	public double getArea() {
		return segShape.getArea();
	}
	
	/*
	 * Encapsulate.
	 */
	public Point getCenter() {
		return segShape.getCentroid();
	}
	
	/*
	 * Encapusulate.
	 */
	public double intersectionArea(Geometry p) {
		return (segShape.intersection(p)).getArea();
	}
	
	/*
	 * 
	 */
	public double getProportionIntersected(Geometry p) {
		return segShape.intersection(p).getArea()/this.getArea();
	}
	
	/*
	 * 
	 */
	public boolean centerIsIn(Point pt) {
		return segShape.contains(pt);
	}
	
	/*
	 * 2008 continued...
	 */
	
	// Lucieer and Stein.  Most Methods in TrainingObject.
	/*
	 * 
	 */
	public Coordinate[] getCoords() {
		return segShape.getCoordinates().clone();
	}
	
	// Moller et al.
	/*
	 * 
	 */
	public double getRAsub() {
		return RAsub;
	}
	
	/*
	 * 
	 */
	public double getRAsuper() {
		return RAsuper;
	}
	
	/*
	 * 
	 */
	public double getRPsub() {
		return RPsub;
	}
	
	/*
	 * Run before getRPso()!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 */
	public boolean setRPso(double theRPso) {
		if (theRPso < 0 || theRPso > 1.0) {
			System.err.println("Whoops!! The RPso is out of range!!");
			return false;
		}
		else {
			RPso = theRPso;
			return true;
		}
	}
	
	/*
	 * Run setRPso() first!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 */
	public double getRPso() {
		return RPso;
	}
	
	// Zhan et al.
	/*
	 * 
	 */
	public double getQLoq() {
		return RPsub;
	}
	
	/*
	 * 
	 */
	public double getSimSize() {
		return simSize;
	}
	
	/*
	 * 
	 */
	public double getQR() {
		return qr;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
