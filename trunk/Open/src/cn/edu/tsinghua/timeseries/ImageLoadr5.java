/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;

import cn.edu.tsinghua.lidar.BitChecker;
import cn.edu.tsinghua.modis.BitCheck;

import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.ImageData;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas Clinton
 * @author Cong Hui He
 * 
 * For loading a vector from WorldClim tiffs in a directory.
 * Similar to ImageLoadr4, but without the QC.
 */
public class ImageLoadr5 implements Loadr  {

	private ArrayList<DatedQCImage> imageList;
	private double[] t;
	private Calendar date0;
	private ImageData[] _image_data;
	
	/**
	 * 
	 * @param directories
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ImageLoadr5(String[] directories) throws Exception {
		System.out.println("Initializing image loader...");
		
		imageList = new ArrayList<DatedQCImage>();

		for (int d=0; d<directories.length; d++) {
			File dir = new File(directories[d]);
			if (!dir.isDirectory()) {
				throw new Exception("Jackass!  Invalid directory assigned. Directory " + dir.getPath() + " not found.");
			}
			File[] dates = dir.listFiles();
			for (File f : dates) {
				// skip non-tif files that may be in the directory
				if (!f.getName().endsWith(".tif")) { continue; }
				
				// Instantiate a new image Object
				DatedQCImage image = new DatedQCImage();
				
				Calendar c = Calendar.getInstance();
				// for WorldClim, average over 1950-2000, so set date to the first of the month in 2000
				c.set(2000, Integer.parseInt(f.getName().substring(f.getName().length()-6, f.getName().length()-4))-1, 1);
				image.cal = c;
				image.imageName = f.getAbsolutePath();
				imageList.add(image);
			}
		}

		// Keep the list in chronological order
		Collections.sort(imageList);
		System.out.println("\t Done!");

		// reference to the first image, unless otherwise specified
		date0 = imageList.get(0).cal;

		_image_data = new ImageData[imageList.size()];

		// set the time vector
		t = new double[imageList.size()];
		for (int i=0; i<imageList.size(); i++) {
			DatedQCImage dImage = imageList.get(i);
			t[i] = diffDays(dImage);
			// set Image Data
			_image_data[i] = new ImageData(dImage.imageName, 1);
			//System.out.println("Loaded: "+t[i]+" : "+_image_data[i]);
		}
	}


	/**
	 * Optionally set the zero reference for the time series, i.e. the reference time compared
	 * to which the t-coordinate of the images will be computed.
	 * @param cal
	 */
	public void setDateZero(Calendar cal) {
		date0 = cal;
		// rebuild t
		for (int i=0; i<imageList.size(); i++) {
			DatedQCImage dImage = imageList.get(i);
			t[i] = diffDays(dImage);
		}
	}


	/**
	 * 
	 * @return
	 */
	public DatedQCImage getLast() {
		if (imageList != null) {
			return imageList.get(imageList.size()-1);
		}
		return null;
	} 

	/**
	 * 
	 * @param i
	 * @return
	 */
	public DatedQCImage getI(int i) {
		if (imageList != null) {
			return imageList.get(i);
		}
		return null;
	} 

	/**
	 * Get the overall length of the time series rounded to the nearest number of days.
	 * @return
	 */
	public int getLengthDays() {
		return diffDays(imageList.get(0).cal, getLast().cal);
	}

	/**
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static int diffDays(Calendar c1, Calendar c2) {
		long milliseconds1 = c1.getTimeInMillis();
		long milliseconds2 = c2.getTimeInMillis();
		long diff = milliseconds2 - milliseconds1;
		return Math.round(diff / (24 * 60 * 60 * 1000));
	}

	/**
	 * 
	 * @param im1
	 * @param im2
	 * @return
	 */
	public int diffDays(DatedQCImage im2) {
		return diffDays(date0, im2.cal);
	}

	/**
	 * Return size of the list.
	 * @return
	 */
	public int getLengthImages() {
		return imageList.size();
	}

	@Override
	public String toString() {
		String out = "";
		for (int i=0; i<imageList.size(); i++) {
			out+=imageList.get(i)+"\n";
		}
		return out;
	}

	/**
	 * Due to disk read loads, synchonized, so multiple this.getSeries() requests don't occur.
	 * @param pt is a georeferenced Point
	 * @return a list of {t, value} double arrays.
	 */
	public synchronized List<double[]> getSeries(Point pt) {		
		return getSeries(pt.getX(), pt.getY());
	}


	/**
	 * @param x is a georeferenced coordinate
	 * @param y is a georeferenced coordinate
	 * @return a list of {t, value} double arrays.
	 */
	public synchronized List<double[]> getSeries(double x, double y) {		
		LinkedList<double[]> out = new LinkedList<double[]>();

		for (int i=0; i<imageList.size(); i++) {
			try {
				// else, write the time offset and the image data
				double data = _image_data[i].imageValue(x, y, 1);
				out.add(new double[] {t[i], data});
			} catch (Exception e1) {
				e1.printStackTrace();
			} 
		}
		return out;
	}
	
	/**
	 * Get a Y-vector from WorldClim.  Should be length 12 (months).
	 * @param x is a georeferenced coordinate
	 * @param y is a georeferenced coordinate
	 * @return a vector of Y values with NaN for missing data
	 */
	public synchronized double[] getY(double x, double y) {		
		double[] y_vec = new double[imageList.size()];
		for (int i=0; i<imageList.size(); i++) {
			try {
				// else, write the time offset and the image data
				y_vec[i] = _image_data[i].imageValue(x, y, 1);
			} catch (Exception e1) {
				y_vec[i] = Double.NaN;
				e1.printStackTrace();
			} 
		}
		return y_vec;
	}

	/**
	 * Get a complete X vector for the time series.
	 * @return
	 */
	public double[] getX() {
		return t;
	}

	/**
	 * 
	 */
	public void close() {
		for (int i=0; i<imageList.size(); i++) {
			_image_data[i].deleteDataSet();
		}
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

	/*
	 * 
	 */
	public static void main(String[] args) {
		
//		String[] evi = new String[] {"/Volumes/LacieExFat/MOD13A2/2010", "/Volumes/LacieExFat/MOD13A2/2011"};
//		String eviDir = "EVI";
//		String eviQCDir = "VI_QC";
//		BitCheck mod13Checker = new BitCheck() {
//			@Override
//			public boolean isOK(int check) {
//				return BitChecker.mod13ok(check);
//			}
//			
//		};
//		try {
//			ImageLoadr4 responseLoadr = new ImageLoadr4(evi, eviDir, eviQCDir, mod13Checker);
//			double x = -120.0;
//			for (double y=30; y<40.0; y+=0.5) {
//				List<double[]> series = responseLoadr.getSeries(x,y);
//				System.out.println("Point: "+Arrays.toString(new double[] {x,y})+"Length: "+series.size());
//				for (double[] t : series) {
//					System.out.println("\t"+Arrays.toString(t));
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// in the previous, the BlockXSize returned from Band is the same as the image line size
		
//		try {
//			ImageLoadr5 loadr = new ImageLoadr5(new String[] {"/Users/nclinton/Documents/XJY_carbon/prec/"});
//			double x = -120.0;
//			for (double y=34.5; y<40.5; y+=0.5) {
//				List<double[]> series = loadr.getSeries(x,y);
//				System.out.println("Point: "+Arrays.toString(new double[] {x,y})+"Length: "+series.size());
//				for (double[] t : series) {
//					System.out.println("\t"+Arrays.toString(t));
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// In this image, the BlockXSize returned from Band is NOT the same as the image line size
		
		// EXAMPLE:
		try {
			// Directory containing WorldClim data as tiffs:
			ImageLoadr5 loadr = new ImageLoadr5(new String[] {"/Users/nclinton/Documents/XJY_carbon/prec/"});
			// coordinates of a random point
			double x = -120.0;
			double y = 39.5;
			// get a list of double[]'s, where each double[] is {t, value}, t in days from t0, value in units of...?
			List<double[]> series = loadr.getSeries(x,y);
			System.out.println("Point: "+Arrays.toString(new double[] {x,y})+"Length: "+series.size());
			// print the {t, value} data
			for (double[] t : series) {
				System.out.println("\t"+Arrays.toString(t));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
