/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.descriptive.moment.VectorialCovariance;

import ru.sscc.spline.Spline;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 * 20130809 Latest version running on server
 */
public class Correlatr {

	Loadr eviLoadr;
	Loadr persiannLoadr;
	int[] lags;
	BlockingQueue<Pixel> pixels; // reusable objects
	BlockingQueue<Pixel> queue; // read, but not processed
	ThreadPoolExecutor service;
	ExecutorCompletionService<Pixel> ecs; // processing
	PlanarImage ref;

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
			persiannLoadr.setDateZero(cal);
		}
	}

	/**
	 * 
	 */
	public void close() {
		eviLoadr.close();
		persiannLoadr.close();
	}

	/**
	 * t-values should be relative to the same date.  Use interpolating splines.
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
	 * Use interpolating splines.
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
		TreeMap<Integer, Double> map = new TreeMap<Integer, Double>();
		for (double[] tc : covariate) {
			map.put((int)tc[0], tc[1]);
		}
		//

		VectorialCovariance cov = new VectorialCovariance(2, false);
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
					Double cDouble = map.get((int)t-lags[l]);
					if (cDouble == null) { // t was a no data point
						continue;
					}
					//System.out.println(t+","+r+","+c);
					cov.increment(new double[] {r, cDouble.doubleValue()});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// not enough data
			if (cov.getN() < 5) {
				return new double[] {0, 0};
			}
			// the variance/covariance matrix
			RealMatrix vc = cov.getResult();
			// if either variable is constant...
			if (vc.getEntry(0, 0) == 0 || vc.getEntry(1, 1) == 0) {
				return new double[] {0, 0};
			}
			// normalize by SD
			correlations[l] = vc.getEntry(0, 1)/Math.sqrt(vc.getEntry(0, 0)*vc.getEntry(1, 1));
			//System.out.println(correlations[l]);
		}
		int max = weka.core.Utils.maxIndex(correlations); // might be negative
		if (correlations[max] < 0) {
			int min = weka.core.Utils.minIndex(correlations);
			return new double[] {correlations[min], lags[min]};
		}
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
		JAIUtils.writeTiff(corr, base+"_corr.tif");
		JAIUtils.writeTiff(days, base+"_days.tif");
		JAIUtils.writeTiff(eviN, base+"_eviN.tif");
	}

	/**
	 * New-school, asynchronous.
	 * @param base
	 * @param reference
	 * @param lags
	 */
	public void writeImagesParallel(String base, String reference, int[] lags, int nThreads) {
		this.lags = lags;

		// object pool of pixels
		pixels = new ArrayBlockingQueue<Pixel>(3*nThreads);
		for (int p=0; p<3*nThreads; p++) {
			pixels.add(new Pixel(false));
		}
		queue = new ArrayBlockingQueue<Pixel>(nThreads);
		//ecs = new ExecutorCompletionService<Pixel>(
		//		Executors.newFixedThreadPool(nThreads));
		service = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, 
				new ArrayBlockingQueue<Runnable>(nThreads), 
				new ThreadPoolExecutor.DiscardPolicy());
		ecs = new ExecutorCompletionService<Pixel>(service);
		ref = JAIUtils.readImage(reference);
		// read thread
		PixelEnumeration enumerator = new PixelEnumeration();
		new Thread(enumerator).start();
		// compute thread
		PixelCompute computer = new PixelCompute();
		new Thread(computer).start();
		// write thread
		PixelWrite write = new PixelWrite(base);
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
		boolean dummy;

		//		public Pixel(Future<List<double[]>> eviF, Future<List<double[]>> persiannF, int x, int y) throws Exception {
		//			this.x = x;
		//			this.y = y;
		//			if (eviF != null && persiannF != null) {  // could happen if DUMMY
		//				persiann = persiannF.get();
		//				evi = eviF.get();
		//				//System.out.println("Initialized "+this);
		//			}
		//		}

		public Pixel() {
			dummy = true;
		}

		public Pixel(boolean dummy) {
			this.dummy = dummy;
		}

		public void set(List<double[]> evi, List<double[]> persiann, int x, int y) {
			this.evi = evi;
			this.persiann = persiann;
			this.x = x;
			this.y = y;
			dummy = false;
		}

		public void clear() {
			evi = null;
			persiann = null;
			correlation = null;
			x = -1;
			y = -1;
		}

		@Override
		public Pixel call() {
			// DUMMY, nothing to do
			if (evi==null && persiann==null) {
				return this; 
			}
			// not enough data
			if (evi.size() < 5 || persiann.size() < 5) { 
				correlation = new double[] {0, 0}; 
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
			return "("+x+","+y+"): "+Arrays.toString(correlation)+", n="+evi.size();
		}	
	}

	/**
	 * 
	 * @author Nicholas
	 *
	 */
	class ReadRunner implements Runnable {

		double x;
		double y;
		Loadr loadr;
		List<double []> list;

		public ReadRunner(double x, double y, Loadr loadr) {
			this.x = x;
			this.y = y;
			this.loadr = loadr;
		}

		public ReadRunner() {
			// wait for instance variables to be set
		}

		@Override
		public void run() {
			list = loadr.getSeries(x, y);
		}
	}

	/**
	 * A Thread that does the reading.  Loadr read methods are synchronized.
	 * @author Nicholas
	 *
	 */
	class PixelEnumeration implements Runnable {
		ExecutorService es;
		RandomIter iter;

		/**
		 * 
		 * @param queue
		 * @param reference
		 */
		public PixelEnumeration() {
			//service = Executors.newFixedThreadPool(2);
			es = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(2), new ThreadPoolExecutor.DiscardPolicy());
			iter = RandomIterFactory.create(ref, null);
		}

		@Override
		public void run() {
			try {
				ennumerate();
				//queue.put(new Pixel(null, null, -1, -1)); // DUMMY
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
			// center coords
			double delta = 0.008365178679831331;
			// center
			double ulx = -179.9815962788889; 
			double uly = 69.99592926598972;
			// dimensions of the reference land mask
			int width = ref.getWidth();
			int height = ref.getHeight();

			ReadRunner eviReader = new ReadRunner();
			ReadRunner persiannReader = new ReadRunner();
			Future eviF, persiannF;

			for (int x=0; x<width; x++) {
				for (int y=0; y<height; y++) {
					//			for (int x=888; x<15358; x++) { // North America
					//				for (int y=700; y<12372; y++) {
					//			for (int x=6500; x<13000; x++) { // US
					//				for (int y=2500; y<5500; y++) {
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

					//					final Point eviPt = GISUtils.makePoint(_x, _y);
					//					Future<List<double[]>> eviF = service.submit(new Callable<List<double[]>>() {
					//						@Override
					//						public List<double[]> call() throws Exception {
					//							return eviLoadr.getSeries(eviPt);
					//						}
					//					});
					//					final Point persiannPt = GISUtils.makePoint((_x < 0 ? _x+360.0 : _x), _y);
					//					Future<List<double[]>> persiannF = service.submit(new Callable<List<double[]>>() {
					//						@Override
					//						public List<double[]> call() throws Exception {
					//							return persiannLoadr.getSeries(persiannPt);
					//						}
					//					});

					eviReader.x = _x;
					eviReader.y = _y;
					eviReader.loadr = eviLoadr;
					persiannReader.x = (_x < 0 ? _x+360.0 : _x);
					persiannReader.y = _y;
					persiannReader.loadr = persiannLoadr;
					// This should run serially if the data are stored on one disk
					// don't want futures hanging around?
					//eviF = service.submit(eviReader);
					//persiannF = service.submit(persiannReader);
					//while (!(eviF.isDone() && persiannF.isDone())) {
					//	Thread.currentThread().sleep(0, 1); // wait for a nanosecond
					//}
					es.submit(eviReader).get();
					es.submit(persiannReader).get();

					// re-use these pixels
					Pixel p = pixels.take();
					p.set(eviReader.list, persiannReader.list, x, y);
					//System.out.println("read "+p);
					queue.put(p);
				}
			}
			// shut 'er down
			es.shutdown();
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

		WritableRaster corr, days, eviN;
		String base;

		/**
		 * 
		 * @param base is the base name of the output images
		 */
		public PixelWrite (String base) {
			this.base = base;
			// initialize outputs
			int width = ref.getWidth();
			int height = ref.getHeight();
			corr = RasterFactory.createBandedRaster(
					DataBuffer.TYPE_INT,
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
			long now = System.currentTimeMillis();
			while (true) {
				//System.out.println("queue: "+queue.size());
				//Future<Pixel> futurePix = ecs.take();
				Pixel finishedPix = ecs.take().get();
				//if (finishedPix.x == -1 && finishedPix.y == -1) { // DUMMY
				if (finishedPix.dummy) { // DUMMY
					break;
				}
				writeCorr((finishedPix.correlation[0]*100), finishedPix);
				writeDays((byte)finishedPix.correlation[1], finishedPix);
				writeN(finishedPix.evi.size(), finishedPix);
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
				// re-use the finished pixel
				finishedPix.clear();
				pixels.add(finishedPix);
			}
			diskWrite();
		}

		/**
		 * 
		 */
		private void diskWrite() {
			JAIUtils.writeTiff(corr, base+"_corr.tif");
			JAIUtils.writeTiff(days, base+"_days.tif");
			JAIUtils.writeTiff(eviN, base+"_eviN.tif");
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

		Correlatr corr = null;
		try {
			corr = new Correlatr(
					new String[] {"/home/nclinton/MOD13A2/2010/", 
					"/home/nclinton/MOD13A2/2011/"}, 
					new String[] {"/home/nclinton/PERSIANN/2010/",
							"/home/nclinton/PERSIANN/2011/"
					},
					new int[] {2010, 0, 1}
					);
			String reference = "/home/nclinton/land_mask.tif";
			//String base = "/mnt/usb1/evi_persiann";
			String base = "/home/nclinton/evi_persiann";
			int[] lags = {0,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100,105,110,115,120};
			//int[] lags = {0,5,10,15,20,25,30,35,40,45,50,55,60};
			corr.writeImagesParallel(base, reference, lags, 100);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}