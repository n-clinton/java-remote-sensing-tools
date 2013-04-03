/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.descriptive.moment.VectorialCovariance;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import ru.sscc.spline.Spline;

/**
 * @author Nicholas
 *
 */
public class Correlatr {

	Loadr eviLoadr;
	Loadr persiannLoadr;
	int[] lags;
	
	/**
	 * 
	 * @param eviDirectories
	 * @param persiannDirectories
	 * @param ymd is {year, month, day} where month is zero-referenced
	 * @throws Exception
	 */
	public Correlatr(String[] eviDirectories, String[] persiannDirectories, int[] ymd) throws Exception {
		eviLoadr = new ImageLoadr2(eviDirectories);
		persiannLoadr = new PERSIANNLoadr(persiannDirectories);
		Calendar cal = Calendar.getInstance();
		cal.set(ymd[0], ymd[1], ymd[2]); 
		if (ymd != null) {
			System.out.println("Setting zero-reference to: "+cal.getTime());
			eviLoadr.setDateZero(cal);
			eviLoadr.setDateZero(cal);
		}
	}
	
	/**
	 * t-values should be relative to the same date.
	 * @param response
	 * @param covariate
	 * @param lag
	 * @return
	 */
	public static double correlation(List<double[]> response, List<double[]> covariate, int lag) {
		VectorialCovariance cov = new VectorialCovariance(2, false);
		double min = response.get(0)[0]; // minumum X
		double max = response.get(response.size()-1)[0];
		// fit splines
		double[][] xy = TSUtils.getSeriesAsArray(response);
		Spline rSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		xy = TSUtils.getSeriesAsArray(covariate);
		Spline cSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		// compute covariance
		for (double t=min; t<=max; t++) { // daily step
			if (t-lag<0 || t-lag>covariate.get(covariate.size()-1)[0]) {
				continue; // don't go out of bounds
			}
			try {
				//System.out.println(t+","+r+","+c);
				cov.increment(new double[] {rSpline.value(t), cSpline.value(t-lag)});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		RealMatrix vc = cov.getResult();
		// normalize by SD
		return vc.getEntry(0, 1)/Math.sqrt(vc.getEntry(0, 0)*vc.getEntry(1, 1));
	}
	
	/**
	 * 
	 * @param response
	 * @param covariate
	 * @return
	 */
	public double[] maxCorrelation1(List<double[]> response, List<double[]> covariate, int[] lags) {
		VectorialCovariance cov = new VectorialCovariance(2, false);
		RealMatrix vc = null; // the variance/covariance matrix
		double[] correlations = new double[lags.length];
		double t0 = response.get(0)[0]; // minumum t
		double tn = response.get(response.size()-1)[0]; // max t
		// fit splines
		double[][] xy = TSUtils.getSeriesAsArray(response);
		Spline rSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		xy = TSUtils.getSeriesAsArray(covariate);
		Spline cSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		for (int l=0; l<lags.length; l++) {
			cov.clear();
			// compute covariance
			for (double t=t0; t<=tn; t++) { // daily step
				if (t-lags[l]<0 || t-lags[l]>covariate.get(covariate.size()-1)[0]) {
					continue; // don't go out of bounds
				}
				try {
					//System.out.println(t+","+r+","+c);
					cov.increment(new double[] {rSpline.value(t), cSpline.value(t-lags[l])});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			vc = cov.getResult();
			// normalize by SD
			correlations[l] = vc.getEntry(0, 1)/Math.sqrt(vc.getEntry(0, 0)*vc.getEntry(1, 1));
			//System.out.println(correlations[l]);
		}
		int max = weka.core.Utils.maxIndex(correlations); // might be negative
		return new double[] {correlations[max], lags[max]};
	}
	
	/**
	 * This version is meant for daily PERSIANN data, for which it makes no sense to fit a spline.
	 * @param response
	 * @param covariate
	 * @return
	 */
	public double[] maxCorrelation(List<double[]> response, List<double[]> covariate, int[] lags) {
		// 
		double[] covariates = new double[covariate.size()];
		Arrays.fill(covariates, -9999.0);
		for (int i=0; i<covariates.length; i++) {
			double[] tc = covariate.get(i);
			covariates[(int)tc[0]] = tc[1];
		}
		//
		
		VectorialCovariance cov = new VectorialCovariance(2, false);
		RealMatrix vc = null; // the variance/covariance matrix
		double[] correlations = new double[lags.length];
		double t0 = response.get(0)[0]; // minumum t
		double tn = response.get(response.size()-1)[0]; // max t
		// fit a spline to the EVI response
		double[][] xy = TSUtils.getSeriesAsArray(response);
		Spline rSpline = TSUtils.duchonSpline(xy[0], xy[1]);

		for (int l=0; l<lags.length; l++) {
			cov.clear();
			// compute covariance
			for (double t=t0; t<=tn; t++) { // daily step
				if (t-lags[l]<0 || t-lags[l]>covariate.get(covariate.size()-1)[0]) {
					continue; // don't go out of bounds
				}
				try {
					double r = rSpline.value(t);
					if (r < 0) { // want only EVI greater than zero
						continue;
					}
					double c = covariates[(int)t];
					if (c < 0) { // skip no data value: -9999.0
						continue;
					}
					//System.out.println(t+","+r+","+c);
					cov.increment(new double[] {r, c});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			vc = cov.getResult();
			// normalize by SD
			correlations[l] = vc.getEntry(0, 1)/Math.sqrt(vc.getEntry(0, 0)*vc.getEntry(1, 1));
			//System.out.println(correlations[l]);
		}
		int max = weka.core.Utils.maxIndex(correlations); // might be negative
		return new double[] {correlations[max], lags[max]};
	}
	
	/**
	 * Old-school, synchronous way.
	 * @param EVIloadr
	 * @param PERSIANNloadr
	 * @param base
	 */
	public void writeImages(String base, String reference, int[] lags) {
		// dimensions of the EVI images
		int width = 43032;
		int height = 15539;
		// center coords
		double delta = 0.008365178679831331;
		// center
		double ulx = -179.9815962788889; 
		double uly = 69.99592926598972;
		
		// outputs, written as unsigned bytes
		WritableRaster corr = RasterFactory.createBandedRaster(
    			DataBuffer.TYPE_BYTE,
    			width,
    			height,
    			1, // bands
    			new java.awt.Point(0,0)); // origin
		WritableRaster days = RasterFactory.createBandedRaster(
				DataBuffer.TYPE_BYTE,
				width,
				height,
				1, // bands
				new java.awt.Point(0,0)); // origin
		WritableRaster eviN = RasterFactory.createBandedRaster(
				DataBuffer.TYPE_BYTE,
				width,
				height,
				1, // bands
				new java.awt.Point(0,0)); // origin
		
		PlanarImage ref = JAIUtils.readImage(reference);
		RandomIter iterator = RandomIterFactory.create(ref, null);
		
//		for (int x=888; x<15358; x++) { // North America
//			for (int y=700; y<12372; y++) {
		for (int x=888; x<2000; x++) {
			for (int y=700; y<2000; y++) {
				double _x = ulx+x*delta;
				double _y = uly-y*delta;
				//System.out.println(x+","+y);
				//System.out.println(_x+","+_y);
				if (Math.abs(_y) > 60.0) {
					//System.out.println("PERSIANN out of bounds.");
					continue; // outside bounds of PERSIANN
					
				}
				if (iterator.getSample(x, y, 0) == 0) {
					//System.out.println("Not land.");
					continue; // not land
				}

				Point eviPt = GISUtils.makePoint(_x, _y);
				List<double[]> evi = eviLoadr.getSeries(eviPt);
				if(evi.size() < 5) { // arbitrary cutoff
					continue;
				}
				Point persiannPt = GISUtils.makePoint((_x < 0 ? _x+360.0 : _x), _y);
				List<double[]> precip = persiannLoadr.getSeries(persiannPt);
				if(precip.size() < 5) { // arbitrary cutoff
					continue;
				}
				List<double[]> persiann = persiannLoadr.getSeries(persiannPt);
				double[] correlation = maxCorrelation(evi, persiann, lags);
				// 10*correlation, integer part
				corr.setSample(x, y, 0, (correlation[0]*100));
				days.setSample(x, y, 0, correlation[1]);
				eviN.setSample(x, y, 0, evi.size());
				System.out.println(x+","+y+","+(correlation[0])+","+correlation[1]+","+evi.size());	
			}
		}
		JAIUtils.writeTiff(corr, base+"_corr.tif", DataBuffer.TYPE_BYTE);
		JAIUtils.writeTiff(days, base+"_days.tif", DataBuffer.TYPE_BYTE);
		JAIUtils.writeTiff(eviN, base+"_eviN.tif", DataBuffer.TYPE_BYTE);
	}
	
	/**
	 * New-school, asynchronous.
	 * @param base
	 * @param reference
	 * @param lags
	 */
	public void writeImagesParallel(String base, String reference, int[] lags, int nThreads) {
		this.lags = lags;
		int qSize = 100;
		BlockingQueue<Pixel> queue1 = new ArrayBlockingQueue<Pixel>(qSize);
		PlanarImage ref = JAIUtils.readImage(reference);
		// read thread
		PixelEnumeration enumerator = new PixelEnumeration(queue1, ref);
		new Thread(enumerator).start();
		// compute thread
		ExecutorCompletionService<Pixel> ecs = new ExecutorCompletionService<Pixel>(
				Executors.newFixedThreadPool(nThreads));
		PixelCompute computer = new PixelCompute(queue1, ecs);
		new Thread(computer).start();
		// write thread
		PixelWrite write = new PixelWrite(ecs, base, ref.getWidth(), ref.getHeight());
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
		List<double[]> evi, persiann;
		int x, y;
		double[] correlation;
		
		public Pixel(Future<List<double[]>> eviF, Future<List<double[]>> persiannF, int x, int y) throws Exception {
			this.x = x;
			this.y = y;
			if (eviF != null && persiannF != null) {  // could happen if DUMMY
				evi = eviF.get();
				persiann = persiannF.get();
				//System.out.println("Initialized "+this);
			}
		}
		
		@Override
		public Pixel call() {
			// DUMMY, nothing to do
			if (evi==null && persiann==null) {
				return this; 
			}
			// not enough data
			if (evi.size() < 5 || persiann.size() < 5) { 
				correlation = new double[] {0, -1}; 
			}
			// do the computation
			else {
				correlation = maxCorrelation(evi, persiann, lags);
			}
			//System.out.println("\t called "+this);
			return this;
		}
		@Override
		public String toString() {
			return "("+x+","+y+")";
		}
	}
	
	
	/**
	 * A Thread that does the reading.  Loadr read methods are synchronized.
	 * @author Nicholas
	 *
	 */
	class PixelEnumeration implements Runnable {
		ExecutorService service;
		BlockingQueue<Pixel> queue;
		RandomIter iter;
		
		/**
		 * 
		 * @param queue
		 * @param reference
		 */
		public PixelEnumeration(BlockingQueue<Pixel> queue, PlanarImage ref) {
			this.queue = queue;
			service = Executors.newFixedThreadPool(2);
			iter = RandomIterFactory.create(ref, null);
		}
		
		@Override
		public void run() {
			try {
				ennumerate();
				queue.put(new Pixel(null, null, -1, -1)); // DUMMY
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
			// center coords
			double delta = 0.008365178679831331;
			// center
			double ulx = -179.9815962788889; 
			double uly = 69.99592926598972;
			// dimensions of the EVI images
			int width = 43032;
			int height = 15539;
			
			for (int x=0; x<width; x++) {
				for (int y=0; y<height; y++) {
//			for (int x=888; x<15358; x++) { // North America
//				for (int y=700; y<12372; y++) {
					double _x = ulx+x*delta;
					double _y = uly-y*delta;
					//System.out.println(x+","+y);
					//System.out.println(_x+","+_y);
					if (Math.abs(_y) > 60.0) {
						//System.out.println("PERSIANN out of bounds.");
						continue; // outside bounds of PERSIANN
						
					}
					if (iter.getSample(x, y, 0) == 0) {
						//System.out.println("Not land.");
						continue; // not land
					}

					final Point eviPt = GISUtils.makePoint(_x, _y);
					Future<List<double[]>> eviF = service.submit(new Callable<List<double[]>>() {
						@Override
						public List<double[]> call() throws Exception {
							return eviLoadr.getSeries(eviPt);
						}
					});
					final Point persiannPt = GISUtils.makePoint((_x < 0 ? _x+360.0 : _x), _y);
					Future<List<double[]>> persiannF = service.submit(new Callable<List<double[]>>() {
						@Override
						public List<double[]> call() throws Exception {
							return persiannLoadr.getSeries(persiannPt);
						}
					});
					queue.put(new Pixel(eviF, persiannF, x, y));
				}
			}
			// shut 'er down
			service.shutdown();
		}
	}
	
	
	/**
	 * A Thread that initiates the correlation calculation.
	 * @author Nicholas
	 *
	 */
	class PixelCompute implements Runnable {
		BlockingQueue<Pixel> queue1;
		ExecutorCompletionService<Pixel> ecs;
		
		/**
		 * 
		 * @param queue
		 * @param reference
		 */
		public PixelCompute(BlockingQueue<Pixel> queue1, 
				ExecutorCompletionService<Pixel> ecs) {
			this.queue1 = queue1;
			this.ecs = ecs; 
		}
		
		/**
		 * 
		 * @throws InterruptedException
		 * @throws Exception
		 */
		public void compute() throws InterruptedException, ExecutionException {
			while (true) {
				// take off the first queue, read, but unprocessed
				Pixel pix = queue1.take();
				if (pix.x == -1 && pix.y == -1) { // DUMMY
					break; // done, don't move the dummy over
				}
				ecs.submit(pix);
			}
		}
		
		@Override
		public void run() {
			try {
				compute();
				ecs.submit(new Pixel(null, null, -1, -1)); // DUMMY
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	} // end compute class
	
	
	/**
	 * A Thread that runs the writing operation.  Operations on the databuffers are synchronized.
	 * @author Nicholas
	 *
	 */
	class PixelWrite implements Runnable {
		
		ExecutorCompletionService<Pixel> ecs;
		WritableRaster corr, days, eviN;
		String base;
		
		/**
		 * 
		 * @param queue
		 * @param base
		 * @param lags
		 * @param width
		 * @param height
		 */
		public PixelWrite (ExecutorCompletionService<Pixel> ecs, String base, int width, int height) {
			this.ecs = ecs;
			this.base = base;
			// initialize outputs
			corr = RasterFactory.createBandedRaster(
	    			DataBuffer.TYPE_FLOAT,
	    			width,
	    			height,
	    			1, // bands
	    			new java.awt.Point(0,0)); // origin
			days = RasterFactory.createBandedRaster(
					DataBuffer.TYPE_BYTE,
					width,
					height,
					1, // bands
					new java.awt.Point(0,0)); // origin
			eviN = RasterFactory.createBandedRaster(
					DataBuffer.TYPE_BYTE,
					width,
					height,
					1, // bands
					new java.awt.Point(0,0)); // origin
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
			int interval = 1000000; // frequency of write
			int counter = 0;
			while (true) {
				//System.out.println("queue: "+queue.size());
				Pixel finishedPix = ecs.take().get();
				if (finishedPix.x == -1 && finishedPix.y == -1) { // DUMMY
					break;
				}
				writeCorr((finishedPix.correlation[0]), finishedPix);
				writeDays(finishedPix.correlation[1], finishedPix);
				writeN(finishedPix.evi.size(), finishedPix);
				System.out.println(finishedPix.x+","+finishedPix.y+","+
				(finishedPix.correlation[0])+","+
					finishedPix.correlation[1]+","+
						finishedPix.evi.size());
				// periodically write to disk
				counter++;
				if (counter > interval) {
					diskWrite();
					counter = 0;
				}
			}
			diskWrite();
		}
		
		/**
		 * 
		 */
		private void diskWrite() {
			JAIUtils.writeTiff(corr, base+"_corr.tif", DataBuffer.TYPE_FLOAT);
			JAIUtils.writeTiff(days, base+"_days.tif", DataBuffer.TYPE_BYTE);
			JAIUtils.writeTiff(eviN, base+"_eviN.tif", DataBuffer.TYPE_BYTE);
		}
		
		public synchronized void writeCorr(double val, Pixel pix) {
			corr.setSample(pix.x, pix.y, 0, val);
		}
		public synchronized void writeDays(double val, Pixel pix) {
			days.setSample(pix.x, pix.y, 0, val);
		}
		public synchronized void writeN(double val, Pixel pix) {
			eviN.setSample(pix.x, pix.y, 0, val);
		}
		
	} // end writing class
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		try {
			
			// TEST:
//			Calendar cal = Calendar.getInstance();
//			cal.set(2010, 0, 1); // January 1, 2010
//			ImageLoadr2 EVIloadr = new ImageLoadr2(
//				new String[] {
//					"I:/MOD13A2/2010/", 
//					"I:/MOD13A2/2011/"
//				}
//			);
//			EVIloadr.setDateZero(cal);
//			PERSIANNLoadr PERSIANNloadr = new PERSIANNLoadr(
//				new String[] {
//					"C:/Users/Public/Documents/PERSIANN/8km_daily/2010",
//					"C:/Users/Public/Documents/PERSIANN/8km_daily/2011"
//				}
//			);
//			PERSIANNloadr.setDateZero(cal);
//			Point pt1 = GISUtils.makePoint(-121.0, 38.0);
//			List<double[]> evi = EVIloadr.getSeries(pt1);
			// see C:\Users\Nicholas\Documents\global_pca\evi_pixel.txt
//			for (double[] datapoint : evi) {
//				System.out.println(datapoint[0]+","+datapoint[1]);
//			}
//			System.out.println();
//			Point pt2 = GISUtils.makePoint((-121.0+360.0), 38.0);
//			List<double[]> precip = PERSIANNloadr.getSeries(pt2);
			// see C:\Users\Nicholas\Documents\global_pca\persiann_pixel.txt
//			for (double[] datapoint : precip) {
//				System.out.println(datapoint[0]+","+datapoint[1]);
//			}
//			System.out.println(correlation(evi, precip, 0));
//			System.out.println(correlation(evi, precip, 5));
//			System.out.println(correlation(evi, precip, 10));
//			System.out.println(correlation(evi, precip, 15));
//			System.out.println(correlation(evi, precip, 20));
//			System.out.println(correlation(evi, precip, 25));
//			System.out.println(correlation(evi, precip, 30));
//			System.out.println(correlation(evi, precip, 35));
//			System.out.println(correlation(evi, precip, 40));
//			System.out.println(correlation(evi, precip, 45));
//			System.out.println(correlation(evi, precip, 50));
//			System.out.println(correlation(evi, precip, 55));
//			System.out.println();
//			long now = System.currentTimeMillis();
//			double[] corr = maxCorrelation(evi, precip);
//			long later = System.currentTimeMillis();
//			System.out.println("Took "+(later-now)+" milliseconds");
//			System.out.println(corr[0]+", "+corr[1]);
			
			// check
//			String reference = "C:/Users/Nicholas/Documents/global_phenology/land_mask.tif";
//			JAIUtils.describeGeoTiff(reference);
//			PlanarImage im = JAIUtils.readImage(reference);
//			double[] xy = JAIUtils.getProjectedXY(new int[] {0,0}, im);
//			System.out.println(xy[0]+","+xy[1]);
//			System.out.println(JAIUtils.imageValue(pt1, im));
			
			// 20121125
//			Correlatr corr = new Correlatr(new String[] {"I:/MOD13A2/2010/", 
//														 "I:/MOD13A2/2011/"}, 
//										   new String[] {"C:/Users/Public/Documents/PERSIANN/8km_daily/2010",
//														 "C:/Users/Public/Documents/PERSIANN/8km_daily/2011"},
//										   new int[] {2010, 0, 1}); 
			
			// from 7th floor machine:
			// 20121203 multi-threaded
//			Correlatr corr = new Correlatr(new String[] {
//					"H:/Nick_data/MOD13A2/2010/", 
//			 		"H:/Nick_data/MOD13A2/2011/"}, 
//			 								new String[] {
//					"G:/Nick_data/PERSIANN/2010",
//			 		"G:/Nick_data/PERSIANN/2011"},
//			 								new int[] {2010, 0, 1});
//			String reference = "H:/Nick_data/land_mask.tif";
//			String base = "H:/Nick_data/evi_persiann";
//			int[] lags = {0,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90};
//			//int[] lags = {0,5,10,15,20,25,30,35,40,45,50,55,60};
//			long now = System.currentTimeMillis();
//			corr.writeImagesParallel(base, reference, lags, 100);
//			long later = System.currentTimeMillis();
//			System.out.println("time: "+(later-now));
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}


	}

}