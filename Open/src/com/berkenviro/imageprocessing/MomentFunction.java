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

/**
 * A utility class that computes first and second moments of an ArrayFunction.
 * 
 * @author Nicholas Clinton
 */
public class MomentFunction extends ArrayFunction {

	public static final int FIRST_MOMENT = 1;
	public static final int SECOND_MOMENT = 2;
	// should be one of the above
	private int moment;
	
	/**
	 * Constructor builds an ArrayFunction.
	 * @param array is the {x,y} array of function observations
	 * @param moment should be a momentFunction.FIRST_MOMENT or momentFunction.SECOND_MOMENT
	 */
	public MomentFunction(double[][] array, int moment) {
		super(array);
		this.moment = moment;
	}

	/**
	 * Linear approximator.  Assumes x is ORDERED and y is POSITIVE
	 * @see org.apache.commons.math.analysis.UnivariateRealFunction#value(double)
	 * @param x is the value at which to compute y
	 * @return the value at x
	 */
	@Override
	public double value(double x) {
		double val = super.value(x);
		if (moment == FIRST_MOMENT) {
			return x*val;
		} else {
			return Math.pow(x, 2)*val;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
