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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.MatrixIndexException;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;


/**
 * 
 * Utility class with static methods.
 * 
 * @author Nicholas Clinton
 */
public class Utils {

	/**
	 * Time converter.
	 * @param time as "hours:minutes:seconds.decimal seconds"
	 * @return decimal hours
	 */
	public static double timeDecimal(String time) {
		String[] times = time.split(":");
		if (times.length != 3) {
			return -9999;
		}
		double decimal = Double.parseDouble(times[0]);
		decimal += Double.parseDouble(times[1])/60.0;
		decimal += Double.parseDouble(times[2])/3600.0;
		return decimal;
	}
	
	/**
	 * Time converter.
	 * @param time as "hours:minutes:seconds.decimal seconds"
	 * @return decimal seconds
	 */
	public static double timeDecimalSeconds(String time) {
		return timeDecimal(time)*3600;
	}
	
	/**
	 * @param year
	 * @param month. Valid values are in [1,12]
	 * @param day
	 * @return
	 */
	public static int dayOfYear(int year, int month, int day) {
		Calendar c = Calendar.getInstance();
		c.set(year, month-1, day);
		return c.get(c.DAY_OF_YEAR);
	}
	
	/**
	 * Compute seasonal Earth-Sun distance.
	 * See http://noaasis.noaa.gov/NOAASIS/ml/aboutn14vis.html
	 * @param year
	 * @param month
	 * @param day
	 * @return Earth-Sun Distance in Astronomical units.
	 */
	public static double esDistance (int year, int month, int day) {
		int dayOfYear = dayOfYear(year, month, day);
		return esDistance(dayOfYear);
	}
	
	/**
	 * Compute seasonal Earth-Sun distance.
	 * See http://noaasis.noaa.gov/NOAASIS/ml/aboutn14vis.html
	 * @param dayOfYear
	 * @return Earth-Sun Distance in Astronomical units.
	 */
	public static double esDistance(int dayOfYear) {
		double theta = 0.9863*dayOfYear*Math.PI/180.0;
		double d_sq = 1.0/(1.00011 + 0.034221*Math.cos(theta) + 0.001280*Math.sin(theta) + 
				0.000719*Math.cos(2.0*theta) + 0.000077*Math.sin(2.0*theta));
		return Math.sqrt(d_sq);
	}
	
	/**
	 * Bit shifting magic.
	 * @param x is an integer to find the nearest power of 2.
	 * @return the int the power of two of which is closest to x.
	 */
	public static int nearestPowerOf2( int x ) {
	    --x;    
		x |= x >> 1;
	    x |= x >> 2;    
		x |= x >> 4;    
		x |= x >> 8;    
		x |= x >> 16;
		x |= x >> 32;
		return ++x;
	}
	
	/**
	 * For use with the Apache commons math FFT, which requires a power of 2 length array.
	 * @param input is the array to be padded
	 * @return a larger array, the end of which has been padded with zeros.
	 */
	public static double[] padZeros (double[] input) {
		int newLength = nearestPowerOf2(input.length);
		double[] output = new double[newLength];
		// copy
		for (int i=0; i<input.length; i++) {
			output[i] = input[i];
		}
		// pad
		for (int i = input.length; i<output.length; i++) {
			output[i] = 0.0;
		}
		return output;
	}
	
	
	/**
	 * For use with the Apache commons math FFT, which requires a power of 2 length array.
	 * @param input is the array to be padded
	 * @return a larger array, front and back of which have been padded
	 */
	public static double[] padEnds(double[] input) {
		int newLength = nearestPowerOf2(input.length);
		double[] output = new double[newLength];
		int padLength = Math.abs(input.length-newLength)/2;
		double first = input[0];
		double last = input[input.length-1];
		// pad and copy
		for (int i=0; i<output.length; i++) {
			if (i < padLength) {
				output[i] = first;
			}
			else if (i >= input.length+padLength) {
				output[i] = last;
			}
			else {
				output[i] = input[i-padLength];
			}
		}

		return output;
	}
	
	
	/**
	 * Recover the original data from a padded array.
	 * @param padded is the extended array, that has been padded.
	 * @param length is the original length
	 * @return the unpadded array.
	 */
	public static double[] getUnPadded(double[] padded, int length) {
		double[] original = new double[length];
		int padLength = Math.abs(length-padded.length)/2;
		// skip pad values
		for (int i=0; i<padded.length; i++) {
			if (i < padLength) {
				continue;
			}
			else if (i >= length+padLength) {
				continue;
			}
			else {
				original[i-padLength] = padded[i];
			}
		}

		return original;
	}
	
	/**
	 * Butterworth filter for frequency domain.
	 * @param length is the size of the filter
	 * @param thresh is the threshold where frequencies > threshold are attenuated.
	 * @return 
	 */
	public static double[] butterworth(int length, double threshold) {
		double[] freqs = freqs(length);
		double[] filter = new double[length];
		for (int i=0; i<length; i++) {
			filter[i] = 1.0/Math.sqrt(1.0+Math.pow((freqs[i]/threshold), 10));
		}
		// temporary check 20091209
		//String check = "C:/Documents and Settings/nick/My Documents/BB_smoothing/03-613_bb_smoothing/bworth01_filt_check.txt";
		//writeFile(new double[][] {freqs, filter}, check);
		return filter;
	}
	
	/**
	 * Compute frequencies in per sample (wavenumber) units
	 * @param length of the fft vector for which corresponding frequencies are returned
	 * @return a vector of frequencies in wavenumber per sample 
	 */
	public static double[] freqs(int length) {
		double[] freqs = new double[length];
		int nyquist = length/2;
		for (int i=0; i<length; i++) {
			freqs[i] = (i <= nyquist) ? (double)i/length : (double)(length-i)/length;
		}
		return freqs;
	}
	
	/**
	 * Planck equation.
	 * @param temp in Kelvin
	 * @param wavelength is in nm*10
	 * @return radiance in W / sq. M / sr / micron
	 */
	public static double radiance(double temp, double wavelength) {
		double c1 = 1.191043888527342E-16;
        double c2 = 1.438768612341234E-02;
        // wavelength in microns to meters
        double w = wavelength*Math.pow(10.0, -6.0);
        // radiance in watts per square meter per steriadian per meter
        double r = (c1 / (w*w*w*w*w)) / (Math.exp(c2/(w*temp)) - 1.0);
        return r*Math.pow(10.0, -6.0);                              
	}
	
	/**
	 * Compute band weighted radiance from a temperature and SRF.
	 * See King et al. 1996
	 * 
	 * @param temp in Kelvin
	 * @param srf is the SRF to use
	 * @return
	 */
	public static double radiance(double temp, double[][] srf) {
		// array of all radiances over the spectral response
		double[] radiances = new double[srf[0].length];
		for (int i=0; i<srf[0].length; i++) {
			// assume nanometers, convert for the radiance() method
			radiances[i] = radiance(temp, srf[0][i]*10);
		}
		// convolve
		double wRadiance = convolve3(srf, new double[][] {srf[0], radiances});
		return wRadiance;
	}
	
	/**
	 * Inverse Planck equation from newmas.
	 * @param radiance in Watts per square meter per steradian per micron
	 * @param wavelength is microns*10000=nanometers*10, as stored digitally
	 * @return temp K
	 */
	public static double temp(double radiance, double wavelength) throws Exception {
		double c1 = 1.191043888527342E-16;
        double c2 = 1.438768612341234E-02;
        double        w, r, t;
/*       check for illegal value of radiance */
        if (radiance <= 0.0) { throw new Exception("Negative Radiance in Utils.temp()."); }          /* flag value */
/*       wavelength in 10s of nanometers to microns to meters */
        w = wavelength*(1.0E-6)*0.0001;
/*       Convert radiance units to watts per square meter per steradian per  */
        r = radiance * 1.0E6;                                /*       micron*/
/*       Compute temperature */
        t = (c2 / w) / Math.log (c1 / (r * (w*w*w*w*w)) + 1.0);
/*       return temperature (in degrees K) */
        return t;

	}
	
	/**
	 * Vector implementation of temp(double radiance, double wavelength)
	 * @param radiance
	 * @param wavelength in nm*10
	 * @return a vector of temps in K
	 */
	public static ArrayRealVector tempVector(RealVector radiance, double wavelength) {
		ArrayRealVector temps = new ArrayRealVector(radiance.getDimension());
		for (int i=0; i<temps.getDimension(); i++) {
			try {
				temps.setEntry(i, temp(radiance.getEntry(i), wavelength));
			} catch (MatrixIndexException e) {
				e.printStackTrace();
			} catch (Exception e) {
				//System.err.println("WARNING: Negative radiance.");
				temps.setEntry(i, -9999.0);
			}
		}
		return temps;
	}
	
	
	/**
	 * Summary Statistics for a whole matrix
	 * @param data is the matrix of data
	 * @return a SummaryStatistics
	 */
	public static SummaryStatistics stats(RealMatrix data) {
		SummaryStatistics sumstats = new SummaryStatistics();
		for (int i=0; i<data.getRowDimension(); i++) {
			for (int j=0; j<data.getColumnDimension(); j++) {
				sumstats.addValue(data.getEntry(i, j));
			}
		}
		return sumstats;
	}
	
	/**
	 * Summary Statistics for a whole vector
	 * @param data is the matrix of data
	 * @return a SummaryStatistics
	 */
	public static SummaryStatistics stats(RealVector data) {
		SummaryStatistics sumstats = new SummaryStatistics();
		for (int i=0; i<data.getDimension(); i++) {
			sumstats.addValue(data.getEntry(i));
		}
		return sumstats;
	}
	
	
	/**
	 * Compute column means of a matrix
	 * @param data is the input matrix
	 * @return a vector of column means
	 */
	public static RealVector cMeans(RealMatrix data) {
		int columns = data.getColumnDimension();
		ArrayRealVector means = new ArrayRealVector(columns);
		for (int i=0; i<columns; i++) {
			double mean = stats(data.getColumnVector(i)).getMean();
			means.setEntry(i, mean);
		}
		return means;
	}
	
	/**
	 * Compute column standard deviations from a matrix
	 * @param data is the input matrix
	 * @return a vector of column SDs
	 */
	public static RealVector cSDs(RealMatrix data) {
		int columns = data.getColumnDimension();
		ArrayRealVector sds = new ArrayRealVector(columns);
		for (int i=0; i<columns; i++) {
			double sd = stats(data.getColumnVector(i)).getStandardDeviation();
			sds.setEntry(i, sd);
		}
		return sds;
	}
	
	/**
	 * Print the current time.
	 */
	public static void time() {
		Calendar c = Calendar.getInstance();
		System.out.println(c.getTime());
	}
	
	/**
	 * Recursive binary search.  Finds the indices that bracket the provided value.
	 * If an exact match is found, that index is returned in both spots.
	 * Returns negative indices if not found.  Sorted X, ascending or descending.
	 * See also java.util.Arrays.
	 * 
	 * @param input is an array in which to search
	 * @param bounds is a length 2 int array of indices between which to search
	 * @param x is the value to find
	 * @return the indices of the elements that bracket x.  The indices are identical if an exact match is found.  {-1,-2} means not found. 
	 */
	public static int[] search(double[] input, int[] bounds, double x) {
		// called on same index
		if (bounds[0] == bounds[1]) { return new int[] {bounds[0], bounds[1]}; }
		// in between the two indices
		if (bounds[1]-bounds[0] == 1) { return new int[] {bounds[0], bounds[1]}; }
		// exact match to one of the indices
		if (input[bounds[0]] == x) { return new int[] {bounds[0], bounds[0]}; }
		if (input[bounds[1]] == x) { return new int[] {bounds[1], bounds[1]}; }
		// otherwise, keep going
		int mid = (bounds[0]+bounds[1])/2;
		if ( (x >= input[mid] && x < input[bounds[1]]) || (x <= input[mid] && x > input[bounds[1]]) ) {
			return search(input, new int[] {mid, bounds[1]}, x);
		}
		if ( (x <= input[mid] && x > input[bounds[0]]) || (x >= input[mid] && x < input[bounds[0]]) ) {
			return search(input, new int[] {bounds[0], mid}, x);
		}
		// error condition
		return new int[] {-1, -2};
	}
	
	/**
	 * Reverse the order of elements.  Assumes input is rectangular.
	 * @param input is an array of arrays to be reversed
	 */
	public static double[][] reverse(double[][] input) {
		double[][] output = new double[input.length][input[0].length];
		for (int j=0; j<input.length; j++) {
			for (int i=0; i<input[0].length; i++) {
				output[j][output[0].length-i-1] = input[j][i];
			}
		}
		return output;
	}
	
	/**
	 * Weitghed average two arrays with different x and y.  Assumes that the range of
	 * x overlaps at one or more points.  Use range of array1.  Assumes sorted by x.
	 * Uses linear interpolation.
	 * 
	 * @param array1 is a numeric array with first row x and second row y
	 * @param array2 is a numeric array with first row x and second row y
	 * @return the value of array2 weighted by array1
	 * @see com.berkenviro.imageprocessing.ArrayFunction#ArrayFunction(double[][])
	 * @see com.berkenviro.imageprocessing.DoubleFunction#DoubleFunction(UnivariateRealFunction, UnivariateRealFunction)
	 */
	public static double convolve(double[][] array1, double[][] array2) {
		// make two IntegralFunctions from the arrays
		UnivariateRealFunction if1 = new ArrayFunction(array1);
		UnivariateRealFunction if2 = new ArrayFunction(array2);
		UnivariateRealFunction if3 = new DoubleFunction(if1, if2);
		// determine the limits of integration
		double max, min;
		if (array1[0][0] > array1[0][array1[0].length-1]) {
			max = array1[0][0];
			min = array1[0][array1[0].length-1];
		}
		else {
			min = array1[0][0];
			max = array1[0][array1[0].length-1];
		}
		
		SimpsonIntegrator integrator = new SimpsonIntegrator();
		double srfArea = -9999.0;
        //double radArea;
        double comArea = -9999.0;
		try {
			srfArea = integrator.integrate(if1, min, max);
			//radArea = integrator.integrate(if2, min, max);
			comArea = integrator.integrate(if3, min, max);
		} catch (MaxIterationsExceededException e) {
			e.printStackTrace();
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
        
        //System.out.println("Area under array1 = " + srfArea);
        //System.out.println("Area under array2 = " + radArea);
        //System.out.println("Area under array1*array2 = " + comArea);
        
        return comArea/srfArea;
		
	}
	
	
	/**
	 * Weighted average two arrays with different x and y.  Assumes that the range of
	 * x overlaps at one or more points.  Use range of array1.  Assumes sorted by x.
	 * Uses cubic spline interpolation.
	 * 
	 * @param array1 is a numeric array with first row x and second row y
	 * @param array2 is a numeric array with first row x and second row y
	 * @return the value of array2 weighted by array1
	 */
	public static double convolve3(double[][] array1, double[][] array2) {
		// make two IntegralFunctions from the arrays
		UnivariateRealFunction if1 = new SplineFunction(array1);
		UnivariateRealFunction if2 = new SplineFunction(array2);
		UnivariateRealFunction if3 = new DoubleFunction(if1, if2);
		// determine the limits of integration
		double max, min;
		if (array1[0][0] > array1[0][array1[0].length-1]) {
			max = array1[0][0];
			min = array1[0][array1[0].length-1];
		}
		else {
			min = array1[0][0];
			max = array1[0][array1[0].length-1];
		}
		
		SimpsonIntegrator integrator = new SimpsonIntegrator();
        double srfArea = -9999.0;
        double radArea;
        double comArea = -9999.0;
		try {
			srfArea = integrator.integrate(if1, min, max);
			radArea = integrator.integrate(if2, min, max);
			comArea = integrator.integrate(if3, min, max);
		} catch (MaxIterationsExceededException e) {
			e.printStackTrace();
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
        
        
        //System.out.println("Area under array1 = " + srfArea);
        //System.out.println("Area under array2 = " + radArea);
        //System.out.println("Area under array1*array2 = " + comArea);
        
        return comArea/srfArea;
		
	}
	
	
	
	/**
	 * Computes the angle between the incident and look vectors based on 
	 * sun-target-sensor geometry.  TODO: convert to spherical?
	 * @param sun_z is sun-zenith angle in radians
	 * @param sunaz sun azimuth in radians
	 * @param sens_z is sensor-zenith angle in radians
	 * @param sensaz sensor azimuth in radians
	 */
	public static double rfunction(double sun_z, double sunaz, double sens_z, double sensaz) {
		double sunx = Math.cos(sunaz)*Math.sin(sun_z);
		double suny = Math.sin(sunaz)*Math.sin(sun_z);
		double sunz = Math.cos(sun_z);
		
		double sensx = Math.cos(sensaz)*Math.sin(sens_z);
		double sensy = Math.sin(sensaz)*Math.sin(sens_z);
		double sensz = Math.cos(sens_z);
		
		double dot = sunx*sensx + suny*sensy + sunz*sensz;
		double lsens = Math.sqrt(Math.pow(sensx,2.0)+Math.pow(sensy,2.0)+Math.pow(sensz,2.0));
		double lsun = Math.sqrt(Math.pow(sunx,2.0)+Math.pow(suny,2.0)+Math.pow(sunz,2.0));
		return Math.acos(dot/(lsens*lsun));
		
	}
	
	
	/**
	 * Generic function to read a text file into a double[][] with an arbitrary number of columns.
	 * 
	 * @param file is the full path of the text file to read.
	 * @param header is the number of lines of header to skip
	 * @return double[][] {column_1, ..., column_N}
	 */
	public static double[][] readFile(File file, int header) {
		// initialize output
		double[][] output = null;
		// read the files
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			// skip the header
			for (int i=1; i<=header; i++) {
				reader.readLine();
			}
			// first line, set up number of columns
			String line = reader.readLine();
			String[] tokens = tokenize(line);
			int columns = tokens.length;
			// initialize
			ArrayList[] listArr = new ArrayList[columns];
			for (int a=0; a<columns; a++) {
				listArr[a] = new ArrayList();
			}
			// get strings from first line
			for (int s=0; s<columns; s++) {
				listArr[s].add(tokens[s]);
			}
			// read the rest of the file, adding token strings to the lists
			while ((line = reader.readLine()) != null) {
				tokens = tokenize(line);
				for (int s=0; s<columns; s++) {
					listArr[s].add(tokens[s]);
				}
			}
			// arraylists full of strings, read to arrays
			output = new double [columns][listArr[0].size()];
			for (int i=0; i<listArr[0].size(); i++) {
				for (int a=0; a<columns; a++) {
					
					output[a][i] = Double.parseDouble((String)listArr[a].get(i));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return output;	
	}
	
	
	/**
	 * Tokenizing helper.  Works on space, tab and comma separated files.
	 * 
	 * @param input is a line to be tokenized
	 * @return a String[] of the strings in between the delimiters
	 */
	public static String[] tokenize(String input) {
		if (input.contains(",")) {  // comma delimited
			String[] ret = input.split(",");
			// trim leading whitespace
			for (String s : ret) {
				s.trim();
			}
			return ret;
		}
		else if (input.contains("\\t")) {  // tab delimited
			return input.split("\\t+");
		}
		else {  // whitespace delimited
			return input.trim().split("\\s+");
		}
	}
	
	/**
	 * Write a double[][] with an arbitrary number of columns to a file.
	 * @param toWrite is the double[][] to write
	 * @param outName is the full output path
	 * @return true if successfully completed.
	 */
	public static boolean writeFile(double[][] toWrite, String outName) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(outName)));
			for (int l=0; l<toWrite[0].length; l++) {
				String line = "";
				for (int c=0; c<toWrite.length; c++) {
					if (c == toWrite.length-1) {
						line += toWrite[c][l];
					}
					else {
						line+= toWrite[c][l] +",";
					}
				}
				writer.write(line);
				writer.newLine();
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	
	/**
	 * Write an int[][] with an arbitrary number of columns to a file.
	 * @param toWrite is an int[][] to write
	 * @param outName is the full output path
	 * @return true if successfully completed.
	 */
	public static boolean writeFile(int[][] toWrite, String outName) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(outName)));
			for (int l=0; l<toWrite[0].length; l++) {
				String line = "";
				for (int c=0; c<toWrite.length; c++) {
					if (c == toWrite.length-1) {
						line += toWrite[c][l];
					}
					else {
						line+= toWrite[c][l] +",";
					}
				}
				writer.write(line);
				writer.newLine();
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	
	/**
	 * Create a directory.
	 * @param containerDir is the parent directory in which to make a new directory
	 * @param name is the name of the directory
	 * @return true if successfully completed
	 */
	public static boolean makedir(String containerDir, String name) {
		return (new File(containerDir+"/"+name)).mkdir();
	}
	
	
	/**
	 * Make a histogram of bit number from a given sample of ints.
	 * @param data is the int sample
	 * @return an int[32] array in which each value is the number of times that bit was present
	 */
	public static int[] bitHistogram(int[] data) {
		int[] histogram = new int[32];
		for (int i=0; i<data.length; i++) {
			for (int b=0; b<32; b++) {
				histogram[b] += (1 & (data[i] >>> b));
			}
		}
		return histogram;
	}
	
	
	/**
	 * Test code and processing log.
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		for (int m=1; m<=12; m++) {
			for (int d=1; d<=28; d++) {
				System.out.println(esDistance(2011, m, d));
			}
		}
		*/ //OK
		
		
	}

	
}
