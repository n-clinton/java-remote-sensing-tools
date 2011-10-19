/*
 *  Copyright (C) 2008-2011  Nicholas Clinton
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import javax.swing.JFileChooser;

//import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;



/**
 * Soquel Demonstration State Forest custom class for doing 
 * a WHR classification from the attributes
 * of a shapefile.  Writes a table with WHR classes.  An input
 * shapefile is chosen from a dialog and must have an 'id' field
 * in the attributes.
 * COMMENTED due to incompatibility with newer Geotools.
 */
public class WHRclassr {
	
	/*
	// The polygons to be classified
	FeatureCollection shapes;
	File myFile;
	
	
	 * Constructor initializes the shapes from a shapefile;
	 
	public WHRclassr() {
		// launch the dialog to open a shapefile, assumed polygons
		shapes = getFile();
	}
	
	
	 * Get a feature collection from a shapefile using a dialog.
	 * Initialize myFile.
	 
	public FeatureCollection getFile() {
		// get the shapefile URL by loading it from the file system
		URL shapeURL = null;
		File f = null;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new SimpleFileFilter("shp", "Shapefile"));
		int result = fileChooser.showOpenDialog(null);
		try {
			if (result == JFileChooser.APPROVE_OPTION) {
				f = fileChooser.getSelectedFile();
				myFile = f;
				shapeURL = f.toURL();	
			} else {
				return null;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		// Get the feature collection from the supplied URL
		FeatureCollection fc = gis.Utils.getFeatureCollection(shapeURL);
		return fc;
	}
	
	
	 * Return a WRH class based on the data in the attributes of the shapefile.
	 * Custom for SDSF.  returns {type, size, closure}
	 
	public String[] whrClass(Feature f) {

		String type = "";
		String size = "";
		String closure = "";
		
		// get ccrw, ccdf, cchw, dbh from the feature
		double[] data = getData(f);
		// convert from 8bit (except dbh) to percent
		//double ccrw = (data[0]/255.0)*100;
		double ccrw = data[0];
		//System.out.println("ccrw = "+ccrw);
		//double ccdf = (data[1]/255.0)*100;
		double ccdf = data[1];
		//System.out.println("ccdf = "+ccdf);
		//double cchw = (data[2]/255.0)*100;
		double cchw = data[2];
		//System.out.println("cchw = "+cchw);
		double dbh = data[3];
		//System.out.println("dbh = "+dbh);
		// define these as follows:
		double cctot = ccrw+ccdf+cchw;
		double cccon = ccrw+ccdf;
		
		// classification:
		if (cctot >= 10.0) {  // more than 10% total canopy closure: tree
			if (cccon >= 50.0) { // more than 50% conifer, regardless of HW
				if (ccrw > ccdf) {
					type = "RDW";
				}
				else {
					type = "DFR";
				}
			}
			else { // there is less than 50% conifer
				// the following is my interpretation of Intro. Fig.1 and MHC description
				if (cchw >= 25.0 & cccon >= 25.0) {  // mixed
					type = "MHC";
				}
				else {  // one or both of HW and conifer are less than 25%
					if (cccon >= cchw) { // conifer
						// same rule as for over 50% conifer
						if (ccrw > ccdf) {
							type = "RDW";
						}
						else {
							type = "DFR";
						}
					}
					else { // hardwood
						type = "COW"; 
						// could also be "MRI", check spatially
					}
				}
			}
		}
		else { // not tree, assumed shrub
			return new String[] {"MCH", "0", "D"};
		}
		
		// check canopy closure
		closure = whrDensity(cctot);
	
		// check the size classes
		size = whrSize(dbh);
		
		return new String[] {type, size, closure};
	}
	
	
	 * Implement WHR density using canopy closure
	 
	private String whrDensity(double cc) {
		if (cc >= 10.0 & cc < 25.0) {
			return "S";
		}
		else if (cc >= 25.0 & cc < 40.0) {
			return "P";
		}
		else if (cc >= 40.0 & cc < 60.0) {
			return "M";
		}
		else if (cc >= 60.0) {
			return "D";
		}
		else {
			return "NA";
		}
	}
	
	
	 * Implement WHR size using dbh
	 
	private String whrSize(double dbh) {
		if (dbh == 0.0) {
			return "NA";
		}
		else if (dbh < 1.0) {
			return "1";
		}
		else if (dbh >= 1.0 & dbh < 6.0) {
			return "2";
		}
		else if (dbh >= 6.0 & dbh < 12.0) {
			return "3";
		}
		else if (dbh >= 12.0 & dbh < 24.0) {
			return "4";
		}
		else if (dbh >= 24.0) {
			return "5";
		}
		else {
			return "NA";
		}
	}
	
	
	
	 * Get the double[] {ccrw, ccdf, cchw, dbh} from attributes.
	 
	public double[] getData(Feature f) {
		// These are the return values for crown closure and average dbh  
		double ccrw=0.0, ccdf=0.0, cchw=0.0, dbh=0.0;
		FeatureType ft = f.getFeatureType();
		// extract the info from the attributes
		for (int i = 0; i < f.getNumberOfAttributes(); i++) {
			String atName = ft.getAttributeType(i).getName();
			Object attribute = f.getAttribute(i);
			// the following defines the field mappings:
			//******************************************
			// bands are indexed from zero, first three are RGB
			if(atName.contains("b3_mean")) {
				ccrw = Double.parseDouble(attribute.toString());
				//System.out.println("ccrw = "+ccrw);
			}
			if(atName.contains("b4_mean")) {
				ccdf = Double.parseDouble(attribute.toString());
				//System.out.println("ccdf = "+ccdf);
			} 
			if(atName.contains("b5_mean")) {
				cchw = Double.parseDouble(attribute.toString());
				//System.out.println("cchw = "+cchw);
			}
			if(atName.contains("b6_mean")) {
				dbh = Double.parseDouble(attribute.toString());
				//System.out.println("dbh = "+dbh);
			}
			//**********************************************
		}
		return new double[] {ccrw, ccdf, cchw, dbh};
	}
	
	
	 * Get the id from the feature.
	 
	public String getID(Feature f, String idName) {
		
		FeatureType ft = f.getFeatureType();
		for (int i = 0; i < ft.getAttributeCount(); i++) {
			String atName = ft.getAttributeType(i).getName();
		    if (atName.equals(idName)) {
		    	Object attribute = f.getAttribute(i);
		    	return attribute.toString();
		    }
		}
		return "null";  // couldn't find it
	}
	
	
	 * Build the link table FOR COMBINED TREES.
	 * Save to table with inputName_whrTab.txt
	 
	public void writeTab(String attID) {
		
		Iterator iterator = shapes.iterator();
		String path = myFile.getPath();
		String outFileName = path.substring(0, path.length()-4)+"_whrTab.txt";
		System.out.println("Saving to: "+outFileName);
		
		try {
			// open a writer 
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFileName));
			
			// write the field names, self explanatory
			writer.write("id"+","+"whrType"+","+"whrSize"+","+"whrClosure");
			writer.newLine();
			int count = 0;
			while (iterator.hasNext()) {
				count++;
				Feature feature = (Feature)iterator.next();
				System.out.println("Processing feature: "+feature.getID() + " ..."+"\t");
				String[] newLine = whrClass(feature);
				String id = getID(feature, attID);
				System.out.println(id+","+ newLine[0]+","+newLine[1]+","+newLine[2]);
				writer.write(getID(feature, attID) +","+ newLine[0]+","+newLine[1]+","+newLine[2]);
				writer.newLine();
			}
			// done. close the writer
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		     shapes.close(iterator);
		}	
	}
	
	
	
	 * Build the link table for UPPER AND LOWER STORIES.
	 * Save to table with inputName_whrTab.txt
	 
	public void writeTab2(String attID) {
		
		Iterator iterator = shapes.iterator();
		String path = myFile.getPath();
		String outFileName = path.substring(0, path.length()-4)+"_whrTab.txt";
		System.out.println("Saving to: "+outFileName);
		
		try {
			// open a writer 
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFileName));
			
			// write the field names
			writer.write("id"+","+"whrClosureUp"+","+"whrSizeUp"+","+"whrClosureLow"+","+"whrSizeLow");
			writer.newLine();
			int count = 0;
			while (iterator.hasNext()) {
				count++;
				Feature feature = (Feature)iterator.next();
				System.out.println("Processing feature: "+feature.getID() + " ..."+"\t");
				//String[] newLine = whrClass(feature);
				String[] newLine = whrSize(feature);
				String id = getID(feature, attID);
				System.out.println(id+","+ newLine[0]+","+newLine[1]+","+newLine[2]+","+newLine[3]);
				writer.write(id +","+ newLine[0]+","+newLine[1]+","+newLine[2]+","+newLine[3]);
				writer.newLine();
			}
			// done. close the writer
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		     shapes.close(iterator);
		}	
	}
	
	
	 * Return a size and density for the upperStory and lowerStory as
	 * {upDensity, upSize, lowDensity, lowSize}
	 
	public String[] whrSize(Feature f) {
		
		String upDensity = "";
		String upSize = "";
		String lowDensity = "";
		String lowSize = "";
		
		// get data from the feature
		double[] data = getData2(f);
		// convert to percent
		double cc_up = data[0]*100;
		//System.out.println("cc_up = "+cc_up);
		double qmd_up = data[1];
		//System.out.println("qmd_up = "+qmd_up);
		double cc_low = data[2]*100;
		// System.out.println("cc_low = "+cc_low);
		double qmd_low = data[3];
		//System.out.println("qmd_low = "+qmd_low);
		
		upDensity = whrDensity(cc_up);
		upSize = whrSize(qmd_up);
		lowDensity = whrDensity(cc_low);
		lowSize = whrSize(qmd_low);
		
		return new String[] {upDensity, upSize, lowDensity, lowSize};
	}
	
	
	 * Get {upDensity, upSize, lowDensity, lowSize} as doubles
	 * Attributes named as below.
	 
	public double[] getData2(Feature f) {
		// These are the return values for crown closure and average dbh  
		double upDensity=0.0, upSize=0.0, lowDensity=0.0, lowSize=0.0;
		FeatureType ft = f.getFeatureType();
		// extract the info from the attributes
		for (int i = 0; i < f.getNumberOfAttributes(); i++) {
			String atName = ft.getAttributeType(i).getName();
			Object attribute = f.getAttribute(i);
			// the following defines the field mappings:
			//******************************************
			// bands are indexed from zero, first three are RGB
			if(atName.equals("MEAN")) {
				upDensity = Double.parseDouble(attribute.toString());
				//System.out.println("upDensity = "+upDensity);
			}
			if(atName.equals("MEAN_1")) {
				upSize = Double.parseDouble(attribute.toString());
				//System.out.println("upSize = "+upSize);
			} 
			if(atName.equals("MEAN_12")) {
				lowDensity = Double.parseDouble(attribute.toString());
				//System.out.println("lowDensity = "+cchwlowDensity);
			}
			if(atName.equals("MEAN_12_13")) {
				lowSize = Double.parseDouble(attribute.toString());
				//System.out.println("lowSize = "+lowSize);
			}
			//**********************************************
		}
		return new double[] {upDensity, upSize, lowDensity, lowSize};
	}
	
	
	*//**
	 * @param args
	 *//*
	public static void main(String[] args) {
		
		// entry point to the class
		WHRclassr whr = new WHRclassr();
		// testing to windows temp dir
		//whr.writeTab("C:\\DOCUME~1\\NICHOL~1\\LOCALS~1\\Temp\\junkFile", "id");
		
		//whr.writeTab("id");
		whr.writeTab2("id");

	}*/

}
