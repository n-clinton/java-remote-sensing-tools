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

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.DifferentiableMultivariateVectorialFunction;
import org.apache.commons.math.analysis.MultivariateMatrixFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.optimization.fitting.ParametricRealFunction;

/**
 * Implementation of a Gaussian function as a ParametricRealFunction.
 * This allows for minimization and other optimizations to be performed.
 * 
 * @author Nicholas Clinton
 */
public class GaussFunction implements ParametricRealFunction {
	
	/**
	 * No-arg constructor.
	 */
	public GaussFunction() {
		// do nothing
	}
	
	/**
	 * Reference: http://mathworld.wolfram.com/NonlinearLeastSquaresFitting.html
	 * @see org.apache.commons.math.optimization.fitting.ParametricRealFunction#gradient(double, double[])
	 * @param paramaters is {a, mu, sigma}
	 */
	@Override
	public double[] gradient(double x, double[] parameters)
			throws FunctionEvaluationException {
		double a = parameters[0];
		double mu = parameters[1];
		double sigma = parameters[2];
		
		double dFda = value(x, new double[] {1.0, mu, sigma});
		double dFdmu = (a*(x-mu)/Math.pow(sigma, 2))*value(x, new double[] {1.0, mu, sigma});
		double dFdsigma = (a*Math.pow((x-mu), 2)/Math.pow(sigma, 3))*value(x, new double[] {1.0, mu, sigma});
		
		return new double[] {dFda, dFdmu, dFdsigma};
	}

	/**
	 * @see org.apache.commons.math.optimization.fitting.ParametricRealFunction#value(double, double[])
	 * @param paramaters is {a, mu, sigma}
	 */
	@Override
	public double value(double x, double[] parameters)
			throws FunctionEvaluationException {
		double a = parameters[0];
		double mu = parameters[1];
		double sigma = parameters[2];
		double z = ((x-mu)/sigma);
		return a*Math.exp((-1.0/2.0)*Math.pow(z, 2));
	}

	/**
	 * Get a Gaussian with the given FWHM.
	 * See http://mathworld.wolfram.com/GaussianFunction.html
	 * @param fwhm is the full width half max in wavelength units
	 * @return an array of {x,y}, with peak of one, centered at zero
	 */
	public double[][] getGauss(double fwhm) {
		double[][] xy = new double[2][101];
		double sigma = fwhm/(2.0*Math.sqrt(2.0*Math.log(2.0)));
		// mean zero, scaled to one
		double[] params = {1.0, 0.0, sigma};
		for (int i=-50; i<=50; i++) {
			try {
				// go out 3 SD's on either side
				double x = (i/50.0)*3.0*sigma;
				double y = value(x, params);
				xy[0][i+50] = x;
				xy[1][i+50] = y;
			} catch (FunctionEvaluationException e) {
				e.printStackTrace();
			}
		}
		return xy;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
	}



}
