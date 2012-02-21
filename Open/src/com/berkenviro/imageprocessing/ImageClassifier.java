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

import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * 
 * Classifies a stack of images using Weka classifiers and predictors.
 * NO PROJECTION CHECKING.  THE USER MUST INSURE THAT THE IMAGES
 * ALL HAVE THE SAME PROJECTION.  If the Instances submitted to the constructor
 * were made by ArffMakr(), then the Hashtables submitted to ArffMakr() and 
 * ImageClassifier() should be identical with regard to the imagery and the attributes.
 * See the main method for a processing log and examples.
 * 
 * 
 * @author Nicholas Clinton
 */
public class ImageClassifier {	
	
	// Hashtable with attributes as keys, PlanarImages as values
	Hashtable images;
	// the Classifier must have been trained with the same attributes as images
	Classifier myP;
	boolean readyToClassify;
	
	/**
	 * The constructor verifies the existence of imageFileNames as images.  The attributes in the 
	 * imageFileNames keys must match the attributes of training Instances.  The images are then 
	 * mapped to the attributes in a new Hashtable.
	 * The c is built using training, with the rest of the setup of c done in main().
	 * The referenceImage defines the pixels over which to iterate. The Hashtable should
	 * also have been set up using the training attributes, with the appropriate image matched to 
	 * the appropriate attribute.
	 * 
	 * @param imageFileNames is a Hashtable with Weka.core.Attribute as a key, image file path as a value
	 * @param c is a weka.classifiers.Classifier that has already been configured
	 * @param training is a weka.core.Instances to be used to build c
	 * @return an ImageClassifier that is ready to classify the provided stack of images
	 */
	public ImageClassifier (Hashtable imageFileNames, Classifier c, Instances training) {
		readyToClassify = false;
		// set myP, the instance variable referring to the predictor
		try {
			c.buildClassifier(training);
			myP = c;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		images = new Hashtable(); 
		Attribute a;
		String imageFileName;
		PlanarImage pIm;
		Enumeration attribs = training.enumerateAttributes();
		while (attribs.hasMoreElements()) {
			a = (Attribute) attribs.nextElement();
			if (a.equals(training.classAttribute())) {
				continue;
			}
			// get the image file names, test for existence
			imageFileName = (String) imageFileNames.get(a);
			try {
				pIm = JAI.create("fileload", imageFileName);
				pIm.setProperty("fileName", imageFileName);
				System.out.println("Loaded file: "+pIm.getProperty("fileName"));
				System.out.println("For Attribute: " + a.toString());
				
				// add to the images Hashtable
				images.put(a, pIm);
			}
			catch (Exception e) {
				System.out.println("Weka constructor error! Can not load: "+imageFileName);
				e.printStackTrace();
			}
			
		}
		readyToClassify = true;
	}
	
	/**
	 * Make a prediction map, based on the images in the hashtable and the supplied training.
	 * 
	 * @param reference this is the attribute (image) to be used as a geometrical pixel reference
	 * @param training is used only as a list of attributes.  Use the training supplied to the constructor.
	 * @param outFileName is the name of the output image file to be created.
	 */
	public void predict(Attribute reference, Instances training, String outFileName) {
		
		if (!readyToClassify) {
			System.err.println("Not ready to classify!  System will exit.");
			System.exit(-1);
		}
		
		// Test the Instances and report
		System.out.println("Using training dataset:");
		System.out.println(training.toSummaryString());
		
		// use the reference to set up
		PlanarImage pImageR = (PlanarImage) images.get(reference);
		int width = pImageR.getWidth();
		int height = pImageR.getHeight();
		System.out.println("width= "+width);
		System.out.println("height= "+height);
	
		// these two WritableRasters will contain the output data
		Point origin = new Point(0,0);
		int numBands = 1;
		WritableRaster classifiedOut = RasterFactory.createBandedRaster(
    											DataBuffer.TYPE_FLOAT,
    											width,
    											height,
    											numBands,
    											origin);
    
		// initialize:
		
		// this is just a dummy dataset to identify the Instance(s)
		Instances imagePixels = new Instances(training, 1);
		// this is the data slice corresponding to one line
		Instance[] slice = new Instance[width];
		// fill slice with empty instances
		for (int j=0; j<width; j++) {
			slice[j] = new Instance(training.numAttributes());
			slice[j].setDataset(imagePixels);
			slice[j].classIsMissing();
		}
    
		double pixelValue;
		RandomIter iterator = null;
		
		// Begin the massive set of loops, pixel by pixel processing
		int[] xy = new int[2];
		double[] projXY = new double[2];
		int X, Y;
		double prediction = -9999.9;
		PlanarImage pImage;
		
		// define the bounds
		int y0=0;
		int y1=height;
		int x0=0;
		int x1=width;
		
		for (int y=y0; y<y1; y++) {
			System.out.println("Processing line: "+y);
			for (int c=0; c<training.numAttributes(); c++) {		// each input tif
				if(c == training.classAttribute().index()) { continue; }
				// this object will be continually overwritten
				// get the image that corresponds to the attribute
				pImage = (PlanarImage) images.get(training.attribute(c));
				iterator = RandomIterFactory.create(pImage, null);
				
				// read each image into the appropriate attribute of the Instance
				for (int x=x0; x<x1; x++) { 
					
					try {
						// reference image coords
						xy[0] = x;
						xy[1] = y;
						// projected coords on reference image
						projXY = com.berkenviro.imageprocessing.JAIUtils.getProjectedXY(xy, pImageR);
						// convert to pixel coords on attribute image
						xy = com.berkenviro.imageprocessing.JAIUtils.getPixelXY(projXY, pImage);
						// these are the attribute image pixel coordinates, uppercase
						X = xy[0]; 
						Y = xy[1];
						
						// get sample with X,Y uppercase
						pixelValue = iterator.getSampleDouble(X,Y,0);
						slice[x].setValue(c, pixelValue);
						
						// if this is the last scan of the line, all the data has been read
						if (c == training.numAttributes()-1 || 
						   (c == training.numAttributes()-2 && training.classAttribute().index() == training.numAttributes()-1)) {
							// the instance should now have all the data in it, classify
							prediction = myP.classifyInstance(slice[x]);
							classifiedOut.setSample(x,y,0,(float) prediction);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			
				} // end pixels
				
			} // end layers
		} // end lines
    
		// cleanup:
		pImage = null;
		slice = null;
		iterator = null;
		System.gc();
		
		//		 write
		String pImageRfileName = (String)pImageR.getProperty("fileName");
		if (com.berkenviro.imageprocessing.JAIUtils.isGeoTiff(pImageRfileName)) {
			System.out.println("Writing GeoTiff: "+outFileName);
			
			JAIUtils.writeFloatGeoTiff(width, 
									   height, 
									   outFileName, 
									   JAIUtils.getGeoTiffFields(pImageRfileName),
									   classifiedOut);
		}
		else {
			System.out.println("Writing Tiff: "+outFileName);
			JAIUtils.writeFloatTiff(classifiedOut, outFileName);
		}
		
		classifiedOut = null;
		System.gc();

		
	}
	
	/**
	 * Classifies the stack of images relative to the image corresponding to reference.
	 * The output will have the same registration and dimensions as reference
	 * The training instances should be the same one submitted to the constructor, and is
	 * used here simply for its list of attributes.
	 * 
	 * @param reference is the pixel reference for the output 
	 * @param training contains the attributes to iterate over
	 * @param outFileName is the file path for the output tiff
	 */
	public void classify(Attribute reference, Instances training, String outFileName) {
		
		if (!readyToClassify) {
			System.err.println("Not ready to classify!  System will exit.");
			System.exit(-1);
		}
		
		// Test the Instances and report
		System.out.println("Using metaPrediction dataset:");
		System.out.println(training.toSummaryString());
		
		// use the reference to set up
		PlanarImage pImageR = (PlanarImage) images.get(reference);
		int width = pImageR.getWidth();
		int height = pImageR.getHeight();
		System.out.println("width= "+width);
		System.out.println("height= "+height);
	
		// this WritableRaster will contain the output data
		Point origin = new Point(0,0);
		int numBands = 1;
		WritableRaster classifiedOut = RasterFactory.createBandedRaster(
    											DataBuffer.TYPE_INT,
    											width,
    											height,
    											numBands,
    											origin);
    
		// initialize:
		
		// this is just a dummy dataset to identify the Instance(s)
		Instances imagePixels = new Instances(training, 1);
		// this is the data slice corresponding to one line
		Instance[] slice = new Instance[width];
		// fill slice with empty instances
		for (int j=0; j<width; j++) {
			slice[j] = new Instance(training.numAttributes());
			slice[j].setDataset(imagePixels);
			slice[j].classIsMissing();
		}
    
		double pixelValue;
		RandomIter iterator = null;
		
		// Begin the massive set of loops, pixel by pixel processing
		int[] xy = new int[2];
		double[] projXY = new double[2];
		int X, Y;
		String predictor = null;
		int classIndex = -7777;
		PlanarImage pImage;
		
		// this is an alternative to changing the method signature
		// define the bounds (don't go outside the dem)
		int y0=0;
		int y1=height;
		int x0=0;
		int x1=width;
		
		for (int y=y0; y<y1; y++) { // each line
			
			System.out.println("Processing line: "+y);
			// ****** now the class variable is the last attribute *********
			for (int c=0; c<training.numAttributes(); c++) {	// each input tif
				if(c == training.classAttribute().index()) { continue; }
				// this object will be continually overwritten
				// get the image that corresponds to the attribute
				pImage = (PlanarImage) images.get(training.attribute(c));
				iterator = RandomIterFactory.create(pImage, null);
				// read each image into the appropriate attribute of the Instance
				for (int x=x0; x<x1; x++){	// each pixel
					
					try {
						// reference image coords
						xy[0] = x;
						xy[1] = y;
						// projected coords on reference image
						projXY = com.berkenviro.imageprocessing.JAIUtils.getProjectedXY(xy, pImageR);
						// convert to pixel coords on attribute image
						xy = com.berkenviro.imageprocessing.JAIUtils.getPixelXY(projXY, pImage);
						// these are the attribute image pixel coordinates, uppercase
						X = xy[0]; 
						Y = xy[1];
						
						// get sample with X,Y uppercase
						pixelValue = iterator.getSampleDouble(X,Y,0);
						slice[x].setValue(c, pixelValue);
						
						// if this is the last scan of the line, all the data has been read
						if (c == training.numAttributes()-1 || 
						   (c == training.numAttributes()-2 && training.classAttribute().index() == training.numAttributes()-1)) {
							
							// the instance should now have all the data in it, classify
							// the following is from the Weka book
							classIndex = (int) myP.classifyInstance(slice[x]);
							// named predictor
							predictor = training.classAttribute().value(classIndex);
							classifiedOut.setSample(x,y,0,(int)classIndex);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				} // end pixels
			} // end layers
		} // end lines
    
		// cleanup:
		pImage = null;
		slice = null;
		iterator = null;
		System.gc();
		
		
		// create a double sample model
		SampleModel sModel = RasterFactory.createBandedSampleModel(
    											DataBuffer.TYPE_INT,
    											width,
    											height,
    											numBands);
    
		// create a compatible ColorModel
		ColorModel cModel = PlanarImage.createColorModel(sModel);
    
		System.out.println(sModel.toString());
		System.out.println(cModel.toString());
    
		// Create TiledImages using the float SampleModel.
		TiledImage tImageClassified = new TiledImage(0,0,width,height,0,0,sModel,cModel);
		// Set the data of the tiled images to be the rasters.
		tImageClassified.setData(classifiedOut);
		JAI.create("filestore",tImageClassified,outFileName,"TIFF");
		
		tImageClassified = null;
		classifiedOut = null;
		System.gc();
	} 
	
	/**
	 * Display class index values.
	 * @param training is the Instances to get the class value codes
	 * @return a string[][] where return[0] is codes, return[1] is names
	 */
	public static String[][] classVals(Instances training) {
		int numVals = training.numClasses();
		String[][] vals = new String[2][numVals];
		for (int c=0; c<numVals; c++) {
			vals[0][c] = String.valueOf(c);
			vals[1][c] = training.classAttribute().value(c);
			System.out.println(c + " = " + training.classAttribute().value(c));
		}
		return null;
	}
	
	
	/**
	 * Test Code and Processing Log.
	 * @param args
	 */
	public static void main(String[] args) {

		File tFile;
		
		try {
			/*
			// training data:
			tFile = new File("C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/training/training_20090601.csv");
			CSVLoader loader = new CSVLoader();
			loader.setSource(tFile);
			Instances training = loader.getDataSet();
			
			// set the response
			Attribute response = training.attribute("class");
			training.setClass(response);
			System.out.println("The class attribute is...");
			System.out.println(training.classAttribute().name());
			
			System.out.println("Using these instances...");
			Enumeration instenum = training.enumerateInstances();
			while (instenum.hasMoreElements()) {
				System.out.println(((Instance)instenum.nextElement()).toString()); 
				
			}
			
			
			// Sample classifiers and predictors
			/*
			REPTree tree = new REPTree();
			tree.setMaxDepth(-1);
			tree.setNoPruning(false);
			tree.setMinNum(10);
			tree.setNumFolds(3);
			tree.setMinVarianceProp(0.076);
			*/
			/*
			AdditiveRegression ar = new AdditiveRegression();
			ar.setNumIterations(50);
			ar.setShrinkage(0.5);
			ar.setClassifier(tree);
			*/
			/*
			MultilayerPerceptron net = new MultilayerPerceptron();
			net.setAutoBuild(true);
			net.setDecay(true);
			net.setReset(false);
			net.setNormalizeAttributes(true);
			net.setNormalizeNumericClass(true);
			net.setValidationSetSize(0);
			net.setGUI(false);
			net.setHiddenLayers("19");
			net.setLearningRate(0.5);
			net.setMomentum(0.6);
			net.setTrainingTime(5000);
			*/
			/*
			PaceRegression reg = new PaceRegression();
			SelectedTag st = new SelectedTag(10, PaceRegression.TAGS_ESTIMATOR);
			//System.out.println("Method:"+st.getSelectedTag().getReadable());
			reg.setEstimator(st);
			*/
			/*
			SMO svm = new SMO();
			svm.setNumFolds(-1); // use training data
			svm.setRandomSeed(1);
			svm.setBuildLogisticModels(true);
			RBFKernel rbfKernel = new RBFKernel();
			rbfKernel.setGamma(0.01);
			svm.setKernel(rbfKernel);
			svm.setC(1.0);
			*/
			/*
			J48 j48 = new J48();
			j48.setMinNumObj(2);
			j48.setConfidenceFactor(0.1f);
			j48.setUnpruned(false);
			
			String img;
			
			Hashtable h1 = new Hashtable();
			// define mappings between attributes and imagery
			Attribute b1 = training.attribute("band1");
			img = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/imagery2/0500317_03_ref-temp_utm17_wgs84_b1_subset.tif";
			h1.put(b1, img);
			Attribute b3 = training.attribute("band3");
			img = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/imagery2/0500317_03_ref-temp_utm17_wgs84_b3_subset.tif";
			h1.put(b3, img);
			Attribute b5 = training.attribute("band5");
			img = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/imagery2/0500317_03_ref-temp_utm17_wgs84_b5_subset.tif";
			h1.put(b5, img);
			Attribute b7 = training.attribute("band7");
			img = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/imagery2/0500317_03_ref-temp_utm17_wgs84_b7_subset.tif";
			h1.put(b7, img);
			
			
			ImageClassifier ic = new ImageClassifier(h1, j48, training);
			String outFileName = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/imagery2/j48_20090601.tif";
			//ic.classify(training.attribute("band1"), training, outFileName);
				
			classVals(training);
			*/
			
			// 20090605
			/*
			// training data:
			tFile = new File("E:/cr2009/cr_training/training_20090605.csv");
			CSVLoader loader = new CSVLoader();
			loader.setSource(tFile);
			Instances training = loader.getDataSet();
			
			// set the response
			Attribute response = training.attribute("class");
			training.setClass(response);
			System.out.println("The class attribute is...");
			System.out.println(training.classAttribute().name());
			
			System.out.println("Using these instances...");
			System.out.println(training.toSummaryString());
			
			Hashtable h1 = new Hashtable();
			// define mappings between attributes and imagery
			Attribute b1 = training.attribute("band1");
			String img = "E:/cr2009/imagery3/sierpe_subset_20090604_b1.tif";
			h1.put(b1, img);
			Attribute b2 = training.attribute("band2");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b2.tif";
			h1.put(b2, img);
			Attribute b3 = training.attribute("band3");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b3.tif";
			h1.put(b3, img);
			Attribute b4 = training.attribute("band4");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b4.tif";
			h1.put(b4, img);
			Attribute b5 = training.attribute("band5");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b5.tif";
			h1.put(b5, img);
			Attribute b6 = training.attribute("band6");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b6.tif";
			h1.put(b6, img);
			Attribute b7 = training.attribute("band7");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b7.tif";
			h1.put(b7, img);
			Attribute b8 = training.attribute("band8");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b8.tif";
			h1.put(b8, img);
			Attribute b9 = training.attribute("band9");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b9.tif";
			h1.put(b9, img);

			Attribute b10 = training.attribute("band10");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b10.tif";
			h1.put(b10, img);
			Attribute b13 = training.attribute("band13");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b11.tif";
			h1.put(b13, img);

			Attribute b15 = training.attribute("band15");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b12.tif";
			h1.put(b15, img);
			Attribute b16 = training.attribute("band16");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b13.tif";
			h1.put(b16, img);
			Attribute b17 = training.attribute("band17");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b14.tif";
			h1.put(b17, img);
			Attribute b18 = training.attribute("band18");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b15.tif";
			h1.put(b18, img);
			Attribute b19 = training.attribute("band19");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b16.tif";
			h1.put(b19, img);
			
			Attribute b46 = training.attribute("band46");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b17.tif";
			h1.put(b46, img);
			Attribute b47 = training.attribute("band47");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b18.tif";
			h1.put(b47, img);
			Attribute b48 = training.attribute("band48");
			img = "E:/cr2009/imagery3/sierpe_subset_20090604_b19.tif";
			h1.put(b48, img);
			
			
			J48 j48 = new J48();
			j48.setMinNumObj(2);
			j48.setConfidenceFactor(0.3f);
			j48.setUnpruned(false);
			
			ImageClassifier ic = new ImageClassifier(h1, j48, training);
			String outFileName = "E:/cr2009/cr_image/j48_20090604.tif";
			ic.classify(training.attribute("band1"), training, outFileName);
			System.out.println("J48 class vals:");
			classVals(training);
			
			RandomForest rf = new RandomForest();
			rf.setNumFeatures(0);
			rf.setNumTrees(100);
			
			ic = new ImageClassifier(h1, rf, training);
			outFileName = "E:/cr2009/cr_image/rf100_20090604.tif";
			ic.classify(training.attribute("band1"), training, outFileName);
			System.out.println("Random Forest class vals:");
			classVals(training);
			*/
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

}
