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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.output.TeeOutputStream;

/**
 * 
 * Utility object to coordinate atmospheric correction tasks.  Contains setter methods and execution
 * methods that call MODTRANprocessor methods with the set parameters.
 * 
 * @author Nicholas Clinton
 * TODO: initialize/execute from a config file.
 */
public class ATMcorr extends MODTRANprocessor {

	public static int MODTRAN4 = 4;
	public static int MODTRAN5 = 5;
	
	// spectral response functions
	List<double[][]> srfs;
	// Modtran 4 output:
	String r0tp7;
	String r05tp7;
	String r10tp7;
	// Modtran 5 output
	String mod5r0;
	String acdFile;
	// Exo-atmospheric irradiance:
	String exoFile;
	double[][] exo;
	// target emissivity
	double[][] emis;
	// imagery:
	String input;
	String output;
	// image parameters
	double sensor_zenith; // degrees
	double sun_zenith; // degrees
	int year;
	int month;
	int day;
	
	int modVersion;

	// correction parameters
	double[][] unknowns;
	
	/**
	 * Constructor.
	 */
	public ATMcorr() {
		// initialize null
		srfs = null;
		r0tp7 = null;
		r05tp7 = null;
		r10tp7 = null;
		mod5r0 = null;
		acdFile = null;
		exoFile = null;
		input = null;
		output = null;
		sensor_zenith = 0.0; //default
		sun_zenith = 0.0; //default
		year = 0;
		month = 0;
		day = 0;
	}

	/**
	 * Sets the spectral response functions for this ATMcorr object to use.
	 * @param srfs is a list of double arrays where each array is {nm, normalized response} for each band.
	 */
	public void setSrfs(List<double[][]> srfs) {
		this.srfs = srfs;
	}


	/**
	 * 
	 * @param r0tp7 is the zero reflectance Modtran4 tape 7 file output for this to use.
	 */
	public void setR0tp7(String r0tp7) {
		this.r0tp7 = r0tp7;
	}


	/**
	 * 
	 * @param r05tp7 is the 0.5 reflectance Modtran4 tape 7 file output for this to use.
	 */
	public void setR05tp7(String r05tp7) {
		this.r05tp7 = r05tp7;
	}


	/**
	 * 
	 * @param r10tp7 is the unit reflectance Modtran4 tape 7 file output for this to use.
	 */
	public void setR10tp7(String r10tp7) {
		this.r10tp7 = r10tp7;
	}


	/**
	 * 
	 * @param mod5r0 is the zero reflectance Modtran5 tape 7 file output for this to use.
	 */
	public void setMod5r0(String mod5r0) {
		this.mod5r0 = mod5r0;
	}


	/**
	 * 
	 * @param acdFile is the Modtran5 *.acd file output for this to use.
	 */
	public void setAcdFile(String acdFile) {
		this.acdFile = acdFile;
	}


	/**
	 * Set the exo-atmospheric irradiance for this to use.  The units must be in (W / sq M / micron)}.
	 * @param exoFile is the irradiance file.  If modExo is false, it must be {nm, W/sq M/micron}.
	 * @param modExo is a boolean flag for a Modtran formatted input.  If so, unit conversion is performed.
	 */
	public void setExoFile(String exoFile, boolean modExo) {
		this.exoFile = exoFile;
		if (modExo) {
			exo = mIrrad(exoFile);
		}
		else {
			exo = Utils.readFile(new File(exoFile), 2);
		}
	}

	/**
	 * 
	 * @param emis is a double[][] of {nm, emissivity}.
	 */
	public void setEmis(double[][] emis) {
		this.emis = emis;
	}


	/**
	 * 
	 * @param input is the input image file, with radiance units of W / sq M / micron / steradian.
	 */
	public void setInput(String input) {
		this.input = input;
	}


	/**
	 * 
	 * @param output is the output image filename, either reflectance or temp, depending on input.
	 */
	public void setOutput(String output) {
		this.output = output;
	}

	/**
	 * @param sensor_zenith is the sensor-zenith angle, in degrees, to use.
	 */
	public void setSensor_zenith(double sensor_zenith) {
		this.sensor_zenith = sensor_zenith;
	}


	/**
	 * 
	 * @param sun_zenith is the sun-zenith angle, in degrees, to use.
	 */
	public void setSun_zenith(double sun_zenith) {
		this.sun_zenith = sun_zenith;
	}


	/**
	 * 
	 * @param year is the year in which the image was acquired
	 */
	public void setYear(int year) {
		this.year = year;
	}

	/**
	 * 
	 * @param month is the month in which the image was acquired
	 */
	public void setMonth(int month) {
		this.month = month;
	}

	/**
	 * 
	 * @param day is the day in which the image was acquired
	 */
	public void setDay(int day) {
		this.day = day;
	}

	/**
	 * Set the processing type.  If MODTRAN4 is selected, zero, 0.5, and unit reflectance files (*.tp7)
	 * must have been set.  If MODTRAN5 is selected, zero reflectance and a *.acd file must have been set.
	 * @param modVersion is one of ATMcorr.MODTRAN4 or ATMcorr.MODTRAN5
	 */
	public void setModVersion(int modVersion) {
		this.modVersion = modVersion;
	}
	
	
	/**
	 * Convert the Modtran inputs to lookup parameters for the Vis-NIR-SWIR part of the spectrum.
	 * Whatever SRFs have already been set are used (no check is performed on the SRF wavelengths).
	 * The Modtran outputs, exo-atmospheric irradiance, and sensor zenith must have been set as well as SRFs.  
	 * @param processLog is the filename for an output log from the console.
	 * @throws Exception
	 */
	public void processVIS(String processLog) throws Exception {
		PrintStream console = System.out;
		OutputStream fileStream = new FileOutputStream(processLog, true);
		System.setOut(new PrintStream(new TeeOutputStream(System.out, fileStream)));
		
		if (modVersion == ATMcorr.MODTRAN4) {
			unknowns = vbMod4(new String[] {r0tp7, r05tp7, r10tp7}, srfs, exo, sensor_zenith);		
		}
		else if (modVersion == ATMcorr.MODTRAN5) {
			unknowns = acdMod5(acdFile, mod5r0, srfs);
		}
		else {
			throw new Exception("Unrecognized MODTRAN version.");
		}
		// return to console
		System.setOut(console);
	}
	
	/**
	 * Convert the Modtran inputs to lookup parameters for the MWIR-TIR part of the spectrum.
	 * Whatever SRFs have already been set are used (no check is performed on the SRF wavelengths).
	 * The Modtran outputs must have been set as well as the SRFs.  
	 * @param processLog is the filename for an output log from the console.
	 * @throws Exception
	 */
	public void processTIR(String processLog) throws Exception {
		PrintStream console = System.out;
		OutputStream fileStream = new FileOutputStream(processLog,true);
		System.setOut(new PrintStream(new TeeOutputStream(System.out, fileStream)));

		if (modVersion == ATMcorr.MODTRAN4) {
			unknowns = thermal(new String[] {r0tp7, r05tp7, r10tp7}, srfs);		
		}
		else if (modVersion == ATMcorr.MODTRAN5) {
			unknowns = thermal5(acdFile, mod5r0, srfs);
		}
		else {
			throw new Exception("Unrecognized MODTRAN version.");
		}
		// return to console
		System.setOut(console);
	}
	
	/**
	 * Convert the set input image to reflectance, using the unknowns generated by {@link ATMcorr#processVIS(String)}.
	 * The bands indices of the input must match the band indices of the SRFs used to generate the unknowns.
	 */
	public void rad2ref() {
		// in-band exo-atmospheric irradiance
		double[] bExo = SRFUtils.getInBand(srfs, exo);
		rad2ref(input, output, bExo, unknowns, year, month, day, sun_zenith);
	}
	
	/**
	 * Convert the set input image to reflectance, using the unknowns generated by {@link ATMcorr#processVIS(String)}.
	 * The bands indices of the input must match the band indices of the SRFs used to generate the unknowns. 
	 * @param scale is a multiplier for the input data, to put the units in  W/sq.M/micron/steradian.
	 */
	public void rad2ref(double scale) {
		double[] bExo = SRFUtils.getInBand(srfs, exo);
		rad2ref(input, output, bExo, unknowns, year, month, day, sun_zenith, scale);
	}
	
	/**
	 * Convert the set input image to temperature, in K, using the unknowns generated by {@link ATMcorr#processTIR(String)}.
	 * The bands indices of the input must match the band indices of the SRFs used to generate the unknowns.
	 */
	public void rad2temp() {
		double[] wavelengths = SRFUtils.getMeans(srfs);
		double[] emissivity = SRFUtils.getInBand(srfs, emis);
		for (int i=0; i<srfs.size(); i++) {
			System.out.println("band "+(i+1)+"wavelength: "+wavelengths[i]+" nm");
			System.out.println("band "+(i+1)+"emissivity: "+emissivity[i]);
		}
		rad2temp(input, output, unknowns, wavelengths, emissivity);
	}
	
	/**
	 * Unfinished config reading method.  TODO: finish.
	 * @param fileName is the filename of the config file from which to get correction parameters.
	 */
	public void readConfig(String fileName) {
		System.out.println("Using config file: "+fileName);
		
		BufferedReader reader = null;
		try {
			File file = new File(fileName);
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = reader.readLine()) != null) {
				System.out.println("Reading configuration key=value pair: "+line);
				String[] keyVal = line.split("=");
				if (keyVal[0].trim().equals("")) {
					// set something
				}
				else if (keyVal[0].trim().equals("")){
					// set something else
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * Test code and processing log.
	 * @param args
	 */
	public static void main(String[] args) {
		
//		// 20110706 SARP 2011, June 30, Ocean line
//		ATMcorr ocean1 = new ATMcorr();
//		ocean1.setR0tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_ocean_r0.tp7");
//		ocean1.setR05tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_ocean_r05.tp7");
//		ocean1.setR10tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_ocean_r10.tp7");
//		ocean1.setDay(30);
//		ocean1.setMonth(6);
//		ocean1.setYear(2011);
//		// use Modtran exo-atmopsheric irradiance
//		ocean1.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		ocean1.setEmis(Utils.readFile(new File("C:/Users/owner/Documents/ASTL/SARP2011/spectra/seawater_emissivity.csv"), 0));
//		// elevation to solar-zenith
//		ocean1.setSun_zenith(90.0 - 39.8);
//		ocean1.setSensor_zenith(0.0);
//		ocean1.setModVersion(ATMcorr.MODTRAN4);
//		try {
//			// srfs on disk:
//			String srfRoot = "C:/Users/owner/Documents/ASTL/MASTER_srfs_June2011/msr0611a.c";
//			int srfHeader = 12;
//			boolean microns = true;
//			
//			// set the srfs to vis-nir bands
//			ocean1.setSrfs(SRFUtils.readSRFs(srfRoot, 1, 25, srfHeader, microns));
//			String input = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_09_20110630_2334_2341_V01_vis_nir";
//			ocean1.setInput(input);
//			String output = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_09_20110630_2334_2341_V01_vis_nir_ref_";
//			ocean1.setOutput(output);
//			String processLog = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_09_20110630_2334_2341_V01_vis_nir_ref_log.txt";
//			ocean1.processVIS(processLog);
//			ocean1.rad2ref();
//			
//			// set srfs to thermal
//			ocean1.setSrfs(SRFUtils.readSRFs(srfRoot, 26, 50, srfHeader, microns));
//			input = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_09_20110630_2334_2341_V01_tir";
//			ocean1.setInput(input);
//			output = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_09_20110630_2334_2341_V01_tir_temp_";
//			ocean1.setOutput(output);
//			processLog = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_09_20110630_2334_2341_V01_tir_log.txt";
//			ocean1.processTIR(processLog);
//			ocean1.rad2temp();
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		// 20110707 SARP 2011 Lost Hills morning
//		
//		ATMcorr valleyAM1 = new ATMcorr();
//		valleyAM1.setR0tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_us76_r0.tp7");
//		valleyAM1.setR05tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_us76_r05.tp7");
//		valleyAM1.setR10tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_us76_r10.tp7");
//		valleyAM1.setDay(30);
//		valleyAM1.setMonth(6);
//		valleyAM1.setYear(2011);
//		valleyAM1.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		valleyAM1.setEmis(Utils.readFile(new File("C:/Users/owner/Documents/ASTL/SARP2011/spectra/deciduous_trees_emissivity.csv"), 0));
//		valleyAM1.setSun_zenith(90.0 - 58.3);
//		valleyAM1.setSensor_zenith(0.0);
//		valleyAM1.setModVersion(ATMcorr.MODTRAN4);
//		try {
//			// srfs:
//			String srfRoot = "C:/Users/owner/Documents/ASTL/MASTER_srfs_June2011/msr0611a.c";
//			int srfHeader = 12;
//			boolean microns = true;
//			
//			valleyAM1.setSrfs(SRFUtils.readSRFs(srfRoot, 1, 25, srfHeader, microns));
//			String input = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011a/MASTERL1B_1100305_01_20110630_1759_1802_V01_vis_nir";
//			valleyAM1.setInput(input);
//			String output = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011a/MASTERL1B_1100305_01_20110630_1759_1802_V01_vis_nir_ref_";
//			valleyAM1.setOutput(output);
//			String processLog = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011a/MASTERL1B_1100305_01_20110630_1759_1802_V01_vis_nir_log.txt";
//			valleyAM1.processVIS(processLog);
//			valleyAM1.rad2ref();
//			
//			valleyAM1.setSrfs(SRFUtils.readSRFs(srfRoot, 26, 50, srfHeader, microns));
//			input = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011a/MASTERL1B_1100305_01_20110630_1759_1802_V01_tir";
//			valleyAM1.setInput(input);
//			output = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011a/MASTERL1B_1100305_01_20110630_1759_1802_V01_tir_temp_";
//			valleyAM1.setOutput(output);
//			processLog = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011a/MASTERL1B_1100305_01_20110630_1759_1802_V01_tir_log.txt";
//			valleyAM1.processTIR(processLog);
//			valleyAM1.rad2temp();
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		//20110707 Central Valley PM
//		
//		ATMcorr valleyPM1 = new ATMcorr();
//		valleyPM1.setR0tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_us76_PM_r0.tp7");
//		valleyPM1.setR05tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_us76_PM_r05.tp7");
//		valleyPM1.setR10tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_us76_PM_r10.tp7");
//		valleyPM1.setDay(30);
//		valleyPM1.setMonth(6);
//		valleyPM1.setYear(2011);
//		valleyPM1.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		valleyPM1.setEmis(Utils.readFile(new File("C:/Users/owner/Documents/ASTL/SARP2011/spectra/deciduous_trees_emissivity.csv"), 0));
//		valleyPM1.setSun_zenith(90.0 - 60.3);
//		valleyPM1.setSensor_zenith(0.0);
//		valleyPM1.setModVersion(ATMcorr.MODTRAN4);
//		try {
//			// srfs:
//			String srfRoot = "C:/Users/owner/Documents/ASTL/MASTER_srfs_June2011/msr0611a.c";
//			int srfHeader = 12;
//			boolean microns = true;
//			
//			valleyPM1.setSrfs(SRFUtils.readSRFs(srfRoot, 1, 25, srfHeader, microns));
//			String input = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_01_20110630_2155_2159_V01_vis_nir";
//			valleyPM1.setInput(input);
//			String output = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_01_20110630_2155_2159_V01_vis_nir_ref_";
//			valleyPM1.setOutput(output);
//			String processLog = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_01_20110630_2155_2159_V01_vis_nir_log.txt";
//			valleyPM1.processVIS(processLog);
//			valleyPM1.rad2ref();
//			
//			valleyPM1.setSrfs(SRFUtils.readSRFs(srfRoot, 26, 50, srfHeader, microns));
//			input = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_01_20110630_2155_2159_V01_tir";
//			valleyPM1.setInput(input);
//			output = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_01_20110630_2155_2159_V01_tir_temp_";
//			valleyPM1.setOutput(output);
//			processLog = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_01_20110630_2155_2159_V01_tir_log.txt";
//			valleyPM1.processTIR(processLog);
//			valleyPM1.rad2temp();
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// 20110707 Headwall CIRPAS
		// 20110712 reprocess
//		ATMcorr ocean1 = new ATMcorr();
//		ocean1.setR0tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11-783-02_l10_r1_r0.tp7");
//		ocean1.setR05tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11-783-02_l10_r1_r05.tp7");
//		ocean1.setR10tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11-783-02_l10_r1_r10.tp7");
//		ocean1.setDay(11);
//		ocean1.setMonth(10);
//		ocean1.setYear(2010);
//		ocean1.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		ocean1.setSun_zenith(90.0 - 45.6);
//		ocean1.setSensor_zenith(0.0);
//		ocean1.setModVersion(ATMcorr.MODTRAN4);
//		String input = "H:/headwall/CIRPAS_2010/Imagery/11-783-02_2010_10_11_19_30_39_9_rad";
//		ocean1.setInput(input);
//		// srfs
//		double[] wavelengths = GDALUtils.getWavelengths(input);
//		double[] fwhms = new double[wavelengths.length];
//		Arrays.fill(fwhms, 1.5);
//		ocean1.setSrfs(SRFUtils.getGaussians(wavelengths, fwhms));
//		try {
//			String output = "H:/headwall/CIRPAS_2010/Imagery/11-783-02_2010_10_11_19_30_39_9_ref_";
//			ocean1.setOutput(output);
//			String processLog = "H:/headwall/CIRPAS_2010/Imagery/11-783-02_2010_10_11_19_30_39_9_ref_log.txt";
//			ocean1.processVIS(processLog);
//			ocean1.rad2ref(1.0/100.0);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		// 20110708 Headwall CIRPAS Oct 11, binning=4
//		// 20110712 reprocess
//		ocean1 = new ATMcorr();
//		ocean1.setR0tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11-783-02_l10_r2_r0.tp7");
//		ocean1.setR05tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11-783-02_l10_r2_r05.tp7");
//		ocean1.setR10tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11-783-02_l10_r2_r10.tp7");
//		ocean1.setDay(11);
//		ocean1.setMonth(10);
//		ocean1.setYear(2010);
//		ocean1.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		ocean1.setSensor_zenith(0.0);
//		ocean1.setModVersion(ATMcorr.MODTRAN4);
//		input = "H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_38_34_1_rad";
//		ocean1.setInput(input);
//		// srfs, good for all the 4x4 binned files in this group
//		wavelengths = GDALUtils.getWavelengths(input);
//		fwhms = new double[wavelengths.length];
//		Arrays.fill(fwhms, 3.0);
//		ocean1.setSrfs(SRFUtils.getGaussians(wavelengths, fwhms));
//		try {
//			ocean1.setSun_zenith(90.0 - 44.7);
//			ocean1.setOutput("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_38_34_1_ref_");
//			ocean1.processVIS("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_38_34_1_ref_log.txt");
//			ocean1.rad2ref(1.0/100.0);
//			
//			// next...
//			ocean1.setInput("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_44_41_2_rad");
//			ocean1.setSun_zenith(90.0 - 44.4);
//			ocean1.setOutput("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_44_41_2_ref_");
//			ocean1.processVIS("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_44_41_2_ref_log.txt");
//			ocean1.rad2ref(1.0/100.0);
//			
//			ocean1.setInput("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_50_13_3_rad");
//			ocean1.setSun_zenith(90.0 - 44.1);
//			ocean1.setOutput("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_50_13_3_ref_");
//			ocean1.processVIS("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_50_13_3_ref_log.txt");
//			ocean1.rad2ref(1.0/100.0);
//			
//			ocean1.setInput("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_56_20_4_rad");
//			ocean1.setSun_zenith(90.0 - 43.7);
//			ocean1.setOutput("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_56_20_4_ref_");
//			ocean1.processVIS("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_20_56_20_4_ref_log.txt");
//			ocean1.rad2ref(1.0/100.0);
//			
//			ocean1.setInput("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_21_03_31_5_rad");
//			ocean1.setSun_zenith(90.0 - 43.3);
//			ocean1.setOutput("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_21_03_31_5_ref_");
//			ocean1.processVIS("H:/headwall/CIRPAS_2010/Imagery/11-783-02d_2010_10_11_21_03_31_5_ref_log.txt");
//			ocean1.rad2ref(1.0/100.0);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// 20110712 Headwall Pinto Lake
//		ATMcorr ocean1 = new ATMcorr();
//		ocean1.setR0tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11-783-04_pinto_r0.tp7");
//		ocean1.setR05tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11-783-04_pinto_r05.tp7");
//		ocean1.setR10tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11-783-04_pinto_r10.tp7");
//		ocean1.setDay(13);
//		ocean1.setMonth(10);
//		ocean1.setYear(2010);
//		ocean1.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		ocean1.setSensor_zenith(0.0);
//		ocean1.setModVersion(ATMcorr.MODTRAN4);
//		// Pinto Lake, binning = 2x2
//		String input = "H:/headwall/CIRPAS_2010/Imagery/Pinto/11-783-04b_2010_10_13_18_19_58_3_rad";
//		ocean1.setInput(input);
//		double[] wavelengths = GDALUtils.getWavelengths(input);
//		double[] fwhms = new double[wavelengths.length];
//		Arrays.fill(fwhms, 1.5);
//		ocean1.setSrfs(SRFUtils.getGaussians(wavelengths, fwhms));
//		try {
//			ocean1.setSun_zenith(90.0 - 40.1);
//			ocean1.setOutput("H:/headwall/CIRPAS_2010/Imagery/Pinto/11-783-04b_2010_10_13_18_19_58_3_ref_");
//			ocean1.processVIS("H:/headwall/CIRPAS_2010/Imagery/Pinto/11-783-04b_2010_10_13_18_19_58_3_ref_log.txt");
//			ocean1.rad2ref(1.0/100.0);
//			
//			// Pinto Lake, binning = 4x4
//			input = "H:/headwall/CIRPAS_2010/Imagery/Pinto/11-783-04c_2010_10_13_18_42_26_2_rad";
//			ocean1.setInput(input);
//			wavelengths = GDALUtils.getWavelengths(input);
//			fwhms = new double[wavelengths.length];
//			Arrays.fill(fwhms, 3.0);
//			ocean1.setSrfs(SRFUtils.getGaussians(wavelengths, fwhms));
//			ocean1.setSun_zenith(90.0 - 42.0);
//			ocean1.setOutput("H:/headwall/CIRPAS_2010/Imagery/Pinto/11-783-04c_2010_10_13_18_42_26_2_ref_");
//			ocean1.processVIS("H:/headwall/CIRPAS_2010/Imagery/Pinto/11-783-04c_2010_10_13_18_42_26_2_ref_log.txt");
//			ocean1.rad2ref(1.0/100.0);
//			
//			ocean1.setInput("H:/headwall/CIRPAS_2010/Imagery/Pinto/11-783-04c_2010_10_13_18_50_16_3_rad");
//			ocean1.setSun_zenith(90.0 - 42.8);
//			ocean1.setOutput("H:/headwall/CIRPAS_2010/Imagery/Pinto/11-783-04c_2010_10_13_18_50_16_3_ref_");
//			ocean1.processVIS("H:/headwall/CIRPAS_2010/Imagery/Pinto/11-783-04c_2010_10_13_18_50_16_3_ref_log.txt");
//			ocean1.rad2ref(1.0/100.0);	
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20110713 SARP 2011, June 30, Ocean lines 10-12
//		ATMcorr ocean2 = new ATMcorr();
//		ocean2.setR0tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_ocean2_r0.tp7");
//		ocean2.setR05tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_ocean2_r05.tp7");
//		ocean2.setR10tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_ocean2_r10.tp7");
//		ocean2.setDay(30);
//		ocean2.setMonth(6);
//		ocean2.setYear(2011);
//		// use Modtran exo-atmopsheric irradiance
//		ocean2.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		ocean2.setEmis(Utils.readFile(new File("C:/Users/owner/Documents/ASTL/SARP2011/spectra/seawater_emissivity.csv"), 0));
//		// elevation to solar-zenith
//		ocean2.setSensor_zenith(0.0);
//		ocean2.setModVersion(ATMcorr.MODTRAN4);
//		try {
//			// srfs on disk:
//			String srfRoot = "C:/Users/owner/Documents/ASTL/MASTER_srfs_June2011/msr0611a.c";
//			int srfHeader = 12;
//			boolean microns = true;
//			// set the srfs to vis-nir bands
//			ocean2.setSrfs(SRFUtils.readSRFs(srfRoot, 1, 25, srfHeader, microns));
//			// line 10
//			ocean2.setSun_zenith(90.0 - 40.8);
//			ocean2.setInput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_10_20110630_2344_2350_V01_vis_nir");
//			ocean2.setOutput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_10_20110630_2344_2350_V01_ref");
//			ocean2.processVIS("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_10_20110630_2344_2350_V01_ref_log.txt");
//			ocean2.rad2ref();
//			// line 11
//			ocean2.setSun_zenith(90.0 - 38.8);
//			ocean2.setInput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_11_20110630_2354_0001_V01_vis_nir");
//			ocean2.setOutput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_11_20110630_2354_0001_V01_ref");
//			ocean2.processVIS("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_11_20110630_2354_0001_V01_ref_log.txt");
//			ocean2.rad2ref();
//			// line 12
//			ocean2.setSun_zenith(90.0 - 36.8);
//			ocean2.setInput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_12_20110701_0004_0010_V01_vis_nir");
//			ocean2.setOutput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_12_20110701_0004_0010_V01_ref");
//			ocean2.processVIS("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_12_20110701_0004_0010_V01_ref_log.txt");
//			ocean2.rad2ref();
//			
//			// set srfs to thermal
//			ocean2.setSrfs(SRFUtils.readSRFs(srfRoot, 26, 50, srfHeader, microns));
//			// line 10
//			ocean2.setSun_zenith(90.0 - 40.8);
//			ocean2.setInput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_10_20110630_2344_2350_V01_tir");
//			ocean2.setOutput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_10_20110630_2344_2350_V01_temp");
//			ocean2.processTIR("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_10_20110630_2344_2350_V01_temp_log.txt");
//			ocean2.rad2temp();
//			// line 11
//			ocean2.setSun_zenith(90.0 - 38.8);
//			ocean2.setInput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_11_20110630_2354_0001_V01_tir");
//			ocean2.setOutput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_11_20110630_2354_0001_V01_temp");
//			ocean2.processTIR("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_11_20110630_2354_0001_V01_temp_log.txt");
//			ocean2.rad2temp();
//			// line 12
//			ocean2.setSun_zenith(90.0 - 36.8);
//			ocean2.setInput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_12_20110701_0004_0010_V01_tir");
//			ocean2.setOutput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_12_20110701_0004_0010_V01_temp");
//			ocean2.processTIR("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_12_20110701_0004_0010_V01_temp_log.txt");
//			ocean2.rad2temp();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// 20110718 Ocean line, June 28
//		ATMcorr ocean3 = new ATMcorr();
//		ocean3.setR0tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100304_ocean_r0.tp7");
//		ocean3.setR05tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100304_ocean_r05.tp7");
//		ocean3.setR10tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100304_ocean_r10.tp7");
//		ocean3.setDay(28);
//		ocean3.setMonth(6);
//		ocean3.setYear(2011);
//		// use Modtran exo-atmopsheric irradiance
//		ocean3.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		ocean3.setEmis(Utils.readFile(new File("C:/Users/owner/Documents/ASTL/SARP2011/spectra/seawater_emissivity.csv"), 0));
//		// elevation to solar-zenith
//		ocean3.setSensor_zenith(0.0);
//		ocean3.setModVersion(ATMcorr.MODTRAN4);
//		try {
//			// srfs on disk:
//			String srfRoot = "C:/Users/owner/Documents/ASTL/MASTER_srfs_June2011/msr0611a.c";
//			int srfHeader = 12;
//			boolean microns = true;
//			// set the srfs to vis-nir bands
//			ocean3.setSrfs(SRFUtils.readSRFs(srfRoot, 1, 25, srfHeader, microns));
//			// line 10
//			ocean3.setSun_zenith(51.1);
//			ocean3.setInput("C:/Users/owner/Documents/ASTL/SARP2011/28June2011b/MASTERL1B_1100304_01_20110628_2348_2356_V01_vis_swir");
//			ocean3.setOutput("C:/Users/owner/Documents/ASTL/SARP2011/28June2011b/MASTERL1B_1100304_01_20110628_2348_2356_V01_ref");
//			ocean3.processVIS("C:/Users/owner/Documents/ASTL/SARP2011/28June2011b/MASTERL1B_1100304_01_20110628_2348_2356_V01_ref_log.txt");
//			ocean3.rad2ref();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20110720
//		ATMcorr bakers = new ATMcorr();
//		bakers.setR0tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_bakers_r0.tp7");
//		bakers.setR05tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_bakers_r05.tp7");
//		bakers.setR10tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/1100305_bakers_r10.tp7");
//		bakers.setDay(30);
//		bakers.setMonth(6);
//		bakers.setYear(2011);
//		// use Modtran exo-atmopsheric irradiance
//		bakers.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		// elevation to solar-zenith
//		bakers.setSensor_zenith(0.0);
//		bakers.setModVersion(ATMcorr.MODTRAN4);
//		try {
//			// srfs on disk:
//			String srfRoot = "C:/Users/owner/Documents/ASTL/MASTER_srfs_June2011/msr0611a.c";
//			int srfHeader = 12;
//			boolean microns = true;
//			// set the srfs to vis-nir bands
//			bakers.setSrfs(SRFUtils.readSRFs(srfRoot, 1, 25, srfHeader, microns));
//			// line 10
//			bakers.setSun_zenith(35.3);
//			bakers.setInput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_05_20110630_2230_2235_V01_vis_swir");
//			bakers.setOutput("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_05_20110630_2230_2235_V01_ref");
//			bakers.processVIS("C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_05_20110630_2230_2235_V01_ref_log.txt");
//			bakers.rad2ref();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// Ivanpah day 1
//		ATMcorr ivanpah = new ATMcorr();
//		// modtran4
//		ivanpah.setR0tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11651_Ivanpah_sonde_r0.tp7");
//		ivanpah.setR05tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11651_Ivanpah_sonde_r05.tp7");
//		ivanpah.setR10tp7("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/TEST/11651_Ivanpah_sonde_r10.tp7");
//		// modtran5
//		ivanpah.setAcdFile("C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11651_Ivanpah_sonde_r0.acd");
//		ivanpah.setMod5r0("C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11651_Ivanpah_sonde_r0.tp7");
//		
//		ivanpah.setDay(8);
//		ivanpah.setMonth(6);
//		ivanpah.setYear(2011);
//		// use Modtran exo-atmopsheric irradiance
//		ivanpah.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		// elevation to solar-zenith
//		ivanpah.setSensor_zenith(0.0);
//		
//		try {
//			// srfs on disk:
//			String srfRoot = "C:/Users/owner/Documents/ASTL/MASTER_srfs_June2011/msr0611a.c";
//			int srfHeader = 12;
//			boolean microns = true;
//			// set the srfs to vis-nir bands
//			ivanpah.setSrfs(SRFUtils.readSRFs(srfRoot, 1, 25, srfHeader, microns));
//			// inputs
//			ivanpah.setSun_zenith(46.3);
//			ivanpah.setInput("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165100_04_20110608_2305_2310_V00_vis_swir");
//			
//			// 11-651-00, modtran 4
////			ivanpah.setModVersion(ATMcorr.MODTRAN4);
////			ivanpah.setOutput("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165100_04_20110608_2305_2310_V00_ref_mod4_sonde");
////			ivanpah.processVIS("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165100_04_20110608_2305_2310_V00_ref_mod4_sonde_log.txt");
////			ivanpah.rad2ref();
//			
//			// 11-651-00, modtran 5
//			ivanpah.setModVersion(ATMcorr.MODTRAN5);
//			ivanpah.setOutput("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165100_04_20110608_2305_2310_V00_ref_mod5_sonde");
//			ivanpah.processVIS("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165100_04_20110608_2305_2310_V00_ref_mod5_sonde_log.txt");
//			ivanpah.rad2ref();
//			
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// Day 2
//		ATMcorr ivanpah = new ATMcorr();
//		// modtran5
//		ivanpah.setAcdFile("C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11652_Ivanpah_sonde_r0.acd");
//		ivanpah.setMod5r0("C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11652_Ivanpah_sonde_r0.tp7");
//		ivanpah.setDay(9);
//		ivanpah.setMonth(6);
//		ivanpah.setYear(2011);
//		// use Modtran exo-atmopsheric irradiance
//		ivanpah.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		// elevation to solar-zenith
//		ivanpah.setSensor_zenith(0.0);
//		
//		try {
//			// srfs on disk:
//			String srfRoot = "C:/Users/owner/Documents/ASTL/MASTER_srfs_June2011/msr0611a.c";
//			int srfHeader = 12;
//			boolean microns = true;
//			// set the srfs to vis-nir bands
//			ivanpah.setSrfs(SRFUtils.readSRFs(srfRoot, 1, 25, srfHeader, microns));
//			// inputs
//			ivanpah.setSun_zenith(37.8);
//			ivanpah.setInput("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165200_01_20110609_1654_1656_V00_vis_swir");
//			
//			// 11-651-00, modtran 5
//			ivanpah.setModVersion(ATMcorr.MODTRAN5);
//			ivanpah.setOutput("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165200_01_20110609_1654_1656_V00_ref_mod5_sonde");
//			ivanpah.processVIS("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165200_01_20110609_1654_1656_V00_ref_mod5_sonde_log.txt");
//			ivanpah.rad2ref();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
//		ATMcorr ivanpah = new ATMcorr();
//		// modtran5
//		ivanpah.setAcdFile("C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11651_Ivanpah_sonde2_r0.acd");
//		ivanpah.setMod5r0("C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11651_Ivanpah_sonde2_r0.tp7");
//		
//		ivanpah.setDay(8);
//		ivanpah.setMonth(6);
//		ivanpah.setYear(2011);
//		// use Modtran exo-atmopsheric irradiance
//		ivanpah.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		// elevation to solar-zenith
//		ivanpah.setSensor_zenith(0.0);
//		
//		try {
//			// srfs on disk:
//			String srfRoot = "C:/Users/owner/Documents/ASTL/MASTER_srfs_June2011/msr0611a.c";
//			int srfHeader = 12;
//			boolean microns = true;
//			// set the srfs to vis-nir bands
//			ivanpah.setSrfs(SRFUtils.readSRFs(srfRoot, 1, 25, srfHeader, microns));
//			// inputs
//			ivanpah.setSun_zenith(46.3);
//			ivanpah.setInput("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165100_04_20110608_2305_2310_V00_vis_swir");
//			
//			// 11-651-00, modtran 5
//			ivanpah.setModVersion(ATMcorr.MODTRAN5);
//			ivanpah.setOutput("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165100_04_20110608_2305_2310_V00_ref_mod5_sonde2");
//			ivanpah.processVIS("C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165100_04_20110608_2305_2310_V00_ref_mod5_sonde2_log.txt");
//			ivanpah.rad2ref();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// AVIRIS Ivanpah 06092011 -- Bad georeferencing
//		ATMcorr ivanpah = new ATMcorr();
//		// modtran5
//		ivanpah.setAcdFile("C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11652_Ivanpah_sonde_r0.acd");
//		ivanpah.setMod5r0("C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11652_Ivanpah_sonde_r0.tp7");
//		ivanpah.setDay(9);
//		ivanpah.setMonth(6);
//		ivanpah.setYear(2011);
//		// use Modtran exo-atmopsheric irradiance
//		ivanpah.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		// elevation to solar-zenith
//		ivanpah.setSensor_zenith(0.0);
//		ivanpah.setSun_zenith(37.74); // AVIRIS scene midpoint
//		
//		// AVIRIS
//		String input = "C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/f110609t01p00r06rdn_a/f110609t01p00r06rdn_a_sc01_ort_img_scaled";
//		ivanpah.setInput(input);
//		ivanpah.setModVersion(ATMcorr.MODTRAN5);
//		double[] wavelengths = GDALUtils.getAVIRISWavelengths(input);
//		double[] fwhms = GDALUtils.getENVIfwhm(input);
//		ivanpah.setSrfs(SRFUtils.getGaussians(wavelengths, fwhms));
//		try {
//			ivanpah.setOutput(input+"_mod5_ref");
//			ivanpah.processVIS(input+"_mod5_ref_log.txt");
//			ivanpah.rad2ref();
//				
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// AVIRIS Ivanpah 06082011
//		ATMcorr ivanpah = new ATMcorr();
//		// modtran5
//		ivanpah.setAcdFile("C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11651_Ivanpah_sonde2_r0.acd");
//		ivanpah.setMod5r0("C:/Users/owner/Documents/MODTRAN/pc_5v2r11/TEST/11651_Ivanpah_sonde2_r0.tp7");
//		ivanpah.setDay(8);
//		ivanpah.setMonth(6);
//		ivanpah.setYear(2011);
//		// use Modtran exo-atmopsheric irradiance
//		ivanpah.setExoFile("C:/Users/owner/Documents/MODTRAN/mod4v2r1/PC/DATA/newkur.dat", true);
//		// elevation to solar-zenith
//		ivanpah.setSensor_zenith(0.0); // nadir
//		ivanpah.setSun_zenith(46.684); // AVIRIS scene midpoint
//		
//		// AVIRIS
//		String input = "C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/f110608t01p00r08rdn_a/f110608t01p00r08rdn_a_sc01_ort_img_scaled";
//		ivanpah.setInput(input);
//		ivanpah.setModVersion(ATMcorr.MODTRAN5);
//		double[] wavelengths = GDALUtils.getAVIRISWavelengths(input);
//		double[] fwhms = GDALUtils.getENVIfwhm(input);
//		ivanpah.setSrfs(SRFUtils.getGaussians(wavelengths, fwhms));
//		try {
//			ivanpah.setOutput(input+"_mod5_ref");
//			ivanpah.processVIS(input+"_mod5_ref_log.txt");
//			ivanpah.rad2ref();
//				
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		
	}

	
	
	
}
