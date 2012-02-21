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


import com.berkenviro.imageprocessing.JAIUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.media.jai.PlanarImage;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import com.vividsolutions.jts.geom.Point;

/**
 * 
 * Class for constructing Weka input data sets from points as ESRI shapefiles.
 * No compatibility checking!  The user must insure that the points and the 
 * images are in the same projection.  The user must also insure that the imagery 
 * exists under all the points.  If the point is out of bounds of one of the images,
 * an exception will be thrown and the result will not be valid.
 * 
 * The points are overlaid on a stack of images specified in Main().  The writeCSV() method
 * is invoked to write the output to a comma delimited text file that can be read by Weka.
 * 
 * @author Nick Clinton
 *
 */

public class ArffMakr {

	// Hashtable with attributes as keys, PlanarImages as values
	Hashtable images;
	FeatureCollection points;
	FastVector attInfo;
	Attribute[] order;
	
	
	/**
	 * The constructor verifies existence of the shapefile and images and builds a new
	 * Hastable that makes the attribute to the image.
	 * 
	 * @param shpFileName is the name of the shapefile 
	 * @param map is a Hashtable with Weka.core.Attribute as a key, image file path as a value
	 * @return an ArffMakr that is ready to write out a training data set as a file.
	 */
	public ArffMakr(String shpFileName, Hashtable map) {
		// iniitalize the instance variable
		images = new Hashtable(); 
		
		Attribute a;
		String imageFileName;
		PlanarImage pIm;
		// these are the attributes in the instances
		attInfo = new FastVector(map.size()+2);
		
		// enumerate the attributes
		Enumeration attribs = map.keys();
		while (attribs.hasMoreElements()) {
			
			a = (Attribute) attribs.nextElement();
			
			// get the image file names, test for existence
			imageFileName = (String) map.get(a);
			try {
				pIm = JAIUtils.readImage(imageFileName);
				System.out.println("Loaded file: "+pIm.getProperty("fileName"));
				System.out.println("For Attribute: " + a.toString());
				
				// add to the images Hashtable
				images.put(a, pIm);
				// add the attribute to the FastVector
				attInfo.addElement(a);
			}
			catch (Exception e) {
				System.out.println("ArffMakr constructor error!");
				e.printStackTrace();
			}
		}
		// done with the hashtable of images and attributes
		
		System.out.println();
		System.out.println("Loading file: "+shpFileName);
		// open up the shapefile, check
		//points = gis.GISUtils.getFeatureCollection(gis.GISUtils.getURL(shpFileName));
		points = GISUtils.getFeatureCollection(new File(shpFileName));
		System.out.println("Num. features = " +points.size());
		System.out.println();
		
	}
	
	
	/**
	 * Writes a CSV file with the specified class value from the attributes of the points.  
	 * 
	 * @param outFileName is the complete file path of the output .csv as a string.
	 * @param classAtt is the attribute name to be used as the response (y).
	 */
	public void writeCSV(String outFileName, String classAtt) {
		//FeatureIterator iter = points.features();
		FeatureIterator<SimpleFeature> iter = points.features();
		
		//Feature feature = null;
		SimpleFeature feature = null;
		Point pt = null;
		// enumeration of the attributes
		Enumeration attribs;
		PlanarImage curImage;
		Attribute curAtt;
		
		BufferedWriter tabWriter = null;
		String header = "class";
		try {
			// set up and header
			tabWriter = new BufferedWriter(new FileWriter(new File(outFileName)));
			attribs = images.keys();
			while(attribs.hasMoreElements()) {
				curAtt = (Attribute) attribs.nextElement();
				header+=","+curAtt.name();
			}
			tabWriter.write(header);
			tabWriter.newLine();
			tabWriter.flush();	

			// iterate over points
			while (iter.hasNext()) {
				String line = "";
				feature = iter.next();
				System.out.println("Processing feature :"+feature.getID());
				// get the class val from the point
				line+=feature.getAttribute(classAtt);
				
				// get the point
				pt = (Point)feature.getDefaultGeometry();
				// set the other values from the imagery
				attribs = images.keys();
				while(attribs.hasMoreElements()) {
					curAtt = (Attribute) attribs.nextElement();
					curImage = (PlanarImage) images.get(curAtt);
					double curVal = JAIUtils.imageValue(pt, curImage);
					line+=","+curVal;
					//System.out.println(curAtt.name()+" = "+curVal);
				}
				tabWriter.write(line);
				tabWriter.newLine();
				tabWriter.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				tabWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/*
	 * Writes an arff file with the specified class value from the 
	 * attributes of the points.  Numeric data only.
	 * Include ID attribute names if desired.
	 */
	public void writeArff(String outFileName, String classAtt, String[] idNames) {
		// if there are id attributes, set those up in the output
		if (idNames != null) {
			for (String s : idNames) {
				Attribute idAtt = new Attribute(s);
				attInfo.addElement(idAtt);
			}
		}
		// Dummy dataset
		Instances dummy = new Instances("ArffMakr", attInfo, 1);
		// set the class attribute
		Attribute classVar = new Attribute("percent");
		// insert the response at the end
		dummy.insertAttributeAt(classVar, dummy.numAttributes());
		dummy.setClassIndex(dummy.attribute(classVar.name()).index());
		
		// initializations
		//Feature feature = null;
		SimpleFeature feature = null;
		Point pt = null;
		PlanarImage curImage;
		
		ArffSaver saver = new ArffSaver();
		try {
			saver.setFile(new File(outFileName));
			saver.setStructure(dummy);
			saver.setRetrieval(saver.INCREMENTAL);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Iterate over the points
		Instance curInst = null;
		//Iterator iter = points.iterator();
		FeatureIterator<SimpleFeature> iter = points.features();
		while (iter.hasNext()) {
			
			curInst = new Instance(dummy.numAttributes());
			curInst.setDataset(dummy);
			
			feature = iter.next();
			System.out.println("Processing feature :"+feature.getID());
			
			// get the class val from the point
			// unknown type: Integer, Long, ???
			Object classVal = feature.getAttribute(classAtt);
			double percent = getDouble(classVal);
			curInst.setClassValue(percent);
			// if there are id attributes, set those up in the output
			if (idNames != null) {
				for (String s : idNames) {
					Object idVal = feature.getAttribute(s);
					double id = getDouble(idVal);
					curInst.setValue(dummy.attribute(s), id);
				}
			}	
			
			// get the point
			pt = (Point)feature.getDefaultGeometry();
			// set the other values from the imagery, use the order[] 
			for(Attribute curAtt : order) {
				curImage = (PlanarImage) images.get(curAtt);
				// MIGHT be MISSING!!!!
				try {
					curInst.setValue(curAtt, JAIUtils.imageValue(pt, curImage));
					//System.out.println(curAtt.name()+" = "+curVal);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				saver.writeIncremental(curInst);
				//System.gc();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// done, send the saver a null to close the file
		try {
			saver.writeIncremental(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/*
	 * Helper
	 */
	public static double getDouble(Object classVal) {
		double percent = -9999;
		if (classVal instanceof Integer) {
			percent = ((Integer)classVal).doubleValue();
		}
		else if (classVal instanceof Long) {
			percent = ((Long)classVal).doubleValue();
		}
		else if (classVal instanceof Float){
			percent = ((Float)classVal).doubleValue();
		}
		else if (classVal instanceof Double){
			percent = ((Double)classVal).doubleValue();
		}
		else if (classVal instanceof String){
			percent = Double.parseDouble((String)classVal);
		}
		return percent;
	}
	
	/*
	 * Helper to enforce attribute order (insuring compatible instances)
	 * by setting an instance variable
	 */
	public void setOrder(Attribute[] thisOrder) {
		if (thisOrder.length != images.size()) {
			System.err.println("Wrong number of ordered attributes!!");
			return;
		}
		order = thisOrder;
	}
	
	
	/**
	 * Test Code and Processing Log.
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			/*
			String shpFileName = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/training/training_20090601.shp";
			String className = "class";
			
			Hashtable h1 = new Hashtable();
			// define mappings between attributes and imagery
			Attribute b1 = new weka.core.Attribute("band1");
			String img = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/imagery2/0500317_03_ref_utm17_wgs84_b1.tif";
			h1.put(b1, img);
			Attribute b3 = new weka.core.Attribute("band3");
			img = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/imagery2/0500317_03_ref_utm17_wgs84_b3.tif";
			h1.put(b3, img);
			Attribute b5 = new weka.core.Attribute("band5");
			img = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/imagery2/0500317_03_ref_utm17_wgs84_b5.tif";
			h1.put(b5, img);
			Attribute b7 = new weka.core.Attribute("band7");
			img = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/imagery2/0500317_03_ref_utm17_wgs84_b7.tif";
			h1.put(b7, img);
		
			// define an order
			Attribute[] order = {b1, b3, b5, b7};
		
			ArffMakr myArffMakr = new ArffMakr(shpFileName, h1);
			myArffMakr.setOrder(order);
			myArffMakr.writeCSV("C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/training/training_20090601.csv", className);
			*/
			
			// 20090506
			/*
			String shpFileName = "E:/cr2009/cr_training/training_20090605.shp";
			String className = "class";
			
			Hashtable h1 = new Hashtable();
			// define mappings between attributes and imagery
			Attribute b1 = new weka.core.Attribute("band1");
			String img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b1.tif";
			h1.put(b1, img);
			Attribute b2 = new weka.core.Attribute("band2");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b2.tif";
			h1.put(b2, img);
			Attribute b3 = new weka.core.Attribute("band3");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b3.tif";
			h1.put(b3, img);
			Attribute b4 = new weka.core.Attribute("band4");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b4.tif";
			h1.put(b4, img);
			Attribute b5 = new weka.core.Attribute("band5");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b5.tif";
			h1.put(b5, img);
			Attribute b6 = new weka.core.Attribute("band6");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b6.tif";
			h1.put(b6, img);
			Attribute b7 = new weka.core.Attribute("band7");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b7.tif";
			h1.put(b7, img);
			Attribute b8 = new weka.core.Attribute("band8");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b8.tif";
			h1.put(b8, img);
			Attribute b9 = new weka.core.Attribute("band9");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b9.tif";
			h1.put(b9, img);

			Attribute b10 = new weka.core.Attribute("band10");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b10.tif";
			h1.put(b10, img);
			Attribute b13 = new weka.core.Attribute("band13");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b13.tif";
			h1.put(b13, img);

			Attribute b15 = new weka.core.Attribute("band15");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b15.tif";
			h1.put(b15, img);
			Attribute b16 = new weka.core.Attribute("band16");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b16.tif";
			h1.put(b16, img);
			Attribute b17 = new weka.core.Attribute("band17");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b17.tif";
			h1.put(b17, img);
			Attribute b18 = new weka.core.Attribute("band18");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b18.tif";
			h1.put(b18, img);
			Attribute b19 = new weka.core.Attribute("band19");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b19.tif";
			h1.put(b19, img);
			
			Attribute b46 = new weka.core.Attribute("band46");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b20.tif";
			h1.put(b46, img);
			Attribute b47 = new weka.core.Attribute("band47");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b21.tif";
			h1.put(b47, img);
			Attribute b48 = new weka.core.Attribute("band48");
			img = "E:/cr2009/imagery3/MASTERL1B_0500317_03_RefTemp_22bands_utm17_su_b22.tif";
			h1.put(b48, img);
		
			// define an order
			Attribute[] order = {b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b13, b15, b16, b17, b18, b19, b46, b47, b48};
		
			ArffMakr myArffMakr = new ArffMakr(shpFileName, h1);
			myArffMakr.setOrder(order);
			myArffMakr.writeCSV("E:/cr2009/cr_training/training_20090605.csv", className);	
			*/
			
			
			// 20101206
			String shpFileName = "D:/SARP2009/ShipData/20090722_underway_tschl_turb_utm10_nad83.shp";
			String className = "MWShapeID";
			
			/*
			Hashtable h1 = new Hashtable();
			// define mappings between attributes and imagery
			Attribute b1 = new weka.core.Attribute("b41");
			String img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/b41_tempK_utm10.tif";
			h1.put(b1, img);
			Attribute b2 = new weka.core.Attribute("b42");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/b42_tempK_utm10.tif";
			h1.put(b2, img);
			Attribute b3 = new weka.core.Attribute("b43");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/b43_tempK_utm10.tif";
			h1.put(b3, img);
			Attribute b4 = new weka.core.Attribute("b44");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/b44_tempK_utm10.tif";
			h1.put(b4, img);
			Attribute b5 = new weka.core.Attribute("b45");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/b45_tempK_utm10.tif";
			h1.put(b5, img);
			Attribute b6 = new weka.core.Attribute("b46");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/b46_tempK_utm10.tif";
			h1.put(b6, img);
			Attribute b7 = new weka.core.Attribute("b47");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/b47_tempK_utm10.tif";
			h1.put(b7, img);
			Attribute b8 = new weka.core.Attribute("b48");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/b48_tempK_utm10.tif";
			h1.put(b8, img);
			Attribute b9 = new weka.core.Attribute("b49");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/b49_tempK_utm10.tif";
			h1.put(b9, img);
			Attribute b10 = new weka.core.Attribute("b50");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/b50_tempK_utm10.tif";
			h1.put(b10, img);
			Attribute b11 = new weka.core.Attribute("flh");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/ENVI Images/FLH_MASTER_gas_geo_.tif";
			h1.put(b11, img);
			*/
			Hashtable h1 = new Hashtable();
			// define mappings between attributes and imagery
			Attribute b1 = new weka.core.Attribute("b41");
			String img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/no_gas/b41_no_gas_geo.tif";
			h1.put(b1, img);
			Attribute b2 = new weka.core.Attribute("b42");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/no_gas/b42_no_gas_geo.tif";
			h1.put(b2, img);
			Attribute b3 = new weka.core.Attribute("b43");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/no_gas/b43_no_gas_geo.tif";
			h1.put(b3, img);
			Attribute b4 = new weka.core.Attribute("b44");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/no_gas/b44_no_gas_geo.tif";
			h1.put(b4, img);
			Attribute b5 = new weka.core.Attribute("b45");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/no_gas/b45_no_gas_geo.tif";
			h1.put(b5, img);
			Attribute b6 = new weka.core.Attribute("b46");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/no_gas/b46_no_gas_geo.tif";
			h1.put(b6, img);
			Attribute b7 = new weka.core.Attribute("b47");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/no_gas/b47_no_gas_geo.tif";
			h1.put(b7, img);
			Attribute b8 = new weka.core.Attribute("b48");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/no_gas/b48_no_gas_geo.tif";
			h1.put(b8, img);
			Attribute b9 = new weka.core.Attribute("b49");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/no_gas/b49_no_gas_geo.tif";
			h1.put(b9, img);
			Attribute b10 = new weka.core.Attribute("b50");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/no_gas/b50_no_gas_geo.tif";
			h1.put(b10, img);
			Attribute b11 = new weka.core.Attribute("flh");
			img = "D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/ENVI Images/FLH_MASTER_withoutgas2_geo_.tif";
			h1.put(b11, img);
		
			ArffMakr myArffMakr = new ArffMakr(shpFileName, h1);
			//myArffMakr.writeCSV("D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/gas_image_data.csv", className);
			myArffMakr.writeCSV("D:/SARP2009/dan_yaitza/SARP 2009/Atmospheric Correction/Image Files/gas/no_gas_image_data.csv", className);
			//*/
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
	}

}
