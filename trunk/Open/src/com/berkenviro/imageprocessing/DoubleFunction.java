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
import org.apache.commons.math.analysis.UnivariateRealFunction;

/**
 * Return the product of two UnivariateRealFunction arguments.
 * 
 * @author Nick Clinton
 */
public class DoubleFunction implements UnivariateRealFunction {

	private UnivariateRealFunction af;
	private UnivariateRealFunction bf;
	
	/**
	 * Returns the product of two IntegralFunctions
	 * @param a is a org.apache.commons.math.analysis.UnivariateRealFunction
	 * @param b is a org.apache.commons.math.analysis.UnivariateRealFunction
	 */
	public DoubleFunction (UnivariateRealFunction a, UnivariateRealFunction b) {
		af = a;
		bf = b;
	}
	
	/**
	 * @see org.apache.commons.math.analysis.UnivariateRealFunction#value(double)
	 * @param x is the value at which to compute y
	 * @return the value at x
	 */
	@Override
	public double value(double x) {
		double returnVal = -9999.0;
		try {
			returnVal = af.value(x)*bf.value(x);
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		}
		return returnVal;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO test code

	}

}
