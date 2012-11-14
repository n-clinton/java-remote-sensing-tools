/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;

import ru.sscc.spline.Spline;

import cn.edu.tsinghua.lidar.BitChecker;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.ArrayFunction;
import com.berkenviro.imageprocessing.GDALUtils;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 *
 */
public class ImageLoadr2 {

	
	private ArrayList<DatedQCImage> imageList;
	private double[] x;
	
	/**
	 * 
	 * @param directories
	 * @throws Exception
	 */
	public ImageLoadr2(String[] directories) throws Exception {
		System.out.println("Initializing image loader...");
		imageList = new ArrayList<DatedQCImage>();
		
		for (int d=0; d<directories.length; d++) {
			File dir = new File(directories[d]);
			if (!dir.isDirectory()) {
				throw new Exception("Jackass!  Invalid directory assigned.");
			}
			File[] dates = dir.listFiles();
			for (File f : dates) {
				// skip files that may be in the directory
				if (f.isFile()) { continue; }
				// Instantiate a new image Object
				DatedQCImage image = new DatedQCImage();
				// parse the directory name to get a date
				String[] ymd = f.getName().split("\\.");
				Calendar c = Calendar.getInstance();
				c.set(Integer.parseInt(ymd[0]), Integer.parseInt(ymd[1])-1, Integer.parseInt(ymd[2]));
				image.cal = c;
				// find the image in a subdirectory
				File imageDir = new File(f.getPath()+"/EVI");
				for (File iFile : imageDir.listFiles()) {
					if (iFile.getName().endsWith(".tif")) {
						image.imageName = iFile.getAbsolutePath();
					}
				}
				// find the QC image in another subdirectory
				File qcDir = new File(f.getPath()+"/VI_QC");
				for (File qcFile : qcDir.listFiles()) {
					if (qcFile.getName().endsWith(".tif")) {
						image.qcImageName = qcFile.getAbsolutePath();
					}
				}
				//System.out.println(image);
				imageList.add(image);
			}
		}
		
		// Keep the list in chronological order
		Collections.sort(imageList);
		System.out.println("\t Done!");
		
		// set the X vector
		x = new double[imageList.size()];
		for (int i=0; i<imageList.size(); i++) {
			DatedQCImage dImage = imageList.get(i);
			x[i] = diffDays(getFirst(), dImage);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public DatedQCImage getFirst() {
		if (imageList != null) {
			return imageList.get(0);
		}
		return null;
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
		return diffDays(getFirst().cal, getLast().cal);
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
	public int diffDays(DatedQCImage im1, DatedQCImage im2) {
		return diffDays(im1.cal, im2.cal);
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
	 * 
	 * @param pt
	 * @return
	 */
	public List<double[]> getSeries(Point pt) {		
		LinkedList<double[]> out = new LinkedList<double[]>();
		// iterate over images
		for (int i=0; i<imageList.size(); i++) {
			DatedQCImage dImage = imageList.get(i);
			Dataset image = GDALUtils.getDataset(dImage.imageName);
			Dataset qcImage = GDALUtils.getDataset(dImage.qcImageName);
			try {
				int qc = (int)GDALUtils.imageValue(qcImage, pt, 1);
				if (!BitChecker.mod13ok(qc)) {
					//System.err.println("Bad data at "+pt+" t="+dImage.cal.getTime());
					continue;
				}
				// else, write the time offset and the image data
				double t = diffDays(getFirst(), dImage);
				double data = GDALUtils.imageValue(image, pt, 1);
				out.add(new double[] {t, data});
			} catch (Exception e1) {
				e1.printStackTrace();
			} finally {
				gdal.Unlink(dImage.imageName);
				gdal.Unlink(dImage.qcImageName);
			}
		}
		return out;
	}
	
	/**
	 * Get a complete X vector for the time series.
	 * @return
	 */
	public double[] getX() {
		return x;
	}

	/**
	 * Fit a thin plate spline to the series and interpolate missing values.
	 * This will fail if first and/or last values are missing, i.e. there is no extrapolation.
	 * @param pt is a georeferenced point with the same coordinate system as the images.
	 * @return a vector of Y values where missing values are interpolated
	 */
	public double[] getY(Point pt) throws Exception {
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
		else if (series.get(0)[0] > x[0]) { // do not extrapolate in the beginning
			throw new Exception("Start of series out of range: "+series.get(0)[0]);
		}
		else if (series.get(series.size()-1)[0] < x[x.length-1]) { // do not extrapolate at the end
			throw new Exception("End of series out of range: "+series.get(series.size()-1)[0]);
		}
		double[][] xy = TSUtils.getSeriesAsArray(series);
		// fit a spline to interpolate
		Spline spline = TSUtils.duchonSpline(xy[0], xy[1]);
		return TSUtils.evaluateSpline(spline, x);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// 2010, 2011 EVI
		String dir1 = "D:/MOD13A2/2010/";
		String dir2 = "D:/MOD13A2/2011/";
		
		try {
			ImageLoadr2 loadr = new ImageLoadr2(new String[] {dir2, dir1});
			List<double[]> series = loadr.getSeries(GISUtils.makePoint(-121.0, 38.0));
			for (double[] datapoint : series) {
				System.out.println(datapoint[0]+","+datapoint[1]);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
