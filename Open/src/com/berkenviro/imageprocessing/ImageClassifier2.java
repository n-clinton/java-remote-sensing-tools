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
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import javax.media.jai.RasterFactory;

import org.gdal.gdal.Dataset;

import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
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
public class ImageClassifier2 {	
	
	// Hashtable with attributes as keys, PlanarImages as values
	Hashtable<Attribute, Dataset> images;
	// the Classifier must have been trained with the same attributes as images
	Classifier classifier;
	Instances training;
	Attribute reference;
	boolean readyToClassify;
	boolean meta;
	
	BlockingQueue<Pixel> pixels; // reusable objects
	BlockingQueue<Pixel> queue; // read, but not processed
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
				images.put(a, GDALUtils.getDataset(imageFileName));
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
						int pixelValue = (int)GDALUtils.imageValue(
								images.get(training.attribute(a)), projXY[0], projXY[1], 1
								);
						//System.out.println(pixelValue);	
						if (training.attribute(a).isNumeric()) {
							instance.setValue(training.attribute(a), pixelValue);
						} else {
							instance.setValue(training.attribute(a), String.valueOf(pixelValue));
						}
					} catch (Exception e) {
						instance.setMissing(training.attribute(a));
						//e.printStackTrace();
					}
				}

				// classification, so int (rather than a numeric response, this is an index)
				int classIndex = -1;
				try {
					classIndex = (int) classifier.classifyInstance(instance);
					//System.out.println(instance);
					String prediction = "-1.0";
					if (meta) {
						String predictedAtt = imagePixels.classAttribute().value(classIndex);
						Attribute a = imagePixels.attribute(predictedAtt);
						prediction = instance.attribute(a.index()).value(classIndex);
					} else {
						prediction = imagePixels.classAttribute().value(classIndex);
					}
					//System.out.println("\t prediction: "+prediction);
					classifiedOut.setSample(x,y,0, Byte.parseByte(prediction));
				} catch (Exception e) {
					e.printStackTrace();
					classifiedOut.setSample(x,y,0, -1);
				}
				
			}
		}
		//		 write
		System.out.println("Writing Tiff: "+outFileName);
		JAIUtils.writeTiff(classifiedOut, outFileName);
	} 
	
	
	/**
	 * New-school, asynchronous.
	 * @param base
	 * @param reference
	 * @param lags
	 */
	public void classifyParallel(Attribute reference, Instances training, String outFileName, boolean meta, int nThreads) {
		if (!readyToClassify) {
			System.err.println("Not ready to classify!  System will exit.");
			System.exit(-1);
		}
		this.meta = meta;
		this.reference = reference;
		queue = new ArrayBlockingQueue<Pixel>(nThreads);
		ecs = new ExecutorCompletionService<Pixel>(
				Executors.newFixedThreadPool(nThreads));

		// read thread
		PixelEnumeration enumerator = new PixelEnumeration();
		new Thread(enumerator).start();
		// compute thread
		PixelCompute computer = new PixelCompute();
		new Thread(computer).start();
		// write thread
		PixelWrite write = new PixelWrite(outFileName);
		new Thread(write).start();
	}
	
	
	/**
	 * A Pixel object that holds time series.  
	 * As soon as it is instantiated, it starts the read operations in other threads.
	 * When called, it computes the correlation between the series.
	 * @author Nicholas
	 *
	 */
	class Pixel implements Callable<Pixel> {
		
		Instance instance;
		int x, y;
		byte result;
		boolean dummy;
		
		public Pixel(int x, int y, Instance instance) {
			this.instance = instance;
			dummy = false;
		}
		
		public Pixel() {
			dummy = true;
		}
		
		@Override
		public Pixel call() {
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
				e.printStackTrace();
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
	 * A Thread that does the reading.  Loadr read methods are synchronized.
	 * @author Nicholas
	 *
	 */
	class PixelEnumeration implements Runnable {
		
		/**
		 * 
		 * @param queue
		 * @param reference
		 */
		public PixelEnumeration() {}
		
		@Override
		public void run() {
			try {
				ennumerate();
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

			for (int y=0; y<height; y++) { // each line
				System.out.println("processing line: "+y);
				for (int x=0; x<width; x++){	// each pixel
					
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
							int pixelValue = (int)GDALUtils.imageValue(
									images.get(training.attribute(a)), projXY[0], projXY[1], 1
									);
							//System.out.println(pixelValue);	
							if (training.attribute(a).isNumeric()) {
								instance.setValue(training.attribute(a), pixelValue);
							} else {
								instance.setValue(training.attribute(a), String.valueOf(pixelValue));
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
	 * A Thread that initiates the correlation calculation.
	 * @author Nicholas
	 *
	 */
	class PixelCompute implements Runnable {
		
		public PixelCompute() {}
		
		@Override
		public void run() {
			try {
				compute();
				ecs.submit(new Pixel()); // DUMMY
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

				if (pix.dummy) { // DUMMY
					break; // done, don't move the dummy over
				}
				ecs.submit(pix);
			}
		}
	} // end compute class
	
	
	/**
	 * A Thread that runs the writing operation.  Operations on the databuffers are synchronized.
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
			int interval = 100000; // frequency of write
			int counter = 0;
			long now = System.currentTimeMillis();
			while (true) {
				//System.out.println("queue: "+queue.size());
				//Future<Pixel> futurePix = ecs.take();
				Pixel finishedPix = ecs.take().get();
				//if (finishedPix.x == -1 && finishedPix.y == -1) { // DUMMY
				if (finishedPix.dummy) { // DUMMY
					break;
				}
				classifiedOut.setSample(finishedPix.x, finishedPix.y, 0, finishedPix.result);
				//System.out.println(finishedPix);
				// periodically write to disk
				counter++;
				if (counter > interval) {
					System.out.println(Calendar.getInstance().getTime());
					System.out.println("\t Free memory: "+Runtime.getRuntime().freeMemory());
					System.out.println("\t Max memory: "+Runtime.getRuntime().maxMemory());
					System.out.println("\t Last pixel written: "+finishedPix);
					System.out.println("\t Time per pixel: "+((System.currentTimeMillis() - now)/interval));

					System.runFinalization();
					System.gc();
					System.gc();
					System.gc();
					diskWrite();

					counter = 0;
					now = System.currentTimeMillis();

				}
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
			
			// 20130519
			// training data:
			String tFileName = "/Users/nclinton/Documents/data/test3/AllMethodAcc_Detailed_nominalized.arff";
			Instances training = WekaUtils.loadArff(tFileName);

			// set the response
			Attribute response = training.attribute("class");
			training.setClassIndex(2);
			System.out.println("The class attribute is...");
			System.out.println(training.classAttribute().name());

			System.out.println("Using these instances...");
			System.out.println(training.toSummaryString());

			Hashtable h1 = new Hashtable();
			// define mappings between attributes and imagery
			Attribute b1 = training.attribute("J48");
			String img = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4-J48.tif";
			h1.put(b1, img);
			Attribute b2 = training.attribute("MLC");
			img = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4-MLC.tif";
			h1.put(b2, img);
			Attribute b3 = training.attribute("RF");
			img = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4-RF.tif";
			h1.put(b3, img);
			Attribute b4 = training.attribute("SVM");
			img = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4-SVM.tif";
			h1.put(b4, img);
			Attribute b5 = training.attribute("Seg");
			img = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4-Seg.tif";
			h1.put(b5, img);
			Attribute b6 = training.attribute("Agg");
			img = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4_Agg.tif";
			h1.put(b6, img);
			// x and y
			Attribute b7 = training.attribute("Lon");
			img = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4_x.tif";
			h1.put(b7, img);
			Attribute b8 = training.attribute("Lat");
			img = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4_y.tif";
			h1.put(b8, img);

			J48 j48 = new J48();

			ImageClassifier2 ic = new ImageClassifier2(h1, j48, training);
			String outFileName = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4_meta.tif";
			ic.classify(training.attribute("J48"), training, outFileName, false);

		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

}
