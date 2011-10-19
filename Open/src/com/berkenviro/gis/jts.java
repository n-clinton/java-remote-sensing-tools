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
package com.berkenviro.gis;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * 
 */

/**
 * Test class.
 * @author Nicholas Clinton
 */
public class jts {

	public static Polygon polyFromCoords(String coordString) {
		// parse the coordString
		double[][] latlngArray = new double[5][2];
		latlngArray[0][0] = 38.0;
		latlngArray[0][1] = -121.0;
		latlngArray[1][0] = 38.0;
		latlngArray[1][1] = -120.0;
		latlngArray[2][0] = 39.0;
		latlngArray[2][1] = -120.0;
		latlngArray[3][0] = 39.0;
		latlngArray[3][1] = -121.0;
		latlngArray[4][0] = 38.0;
		latlngArray[4][1] = -121.0;
		
		Coordinate[] coordArray = new Coordinate[5];
		coordArray[0] = new Coordinate(latlngArray[0][1], latlngArray[0][0]);
		coordArray[1] = new Coordinate(latlngArray[1][1], latlngArray[1][0]);
		coordArray[2] = new Coordinate(latlngArray[2][1], latlngArray[2][0]);
		coordArray[3] = new Coordinate(latlngArray[3][1], latlngArray[3][0]);
		coordArray[4] = new Coordinate(latlngArray[4][1], latlngArray[4][0]);
		
		CoordinateArraySequence cas = new CoordinateArraySequence(coordArray);
		PrecisionModel pm = new PrecisionModel();
		GeometryFactory gf = new GeometryFactory(pm);
		LinearRing lr = new LinearRing(cas, gf); 
		Polygon p = new Polygon(lr, null, gf);
		
		return p;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Polygon p = polyFromCoords("String coordString");
		System.out.println("Area is: "+p.getArea());
		System.out.println("Polygon: "+p.toString());

	}

}
