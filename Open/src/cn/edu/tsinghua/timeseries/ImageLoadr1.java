/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.awt.image.renderable.ParameterBlock;
import java.util.List;

import com.berkenviro.imageprocessing.JAIUtils;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import ru.sscc.spline.Spline;

import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas Clinton
 * Legacy class.  Nothing to see here...
 */
public class ImageLoadr1 {

	private int timePoints;
	private PlanarImage[][] images;
	private RandomIter[][] iters;
	
	/*
	 * Default constructor initializes arrays corresponding to arguments
	 */
	public ImageLoadr1(int numPts, int numSeries) {
		timePoints = numPts;
		images = new PlanarImage[numSeries][numPts];
		iters = new RandomIter[numSeries][numPts];
	}
	
	
	/*
	 * Set the image in the array that corresponds to time point t.  
	 * Data is ALWAYS taken from Band 0.
	 */
	public void setImage(int nSeries, int t, PlanarImage image) {
		images[nSeries][t] = image;
		iters[nSeries][t] = RandomIterFactory.create(image, null);
	}
	
	/*
	 * Return the number of time points
	 */
	public int getTimePoints() {
		return timePoints;
	}
	
	/*
	 * Primary data getting method.  Gets one value at a time,
	 * ALWAYS from band 0!!
	 */
	public double timeValue(int nSeries, int t, Point pt) throws Exception {
		double[] ptXY = {pt.getX(), pt.getY()};
		int[] pixelXY = JAIUtils.getPixelXY(ptXY, images[nSeries][t]);
		return iters[nSeries][t].getSampleDouble(pixelXY[0], pixelXY[1], 0);
	}
	
	/*
	 * Return a spline from the images that represent the specified 
	 * series at the specified point.  If  censor==true, then zero 
	 * values will be removed prior to building the spline.
	 */
	public DuchonSplineFunction getSpline(Point pt, int nSeries, boolean censor) throws Exception {
		
		// initialize
		double[] raw = new double[timePoints];
		double[] x = new double[timePoints];
		double[] y = null;
		
		for (int t=0; t<raw.length; t++) {
			raw[t] = timeValue(nSeries, t, pt);
			x[t] = t;
		}
		
		// censor if specified
		if (censor) {
			double[][] censored = TrainingMakr5.censorZero(raw);
			x = censored[0];
			y = censored[1];
		}
		else {
			y = raw;
		}

		// fit a Spline to the censored values: xCens and yCens
		//Spline mySpline = TSUtils.duchonSpline(x, y);
		//return mySpline;
		return new DuchonSplineFunction(new double[][] {x,y});
	}
	
	/*
	 * Helper method to extract a band
	 */
	public static PlanarImage getBand(PlanarImage source, int band) {
		int[] bandIndices = {band};
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(source);
		pb.add(bandIndices);
		return JAI.create("bandSelect", pb);
	}
	
	/*
	 * Returns the image associated with series s and time t.
	 */
	public PlanarImage getImage(int s, int t) {
		return images[s][t];
	}
	
	/*
	 * Helper method to find the min and max precip years in:
	 * {2001-2002, 2002-2003, 2003-2004, 2004-2005}.
	 * Returns {min, minX, max, maxX} where X is the ndvi year.
	 * REPLACES TrainingMakr5.FindDryWet
	 */
	public int[] findDryWet(Point pt) throws Exception {
		
		double[] precips = new double[4];
		// t is the year index, t+1 is the *water* year index, t-1 is the array index
		for (int t=1; t<5; t++) {
			int index = t*12;
			
			// this is the precip vector
			double[] comboVals = new double[7];
			comboVals[0] = timeValue(1, index-3, pt); // oct
			comboVals[1] = timeValue(1, index-2, pt); // nov
			comboVals[2] = timeValue(1, index-1, pt); // dec
			comboVals[3] = timeValue(1, index, pt); // jan
			comboVals[4] = timeValue(1, index+1, pt); // feb
			comboVals[5] = timeValue(1, index+2, pt); // mar
			comboVals[6] = timeValue(1, index+3, pt); // apr
			// fill in the precips array
			precips[t-1] = weka.core.Utils.sum(comboVals);
		}
		// find the maximum and minimum precipitation years
		int min_t = weka.core.Utils.minIndex(precips);
		int max_t = weka.core.Utils.maxIndex(precips);
		// convert from array index to ndvi year index
		int[] dryWet = {(int)precips[min_t], min_t+2, (int)precips[max_t], max_t+2};
		return dryWet;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Test the image loading and spline getting
		ImageLoadr1 loader = new ImageLoadr1(60, 2);
		
		// this is the individual band that represents a time
		PlanarImage dest = null;
		
		/*
		 * What follows is specialized code that will vary based on the 
		 * structure of the data on the file system, image bands, etc...
		 */ 
		for (int j=1; j<=5; j++) {
			String ndviString = "F:\\NASA_Ames\\MODIS_NDVI\\ndvi200"+j+"_4gdalbf.tif";
			PlanarImage ndviImage = JAIUtils.readImage(ndviString);
			// extract the bands, georeference, set in the loader
			for (int k=0; k<12; k++) {
				dest = getBand(ndviImage, k);
				// set the georeferencing
				JAIUtils.transferGeo(ndviImage, dest);
				// consecutive month
				int monthNum = (j-1)*12 + k;
				// NDVI will be series 0
				loader.setImage(0, monthNum, dest);
			}
		}

		/*
		 * Precip is next
		 */
		for (int i=1; i<=5; i++) {
			String precipString = "F:\\NASA_Ames\\PRIZM_precip\\200"+i+"\\us_ppt_200"+i+"_combo.tif";
			PlanarImage precipImage = JAIUtils.readImage(precipString);
			for (int l=0; l<12; l++) {
				dest = getBand(precipImage, l);
				// set the georeferencing
				JAIUtils.transferGeo(precipImage, dest);
				// consecutive month
				int monthNum = (i-1)*12 + l;
				// precip will be series 1
				loader.setImage(1, monthNum, dest);
			}
		}
	}
	

}
