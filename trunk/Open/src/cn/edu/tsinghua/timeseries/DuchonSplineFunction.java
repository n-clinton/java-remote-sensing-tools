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
package cn.edu.tsinghua.timeseries;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.DifferentiableUnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math.analysis.polynomials.PolynomialSplineFunction;

import ru.sscc.spline.Spline;
import ru.sscc.spline.analytic.GSplineCreator;
import ru.sscc.util.CalculatingException;
import ru.sscc.util.data.DoubleVectors;
import ru.sscc.util.data.RealVectors;

/**
 * A utility class for using a DuchonSpline as if it were a function.
 * Can be integrated with this wrapper.
 * @author Nicholas Clinton
 */
public class DuchonSplineFunction extends SplineInterpolator implements
		UnivariateRealFunction {

	private Spline spline;
	
	/**
	 * Constructor initializes a spline on the array
	 * @param array is {x, y}
	 */
	public DuchonSplineFunction(double[][] array) {
		spline = TSUtils.duchonSpline(array[0], array[1]);
	}
	
	/**
	 * @param x is the point at which to evaluate the spline function
	 * @return interpolated y at x
	 * @see org.apache.commons.math.analysis.UnivariateRealFunction#value(double)
	 */
	@Override
	public double value(double x) throws FunctionEvaluationException {
		return spline.value(x);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
