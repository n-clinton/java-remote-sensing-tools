/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import ru.sscc.spline.Spline;
import ru.sscc.spline.polynomial.PSpline;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Nicholas Clinton
 *
 */
public class TimeSeriesClassifier { 
	
	private ImageLoadr1 loader;
	
	/*
	 * Constructor
	 */
	public TimeSeriesClassifier(ImageLoadr1 loadr) {
		loader = loadr;
	}
	
	/*
	 * 
	 */
	public PlanarImage classify(Instances attributeRef, Classifier c, Geometry polyMask) {
		
		// use the first NDVI image as the pixel reference
		PlanarImage pixelRef = loader.getImage(0,0);
		
		System.out.println("Reference for classification: "+pixelRef.getProperty("fileName"));
		
		// use the reference to set up the output
		int width = pixelRef.getWidth();
		int height = pixelRef.getHeight();
		System.out.println("width= "+width);
		System.out.println("height= "+height);
	
		// this byte image will be the output
		java.awt.Point origin = new java.awt.Point(0,0);
		int numBands = 1;
		WritableRaster classifiedOut = RasterFactory.createBandedRaster(
    											DataBuffer.TYPE_BYTE,
    											width,
    											height,
    											numBands,
    											origin);
		
		// This will be the classified instance, simply get the first, clear everything
		Instance toClassify = (Instance) attributeRef.firstInstance().copy();
		clearInstance(toClassify);

		// iterate over the pixels 
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				
				/*
				 * Start thread (??? 20121018)
				 */
				double[] cCorrs = null;
				double[] lEffects = null;
				double[] seas = null;
				double[] predictors = null;
				// get the data
				double[][] ndviSeries = null;
				double[][] precipSeries = null;
				try {
					// reference image coords
					int[] xy = {x, y};
					// projected coords on reference image
					double[] projXY = JAIUtils.getProjectedXY(xy, pixelRef);
					
					// make a point out of the coords
					Point pt = GISUtils.makePoint(projXY[0], projXY[1]);
					
					// only process in the training shape
					if (polyMask != null && !polyMask.contains(pt)) {
						// use 11 as the out of bounds code
						classifiedOut.setSample(x,y,0,11);
						System.out.println("Pixel ("+x+", "+y+")");
						System.out.println("Out of bounds: "+11);
						continue;
					}
					// get the data
					ndviSeries = getNDVIseries(pt, loader);
					precipSeries = getPRISMseries(pt, loader);
					// get all the prediction info
					cCorrs = crossCorrs(pt, ndviSeries, precipSeries);
					lEffects = linEffects(pt, ndviSeries, precipSeries);
					seas = seasonality(pt, ndviSeries, precipSeries);
					predictors = bioPredictors(pt, loader);
				// if there's a problem (Splines), just skip this pixel
				} catch (Exception e) {
					e.printStackTrace();
					// use 13 as the error code
					classifiedOut.setSample(x,y,0,13);
					System.out.println("Pixel ("+x+", "+y+")");
					System.out.println("Error condition: "+13);
					// and skip the rest
					continue;
				}
				
				/*
				 * Read the data into the instance.  This is the mapping 
				 * between the attributes and the predictors.
				 * predictorArr = {firstMinX,
        						 firstMinY,
        						 firstMaxX,
        						 firstMaxY,
        						 whiteX,
        						 whiteY,
        						 reedX,
        						 reedY,
        						 dryMaxX,
        						 dryMaxY,
        						 wetMaxX,
        						 wetMaxY,
        						 dNDVI};
				 */
				toClassify.setValue(attributeRef.attribute("firstMinX"), predictors[0]);
				toClassify.setValue(attributeRef.attribute("firstMinY"), predictors[1]);
				toClassify.setValue(attributeRef.attribute("firstMaxX"), predictors[2]);
				toClassify.setValue(attributeRef.attribute("firstMaxY"), predictors[3]);
				toClassify.setValue(attributeRef.attribute("whiteX"), predictors[4]);
				toClassify.setValue(attributeRef.attribute("whiteY"), predictors[5]);
				toClassify.setValue(attributeRef.attribute("reedX"), predictors[6]);
				toClassify.setValue(attributeRef.attribute("reedY"), predictors[7]);
				toClassify.setValue(attributeRef.attribute("dryMaxX"), predictors[8]);
				toClassify.setValue(attributeRef.attribute("dryMaxY"), predictors[9]);
				toClassify.setValue(attributeRef.attribute("wetMaxX"), predictors[10]);
				toClassify.setValue(attributeRef.attribute("wetMaxY"), predictors[11]);
				toClassify.setValue(attributeRef.attribute("dNDVI"), predictors[12]);
				//
				toClassify.setValue(attributeRef.attribute("corr10"), cCorrs[0]);
				toClassify.setValue(attributeRef.attribute("corr20"), cCorrs[1]);
				toClassify.setValue(attributeRef.attribute("corr30"), cCorrs[2]);
				toClassify.setValue(attributeRef.attribute("corr40"), cCorrs[3]);
				toClassify.setValue(attributeRef.attribute("corr50"), cCorrs[4]);
				toClassify.setValue(attributeRef.attribute("corr60"), cCorrs[5]);
				toClassify.setValue(attributeRef.attribute("corr70"), cCorrs[6]);
				toClassify.setValue(attributeRef.attribute("corr80"), cCorrs[7]);
				toClassify.setValue(attributeRef.attribute("corr90"), cCorrs[8]);
				toClassify.setValue(attributeRef.attribute("avgCorr"), cCorrs[9]);
				//
				toClassify.setValue(attributeRef.attribute("lin10"), lEffects[0]);
				toClassify.setValue(attributeRef.attribute("lin20"), lEffects[1]);
				toClassify.setValue(attributeRef.attribute("lin30"), lEffects[2]);
				toClassify.setValue(attributeRef.attribute("lin40"), lEffects[3]);
				toClassify.setValue(attributeRef.attribute("lin50"), lEffects[4]);
				toClassify.setValue(attributeRef.attribute("lin60"), lEffects[5]);
				toClassify.setValue(attributeRef.attribute("lin70"), lEffects[6]);
				toClassify.setValue(attributeRef.attribute("lin80"), lEffects[7]);
				toClassify.setValue(attributeRef.attribute("lin90"), lEffects[8]);
				//
				toClassify.setValue(attributeRef.attribute("seas20"), seas[0]);
				toClassify.setValue(attributeRef.attribute("seas40"), seas[1]);
				toClassify.setValue(attributeRef.attribute("seas50"), seas[2]);
				
				// instance is complete, classify
				String prediction;
				try {
					// the following is from the Weka book
					int classIndex = (int) c.classifyInstance(toClassify);
					prediction = attributeRef.classAttribute().value(classIndex);
					
					/*
					 * End thread
					 */
					
					
					System.out.println("Pixel ("+x+", "+y+")");
					System.out.println("Classified as: "+prediction+" ("+classIndex+")");
					classifiedOut.setSample(x,y,0,classIndex);
				} catch (Exception e) {
					e.printStackTrace();
				}
				// and start over
				clearInstance(toClassify);
			}
		}
		
		// create a byte sample model
		SampleModel sModel = RasterFactory.createBandedSampleModel(
    											DataBuffer.TYPE_BYTE,
    											width,
    											height,
    											numBands);
    
		// create a compatible ColorModel
		ColorModel cModel = PlanarImage.createColorModel(sModel);
		// Create TiledImage using the SampleModel.
		TiledImage tImageClassified = new TiledImage(0,0,width,height,0,0,sModel,cModel);
		// Set the data of the tiled images to be the rasters.
		tImageClassified.setData(classifiedOut);
		
		return tImageClassified;
	}
	
	/*
	 * Helper
	 */
	public static void clearInstance(Instance inst) {
		Enumeration en = inst.enumerateAttributes();
		while(en.hasMoreElements()) {
			inst.setMissing((Attribute) en.nextElement());
		}
	}
	
	
	/*
	 * **************************************************************
	 * Abstract all the TrainingMakr5 methods to here:
	 */
	
	/*
	 * 
	 */
	public static double[] crossCorrs(Point pt, double[][] ndviSeries, double[][] precipSeries) {
		
		// hardcode for now
		boolean springOnly = true;
		
		// compute lagged correlation
		double[] corr = new double[100];
		double[] lags = new double[100];
		double cov, avgCorr = 0;
		for (int m=0; m<100; m++) {
			lags[m] = m;
			cov = TrainingMakr5.springCovariance(precipSeries[1], ndviSeries[1], m);
			corr[m] = TrainingMakr5.crossCorrelation(cov, precipSeries[1], ndviSeries[1]);
			if (m > 40 && m <= 60) { // include in the average
				avgCorr += corr[m];
			}
		}
		avgCorr = avgCorr/20.0;
		
		double [] corrArr = new double[10];
		int count = 0;
		for (int t=10; t<=90; t+=10) {
			corrArr[count] = corr[t];
			count++;
		}
		corrArr[9] = avgCorr;
		return corrArr;
	}
	
	/*
	 * 
	 */
	public static double[] linEffects(Point pt, double[][] ndviSeries, double[][] precipSeries) {
		
		double[] effectsArr = new double[9];
		int count = 0;
		for (int t=10; t<=90; t+=10) {
			double rss = TrainingMakr5.springLinearRSS(precipSeries[1], ndviSeries[1], t, 30, true);
			effectsArr[count] = rss;
			count++;
		}
		return effectsArr;
	}
	
	/*
	 * 
	 */
	public static double[] seasonality(Point pt, double[][] ndviSeries, double[][] precipSeries) {
		return TrainingMakr5.seasonality(ndviSeries[1]);
	}
	
	/*
	 * 
	 */
	public static double[] bioPredictors(Point pt, ImageLoadr1 loadr) throws Exception {

		// get the Splines
		//Spline ndviSpline = TrainingMakr5.getNDVISpline(pt);
		//Spline precipSpline = TrainingMakr5.getPrecipSpline(pt);
		Spline ndviSpline = loadr.getSpline(pt, 0, true);
		Spline precipSpline = loadr.getSpline(pt, 1, false);

		double[][] ndviVals = TrainingMakr5.getXandY(ndviSpline);
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
        int[] dryWet = loadr.findDryWet(pt);
        
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
            	dryMaxX = ranges[2]%12.0;
            	dryMaxY = ranges[3];
            	// Bradley and Mustard 2006
            	minNDVI = ndviSpline.value(x1) - ndviSpline.value(x2);
            }
            else if (j == dryWet[3]-1) {
            	wetMaxX = ranges[2]%12.0;
            	wetMaxY = ranges[3];
            	maxNDVI = ndviSpline.value(x1) - ndviSpline.value(x2);
            }
            
            // get the green-up indices
            whiteX = TSUtils.greenUpWhite(ndviVals, xRange);
            whiteY = ndviSpline.value(whiteX);
            whiteXSum += whiteX%12.0;
            whiteYSum += whiteY;
            reedX = TSUtils.greenUpReed(ndviVals, xRange, 42);
            reedY = ndviSpline.value(reedX);
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
		
        double[] predictorArr = {firstMinX,
        						 firstMinY,
        						 firstMaxX,
        						 firstMaxY,
        						 whiteX,
        						 whiteY,
        						 reedX,
        						 reedY,
        						 dryMaxX,
        						 dryMaxY,
        						 wetMaxX,
        						 wetMaxY,
        						 dNDVI};
        return predictorArr;
	}
	
	/*
	 * Helper
	 */
	private static double[][] getNDVIseries(Point pt, ImageLoadr1 loadr) throws Exception {
		//Spline ndviSpline = TrainingMakr5.getNDVISpline(pt);
		Spline ndviSpline = loadr.getSpline(pt, 0, true);
		
		return TrainingMakr5.getXandY(ndviSpline);
	}
	
	/*
	 * Helper
	 */
	private double[][] getPRISMseries(Point pt, ImageLoadr1 loadr) throws Exception {
		//Spline precipSpline = TrainingMakr5.getPrecipSpline(pt);
		Spline precipSpline = loadr.getSpline(pt, 1, false);
		
		return TrainingMakr5.getXandY(precipSpline);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		
		// This is the training
		Instances instances = TrainingProcessr.loadInstances("F:\\NASA_Ames\\training2\\training5b_combined_nominal.arff");
		Attribute abundance = instances.attribute("abundance");
		instances.setClass(abundance);
		
		// Build the classifier
		SMO svm = new SMO();
		// constant parameters
		svm.setNumFolds(-1); // use training data
		svm.setRandomSeed(1);
		svm.setBuildLogisticModels(true);
		RBFKernel rbfKernel = new RBFKernel();
		// use the optimal parameters
		svm.setC(5.0);
		rbfKernel.setGamma(0.675);
		svm.setKernel(rbfKernel);
		try {
			svm.buildClassifier(instances);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// make a new Loadr and load it with images
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
				dest = ImageLoadr1.getBand(ndviImage, k);
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
				dest = ImageLoadr1.getBand(precipImage, l);
				// set the georeferencing
				JAIUtils.transferGeo(precipImage, dest);
				// consecutive month
				int monthNum = (i-1)*12 + l;
				// precip will be series 1
				loader.setImage(1, monthNum, dest);
			}
		}
		
		TimeSeriesClassifier tsc = new TimeSeriesClassifier(loader);
		
		// This is the processing polygon
		String polyString = "F:\\NASA_Ames\\shapefiles\\processing_shape.shp";
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = GISUtils.getFeatureCollection(new File(polyString));
		FeatureIterator<SimpleFeature> iter=collection.features();
		SimpleFeature feature = null;
		while (iter.hasNext()) {
			feature = iter.next();  // should have only one
		}
		PlanarImage classified = tsc.classify(instances, svm, (Polygon)feature.getDefaultGeometry());
		//PlanarImage classified = tsc.classify(instances, svm, null);
		String outFileName = "F:\\NASA_Ames\\outImages\\2005class.tiff";
		JAI.create("filestore", classified, outFileName, "TIFF");

		
	}

}
