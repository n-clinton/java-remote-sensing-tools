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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.distribution.TDistributionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.descriptive.moment.VectorialCovariance;

import ru.sscc.spline.Spline;
import cn.edu.tsinghua.lidar.BitChecker;
import cn.edu.tsinghua.modis.BitCheck;

import com.berkenviro.imageprocessing.JAIUtils;

/**
 * @author Nicholas Clinton
 * @author Cong Hui He
 * 
 * 
 */
public class Correlatr {
	Loadr responseLoadr;
	Loadr predictLoadr;
	int[] lags;
	BlockingQueue<Pixel> pixels; // reusable objects
	BlockingQueue<Pixel> queue; // read, but not processed
	ThreadPoolExecutor service;
	ExecutorCompletionService<Pixel> ecs; // processing
	PlanarImage ref;
	boolean maxCorrInterp;
	
	/**
	 * 
	 * @param response
	 * @param predictor
	 * @param ymd is {year, month, day} where month is zero-referenced
	 */
	public Correlatr(Loadr response, Loadr predictor, int[] ymd) {
		responseLoadr = response;
		predictLoadr = predictor;
		Calendar cal = Calendar.getInstance();
		cal.set(ymd[0], ymd[1], ymd[2]);
		if (ymd != null) {
			System.out.println("Setting zero-reference to: " + cal.getTime());
			responseLoadr.setDateZero(cal);
			predictLoadr.setDateZero(cal);
		}
	}

	/**
	 * 
	 */
	public void close() {
		responseLoadr.close();
		predictLoadr.close();
	}

	/**
	 * Use interpolating splines.
	 * 
	 * @param response
	 * @param covariate
	 * @param lag
	 * @return
	 */
//	public static double correlation(List<double[]> response,
//			List<double[]> covariate, int lag) {
//		VectorialCovariance cov = new VectorialCovariance(2, false);
//		double min = response.get(0)[0]; // minumum X
//		double max = response.get(response.size() - 1)[0];
//		// fit splines
//		double[][] xy = TSUtils.getSeriesAsArray(response);
//		Spline rSpline = TSUtils.duchonSpline(xy[0], xy[1]);
//		xy = TSUtils.getSeriesAsArray(covariate);
//		Spline cSpline = TSUtils.duchonSpline(xy[0], xy[1]);
//		// compute covariance
//		for (double t = min; t <= max; t++) { // daily step
//			if (t - lag < 0 || t - lag > covariate.get(covariate.size() - 1)[0]) {
//				continue; // don't go out of bounds
//			}
//			try {
//				// System.out.println(t+","+r+","+c);
//				cov.increment(new double[] { rSpline.value(t),
//						cSpline.value(t - lag) });
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		RealMatrix vc = cov.getResult();
//		// normalize by SD
//		return vc.getEntry(0, 1)
//				/ Math.sqrt(vc.getEntry(0, 0) * vc.getEntry(1, 1));
//	}

	/**
	 * Use interpolating splines.  Response must be greater than zero.
	 * 
	 * @param response
	 * @param covariate
	 * @return
	 */
	public double[] maxCorrelationInterpolated(List<double[]> response,
			List<double[]> covariate, int[] lags) {
		VectorialCovariance cov = new VectorialCovariance(2, false);
		RealMatrix vc = null; // the variance/covariance matrix
		double[] correlations = new double[lags.length];
		double[] n = new double[lags.length];
		
		double t0 = response.get(0)[0]; // minumum t
		double tn = response.get(response.size() - 1)[0]; // max t
		// fit splines
		double[][] xy = TSUtils.getSeriesAsArray(response);
		Spline rSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		xy = TSUtils.getSeriesAsArray(covariate);
		Spline cSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		for (int l = 0; l < lags.length; l++) {
			cov.clear();
			// compute covariance
			for (double t = t0; t <= tn; t++) { // daily step
				if (t - lags[l] < 0
						|| t - lags[l] > covariate.get(covariate.size() - 1)[0]) {
					continue; // don't go out of bounds
				}
				try {
					double r = rSpline.value(t);
					if (r < 0) { // want only EVI greater than zero
						continue;
					}
					double c = cSpline.value(t - lags[l]);
					// System.out.println(t+","+r+","+c);
					cov.increment(new double[] { r, c });
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			vc = cov.getResult();
			// normalize by SD
			correlations[l] = vc.getEntry(0, 1)
					/ Math.sqrt(vc.getEntry(0, 0) * vc.getEntry(1, 1));
			// System.out.println(correlations[l]);
			n[l] = cov.getN();
		}
		int max = weka.core.Utils.maxIndex(correlations); // might be negative
		if (correlations[max] < 0) {
			int min = weka.core.Utils.minIndex(correlations);
			return new double[] { correlations[min], lags[min], n[min]};
		}
		return new double[] { correlations[max], lags[max], n[max]};
	}

	/**
	 * This version is meant for daily predictor data (PERSIANN), for which it makes no
	 * sense to fit a spline.  The response (EVI) must be greater than zero and n>=5.
	 * 
	 * @param response
	 * @param covariate
	 * @return
	 */
	public double[] maxCorrelation(List<double[]> response,
			List<double[]> covariate, int[] lags) {
		// insert the predictor data to a TreeMap, with key=time
		TreeMap<Integer, Double> map = new TreeMap<Integer, Double>();
		for (double[] tc : covariate) {
			map.put((int) tc[0], tc[1]);
		}

		VectorialCovariance cov = new VectorialCovariance(2, false);
		double[] correlations = new double[lags.length];
		double[] n = new double[lags.length];
		double t0 = response.get(0)[0]; // minumum t
		double tn = response.get(response.size() - 1)[0]; // max t
		// fit a spline to the EVI response
		double[][] xy = TSUtils.getSeriesAsArray(response);
		Spline rSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		for (int l = 0; l < lags.length; l++) {
			cov.clear();
			// compute covariance
			for (double t = t0; t <= tn; t++) { // daily step
				if (t - lags[l] < 0
						|| t - lags[l] > covariate.get(covariate.size() - 1)[0]) {
					continue; // don't go out of bounds
				}
				try {
					// this will sometimes throw an Exception if the spline can't be created
					double r = rSpline.value(t);
					if (r < 0) { // want only EVI greater than zero
						continue;
					}
					Double cDouble = map.get((int) t - lags[l]);
					if (cDouble == null) { // t was a no data point
						continue;
					}
					// System.out.println(t+","+r+","+c);
					cov.increment(new double[] { r, cDouble.doubleValue() });
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}

			// the variance/covariance matrix
			RealMatrix vc = cov.getResult();
			// if either variable is constant...
			if (vc.getEntry(0, 0) == 0 || vc.getEntry(1, 1) == 0) {
				//System.err.println("\t No variance.");
				return new double[] { 0, 0, 0};
			}
			// normalize by SD
			correlations[l] = vc.getEntry(0, 1)
					/ Math.sqrt(vc.getEntry(0, 0) * vc.getEntry(1, 1));
			// System.out.println(correlations[l]);
			n[l] = (int)cov.getN();
		}
		int max = weka.core.Utils.maxIndex(correlations); // might be negative
		if (correlations[max] < 0) {
			int min = weka.core.Utils.minIndex(correlations);
			return new double[] { correlations[min], lags[min], n[min]};
		}
		return new double[] { correlations[max], lags[max], n[max]};
	}


	/**
	 * 
	 * @param base is the base filename for output files
	 * @param reference is the pixel reference.  An output will be written at each reference pixel == 1.
	 * @param lags is an array of integer lags to test
	 * @param nThreads is the number of compute threads to use
	 * @param maxCorrInterp if true will interpolate the predictor variable
	 */
	public void writeImagesParallel(String base, String reference, int[] lags,
			int nThreads, boolean maxCorrInterp) {
		this.lags = lags;
		this.maxCorrInterp = maxCorrInterp;

		// object pool of pixels
		int numPixels = 5*nThreads;
		pixels = new ArrayBlockingQueue<Pixel>(numPixels);
		for (int p = 0; p < numPixels; p++) {
			pixels.add(new Pixel(false));
		}
		queue = new ArrayBlockingQueue<Pixel>(nThreads);
		// ecs = new ExecutorCompletionService<Pixel>(Executors.newFixedThreadPool(nThreads));
		service = new ThreadPoolExecutor(nThreads, nThreads, 0L,
				TimeUnit.MILLISECONDS, 
				new ArrayBlockingQueue<Runnable>(nThreads), 
				new ThreadPoolExecutor.DiscardPolicy());
		ecs = new ExecutorCompletionService<Pixel>(service);
		ref = JAIUtils.readImage(reference);
		JAIUtils.register(ref);

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
	 * 
	 * @author Nicholas
	 * 
	 */
	class Pixel implements Callable<Pixel> {

		List<double[]> response, covariate;
		int x, y;
		double[] correlation;
		boolean dummy;

		public Pixel(boolean dummy) {
			this.dummy = dummy;
		}

		public void set(List<double[]> response, List<double[]> covariate, int x,
				int y) {
			this.response = response;
			this.covariate = covariate;
			this.x = x;
			this.y = y;
			dummy = false;
		}

		public void clear() {
			response = null;
			covariate = null;
			correlation = null;
			x = -1;
			y = -1;
		}

		@Override
		public Pixel call() {
			long start = System.nanoTime();
			// DUMMY, nothing to do
			if (dummy) {
				return this;
			}
			// sort of arbitrary, but if size=0, below throws an exception and the write thread will stop
			if (response.size() < 23 || covariate.size() < 23) {
				correlation = new double[] { 0, 0, 0};
				return this;
			}
			// not enough data check
			boolean gap = false;
			double[] response0 = response.get(0);
			for (int t=1; t<response.size(); t++) {
				double[] responset = response.get(t);
				if ((responset[0]-response0[0]) > 64) { // 4 consecutive, missing 16-day composites
					gap = true;
				}
				response0 = responset;
			}
			double[] cov0 = covariate.get(0);
			for (int t=1; t<covariate.size(); t++) {
				double[] covt = covariate.get(t);
				if ((covt[0]-cov0[0]) > 32) { // 4 consecutive, missing 8-day composites
					gap = true;
				}
				cov0 = covt;
			}
			
			if (gap) {
				//System.err.println("\t Not enough data: response n="+response.size()+", covariate n="+covariate.size());
				correlation = new double[] { 0, 0, 0};
				return this;
			}

			// do the computation
			if (maxCorrInterp) {
				correlation = maxCorrelationInterpolated(response, covariate, lags);
			} else {
				correlation = maxCorrelation(response, covariate, lags);
			}
			
			// Assume the Spline functions are free to vary everywhere except the control points.
			// therefore df = N - (N - responseN - covariateN) - 2, where 2 is for the means?
			// some of the data points may not be used (due to the lag), 
			// therefore subtract lag/16 (16-day composites), 
			// although the data outside the lag still constrain the fit, so perhaps this is conservative?
			//double df = response.size() + covariate.size() - 2.0 - correlation[1]/16.0 - correlation[1]; // PERSIANN
			double df = response.size() + covariate.size() - 2.0 - correlation[1]/16.0 - correlation[1]/8.0; // MYD11
			double t = correlation[0] * Math.sqrt(df / (1-Math.pow(correlation[0], 2)));
			// compare:
			//double z = 0.5*Math.log((1.0 + correlation[0]) / (1.0 - correlation[0]))*Math.sqrt(df-1.0);
			// replace the cov.N with a p-value
			if (df < 0) {
				correlation[2] = 0.5; // undefined
			}
			else {
				TDistributionImpl tDist = new TDistributionImpl(df);
				//NormalDistributionImpl zDist = new NormalDistributionImpl();
				double p = 0;
				try {
					p = tDist.cumulativeProbability(t);
					// quick check: t is bigger
					//System.out.println("\t\t T-dist: "+(1.0-p)+" Normal dist: "+(1.0-zDist.cumulativeProbability(t)));
				} catch (MathException e) {
					e.printStackTrace();
				}
				if (correlation[0] < 0) {
					correlation[2] = p;
				}
				else {
					correlation[2] = 1.0 - p;
				}
			}
			
			long stop = System.nanoTime();
			double elapsed = (double)(stop - start) / 1000.0;
			//System.out.println("\t compute time: " + elapsed);
			return this;
		}

		@Override
		public String toString() {
			return "(" + x + "," + y + "): " + Arrays.toString(correlation)
					+ ", n=" + response.size()+ ", n=" + covariate.size();
		}
	}

	/**
	 * A Runnable reading object.
	 * @author Nicholas
	 * 
	 */
	class ReadRunner implements Runnable {
		double x;
		double y;
		Loadr loadr;
		List<double[]> list;

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
			long start = System.nanoTime();
			list = loadr.getSeries(x, y);
			long stop = System.nanoTime();
			//System.out.println("\t read time: " + (double)(stop - start) / 1000.0);
			//print();
		}

		private void print() {
			for (int i = 0; i < list.size(); i++) {
				// print the array
				System.out.println(Arrays.toString(list.get(i)));
			}
			System.out.println();
		}
	}

	/**
	 * A Thread that does the reading.
	 * 
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
			// two read threads, but only one is used
			es = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
					new ArrayBlockingQueue<Runnable>(2),
					new ThreadPoolExecutor.DiscardPolicy());
			iter = RandomIterFactory.create(ref, null);
		}

		@Override
		public void run() {
			long start = System.currentTimeMillis();
			try {
				enumerate();
				System.out.println("Adding a dummy pixel into the queue...");
				// when we finish enumeration, we add a dummy  Pixel into the queue
				// to mark the end of the program
				queue.put(new Pixel(true)); // DUMMY
				System.out.println("Finished enumerating.");
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			long stop = System.currentTimeMillis();
			long time_elapsed_millis = stop - start;
			System.out.println("Time used: " + time_elapsed_millis + " milliseconds");
			System.out.println("Read Times: " + PERSIANNFile.READ_TIMES);
		}

		/**
		 * 
		 * @throws InterruptedException
		 */
		public void enumerate() {
			// center coords
//			final double delta = 0.008365178679831331;
			final double delta = (Double) ref.getProperty("deltaX");
			// center
//			final double ulx = -179.9815962788889;
//			final double uly = 69.99592926598972;
			double ulx = (Double) ref.getProperty("ulX") + (Double) ref.getProperty("deltaX") / 2.0;
			double uly = (Double) ref.getProperty("ulY") + (Double) ref.getProperty("deltaY") / 2.0;
			
			ReadRunner responseReader = new ReadRunner();
			ReadRunner covariateReader = new ReadRunner();

			// North America
//			int width_begin = 888;
//			int width_end = 15358;
//			int height_begin = 700;
//			int height_end = 12372;
			// US
			int width_begin = 6500;
			int width_end = 13000;
			int height_begin = 2500;
			int height_end = 5500;

//			int width_begin = 0;
//			int width_end = ref.getWidth();
//			int height_begin = 0;
//			int height_end = ref.getHeight();
			
			for (int  y = height_begin; y < height_end; y++) {
				for (int x = width_begin; x < width_end; x++) {
					double _x = ulx + x * delta;
					double _y = uly - y * delta;
					// System.out.println(x+","+y);
					// System.out.println(_x+","+_y);
					if (Math.abs(_y) > 60.0) {
						//System.out.println("PERSIANN out of bounds.");
						continue; // outside bounds of PERSIANN
					}
					if (iter.getSample(x, y, 0) == 0) {
//						System.out.println("Not land.");
						continue; // not land
					}

					responseReader.x = _x;
					responseReader.y = _y;
					responseReader.loadr = responseLoadr;
					// covariateReader.x = (_x < 0 ? _x + 360.0 : _x); PERSIANN
					covariateReader.x = _x; // LST
					covariateReader.y = _y;
					covariateReader.loadr = predictLoadr;

					try {
						// watch out for memory leak here
						// also watch out for simultaneous reads
						Future f1 = es.submit(responseReader); 
						Future f2 = es.submit(covariateReader);
						while (true) {
							if (f1.isDone() && f2.isDone()) {
								break;
							}
						}
						// synchronous reads make sense when the data are on the same physical disk (raid5)?
//						es.submit(responseReader).get();
//						es.submit(covariateReader).get();
						// re-use these pixels
						Pixel p = pixels.take();
						//System.out.println("\t\t pixels pool: "+pixels.size());
						p.set(responseReader.list, covariateReader.list, x, y);
						queue.put(p);
						//System.out.println("\t\t queue size: "+queue.size());
					} catch (InterruptedException  e) {
						e.printStackTrace();
					}
				}
			}
			// shut 'er down
			es.shutdown();
			System.out.println("The ExecutorService has been issued a shutdown command.");
			try {
			if (!es.awaitTermination(120, TimeUnit.SECONDS)) {
				System.err.println("Pool did not terminate");
			}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * A Thread that initiates the correlation calculation.
	 * 
	 * @author Nicholas
	 * 
	 */
	class PixelCompute implements Runnable {

		public PixelCompute() {}

		@Override
		public void run() {
			try {
				compute();
				System.out.println("Finished computing.");
				ecs.submit(new Pixel(true)); // DUMMY
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
		 * 
		 * @throws InterruptedException
		 * @throws Exception
		 */
		public void compute() throws InterruptedException, ExecutionException {
			Pixel pix;
			while (true) {
				// take off the first queue, read, but unprocessed
				pix = queue.take();
				if (pix.dummy) { // DUMMY
					break; // done, exit the loop
				}
				ecs.submit(pix);
			}
		}
	} // end compute class

	/**
	 * A Thread that runs the writing operation. Operations on the databuffers
	 * are synchronized.
	 * 
	 * @author Nicholas
	 * 
	 */
	class PixelWrite implements Runnable {

		WritableRaster corr, days, p;
		String base;

		/**
		 * 
		 * @param base is the base name of the output images
		 */
		public PixelWrite(String base) {
			this.base = base;
			// initialize outputs
			int width = ref.getWidth();
			int height = ref.getHeight();
			corr = RasterFactory.createBandedRaster(DataBuffer.TYPE_SHORT, // signed short
					width, height, 1, // bands
					new java.awt.Point(0, 0)); // origin
			days = RasterFactory.createBandedRaster(DataBuffer.TYPE_SHORT, // signed short
					width, height, 1, // bands
					new java.awt.Point(0, 0)); // origin
			p = RasterFactory.createBandedRaster(DataBuffer.TYPE_BYTE, // unsigned byte
					width, height, 1, // bands
					new java.awt.Point(0, 0)); // origin
		}

		/**
		 * 
		 */
		@Override
		public void run() {
			write();
			System.out.println("Finished writing!!");
		}

		/**
		 * 
		 * @throws InterruptedException
		 * @throws ExecutionException
		 */
		public void write() {
			int interval = 1000000; // frequency of write, original value 10000000
			int counter = 0;
			long now = System.currentTimeMillis();
			while (true) {
				Pixel finishedPix;
				try {
					finishedPix = ecs.take().get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					finishedPix = new Pixel(false);
					finishedPix.correlation = new double[] {0, 0, 0};
				}
				
				if (finishedPix.dummy) { // DUMMY
					break;
				}
				
				//System.out.println(finishedPix);

				writeCorr((short)(255*finishedPix.correlation[0]), finishedPix);
				writeDays((short) finishedPix.correlation[1], finishedPix);
				writeP((byte)(255*finishedPix.correlation[2]), finishedPix);

				// periodically write to disk
				counter++;
				if (counter > interval) {
					System.out.println(Calendar.getInstance().getTime());
					System.out.println("\t Free memory: "
							+ Runtime.getRuntime().freeMemory());
					System.out.println("\t Max memory: "
							+ Runtime.getRuntime().maxMemory());
					System.out.println("\t Last pixel written: " + finishedPix);
					System.out.println("\t Time per pixel: "
							+ ((System.currentTimeMillis() - now) / interval));

					// write it to the file in case of disaster
					diskWrite();
					counter = 0;
					now = System.currentTimeMillis();
				}

				// re-use the finished pixel
				finishedPix.clear();
				pixels.add(finishedPix);
			}
			diskWrite();
			service.shutdown();
			try {
				if (!service.awaitTermination(120, TimeUnit.SECONDS)) {
					System.err.println("The service did not terminate");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 
		 */
		private void diskWrite() {
			JAIUtils.writeTiff(corr, base + "_corr.tif");
			JAIUtils.writeTiff(days, base + "_days.tif");
			JAIUtils.writeTiff(p, base + "_p.tif");
		}

		public synchronized void writeCorr(double val, Pixel pix) {
			corr.setSample(pix.x, pix.y, 0, val);
		}

		public synchronized void writeDays(double val, Pixel pix) {
			days.setSample(pix.x, pix.y, 0, val);
		}

		public synchronized void writeP(double val, Pixel pix) {
			p.setSample(pix.x, pix.y, 0, val);
		}

	} // end writing class

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 201310017 From Haohuan's server.  Ran in two separate runs:
//		Correlatr corr = null;
//		try {
//			corr = new Correlatr(new String[] { "/home/nick/MOD13A2/2010",
//			"/home/nick/MOD13A2/2011" }, new String[] {
//					"/home/nick/PERSIANN/2010/", "/home/nick/PERSIANN/2011/" },
//					new int[] { 2010, 0, 1 });
//			String reference = "/home/nick/workspace/CLINTON/lib/dfg/land_mask.tif";
//			String base = "/home/nick/workspace/CLINTON/Open/result/evi_persiann_";
//			int[] lags = { 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60,
//					65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120 };
//			corr.writeImagesParallel(base, reference, lags, 10); // well, 10 is a quite reasonable argument
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// File 1: evi_persiann_126_corr
		// File 2: evi_persiann_3_corr
		// This construction method is now deprecated, to decouple construction from the type of imagery
		// Also, this run never finished processing South America
		
//		Correlatr corr = null;
//		try {
//			// EVI vegetation index response
//			String[] evi = new String[] {"/home/nick/MOD13A2/2010", "/home/nick/MOD13A2/2011"};
//			String eviDir = "EVI";
//			String eviQCDir = "VI_QC";
//			BitCheck mod13Checker = new BitCheck() {
//				@Override
//				public boolean isOK(int check) {
//					return BitChecker.mod13ok(check);
//				}
//				
//			};
//			ImageLoadr4 responseLoadr = new ImageLoadr4(evi, eviDir, eviQCDir, mod13Checker);
//			// PERSIANN rainfall predictor
//			String[] persiann = new String[] {"/home/nick/PERSIANN/2010/", "/home/nick/PERSIANN/2011/"};
//			PERSIANNLoadr predictorLoadr = new PERSIANNLoadr(persiann);
//			// the Correlatr
//			corr = new Correlatr(responseLoadr, predictorLoadr, new int[] { 2010, 0, 1 });
//			String reference = "/home/nick/workspace/CLINTON/lib/dfg/land_mask.tif";
//			String base = "/home/nick/workspace/CLINTON/Open/result/evi_persiann_";
//			int[] lags = { 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60,
//					65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120 };
//			corr.writeImagesParallel(base, reference, lags, 10, false); // well, 10 is a quite reasonable argument
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		/*
		 * Note that the previous can result in zero correlation (indicating insufficient data)
		 * from the following conditions: if a combination of no data (from either data source)
		 * and/or EVI<0 results in <5 data points.
		 */
		
		// 20131014 Temperature processing
//		Correlatr corr = null;
//		try {
//			// EVI vegetation index response
//			String[] evi = new String[] {"/home/nick/MOD13A2/2010", "/home/nick/MOD13A2/2011"};
//			String eviDir = "EVI";
//			String eviQCDir = "VI_QC";
//			BitCheck mod13Checker = new BitCheck() {
//				@Override
//				public boolean isOK(int check) {
//					return BitChecker.mod13ok(check);
//				}
//				
//			};
//			ImageLoadr4 responseLoadr = new ImageLoadr4(evi, eviDir, eviQCDir, mod13Checker);
//			
//			// MODIS Aqua daytime temperature
//			String[] temperature = new String[] {"/home/nick/MYD11A2/2010", "/home/nick/MYD11A2/2011"};
//			String tempDir = "LST_DAY";
//			String tempQCDir = "QC_DAY";
//			BitCheck mod11Checker = new BitCheck() {
//				@Override
//				public boolean isOK(int check) {
//					return BitChecker.mod11ok(check);
//				}
//				
//			};
//			ImageLoadr4 predictLoadr = new ImageLoadr4(temperature, tempDir, tempQCDir, mod11Checker);
//			
//			// the Correlatr
//			corr = new Correlatr(responseLoadr, predictLoadr, new int[] { 2010, 0, 1 });
//			String reference = "/home/nick/workspace/CLINTON/lib/dfg/land_mask.tif";
//			String base = "/home/nick/workspace/CLINTON/Open/result/evi_temperature_";
//			int[] lags = { 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60,
//					65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120 };
//			corr.writeImagesParallel(base, reference, lags, 10, true); // well, 10 is a quite reasonable argument
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20140102 Final missing piece at Tierra del Fuego
		// still not getting that piece down there.  Edit enumerate() and here for debug:
//		Correlatr corr = null;
//		try {
//			// EVI vegetation index response
//			//String[] evi = new String[] {"/home/nick/MOD13A2/2010", "/home/nick/MOD13A2/2011"};
//			String[] evi = new String[] {"D:/MOD13A2/2010", "D:/MOD13A2/2011"};
//			String eviDir = "EVI";
//			String eviQCDir = "VI_QC";
//			BitCheck mod13Checker = new BitCheck() {
//				@Override
//				public boolean isOK(int check) {
//					return BitChecker.mod13ok(check);
//				}
//				
//			};
//			ImageLoadr4 responseLoadr = new ImageLoadr4(evi, eviDir, eviQCDir, mod13Checker);
//			// PERSIANN rainfall predictor
//			//String[] persiann = new String[] {"/home/nick/PERSIANN/2010/", "/home/nick/PERSIANN/2011/"};
//			String[] persiann = new String[] {"D:/PERSIANN/8km_daily/2010/", "D:/PERSIANN/8km_daily/2011/"};
//			PERSIANNLoadr predictorLoadr = new PERSIANNLoadr(persiann);
//			// the Correlatr
//			corr = new Correlatr(responseLoadr, predictorLoadr, new int[] { 2010, 0, 1 });
//			//String reference = "/home/nick/workspace/CLINTON/lib/dfg/land_mask.tif";
//			String reference = "X:/Documents/global_phenology/land_mask.tif";
//			//String base = "/home/nick/workspace/CLINTON/Open/result/evi_persiann_tdf_";
//			String base = "X:/Documents/global_phenology/precipitation/evi_persiann_tdf_";
//			int[] lags = { 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60,
//					65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120 };
//			corr.writeImagesParallel(base, reference, lags, 10, false); // well, 10 is a quite reasonable argument
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20140317 redo 
		// change the quality check 
		// change the number of measurements required
		// Added the DOY computation
		// Added P-value
//		Correlatr corr = null;
//		try {
//			// EVI vegetation index response
//			String[] evi = new String[] {"/data/MOD13A2/2010", "/data/MOD13A2/2011"};
//			String eviDir = "EVI";
//			String eviQCDir = "VI_QC";
//			String doyDir = "DOY";
//			BitCheck mod13Checker = new BitCheck() {
//				@Override
//				public boolean isOK(int check) {
//					return BitChecker.mod13ok(check);
//				}
//			};
//			ImageLoadr6 responseLoadr = new ImageLoadr6(evi, eviDir, eviQCDir, doyDir, mod13Checker);
//			// PERSIANN rainfall predictor
//			String[] persiann = new String[] {"/data/PERSIANN/8km_daily/2010/", "/data/PERSIANN/8km_daily/2011/"};
//			PERSIANNLoadr predictorLoadr = new PERSIANNLoadr(persiann);
//			// the Correlatr
//			corr = new Correlatr(responseLoadr, predictorLoadr, new int[] { 2010, 0, 1 });
//			String reference = "/data/GlobalLandCover/modis/land_mask.tif";
//			//String base = "/home/nclinton/Documents/evi_persiann_us_20140319"; // eviN>64, mod13usefulness<8, duplicate data from ImageLoadr6
//			//String base = "/home/nclinton/Documents/evi_persiann_us_20140321"; // eviN>48, mod13usefulness<4, duplicate data fix
//			//String base = "/home/nclinton/Documents/evi_persiann_us_20140322"; // eviN>64, mod13usefulness<8
//			//String base = "/home/nclinton/Documents/evi_persiann_us_20140323"; // eviN>48, mod13usefulness<8, smaller df
//			String base = "/home/nclinton/Documents/evi_persiann_20140324"; // eviN>64, mod13usefulness<8, smaller df
//			int[] lags = { 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60,
//					65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150 };
//			corr.writeImagesParallel(base, reference, lags, 10, false); // well, 10 is a quite reasonable argument
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20140401 Temperature processing
		Correlatr corr = null;
		try {
			// EVI vegetation index response
			String[] evi = new String[] {"/data/MOD13A2/2010", "/data/MOD13A2/2011"};
			String eviDir = "EVI";
			String eviQCDir = "VI_QC";
			String doyDir = "DOY";
			BitCheck mod13Checker = new BitCheck() {
				@Override
				public boolean isOK(int check) {
					return BitChecker.mod13ok(check);
				}
			};
			ImageLoadr6 responseLoadr = new ImageLoadr6(evi, eviDir, eviQCDir, doyDir, mod13Checker);

			// MODIS Aqua daytime temperature
			String[] temperature = new String[] {"/data/MYD11A2/2010", "/data/MYD11A2/2011"};
			String tempDir = "LST_DAY";
			String tempQCDir = "QC_DAY";
			BitCheck mod11Checker = new BitCheck() {
				@Override
				public boolean isOK(int check) {
					return BitChecker.mod11ok(check);
				}
			};
			ImageLoadr4 predictLoadr = new ImageLoadr4(temperature, tempDir, tempQCDir, mod11Checker);

			// the Correlatr
			corr = new Correlatr(responseLoadr, predictLoadr, new int[] { 2010, 0, 1 });
			String reference = "/data/GlobalLandCover/modis/land_mask.tif";
			String base = "/home/nclinton/Documents/evi_temp_us_20140401";
			int[] lags = { 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60,
					65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120 };
			corr.writeImagesParallel(base, reference, lags, 10, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		
	}

}