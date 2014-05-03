/*
 * Copyright (c) 2011, Nicholas Clinton
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.  Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution.  The names of its contributors may not be
 * used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * This software is based on these references:
 * 
 * Verhoef, Wout and Heike Bach. 2003. Simulation of hyperspectral and directional
 * radiance images using coupled biophysical and atmospheric radiative transfer models.
 * Remote Sensing of Environment. 87:23-41
 * 
 * http://dragon2.esa.int/landtraining2010/pdf/D2L5a_verhoef.pdf
 *
 * Liang, Shunlin. 2004. Quantitative Remote Sensing of Land Surfaces. John Wiley &
 * Sons. Hoboken, NJ.
 * 
 * Pu, Ruiliang, Peng Gong, G.S. Biging. 2003. Simple calibration of AVIRIS data and
 * LAI mapping of forest plantation in southern Argentina. International Journal of Remote
 * Sensing. 24(23):4699-4714
 * 
 * 
 */
package com.berkenviro.imageprocessing;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.io.output.TeeOutputStream;



/**
 * 
 * This class is for extracting atmospheric parameters from the output of Modtran 4.
 * There are methods for reading the tape7 file, generating radiative transfer variables,
 * and processing single band imagery to physical units.  Each band of the image is handled
 * separately, in a different file.  The methods are static and the processing log can be found
 * in the main method.
 * 
 * @author Nick Clinton
 * 
 * Last mods 20110622
 *
 */
public class MODTRANprocessor {
	
	// constants
	// Planck's constant J/s
	static double h = 6.626068*Math.pow(10.0, -34.0);
	// speed of light m/s
	static double c = 2.99792458*Math.pow(10.0, 8.0);
	// Boltzmann's constant m^2 * kg / s^2 / K
	static double k = 1.3806503 * Math.pow(10.0, -23.0);
	
	
	/**
	 * Method to read a Modtran4 tape 7 file into an array.
	 * Columns of a tape7 are:
	 * 0. FREQ	
	 * 1. TOT TRANS	
	 * 2. PTH THRML	
	 * 3. THRML SCT	
	 * 4. SURF EMIS	
	 * 5. SOL SCAT	
	 * 6. SING SCAT	
	 * 7. GRND RFLT	
	 * 8. DRCT RFLT	
	 * 9. TOTAL RAD	
	 * 10. REF SOL	
	 * 11. SOL@OBS	
	 * 12. DEPTH
	 * @param file is the absolute path of the input file
	 * @param run is the run to read from a multi-run file. 1 for a single run file.
	 * @return doubl[][] in which each row is a column of the tape 7 file
	 */
	public static double[][] readTape7(String file, int run) {
		double[][] output = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(file)));
			// read each data line to this list
			ArrayList data = new ArrayList();
			// on/off switch
			boolean record = false;
			int thisrun = 1;
			String line;
			while ((line = reader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);
				String token = null;
				if (thisrun != run) {
					continue; // just skip
				}
				// check the first token of the line
				if ((token = st.nextToken()).equals("FREQ")) { 
					record = true;  // start reading from the next line
					continue;
				}
				else if (token.equals("-9999.")) {
					record = false;  // stop recording
					thisrun++;  // increment the run counter
					continue;
				}
				// if we're in the run, and we're in the data, add the line
				if (record) {
					data.add(line);
					//System.out.println(line);
				}
			}
			
			// file has been read, data is in the list, convert to a double[]
			output = new double [13][data.size()];
			for (int i=0; i<data.size(); i++) {
				String toParse = (String) data.get(i);
				String theDouble;
				// fixed format based extraction
				theDouble = toParse.substring(0, 7).trim();
				output[0][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // FREQ
				theDouble = toParse.substring(7, 18).trim();
				output[1][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // TOT TRANS
				theDouble = toParse.substring(18, 29).trim();
				output[2][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // PTH THRML
				theDouble = toParse.substring(29, 40).trim();
				output[3][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // THRML SCT
				theDouble = toParse.substring(40, 51).trim();
				output[4][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // SURF EMIS
				theDouble = toParse.substring(51, 62).trim();
				output[5][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // SOL SCAT
				theDouble = toParse.substring(62, 73).trim();
				output[6][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // SING SCAT
				theDouble = toParse.substring(73, 84).trim();
				output[7][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // GRND RFLT
				theDouble = toParse.substring(84, 95).trim();
				output[8][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // DRCT RFLT
				theDouble = toParse.substring(95, 106).trim();
				output[9][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // TOTAL RAD
				theDouble = toParse.substring(106, 115).trim();
				output[10][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // REF SOL
				theDouble = toParse.substring(115, 124).trim();
				output[11][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // SOL@OBS
				theDouble = toParse.substring(124, 132).trim();
				output[12][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // DEPTH
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;	
	}
	
	/**
	 * Method to read a Modtran5 tape7 file into an array.
	 * Columns of a tape7 are:
	 * 0. FREQ	
	 * 1. TOT TRANS	
	 * 2. PTH THRML	
	 * 3. THRML SCT	
	 * 4. SURF EMIS	
	 * 5. SOL SCAT	
	 * 6. SING SCAT	
	 * 7. GRND RFLT	
	 * 8. DRCT RFLT	
	 * 9. TOTAL RAD	
	 * 10. REF SOL	
	 * 11. SOL@OBS	
	 * 12. DEPTH
	 * 13. DIR_EM
	 * 14. TOA_SUN
	 * @param file is the absolute path of the input file
	 * @param run is the run to read from a multi-run file. 1 for a single run file.
	 * @return doubl[][] in which each row is a column of the tape 7 file
	 */
	public static double[][] readTape7mod5(String file, int run) {
		double[][] output = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(file)));
			// read each data line to this list
			ArrayList data = new ArrayList();
			// on/off switch
			boolean record = false;
			int thisrun = 1;
			String line;
			while ((line = reader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);
				String token = null;
				if (thisrun != run) {
					continue; // just skip
				}
				// check the first token of the line
				if ((token = st.nextToken()).equals("FREQ")) { 
					record = true;  // start reading from the next line
					continue;
				}
				else if (token.equals("-9999.")) {
					record = false;  // stop recording
					thisrun++;  // increment the run counter
					continue;
				}
				// if we're in the run, and we're in the data, add the line
				if (record) {
					data.add(line);
					//System.out.println(line);
				}
			}
			
			boolean print = false;
			
			// file has been read, data is in the list, convert to a double[]
			output = new double [15][data.size()];
			for (int i=0; i<data.size(); i++) {
				String toParse = (String) data.get(i);
				String theDouble;
				// fixed format based extraction
				theDouble = toParse.substring(0, 8).trim();
				if (print) { System.out.println(toParse.substring(0, 8).trim()); }
				output[0][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // FREQ
				theDouble = toParse.substring(8, 19).trim();
				if (print) { System.out.println(toParse.substring(8, 19).trim()); }
				output[1][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // TOT TRANS
				theDouble = toParse.substring(19, 30).trim();
				if (print) { System.out.println(toParse.substring(19, 30).trim()); }
				output[2][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // PTH THRML
				theDouble = toParse.substring(30, 41).trim();
				if (print) { System.out.println(toParse.substring(30, 41).trim()); }
				output[3][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // THRML SCT
				theDouble = toParse.substring(41, 52).trim();
				if (print) { System.out.println(toParse.substring(41, 52).trim()); }
				output[4][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // SURF EMIS
				theDouble = toParse.substring(52, 63).trim();
				if (print) { System.out.println(toParse.substring(52, 63).trim()); }
				output[5][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // SOL SCAT
				theDouble = toParse.substring(63, 74).trim();
				if (print) { System.out.println(toParse.substring(63, 74).trim()); }
				output[6][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // SING SCAT
				theDouble = toParse.substring(74, 85).trim();
				if (print) { System.out.println(toParse.substring(74, 85).trim()); }
				output[7][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // GRND RFLT
				theDouble = toParse.substring(85, 96).trim();
				if (print) { System.out.println(toParse.substring(85, 96).trim()); }
				output[8][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // DRCT RFLT
				theDouble = toParse.substring(96, 107).trim();
				if (print) { System.out.println(toParse.substring(96, 107).trim()); }
				output[9][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // TOTAL RAD
				theDouble = toParse.substring(107, 116).trim();
				if (print) { System.out.println(toParse.substring(107, 116).trim()); }
				output[10][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // REF SOL
				theDouble = toParse.substring(116, 125).trim();
				if (print) { System.out.println(toParse.substring(116, 125).trim()); }
				output[11][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // SOL@OBS
				theDouble = toParse.substring(125, 133).trim();
				if (print) { System.out.println(toParse.substring(125, 133).trim()); }
				output[12][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // DEPTH
				theDouble = toParse.substring(133, 140).trim();
				if (print) { System.out.println(toParse.substring(133, 140).trim()); }
				output[13][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // DIR_EM
				theDouble = toParse.substring(140, 151).trim();
				if (print) { System.out.println(toParse.substring(140, 151).trim()); }
				output[14][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // TOA_SUN
				
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;	
	}
	
	
	/**
	 * Method to read a Modtran5 ACD file into an array.
	 * Columns of an are:
	 * 0. FREQ(cm-1)	
	 * 1. k-int  	
	 * 2. Weight    	
	 * 3. Diffus_Sun_Tr 	
	 * 4. Direct_Sun_Tr 	
	 * 5. Diffus_Obs_Tr 	
	 * 6. Direct_Obs_Tr 	
	 * 7. Spherical_Alb	
	 * @param file is the absolute path of the input file
	 * @return double[][] {FREQ(cm-1), Diffus_Sun_Tr, Direct_Sun_Tr, Diffus_Obs_Tr, Direct_Obs_Tr, Spherical_Alb}
	 */
	public static double[][] readACDmod5(String file) {
		double[][] output = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(file)));
			// read each data line to this list
			ArrayList data = new ArrayList();
			// on/off switch
			boolean record = false;
			String line;
			while ((line = reader.readLine()) != null) {
				// first line is blank
				if (line.equals("")) { continue; }
				// header line
				if (line.contains("FREQ")) { 
					record = true;  // start reading from the next line
					continue;
				}
				// if we're in the run, and we're in the data, add the line
				if (record) {
					data.add(line);
					//System.out.println(line);
				}
			}
			
			// file has been read, data is in the list, convert to a double[]
			output = new double [8][data.size()];
			for (int i=0; i<data.size(); i++) {
				String[] tokens = Utils.tokenize((String) data.get(i));
				for (int j=0; j<output.length; j++) {
					output[j][i] = (new Double(tokens[j])).doubleValue();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;	
	}
	
	
	
	/**
	 * Read a tape 7 file from a modtran transmittance run.
	 * Columns of a tape7 are:
	 * 0. FREQ	
	 * 1. TOT TRANS	
	 * 
	 * @param file is the absolute path of the input file
	 * @param run is the run to read from a multi-run file. 1 for a single run file.
	 * @return doubl[][] in which each row is a column of the tape 7 file
	 */
	public static double[][] readTape7trans(String file, int run) {
		double[][] output = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(file)));
			// read each data line to this list
			ArrayList data = new ArrayList();
			// on/off switch
			boolean record = false;
			int thisrun = 1;
			String line;
			while ((line = reader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);
				String token = null;
				if (thisrun != run) {
					continue; // just skip
				}
				// check the first token of the line
				if ((token = st.nextToken()).equals("CM-1")) { 
					record = true;  // start reading from the next line
					continue;
				}
				else if (token.equals("-9999.")) {
					record = false;  // stop recording
					thisrun++;  // increment the run counter
					continue;
				}
				// if we're in the run, and we're in the data, add the line
				if (record) {
					data.add(line);
					//System.out.println(line);
				}
			}
			
			// file has been read, data is in the list, convert to a double[]
			output = new double [22][data.size()];
			for (int i=0; i<data.size(); i++) {
				String toParse = (String) data.get(i);
				// Modtran4 fixed format based extraction
				///*
				String theDouble;
				theDouble = toParse.substring(0, 6).trim();
				output[0][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // FREQ
				theDouble = toParse.substring(6, 12).trim();
				output[1][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // TOT TRANS
				theDouble = toParse.substring(12, 18).trim();
				output[2][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // H2O TRANS
				theDouble = toParse.substring(18, 24).trim();
				output[3][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // CO2+ TRANS
				theDouble = toParse.substring(24, 30).trim();
				output[4][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // O3 TRANS
				theDouble = toParse.substring(30, 36).trim();
				output[5][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // TRACE TRANS
				theDouble = toParse.substring(36, 42).trim();
				output[6][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // N2 CONT
				theDouble = toParse.substring(42, 48).trim();
				output[7][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // H2O CONT
				theDouble = toParse.substring(48, 54).trim();
				output[8][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // MOLEC SCAT
				theDouble = toParse.substring(54, 60).trim();
				output[9][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // AER TRANS
				theDouble = toParse.substring(60, 66).trim();
				output[10][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // HNO3 TRANS
				theDouble = toParse.substring(66, 72).trim();
				output[11][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // AERab TRANS
				theDouble = toParse.substring(72, 78).trim();
				output[12][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // -LOG TOTAL
				theDouble = toParse.substring(78, 84).trim();
				output[13][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // CO2 TRANS
				theDouble = toParse.substring(84, 90).trim();
				output[14][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // CO TRANS
				theDouble = toParse.substring(90, 96).trim();
				output[15][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // CH4 TRANS
				theDouble = toParse.substring(96, 102).trim();
				output[16][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // N2O TRANS
				theDouble = toParse.substring(102, 108).trim();
				output[17][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // O2 TRANS
				theDouble = toParse.substring(108, 114).trim();
				output[18][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // NH3 TRANS
				theDouble = toParse.substring(114, 120).trim();
				output[19][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // NO TRANS
				theDouble = toParse.substring(120, 126).trim();
				output[20][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // NO2 TRANS
				theDouble = toParse.substring(126, 132).trim();
				output[21][i] = theDouble.equals("") ? 0.0 : (new BigDecimal(theDouble)).doubleValue(); // SO2 TRANS
				
				
				//*/
				// Modtran 5 method
				/*
				String [] tokens = Utils.tokenize(toParse);
				output[0][i] = Double.parseDouble(tokens[0]);
				output[1][i] = Double.parseDouble(tokens[1]);
				//*/
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;	
	}
	
	
	/**
	 * Update the modroot.in file.  Assumes tape 5 files exist.
	 * @param root is the absolute path name of modroot.in
	 */
	public static void modroot(String root) {
		try {
			FileWriter writer = new FileWriter(new File("C:/Documents and Settings/nick/My Documents/MODTRAN/mod4v2r1/PC/modroot.in"));
			writer.write("TEST/"+root);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Programmed execution of MODTRAN4, tape5 assumed already to exist.
	 * Works, but awaits user interaction with the annoying window that pops up.
	 */
	public static void runM() {
		try {
			// add arguments, if desired
			String cmd = "\"C:/Documents and Settings/nick/My Documents/MODTRAN/mod4v2r1/PC/4V2R1.exe\"";
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.directory(new File("C:/Documents and Settings/nick/My Documents/MODTRAN/mod4v2r1/PC"));
			Process p = pb.start();
			p.waitFor();
			System.out.println("Modtran exit code: " + p.exitValue());

		}
		catch (Exception err) {
			err.printStackTrace();
		}
	}
	            
	/**
	 * Derivation of spherical albedo, total transmitance, and path radiance using the 
	 * method of Verhoef and Bach (2003).
	 * @param ref0 is zero reflectance, generated by dataprocessing.MODTRANprocessor.readTape7()
	 * @param ref05 is 0.5 reflectance, generated by dataprocessing.MODTRANprocessor.readTape7()
	 * @param ref1 is 1.0 reflectance, generated by dataprocessing.MODTRANprocessor.readTape7()
	 * @param srf has been read from dataprocessing.SRFProcessr.readSRF()
	 * @param sensor_zenith is the solar-zenith angle in degrees
	 * @param exo is exo-atmospheric irradiance in W/M^2/micron
	 * @return a double array {path radiance, spherical albedo, total two way transmittance}
	 * @see com.berkenviro.imageprocessing.Utils#convolve(double[][], double[][])
	 */
	public static double[] vbMod4(double[][] ref0, double[][] ref05, double[][] ref1, double[][] srf, double[][] exo, double sensor_zenith) {
		// initializations:
		
		// vector of wavelength in nanometers
		double[] lambda_nm = new double[ref0[0].length];
		// simulated at sensor radiance, modtran TOTAL RAD (column 9), NOT USED
		double[] rad0 = new double[ref0[0].length];
		double[] rad05 = new double[ref0[0].length];
		double[] rad1 = new double[ref0[0].length];
		// Verhoef and Bach PATH, modtran SOL SCAT (column 5)
		double[] path0 = new double[ref0[0].length];
		double[] path05 = new double[ref0[0].length];
		double[] path1 = new double[ref0[0].length];
		// Verhoef and Bach GSUN, modtran DRCT RFLT (column 8)
		double[] drct_reflt0 = new double[ref0[0].length];
		double[] drct_reflt05 = new double[ref0[0].length];
		double[] drct_reflt1 = new double[ref0[0].length];
		// Verhoef and Bach GTOT, modtran GRND RFLT (column 7)
		double[] gnd_reflt0 = new double[ref0[0].length];
		double[] gnd_reflt05 = new double[ref0[0].length];
		double[] gnd_reflt1 = new double[ref0[0].length];
		// total transmittance gnd-sensor path, modtran TOT TRANS (column 1)
		double[] tao = new double[ref0[0].length];
		// optical depth, modtran DEPTH (column 12)
		double[] b = new double[ref0[0].length];
		// Verhoef, Wuhan 2008
		double[] ref_sol1 = new double[ref0[0].length];
		double[] sol_at_obs1 = new double[ref0[0].length];
		
		// Convert to watts/square meter/steradian/micron by multiplying by squared wavenumber
		for (int i=0; i<ref0[0].length; i++) {
			// compute a vector of wavelength in nanometers
			lambda_nm[i] = 1.0 / ref0[0][i] * Math.pow(10, 7);
			// simulated at sensor radiance, modtran TOTAL RAD (column 9), NOT USED
			rad0[i] = Math.pow(ref0[0][i], 2) * ref0[9][i];
			rad05[i] = Math.pow(ref05[0][i], 2) * ref05[9][i];
			rad1[i] = Math.pow(ref1[0][i], 2) * ref1[9][i];
			// Verhoef and Bach PATH, modtran SOL SCAT (column 5)
			path0[i] = Math.pow(ref0[0][i], 2) * ref0[5][i];
			path05[i] = Math.pow(ref05[0][i], 2) * ref05[5][i];
			path1[i] = Math.pow(ref1[0][i], 2) * ref1[5][i];
			// Verhoef and Bach GSUN, modtran DRCT RFLT (column 8)
			drct_reflt0[i] = Math.pow(ref0[0][i], 2) * ref0[8][i];
			drct_reflt05[i] = Math.pow(ref05[0][i], 2) * ref05[8][i];
			drct_reflt1[i] = Math.pow(ref1[0][i], 2) * ref1[8][i];
			// Verhoef and Bach GTOT, modtran GRND RFLT (column 7)
			gnd_reflt0[i] = Math.pow(ref0[0][i], 2) * ref0[7][i];
			gnd_reflt05[i] = Math.pow(ref05[0][i], 2) * ref05[7][i];
			gnd_reflt1[i] = Math.pow(ref1[0][i], 2) * ref1[7][i];
			// total transmittance gnd-sensor path, modtran TOT TRANS (column 1)
			tao[i] = ref0[1][i];
			// optical depth, modtran DEPTH (column 12)
			b[i] = ref0[12][i];
			// Verhoef, Wuhan 2008
			ref_sol1[i] = Math.pow(ref1[0][i], 2) * ref1[10][i];
			sol_at_obs1[i] = Math.pow(ref1[0][i], 2) * ref1[11][i];
		}
		
		// Variables now read into vectors.  Determine band specific values.
		double L_0 = Utils.convolve(srf, new double[][] {lambda_nm, rad0});
		System.out.println(String.format("\t at-sensor radiance, zero reflectance = %.4f", L_0));
		double L_05 = Utils.convolve(srf, new double[][] {lambda_nm, rad05});
		System.out.println(String.format("\t at-sensor radiance, 0,5 reflectance = %.4f", L_05));
		double L_1 = Utils.convolve(srf, new double[][] {lambda_nm, rad1});
		System.out.println(String.format("\t at-sensor radiance, unit reflectance = %.4f", L_1));
		
		double PATH_0 = Utils.convolve(srf, new double[][] {lambda_nm, path0});
		System.out.println(String.format("\t path radiance, zero reflectance = %.4f", PATH_0));
		double PATH_1 = Utils.convolve(srf, new double[][] {lambda_nm, path1});
		System.out.println(String.format("\t path radiance, unit reflectance = %.4f", PATH_1));
		
		double GTOT_05 = Utils.convolve(srf, new double[][] {lambda_nm, gnd_reflt05});
		System.out.println(String.format("\t total ground-reflected radiance, 0.5 reflectance = %.4f", GTOT_05));
		double GTOT_1 = Utils.convolve(srf, new double[][] {lambda_nm, gnd_reflt1});
		System.out.println(String.format("\t total ground-reflected radiance, unit reflectance = %.4f", GTOT_1));
		
		double GSUN_1 = Utils.convolve(srf, new double[][] {lambda_nm, drct_reflt1});
		System.out.println(String.format("\t radiance contribution due to ground-reflected sunlight = %.4f", GSUN_1));
		double b_modtran = Utils.convolve(srf, new double[][] {lambda_nm, b});
		System.out.println(String.format("\t optical depth = %.4f", b_modtran));
		
		double REFSOL_1 = Utils.convolve(srf, new double[][] {lambda_nm, ref_sol1});
		System.out.println(String.format("\t REF_SOL irradiance = %.4f", REFSOL_1));
		
		double SOLatOBS = Utils.convolve(srf, new double[][] {lambda_nm, sol_at_obs1});
		System.out.println(String.format("\t SOL@OBS irradiance = %.4f", SOLatOBS));
		
		double exoIrrad = Utils.convolve(srf, exo);
		System.out.println(String.format("\t exo-atmospheric irradiance = %.4f", exoIrrad));
		
		System.out.println("\t\t Derived values:");
		// direct transmittances
		double tao_oo = Utils.convolve(srf, new double[][] {lambda_nm, tao});
		System.out.println(String.format("\t\t tao_oo = %.4f", tao_oo));
		
		// Verhoef 2008 Wuhan present version (true ONLY if sensor is above the atmosphere)
		//double tao_ss = REFSOL_1 / (SOLatOBS * tao_oo);
		// True regardless
		double tao_ss = REFSOL_1 / (exoIrrad * tao_oo);
		System.out.println(String.format("\t\t tao_ss = %.4f", tao_ss));
		
		// Verhoef Bach 2003 version (ground sensor)
		double tao_beers = Math.exp(-b_modtran/Math.cos(sensor_zenith/180*Math.PI));
		System.out.println(String.format("\t\t tao_beers = %.4f", tao_beers));
        
        // spherical albedo, Verhoef and Bach (2003) equation 14
        double s = (GTOT_1 - 2.0*GTOT_05)/(GTOT_1 - GTOT_05);
        System.out.println(String.format("\t\t spherical albedo = %.4f", s));
        
        // diffuse transmittance from Verhoef and Bach (2003) equation 15
        double tao_sd = ( ( (GTOT_1 * (1.0-s) )/GSUN_1) - 1.0 ) * tao_ss;
        // don't let transmittance exceed 1
        tao_sd = (1.0-tao_ss < tao_sd) ? 1.0-tao_ss : tao_sd;
        System.out.println(String.format("\t\t tao_sd = %.4f", tao_sd));

        // diffuse transmittance from Verhoef and Bach (2003) equation 16
        double tao_do = ((PATH_1-PATH_0)/GTOT_1)*tao_oo;
        tao_do = (1.0-tao_oo < tao_do) ? 1.0-tao_oo : tao_do;
        System.out.println(String.format("\t\t tao_do = %.4f", tao_do));
        
        double tao_total = (tao_ss + tao_sd) * (tao_oo + tao_do);
        System.out.println(String.format("\t\t tao_total = %.4f", tao_total));
        System.out.println();
        
		return new double[] {PATH_0, s, tao_total};
	}
	                	
	
	/**
	 * Derivation of spherical albedo, total transmitance, and path radiance using the 
	 * method of Verhoef and Bach (2003).
	 * 
	 * @param tape7s is an array of filenames corresponding to the 0, 0.5, 1.0 reflectance runs
	 * @param srfs is a List of double[][] where each double[][] is an SRF
	 * @param exo is the Exo-atmospheric irradiance
	 * @param sensor_zenith is the sensor-zenith in degrees
	 * @return {path radiance, spherical albedo, total two way transmittance} for each band in an array
	 */
	public static double[][] vbMod4(String[] tape7s, List<double[][]> srfs, double[][] exo, double sensor_zenith) {
		// read the tape 7 files
		int run = 1;
		double[][] ref0 = MODTRANprocessor.readTape7(tape7s[0], run);
		System.out.println("Read zero reflectance tape7 file: "+tape7s[0]);
		double[][] ref05 = MODTRANprocessor.readTape7(tape7s[1], run);
		System.out.println("Read 0.5 reflectance tape7 file: "+tape7s[1]);
		double[][] ref1 = MODTRANprocessor.readTape7(tape7s[2], run);
		System.out.println("Read unit reflectance tape7 file: "+tape7s[2]);
		
		// iterate over each band
		int bands = srfs.size();
		double[][] unknowns = new double[bands][3];
		for (int b=0; b<bands; b++) {
			System.out.println("Processing band "+(b+1));
			double[][] srf = srfs.get(b);
			unknowns[b] = vbMod4(ref0, ref05, ref1, srf, exo, sensor_zenith);
		}
		return unknowns;
	}	 
	
	
	/**
	 * Derivation of spherical albedo, total transmitance, and path radiance using Modtran 5 outputs. 
	 * @param ref0 is the Modtran 5 zero reflectance tape 7 output
	 * @param acd is the Modtran 5 acd output
	 * @param srf is the SRF to process
	 * @return a double array {path radiance, spherical albedo, total two way transmittance}
	 */
	public static double[] acdMod5(double[][] ref0, double[][] acd, double[][] srf) {
		// vector of wavelength in nanometers
		double[] nm7 = new double[ref0[0].length];
		// simulated at sensor radiance, modtran TOTAL RAD (column 9), NOT USED
		double[] rad0 = new double[ref0[0].length];
		
		// path radiance from tape 7 file
		for (int i=0; i<ref0[0].length; i++) {
			// compute a vector of wavelength in nanometers
			nm7[i] = 1.0 / ref0[0][i] * Math.pow(10, 7);
			// Convert to watts/square meter/steradian/micron by multiplying by squared wavenumber
			// simulated at sensor radiance, modtran TOTAL RAD (column 9)
			rad0[i] = Math.pow(ref0[0][i], 2) * ref0[9][i];
		}
		// variables now read into vectors.  Convolve to determine band specific values.
		double L_0 = Utils.convolve(srf, new double[][] {nm7, rad0});
		System.out.println(String.format("\t at-sensor radiance, zero reflectance = %.4f", L_0));
		
		// initializations for ACD variables:
		double[] nmACD = new double[acd[0].length];
		double[] dirSun = new double[acd[0].length];
		double[] difSun = new double[acd[0].length];
		double[] dirSens = new double[acd[0].length];
		double[] difSens = new double[acd[0].length];
		double[] sphAlb = new double[acd[0].length];
		// transmittance and spherical albedo from ACD file
		for (int i=0; i<acd[0].length; i++) {
			// compute a vector of wavelength in nanometers
			nmACD[i] = 1.0 / acd[0][i] * Math.pow(10, 7);
			dirSun[i] = acd[4][i];
			difSun[i] = acd[3][i];
			dirSens[i] = acd[6][i];
			difSens[i] = acd[5][i];
			sphAlb[i] = acd[7][i];
		}
		
		double tao_oo = Utils.convolve(srf, new double[][] {nmACD, dirSens});
		System.out.println(String.format("\t tao_oo = %.4f", tao_oo));

		double tao_ss = Utils.convolve(srf, new double[][] {nmACD, dirSun});
		System.out.println(String.format("\t tao_ss = %.4f", tao_ss));
        
        double s = Utils.convolve(srf, new double[][] {nmACD, sphAlb});
        System.out.println(String.format("\t spherical albedo = %.4f", s));
        
        double tao_sd = Utils.convolve(srf, new double[][] {nmACD, difSun});
        System.out.println(String.format("\t tao_sd = %.4f", tao_sd));

        double tao_do = Utils.convolve(srf, new double[][] {nmACD, difSens});
        System.out.println(String.format("\t tao_do = %.4f", tao_do));
        
        double tao_sun_gnd = tao_ss + tao_sd;
        System.out.println(String.format("\t tao_sun_gnd = %.4f", tao_sun_gnd));
        
        double tao_gnd_sens = tao_oo + tao_do;
        System.out.println(String.format("\t tao_gnd_sens = %.4f", tao_gnd_sens));
        
        double tao_total = tao_sun_gnd * tao_gnd_sens;
        System.out.println(String.format("\t tao_total = %.4f", tao_total));
        
		//return new double[] {L_0, s, tao_sun_gnd, tao_gnd_sens};
        return new double[] {L_0, s, tao_total};
	}
	
	/**
	 * Derivation of spherical albedo, total transmitance, and path radiance using Modtran 5 outputs. 
	 * @param acdFile is a Modtran5 output atmospheric correction file
	 * @param tape7 is a Modtran5 output tape7 file corresponding to a zero reflectance target
	 * @param srfs is a List of double[][] where each double[][] is an SRF 
	 * @return a double array {path radiance, spherical albedo, total two way transmittance}
	 */
	public static double[][] acdMod5(String acdFile, String tape7, List<double[][]> srfs) {
		// read the tape 7 and ACD files
		int run = 1;
		double[][] ref0 = MODTRANprocessor.readTape7mod5(tape7, 1);
		System.out.println("Read zero reflectance tape7 file: "+tape7);
		double[][] acd = readACDmod5(acdFile);
		System.out.println("Read Modtran5 acd file: "+acdFile);
		
		// iterate over each band
		int bands = srfs.size();
		double[][] unknowns = new double[bands][3];
		for (int b=0; b<bands; b++) {
			System.out.println("Processing band "+(b+1));
			double[][] srf = srfs.get(b);
			unknowns[b] = acdMod5(ref0, acd, srf);
		}
		return unknowns;
	}
	
	
	/**
	 * Convert a single observation from radiance to reflectance.
	 * @param radiance in units of W / m^2 / micrometers / sr
	 * @param pathRad in units of W / m^2 / micrometers / sr
	 * @param trans is total two-way transmittance, unitless, in [0,1]
	 * @param sphAlb is atmospheric albedo, unitless, in [0,1]
	 * @param irrad is exo-atmospheric irradiance in W / m^2 / micrometers
	 * @param d_squared is Earth-Sun distance in astronomical units
	 * @param cosTheta is the cos of the solar zenith angle
	 * @return reflectance, unitless, in [0,1]
	 */
	public static float rad2ref(double radiance, double pathRad, double trans, double sphAlb, double exo, double d_squared, double cosTheta){
		radiance = (radiance < 0) ? Float.MIN_NORMAL : radiance;
		// this is the important conversion:
		double reflectance = ( radiance-pathRad ) /
		( ((radiance-pathRad)*sphAlb) + (trans*exo*cosTheta/Math.PI/d_squared) );
		// truncate
		if (reflectance > 1.0) { return 1.0f; }
		else if (reflectance < 0) { return Float.MIN_NORMAL; }
		else { return (float) reflectance; }
	}
	
	
	/**
	 * Convert an image from radiance to reflectance, using the supplied parameters.
	 * Band i in the input is processed using exoIrrad[i] and unknowns[i].
	 * 
	 * @param input is the full pathname of the input image in W/m^2/micron/sr
	 * @param output is the full pathname of the output image
	 * @param exoIrrad is an array of in-band exo-Atmospheric irradiances
	 * @param unknowns is an array of in-band {path radiance, spherical albedo, two way transmittance}
	 * @param acqYear is the image year
	 * @param acqMonth is the image month
	 * @param acqDay is the image day
	 * @param solar_zenith is in degrees 
	 */
	public static void rad2ref(String input, 
							   String output, 
							   double[] exoIrrad, 
							   double[][] unknowns,
							   int acqYear,
							   int acqMonth,
							   int acqDay,
							   double solar_zenith) {
		rad2ref(input, output, exoIrrad, unknowns, acqYear, acqMonth, acqDay, solar_zenith, 1.0);
	}
	
	/**
	 * Convert an image from radiance to reflectance, using the supplied parameters.
	 * Band i in the input is processed using exoIrrad[i] and unknowns[i].
	 * 
	* @param input is the full pathname of the input image
	 * @param output is the full pathname of the output image
	 * @param exoIrrad is an array of in-band exo-Atmospheric irradiances
	 * @param unknowns is an array of in-band {path radiance, spherical albedo, two way transmittance}
	 * @param acqYear is the image year
	 * @param acqMonth is the image month
	 * @param acqDay is the image day
	 * @param solar_zenith is in degrees 
	 * @param scale is a scale multiplier to make the input units W / m^2 / micrometers / sr
	 */
	public static void rad2ref(String input, 
			   String output, 
			   double[] exoIrrad, 
			   double[][] unknowns,
			   int acqYear,
			   int acqMonth,
			   int acqDay,
			   double solar_zenith,
			   double scale) {
		// write to the log
		System.out.println("rad2ref input: "+input);
		Date start = Calendar.getInstance().getTime();
		
		double cosTheta = Math.cos(solar_zenith/180.0*Math.PI);
		float d_squared = (float) Math.pow(Utils.esDistance(acqYear,acqMonth,acqDay), 2.0);
		
		// input
		final File inFile = new File(input); 
		ImageReader reader;
		try {
			// Read the input
			//reader = new ENVIHdrImageReaderSpi().createReaderInstance();
			reader = ImageIO.getImageReaders(inFile).next();
			final ParameterBlockJAI pbjImageRead;
			pbjImageRead = new ParameterBlockJAI("ImageRead");
			pbjImageRead.setParameter("Input", inFile);
			pbjImageRead.setParameter("reader", reader);
			PlanarImage image = JAI.create("ImageRead", pbjImageRead);
			RandomIter inputIter = RandomIterFactory.create(image, null);
			// set up the output
			RandomAccessFile file = new RandomAccessFile(output, "rw");
			FileImageOutputStream stream = new FileImageOutputStream(file);
			stream.setByteOrder(ByteOrder.nativeOrder());
			
			for (int y=0; y<image.getHeight(); y++) { // each line
				System.out.println("Processing line: "+y);
				for (int b=0; b<image.getNumBands(); b++) { // each band
					for (int x=0; x<image.getWidth(); x++){	// each pixel
						// rad2ref(radiance, pathRad, trans, sphAlb, exo, d_squared, cosTheta)
						float reflectance = rad2ref(inputIter.getSampleDouble(x,y,b)*scale, 
													 unknowns[b][0], 
													 unknowns[b][2], 
													 unknowns[b][1], 
													 exoIrrad[b], 
													 d_squared, 
													 cosTheta);
						//System.out.println("Processing (line, band, pixel):reflectance - "+String.format("(%d,%d,%d):%.4f", y,b,x,reflectance));
						stream.writeFloat(reflectance);
					}
				}
				stream.flush();
			}
			stream.close();
			file.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		// write finished
		System.out.println("rad2ref output: "+output);
		long elapsed = Calendar.getInstance().getTimeInMillis()-start.getTime();
		System.out.println(String.format("Processing time: %.1f seconds", elapsed/1000.0));
		Utils.time();
	}
	
	/**
	 * Derivation of path radiance and ground-sensor transmittance for the thermal case.
	 * Reference http://dragon2.esa.int/landtraining2008/pdf/D2L5a_Verhoef.pdf
	 * 
	 * @param tape7s is an array of filenames for the 0, 0.5 and 1.0 reflectance runs
	 * @param srfs is a list of SRFs to process (assumed thermal, not checked)
	 * @return a double[][] {path radiance, total two way transmittance, Lg}
	 */
	public static double[][] thermal(String[] tape7s, List<double[][]> srfs) {
		// read the tape 7 files
		int run = 1;
		double[][] ref0 = MODTRANprocessor.readTape7(tape7s[0], run);
		System.out.println("Read zero reflectance tape7 file: "+tape7s[0]);
		double[][] ref05 = MODTRANprocessor.readTape7(tape7s[1], run);
		System.out.println("Read 0.5 reflectance tape7 file: "+tape7s[1]);
		double[][] ref1 = MODTRANprocessor.readTape7(tape7s[2], run);
		System.out.println("Read unit reflectance tape7 file: "+tape7s[2]);
		// iterate over each band
		int bands = srfs.size();
		double[][] unknowns = new double[bands][3];
		for (int b=0; b<bands; b++) {
			System.out.println("Processing band "+(b+1));
			double[][] srf = srfs.get(b);
			unknowns[b] = thermal(ref0, ref05, ref1, srf);
		}
		return unknowns;
	}
	
	/**
	 * Derivation of path radiance and ground-sensor transmittance for the thermal case.
	 * Reference http://dragon2.esa.int/landtraining2008/pdf/D2L5a_Verhoef.pdf
	 * 
	 * @param ref0 is the output of a modtran zero reflectance run
	 * @param ref05 is the output of a modtran 0.5 reflectance run
	 * @param ref1 is the output of a modtran 1.0 reflectance run
	 * @param srf is the SRF of the band to process
	 * @return a double array {path radiance, total two way transmittance, Lg}
	 */
	public static double[] thermal(double[][] ref0, double[][] ref05, double[][] ref1, double[][] srf) {
		// initializations:
		
		// vector of wavelength in nanometers
		double[] lambda_nm = new double[ref0[0].length];
		// simulated at sensor radiance, modtran TOTAL RAD (column 9)
		double[] rad0 = new double[ref0[0].length];
		double[] rad1 = new double[ref0[0].length];
		// Verhoef PTEM, modtran PTH THRML (column2)
		double[] ptem0 = new double[ref0[2].length];
		double[] ptem1 = new double[ref0[2].length];
		// Verhoef SFEM, modtran SURF EMIS (column 4)
		double[] sfem0 = new double[ref0[4].length];
		double[] sfem05 = new double[ref0[4].length];
		double[] sfem1 = new double[ref0[4].length];
		// Verhoef PATH, modtran SOL SCAT (column 5)
		double[] path0 = new double[ref0[0].length];
		double[] path1 = new double[ref0[0].length];
		// Verhoef and Bach GRFL (previously GTOT), modtran GRND RFLT (column 7)
		double[] gnd_reflt0 = new double[ref0[0].length];
		double[] gnd_reflt05 = new double[ref0[0].length];
		double[] gnd_reflt1 = new double[ref0[0].length];
		// Verhoef and Bach GSUN, modtran DRCT RFLT (column 8)
		double[] drct_reflt05 = new double[ref0[0].length];
		// total transmittance gnd-sensor path, modtran TOT TRANS (column 1)
		double[] tao = new double[ref0[0].length];
		// optical depth, modtran DEPTH (column 12)
		double[] b = new double[ref0[0].length];
		// Verhoef, Wuhan 2008
		double[] ref_sol1 = new double[ref0[0].length];
		
		// Convert to watts/square meter/steradian/micron by multiplying by squared wavenumber
		for (int i=0; i<ref0[0].length; i++) {
			
			// compute a vector of wavelength in nanometers
			lambda_nm[i] = 1.0 / ref0[0][i] * Math.pow(10, 7);
			// simulated at sensor radiance, modtran TOTAL RAD (column 9)
			rad0[i] = Math.pow(ref0[0][i], 2) * ref0[9][i];
			rad1[i] = Math.pow(ref1[0][i], 2) * ref1[9][i];
			// Verhoef and Bach PATH, modtran SOL SCAT (column 5)
			path0[i] = Math.pow(ref0[0][i], 2) * ref0[5][i];
			path1[i] = Math.pow(ref1[0][i], 2) * ref1[5][i];
			// Verhoef and Bach GRFL, modtran GRND RFLT (column 7)
			gnd_reflt0[i] = Math.pow(ref0[0][i], 2) * ref0[7][i];
			gnd_reflt05[i] = Math.pow(ref05[0][i], 2) * ref05[7][i];
			gnd_reflt1[i] = Math.pow(ref1[0][i], 2) * ref1[7][i];
			// Verhoef PTEM, modtran PTH THRML (column 2)
			ptem0[i] = Math.pow(ref0[0][i], 2) * ref0[2][i];
			ptem1[i] = Math.pow(ref1[0][i], 2) * ref1[2][i];
			// Verhoef SFEM, modtran SURF EMIS (column 4)
			sfem0[i] = Math.pow(ref0[0][i], 2) * ref0[4][i];
			sfem05[i] = Math.pow(ref05[0][i], 2) * ref05[4][i];
			sfem1[i] = Math.pow(ref1[0][i], 2) * ref1[4][i];
			// modtran DRCT RFLT (column 8)
			drct_reflt05[i] = Math.pow(ref05[0][i], 2) * ref05[8][i];
			// total transmittance gnd-sensor path, modtran TOT TRANS (column 1)
			tao[i] = ref0[1][i];
			// optical depth, modtran DEPTH (column 12)
			b[i] = ref0[12][i];
			// Verhoef, Wuhan 2008
			ref_sol1[i] = Math.pow(ref1[0][i], 2) * ref1[10][i];
		}
		
		double b_modtran = Utils.convolve(srf, new double[][] {lambda_nm, b});
		System.out.println(String.format("\t Convolved optical depth = %.4f", b_modtran));

		double L_0 = Utils.convolve(srf, new double[][] {lambda_nm, rad0});
		System.out.println(String.format("\t Simulated at-sensor radiance, zero reflectance = %.4f",L_0));
		double L_1 = Utils.convolve(srf, new double[][] {lambda_nm, rad1});
		System.out.println(String.format("\t Simulated at-sensor radiance, unit reflectance = %.4f",L_1));
		
		double PATH_0 = Utils.convolve(srf, new double[][] {lambda_nm, path0});
		System.out.println(String.format("\t PATH_0 = %.4f",PATH_0));
		double PATH_1 = Utils.convolve(srf, new double[][] {lambda_nm, path1});
		System.out.println(String.format("\t PATH_1 = %.4f",PATH_1));
		
		double GRFL_0 = Utils.convolve(srf, new double[][] {lambda_nm, gnd_reflt0});
		System.out.println(String.format("\t GRFL_0 = %.4f",GRFL_0));
		double GRFL_05 = Utils.convolve(srf, new double[][] {lambda_nm, gnd_reflt05});
		System.out.println(String.format("\t GRFL_05 = %.4f",GRFL_05));
		double GRFL_1 = Utils.convolve(srf, new double[][] {lambda_nm, gnd_reflt1});
		System.out.println(String.format("\t GRFL_1 = %.4f",GRFL_1));
		
		double PTEM_0 = Utils.convolve(srf, new double[][] {lambda_nm, ptem0});
		System.out.println(String.format("\t PTEM_0 = %.4f",PTEM_0));
		double PTEM_1 = Utils.convolve(srf, new double[][] {lambda_nm, ptem1});
		System.out.println(String.format("\t PTEM_1 = %.4f",PTEM_1));
		
		double SFEM_0 = Utils.convolve(srf, new double[][] {lambda_nm, sfem0});
		System.out.println(String.format("\t SFEM_0 = %.4f",SFEM_0));
		double SFEM_05 = Utils.convolve(srf, new double[][] {lambda_nm, sfem05});
		System.out.println(String.format("\t SFEM_05 = %.4f",SFEM_05));
		double SFEM_1 = Utils.convolve(srf, new double[][] {lambda_nm, sfem1});
		System.out.println(String.format("\t SFEM_1 = %.4f",SFEM_1));
		
		double GSUN_05 = Utils.convolve(srf, new double[][] {lambda_nm, drct_reflt05});
		System.out.println(String.format("\t GSUN_05 = %.4f",GSUN_05));
		
		double REFSOL_1 = Utils.convolve(srf, new double[][] {lambda_nm, ref_sol1});
		System.out.println(String.format("\t Convolved ref_sol = %.4f",REFSOL_1));
		
		// direct transmittances
		double tao_oo = Utils.convolve(srf, new double[][] {lambda_nm, tao});
		System.out.println(String.format("\t tao_oo = %.4f",tao_oo));
		
		// computed from Verhoef Wuhan presentation formulas
		double tao_do  = (PATH_1 - PATH_0 + PTEM_1 - PTEM_0) / (GRFL_1 - SFEM_0) * tao_oo;
		tao_do = (tao_do < 0) ? 0.0 : tao_do;
		System.out.println(String.format("\t tao_do = %.4f",tao_do));
		
		// reflection of 3 downward flux components: direct solar, diffuse solar and diffuse thermal.
		double L_g = GRFL_1 / (tao_oo + tao_do);
		// Borel style when Modtran executed in thermal radiance mode
		//double L_g = L_1 - PTEM_1;
		System.out.println(String.format("\t L_g = %.4f",L_g));
		
        // path radiance
        double L_zero = PTEM_1 + PATH_0;
        System.out.println(String.format("\t L_zero = %.4f",L_zero));
       
		return new double[] {L_zero, tao_oo+tao_do, L_g};
        //return new double[] {L_zero, tao_oo, L_g};
	}
	
	
	/**
	 * Derivation of path radiance and ground-sensor transmittance for the thermal case.
	 * Uses Modtran 5 output.
	 * 
	 * @param acdFile is a Modtran5 acd file for a zero reflectance target.
	 * @param tape7 is a Modtran5 tape7 file from the zero reflectance run
	 * @param srfs are the SRFs to process (should be thermal, but not checked)
	 * @return a double array {path radiance, total two way transmittance, Lg}
	 */
	public static double[][] thermal5(String acdFile, String tape7, List<double[][]> srfs) {
		
		double[][] ref0 = MODTRANprocessor.readTape7mod5(tape7, 1);
		System.out.println("Read zero reflectance tape7 file: "+tape7);
		double[][] acd = readACDmod5(acdFile);
		System.out.println("Read Modtran5 acd file: "+acdFile);
		// iterate over each band
		int bands = srfs.size();
		double[][] unknowns = new double[bands][3];
		for (int b=0; b<bands; b++) {
			System.out.println("Processing band "+(b+1));
			double[][] srf = srfs.get(b);
			unknowns[b] = thermal5(ref0, acd, srf);
		}
		return unknowns;
	}
	
	
	/**
	 * 
	 * Derivation of path radiance and ground-sensor transmittance for the thermal case.
	 * Uses Modtran 5 output.
	 * 
	 * @param acd is Modtran5 acd output for a zero reflectance target.
	 * @param ref0 is Modtran5 tape7 output of the zero reflectance run
	 * @param srf is the SRFs to process (should be thermal, but not checked)
	 * @return a double array {path radiance, total two way transmittance, Lg}
	 */
	public static double[] thermal5(double[][] ref0, double[][] acd, double[][] srf) {
		
		// vector of wavelength in nanometers
		double[] nm7 = new double[ref0[0].length];
		// simulated path radiance, reflected
		double[] sol_scat = new double[ref0[0].length];
		// simulated path radiance, thermal
		double[] path_thm = new double[ref0[0].length];
		// TOA irradiance
		double[] toa_sun = new double[ref0[0].length];
		
		// read from tape 7 file
		for (int i=0; i<ref0[0].length; i++) {
			// compute a vector of wavelength in nanometers
			nm7[i] = 1.0 / ref0[0][i] * Math.pow(10, 7);
			// Convert to watts/square meter/steradian/micron by multiplying by squared wavenumber
			// simulated path radiance, modtran SOL_SCAT (column 5)
			sol_scat[i] = Math.pow(ref0[0][i], 2) * ref0[5][i];
			// simulated thermal path radiance, modtran PATH_THERM (column 2)
			path_thm[i] = Math.pow(ref0[0][i], 2) * ref0[2][i];
			// TOA_SUN irradiance
			toa_sun[i] = Math.pow(ref0[0][i], 2) * ref0[14][i];
		}
		
		// variables now read into vectors.  Convolve to determine band specific values.
		double L_0 = Utils.convolve(srf, new double[][] {nm7, sol_scat});
		System.out.println(String.format("\t zero reflectance solar scattered radiance = %.4f", L_0));
		double L_thm = Utils.convolve(srf, new double[][] {nm7, path_thm});
		System.out.println(String.format("\t zero emissivity thermal path radiance = %.4f", L_thm));
		// in-band exo-atmospheric irradiance
		double E_0 = Utils.convolve(srf, new double[][] {nm7, toa_sun});
		System.out.println(String.format("\t in-band exo-atmospheric irradiance = %.4f", E_0));
		
		// initializations for ACD variables:
		double[] nmACD = new double[acd[0].length];
		double[] dirSun = new double[acd[0].length];
		double[] difSun = new double[acd[0].length];
		double[] dirSens = new double[acd[0].length];
		double[] difSens = new double[acd[0].length];
		double[] sphAlb = new double[acd[0].length];
		// transmittance and spherical albedo from ACD file
		for (int i=0; i<acd[0].length; i++) {
			// compute a vector of wavelength in nanometers
			nmACD[i] = 1.0 / acd[0][i] * Math.pow(10, 7);
			dirSun[i] = acd[4][i];
			difSun[i] = acd[3][i];
			dirSens[i] = acd[6][i];
			difSens[i] = acd[5][i];
			sphAlb[i] = acd[7][i];
		}
		
		double tao_oo = Utils.convolve(srf, new double[][] {nmACD, dirSens});
		System.out.println(String.format("\t tao_oo = %.4f", tao_oo));

		double tao_ss = Utils.convolve(srf, new double[][] {nmACD, dirSun});
		System.out.println(String.format("\t tao_ss = %.4f", tao_ss));
        
        double s = Utils.convolve(srf, new double[][] {nmACD, sphAlb});
        System.out.println(String.format("\t spherical albedo = %.4f", s));
        
        double tao_sd = Utils.convolve(srf, new double[][] {nmACD, difSun});
        System.out.println(String.format("\t tao_sd = %.4f", tao_sd));

        double tao_do = Utils.convolve(srf, new double[][] {nmACD, difSens});
        System.out.println(String.format("\t tao_do = %.4f", tao_do));
        
        double tao_sun_gnd = tao_ss + tao_sd;
        System.out.println(String.format("\t tao_sun_gnd = %.4f", tao_sun_gnd));
        
        double tao_gnd_sens = tao_oo + tao_do;
        System.out.println(String.format("\t tao_gnd_sens = %.4f", tao_gnd_sens));
        
        double tao_total = tao_sun_gnd * tao_gnd_sens;
        System.out.println(String.format("\t tao_total = %.4f", tao_total));
		
        double L_g = E_0 * tao_sd*tao_ss;
        System.out.println(String.format("\t\t downwelling radiance at ground (L_g) = ", L_g));
        
		double L_zero = L_0 + L_thm;
		System.out.println(String.format("\t\t total path radiance (L_zero) = ", L_zero));
       
		return new double[] {L_zero, tao_oo+tao_do, L_g};
	}
	
	/**
	 * Inverse Planck function.
	 * @param radiance in units of Watts/M^2/sr/micron
	 * @param l_gnd is downwelling radiance at the target in Watts/M^2/sr/micron
	 * @param tao is target to sensor transmittance, unitless
	 * @param path is the thermal path radiance, in Watts/M^2/sr/micron
	 * @param lambda is the (center) wavelength, nm
	 * @param e is the in-band emissivity of the target, unitless
	 * @return the temp in K
	 */
	public static float rad2temp(double radiance, double l_gnd, double tao, double path, double lambda, double e) {
		// truncate
		radiance = (radiance < 0) ? Float.MIN_NORMAL : radiance;
		// at ground converted to W/sq.m./sr./M
		double L_up = (( radiance - path ) / tao) * Math.pow(10, 6.0);
		// truncate
		L_up = (L_up < 0) ? Float.MIN_NORMAL : L_up;
		// blackbody = upwelling minus reflected component, divided by emissivity
		double rho = 1.0-e;
		double L_bb = ( L_up - (rho * l_gnd) ) / e;
		// truncate
		L_bb = (L_bb < 0) ? Float.MIN_NORMAL : L_bb;
		// convert to radiance in Watts/M^2/sr/meter
		L_bb = L_bb*Math.pow(10,6);
		// convert to meters
		lambda = lambda*Math.pow(10, -9);
		
		return (float)(h*c / ( k*lambda * Math.log( ((2.0*h*Math.pow(c, 2)) / (L_bb*Math.pow(lambda, 5))) + 1.0 ) ));
		
	}
	
	/**
	 * Convert an input radiance (units Watts/M^2/sr/micron) image to temperature.
	 * Band i of the input image will be processed with unknowns[i], wavelengths[i], emissivity[i]
	 * 
	 * @param input is the full path of the input image 
	 * @param output is full path name of the output image
	 * @param unknowns is the array of in-band thermal unknowns.
	 * @param wavelengths is the array of band wavelengths
	 * @param emissivity is the array of in-band emissivities of the target
	 */
	public static void rad2temp(String input, 
							   String output, 
							   double[][] unknowns,
							   double[] wavelengths,
							   double[] emissivity) {
		// write to the log
		System.out.println("rad2temp input: "+input);
		Utils.time();
		Date start = Calendar.getInstance().getTime();
		
		// input
		final File inFile = new File(input); 
		ImageReader reader;
		try {
			// Read the input
			//reader = new ENVIHdrImageReaderSpi().createReaderInstance();
			reader = ImageIO.getImageReaders(inFile).next();
			final ParameterBlockJAI pbjImageRead;
			pbjImageRead = new ParameterBlockJAI("ImageRead");
			pbjImageRead.setParameter("Input", inFile);
			pbjImageRead.setParameter("reader", reader);
			PlanarImage image = JAI.create("ImageRead", pbjImageRead);
			RandomIter inputIter = RandomIterFactory.create(image, null);
			// set up the output
			RandomAccessFile file = new RandomAccessFile(output, "rw");
			FileImageOutputStream stream = new FileImageOutputStream(file);
			stream.setByteOrder(ByteOrder.nativeOrder());
			
			// iterate over input radiance image
			for (int y=0; y<image.getHeight(); y++) { // each line
				System.out.println("Processing line: "+y);
				for (int b=0; b<image.getNumBands(); b++) { // each band
					for (int x=0; x<image.getWidth(); x++){	// each pixel
						// unknowns: {L_zero, tao_oo+tao_do, L_g}
						// rad2temp(radiance, l_gnd, tao, path, lambda, e, rho)
						double radiance = inputIter.getSampleDouble(x,y,b);
						float temp = rad2temp(radiance, 
								               unknowns[b][2], // L_g
								               unknowns[b][1], // tao
								               unknowns[b][0], // L_zero
								               wavelengths[b], 
								               emissivity[b]);
						//System.out.println("Processing (line, band, pixel):temp, radiance - "+String.format("(%d,%d,%d):%.4f, %.4f", y,b,x,temp,radiance));
						stream.writeFloat(temp);
					}
				}
				stream.flush();
			}
			stream.close();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// write finished
		System.out.println("rad2temp output: "+output);
		long elapsed = Calendar.getInstance().getTimeInMillis()-start.getTime();
		System.out.println(String.format("Processing time: %.1f seconds", elapsed/1000.0));
		Utils.time();
	}
	
	
	/**
	 * Converts at-sensor radiance in units of W / m^2 / sr / micrometers
	 * to ground upwelling radiance in units of W / m^2 / sr / micrometers.  
	 * Uses output of dataprocessing.MODTRANprocessor.thermal()
	 * 
	 * This version is old.  TODO: Update.
	 * 
	 * @param image is the input radiance image in W/SQ.M/ST./MICRON
	 * @param srf has been read from dataprocessing.SRFProcessr.readSRF()
	 * @param unknowns has been read from dataprocessing.MODTRANprocessor.thermal()
	 * @param micron is the band center wavelength in micrometers
	 * @param exo has been read from dataprocessing.MODTRANprocessor.solarIrrad()
	 * @param sensor_zenith is the solar-zenith angle in radians
	 */
	public static void rad2rad(String image, double[][] srf, double[] unknowns, double[][] exo) {
		// path radiance
		double L_0 = unknowns[0];
		// transmittance
		double tao_oo = unknowns[1];
		//double tao_ss = unknowns[2];
		// this is for the output image
		Point origin = new Point(0,0);
		int numBands = 1;
		double L_s; // at sensor radiance
		double L_up; // upwelling ground radiance
		
		try {
			// load the image
			PlanarImage pImage = JAIUtils.readImage(image);
        	System.out.println("Radiance Image:");
        	JAIUtils.imageStats(pImage);
        	
        	// make the output image
        	int width = pImage.getWidth();
			int height = pImage.getHeight();
			WritableRaster rOut = RasterFactory.createBandedRaster(
	    											DataBuffer.TYPE_FLOAT,
	    											width,
	    											height,
	    											numBands,
	    											origin);
			// iterate over the input, set the output raster value
			RandomIter iterator = RandomIterFactory.create(pImage, null);
			for (int y=0; y<height; y++) {  	// each line
				for (int x=0; x<width; x++){	// each pixel
					// at sensor
					L_s = iterator.getSampleDouble(x,y,0);
					// at ground
					L_up = ( L_s - L_0 ) / tao_oo;
					// truncate
					L_up = (L_up < 0) ? Double.MIN_NORMAL : L_up;
					rOut.setSample(x,y,0,(float) L_up);
				}
			}
			
			// write
			String tFileName = image.replace(".tif", "_rad.tif");
			System.out.println("Creating file "+tFileName);
			JAIUtils.writeTiff(rOut, tFileName);
			PlanarImage outImage = JAIUtils.readImage(tFileName);
			System.out.println();
        	System.out.println("Temp Image:");
        	JAIUtils.imageStats(outImage);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	} 
	
	
	/**
	 * Reads Solar Irradiance from Modtran newkur.dat.
	 * @param file is tha absolute path of the solar irradiance text file
	 * @return a double[][] {wavlength in nm, irradiance (W / sq M / micron)}
	 */
	public static double[][] mIrrad(String file) {
		// initialize output
		double[][] output = null;
		// read the files written by cal_srf
		BufferedReader reader = null;
		File bFile = new File(file);
		try {
			reader = new BufferedReader(new FileReader(bFile));
			// skip the header
			for (int i=1; i<=2; i++) {
				reader.readLine();
			}
			// read the numeric data to lists
			ArrayList wv = new ArrayList();
			ArrayList rad = new ArrayList();
			String line = null;
			while ((line = reader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);
				wv.add(st.nextToken());
				rad.add(st.nextToken());
			}
			// check
			if (wv.size() != rad.size()) { System.err.println("Whoops!"); }
			// arraylists full of strings, read to arrays
			output = new double [2][wv.size()];
			for (int i=0; i<wv.size(); i++) {
				double wavenum = Double.parseDouble((String)wv.get(i));
				// convert to nm
				output[0][i] = 1.0 / wavenum * Math.pow(10, 7);
				// convert W CM-2 / CM-1 -> W / sq M / micron
				output[1][i] = Double.parseDouble((String)rad.get(i)) * Math.pow(wavenum, 2);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;	
		
	}
	
	
	/**
	 * Test code and processing log.
	 * @param args
	 */
	public static void main(String[] args) {
		
		// 20110310: See MODTRANprocessing.main()

		// 20110620 testing
		
		// srfs:
//		String srfRoot = "H:/SARP2010/srfs/msr040610a.c";
//		// srf parameters
//		int band1 = 1;
//		int bandN = 25;
//		int srfHeader = 12;
//		boolean microns = true;
//		
//		// Modtran 4 output:
//		String r0tp7 = "C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1000403_line1_OAK_sonde_r0.tp7";
//		String r05tp7 = "C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1000403_line1_OAK_sonde_r05.tp7";
//		String r10tp7 = "C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1000403_line1_OAK_sonde_r10.tp7";
//		String[] tape7s = {r0tp7, r05tp7, r10tp7};
//		// Modtran 5 output
//		String mod5r0 = "C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11652_Ivanpah_us76_r0.tp7";
//		String acdFile = "C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11652_Ivanpah_us76_r0.acd";
//		
//		// Exo-atmospheric irradiance:
//		//String exoFile = "C:/Users/owner/Documents/MODTRAN/SolarIrradiance.txt";
//		String exoFile = "C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat";
//		double[][] exo = mIrrad(exoFile);
//		
//		// imagery:
//		String input = "H:/10-004-03/Central_Valley/MASTERL1B_1000403_01_20100629_1846_1854_V02_b1_25";
//		String output = "H:/10-004-03/Central_Valley/MASTERL1B_1000403_01_20100629_1846_1854_V02_b1_25_ref_kurucz.tif";
//		// image parameters
//		double sensor_zenith = 0.0; // degrees
//		double sun_zenith = 19.8; // degrees
//		int year = 2010;
//		int month = 6;
//		int day = 30;
//		
//		// tee'd output
//		String processLog = "C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1000403_line1_OAK_sonde_processing_kurucz.txt";
//		
//		// go:
//		try {
//			PrintStream console = System.out;
//			OutputStream fileStream = new FileOutputStream(processLog,true);
//			System.setOut(new PrintStream(new TeeOutputStream(System.out, fileStream)));
//			// first 25 bands SRFs
//			List<double[][]> srfs = SRFUtils.readSRFs(srfRoot, band1, bandN, srfHeader, microns);
//			// generate unknowns Modtran 4
//			double[][] unknowns4 = vbMod4(tape7s, srfs, exo, sensor_zenith);
//			double[][] unknowns5 = acdMod5(acdFile, mod5r0, srfs);
//			
//			// in-band exo-atmospheric irradiance
//			double[] bExo = SRFUtils.getInBand(srfs, exo);
//			
//			System.setOut(console);
//			// process mod4
//			rad2ref(input, output, bExo, unknowns4, year, month, day, sun_zenith);
//			// process mod5
//			//rad2ref(input, output, bExo, unknowns5, year, month, day, sun_zenith);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}	
		
//		String srfRoot = "C:/Users/owner/Documents/ASTL/MASTER_srfs_June2011/msr0611a.c";
//		String r0tp7 = "C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11652_Ivanpah_us76_r0.tp7";
//		String r05tp7 = "C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11652_Ivanpah_us76_r05.tp7";
//		String r10tp7 = "C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11652_Ivanpah_us76_r10.tp7";
		
		// 20131022
		// Landsat 8 OLI processing for class demo
//		String newkur = "/Volumes/LaCieX/Documents/Modtran/newkur.dat";
//		String r0tp7 = "/Volumes/LaCieX/Documents/Modtran/Dunhuang_00_az_zen.tp7";
//		String r05tp7 = "/Volumes/LaCieX/Documents/Modtran/Dunhuang_05_az_zen.tp7";
//		String r10tp7 = "/Volumes/LaCieX/Documents/Modtran/Dunhuang_10_az_zen.tp7";
//		
//		double[][] r00 = readTape7mod5(r0tp7, 1);
//		double[][] r05 = readTape7mod5(r05tp7, 1);
//		double[][] r10 = readTape7mod5(r10tp7, 1);
//		double[][] exo = mIrrad(newkur);
//		
//		double sensor_zenith = 0.0;
				
//		List<double[][]> srfs = 
//			SRFUtils.readSRFs(
//				"/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/l8srf",
//				1, 8, 1, false
//			);
//		
//		int band = 1;
//		for (double[][] srf : srfs) {
//			double[] vbmod5 = vbMod4(r00, r05, r10, srf, exo, sensor_zenith);
//			System.out.println("Band "+band+" : "+Arrays.toString(vbmod5));
//			band++;
//		}
		
		// 20131022 thermal processing for tirs band 11: 11.5-12.5 microns
		// simulated Gaussian spectral response function, zero mean
//		double[][] srf11 = (new GaussFunction()).getGauss(1000);
//		// shift into position
//		for (int l=0; l<srf11[0].length; l++) {
//			srf11[0][l] = srf11[0][l] + 12000.0;
//			//System.out.println(srf11[0][l]+","+srf11[1][l]);
//		}
//		double[] vbmod5 = thermal(r00, r05, r10, srf11);
//		System.out.println("Band 11 : "+Arrays.toString(vbmod5));
		


	}

}
