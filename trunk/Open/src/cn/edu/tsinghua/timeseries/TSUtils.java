/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import mr.go.sgfilter.SGFilter;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math.complex.Complex;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.moment.VectorialCovariance;
import org.apache.commons.math.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.DftNormalization;

import JSci.awt.DefaultGraph2DModel;
import JSci.awt.Graph2D;
import JSci.awt.DefaultGraph2DModel.DataSeries;
import JSci.swing.JLineGraph;
import cn.edu.tsinghua.gui.TSDisplayer;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.ArrayFunction;
import com.berkenviro.imageprocessing.SplineFunction;
import com.berkenviro.imageprocessing.Utils;

import flanagan.complex.ComplexMatrix;
import ru.sscc.spline.Spline;
import ru.sscc.spline.analytic.GSplineCreator;
import ru.sscc.spline.polynomial.POddSplineCreator;
import ru.sscc.spline.polynomial.PSpline;
import ru.sscc.util.CalculatingException;
import ru.sscc.util.data.DoubleVectors;
import ru.sscc.util.data.RealVectors;

/**
 * @author Nicholas Clinton
 * Utility class for getting time series from images, fitting curves, etc.
 * 20121113 All methods converted to double[][] inputs and/or generic spline inputs.
 */
public class TSUtils {
	
	/**
	 * 
	 * @param series
	 * @param width
	 * @param degree
	 * @return
	 */
	public static double[][] sgSmooth(double[][] series, int width, int degree) {
		double[] padded = new double[series[1].length + 2*width];
		for (int i=0; i<padded.length; i++) {
			if (i < width) {
				padded[i] = series[1][0];
			}
			else if(i >= series[1].length + width) {
				padded[i] = series[1][series[1].length-1];
			}
			else {
				padded[i] = series[1][i-width];
			}
		}
		SGFilter filter = new SGFilter(width, width);
		double[] sgCoeffs = SGFilter.computeSGCoefficients(width, width, degree);
		// smooth
		double[] smooth = filter.smooth(padded, sgCoeffs);
		double[] out = new double[series[1].length];
		for (int i=0; i<out.length; i++) {
			out[i] = smooth[i+width];
		}
		return new double[][] {series[0], out};
	}
	
	
	/**
	 * Smooth a series with a low-pass filter on a Fourier transform
	 * @param spline
	 * @param min
	 * @param max
	 * @param p the proportion of frequency components to REMOVE.
	 * @return
	 */
	public static double[][] smoothFunction(UnivariateRealFunction spline, double min, double max, double p) {
		double[] smooth = null;
		int n = 1024;
		FastFourierTransformer fft = new FastFourierTransformer();
		try {
			// transform
			Complex[] transform = fft.transform(spline, min, max, n);
			// symmetric series, keep the zero-frequencies on the ends
			int keep = (int)(n*(1.0-p))/2;
			//System.out.println(keep);
			for (int c=0; c<transform.length; c++) {
				if (c>keep-1 && c<n-keep) {
					// blast the high frequency components
					transform[c] = Complex.ZERO;
				}
			}
			// invert
			Complex[] smoothed = fft.inversetransform(transform);
			// get the real part
			smooth = new double[smoothed.length];
			for (int i=0; i<smooth.length; i++) {
				smooth[i] = smoothed[i].getReal();
			}
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		// x values
		double s[] = new double[n];
        double h = (max - min) / n;
        for (int i = 0; i < n; i++) {
            s[i] = min + i * h;
        }
		return new double[][] {s, smooth};
	}
	
	
	public static ComplexMatrix ndftMatrix(double[] times) {
		ComplexMatrix mult = new ComplexMatrix(times.length, times.length);
		double period = times[times.length-1] - times[0]; // T
		flanagan.complex.Complex common = flanagan.complex.Complex.minusJay().times(2.0*Math.PI/period);
		for (int m=0; m<times.length; m++) { // row 
			for (int t=0; t<times.length; t++) { // column
				mult.setElement(m, t, common.times(m*times[t]).exp());
			}
		}
		return mult;
	}
	
	public ComplexMatrix ndftForward(double[][] series) {
		return ndftMatrix(series[0]).times(new double[][] {series[1]}); // column vector?
	}
	
	public ComplexMatrix ndftReverse(ComplexMatrix mult, ComplexMatrix frequencies) {
		return mult.inverse().times(frequencies);
	}
	
	public double[] realPart(ComplexMatrix matrix) {
		double[] reals = new double[matrix.getNrow()];
		for (int i=0; i<matrix.getNrow(); i++) {
			reals[i] = matrix.getArrayReference()[0][i].getReal();
		}
		return reals;
	}
	
	public double[] logPowerSpectrum(ComplexMatrix frequencies) {
		double[] power = new double[frequencies.getNrow()];
		for (int i=0; i<frequencies.getNrow(); i++) {
			power[i] = Math.log(frequencies.getArrayReference()[0][i].abs());
		}
		return power;
	}
	
	/**
	 * Read out a list into an array.  Assumes List is already sorted chronologically.
	 * @param series is a List<double[]>
	 * @return a double[2][series.size()]
	 */
	public static double[][] getSeriesAsArray(List<double[]> series) {
		double[][] xy = new double[2][series.size()];
		for (int t=0; t<series.size(); t++) {
			xy[0][t] = series.get(t)[0];
			xy[1][t] = series.get(t)[1];
		}
		return xy;
	}
	
	/**
	 * Get a thin-plate, or Duchon spline as created by JSpline.
	 * @param series as a list of double[] where each double[] is {x,y}
	 * @return the spline
	 */
	public static Spline getThinPlateSpline(List<double[]> series) {
		double[][] xy = getSeriesAsArray(series);
		return duchonSpline(xy[0], xy[1]);
	}
	
	/**
	 * Get a third-order polynomial spline as implemented by Commons Math.
	 * @param series as a list of double[] where each double[] is {x,y}
	 * @return the spline
	 */
	public SplineFunction getPolySpline(List<double[]> series) {
		return new SplineFunction(getSeriesAsArray(series));
	}
	
	/*
	 * Like gis.Utils.pixelValue(), except takes image coords and returns an
	 * array of data from each band.
	 */
	public static double[] imageValues(int pixelX, int pixelY, PlanarImage image) {
		double[] pixelVals = new double[image.getNumBands()];
		RandomIter iterator = null;
		iterator = RandomIterFactory.create(image, null);
		for (int b=0; b<image.getNumBands(); b++) {
			pixelVals[b] = iterator.getSampleDouble(pixelX,pixelY,b);
			//pixelVals[b] = iterator.getSample(pixelX,pixelY,b);
		}
		return pixelVals;
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @return a thin-plate spline as fitted by JSpline
	 */
	public static Spline duchonSpline(double[] x, double[] y) {
		// data type conversion
		RealVectors xPts = new DoubleVectors(1, x.length);
        for (int i=0; i<xPts.size; i++) {
        	xPts.set(i,0,x[i]);
        }
        
        Spline spline = null;
        try {
        	// the pseudo-quadratic RBF w/ polynomial kernel of 1st degree 
			//spline = GSplineCreator.createSpline(2, xPts, y);
        	spline = GSplineCreator.createSpline(1, xPts, y);
		} catch (CalculatingException e) {
			e.printStackTrace();
			System.out.println("length: "+x.length);
			for (int i=0; i<x.length; i++) {
				System.out.println(x[i]+","+y[i]);
			}
		}
		return spline;
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @param degree
	 * @return
	 */
	public static Spline polynomialSpline(double[] x, double[] y, int degree) {
		Spline spline = null;
		try {
			// see ru.sscc.spline.polynomial.POddSplineCreator for description
			spline = (new POddSplineCreator()).createSpline(degree, x, y);
		} catch (CalculatingException e) {
			e.printStackTrace();
		}
		return spline;
	}
	
	/**
	 * 
	 * @param spline
	 * @param x
	 * @return
	 */
	public static double[] evaluateSpline(UnivariateRealFunction spline, double[] x) throws Exception {
		double[] y = new double[x.length];
        for (int j=0; j<x.length; j++) {
        	y[j] = spline.value(x[j]);
        	if (spline instanceof Spline) {
        		y[j] = ((Spline)spline).value(x[j]);
    		}
        }
        return y;
	}
	
	/**
	 * 
	 * @param spline
	 * @param xRange
	 * @param n
	 * @return
	 * @throws Exception
	 */
	public static double[][] splineValues(UnivariateRealFunction spline, double[] xRange, int n) throws Exception {
		// generate 1000 points in the provided range
		double[][] splineVals = new double[2][n];
        double step = (xRange[1] - xRange[0])/n;
        // starting point
        double x = xRange[0];
        for (int j=0; j<n; j++) {
        	splineVals[0][j] = x;
        	splineVals[1][j] = spline.value(x);
        	System.out.print(splineVals[1][j]+",");
        	x += step;
        }
        System.out.println();
        return splineVals;
	}
	
	/**
	 * 
	 * @param spline
	 * @param xRange
	 * @return an array of 100 equally spaced points
	 * @throws Exception
	 */
	public static double[][] splineValues(UnivariateRealFunction spline, double[] xRange) throws Exception {
		return splineValues(spline, xRange, 100);
	}
	
	
	
	/**
	 * Uses the bisection method to find roots.  Input values should
	 * bracket a root, or else an endpoint will be returned.
	 * @param spline
	 * @param xRange
	 * @return
	 */
	public static Double root(UnivariateRealFunction spline, double[] xRange) throws Exception {
		double mid;
		double x1 = xRange[0];
		double x2 = xRange[1];	
		while (Math.abs(x2-x1) > 0.000000001) {
			mid = (x1 + x2)/2.0;
			
			if ( spline.value(x1)*spline.value(mid) < 0 ) {
				x2 = mid;
			} else { 
				x1 = mid;
			} 
		}
		return new Double((x1 + x2)/2.0);
	}
	

	/**
	 * Find all the maxima and minima of a Spline by finding the roots
	 * of its derivative.  Return a list of Extrema in chronological order.
	 * @param deriv
	 * @param bounds
	 * @return
	 * @throws Exception
	 */
	public static List<Extremum> getExtrema(UnivariateRealFunction deriv, double[] bounds) throws Exception {
		
		List<Extremum> extrema = new LinkedList<Extremum>();
		Extremum extremum = null;
		
		double[][] dVals = splineValues(deriv, bounds);
		// the second derivative is used to characterize the extrema
		Spline pSpline = TSUtils.polynomialSpline(dVals[0], dVals[1], 1);
        Spline secondDeriv = PSpline.derivative(pSpline);
		
        // scan over the derivative, find roots
		for (int t=1; t<dVals[0].length; t++) {
			
			Double root = null;
			// if there's a root in there, find it
			if (dVals[1][t-1]*dVals[1][t] < 0) {
				double[] range = {dVals[0][t-1], dVals[0][t]};
				root = root(deriv, range);
			}
			if (root != null) {
				// determine the type of root
				if (secondDeriv.value(root.doubleValue()) < 0) {
					extremum = new Extremum(root.doubleValue(), Extremum.EXTREMUM_TYPE_MAX);
				}
				else if (secondDeriv.value(root.doubleValue()) > 0) {
					extremum = new Extremum(root.doubleValue(), Extremum.EXTREMUM_TYPE_MIN);
				}
				extrema.add(extremum);
			}
		}
		return extrema;
	}
	
	/**
	 * TODO: Add absolute maximum and minimum?
	 * This method to evaluate the list of extrema and return a double[] that
	 * corresponds to {firstMinX, firstMinY, firstMaxX, firstMaxY, 
	 * secondMinX, secondMinY, secondMaxX, secondMaxY}
	 * @param extrema
	 * @param spline
	 * @param bounds
	 * @return
	 */
	public static double[] evaluateExtrema(List<Extremum> extrema, UnivariateRealFunction spline, double[] bounds) throws Exception {
		Extremum[] eArray = new Extremum[extrema.size()];
		Extremum e;
		// iterate over the List, write into the Array
		Iterator<Extremum> iter = extrema.iterator();
		int count = 0;
		while (iter.hasNext()) {
			e = iter.next();
			//System.out.println("Reading extremum "+count+": "+e.toString());
			eArray[count] = e;
			count++;
		}
		
		// initialize this way for debugging
		double firstMinX = -9999;
		double firstMinY = -9999;
		double firstMaxX = -9999;
		double firstMaxY = -9999;
		double secondMinX = -9999;
		double secondMinY = -9999;
		double secondMaxX = -9999;
		double secondMaxY = -9999;
		double range = -9999;
		double range1 = 0;
		double range2 = 0;

		// special cases:
		if (eArray.length == 0) {  // error condition
			System.err.println("Error: no extrema!");
		}
		else if (eArray.length == 1) {  // one extremum
			// compute the range from the endpts 
			if (eArray[0].getType() == Extremum.EXTREMUM_TYPE_MAX) {
				firstMinX = bounds[0];
				firstMaxX = eArray[0].getX();
			}
			else { // it's convex
				// just keep the initialization values
				//firstMinX = eArray[0].getX();
				//firstMaxX = bounds[1];
				System.err.println("Error: curve is convex!");
			}
		}
		else if (eArray.length == 2) { // a max and a min
			// if the first extrema is a min, return the min->max slope
			if (eArray[0].getType() == Extremum.EXTREMUM_TYPE_MIN) {
				firstMinX = eArray[0].getX();
				firstMaxX = eArray[1].getX();
			}
			// it does this: /\_/  check the ends, return the larger
			else {
				if ( (spline.value(eArray[0].getX()) - spline.value(bounds[0])) > 
					 (spline.value(bounds[1])) - spline.value(eArray[1].getX()) ) {
					firstMinX = bounds[0];
					firstMaxX = eArray[0].getX();
				}
				else {
					firstMinX = eArray[1].getX();
					firstMaxX = bounds[1];
				}
			}

		}
		else { // more than two extrema
			for (int i=0; i<eArray.length; i++) {
				double x1 = 0, x2 = 0;
				// use a minimum -> maximum combination
				if (eArray[i].getType() == Extremum.EXTREMUM_TYPE_MIN && i != eArray.length-1) {
					x1 = eArray[i].getX();
					x2 = eArray[i+1].getX();
				}
				// use the first part of the curve if a first max
				else if (eArray[i].getType() == Extremum.EXTREMUM_TYPE_MAX && i == 0) {
					//System.err.println("Warning: first extrema is a max.");
					x1 = bounds[0];
					x2 = eArray[i].getX();
				}
				// compute the range
				double y1 = (spline.value(x1) < 0) ? 0 : spline.value(x1);
				double y2 = (spline.value(x2) < 0) ? 0 : spline.value(x2);
				range = y2 - y1;
				// debugging
				if (range < 0) { // error condition
					System.err.println("Error: range is negative!");
					System.out.println("When checking: "+eArray[i+1].toString()+" - "+eArray[i].toString());
					System.out.println("y2= "+y2+" , y1= "+y1);
				}
				if (range > range1) {
					range1 = range;
					firstMinX = x1;
					firstMaxX = x2;
				}
				else {
					if (range > range2) {
						range2 = range;
						secondMinX = x1;
						secondMaxX = x2;
					}
				}
			}
		}
		// if x != initialization value, -9999
		if (firstMinX >= 0 ) {
			// if the spline value is below 0.0, set to 0.0
			firstMinY = (spline.value(firstMinX) < 0.0) ? 0.0 : spline.value(firstMinX);		 
		}
		if (firstMaxX >= 0) {
			firstMaxY = (spline.value(firstMaxX) < 0.0) ? 0.0 : spline.value(firstMaxX);
		}
		if (secondMinX >= 0) {
			secondMinY = (spline.value(secondMinX) < 0.0) ? 0.0 : spline.value(secondMinX);
		}
		if (secondMaxX >= 0) {
			secondMaxY = (spline.value(secondMaxX) < 0.0) ? 0.0 : spline.value(secondMaxX);
		}
		
		return new double[] {firstMinX, firstMinY, firstMaxX, firstMaxY, secondMinX, secondMinY, secondMaxX, secondMaxY};	
	}
	
	
	/**
	 * Implement according to White et al. 1997.
	 * @param series, assumed to be already smooth
	 * @param bounds
	 * @return the t value of the first time the series exceeds the VI ratio.
	 * @throws Exception
	 */
	public static double greenUpWhite(double[][] series, double[] bounds) throws Exception  {
		
		SplineFunction pSpline = new SplineFunction(series);
		PolynomialSplineFunction deriv = (PolynomialSplineFunction)pSpline.derivative();
		double[][] ratioVals = ndviRatio(series, bounds);
		SplineFunction ratio = new SplineFunction(ratioVals);
		double t = StatUtils.min(ratioVals[0]);
		while (true) { //scan
			//System.out.println("t="+t+" mean= "+mean.value(t)+"smoothed="+smoothed.value(t)+" deriv="+deriv.value(t));
			double tplus = t + (bounds[1]-bounds[0])/1000;
			if (ratio.value(t) < 0.5 && ratio.value(tplus) > 0.5 && deriv.value(t) > 0) {
				break;
			}
			t = tplus;
		}
		return t;
	}
	

	/**
	 * Calculates an array of NDVIratio as defined in White et al. 1997
	 * @param series
	 * @param bounds
	 * @return
	 * @throws Exception
	 */
	public static double[][] ndviRatio(double[][] series, double[] bounds) throws Exception {
		double min = StatUtils.min(series[1]); // min of y
		double max = StatUtils.max(series[1]); // max of y
		double[][] ratioVals = new double[2][series[0].length];
		for (int i=0; i<series[0].length; i++) {
			// copy the x coordinate
			ratioVals[0][i] = series[0][i];
			// compute the ratio
			ratioVals[1][i] = (series[1][i] - min) / (max - min);
		}
		return ratioVals;
	}
	
	
	/**
	 * Implement according to Reed et al. 1994.  Moving window of 42 (?? legacy).
	 * @param series, assumed to be not smooth, so the original values can be used in the running mean
	 * @param bounds
	 * @param width
	 * @return
	 * @throws Exception
	 */
	public static double greenUpReed(double[][] series, double[] bounds, int width) throws Exception {

		SplineFunction pSpline = new SplineFunction(series);
		double[][] smooth = smoothFunction(pSpline, bounds[0], bounds[1], 0.85);
		SplineFunction smoothed = new SplineFunction(smooth);
		PolynomialSplineFunction deriv = (PolynomialSplineFunction)smoothed.derivative();
		
        // get the running mean and start checking it.
		double[][] meanVals = runningMean(series, width);
		SplineFunction mean = new SplineFunction(meanVals);
		double t = StatUtils.min(smooth[0]);
		// go to where the smoothed data is increasing and crosses the running mean
		while (true) { //scan
			//System.out.println("t="+t+" mean= "+mean.value(t)+"smoothed="+smoothed.value(t)+" deriv="+deriv.value(t));
			double tplus = t + (bounds[1]-bounds[0])/1000;
			if (smoothed.value(t) < mean.value(t) && smoothed.value(tplus) > mean.value(tplus) && deriv.value(t) > 0) {
				break;
			}
			t = tplus;
		}
		return t;
	}
	

	/**
	 * Return an array of the running average, truncated at the ends
	 * @param series, assumed to be not smooth, so the original values can be used in the running mean
	 * @param width of the mean window in array-units
	 * @return 
	 * @throws Exception
	 */
	public static double[][] runningMean(double[][] series, int width) throws Exception {

		double[][] meanVals = new double[2][series[0].length];
		int start = -1;
		int end = -1;
		int count = 0;
		int halfWidth = width/2;
		double meanSum;
		double mean;

		for (int i=0; i<series[0].length; i++) {
			// copy the x coordinate
			meanVals[0][i] = series[0][i];
			
			// figure out the range over which to average
			if (i-halfWidth < 0) { // start
				start = 0;
			} else {
				start = i-halfWidth;
			}
			if (i+halfWidth > series[1].length-1) { // end
				end = series[1].length-1;
			} else {
				end = i+halfWidth;
			}
			
			// compute the mean
			count = 0;
			meanSum = 0;
			for (int j=start; j<end; j++) {
				meanSum += series[1][j];
				count++;
			}
			mean = meanSum/(double)count;
			meanVals[1][i] = mean;
		}
		return meanVals;
	}
	
	
	/**
	 * 
	 * @param series
	 * @param interval
	 * @return 
	 */
	public static TreeMap<Double, Double> getPieceWise(List<double[]> series, double interval) {
		TreeMap<Double, Double> map = new TreeMap<Double, Double>();
		LinkedList<double[]> piece = new LinkedList<double[]>();
		piece.add(series.get(0));
		for (int i=1; i<series.size(); i++) {
			double[] cur = series.get(i);
			//System.out.println("Processing: "+Arrays.toString(cur));
			if ((cur[0] - piece.getLast()[0]) > interval) { // interval too big
				//System.out.println("\t Updating tree.");
				updateTree(piece, map);
				// start a new piece
				piece = new LinkedList<double[]>();
			} 
			// append the current time point to the new piece, which may be empty
			piece.add(cur);
			if (i == series.size()-1) { // it's the last time point
//				System.out.println("\t End. Updating tree");
				updateTree(piece, map);
			}
		}
		return map;
	}
	
	/**
	 * Helper for {@link #getPieceWise(List, double)}
	 * @param piece
	 * @param map
	 */
	private static void updateTree(LinkedList<double[]> piece, TreeMap<Double, Double> map) {
		for (double[] d : piece) {
			System.out.println("\t\t Updating: "+Arrays.toString(d));
		}
		if (piece.size() > 2) { // big enough to fit a spline
			Spline spline = getThinPlateSpline(piece);
//			double[][] data = getSeriesAsArray(piece);
//			ArrayFunction function = new ArrayFunction(data);
//			System.out.println("start: "+data[0][0]+", end: "+data[0][data[0].length-1]);
//			double[][] smooth = smoothFunction(function, data[0][0], data[0][data[0].length-1]+1, 0.99);
//			function = new ArrayFunction(smooth);
			for (double t = piece.getFirst()[0]; t <= piece.getLast()[0]; t++) {
				map.put(t, spline.value(t));
//				map.put(t, function.value(t));
			}
		} else { // only one or two values in the piece.  
			// just insert the known points
			for (int j=0; j<piece.size(); j++) {
				double[] obs = piece.get(j);
				map.put(obs[0], obs[1]);
			}
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// check smoothing
//		String dir1 = "D:/MOD13A2/2010/";
//		String dir2 = "D:/MOD13A2/2011/";
//		try {
//			ImageLoadr2 loadr = new ImageLoadr2(new String[] {dir2, dir1});
//			List<double[]>pixelValues = loadr.getSeries(GISUtils.makePoint(-83.1438, 9.594));
//			final double[][] series = TSUtils.getSeriesAsArray(pixelValues);
//			// splines on the original data, un-scaled
//			final DuchonSplineFunction dSpline = new DuchonSplineFunction(series);
//			double[] minMax = {StatUtils.min(series[0]), StatUtils.max(series[0])};
//			final double[][] smooth1 = TSUtils.sgSmooth(series, 5, 2);
//			final double[][] smooth2 = TSUtils.smoothFunction(dSpline, minMax[0], minMax[1], 0.7);
//			final double[][] smooth3 = TSUtils.smoothFunction(dSpline, minMax[0], minMax[1], 0.05);
//
//			SwingUtilities.invokeLater(new Runnable() {
//	            public void run() {
//	            	TSDisplayer disp = new TSDisplayer();
//	            	disp.graphSeries(series);
//	            	disp.addSpline(dSpline);
//	            	disp.graphSeries(smooth1);
//	            	//disp.graphSeries(smooth2);
//	            	disp.graphSeries(smooth3);
//
//	            }
//	        });
//	        
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

//		String MCD43 = "/Users/nclinton/Documents/GlobalPhenology/amazon/Morton_replication/MCD43_export.txt";
//		double[][] mcd43 = Utils.readFile(new File(MCD43), 1);
//		ArrayFunction mcd43f = new ArrayFunction(mcd43);
//		double[][] mcd43smooth = smoothFunction(mcd43f, 0, 365, 0.992);
//		String MOD13 = "/Users/nclinton/Documents/GlobalPhenology/amazon/Morton_replication/MOD13_export.txt";
//		double[][] mod13 = Utils.readFile(new File(MOD13), 1);
//		ArrayFunction mod13f = new ArrayFunction(mod13);
//		double[][] mod13smooth = smoothFunction(mod13f, 8, 362, 0.992);
//		String MYD13 = "/Users/nclinton/Documents/GlobalPhenology/amazon/Morton_replication/MYD13_export.txt";
//		double[][] myd13 = Utils.readFile(new File(MYD13), 1);
//		ArrayFunction myd13f = new ArrayFunction(myd13);
//		double[][] myd13smooth = smoothFunction(myd13f, 16, 355, 0.992);
//		String MOD09 = "/Users/nclinton/Documents/GlobalPhenology/amazon/Morton_replication/MOD09_export.txt";
//		double[][] mod09 = Utils.readFile(new File(MOD09), 1);
//		ArrayFunction mod09f = new ArrayFunction(mod09);
//		double[][] mod09smooth = smoothFunction(mod09f, 0, 365, 0.992);
//		String MYD09 = "/Users/nclinton/Documents/GlobalPhenology/amazon/Morton_replication/MYD09_export.txt";
//		double[][] myd09 = Utils.readFile(new File(MYD09), 1);
//		ArrayFunction myd09f = new ArrayFunction(myd09);
//		double[][] myd09smooth = smoothFunction(myd09f, 0, 365, 0.992);
//		
//		double[][] combined = {mcd43smooth[0], mcd43smooth[1], 
//								mod13smooth[0], mod13smooth[1], 
//								myd13smooth[0], myd13smooth[1],
//								mod09smooth[0], mod09smooth[1],
//								myd09smooth[0], myd09smooth[1]};
//		Utils.writeFile(combined, "/Users/nclinton/Documents/GlobalPhenology/amazon/Morton_replication/Combined_export.csv");
		
		
//		String azimuth = "/Users/nclinton/Documents/GlobalPhenology/amazon/Morton_replication/MYD09_azimuth_export.txt";
//		double[][] az = Utils.readFile(new File(azimuth), 1);
//		ArrayFunction azf = new ArrayFunction(az);
//		double[][] aZsmooth = smoothFunction(azf, 0, 365, 0.992);
//		double[][] azOut = {aZsmooth[0], aZsmooth[1]};
//		Utils.writeFile(azOut, "/Users/nclinton/Documents/GlobalPhenology/amazon/Morton_replication/MYD09_azimuth_smooth.csv");
		
		
		
	}

}

