/*
 *  Copyright (C) 2013  Nicholas Clinton
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
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.media.jai.RasterFactory;

import org.gdal.gdal.Dataset;

import com.berkenviro.imageprocessing.ImageClassifier2.Pixel;

import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * 
 * @author Nicholas Clinton
 */
public class ImageClassifier2 {	
	
	// Hashtable with attributes as keys, PlanarImages as values
	Hashtable<Attribute, Dataset> images;
	// the Classifier must have been trained with the same attributes as images
	Classifier classifier;
	Instances training;
	Attribute reference;
	boolean readyToClassify;
	boolean meta;

	BlockingQueue<Pixel> queue; // read, but not processed
	ThreadPoolExecutor service;
	ExecutorCompletionService<Pixel> ecs; // processing
	
	
	/**
	 * 
	 * @param imageFileNames
	 * @param c
	 * @param training
	 */
	public ImageClassifier2 (Hashtable<Attribute, String> imageFileNames, Classifier c, Instances training) {
		this.training = training;
		
		readyToClassify = false;
		// set myP, the instance variable referring to the predictor
		try {
			c.buildClassifier(training);
			classifier = c;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		images = new Hashtable<Attribute, Dataset>(); 

		for (Attribute a : imageFileNames.keySet()) {
			if (a.equals(training.classAttribute())) {
				continue;
			}
			// get the image file names, test for existence
			String imageFileName = imageFileNames.get(a);
			try {
				//System.out.println("Trying to load "+imageFileName);
				images.put(a, GDALUtils.getDataset(imageFileName));
				System.out.println("For Attribute: " + a.toString()+",");
				System.out.println("Loaded:"+images.get(a).GetDescription());
				
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		readyToClassify = true;
	}
	
	
	/**
	 * 
	 * @param reference
	 * @param training
	 * @param outFileName
	 * @param meta
	 */
	public void classify(Attribute reference, Instances training, String outFileName, boolean meta) {

		if (!readyToClassify) {
			System.err.println("Not ready to classify!  System will exit.");
			System.exit(-1);
		}
		
		// use the reference to set up
		Dataset ref = images.get(reference);
		int width = ref.getRasterXSize();
		int height = ref.getRasterYSize();
		System.out.println("width= "+width);
		System.out.println("height= "+height);
	
		WritableRaster classifiedOut = RasterFactory.createBandedRaster(
    											DataBuffer.TYPE_BYTE,
    											width,
    											height,
    											1,
    											new Point(0,0));

		// this is just a dummy dataset to identify the Instance(s)
		Instances imagePixels = new Instances(training, 1);

		for (int y=0; y<height; y++) { // each line
			System.out.println("processing line: "+y);
			for (int x=0; x<width; x++){	// each pixel
//		for (int y=3000; y<4000; y++) { // each line
//			System.out.println("processing line: "+y);
//			for (int x=3000; x<4000; x++){	// each pixel
				int[] xy = {x, y};
				double[] projXY = null;
				try {
					projXY = GDALUtils.getProjectedXY(xy, ref);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				Instance instance = new Instance(training.numAttributes());
				instance.setDataset(imagePixels);
				instance.classIsMissing();
				for (int a=0; a<training.numAttributes(); a++) {
					try {
						double pixelValue = GDALUtils.imageValue(
								images.get(training.attribute(a)), projXY[0], projXY[1], 1
								);
						//System.out.println(pixelValue);	
						if (training.attribute(a).isNumeric()) {
							instance.setValue(training.attribute(a), pixelValue);
						} else {
							instance.setValue(training.attribute(a), String.valueOf((int)pixelValue));
						}
					} catch (Exception e) {
						instance.setMissing(training.attribute(a));
						//e.printStackTrace();
					}
				}
				
				//System.out.println(instance);
				
				// classification, so int (rather than a numeric response, this is an index)
				int classIndex = -1;
				try {
					classIndex = (int) classifier.classifyInstance(instance);
					//System.out.println(instance);
					String prediction = "-1.0";
					if (meta) {
						String predictedAtt = imagePixels.classAttribute().value(classIndex);
						//System.out.println("\t "+predictedAtt);
						Attribute a = imagePixels.attribute(predictedAtt);
						//prediction = instance.attribute(a.index()).value(classIndex);
						double index = instance.value(a);
						//System.out.println("\t\t index: "+index);
						prediction = a.value((int)index);
						//System.out.println("\t\t\t prediction: "+prediction);
					} else {
						prediction = imagePixels.classAttribute().value(classIndex);
						//System.out.println("\t prediction: "+prediction);
					}
					
					classifiedOut.setSample(x,y,0, Byte.parseByte(prediction));
				} catch (Exception e) {
					e.printStackTrace();
					classifiedOut.setSample(x,y,0, -1);
				}
				
			}
		}
		//		 write
		JAIUtils.writeTiff(classifiedOut, outFileName);
	} 
	
	
	/**
	 * New-school, asynchronous.
	 * @param base
	 * @param reference
	 * @param lags
	 */
	public void classifyParallel(Attribute reference, String outFileName, boolean meta, int nThreads) {
		if (!readyToClassify) {
			System.err.println("Not ready to classify!  System will exit.");
			System.exit(-1);
		}
		this.meta = meta;
		this.reference = reference;
		queue = new ArrayBlockingQueue<Pixel>(nThreads);

		//service = (ThreadPoolExecutor)Executors.newFixedThreadPool(nThreads);  // memory leak!!
		//System.out.println(service);
		service = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(nThreads), new ThreadPoolExecutor.DiscardPolicy());
		ecs = new ExecutorCompletionService<Pixel>(service);

		// read thread
		PixelEnumeration enumerator = new PixelEnumeration();
		Thread enumThread = new Thread(enumerator);
		enumThread.start();
		// compute thread
		PixelCompute computer = new PixelCompute();
		Thread compThread = new Thread(computer);
		compThread.start();
		// write thread
		PixelWrite write = new PixelWrite(outFileName);
		Thread writeThread = new Thread(write);
		writeThread.start();
		while (true) {
			if (!enumThread.isAlive() && !compThread.isAlive() && !writeThread.isAlive()) {
				System.exit(0);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * 
	 * @author Nicholas
	 *
	 */
	class Pixel implements Callable<Pixel> {
		
		Instance instance;
		int x, y;
		byte result;
		boolean dummy;
		
		public Pixel(int x, int y, Instance instance) {
			this.x = x;
			this.y = y;
			this.instance = instance;
			dummy = false;
		}
		
		public Pixel() {
			dummy = true;
		}
		
		@Override
		public Pixel call() {
			if (dummy) { return this; }
			// classification, so int (rather than a numeric response, this is an index)
			int classIndex = -1;
			try {
				classIndex = (int) classifier.classifyInstance(instance);
				//System.out.println(instance);
				String prediction = "-1.0";
				if (meta) {
					String predictedAtt = training.classAttribute().value(classIndex);
					Attribute a = training.attribute(predictedAtt);
					prediction = instance.attribute(a.index()).value(classIndex);
				} else {
					prediction = training.classAttribute().value(classIndex);
				}
				//System.out.println("\t prediction: "+prediction);
				result = Byte.parseByte(prediction);
			} catch (Exception e) {
				//e.printStackTrace();
				result = -1;
			}
			return this;
		}
		
		@Override
		public String toString() {
			return "("+x+","+y+") : "+instance+" : "+result;
		}	
	}
	
	
	/**
	 * 
	 * @author Nicholas
	 *
	 */
	class PixelEnumeration implements Runnable {
		
		@Override
		public void run() {
			try {
				ennumerate();
				System.out.println("Inserting dummy to queue...");
				queue.put(new Pixel()); // DUMMY
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * 
		 * @throws InterruptedException
		 */
		public void ennumerate() throws InterruptedException, Exception {
			
			// use the reference to set up
			Dataset ref = images.get(reference);
			int width = ref.getRasterXSize();
			int height = ref.getRasterYSize();
			System.out.println("width= "+width);
			System.out.println("height= "+height);
		
			// this is just a dummy dataset to identify the Instance(s)
			Instances imagePixels = new Instances(training, 1);

			double[] projXY = null;
			for (int y=0; y<height; y++) { // each line
				System.out.println("processing line: "+y);
				for (int x=0; x<width; x++){	// each pixel
//			for (int y=3500; y<3600; y++) { // each line
//				System.out.println("processing line: "+y);
//				for (int x=3500; x<3600; x++){	// each pixel
					try {
						projXY = GDALUtils.getProjectedXY(new int[] {x, y}, ref);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					Instance instance = new Instance(training.numAttributes());
					instance.setDataset(imagePixels);
					instance.classIsMissing();
					for (int a=0; a<training.numAttributes(); a++) {
						try {
							// Exception on class attribute
							double pixelValue = GDALUtils.imageValue(
									images.get(training.attribute(a)), projXY[0], projXY[1], 1
									);
							//System.out.println(pixelValue);	
							if (training.attribute(a).isNumeric()) {
								instance.setValue(training.attribute(a), pixelValue);
							} else {
								instance.setValue(training.attribute(a), String.valueOf((int)pixelValue));
							}
						} catch (Exception e) {
							instance.setMissing(training.attribute(a));
							//e.printStackTrace();
						}
					}
					queue.put(new Pixel(x, y, instance));
				}
			}
		}
		
	}
	
	/**
	 * 
	 * @author Nicholas
	 *
	 */
	class PixelCompute implements Runnable {
		
		@Override
		public void run() {
			try {
				compute();
				System.out.println("Submitting dummy to completion service...");
				ecs.submit(new Pixel()); // DUMMY
				service.shutdown();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Simply move the pixels from the read queue to the completion service.
		 * @throws InterruptedException
		 * @throws Exception
		 */
		public void compute() throws InterruptedException, ExecutionException {
			Pixel pix;
			while (true) {
				// take off the first queue, read, but unprocessed
				pix = queue.take();
				//System.out.println("\t queue size: "+queue.size());

				if (pix.dummy) {
					System.out.println("Found read dummy...");
					break; // done, don't move the dummy over
				}
				ecs.submit(pix);
			}
		}
	} // end compute class
	
	
	/**
	 * 
	 * @author Nicholas
	 *
	 */
	class PixelWrite implements Runnable {
		
		WritableRaster classifiedOut;
		String outFileName;
		
		/**
		 * 
		 * @param base is the base name of the output images
		 */
		public PixelWrite(String outFileName) {
			this.outFileName = outFileName;
			classifiedOut = RasterFactory.createBandedRaster(
	    						DataBuffer.TYPE_BYTE,
	    						images.get(reference).getRasterXSize(),
	    						images.get(reference).getRasterYSize(),
	    						1,
	    						new Point(0,0)
	    					);
		}

		/**
		 * 
		 */
		@Override
		public void run() {
			try {
				write();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * 
		 * @throws InterruptedException
		 * @throws ExecutionException
		 */
		public void write() throws InterruptedException, ExecutionException {

			while (true) {
				//System.out.println("queue: "+queue.size());

				Pixel finishedPix = ecs.take().get();
				
				if (finishedPix.dummy) { // DUMMY
					System.out.println("Found compute dummy...");
					break;
				}
				classifiedOut.setSample(finishedPix.x, finishedPix.y, 0, finishedPix.result);
				//System.out.println(finishedPix);
			}
			diskWrite();
		}
		
		/**
		 * 
		 */
		private void diskWrite() {
			JAIUtils.writeTiff(classifiedOut, outFileName);
		}
		
	}
	
	
	/**
	 * Test Code and Processing Log.
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			
			// 20130519, 20130610
			// training data:
			//String tFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/AllMethodAcc_Detailed_nominalized.arff";
			String tFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/AllMethodAcc_Detailed_nominalized_meta.arff";
			Instances training = WekaUtils.loadArff(tFileName);

			// set the response
			//training.setClassIndex(2);
			training.setClassIndex(8);
			System.out.println("The class attribute is...");
			System.out.println(training.classAttribute().name());

//			System.out.println("Using these instances...");
			System.out.println(training.toSummaryString());

			Hashtable<Attribute, String> h1 = new Hashtable<Attribute, String>();
			// define mappings between attributes and imagery
			Attribute b1 = training.attribute("J48");
			String img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4-J48.tif";
			h1.put(b1, img);
			Attribute b2 = training.attribute("MLC");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4-MLC.tif";
			h1.put(b2, img);
			Attribute b3 = training.attribute("RF");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4-RF.tif";
			h1.put(b3, img);
			Attribute b4 = training.attribute("SVM");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4-SVM.tif";
			h1.put(b4, img);
			Attribute b5 = training.attribute("Seg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4-Seg.tif";
			h1.put(b5, img);
			Attribute b6 = training.attribute("Agg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4_Agg.tif";
			h1.put(b6, img);
			// x and y
			Attribute b7 = training.attribute("Lon");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4_long.tif";
			h1.put(b7, img);
			Attribute b8 = training.attribute("Lat");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4_lat.tif";
			h1.put(b8, img);

			J48 j48 = new J48();

			ImageClassifier2 ic = new ImageClassifier2(h1, j48, training);
			//String outFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4_meta_sync.tif";
			//ic.classify(training.attribute("J48"), training, outFileName, false);
			//ic.classifyParallel(training.attribute("J48"), outFileName, false, 10);
			
			String outFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4_meta_label_sync.tif";
			ic.classify(training.attribute("J48"), training, outFileName, true);
			GDALUtils.transferGeo(img, outFileName);
			
			
			
			// 20130610
			// ****************************************************************************************************************
			tFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/AllMethodAcc_Detailed_nominalized.arff";
			training = WekaUtils.loadArff(tFileName);

			// set the response
			training.setClassIndex(2);
			System.out.println("The class attribute is...");
			System.out.println(training.classAttribute().name());

//			System.out.println("Using these instances...");
			System.out.println(training.toSummaryString());

			h1 = new Hashtable<Attribute, String>();
			// define mappings between attributes and imagery
			b1 = training.attribute("J48");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_J48.tif";
			h1.put(b1, img);
			b2 = training.attribute("MLC");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_MLC.tif";
			h1.put(b2, img);
			b3 = training.attribute("RF");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_RF.tif";
			h1.put(b3, img);
			b4 = training.attribute("SVM");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_SVM.tif";
			h1.put(b4, img);
			b5 = training.attribute("Seg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE-Seg.tif";
			h1.put(b5, img);
			b6 = training.attribute("Agg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE-Agg.tif";
			h1.put(b6, img);
			// x and y
			b7 = training.attribute("Lon");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_long.tif";
			h1.put(b7, img);
			b8 = training.attribute("Lat");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_lat.tif";
			h1.put(b8, img);

			j48 = new J48();

			ic = new ImageClassifier2(h1, j48, training);
			outFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_meta_sync.tif";
			ic.classify(training.attribute("J48"), training, outFileName, false);
			GDALUtils.transferGeo(img, outFileName);
			
			// LABEL****************************************************************************************************************
			tFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/AllMethodAcc_Detailed_nominalized_meta.arff";
			training = WekaUtils.loadArff(tFileName);

			// set the response
			training.setClassIndex(8);
			System.out.println("The class attribute is...");
			System.out.println(training.classAttribute().name());

			System.out.println(training.toSummaryString());

			h1 = new Hashtable<Attribute, String>();
			// define mappings between attributes and imagery
			b1 = training.attribute("J48");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_J48.tif";
			h1.put(b1, img);
			b2 = training.attribute("MLC");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_MLC.tif";
			h1.put(b2, img);
			b3 = training.attribute("RF");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_RF.tif";
			h1.put(b3, img);
			b4 = training.attribute("SVM");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_SVM.tif";
			h1.put(b4, img);
			b5 = training.attribute("Seg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE-Seg.tif";
			h1.put(b5, img);
			b6 = training.attribute("Agg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE-Agg.tif";
			h1.put(b6, img);
			// x and y
			b7 = training.attribute("Lon");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_long.tif";
			h1.put(b7, img);
			b8 = training.attribute("Lat");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_lat.tif";
			h1.put(b8, img);

			j48 = new J48();

			ic = new ImageClassifier2(h1, j48, training);

			outFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_meta_label_sync.tif";
			ic.classify(training.attribute("J48"), training, outFileName, true);
			GDALUtils.transferGeo(img, outFileName);
			
			
			
			// ****************************************************************************************************************
			tFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/AllMethodAcc_Detailed_nominalized.arff";
			training = WekaUtils.loadArff(tFileName);

			// set the response
			training.setClassIndex(2);
			System.out.println("The class attribute is...");
			System.out.println(training.classAttribute().name());

			//						System.out.println("Using these instances...");
			System.out.println(training.toSummaryString());

			h1 = new Hashtable<Attribute, String>();
			// define mappings between attributes and imagery
			b1 = training.attribute("J48");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_J48.tif";
			h1.put(b1, img);
			b2 = training.attribute("MLC");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_MLC.tif";
			h1.put(b2, img);
			b3 = training.attribute("RF");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_RF.tif";
			h1.put(b3, img);
			b4 = training.attribute("SVM");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_SVM.tif";
			h1.put(b4, img);
			b5 = training.attribute("Seg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE-Seg.tif";
			h1.put(b5, img);
			b6 = training.attribute("Agg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE-Agg.tif";
			h1.put(b6, img);
			// x and y
			b7 = training.attribute("Lon");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_long.tif";
			h1.put(b7, img);
			b8 = training.attribute("Lat");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_lat.tif";
			h1.put(b8, img);

			j48 = new J48();

			ic = new ImageClassifier2(h1, j48, training);
			outFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_meta_sync.tif";
			ic.classify(training.attribute("J48"), training, outFileName, false);
			GDALUtils.transferGeo(img, outFileName);
			

			// LABEL****************************************************************************************************************
			tFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/AllMethodAcc_Detailed_nominalized_meta.arff";
			training = WekaUtils.loadArff(tFileName);

			// set the response
			training.setClassIndex(8);
			System.out.println("The class attribute is...");
			System.out.println(training.classAttribute().name());

			System.out.println(training.toSummaryString());

			h1 = new Hashtable<Attribute, String>();
			// define mappings between attributes and imagery
			b1 = training.attribute("J48");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_J48.tif";
			h1.put(b1, img);
			b2 = training.attribute("MLC");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_MLC.tif";
			h1.put(b2, img);
			b3 = training.attribute("RF");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_RF.tif";
			h1.put(b3, img);
			b4 = training.attribute("SVM");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_SVM.tif";
			h1.put(b4, img);
			b5 = training.attribute("Seg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE-Seg.tif";
			h1.put(b5, img);
			b6 = training.attribute("Agg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE-Agg.tif";
			h1.put(b6, img);
			// x and y
			b7 = training.attribute("Lon");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_long.tif";
			h1.put(b7, img);
			b8 = training.attribute("Lat");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_lat.tif";
			h1.put(b8, img);

			j48 = new J48();

			ic = new ImageClassifier2(h1, j48, training);

			outFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_meta_label_sync.tif";
			ic.classify(training.attribute("J48"), training, outFileName, true);
			GDALUtils.transferGeo(img, outFileName);
			

			// ****************************************************************************************************************
			tFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/AllMethodAcc_Detailed_nominalized.arff";
			training = WekaUtils.loadArff(tFileName);

			// set the response
			training.setClassIndex(2);
			System.out.println("The class attribute is...");
			System.out.println(training.classAttribute().name());

			//						System.out.println("Using these instances...");
			System.out.println(training.toSummaryString());

			h1 = new Hashtable<Attribute, String>();
			// define mappings between attributes and imagery
			b1 = training.attribute("J48");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_J48.tif";
			h1.put(b1, img);
			b2 = training.attribute("MLC");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_MLC.tif";
			h1.put(b2, img);
			b3 = training.attribute("RF");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_RF.tif";
			h1.put(b3, img);
			b4 = training.attribute("SVM");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_SVM.tif";
			h1.put(b4, img);
			b5 = training.attribute("Seg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE-Seg.tif";
			h1.put(b5, img);
			b6 = training.attribute("Agg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE-Agg.tif";
			h1.put(b6, img);
			// x and y
			b7 = training.attribute("Lon");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_long.tif";
			h1.put(b7, img);
			b8 = training.attribute("Lat");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_lat.tif";
			h1.put(b8, img);

			j48 = new J48();

			ic = new ImageClassifier2(h1, j48, training);
			outFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_meta_sync.tif";
			ic.classify(training.attribute("J48"), training, outFileName, false);
			GDALUtils.transferGeo(img, outFileName);
			

			// LABEL****************************************************************************************************************
			tFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/AllMethodAcc_Detailed_nominalized_meta.arff";
			training = WekaUtils.loadArff(tFileName);

			// set the response
			training.setClassIndex(8);
			System.out.println("The class attribute is...");
			System.out.println(training.classAttribute().name());

			System.out.println(training.toSummaryString());

			h1 = new Hashtable<Attribute, String>();
			// define mappings between attributes and imagery
			b1 = training.attribute("J48");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_J48.tif";
			h1.put(b1, img);
			b2 = training.attribute("MLC");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_MLC.tif";
			h1.put(b2, img);
			b3 = training.attribute("RF");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_RF.tif";
			h1.put(b3, img);
			b4 = training.attribute("SVM");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_SVM.tif";
			h1.put(b4, img);
			b5 = training.attribute("Seg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE-Seg.tif";
			h1.put(b5, img);
			b6 = training.attribute("Agg");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE-Agg.tif";
			h1.put(b6, img);
			// x and y
			b7 = training.attribute("Lon");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_long.tif";
			h1.put(b7, img);
			b8 = training.attribute("Lat");
			img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_lat.tif";
			h1.put(b8, img);

			j48 = new J48();

			ic = new ImageClassifier2(h1, j48, training);

			outFileName = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_meta_label_sync.tif";
			ic.classify(training.attribute("J48"), training, outFileName, true);
			GDALUtils.transferGeo(img, outFileName);
			
						
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

}
