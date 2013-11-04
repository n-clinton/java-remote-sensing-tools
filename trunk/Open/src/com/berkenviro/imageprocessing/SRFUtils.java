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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math.analysis.solvers.BisectionSolver;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.RealVector;

import com.berkenviro.imageprocessing.GaussFunction;
import com.berkenviro.imageprocessing.OLSregression;
import com.berkenviro.imageprocessing.SRFfunction;

/**
 * Utility class containing static methods for dealing with spectral response functions (SRFs).
 * Contains various routines for characterizing and fitting SRFs.
 * 
 * @author Nicholas Clinton
 */
public class SRFUtils {
	
	/**
	 * Fit a Gaussian to the specified noisy SRF using non-linear least squares.
	 * Reference: http://mathworld.wolfram.com/NonlinearLeastSquaresFitting.html
	 * 
	 * @param srf is the input
	 * @return a Gaussian fitted to the SRF.
	 */
	public static double[][] fitGauss (double[][] srf) {
		// initial conditions, standard normal density
		double a0 = 1.0/Math.sqrt(2*Math.PI);
		double mu0 = getPeak(srf);
		double sigma0 = getFWHM(srf);
		// intialize
		RealVector params = new ArrayRealVector(new double[] {a0, mu0, sigma0});
		ArrayRealVector fittedVals = new ArrayRealVector(srf[1].length);
		ArrayRealVector residuals = new ArrayRealVector(srf[1].length);
		Array2DRowRealMatrix jacobian = new Array2DRowRealMatrix(srf[1].length, 3);
		RealVector delta = new ArrayRealVector(new double[] {1, 1, 1});
		GaussFunction gFunc = new GaussFunction();
		OLSregression ols = new OLSregression();
		// now, iterate until the delta settles down
		while (delta.dotProduct(delta) > 0.001) {
			System.out.println("parameters: ["+params.getEntry(0)+", "+params.getEntry(1)+", "+params.getEntry(2)+"]");
			for (int i=0; i<srf[1].length; i++) {
				try {
					// compute Gaussian values
					fittedVals.setEntry(i, gFunc.value(srf[0][i], params.getData()));
					// compute errors
					residuals.setEntry(i, srf[1][i]-fittedVals.getEntry(i));
					// gradient w/respect to parameters
					jacobian.setRow(i, gFunc.gradient(srf[0][i], params.getData()));
					//System.out.println(jacobian.getRowVector(i));
				} catch (FunctionEvaluationException e) {
					e.printStackTrace();
				}
			}
			// delta from pseudo-inverse
			ols.newSampleData(residuals.getData(), jacobian.getData());
			delta = ols.calculateBeta();
			//System.out.println("Delta: ["+delta.getEntry(0)+", "+delta.getEntry(1)+", "+delta.getEntry(2)+"]");
			params = params.add(delta.ebeDivide(new double[] {2, 2, 2}));
			// make variance non-negative
			if (params.getEntry(2) < 0) {
				params.setEntry(2, Math.abs(params.getEntry(2)));
			}
		}
	
	
		return new double[][] {srf[0], fittedVals.getData()};
	}

	/**
	 * Return a Gaussian SRF of the specified position and width.
	 * 
	 * @param center is the mean of the Gaussian
	 * @param fwhm is the FWHM of the Gaussian
	 * @return a pure Gaussian SRF with the specified mean and FWHM
	 */
	public static double[][] getGaussian(double center, double fwhm) {
		// get a meaan zero Gaussian with the specified FWHM
		double[][] srf = (new GaussFunction()).getGauss(fwhm);
		// shift
		for (int i=0; i<srf[1].length; i++) {
			srf[0][i] = srf[0][i] + center;
		}
		return srf;
	}
	
	/**
	 * Get a list of Gaussian srfs of the specified means and FWHMs.  
	 * @param centers is an array of center wavlengths (means)
	 * @param fwhms is an array of FWHMs for the Gaussians
	 * @return a List of Gaussian SRFs
	 */
	public static List<double[][]> getGaussians(double[] centers, double[] fwhms) {
		ArrayList srfs = new ArrayList(centers.length);
		for (int i=0; i<centers.length; i++) {
			srfs.add(i, getGaussian(centers[i], fwhms[i]));
		}
		return srfs;
	}
	
	
	/**
	 * Compute the power point equidistant from the 50% power points.  Contrast to moment method.
	 * @param srf is the input SRF
	 * @return the center power point.
	 */
	public static double getCenter(double[][] srf) {
		double[] fifties = getFifties(srf);
		return (fifties[0] + fifties[1]) / 2.0;
	}
	
	/**
	 * Compute the upper and lower 50% power points.
	 * This function operates by finding the roots of an SRF shifted down by 0.5.
	 * 
	 * @param srf is the input SRF
	 * @return an array of {lower 50% wavelength, upper 50% wavelength}
	 */
	public static double[] getFifties(double[][] srf) {
		// shift the SRF such that the 50% power points lie on the X-axis
		UnivariateRealFunction spline = new SRFfunction(srf, 0.5);
		// find the wavelength boundaries of the function
		double lower = Utils.stats(new ArrayRealVector(srf[0])).getMin();
		double upper = Utils.stats(new ArrayRealVector(srf[0])).getMax();
		// middle wavelength
		double mid = getPeak(srf);
		BisectionSolver solver = new BisectionSolver();
		double left = -99.0;
		double right = -99.0;
		// find the roots in the left and right sides of the curve
		try {
			left = solver.solve(spline, lower, mid);
			right = solver.solve(spline, mid, upper);
		} catch (MaxIterationsExceededException e) {
			e.printStackTrace();
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		}
		return new double[] {left, right};
	}

	/**
	 * Palmer method.  Return the mean of the SRF.  
	 * @param srf is the input array
	 * @return the mean of the SRF
	 */
	public static double getFirstMoment(double[][] srf) {
		UnivariateRealFunction if1 = new MomentFunction(srf, MomentFunction.FIRST_MOMENT);
		UnivariateRealFunction if2 = new ArrayFunction(srf);
		// determine the limits of integration
		double max, min;
		if (srf[0][0] > srf[0][srf[0].length-1]) {
			max = srf[0][0];
			min = srf[0][srf[0].length-1];
		}
		else {
			min = srf[0][0];
			max = srf[0][srf[0].length-1];
		}
		
		SimpsonIntegrator integrator = new SimpsonIntegrator();
		double srfArea = -9999.0;
	    //double radArea;
	    double mean = -9999.0;
		try {
			mean = integrator.integrate(if1, min, max);
			srfArea = integrator.integrate(if2, min, max);
		} catch (MaxIterationsExceededException e) {
			e.printStackTrace();
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	    
	    return mean/srfArea;
	}

	/**
	 * Get the FWHM using the 50% power points.
	 * @param srf is the input array
	 * @return the FWHM in wavlength units
	 */
	public static double getFWHM(double[][] srf) {
		double[] fifties = getFifties(srf);
		return (fifties[1] - fifties[0]);
	}

	/**
	 * Get the peak wavelength of a normalized SRF.
	 * @param srf is the normalized input array
	 * @return the wavelength where the SRF==1
	 */
	public static double getPeak(double[][] srf) {
		for (int i=0; i<srf[1].length; i++) {
			if (srf[1][i] == 1.0) { 
				return srf[0][i]; 
			}
		}
		// error condition
		return -99.0;
	}

	/**
	 * Band weight the provided spectrum with the provided srfs.
	 * Wavelength units must be the same.
	 * @param srfs is a list of SRFs to compute
	 * @param spectrum is the continuous spectrum data to sample with the SRFs
	 * @return the in-band values of spectrum
	 */
	public static double[] getInBand(List<double[][]> srfs, double[][] spectrum) {
		double[] inBand = new double[srfs.size()];
		for (int b=0; b<srfs.size(); b++) {
			double[][] srf = srfs.get(b);
			inBand[b] = Utils.convolve(srf, spectrum);
		}
		return inBand;
	}
	
	
	/**
	 * Get the means of the SRFs in the input list
	 * @param srfs is a list of SRFs as double[][]
	 * @return the array of mean.
	 */
	public static double[] getMeans(List<double[][]> srfs) {
		double[] means = new double[srfs.size()];
		for (int b=0; b<srfs.size(); b++) {
			double[][] srf = srfs.get(b);
			means[b] = getFirstMoment(srf);
		}
		return means;
	}

	/**
	 * Read a lot of SRFs at once, returning them all in a List.  This method is useful for reading
	 * multiple files from disk, where the files all have the band index in the file name and the same base name.
	 * The SRFs read will be in units of nanometers.
	 * 
	 * @param inRoot is the base of the filename.  All files should be inRoot*** where *** is a band index
	 * @param band1 is the first band (indexed as the filenames are) to process.
 	 * @param bandN is the last band (indexed as the filenames are) to process
	 * @param header is the number of header lines to skip in each file
	 * @param microns is a boolean flag to indicate that the wavelength units are microns
	 * @return a List of SRFs read.
	 */
	public static List<double[][]> readSRFs(String inRoot, int band1, int bandN, int header, boolean microns) {
		ArrayList<double[][]> srfs = new ArrayList<double[][]>(bandN-band1+1);
		// zero indexing everywhere except filenames
		for (int s=band1; s<=bandN; s++) {
			// assume single digit bands are prefixed by zero in the filename
			String num = (s<10) ? num = "0"+s : String.valueOf(s);
			double[][] srf = Utils.readFile(new File(inRoot+num), header);
			if (microns) {  // scale the column of wavelengths
				for (int i=0; i<srf[0].length; i++) { srf[0][i] = srf[0][i]*1000.0; }
			}
			// zero-indexed list
			srfs.add(s-band1, srf);
			System.out.println("Read srf: "+inRoot+num);
		}
		return srfs;
	}
	
	/**
	 * Test code and processing log.
	 * @param args
	 */
	public static void main(String[] args) {
		// Landsat 8 in-band exo-atmospheric irradiance
		String irrad = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/SolarIrradiance.txt";
		double[][] exo = Utils.readFile(new File(irrad), 2);
		List<double[][]> srfs = readSRFs("/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/l8srf",
				1, 8, 1, false);
		int band = 1;
		for (double[][] srf : srfs) {
			System.out.println("band "+band+": "+SRFUtils.getFirstMoment(srf)+" : "+Utils.convolve3(srf, exo));
			band++;
		}
		
	}

}
