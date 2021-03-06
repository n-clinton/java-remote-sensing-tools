/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ru.sscc.spline.Spline;

import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 *
 */
public class PERSIANNLoadr implements Loadr {

	private ArrayList<PERSIANNFile> imageList;
	private double[] t;
	private Calendar date0;
	
	/**
	 * 
	 * @param directories
	 */
	public PERSIANNLoadr(String[] directories) throws Exception {
		System.out.println("Initializing PERSIANNFile loader...");
		imageList = new ArrayList<PERSIANNFile>();
		
		for (int d=0; d<directories.length; d++) {
			File dir = new File(directories[d]);
			if (!dir.isDirectory()) {
				throw new Exception("Jackass!  Invalid directory assigned.");
			}
			File[] bins = dir.listFiles();
			for (File f : bins) {
				// skip subdirectories
				if (f.isDirectory()) { continue; }
				// if it's not a PERSIANN File, skip
				if (!f.getName().endsWith(".bin")) { continue; }
				// otherwise, it's a PERSIANN file
				imageList.add(new PERSIANNFile(f.getAbsolutePath()));
			}
		}
		// Keep the list in chronological order
		Collections.sort(imageList);
		System.out.println("The PERSIANN image list are sorted!");
		// zero reference image
		date0 = imageList.get(0).cal;
		
		// set the X vector
		t = new double[imageList.size()];
	}
	
	/**
	 * Optionally set the zero reference for the time series, i.e. the reference time compared
	 * to which the t-coordinate of the images will be computed.
	 * @param cal
	 */
	public void setDateZero(Calendar cal) {
		date0 = cal;
		// rebuild X
		for (int i=0; i<imageList.size(); i++) {
			PERSIANNFile pf = imageList.get(i);
			t[i] = diffDays(pf);
		}
	}
	
	/**
	 * Get the overall length of the time series rounded to the nearest number of days.
	 * @return
	 */
	public int getLengthDays() {
		return ImageLoadr2.diffDays(imageList.get(0).cal, imageList.get(imageList.size()-1).cal);
	}
	
	
	/**
	 * 
	 * @param im1
	 * @param im2
	 * @return
	 */
	public int diffDays(PERSIANNFile pf) {
		long milliseconds1 = date0.getTimeInMillis();
	    long milliseconds2 = pf.cal.getTimeInMillis();
	    long diff = milliseconds2 - milliseconds1;
	    //return Math.round(diff / (24 * 60 * 60 * 1000));
	    return (int)(diff / (24 * 60 * 60 * 1000));
	}
	
	
	/**
	 * 
	 * @param pt
	 * @return
	 */
	public synchronized List<double[]> getSeries(Point pt) {		
		return getSeries(pt.getX(), pt.getY());
	}
	
	
	/**
	 * @param x
	 * @param y
	 * @return
	 */
	public synchronized List<double[]> getSeries(double x, double y) {		
		LinkedList<double[]> out = new LinkedList<double[]>();
		// iterate over images
//		System.out.println("DEBUG_INFO: imageList.size(): " + imageList.size());
		
		long start = System.nanoTime();
		for (int i=0; i<imageList.size(); i++) {
			PERSIANNFile pf = imageList.get(i);
			try {
				
				float val = pf.imageValue(x, y);
				if (val == -9999.f) { continue; }
				out.add(new double[] {t[i], val});
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
		long stop = System.nanoTime(); 
		double elapsed = (double)(stop - start) / 1000.0;
//		System.err.println("time: " + elapsed);
		
		return out;
	}
	
	/**
	 * 
	 */
	public void close() {
		for (int i=0; i<imageList.size(); i++) {
			try {
				imageList.remove(i).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		imageList = null;
		System.gc();
	}
	
	/**
	 * Get a complete X vector for the time series.
	 * @return
	 */
	public double[] getX() {
		return t;
	}

	/**
	 * Fit a thin plate spline to the series and interpolate missing values.
	 * This will fail if first and/or last values are missing, i.e. there is no extrapolation.
	 * @param pt is a georeferenced point with the same coordinate system as the images.
	 * @return a vector of Y values where missing values are interpolated
	 */
	public synchronized double[] getY(Point pt) throws Exception {
		// get the time series under the point
		List<double[]> series = getSeries(pt);
		if (series.size() == imageList.size()) {
			double[] y = new double[series.size()];
			for (int t=0; t<series.size(); t++) {
				y[t] = series.get(t)[1];
			}
			return y;
		}
		else if (series.size() < 4) {  // not enough points to do an interpolation, 4 is arbitrary
			throw new Exception("Not enough data!  n="+series.size());
		}
		else if (series.get(0)[0] > t[0]) { // do not extrapolate in the beginning
			throw new Exception("Start of series out of range: "+series.get(0)[0]);
		}
		else if (series.get(series.size()-1)[0] < t[t.length-1]) { // do not extrapolate at the end
			throw new Exception("End of series out of range: "+series.get(series.size()-1)[0]);
		}
		double[][] xy = TSUtils.getSeriesAsArray(series);
		// fit a spline to interpolate
		//Spline spline = TSUtils.duchonSpline(xy[0], xy[1]);
		DuchonSplineFunction spline = new DuchonSplineFunction(xy);
		return TSUtils.evaluateSpline(spline, t);
	}
	
	@Override
	public String toString() {
		String ret = "";
		for (PERSIANNFile pf : this.imageList) {
			ret += (pf.toString()+"\t");
		}
		return ret;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// PERSIANN rainfall predictor
		//String[] persiann = new String[] {"D:/PERSIANN/8km_daily/2010/", "D:/PERSIANN/8km_daily/2011/"};
//		String[] persiann = new String[] {"/data/PERSIANN/8km_daily/2010/", "/data/PERSIANN/8km_daily/2011/"};
//		try {
//			PERSIANNLoadr predictorLoadr = new PERSIANNLoadr(persiann);
//			Calendar cal = Calendar.getInstance();
//			cal.set(2010, 0, 1);
//			predictorLoadr.setDateZero(cal);
////			double x = -71.0;
////			for (double y=-48.0; y>-70.0; y-=0.5) {
////				List<double[]> series = predictorLoadr.getSeries(x,y);
////				System.out.println("Point: "+Arrays.toString(new double[] {x,y})+"Length: "+series.size());
////				for (double[] t : series) {
////					System.out.println("\t"+Arrays.toString(t));
////				}
////			}
////			double x = 133.1;
////			double y = -19.0;
////			List<double[]> series = predictorLoadr.getSeries(x,y);
////			for (int t=0; t<series.size(); t++) {
////				System.out.println("\t predictorLoadr:"+Arrays.toString(series.get(t)));
////			}
//			// consistent with Correlatr, 6 months of missing PERSIANN
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		String[] persiann = new String[] {"/data/PERSIANN/8km_daily/2008/", "/data/PERSIANN/8km_daily/2009/", "/data/PERSIANN/8km_daily/2010/", "/data/PERSIANN/8km_daily/2011/"};
		try {
			PERSIANNLoadr predictorLoadr = new PERSIANNLoadr(persiann);
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 2008);
			cal.set(Calendar.DAY_OF_YEAR, 0); // first two days are both time=0 if this is not set to 0
			predictorLoadr.setDateZero(cal);
			System.out.println(predictorLoadr);
			System.out.println(Arrays.toString(predictorLoadr.t));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
