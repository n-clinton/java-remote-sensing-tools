/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
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

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.correlation.StorelessCovariance;

import cn.edu.tsinghua.lidar.BitChecker;
import cn.edu.tsinghua.modis.BitCheck;

import com.berkenviro.imageprocessing.JAIUtils;

/**
 * The same functionality is available from Correlatr2 with longestSum=0.
 * Not sure if this one is faster, but it's cleaner.
 * @author Nicholas Clinton
 * @author Cong Hui He
 * 
 */
public class Correlatr3 {
	
	final int longestLag;
	final int longestSum;
	final int longestInterval;
	final boolean maxCorrInterp;
	
	Loadr responseLoadr;
	Loadr predictLoadr;
	PlanarImage ref;
	
	// image dimensions
	final int width_begin;
	final int width_end;
	final int height_begin;
	final int height_end;
	
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
	 * @param longestLag is the longest lag (in days) to check
	 * 
	 * @param longestInterval is the longest interval permitted between samples (only used if maxCorrInterp=true)
	 * @param maxCorrInterp if true will interpolate the predictor variable
	 * @throws Exception
	 */
	public Correlatr3(Loadr response, Loadr predictor, String reference, 
			int[] ymd, int longestLag, int longestSum, int longestInterval, boolean maxCorrInterp) throws Exception {
		System.out.println("Initializing... ");
		responseLoadr = response;
		predictLoadr = predictor;
		Calendar cal = Calendar.getInstance();
		cal.set(ymd[0], ymd[1], ymd[2]);
		System.out.println("Setting time series zero reference to " + cal.getTime());
		responseLoadr.setDateZero(cal);
		predictLoadr.setDateZero(cal);
		this.maxCorrInterp = maxCorrInterp;
		System.out.println("Interpolation is "+this.maxCorrInterp);
		this.longestInterval = longestInterval;
		System.out.println("longestInterval: "+this.longestInterval);
		
		this.longestLag = longestLag;
		System.out.println("longestLag: "+this.longestLag);
		this.longestSum = longestSum;
		System.out.println("Sum days: "+this.longestSum);
		
		ref = JAIUtils.readImage(reference);
		JAIUtils.register(ref);
		
		// Processing extents
		// North America
//		width_begin = 888;
//		width_end = 15358;
//		height_begin = 700;
//		height_end = 12372;
		// US
//		width_begin = 6500;
//		width_end = 13000;
//		height_begin = 2500;
//		height_end = 5500;

		width_begin = 0;
		width_end = ref.getWidth();
		height_begin = 0;
		height_end = ref.getHeight();
		
	}

	/**
	 * 
	 */
	public void close() {
		responseLoadr.close();
		predictLoadr.close();
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
	 * When called, it computes the correlation between the series.
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

		public double[] maxCorrelation() {
			// get a Map for the response
			Double[] rDoub = new Double[(int)response.get(response.size()-1)[0]+1];
			for (double[] tr : response) {
				rDoub[(int)tr[0]] = tr[1];
			}
			// get a Map for the covariate
			Double[] cDoub = new Double[
			         Math.max((int)covariate.get(covariate.size()-1)[0]+1,
			        		  (int)response.get(response.size()-1)[0]+1)];
			for (double[] tc : covariate) {
				cDoub[(int)tc[0]] = tc[1];
			}

			double minCorr = 1.0;
			double maxCorr = -1.0;
			int minLag = 0;
			int maxLag = 0;
			int minSum = 0;
			int maxSum = 0;
			int minN = 0;
			int maxN = 0;
			for (int sum=0; sum<=longestSum; sum+=8) {
				//System.out.print("sum="+sum+", ");
				for (int l=0; l<=longestLag; l+=8) {
					//System.out.println("l="+l);
					StorelessCovariance cov = new StorelessCovariance(2, false);
					int n = 0;
					for (double[] rt : response) { // iterate over every t for the response
						double t = rt[0];
						//System.out.println("\t t="+t);
						if (t - l - sum < 0) {
							//System.err.println("\t\t out of bounds: "+(t - l - sum));
							continue; // don't go out of bounds
						}
						try {
							// response---------------
							double r = rDoub[(int)t];
							if (r < 0) { // want only EVI greater than zero
								//System.err.println("\t\t EVI<0.");
								continue;
							}
							// summation
							double cSum = 0;
							int index = (int) t - l;
							//System.out.println("\t\t Summing from "+(t - lags[l])+"...");
							
							for (int s=0; s<=sum; s++) { // DAILY time step on the covariate
								//System.out.println("\t\t\t time="+index);
								Double cDouble = cDoub[index];
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
								n++;
							}
							
						} catch (Exception e) {
							e.printStackTrace();
						}

					} // end responses

					// if not enough data (arbitrary number)
					if (n < 5) {
						//System.err.println("\t Not enough data. n="+n);
						continue;
					}
					// if either variable is constant...
					if (cov.getCovariance(0, 0) == 0 || cov.getCovariance(1, 1) == 0) {
						//System.err.println("\t No variance: var(r)="+cov.getCovariance(0, 0)+", var(c)="+cov.getCovariance(1, 1));
						continue;
					}
					// normalize by SD
					double correlation = cov.getCovariance(0, 1)
							/ Math.sqrt(cov.getCovariance(0, 0) * cov.getCovariance(1, 1));
					if (correlation < minCorr) {
						minCorr = correlation;
						minLag = l;
						minSum = sum;
						minN = n;
					}
					if (correlation > maxCorr) {
						maxCorr = correlation;
						maxLag = l;
						maxSum = sum;
						maxN = n;
					}

				} // end lags
			} // end summations
			
			if (maxCorr < 0) {
				return new double[] {minCorr, minLag, minSum, minN};
			}
			return new double[] {maxCorr, maxLag, maxSum, maxN};
		}
		
		@Override
		public Pixel call() {
			//long start = System.nanoTime();
			// DUMMY, nothing to do
			if (dummy) {
				return this;
			}
			// sort of arbitrary, but if size=0, below throws an exception and the write thread will stop
			if (response.size() < 9 || covariate.size() < 9) {
				//System.err.println("Not enough data. Response="+response.size()+", Covariate="+covariate.size());
				correlation = new double[] {0, 0, 0, 0};
				return this;
			}

			correlation = maxCorrelation();
			
			if (correlation[3] < 5) { // arbitrary, already checked in maxCorrelation
				//System.err.println("Not enough data. N="+correlation[3]);
				correlation = new double[] {0, 0, 0, 0};
				return this;
			}
			
			double df = correlation[3] - 2.0; // for data that is not interpolated
			double t = correlation[0] * Math.sqrt(df / (1-Math.pow(correlation[0], 2)));
			// compare:
			//double z = 0.5*Math.log((1.0 + correlation[0]) / (1.0 - correlation[0]))*Math.sqrt(df-1.0);
			
			// replace the cov.N with a p-value
			if (df <= 0) {
				correlation[3] = 0.5; // undefined
			}
			else {
				TDistribution tDist = new TDistribution(df);
				//NormalDistributionImpl zDist = new NormalDistributionImpl();
				double p = 0;
				p = tDist.cumulativeProbability(t);
				// quick check: t is bigger
				//System.out.println("\t\t T-dist: "+(1.0-p)+" Normal dist: "+(1.0-zDist.cumulativeProbability(t)));

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

		private String toString(List<double[]> list) {
			String listString = "";
			for (double[] arr : list) {
				listString+=(Arrays.toString(arr)+",");
			}
			return listString;
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
			// two read threads
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
			}
			long stop = System.currentTimeMillis();
			long time_elapsed_millis = stop - start;
			System.out.println("Time used: " + time_elapsed_millis + " milliseconds");
			System.out.println("Read Times: " + PERSIANNFile.READ_TIMES);
			
			// shut down the reading service
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

		/**
		 * 
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
					//covariateReader.x = (_x < 0 ? _x + 360.0 : _x); // PERSIANN
					covariateReader.x = _x; // LST
					covariateReader.y = _y;
					covariateReader.loadr = predictLoadr;

					try {
						// watch out for memory leak here
						// also watch out for simultaneous reads
						Future f1 = es.submit(responseReader); 
						Future f2 = es.submit(covariateReader);
						while (true) {
							if (f1.get()==null && f2.get()==null) {
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
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}
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
			compute();
			System.out.println("Finished computing!!");
			ecs.submit(new Pixel(true)); // DUMMY
		}

		/**
		 * Simply move the pixels from the read queue to the completion service.
		 * 
		 * @throws InterruptedException
		 * @throws Exception
		 */
		public void compute() {
			while (true) {
				Pixel pix;
				try {
					// take off the first queue, read, but unprocessed
					pix = queue.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
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
			int width = width_end - width_begin;
			int height = height_end - height_begin;
			
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
			
			// shut down the computing service
			service.shutdown();
			System.out.println("The ThreadPoolExector has been issued a shutdown command.");
			try {
				if (!service.awaitTermination(120, TimeUnit.SECONDS)) {
					System.err.println("The service did not terminate");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			close();
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
					continue;
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
					System.gc();
					System.gc();
					System.gc();
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
			JAIUtils.writeTiff(corr, base + "_corr.tif");
			JAIUtils.writeTiff(days, base + "_days.tif");
			JAIUtils.writeTiff(sum, base + "_sum.tif");
			JAIUtils.writeTiff(p, base + "_p.tif");
		}

		public synchronized void writeCorr(double val, Pixel pix) {
			corr.setSample(pix.x - width_begin, pix.y - height_begin, 0, val);
		}

		public synchronized void writeDays(double val, Pixel pix) {
			days.setSample(pix.x - width_begin, pix.y - height_begin, 0, val);
		}
		
		public synchronized void writeSum(double val, Pixel pix) {
			sum.setSample(pix.x - width_begin, pix.y - height_begin, 0, val);
		}

		public synchronized void writeP(double val, Pixel pix) {
			p.setSample(pix.x - width_begin, pix.y - height_begin, 0, val);
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
		Pixel testPixel = new Pixel(false);
		testPixel.set(response, covariate, -9, -9);
		return testPixel.maxCorrelation();
	}
	
	/**
	 * 20140507 start new processing log
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// 
		Correlatr3 corr = null;
		String reference = "/data/GlobalLandCover/modis/land_mask.tif";
		int longestLag = 168; // 24 weeks, 6 months
		int longestSum = 8; // 12 weeks, 3 months
		int longestInterval = 64; // not used unless interpolation
		int threads = 10;
		// EVI vegetation index response
		String[] evi = new String[] {"/data/MOD13A2/2008", "/data/MOD13A2/2009", "/data/MOD13A2/2010", "/data/MOD13A2/2011"};
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

//		// PERSIANN rainfall predictor
//		String[] persiann = new String[] {"/data/PERSIANN/8km_daily/2008/", "/data/PERSIANN/8km_daily/2009/", "/data/PERSIANN/8km_daily/2010/", "/data/PERSIANN/8km_daily/2011/"};
//		PERSIANNLoadr predictorLoadr = new PERSIANNLoadr(persiann);
//		// the Correlatr
//		corr = new Correlatr3(responseLoadr, predictorLoadr, reference, new int[] { 2008, 0, 0 }, longestLag, longestSum, longestInterval, false);
//		//String base = "/home/nclinton/Documents/evi_persiann_sum_us_20140514"; // no interpolation, 4 years, lag 168, sum 84
//		String base = "/home/nclinton/Documents/evi_persiann_sum_20140515"; // no interpolation, 4 years, lag 168, sum 84
//		corr.writeImagesParallel(base, threads);
		// Looks good!
		
//		System.out.println(Arrays.toString(corr.correlation(-13.3, 133.4))); // Australia
//		System.out.println(Arrays.toString(corr.correlation(38.0, -121.0))); // CA
		
//		WritableRaster check = RasterFactory.createBandedRaster(DataBuffer.TYPE_SHORT, // signed short
//				1000, 1000, 1, // bands
//				new java.awt.Point(0, 0)); // origin
//		for (int i=0; i<1000; i++) {
//			for(int j=0; j<1000; j++) {
//				check.setSample(i, j, 0, i*j);
//			}
//		}
//		String checkFile = "/home/nclinton/Documents/check_image_20140509";
//		JAIUtils.writeTiff(check, checkFile);
		// OK. 
		
		// 20140520 temperature
		String[] temperature = new String[] {"/data/MYD11A2/2008", "/data/MYD11A2/2009", "/data/MYD11A2/2010", "/data/MYD11A2/2011"};
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
		corr = new Correlatr3(responseLoadr, predictLoadr, reference, new int[] { 2008, 0, 0 }, longestLag, longestSum, longestInterval, false);
		//String base = "/home/nclinton/Documents/evi_temperature_sum_20140520"; // no interpolation, 4 years, lag 168, sum 84, n>9
		//String base = "/home/nclinton/Documents/evi_temperature_sum_20140522"; // no interpolation, 4 years, lag 168, sum 84, n>5
		String base = "/home/nclinton/Documents/evi_temperature_sum_20140523"; // no interpolation, 4 years, lag 168, sum 8, n>5
		corr.writeImagesParallel(base, threads);
		
		//System.out.println(Arrays.toString(corr.correlation(-13.3, 133.4))); // Australia
		//System.out.println(Arrays.toString(corr.correlation(38.0, -121.0))); // CA
		
	}

}