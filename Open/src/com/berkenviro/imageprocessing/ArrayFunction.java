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

import org.apache.commons.math.analysis.UnivariateRealFunction;

/**
 * A class to treat an array as a function using linear interpolation between data points.
 * 
 * @author Nick Clinton
 */
public class ArrayFunction implements UnivariateRealFunction {

	private double[] myX;
	private double[] myY;
	
	/**
	 * Constructor.  Assumes x is ORDERED and y is POSITIVE.
	 * @param array in which array[0] is x and array[1] is y
	 */
	public ArrayFunction(double[][] array) {
		myX = array[0];
		myY = array[1];
	}
	
	/**
	 * Linear approximator.  
	 * @see org.apache.commons.math.analysis.UnivariateRealFunction#value(double)
	 * @param x is the value at which to compute y
	 * @return the value at x
	 */
	@Override
	public double value(double x) {
		int max = (myX[0]<myX[myX.length-1]) ? myX.length-1 : 0;
		int min = (myX[0]>myX[myX.length-1]) ? myX.length-1 : 0;
		// if out of bounds, return zero mass
		if (x > myX[max] || x < myX[min]) { return 0.0; }
		// search
		int[] indices = Utils.search(myX, new int[] {0, myX.length-1}, x);
        // if an exact match, return it
		if (indices[0] == indices[1]) { return myY[indices[0]]; }
		// otherwise, do a linear interpolation
		double m = (myY[indices[1]] - myY[indices[0]]) / (myX[indices[1]] - myX[indices[0]]);
		return m * (x - myX[indices[0]]) + myY[indices[0]];
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// test code
		
	}

}
