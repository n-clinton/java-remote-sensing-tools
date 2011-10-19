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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureComparators;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Geometry;

/**
 * This is a custom one-off class that is designed to search through a downloaded SPOT image
 * catalog and find imagery that meets specific criteria.  Probably not useful otherwise.
 * @author Nicholas Clinton
 */
public class ImageFinder {

	/**
	 * 
	 * @param pts
	 * @param imgs
	 * @param outTable
	 * @throws Exception
	 */
	public static void makeIntersectionTable(String pts, String imgs, String outTable) throws Exception {
		
		System.out.println(Calendar.getInstance().getTime());
		System.out.println("Making "+outTable);
		File ptsFile = new File(pts);
		File imgsFile = new File(imgs);
		FeatureCollection<SimpleFeatureType, SimpleFeature> ptsFeatures = GISUtils.getFeatureCollection(ptsFile);
		FeatureCollection<SimpleFeatureType, SimpleFeature> imgsFeatures = GISUtils.getFeatureCollection(imgsFile);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outTable));
		String header = "pointID, pointDate, type, id, date, clouds, resol, mode";
		writer.write(header);
		writer.newLine();
		
		FeatureIterator<SimpleFeature> ptsIter = ptsFeatures.features();
		
		// reference date
		Date jan2007 = new GregorianCalendar(2007, 1, 1).getTime();
		// iterate over the points
		while (ptsIter.hasNext()) {
			SimpleFeature ptFeature = ptsIter.next();
			Date ptDate = (Date)ptFeature.getAttribute("date_");
			// skip older points, for now
			if (ptDate.before(jan2007)) { continue; }
			
			// Make a list of dates, including the point date
			LinkedList<SimpleFeature> list = new LinkedList<SimpleFeature>();
			list.add(ptFeature);

			// now check the intercepts
			System.out.println();
			System.out.println("Intersecting point "+ptFeature.getID());
			Geometry ptGeo = (Geometry)ptFeature.getDefaultGeometry();
			FeatureIterator<SimpleFeature> imgIter = imgsFeatures.features();
			while (imgIter.hasNext()) {
				SimpleFeature poly = imgIter.next();
				Geometry imgGeo = (Geometry)poly.getDefaultGeometry();
				if (ptGeo.within(imgGeo)) {
					//System.out.println(poly.getAttribute("DATE_ACQ")); // original field SPOT
					//Date polyDate = (Date)poly.getAttribute("date_"); // added manually
					list.add(poly);
				}
			}
			
			// Make a comparator for the features
			Comparator c = FeatureComparators.byAttributeName("date_");
			// searched the images, filled the list, sort it
			Collections.sort(list, c);
			// search for the position of the point
			int ptIndex = Collections.binarySearch(list, ptFeature, c);
			System.out.println("pt is "+ptIndex+" of "+list.size());
			
			System.out.println("Searching imagery...");
			SimpleFeature ft;
			int pre = 0; 
			int post = 0;
			// list of possible lines to write
			LinkedList<String> lineList = new LinkedList<String>();
			for (int i=0; i<list.size(); i++) {
				//if (i == ptIndex) { continue; }
				ft = (SimpleFeature)list.get(i);
				if (ft.equals(ptFeature)) { continue; }
				String line = "";
				
				double cloudper = (Double)ft.getAttribute("CLOUD_PER");
				double resol = (Double)ft.getAttribute("RESOL");
				String mode = (String)ft.getAttribute("MODE");
				// image selection logic
				if (cloudper < 11.0) {
					if (resol < 5.0) {
						if (mode.trim().equals("COLOR")) {
							Date d = (Date)ft.getAttribute("date_");
							String prepost;
							if (d.compareTo(ptDate) < 0) {
								prepost = "pre";
								pre++;
							}
							else {
								prepost = "post";
								post++;
							}
							//String prepost = (d.compareTo(ptDate) < 0) ? "pre" : "post";
							line+=ptFeature.getID()+",";
							line+=ptDate+",";
							line+=prepost+",";
							line+=ft.getID()+",";
							line+=d+",";
							line+=ft.getAttribute("CLOUD_PER")+",";
							line+=ft.getAttribute("RESOL")+",";
							line+=ft.getAttribute("MODE");
//							writer.write(line);
//							writer.newLine();
//							writer.flush();
							System.out.println(line);
							lineList.add(line);
						}
					}
				}
			} // end list
			if (pre > 0 && post > 0) {
				Iterator iter = lineList.iterator();
				while (iter.hasNext()) {
					writer.write((String)iter.next());
					writer.newLine();
				}
				writer.flush();
			} 
			else {
				System.out.println("No pre or post. Skipping this point...");
			}
			
		} // end point
		writer.close();
	}
	
	/**
	 * Temporary helper function.  Original date format in SPOT archive shapefiles.
	 * @param spotField
	 * @return
	 */
	private Date dateFromSPOT(String spotField) {
		// turn the date string into a Date
		String[] dmy = spotField.split("/");
		GregorianCalendar imgCal = new GregorianCalendar(
				Integer.parseInt(dmy[2]),
				Integer.parseInt(dmy[1])-1,
				Integer.parseInt(dmy[0]));

		return imgCal.getTime();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		try {
			String pts = "C:/Users/owner/Documents/Forest_Carbon/locator_shapefiles/Global1kmDecrease_NE_Borneo_date_joined.shp";
			//String pts = "C:/Users/nick/Documents/Forest_Carbon/locator_shapefiles/Global1kmDecrease_group2.shp";
			//String pts = "C:/Users/nick/Documents/Forest_Carbon/locator_shapefiles/Global1kmDecrease_group1_7_merge.shp";
			//GISUtils.printAttributes(pts);
			String imgs = "C:/Users/owner/Documents/Forest_Carbon/locator_shapefiles/SPOT_Borneo_2006_2010.shp";
			//GISUtils.printAttributes(imgs);
			//String outTable = "C:/Users/nick/Documents/Forest_Carbon/SPOT_group1_7_merge_image_table_20110209.csv";
			//String outTable = "C:/Users/nick/Documents/Forest_Carbon/SPOT_group1_7_merge_r25_c10_table.csv";
			String outTable = "C:/Users/owner/Documents/Forest_Carbon/Global1kmDecrease_NE_Borneo_r25_c10_table.csv";
			makeIntersectionTable(pts, imgs, outTable);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
