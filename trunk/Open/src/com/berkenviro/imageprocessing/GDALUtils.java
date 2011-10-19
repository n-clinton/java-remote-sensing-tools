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
package com.berkenviro.imageprocessing;

import it.geosolutions.imageio.plugins.envihdr.ENVIHdrImageReaderSpi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.imageio.ImageReader;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

/**
 * @author owner
 *
 */
public class GDALUtils {

	/**
	 * Initialize GDAL.
	 */
	static {
		System.out.println("GDAL init...");
		gdal.AllRegister();
	}
	
	/**
	 * Lookup items (which might be wavelengths) in the GDAL metadata dictionary.
	 * This was developed for a particular ENVI file format.  May or may not work with other datasets.
	 * @param fileName is the name of the input file.
	 * @return the metadata values as a double[]
	 */
	public static double[] getWavelengths(String fileName) {
		
		File f = new File(fileName);
		Dataset poDataset = (Dataset) gdal.Open(f.getAbsolutePath(), gdalconst.GA_ReadOnly);
		double[] wavelengths = new double[poDataset.getRasterCount()];
		Hashtable dict = poDataset.GetMetadata_Dict("");
		Enumeration keys = dict.keys();
		//System.out.println(dict.size() + " items of metadata found (via Hashtable dict):");
		while(keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			//System.out.println(" :" + key + ":==:" + dict.get(key) + ":");
			if (key.startsWith("Band_")) {
				String[] band = key.split("_");
				int index = Integer.parseInt(band[1]) - 1;
				wavelengths[index] = Double.parseDouble((String)dict.get(key));
				//System.out.println(" band " + index + " wavelength: " + wavelengths[index]);
			}
		}
		return wavelengths;
	}
	
	/**
	 * Use getAttribute("wavelength") instead.  AVIRIS has a weird metadata key structure.
	 * @param fileName is the AVIRIS filename
	 * @return
	 */
	@Deprecated
	public static double[] getAVIRISWavelengths(String fileName) {
		
		File f = new File(fileName);
		Dataset poDataset = (Dataset) gdal.Open(f.getAbsolutePath(), gdalconst.GA_ReadOnly);
		double[] wavelengths = new double[poDataset.getRasterCount()];
		Hashtable dict = poDataset.GetMetadata_Dict("");
		Enumeration keys = dict.keys();
		//System.out.println(dict.size() + " items of metadata found (via Hashtable dict):");
		while(keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			System.out.println(" :" + key + ":==:" + dict.get(key) + ":");
			if (key.startsWith("Band_")) {
				String[] band = key.split("_");
				int index = Integer.parseInt(band[1]) - 1;
				String[] toks = ((String)dict.get(key)).split("\\(");
				String string = toks[toks.length-1];
				String wave = string.substring(0, (string.length())-2);
				wavelengths[index] = Double.parseDouble(wave);
				//System.out.println(" band " + index + " wavelength: " + wavelengths[index]);
			}
		}
		return wavelengths;
	}
	
	/**
	 * Read an ENVI header file to retrieve the FWHMs.  Return them as a double[].
	 * GDAL is only used to get the number of bands.
	 * @param fileName is the name of the ENVI image file.
	 * @return
	 */
	public static double[] getENVIfwhm(String fileName) {
		return getAttribute(fileName, "fwhm");
	}
	
	
	/**
	 * Read an ENVI header file to retrieve a metadata value with key = attribute.
	 * GDAL is only used to get the number of bands.
	 * @param fileName is the name of the ENVI image file.
	 * @param attribute is the key for the attribute as a string.
	 * @return
	 */
	public static double[] getAttribute(String fileName, String attribute) {
		File f = new File(fileName);
		Dataset poDataset = (Dataset) gdal.Open(f.getAbsolutePath(), gdalconst.GA_ReadOnly);
		double[] array = new double[poDataset.getRasterCount()];
		String hdrName = fileName+".hdr";
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(hdrName)));
			// read each data line to this list
			ArrayList data = new ArrayList();
			// on/off switch
			boolean record = false;
			int thisrun = 1;
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains(attribute)) { // begin
					record = true; 
					continue;
				}
				if (record) {
					if (line.contains("}")) { // end
						line = line.replace("}", "");
						record = false;
					}
					String[] toks = Utils.tokenize(line);
					for (String s : toks) {
						data.add(s.trim());
					}
				}
			}
			for (int i=0; i<data.size(); i++) {
				array[i] = Double.parseDouble((String)data.get(i));
				//System.out.println("i, fwhm: "+i+", "+array[i]);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return array;
	}
	
	/**
	 * Test code and processing log.
	 * @param args
	 */
	public static void main(String[] args) {
		//String hFile = "H:/headwall/CIRPAS_2010/11-783-02_2010_10_11_19_30_39_9_rad";
		String aFile = "C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/f110609t01p00r06rdn_a/f110609t01p00r06rdn_a_sc01_ort_img";
		double[] waves = getENVIfwhm(aFile);

	}

}
