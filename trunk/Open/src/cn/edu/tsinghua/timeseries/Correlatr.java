/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
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
	public double[] maxCorrelation(List<double[]> response, List<double[]> covariate, int[] lags) {
		VectorialCovariance cov = new VectorialCovariance(2, false);
		RealMatrix vc = null; // the variance/covariance matrix
		double[] correlations = new double[lags.length];
		double min = response.get(0)[0]; // minumum X
		double max = response.get(response.size()-1)[0];
		// fit splines
		double[][] xy = TSUtils.getSeriesAsArray(response);
		Spline rSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		xy = TSUtils.getSeriesAsArray(covariate);
		Spline cSpline = TSUtils.duchonSpline(xy[0], xy[1]);
		for (int l=0; l<lags.length; l++) {
			cov.clear();
			// compute covariance
			for (double t=min; t<=max; t++) { // daily step
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
		int index = weka.core.Utils.maxIndex(correlations);
		return new double[] {correlations[index], lags[index]};
	}
	
	/**
	 * 
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
		
		// outputs
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
		
//		for (int x=888; x<15358; x++) {
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
	 * 
	 * @param base
	 * @param reference
	 * @param lags
	 */
	public void writeImagesParallel(String base, String reference, int[] lags) {
		int qSize = 100;
		BlockingQueue<Pixel> queue = new ArrayBlockingQueue<Pixel>(qSize);
		PlanarImage ref = JAIUtils.readImage(reference);
		PixelEnumeration enumerator = new PixelEnumeration(queue, ref);
		new Thread(enumerator).start();
		PixelWrite write = new PixelWrite(queue, base, lags, ref.getWidth(), ref.getHeight());
		new Thread(write).start();
	}
	
	/**
	 * 
	 * @author Nicholas
	 *
	 */
	class PixelEnumeration implements Runnable {
		ExecutorService service;
		//CompletionService<List<double[]>> producer;
		BlockingQueue<Pixel> queue;
		RandomIter iter;
		
		/**
		 * 
		 * @param queue
		 * @param reference
		 */
		public PixelEnumeration(BlockingQueue queue, PlanarImage ref) {
			this.queue = queue;
			service = Executors.newFixedThreadPool(2);
			//producer = new ExecutorCompletionService<List<double[]>>(service);
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
			
			for (int x=888; x<15358; x++) {
				for (int y=700; y<12372; y++) {
//			for (int x=888; x<2000; x++) {
//				for (int y=700; y<2000; y++) {
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
	 * 
	 * @author Nicholas
	 *
	 */
	class Pixel {
		List<double[]> evi, persiann;
		int x, y;
		public Pixel(Future<List<double[]>> eviF, Future<List<double[]>> persiannF, int x, int y) throws Exception {
			this.x = x;
			this.y = y;
			if (eviF != null && persiannF != null) {  // could happen if DUMMY
				evi = eviF.get();
				persiann = persiannF.get();
			}
		}
	}
	
	
	/**
	 * 
	 * @author Nicholas
	 *
	 */
	class PixelWrite implements Runnable {
		
		BlockingQueue<Pixel> queue;
		WritableRaster corr, days, eviN;
		int[] lags;
		String base;
		
		/**
		 * 
		 * @param queue
		 * @param base
		 * @param lags
		 * @param width
		 * @param height
		 */
		public PixelWrite (BlockingQueue queue, String base, int[] lags, int width, int height) {
			this.queue = queue;
			this.lags = lags;
			this.base = base;
			// outputs
			corr = RasterFactory.createBandedRaster(
	    			DataBuffer.TYPE_BYTE,
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

		@Override
		public void run() {
			try {
				write();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			JAIUtils.writeTiff(corr, base+"_corr.tif", DataBuffer.TYPE_BYTE);
			corr = null;
			System.gc();
			JAIUtils.writeTiff(days, base+"_days.tif", DataBuffer.TYPE_BYTE);
			days = null;
			System.gc();
			JAIUtils.writeTiff(eviN, base+"_eviN.tif", DataBuffer.TYPE_BYTE);
		}
		
		/**
		 * 
		 * @throws InterruptedException
		 * @throws ExecutionException
		 */
		public void write() throws InterruptedException, ExecutionException {
			boolean done = false;
			while (!done) {
				//System.out.println("queue: "+queue.size());
				Pixel pix = queue.take();
				if (pix.x != -1 && pix.y != -1) { // not the DUMMY
					List<double[]> evi = pix.evi;
					if (evi.size() < 5) { continue; }
					List<double[]> persiann = pix.persiann;
					if (persiann.size() < 5) { continue; }
					double[] correlation = maxCorrelation(evi, persiann, lags);
					// 10*correlation, integer part
					corr.setSample(pix.x, pix.y, 0, (correlation[0]*100));
					days.setSample(pix.x, pix.y, 0, correlation[1]);
					eviN.setSample(pix.x, pix.y, 0, evi.size());
					System.out.println(pix.x+","+pix.y+","+(correlation[0])+","+correlation[1]+","+evi.size());	
				}
				else {
					done = true;
				}
			}
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
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
			Correlatr corr = new Correlatr(new String[] {"I:/MOD13A2/2010/", 
														 "I:/MOD13A2/2011/"}, 
										   new String[] {"C:/Users/Public/Documents/PERSIANN/8km_daily/2010",
														 "C:/Users/Public/Documents/PERSIANN/8km_daily/2011"},
										   new int[] {2010, 0, 1}); 
			String reference = "C:/Users/Nicholas/Documents/global_phenology/land_mask.tif";
			String base = "C:/Users/Nicholas/Documents/global_phenology/evi_persiann";
			//int[] lags = {0,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90};
			int[] lags = {0,5,10,15,20,25,30,35,40,45,50,55,60};
			long now = System.currentTimeMillis();
			corr.writeImagesParallel(base, reference, lags);
			long later = System.currentTimeMillis();
			System.out.println("time: "+(later-now));
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}