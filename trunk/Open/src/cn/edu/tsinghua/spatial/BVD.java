/**
 * 
 */
package cn.edu.tsinghua.spatial;

/**
 * BIAS-VARIANCE DECOMPOSITION FOR ZERO-ONE LOSS

	Ported to Java 20130501 by Nicholas Clinton

	Original Description (Pedro Domingos):
	April 18, 2000
	The C functions in this file implement the bias-variance decomposition for
	zero-one loss described in the paper "A Unified Bias-Variance Decomposition."
	Zero noise is assumed. biasvar() computes the average zero-one loss, bias, net
	variance, and components of the variance on a test set of examples. biasvarx()
	computes the loss, bias and variance on an individual example.

	Copyright (C) 2000 Pedro Domingos

	This code is free software; you can redistribute it and/or modify it under
	the terms of the GNU Lesser General Public License as published by the Free
	Software Foundation. See http://www.gnu.org/copyleft/lesser.html.

	This code is distributed in the hope that it will be useful, but WITHOUT ANY
	WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
	A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
	details.
 */
public class BVD {

	static int MaxTestExs = 10000;
	static int MaxTrSets = 100;
	static int MaxClasses = 100;	

	/**
	 * 
	 * @param classes is a vector containing the actual classes of the test examples,
	          represented as integers; classes[i] is the class of test example i
	 * @param preds is an array where preds[i][j] is the class predicted for example i
	          by the classifier learned on training set j; 
	          classes are represented as integers, consistent with those used in the classes vector
	 * @param ntestexs is the number of test examples used
	 * @param ntrsets is the number of training sets used
	 * @return {loss, bias, var, varp, varn, varc}
	 */
	public double[] biasvar(int[] classes, int[][] preds, int ntestexs, int ntrsets) {

		double loss = 0.0; // average loss
		double bias = 0.0; // average bias
		double varp = 0.0; // average contribution to variance from unbiased examples
		double varn = 0.0; // average contribution to variance from biased examples
		
		/* Average contribution to variance from biased examples:
		 * the variance from each example weighted by the probability that 
		 * the class predicted for the example is the optimal prediction, 
		 * given that it is not the main prediction. In multiclass domains,
		 * net variance equals (varp - varc), not (varp - varn). 
        */
		double varc = 0.0;
		for (int e = 0; e < ntestexs; e++) {
			double[] bvl = biasvarx(classes[e], preds[e], ntrsets);
			double biasx = bvl[0];
			double varx = bvl[1];
			double lossx = bvl[2];
			loss += lossx;
			bias += biasx;
			if (biasx != 0.0) {
				varn += varx;
				varc += 1.0;
				varc -= lossx;
			} else {
				varp += varx;
			}
		}
		loss /= ntestexs;
		bias /= ntestexs;
		double var = loss - bias; // net variance
		varp /= ntestexs;
		varn /= ntestexs;
		varc /= ntestexs;
		return new double[] {loss, bias, var, varp, varn, varc};
	}


	/**
	 * 
	 * @param classx is the actual class of the example, represented as an integer
	 * @param predsx is a vector where predsx[j] is the class predicted for the
	          example by the classifier learned on training set j; 
	          classes are represented as integers, consistent with classx
	 * @param ntrsets the number of training sets used
	 * @return
	 */
	public double[] biasvarx(int classx, int[] predsx, int ntrsets) {
		int[] nclass = new int[MaxClasses];
		int majclass = -1;
		int nmax = 0;

		for (int c = 0; c < MaxClasses; c++) {
			nclass[c] = 0;
		}
		for (int t = 0; t < ntrsets; t++) {
			nclass[predsx[t]]++;
		}
		for (int c = 0; c < MaxClasses; c++) {
			if (nclass[c] > nmax) {
				majclass = c;
				nmax = nclass[c];
			}
		}
		// the loss on the example
		double lossx = 1.0 - (double)nclass[classx] / ntrsets;
		// the bias on the example
		double biasx = (majclass != classx) ? 1 : 0;
		// the variance on the example
		double varx = 1.0 - (double)nclass[majclass] / ntrsets;
		return new double[] {biasx, varx, lossx};
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
