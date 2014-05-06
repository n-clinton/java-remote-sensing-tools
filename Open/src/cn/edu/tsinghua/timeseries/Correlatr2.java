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
public class Correlatr2 {
	
	int longestLag;
	int longestSum;
	boolean maxCorrInterp;
	
	Loadr responseLoadr;
	Loadr predictLoadr;
	PlanarImage ref;
	
	BlockingQueue<Pixel> pixels; // reusable objects
	BlockingQueue<Pixel> queue; // read, but not processed
	ThreadPoolExecutor service;
	ExecutorCompletionService<Pixel> ecs; // processing
	
	/**
	 * 
	 * @param response
	 * @param predictor
	 * @param reference is the pixel reference.  An output will be written at each reference pixel == 1.
	 * @param ymd is {year, month, day} where month is zero-referenced
	 * @param lags is an array of integer lags to test
	 * @param longestSum is the most number of days to sum the covariate
	 * @param maxCorrInterp if true will interpolate the predictor variable
	 * @throws Exception
	 */
	public Correlatr2(Loadr response, Loadr predictor, String reference, 
			int[] ymd, int longestLag, int longestSum, boolean maxCorrInterp) throws Exception {
		System.out.println("Initializing... ");
		this.maxCorrInterp = maxCorrInterp;
		System.out.println("Interpolation is "+this.maxCorrInterp);
		this.longestLag = longestLag;
		System.out.println("longestLag: "+this.longestLag);
		this.longestSum = longestSum;
		System.out.println("Sum days: "+this.longestSum);
		
		ref = JAIUtils.readImage(reference);
		JAIUtils.register(ref);
		responseLoadr = response;
		predictLoadr = predictor;
		Calendar cal = Calendar.getInstance();
		cal.set(ymd[0], ymd[1], ymd[2]);
		System.out.println("Setting zero-reference to: " + cal.getTime());
		responseLoadr.setDateZero(cal);
		predictLoadr.setDateZero(cal);

	}

	/**
	 * 
	 */
	public void close() {
		responseLoadr.close();
		predictLoadr.close();
	}

	
	/**
	 * Use interpolating splines.  Response must be greater than zero.
	 * 
	 * @param response
	 * @param covariate
	 * @return
	 */
	public double[] maxCorrelationInterpolatedSummed(List<double[]> response, List<double[]> covariate) {
		VectorialCovariance cov = new VectorialCovariance(2, false);
		// fit splines
		double[][] xy = TSUtils.getSeriesAsArray(response);
		Spline rSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		xy = TSUtils.getSeriesAsArray(covariate);
		Spline cSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		
		double minCorr = 1.0;
		double maxCorr = -1.0;
		int minLag = 0;
		int maxLag = 0;
		int minSum = 0;
		int maxSum = 0;
		int minN = 0;
		int maxN = 0;
		double t0 = response.get(0)[0]; // minumum t
		double tn = response.get(response.size() - 1)[0]; // max t
		for (int sum=0; sum<=longestSum; sum+=5) {
			//System.out.print("sum="+sum+", ");
			for (int l = 0; l <= longestLag; l+=5) {
				//System.out.print("\t lag="+lags[l]+", ");
				cov.clear();
				// compute covariance
				for (double t = t0; t <= tn; t++) { // daily step
					if (t - l - sum < 0) {
						//System.out.println("\t\t out of bounds"+(t - lags[l] - sum));
						continue; // don't go out of bounds
					}
					try {
						double r = rSpline.value(t);
						if (r < 0) { // want only EVI greater than zero
							//System.err.println("\t\t EVI<0.");
							continue;
						}
						// summation
						double cSum = 0;
						int index = (int) (t - l);
						//System.out.println("\t\t Summing from "+(t - lags[l])+"...");
						for (int s=0; s<=sum; s++) {
							//System.out.println("\t\t\t time="+index);
							double c = cSpline.value(index);
							//System.out.println("\t\t\t adding..."+c);
							cSum += c;
							index--;
						}
						//System.out.println("\t\t incrementing: "+t+","+r+","+cSum);
						cov.increment(new double[] { r, cSum });
					} catch (Exception e) {
						//e.printStackTrace();
					}
				}

				// the variance/covariance matrix
				RealMatrix vc = cov.getResult();
				// if either variable is constant...
				if (vc.getEntry(0, 0) == 0 || vc.getEntry(1, 1) == 0) {
					//System.err.println("\t No variance.");
					return new double[] {0, 0, 0, 0};
				}
				// normalize by SD
				double correlation = vc.getEntry(0, 1)
						/ Math.sqrt(vc.getEntry(0, 0) * vc.getEntry(1, 1));
				if (correlation < minCorr) {
					minCorr = correlation;
					minLag = l;
					minSum = sum;
					minN = (int)cov.getN();
				}
				if (correlation > maxCorr) {
					maxCorr = correlation;
					maxLag = l;
					maxSum = sum;
					maxN = (int)cov.getN();
				}
			}
		}

		if (maxCorr < 0) {
			return new double[] {minCorr, minLag, minSum, minN};
		}
		return new double[] {maxCorr, maxLag, maxSum, maxN};
	}

	
	/**
	 * This version is meant for daily predictor data (PERSIANN), for which it makes no
	 * sense to fit a spline.  The response (EVI) must be greater than zero.
	 * 
	 * @param response
	 * @param covariate
	 * @return 
	 */
	public double[] maxCorrelationSummed(List<double[]> response, List<double[]> covariate) {
		// insert the predictor data to a TreeMap, with key=time
		TreeMap<Integer, Double> map = new TreeMap<Integer, Double>();
		for (double[] tc : covariate) {
			map.put((int) tc[0], tc[1]);
		}

		VectorialCovariance cov = new VectorialCovariance(2, false);
		// fit a spline to the EVI response
		double[][] xy = TSUtils.getSeriesAsArray(response);
		Spline rSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		double minCorr = 1.0;
		double maxCorr = -1.0;
		int minLag = 0;
		int maxLag = 0;
		int minSum = 0;
		int maxSum = 0;
		int minN = 0;
		int maxN = 0;
		double t0 = response.get(0)[0]; // minumum t
		double tn = response.get(response.size() - 1)[0]; // max t
		for (int sum=0; sum<=longestSum; sum+=5) {
			//System.out.println("sum="+sum+", ");
			for (int l = 0; l<=longestLag; l+=5) {
				//System.out.println("\t lag="+lags[l]+", ");
				cov.clear();
				// compute covariance
				for (double t = t0; t <= tn; t++) { // daily step
					if (t - l - sum < 0) {
						//System.err.println("\t\t out of bounds"+(t - lags[l] - sum));
						continue; // don't go out of bounds
					}
					try {
						double r = rSpline.value(t);
						if (r < 0) { // want only EVI greater than zero
							//System.err.println("\t\t EVI<0.");
							continue;
						}
						// summation
						double cSum = 0;
						int index = (int) (t - l);
						//System.out.println("\t\t Summing from "+(t - lags[l])+"...");
						for (int s=0; s<=sum; s++) {
							//System.out.println("\t\t\t time="+index);
							Double cDouble = map.get(index);
							if (cDouble == null) { // index was a no data point
								//System.err.println("\t\t\t"+index+" was no data.");
								continue;
							}
							//System.out.println("\t\t\t adding: "+cDouble.doubleValue());
							cSum += cDouble.doubleValue();
							index--;
						}
						if (index < (t - l)) { // don't increment a false zero (all no-data)
							//System.out.println("\t\t incrementing: "+t+","+r+","+cSum);
							cov.increment(new double[] { r, cSum });
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// the variance/covariance matrix
				RealMatrix vc = cov.getResult();
				// if either variable is constant...
				if (vc.getEntry(0, 0) == 0 || vc.getEntry(1, 1) == 0) {
					//System.err.println("\t No variance.");
					return new double[] {0, 0, 0, 0};
				}
				// normalize by SD
				double correlation = vc.getEntry(0, 1)
						/ Math.sqrt(vc.getEntry(0, 0) * vc.getEntry(1, 1));
				if (correlation < minCorr) {
					minCorr = correlation;
					minLag = l;
					minSum = sum;
					minN = (int)cov.getN();
				}
				if (correlation > maxCorr) {
					maxCorr = correlation;
					maxLag = l;
					maxSum = sum;
					maxN = (int)cov.getN();
				}
			} // lags
		} // summation

		if (maxCorr < 0) {
			return new double[] {minCorr, minLag, minSum, minN};
		}
		return new double[] {maxCorr, maxLag, maxSum, maxN};
	}

	
	/**
	 * 
	 * @param base is the base filename for output files
	 * @param nThreads is the number of compute threads to use
	 */
	public void writeImagesParallel(String base, int nThreads) {

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
			//long start = System.nanoTime();
			// DUMMY, nothing to do
			if (dummy) {
				return this;
			}
			// sort of arbitrary, but if size=0, below throws an exception and the write thread will stop
			if (response.size() < 13 || covariate.size() < 13) {
				correlation = new double[] {0, 0, 0, 0};
				return this;
			}
			// not enough data check
			boolean gap = false;
			double[] response0 = response.get(0);
			double[] responset = null;
			for (int t=1; t<response.size(); t++) {
				responset = response.get(t);
				if ((responset[0]-response0[0]) > 64) { // 4 consecutive, missing 16-day composites (64)
					gap = true;
					break;
				}
				response0 = responset;
			}
			double[] cov0 = covariate.get(0);
			double[] covt = null;
			for (int t=1; t<covariate.size(); t++) {
				covt = covariate.get(t);
				if ((covt[0]-cov0[0]) > 64) { // 8 consecutive, missing 8-day composites (64)
					gap = true;
					break;
				}
				cov0 = covt;
			}
			
			if (gap) {
//				System.err.println("\t Not enough data: response n="+response.size()+", covariate n="+covariate.size());
//				System.err.println("\t\t response0="+Arrays.toString(response0)+", responset="+Arrays.toString(responset));
//				System.err.println("\t\t cov0="+Arrays.toString(cov0)+", covt="+Arrays.toString(covt));
//				double[] resp = new double[response.size()];
//				for (int i=0; i<resp.length; i++) {
//					double[] arr = response.get(i);
//					resp[i] = arr[0];
//				}
//				System.out.println("\t"+Arrays.toString(resp));
//				double[] cov = new double[covariate.size()];
//				for (int i=0; i<cov.length; i++) {
//					double[] arr = covariate.get(i);
//					cov[i] = arr[0];
//				}
//				System.out.println("\t"+Arrays.toString(cov));
				correlation = new double[] {0, 0, 0, 0};
				return this;
			}

			// do the computation
			if (maxCorrInterp) {
				correlation = maxCorrelationInterpolatedSummed(response, covariate);
			} else {
				correlation = maxCorrelationSummed(response, covariate);
			}
			
			// Assume the Spline functions are free to vary everywhere except the control points.
			// therefore df = N - (N - responseN - covariateN) - 2, where 2 is for the means?
			// some of the data points may not be used (due to the lag), 
			// therefore subtract lag/16 (16-day composites), 
			// although the data outside the lag still constrain the fit, so perhaps this is conservative?
			double df = response.size() + covariate.size() - 2.0 - correlation[1]/16.0 - correlation[1]; // PERSIANN
			//double df = response.size() + covariate.size() - 2.0 - correlation[1]/16.0 - correlation[1]/8.0; // MYD11
			double t = correlation[0] * Math.sqrt(df / (1-Math.pow(correlation[0], 2)));
			// compare:
			//double z = 0.5*Math.log((1.0 + correlation[0]) / (1.0 - correlation[0]))*Math.sqrt(df-1.0);
			// replace the cov.N with a p-value
			if (df < 0) {
				correlation[3] = 0.5; // undefined
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
					correlation[3] = p;
				}
				else {
					correlation[3] = 1.0 - p;
				}
			}
			
			//long stop = System.nanoTime();
			//double elapsed = (double)(stop - start) / 1000.0;
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
			//long start = System.nanoTime();
			list = loadr.getSeries(x, y);
			//long stop = System.nanoTime();
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
			
			// Australia
//			int width_begin = 37427;
//			int height_begin =  10639;
//			int width_end = 37463;
//			int height_end = 9957;

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
					covariateReader.x = (_x < 0 ? _x + 360.0 : _x); // PERSIANN
					//covariateReader.x = _x; // LST
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

		WritableRaster corr, days, sum, p;
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
			sum = RasterFactory.createBandedRaster(DataBuffer.TYPE_SHORT, // signed short
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
			int interval = 100000; // frequency of write, original value 10000000
			int counter = 0;
			long now = System.currentTimeMillis();
			while (true) {
				Pixel finishedPix;
				try {
					finishedPix = ecs.take().get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					finishedPix = new Pixel(false);
					finishedPix.correlation = new double[] {0, 0, 0, 0};
				}
				
				if (finishedPix.dummy) { // DUMMY
					break;
				}
				
				//System.out.println(finishedPix);

				writeCorr((short)(255*finishedPix.correlation[0]), finishedPix);
				writeDays((short) finishedPix.correlation[1], finishedPix);
				writeSum((short) finishedPix.correlation[2], finishedPix);
				writeP((byte)(255*finishedPix.correlation[3]), finishedPix);

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
			JAIUtils.writeTiff(sum, base + "_sum.tif");
			JAIUtils.writeTiff(p, base + "_p.tif");
		}

		public synchronized void writeCorr(double val, Pixel pix) {
			corr.setSample(pix.x, pix.y, 0, val);
		}

		public synchronized void writeDays(double val, Pixel pix) {
			days.setSample(pix.x, pix.y, 0, val);
		}
		
		public synchronized void writeSum(double val, Pixel pix) {
			sum.setSample(pix.x, pix.y, 0, val);
		}

		public synchronized void writeP(double val, Pixel pix) {
			p.setSample(pix.x, pix.y, 0, val);
		}

	} // end writing class

	
	/**
	 * Debugging helper
	 * @param lat
	 * @param lon
	 * @return
	 */
	public double[] correlation(double lat, double lon) {
		List<double[]> response = responseLoadr.getSeries(lon, lat);
		for (double[] d : response) {
			System.out.print(Arrays.toString(d)+", ");
		}
		System.out.println();
		List<double[]> covariate = predictLoadr.getSeries(lon, lat);
		for (double[] d : covariate) {
			System.out.print(Arrays.toString(d)+", ");
		}
		System.out.println();
		return maxCorrelationSummed(response, covariate);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Correlatr2 corr = null;
		String reference = "/data/GlobalLandCover/modis/land_mask.tif";
		int longestLag = 150;
		int longestSum = 90;
		int threads = 50;
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
		
		// ***************************************************************************
		// PERSIANN rainfall predictor
		String[] persiann = new String[] {"/data/PERSIANN/8km_daily/2010/", "/data/PERSIANN/8km_daily/2011/"};
		PERSIANNLoadr predictorLoadr = new PERSIANNLoadr(persiann);
		// the Correlatr
		corr = new Correlatr2(responseLoadr, predictorLoadr, reference, new int[] { 2010, 0, 1 }, longestLag, longestSum, false);
		String base = "/home/nclinton/Documents/evi_persiann_sum_us_20140420"; // eviN>64, PERSIANN>64, mod13usefulness<8, sum to 60, lags to 150, N>13
		corr.writeImagesParallel(base, threads);
		
		
		// ***************************************************************************
		// 20140401 Temperature processing
		// MODIS Aqua daytime temperature
//		String[] temperature = new String[] {"/data/MYD11A2/2010", "/data/MYD11A2/2011"};
//		String tempDir = "LST_DAY";
//		String tempQCDir = "QC_DAY";
//		BitCheck mod11Checker = new BitCheck() {
//			@Override
//			public boolean isOK(int check) {
//				return BitChecker.mod11ok(check);
//			}
//		};
//		ImageLoadr4 predictLoadr = new ImageLoadr4(temperature, tempDir, tempQCDir, mod11Checker);
//
//		// the Correlatr
//		corr = new Correlatr2(responseLoadr, predictLoadr, reference, new int[] { 2010, 0, 1 }, lags, longestSum, false);
//		//String base = "/home/nclinton/Documents/evi_temp_20140401"; // eviN>64, tempN >48, mod13usefulness<8, lags to 120
//		String base = "/home/nclinton/Documents/evi_temp_20140407"; // eviN>80, tempN>80, mod13usefulness<8, lags to 150, N>13
//		corr.writeImagesParallel(base, reference, lags, 10, true);
		
		// debug
//		double lat = 38.0;
//		double lon = -121.0;
//		System.out.println(Arrays.toString(corr.correlation(lat, lon)));
		// [0.2993948004981556, 20.0, 0.0, 687.0] // PERSIANN
		// [0.5930044644340262, 150.0, 0.0, 61.0] // Temp
		// matches Correlatr
	}

}