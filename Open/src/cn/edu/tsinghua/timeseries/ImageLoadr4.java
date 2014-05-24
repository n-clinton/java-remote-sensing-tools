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

import org.gdal.gdal.gdal;

import cn.edu.tsinghua.gui.Graph;
import cn.edu.tsinghua.lidar.BitChecker;
import cn.edu.tsinghua.modis.BitCheck;

import com.berkenviro.imageprocessing.ArrayFunction;
import com.berkenviro.imageprocessing.ImageData;
import com.berkenviro.imageprocessing.SplineFunction;
import com.vividsolutions.jts.geom.Point;

import flanagan.complex.ComplexMatrix;

/**
 * @author Nicholas Clinton
 * @author Cong Hui He
 * 
 * Load a time series of imagery from a MODIS product.
 * 20131007. Clean up.  
 * 20131012. Move the test code to another class called ImageLoadr4Test
 */
public class ImageLoadr4 implements Loadr {

	private ArrayList<DatedQCImage> imageList;
	private double[] t;
	private Calendar date0;
	private ImageData[] _image_data;
	private ImageData[] _qc_image_data;
	
	private BitCheck bitChecker;

	/**
	 * @param directories is an array of top-level directories expected to contain 
	 * 	subdirectories named by date, according to the convention of the USGS archives
	 * @param dataDir is the name of the subdirectory (under date) containing the image data
	 * @param qaqcDir is the name of the subdirectory (under date) containing the QA/QC data
	 * @param bitChecker is a BitChecker used to evaluate the QC data
	 * @throws Exception 
	 */
	public ImageLoadr4(String[] directories, String dataDir, String qaqcDir, BitCheck bitChecker) throws Exception {
		System.out.println("Initializing image loader...");
		
		this.bitChecker = bitChecker;
		
		imageList = new ArrayList<DatedQCImage>();

		for (int d=0; d<directories.length; d++) {
			File dir = new File(directories[d]);
			if (!dir.isDirectory()) {
				throw new Exception("Jackass!  Invalid directory assigned. Directory " + dir.getPath() + " not found.");
			}
			File[] dates = dir.listFiles();
			for (File f : dates) {
				// skip files that may be in the directory
				if (f.isFile()) { continue; }
				// parse the directory name to get a date
				String[] ymd = f.getName().split("\\.");
				Calendar c = Calendar.getInstance();
				c.set(Integer.parseInt(ymd[0]), Integer.parseInt(ymd[1])-1, Integer.parseInt(ymd[2]));
				// Instantiate a new image Object
				DatedQCImage image = new DatedQCImage(c);
				// find the image in a subdirectory
				File imageDir = new File(f.getPath()+"/"+dataDir);
				for (File iFile : imageDir.listFiles()) {
					if (iFile.getName().endsWith(".tif")) {
						image.imageName = iFile.getAbsolutePath();
					}
				}
				// find the QC image in another subdirectory
				File qcDir = new File(f.getPath()+"/"+qaqcDir);
				for (File qcFile : qcDir.listFiles()) {
					if (qcFile.getName().endsWith(".tif")) {
						image.qcImageName = qcFile.getAbsolutePath();
					}
				}
				//				System.out.println(image);
				imageList.add(image);
			}
		}

		// Keep the list in chronological order
		Collections.sort(imageList);
		System.out.println("\t Done!");

		// reference to the first image, unless otherwise specified
		date0 = imageList.get(0).cal;

		_image_data = new ImageData[imageList.size()];
		_qc_image_data = new ImageData[imageList.size()];

		// set the time vector
		t = new double[imageList.size()];
		for (int i=0; i<imageList.size(); i++) {
			DatedQCImage dImage = imageList.get(i);
			t[i] = diffDays(dImage);

			// set Image Data
			_qc_image_data[i] = new ImageData(dImage.qcImageName, 1);
			_image_data[i] = new ImageData(dImage.imageName, 1);
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
	 * WARNING!  When run on MOD13A2 data, this may result in DUPLICATE data points
	 * at the end of one year and beginning of the next.  It may not be obvious because 
	 * the nominal 16-day composite times are different.  This problem became apparent with 
	 * ImageLoadr6.
	 * 
	 * @param x is a georeferenced coordinate
	 * @param y is a georeferenced coordinate
	 * @return a list of {t, value} double arrays.
	 */
	public synchronized List<double[]> getSeries(double x, double y) {		
		LinkedList<double[]> out = new LinkedList<double[]>();

		for (int i=0; i<imageList.size(); i++) {
			try {
				int qc = (int)_qc_image_data[i].imageValue(x, y, 1);
				if (!bitChecker.isOK(qc)) {
//										System.err.println("Bad data: " + qc);
					continue;
				}

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
			_qc_image_data[i].deleteDataSet();
		}
	}

	/**
	 * WARNING!  When run on MOD13A2 data, this may result in DUPLICATE data points. 
	 * 
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
		DuchonSplineFunction spline = new DuchonSplineFunction(xy);
		return TSUtils.evaluateSpline(spline, t);
	}

	/*
	 * 
	 */
	public static void main(String[] args) {
//		String[] evi = new String[] {"D:/MOD13A2/2010", "D:/MOD13A2/2011"};
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
//			double x = 71.0;
//			for (double y=48.0; y<70.0; y+=0.5) {
//				List<double[]> series = responseLoadr.getSeries(x,y);
//				System.out.println("Point: "+Arrays.toString(new double[] {x,y})+"Length: "+series.size());
//				for (double[] t : series) {
//					System.out.println("\t"+Arrays.toString(t));
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20140321 check blank spots
//		String[] evi = new String[] {"/data/MOD13A2/2010", "/data/MOD13A2/2011"};
//		String eviDir = "EVI";
//		String eviQCDir = "VI_QC";
//		BitCheck mod13Checker = new BitCheck() {
//			@Override
//			public boolean isOK(int check) {
//				return BitChecker.mod13ok(check);
//			}
//		};
//		try {
//			ImageLoadr4 responseLoadr = new ImageLoadr4(evi, eviDir, eviQCDir, mod13Checker);
//			double x = -87.9108613514;
//			double y = 40.4467308069;
//			List<double[]> series = responseLoadr.getSeries(x,y);
//			System.out.println("Point: "+Arrays.toString(new double[] {x,y})+"Length: "+series.size());
//			for (double[] t : series) {
//				System.out.println("\t"+Arrays.toString(t));
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		try {
			String[] temperature = new String[] {"/data/MYD11A2/2008", "/data/MYD11A2/2009", "/data/MYD11A2/2010", "/data/MYD11A2/2011"};
			String tempDir = "LST_DAY";
			String tempQCDir = "QC_DAY";
			BitCheck mod11Checker = new BitCheck() {
				@Override
				public boolean isOK(int check) {
					return BitChecker.mod11ok(check);
				}
			};
			ImageLoadr4 loadr4 = new ImageLoadr4(temperature, tempDir, tempQCDir, mod11Checker);
			Calendar cal = Calendar.getInstance();
			cal.set(2008, 0, 0);
			loadr4.setDateZero(cal);
			//		double x = -87.9108613514;
			//		double y = 40.4467308069;
			//		double x = 135.2;
			//		double y = -24.0;
			double x = 133.4;
			double y = -13.3;
			List<double[]> series4 = loadr4.getSeries(x,y);
			System.out.println("Point: "+Arrays.toString(new double[] {x,y})
					+"Length 4 = "+series4.size());

			System.out.println("max "+series4.get(series4.size() - 1)[0]);
			for (int t=0; t<series4.size(); t++) {
				System.out.println(Arrays.toString(series4.get(t)).replace("[", "").replace("]", ""));
			}
			System.out.println();
			
			double[][] xy = TSUtils.getSeriesAsArray(series4);
			Graph graph = new Graph(xy);
			SplineFunction rSpline = new SplineFunction(xy);
			double[][] sVals = TSUtils.splineValues(rSpline, new double[] {16, 1432}, 1000);
			graph.addSeries(sVals);
			DuchonSplineFunction rSpline2 = new DuchonSplineFunction(xy);
			double[][] sVals2 = TSUtils.splineValues(rSpline2, new double[] {16, 1432}, 1000);
			graph.addSeries(sVals2);
			ArrayFunction function = new ArrayFunction(xy);
			System.out.println("start: "+xy[0][0]+", end: "+xy[0][xy[0].length-1]);
			double[][] smooth = TSUtils.smoothFunction(function, xy[0][0], xy[0][xy[0].length-1]+1, 0.98);
			graph.addSeries(smooth);

			Double[] map = TSUtils.getPieceWise(series4, 32);
			for (int d=0; d<map.length; d++) {
				if (map[d] != null) {
					System.out.println(d+","+map[d]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
