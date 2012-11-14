package cn.edu.tsinghua.timeseries;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.jai.PlanarImage;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;

import ru.sscc.spline.Spline;
import ru.sscc.spline.polynomial.PSpline;


/**
 * 
 */

/**
 * @author Nicholas Clinton
 * This class to combine and improve on the previous TrainingMakrs
 */
public class TrainingMakr5 {

	/*
	 * Helper method to format a Double
	 */
	public static String doubleString(double d) {
		String dString = String.valueOf(d);
		int dotIndex = dString.indexOf(".");
		int maxIndex = (dotIndex+4 > dString.length()-1) ? dString.length()-1 : dotIndex+4;
		return dString.substring(0, dotIndex+1)+dString.substring(dotIndex+1, maxIndex);
	}
	
	/*
	 * Method to get the complete NDVI spline for 2001-2005.
	 * Zero values will be censored.
	 */
	public static Spline getNDVISpline(Point pt) throws Exception {
		
		// initialize
		double[] trainPtXY = {pt.getX(), pt.getY()};
		// index by month
		double[] raw = new double[5*12];
		
		// iterate over the 5 years
		String ndviString;
		PlanarImage ndviImage;
		int[] imageXY = null;
		for (int j=1; j<=5; j++) {
			ndviString = "F:\\NASA_Ames\\MODIS_NDVI\\ndvi200"+j+"_4gdalbf.tif";
			ndviImage = JAIUtils.readImage(ndviString);
			imageXY = JAIUtils.getPixelXY(trainPtXY, ndviImage);
			// this is a length 12 array, one obseravtion for each month
			double[] ndviVals = TSUtils.imageValues(imageXY[0], imageXY[1], ndviImage);
			// read it into the raw array
			for (int k=0; k<12; k++) {
				raw[(j-1)*12 + k] = ndviVals[k];
			}
		}
		
		// get the zeros out of the raw array
		double[][] censored = censorZero(raw);
		double[] xCens = censored[0];
		double[] yCens = censored[1];
		
		// fit a Spline to the censored values: xCens and yCens
		Spline mySpline = TSUtils.duchonSpline(xCens, yCens);
		
		return mySpline;
	}
	
	
	/*
	 * Helper method to return arrays of x and y from the spline.
	 * X is in units of decimal months.  500 values.
	 */	
	public static double[][] getXandY(Spline spline) {
		double[][] xAndY = new double[2][500];
		// return 500 values (100 per year) from this spline
		for (int i=0; i<500; i++) { 
			// compute the x for each index
			xAndY[0][i] = 1+i*(59.0/500.0);
			xAndY[1][i] = spline.value(xAndY[0][i]);
			if (xAndY[1][i] < 0) {
				//System.err.println("Less than zero warning: ("+xAndY[0][i]+", "+xAndY[1][i]+")");
			}
		}
			
		return xAndY;
	}

	/*
	 * Helper method to remove zeros from an array.  Assumes
	 * x values are simply the indices of the array + 1, 1:60.
	 */
	public static double[][] censorZero(double[] hasZero) {

		ArrayList xy = new ArrayList();
		for (int i=0; i<hasZero.length; i++) {
			// if this is a non-zero NDVI point
			if (hasZero[i] != 0.0) {
				// units of months for x
				double[] xAndY = {i+1, hasZero[i]};
				xy.add(xAndY);
			}
		}
		double[] yCens = new double[xy.size()];
		double[] xCens = new double[xy.size()];
		Iterator iterator = xy.iterator();
		int count = 0;
		double[] yAndX;
		while (iterator.hasNext()) {
			yAndX = ((double[])iterator.next());
			xCens[count] = yAndX[0];
			yCens[count] = yAndX[1];
			count++;
		}
		
		double[][] retArray = new double[2][xCens.length];
		retArray[0] = xCens;
		retArray[1] = yCens;
		
		return retArray;
	}

	/*
	 * Method to get the complete Precip series for 2001-2005
	 */
	public static Spline getPrecipSpline(Point pt) throws Exception {
		// initialize
		double[] trainPtXY = {pt.getX(), pt.getY()};
		double[] rawY = new double[5*12];
		double[] rawX = new double[5*12];
		
		// iterate over the 5 years
		String precipString;
		PlanarImage precipImage;
		int[] imageXY = null;
		for (int j=1; j<=5; j++) {
			precipString = "F:\\NASA_Ames\\PRIZM_precip\\200"+j+"\\us_ppt_200"+j+"_combo.tif";
			precipImage = JAIUtils.readImage(precipString);
			imageXY = JAIUtils.getPixelXY(trainPtXY, precipImage);
			double[] precipVals = TSUtils.imageValues(imageXY[0], imageXY[1], precipImage);
			for (int k=0; k<12; k++) {
				rawY[(j-1)*12 + k] = precipVals[k];
				//  indexed by month: 1:60
				rawX[(j-1)*12 + k] = (j-1)*12 + k + 1;
			}
		}
		
		// no need to censor the precip
		Spline mySpline = TSUtils.duchonSpline(rawX, rawY);
		
		return mySpline;
	}
	
	/*
	 * Use the pt to pick a pixel in refImage, return the average value of 
	 * abundImage within that pixel.
	 */
	public static double avgAbundance(Point pt, PlanarImage refImage, PlanarImage abundImage) throws Exception {
		
		double[] trainPtXY = {pt.getX(), pt.getY()};
		AffineTransformation pixel2proj = null;
		int[] imageXY = null;
		Polygon pixelPoly;
		
		// convert to pixel coordinates
		imageXY = JAIUtils.getPixelXY(trainPtXY, refImage);
		// make a polygon in pixel coordinates
		pixelPoly = GISUtils.makePixelPoly(imageXY[0], imageXY[1]);
		// get a transformation based on the image georeferencing
		pixel2proj = GISUtils.raster2proj(refImage);
		// transform the pixel polygon into a projected polygon
		pixelPoly.apply((com.vividsolutions.jts.geom.CoordinateSequenceFilter)pixel2proj);
		// update
		pixelPoly.geometryChanged();
		// get the percent
		SummaryStatistics percentStats = GISUtils.polygonStats(pixelPoly, abundImage, 0);
		
		return percentStats.getMean();
	}
	
	
	/*
	 * Writes tables with the complete NDVI and precip records.
	 * id_ is the link field.
	 */
	public static void writeSeries (String root, String opDir) {
		String fileString = "F:\\NASA_Ames\\training\\"+root+"_2005_centroids.shp";
		FeatureCollection centroids = GISUtils.getFeatureCollection(new File(fileString));
		FeatureIterator<SimpleFeature> iter = centroids.features();
		
		// This is the Landsat based percent:
		String imageString = "F:\\NASA_Ames\\training\\"+root+"_meta05_adjusted.tif";
		PlanarImage percentImage = JAIUtils.readImage(imageString);
		// these are for pixel reference
		PlanarImage ndviRef = JAIUtils.readImage("F:\\NASA_Ames\\MODIS_NDVI\\ndvi2005_4gdalbf.tif");
		
		// set up the output tables (tab delimited text files)
		String ndviTable = opDir + "\\ndvi_"+root+".txt";
		String precipTable = opDir + "\\precip_"+root+".txt";
		BufferedWriter ndviWriter = null;
		BufferedWriter precipWriter = null;
		try {
			ndviWriter = new BufferedWriter(new FileWriter(new File(ndviTable)));
			precipWriter = new BufferedWriter(new FileWriter(new File(precipTable)));
			// Data Labels for header row
			String dataLabels = "id"+"\t"+"x"+"\t"+"y"+"\t"+"percent";
			for (int i=1; i<=500; i++) {
				dataLabels += "\t"+i;
			}
			ndviWriter.write(dataLabels);
			ndviWriter.newLine();
			ndviWriter.flush();
			precipWriter.write(dataLabels);
			precipWriter.newLine();
			precipWriter.flush();	
			
			// initialize
			SimpleFeature feature = null;
			Point pt = null;
			int id;
			double[] trainPtXY = new double[2];
			// iterate over the training points
			while (iter.hasNext()) {

				feature = iter.next();
				// get the id
				id = ((Integer) feature.getAttribute("id_")).intValue();
				System.out.println("Processing id: "+id);
				ndviWriter.write(id+"\t");
				precipWriter.write(id+"\t");
				
				// get the point
				pt = (Point)feature.getDefaultGeometry();
				// get the projected coordinates of the point
				trainPtXY[0] = pt.getX();
				ndviWriter.write(trainPtXY[0]+"\t");
				precipWriter.write(trainPtXY[0]+"\t");
				trainPtXY[1] = pt.getY();
				ndviWriter.write(trainPtXY[1]+"\t");
				precipWriter.write(trainPtXY[1]+"\t");
				
				// get the abundance
				double abundance = avgAbundance(pt, ndviRef, percentImage);
				ndviWriter.write(abundance+"");
				precipWriter.write(abundance+"");
				
				// get the series, write
				Spline ndviSpline = getNDVISpline(pt);
				// get the Y values
				double[] ndvi = getXandY(ndviSpline)[1];
				Spline precipSpline = getPrecipSpline(pt);
				double[] precip = getXandY(precipSpline)[1];
				for (int k=0; k<500; k++) {
					ndviWriter.write("\t"+ndvi[k]);
					precipWriter.write("\t"+precip[k]);
				}
			
				// write a new line
				ndviWriter.newLine();
				ndviWriter.flush();
				precipWriter.newLine();
				precipWriter.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				ndviWriter.close();
				precipWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	}
	
	/*
	 * Writes tables with the lagged cross correlations.
	 * id_ is the link field.
	 */
	public static void writeCrossCorr(String root, boolean springOnly, int lags, String opDir) {
		String fileString = "F:\\NASA_Ames\\training\\"+root+"_2005_centroids.shp";
		FeatureCollection centroids = GISUtils.getFeatureCollection(new File(fileString));
		FeatureIterator<SimpleFeature> iter = centroids.features();
		
		// This is the Landsat based percent:
		String imageString = "F:\\NASA_Ames\\training\\"+root+"_meta05_adjusted.tif";
		PlanarImage percentImage = JAIUtils.readImage(imageString);
		// these are for pixel reference
		PlanarImage ndviRef = JAIUtils.readImage("F:\\NASA_Ames\\MODIS_NDVI\\ndvi2005_4gdalbf.tif");
		
		// set up the output tables (tab delimited text files)
		String outTable = opDir + "\\corrs_"+root+".txt";
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(outTable)));
			// Data Labels for header row
			String dataLabels = "id"+"\t"+"x"+"\t"+"y"+"\t"+"percent";
			for (int i=1; i<=lags; i++) {
				dataLabels += "\t"+i;
			}
			writer.write(dataLabels);
			writer.newLine();
			writer.flush();	
			
			// initialize
			SimpleFeature feature = null;
			Point pt = null;
			int id;
			double[] trainPtXY = new double[2];
			// iterate over the training points
			while (iter.hasNext()) {

				feature = iter.next();
				// get the id
				id = ((Integer) feature.getAttribute("id_")).intValue();
				System.out.println("Processing id: "+id);
				writer.write(id+"\t");
				
				// get the point
				pt = (Point)feature.getDefaultGeometry();
				// get the projected coordinates of the point
				trainPtXY[0] = pt.getX();
				writer.write(trainPtXY[0]+"\t");
				trainPtXY[1] = pt.getY();
				writer.write(trainPtXY[1]+"\t");
				
				// get the abundance
				double abundance = avgAbundance(pt, ndviRef, percentImage);
				writer.write(abundance+"");
				
				// get the series
				Spline ndviSpline = getNDVISpline(pt);
				// get the Y values
				double[] ndvi = getXandY(ndviSpline)[1];
				Spline precipSpline = getPrecipSpline(pt);
				double[] precip = getXandY(precipSpline)[1];
				// get the lagged cross correlations
				double[] corrs = laggedCorr(100, precip, ndvi, springOnly);
				for (int k=0; k<lags; k++) {
					writer.write("\t"+corrs[k]);
				}
			
				// write a new line
				writer.newLine();
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	}
	
	/*
	 * Writes tables with the lagged cross correlations.
	 * id_ is the link field.
	 */
	public static void writeLinEffects(String root, boolean springOnly, int lags, String opDir) {
		String fileString = "F:\\NASA_Ames\\training\\"+root+"_2005_centroids.shp";
		FeatureCollection centroids = GISUtils.getFeatureCollection(new File(fileString));
		FeatureIterator<SimpleFeature> iter = centroids.features();
		
		// This is the Landsat based percent:
		String imageString = "F:\\NASA_Ames\\training\\"+root+"_meta05_adjusted.tif";
		PlanarImage percentImage = JAIUtils.readImage(imageString);
		// these are for pixel reference
		PlanarImage ndviRef = JAIUtils.readImage("F:\\NASA_Ames\\MODIS_NDVI\\ndvi2005_4gdalbf.tif");
		
		// set up the output tables (tab delimited text files)
		String outTable = opDir + "\\effects_"+root+".txt";
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(outTable)));
			// Data Labels for header row
			String dataLabels = "id"+"\t"+"x"+"\t"+"y"+"\t"+"percent";
			for (int i=1; i<=lags; i++) {
				dataLabels += "\t"+i;
			}
			writer.write(dataLabels);
			writer.newLine();
			writer.flush();	
			
			// initialize
			SimpleFeature feature = null;
			Point pt = null;
			int id;
			double[] trainPtXY = new double[2];
			// iterate over the training points
			while (iter.hasNext()) {

				feature = iter.next();
				// get the id
				id = ((Integer) feature.getAttribute("id_")).intValue();
				System.out.println("Processing id: "+id);
				writer.write(id+"\t");
				
				// get the point
				pt = (Point)feature.getDefaultGeometry();
				// get the projected coordinates of the point
				trainPtXY[0] = pt.getX();
				writer.write(trainPtXY[0]+"\t");
				trainPtXY[1] = pt.getY();
				writer.write(trainPtXY[1]+"\t");
				
				// get the abundance
				double abundance = avgAbundance(pt, ndviRef, percentImage);
				writer.write(abundance+"");
				
				// get the series
				Spline ndviSpline = getNDVISpline(pt);
				// get the Y values
				double[] ndvi = getXandY(ndviSpline)[1];
				Spline precipSpline = getPrecipSpline(pt);
				double[] precip = getXandY(precipSpline)[1];
				// get the effects at each lag
				for (int k=0; k<lags; k++) {
					// p is set HERE!!!!!!!!!!!!!!!!!!!!!!!
					int p = 30;
					double effects = springLinearRSS(precip, ndvi, k, p, springOnly);
					writer.write("\t"+effects);
				}
			
				// write a new line
				writer.newLine();
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	}
	
	/*
	 * Get a set of other training variables
	 */
	public static void writePredictors (String root, String opDir) {
		String fileString = "F:\\NASA_Ames\\training\\"+root+"_2005_centroids.shp";
		FeatureCollection centroids = GISUtils.getFeatureCollection(new File(fileString));
		FeatureIterator<SimpleFeature> iter = centroids.features();
		
		// This is the Landsat based percent:
		String imageString = "F:\\NASA_Ames\\training\\"+root+"_meta05_adjusted.tif";
		PlanarImage percentImage = JAIUtils.readImage(imageString);
		// these are for pixel reference
		PlanarImage ndviRef = JAIUtils.readImage("F:\\NASA_Ames\\MODIS_NDVI\\ndvi2005_4gdalbf.tif");
		PlanarImage precipRef = JAIUtils.readImage("F:\\NASA_Ames\\PRIZM_precip\\2005\\us_ppt_2005_combo.tif");
		
		// set up the output table (tab delimited text)
		String outTable = opDir + "\\predictors2_"+root+".txt";
		BufferedWriter tabWriter = null;
		try {
			tabWriter = new BufferedWriter(new FileWriter(new File(outTable)));

			// Data Labels for header row
			String dataLabels = "id"+"\t"+"x"+"\t"+"y"+"\t"+"percent"+"\t"+
			// averaged predictors based on phenology
			"firstMinX"+"\t"+"firstMinY"+"\t"+"firstMaxX"+"\t"+"firstMaxY"+"\t"+
			"whiteX"+"\t"+"whiteY"+"\t"+"reedX"+"\t"+"reedY"+"\t"+
			// predictors based on wet and dry years
			"dryMaxX"+"\t"+"dryMaxY"+"\t"+"wetMaxX"+"\t"+"wetMaxY"+"\t"+"dNDVI";
			tabWriter.write(dataLabels);
			tabWriter.newLine();
			tabWriter.flush();	
			
			// initialize
			SimpleFeature feature = null;
			Point pt = null;
			int id;
			
			// iterate over the training points
			while (iter.hasNext()) {

				feature = iter.next();
				// get the id
				id = ((Integer) feature.getAttribute("id_")).intValue();
				System.out.println("Processing id: "+id);
				tabWriter.write(id+"\t");
				
				// get the point
				pt = (Point)feature.getDefaultGeometry();
				// get the projected coordinates of the point, write
				double[] trainPtXY = {pt.getX(), pt.getY()};
				tabWriter.write(trainPtXY[0]+"\t");
				tabWriter.write(trainPtXY[1]+"\t");
				
				// get the abundance
				double abundance = avgAbundance(pt, ndviRef, percentImage);
				tabWriter.write(abundance+"\t");
				
				// get the Splines
				Spline ndviSpline = getNDVISpline(pt);
				Spline precipSpline = getPrecipSpline(pt);

				/*
				 * Compute average phenology values from these series:
				 * 1.-12: 000-099: 2001
				 * 13-24: 100-199: 2002
				 * 25-36: 200-299: 2003
				 * 37-48: 300-399: 2004
				 * 49-60: 400-499: 2005
				 */
				double[][] ndviVals = getXandY(ndviSpline);
				// make a derivative of the entire ndvi series
				Spline pSpline = TSUtils.polynomialSpline(ndviVals[0], ndviVals[1], 1);
	            Spline myDerivative = PSpline.derivative(pSpline);
	            
	            // initialize for averaging
	            double firstMinX, firstMinXSum = 0;
	            double firstMinY, firstMinYSum = 0;
	            double firstMaxX, firstMaxXSum = 0;
	            double firstMaxY, firstMaxYSum = 0;
	            double whiteX, whiteXSum = 0;
	            double whiteY, whiteYSum = 0;
	            double reedX, reedXSum = 0;
	            double reedY, reedYSum = 0;

	            // these are for the dry-wet indices
	            double dryMaxX=0, dryMaxY=0, wetMaxX=0, wetMaxY=0;
	            double x1, x2, minNDVI=0, maxNDVI=0, dNDVI;
	            // get the dryWet years:
	            int[] dryWet = findDryWet(pt);
	            
	            for (int j=0; j<5; j++) {
	            	// set the appropriate x-Range for each year
	            	double[] xRange = {j*12.0+1.0, j*12.0+12.0};
	            	List extrema = TSUtils.getExtrema(myDerivative, xRange);
		            double[] ranges = TSUtils.evaluateExtrema(extrema, ndviSpline, xRange);
		            // {firstMinX, firstMinY, firstMaxX, firstMaxY, secondMinX, secondMinY, secondMaxX, secondMaxY}
		            //firstMinXSum += ranges[0]-xRange[0];
		            firstMinXSum += ranges[0]%12.0;
		            firstMinYSum += ranges[1];
		            //firstMaxXSum += ranges[2]-xRange[0];
		            firstMaxXSum += ranges[2]%12.0;
		            firstMaxYSum += ranges[3];
		            
		            // check the dryWet data: {min, minX, max, maxX}
		            x1 = j*12.0 + 4.0;  // April
	            	x2 = j*12.0 + 6.0;  // June
		            if (j == dryWet[1]-1) {
		            	// TODO: change the following (use mod(12))
		            	//dryMaxX = ranges[2]-xRange[0];
		            	dryMaxX = ranges[2]%12.0;
		            	dryMaxY = ranges[3];
		            	// TODO: change the following (add range)
		            	//x = xRange[0] + 5.0;
		            	// Bradley and Mustard 2006
		            	minNDVI = ndviSpline.value(x1) - ndviSpline.value(x2);
		            }
		            else if (j == dryWet[3]-1) {
		            	// TODO: change the following (use mod(12))
		            	//wetMaxX = ranges[2]-xRange[0];
		            	wetMaxX = ranges[2]%12.0;
		            	wetMaxY = ranges[3];
		            	// TODO: change the following (add range)
		            	//x = xRange[0] + 5.0;
		            	maxNDVI = ndviSpline.value(x1) - ndviSpline.value(x2);
		            }
		            
		            // get the green-up indices
		            whiteX = TSUtils.greenUpWhite(ndviVals, xRange);
		            whiteY = ndviSpline.value(whiteX);
		            // TODO: change to mod
		            //whiteXSum += whiteX - xRange[0];
		            whiteXSum += whiteX%12.0;
		            whiteYSum += whiteY;
		            reedX = TSUtils.greenUpReed(ndviVals, xRange, 42);
		            reedY = ndviSpline.value(reedX);
		            // TODO: change to mod
		            //reedXSum += reedX - xRange[0];
		            reedXSum += reedX%12.0;
		            reedYSum += reedY;
	            }
	            
	            // compute the Bradley index
	            dNDVI = maxNDVI - minNDVI;
	            
	            // average
	            firstMinX = firstMinXSum/5.0;
	            firstMinY = firstMinYSum/5.0;
	            firstMaxX = firstMaxXSum/5.0;
	            firstMaxY = firstMaxYSum/5.0;
	            whiteX = whiteXSum/5.0;
	            whiteY = whiteYSum/5.0;
	            reedX = reedXSum/5.0;
	            reedY = reedYSum/5.0;
				
				// write
	            tabWriter.write(firstMinX+"\t");
	            tabWriter.write(firstMinY+"\t");
	            tabWriter.write(firstMaxX+"\t");
	            tabWriter.write(firstMaxY+"\t");
	            tabWriter.write(whiteX+"\t");
	            tabWriter.write(whiteY+"\t");
	            tabWriter.write(reedX+"\t");
	            tabWriter.write(reedY+"\t");
	            tabWriter.write(dryMaxX+"\t");   
	            tabWriter.write(dryMaxY+"\t");
	            tabWriter.write(wetMaxX+"\t");
	            tabWriter.write(wetMaxY+"\t");
	            tabWriter.write(dNDVI+"");
				
				// write a new line
				tabWriter.newLine();
				tabWriter.flush();

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				tabWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/*
	 * Helper method to find the min and max precip years in:
	 * {2001-2002, 2002-2003, 2003-2004, 2004-2005}.
	 * Returns {min, minX, max, maxX} where X is the ndvi year.
	 */
	public static int[] findDryWet(Point pt) throws Exception {
		
		double[] precips = new double[4];
		PlanarImage fallPrecipImage;
		PlanarImage sprPrecipImage;
		double[] trainPtXY = {pt.getX(), pt.getY()}; 
		int[] imageXY = null;
		// t is the year index, t+1 is the *water* year index, t-1 is the array index
		for (int t=1; t<5; t++) {
			fallPrecipImage = JAIUtils.readImage("F:\\NASA_Ames\\PRIZM_precip\\200"+t+"\\us_ppt_200"+t+"_combo.tif");
			sprPrecipImage = JAIUtils.readImage("F:\\NASA_Ames\\PRIZM_precip\\200"+(t+1)+"\\us_ppt_200"+(t+1)+"_combo.tif");
			// convert to pixel coordinates, fall image
			imageXY = JAIUtils.getPixelXY(trainPtXY, fallPrecipImage);
			double[] fallVals = TSUtils.imageValues(imageXY[0], imageXY[1], fallPrecipImage);
			// convert to pixel coordinates, spr image
			imageXY = JAIUtils.getPixelXY(trainPtXY, sprPrecipImage);
			double[] sprVals = TSUtils.imageValues(imageXY[0], imageXY[1], sprPrecipImage);
			// this is the precip vector
			double[] comboVals = new double[7];
			comboVals[0] = fallVals[9]; // oct
			comboVals[1] = fallVals[10]; // nov
			comboVals[2] = fallVals[11]; // dec
			comboVals[3] = sprVals[0]; // jan
			comboVals[4] = sprVals[1]; // feb
			comboVals[5] = sprVals[2]; // mar
			comboVals[6] = sprVals[3]; // apr
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
	
	/*
	 * This method to write a training table of stats generated from the 
	 * complete time series.  This includes cross correlation at different
	 * lags as well as the causality indices.
	 */
	public static void writeStats(String root, String opDir) {
		String fileString = "F:\\NASA_Ames\\training\\"+root+"_2005_centroids.shp";
		FeatureCollection centroids = GISUtils.getFeatureCollection(new File(fileString));
		FeatureIterator<SimpleFeature> iter = centroids.features();
		
		// This is the Landsat based percent:
		String imageString = "F:\\NASA_Ames\\training\\"+root+"_meta05_adjusted.tif";
		PlanarImage percentImage = JAIUtils.readImage(imageString);
		// these are for pixel reference
		PlanarImage ndviRef = JAIUtils.readImage("F:\\NASA_Ames\\MODIS_NDVI\\ndvi2005_4gdalbf.tif");
		PlanarImage precipRef = JAIUtils.readImage("F:\\NASA_Ames\\PRIZM_precip\\2005\\us_ppt_2005_combo.tif");
		
		// set up the output table (tab delimited text)
		String outTable = opDir + "\\statistics4_"+root+".txt";
		BufferedWriter tabWriter = null;
		try {
			tabWriter = new BufferedWriter(new FileWriter(new File(outTable)));

			// Data Labels for header row
			String dataLabels = "id"+"\t"+"x"+"\t"+"y"+"\t"+"percent"+"\t"+
			// stats:
			"corr0"+"\t"+"corr10"+"\t"+"corr20"+"\t"+"corr30"+"\t"+"corr40"+"\t"+"corr50"+"\t"+"corr60"+"\t"+"corr70"+"\t"+"corr80"+"\t"+"avgCorr"+"\t"+
			// added 8/21/7
			"lin0"+"\t"+"lin10"+"\t"+"lin20"+"\t"+"lin30"+"\t"+"lin40"+"\t"+"lin50"+"\t"+"lin60"+"\t"+"lin70"+"\t"+"lin80"+"\t"+
			"seas20"+"\t"+"seas40"+"\t"+"seas50";
			tabWriter.write(dataLabels);
			tabWriter.newLine();
			tabWriter.flush();	
			
			// initialize
			SimpleFeature feature = null;
			Point pt = null;
			int id;
			
			// iterate over the training points
			while (iter.hasNext()) {

				feature = iter.next();
				// get the id
				id = ((Integer) feature.getAttribute("id_")).intValue();
				System.out.println("Processing id: "+id);
				tabWriter.write(id+"\t");
				
				// get the point
				pt = (Point)feature.getDefaultGeometry();
				// get the projected coordinates of the point, write
				double[] trainPtXY = {pt.getX(), pt.getY()};
				tabWriter.write(trainPtXY[0]+"\t");
				tabWriter.write(trainPtXY[1]+"\t");
				
				// get the abundance
				double abundance = avgAbundance(pt, ndviRef, percentImage);
				tabWriter.write(abundance+"\t");
				
				// get the Splines
				Spline ndviSpline = getNDVISpline(pt);
				Spline precipSpline = getPrecipSpline(pt);
				
				// get the data
				double[][] ndviSeries = getXandY(ndviSpline);
				double[][] precipSeries = getXandY(precipSpline);
				
				// compute lagged correlation
				double[] corr = new double[100];
				double[] lags = new double[100];
				double cov, avgCorr = 0;
				for (int m=0; m<100; m++) {
					lags[m] = m;
					cov = springCovariance(precipSeries[1], 
							  ndviSeries[1], 
							  m);
					corr[m] = crossCorrelation(cov, precipSeries[1], ndviSeries[1]);
					// enter best separability range here
					if (m > 0 && m <= 20) { // include in the average
						avgCorr += corr[m];
					}
				}
				avgCorr = avgCorr/20.0;
				
				/*
				String title = "id="+id+", abundance="+abundance;
				TSDisplayer tsd = new TSDisplayer(0, 100, -1, 1, title);
				tsd.graphSeries(lags, corr);
				*/
				for (int t=0; t<=80; t+=10) {
					tabWriter.write(corr[t]+"\t");
				}
				tabWriter.write(avgCorr+"\t");
				
				// compute the other effects measures
				// enter p here!!!!!!!!!!!!!!!!!!!!!!!!!!!
				int p = 30;
				for (int t=0; t<=80; t+=10) {
					double rss = springLinearRSS(precipSeries[1], ndviSeries[1], t, p, true);
					tabWriter.write(rss+"\t");
				}
				
				// get the seasonality measures, write
				double[] betas = seasonality(ndviSeries[1]);
				tabWriter.write(betas[2]+"\t"+betas[3]+"\t"+betas[4]);
				
				tabWriter.newLine();
				tabWriter.flush();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				tabWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * Helper to compute cross covariance at a NEGATIVE lag.
	 */
	public static double crossCovariance(double[] x, double[] y, int lag) {
		double cov = 0;
		double sum = 0;
		double meanX = weka.core.Utils.mean(x);
		double meanY = weka.core.Utils.mean(y);
		for (int k=lag; k<y.length; k++) {
			sum += (y[k]-meanY)*(x[k-lag]-meanX);
		}
		cov = sum/(y.length-lag);
		return cov;
	}
	
	/*
	 * Helper to compute cross covariance at a NEGATIVE lag.  Spring ONLY.
	 */
	public static double springCovariance(double[] x, double[] y, int lag) {
		double cov = 0;
		double sum = 0;
		double meanX = weka.core.Utils.mean(x);
		double meanY = weka.core.Utils.mean(y);
		int count = 0;
		for (int k=lag; k<y.length; k++) {
			// 100 observations in a year, want 20-60 only
			if (k%100 < 20 || k%100 > 60) { continue; }
			sum += (y[k]-meanY)*(x[k-lag]-meanX);
			count++;
		}
		cov = sum/count;
		return cov;
	}
	
	/*
	 * Compute arrays of cross-correlation at the specified number of lags.
	 */
	public static double[] laggedCorr(int lags, double[] x, double[] y, boolean springOnly) {
		double[] corr = new double[lags];
		double cov = 0;
		for (int m=0; m<lags; m++) {
			if (springOnly) {
				cov = springCovariance(x, y, m);
			}
			else {
				cov = crossCovariance(x, y, m);
			}
			corr[m] = crossCorrelation(cov, x, y);
		}
		return corr;
	}
	
	/*
	 * Modified linear effect.  Like Geweke, but does not consider auto-regressive
	 * effects in the model.  Returns the ln(RSS) from a linear model computed from the
	 * specified lag and p terms.
	 */
	public static double springLinearRSS (double[] x, double[] y, int lag, int p, boolean springOnly) {
		double rss = 0.0;
		// Set up an arraylist due to unknown vector length
		ArrayList data = new ArrayList(); // will contain double[]'s
		double[] dataVec;
		
		// start from lag+p
		for(int k=lag+p; k<y.length; k++) {
			// 100 observations in a year, want 20-60 only, spring
			if (springOnly) {
				if (k%100 < 20 || k%100 > 60) { continue; }
			}
			// initialize with p terms and dataVec[p] = response
			dataVec = new double[p+1];
			// last observation is the response
			dataVec[p] = y[k];
			// generate the lagged x's: p previous observations
			for (int i=0; i<p; i++) {
				dataVec[i] = x[k-lag-i-1];
			}
			// add the data vector to the List
			data.add(dataVec);			
		}
		
		Array2DRowRealMatrix yMat = new Array2DRowRealMatrix(data.size(), 1);
		Array2DRowRealMatrix xMat = new Array2DRowRealMatrix(data.size(), p);
		
		// populate the matrices
		for (int m=0; m<data.size(); m++) {
			dataVec = (double[]) data.get(m);
			yMat.setEntry(m, 0, dataVec[p]);
			for (int q=0; q<p; q++) {
				xMat.setEntry(m, q, dataVec[q]);
			}
		}
		
		// regression
		OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
		// WARNING!! This has not been tested, i.e. that xMat.getData() returns an [n,k] array 20121018
		ols.newSampleData(yMat.getColumn(0), xMat.getData());
		double[] resids = ols.estimateResiduals();
		rss = StatUtils.sumSq(resids);
		
		return Math.log(rss);
	}
	
	/*
	 * Seasonality effects.  Returns coefficients of indicators corresponding to
	 * {constant, t, Ind(20<t<40), Ind(40<t<60), Ind(60<t<80)}.
	 */
	public static double[] seasonality(double[] x) {
		double[] coeffs = new double[5];
		// the response is simply the x value
		double[] y = x;
		Array2DRowRealMatrix xMat = new Array2DRowRealMatrix(x.length, 4);
		
		// iterate over x, compute the indicators and populate the matrix
		for (int i=0; i<x.length; i++) {
			// see Janaceq eq. 1.5 for the following
			// first term is t
			xMat.setEntry(i, 0, i);
			// next terms are indicators
			if (i%100 > 20 && i%100 < 40) {
				xMat.setEntry(i, 1, 1.0);
				xMat.setEntry(i, 2, 0.0);
				xMat.setEntry(i, 3, 0.0);
			}
			else if (i%100 > 40 && i%100 < 60) {
				xMat.setEntry(i, 1, 0.0);
				xMat.setEntry(i, 2, 1.0);
				xMat.setEntry(i, 3, 0.0);
			}
			else if (i%100 > 60 && i%100 < 80) {
				xMat.setEntry(i, 1, 0.0);
				xMat.setEntry(i, 2, 0.0);
				xMat.setEntry(i, 3, 1.0);
			}
			else {
				xMat.setEntry(i, 1, 0.0);
				xMat.setEntry(i, 2, 0.0);
				xMat.setEntry(i, 3, 0.0);
			}
		}
		
		// regression
		OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
		// WARNING!! This has not been tested, i.e. that xMat.getData() returns an [n,k] array 20121018
		ols.newSampleData(y, xMat.getData());
		return ols.estimateRegressionParameters();
	}
	
	
	/*
	 * Convert covariance to correlation.
	 */
	public static double crossCorrelation(double cov, double[] x, double[] y) {
		return cov/(Math.sqrt(weka.core.Utils.variance(x))*Math.sqrt(weka.core.Utils.variance(y)));
	}
	
	
	/*
	 * Implement Geweke's feedback for ndvi and precip.  Spring only.
	 */
	public static double feedback (double[] x, double[] y) {
		
		// Set up an arraylist due to unknown vector length
		ArrayList data = new ArrayList(); // will contain double[]'s
		double[] dataVec;
		
		// start from when there are at least 5 previous precipitation values
		for(int k=60; k<y.length; k++) {
			// 100 observations in a year, want 20-60 only, spring
			if (k%100 < 20 || k%100 > 60) { continue; }
			// initialize
			dataVec = new double[21];
			// last observation is the response
			dataVec[20] = y[k];
			// generate the lagged y's: 10 previous observations
			for (int i=1; i<=10; i++) {
				dataVec[i-1] = y[k-i];
			}
			// fill the rest with lagged x's
			for (int j=1; j<=10; j++) {
				//dataVec[9+j] = x[k-35-(j*5)];
				dataVec[9+j] = x[k-10-(j*5)];
			}
			// add the data vector to the List
			data.add(dataVec);			
		}
		
		Array2DRowRealMatrix yMat = new Array2DRowRealMatrix(data.size(), 1);
		Array2DRowRealMatrix subMat = new Array2DRowRealMatrix(data.size(), 10);
		Array2DRowRealMatrix fullMat = new Array2DRowRealMatrix(data.size(), 20);
		
		// populate the matrices
		for (int m=0; m<data.size(); m++) {
			dataVec = (double[]) data.get(m);
			for (int p=0; p<10; p++) {
				subMat.setEntry(m, p, dataVec[p]);
				fullMat.setEntry(m, p, dataVec[p]);
			}
			for (int q=10; q<20; q++) {
				fullMat.setEntry(m, q, dataVec[q]);
			}
			yMat.setEntry(m, 0, dataVec[20]);
		}
		// sub-model regression
//		OLS ols = new OLS();
//		ols.Regress(subMat, yMat, true);
//		Matrix subResids = ols.getResiduals();
		
		OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
		// WARNING!! This has not been tested, i.e. that xMat.getData() returns an [n,k] array 20121018
		ols.newSampleData(yMat.getColumn(0), subMat.getData());
		double[] subResids = ols.estimateResiduals();
		
		// full model regression
		ols = new OLSMultipleLinearRegression();
		ols.newSampleData(yMat.getColumn(0), fullMat.getData());
		double[] fullResids = ols.estimateResiduals();
		
		// compute the estimated variances (these better be 1x1)
		double rssSub = StatUtils.sumSq(subResids);
		double rssFull = StatUtils.sumSq(fullResids);
		
		//System.out.println("rssSub: n= "+rssSub.n+", m="+rssSub.m);
		//System.out.println("rssFull: n= "+rssFull.n+", m="+rssFull.m);
		
		// adjust my n-p, unlike Geweke where there is no adjustment
		double varSub = rssSub/(yMat.getRowDimension() - 10);
		double varFull = rssFull/(yMat.getRowDimension() - 20);
		
		//System.out.println("varSub= "+varSub);
		//System.out.println("varFull= "+varFull);
		
		// compute the index, return
		double index = 0;
		try {
			index = Math.log(Math.sqrt(varSub) / Math.sqrt(varFull));
		} catch (Exception e) {
			e.printStackTrace();
		}
		//System.out.println("index= "+index);
		return index;
	}
	
	
	/*
	 * Writes a table with the number of NDVI data points.  60 is max.
	 */
	public static void writeUncertainty(String root, String opDir) {
		String fileString = "F:\\NASA_Ames\\training\\"+root+"_2005_centroids.shp";
		FeatureCollection centroids = GISUtils.getFeatureCollection(new File(fileString));
		FeatureIterator<SimpleFeature> iter = centroids.features();
		
		// This is the Landsat based percent:
		String imageString = "F:\\NASA_Ames\\training\\"+root+"_meta05_adjusted.tif";
		PlanarImage percentImage = JAIUtils.readImage(imageString);
		// these are for pixel reference
		PlanarImage ndviRef = JAIUtils.readImage("F:\\NASA_Ames\\MODIS_NDVI\\ndvi2005_4gdalbf.tif");
		
		// set up the output tables (tab delimited text files)
		String uncertTable = opDir + "\\uncertainty_"+root+".txt";
		BufferedWriter uncertWriter = null;
		try {
			uncertWriter = new BufferedWriter(new FileWriter(new File(uncertTable)));
			// Data Labels for header row
			String dataLabels = "id"+"\t"+"x"+"\t"+"y"+"\t"+"percent"+"\t"+"numPts";
			uncertWriter.write(dataLabels);
			uncertWriter.newLine();
			uncertWriter.flush();	
			
			// initialize
			SimpleFeature feature = null;
			Point pt = null;
			int id;
			double[] trainPtXY = new double[2];
			// iterate over the training points
			while (iter.hasNext()) {

				feature = iter.next();
				// get the id
				id = ((Integer) feature.getAttribute("id_")).intValue();
				System.out.println("Processing id: "+id);
				uncertWriter.write(id+"\t");
				
				// get the point
				pt = (Point)feature.getDefaultGeometry();
				// get the projected coordinates of the point
				trainPtXY[0] = pt.getX();
				uncertWriter.write(trainPtXY[0]+"\t");
				trainPtXY[1] = pt.getY();
				uncertWriter.write(trainPtXY[1]+"\t");
				
				// get the abundance
				double abundance = avgAbundance(pt, ndviRef, percentImage);
				uncertWriter.write(abundance+"\t");
				
				// iterate over the 5 years
				String ndviString;
				PlanarImage ndviImage;
				double[] raw = new double[5*12];
				int[] imageXY = null;
				for (int j=1; j<=5; j++) {
					ndviString = "F:\\NASA_Ames\\MODIS_NDVI\\ndvi200"+j+"_4gdalbf.tif";
					ndviImage = JAIUtils.readImage(ndviString);
					imageXY = JAIUtils.getPixelXY(trainPtXY, ndviImage);
					// this is a length 12 array, one obseravtion for each month
					double[] ndviVals = TSUtils.imageValues(imageXY[0], imageXY[1], ndviImage);
					// read it into the raw array
					for (int k=0; k<12; k++) {
						raw[(j-1)*12 + k] = ndviVals[k];
					}
				}
				
				// count the number of zeros in there
				int numPts = 0;
				for (int k=0; k<raw.length; k++) {
					if (raw[k] != 0.0) {
						numPts++;
					}
				}
				
				uncertWriter.write(numPts+"\t");
			
				// write a new line
				uncertWriter.newLine();
				uncertWriter.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				uncertWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// these should not have changed 
		
		//writeSeries("p37r32", "F:\\NASA_Ames\\training4");
		//writeSeries("p38r34", "F:\\NASA_Ames\\training4");
		
		// done 11/24
		//writePredictors("p37r32", "F:\\NASA_Ames\\training4");
		//writePredictors("p38r34", "F:\\NASA_Ames\\training4");
		
		
		// done 1/17/08
		//writeStats("p37r32", "F:\\NASA_Ames\\training4");
		//writeStats("p38r34", "F:\\NASA_Ames\\training4");
		
		//writeCrossCorr("p37r32", true, 100, "F:\\NASA_Ames\\training4");
		//writeCrossCorr("p38r34", true, 100, "F:\\NASA_Ames\\training4");
		
		//writeLinEffects("p37r32", true, 100, "F:\\NASA_Ames\\training4");
		//writeLinEffects("p38r34", true, 100, "F:\\NASA_Ames\\training4");

		//writeUncertainty("p37r32", "F:\\NASA_Ames\\training4");
		//writeUncertainty("p38r34", "F:\\NASA_Ames\\training4");
		
		
	}

}
