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
import java.util.TreeMap;

import ru.sscc.spline.Spline;
import cn.edu.tsinghua.gui.Graph;
import cn.edu.tsinghua.lidar.BitChecker;
import cn.edu.tsinghua.modis.BitCheck;

import com.berkenviro.imageprocessing.ImageData;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas Clinton
 * @author Cong Hui He
 * 
 * Modified version of ImageLoadr4 to incorporate pixel level date information
 */
public class ImageLoadr6 implements Loadr {

	private ArrayList<DatedQCImage> imageList;
	private ImageData[] _image_data;
	private ImageData[] _qc_image_data;
	private ImageData[] _doy_image_data;
	
	private BitCheck bitChecker;

	/**
	 * @param directories is an array of top-level directories expected to contain 
	 * 	subdirectories named by date, according to the convention of the USGS archives
	 * @param dataDir is the name of the subdirectory (under date) containing the image data
	 * @param qaqcDir is the name of the subdirectory (under date) containing the QA/QC data
	 * @param doyDir is the name of the subdirectory (under date) containing the DOY data
	 * @param bitChecker is a BitChecker used to evaluate the QC data
	 * @throws Exception 
	 */
	public ImageLoadr6(String[] directories, String dataDir, String qaqcDir, String doyDir, BitCheck bitChecker) throws Exception {
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
				// Instantiate a new image Object
				DatedQCImage image = new DatedQCImage();
				// parse the directory name to get a date
				// this will be adjusted by the 
				String[] ymd = f.getName().split("\\.");
				Calendar c = Calendar.getInstance();
				c.set(Integer.parseInt(ymd[0]), Integer.parseInt(ymd[1])-1, Integer.parseInt(ymd[2]));
				image.cal = c;
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
				// find the DOY image in another subdirectory
				File dayDir = new File(f.getPath()+"/"+doyDir);
				for (File dayFile : dayDir.listFiles()) {
					if (dayFile.getName().endsWith(".tif")) {
						image.dateName = dayFile.getAbsolutePath();
					}
				}
				//				System.out.println(image);
				imageList.add(image);
			}
		}

		// Keep the list in chronological order
		Collections.sort(imageList);
		System.out.println("\t Done!");

		_image_data = new ImageData[imageList.size()];
		_qc_image_data = new ImageData[imageList.size()];
		_doy_image_data = new ImageData[imageList.size()];

		for (int i=0; i<imageList.size(); i++) {
			DatedQCImage dImage = imageList.get(i);
			// set Image Data
			_qc_image_data[i] = new ImageData(dImage.qcImageName, 1);
			_image_data[i] = new ImageData(dImage.imageName, 1);
			_doy_image_data[i] = new ImageData(dImage.dateName, 1);
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

		// zero reference for this time series
		Calendar cal0 = imageList.get(0).cal;
		int doy0 = (int)_doy_image_data[0].imageValue(x, y, 1);
		cal0.set(Calendar.DAY_OF_YEAR, doy0);
		
		for (int i=0; i<imageList.size(); i++) {
			try {
				int qc = (int)_qc_image_data[i].imageValue(x, y, 1);
				if (!bitChecker.isOK(qc)) {
//					System.err.println("Bad data: " + qc);
					continue;
				}
				
				// time
				Calendar cal = imageList.get(i).cal;
				int doy = (int)_doy_image_data[i].imageValue(x, y, 1);
				// account for annual roll-overs
				if ((cal.get(Calendar.DAY_OF_YEAR) - doy) > 16) {
					cal.set(Calendar.YEAR, cal.get(Calendar.YEAR)+1);
					cal.set(Calendar.DAY_OF_YEAR, doy);
				}
				else if ((cal.get(Calendar.DAY_OF_YEAR) - doy) < -16) {
					cal.set(Calendar.YEAR, cal.get(Calendar.YEAR)-1);
					cal.set(Calendar.DAY_OF_YEAR, doy);
				} else {
					cal.set(Calendar.DAY_OF_YEAR, doy);
				}
				
				double t = diffDays(cal0, cal);
				// check if this is a duplicate data point
				// can happen at the annual boundary of composites
				if (out.size() > 0) {
					double[] last = out.getLast();
					if (last[0] == t) {
						continue; // already have this data point.
					}
				}
				double data = _image_data[i].imageValue(x, y, 1);
				// write the time offset and the image data
				out.add(new double[] {t, data});
				
			} catch (Exception e1) {
				e1.printStackTrace();
			} 
		}
		return out;
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

	
	// The following three need to be re-implemented
	@Override
	public void setDateZero(Calendar cal) {}

	@Override
	public double[] getY(Point pt) throws Exception {
		return null;
	}

	@Override
	public double[] getX() {
		return null;
	}


	/*
	 * 
	 */
	public static void main(String[] args) {
//		String[] evi = new String[] {"/data/MOD13A2/2010", "/data/MOD13A2/2011"};
//		String eviDir = "EVI";
//		String eviQCDir = "VI_QC";
//		String doyDir = "DOY";
//		BitCheck mod13Checker = new BitCheck() {
//			@Override
//			public boolean isOK(int check) {
//				return BitChecker.mod13ok(check);
//			}
//		};
//		try {
//
//			ImageLoadr4 loadr4 = new ImageLoadr4(evi, eviDir, eviQCDir, mod13Checker);
//			ImageLoadr6 loadr6 = new ImageLoadr6(evi, eviDir, eviQCDir, doyDir, mod13Checker);
//			double x = 71.0;
//			for (double y=48.0; y<70.0; y+=0.5) {
//				List<double[]> series4 = loadr4.getSeries(x,y);
//				List<double[]> series6 = loadr6.getSeries(x,y);
//				System.out.println("Point: "+Arrays.toString(new double[] {x,y})
//						+"Length 4:"+series4.size()+" Length 6:"+series6.size());
//				for (int t=0; t<series4.size(); t++) {
//					System.out.println("\tloadr4:"+Arrays.toString(series4.get(t))+" loadr6:"+Arrays.toString(series6.get(t)));
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// 20140321 blank spots check
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
		try {

			ImageLoadr4 loadr4 = new ImageLoadr4(evi, eviDir, eviQCDir, mod13Checker);
			ImageLoadr6 loadr6 = new ImageLoadr6(evi, eviDir, eviQCDir, doyDir, mod13Checker);
			Calendar cal = Calendar.getInstance();
			cal.set(2010, 0, 1);
			loadr6.setDateZero(cal);
//			double x = -87.9108613514;
//			double y = 40.4467308069;
//			double x = 135.2;
//			double y = -24.0;
			double x = 133.4;
			double y = -13.3;
//			List<double[]> series4 = loadr4.getSeries(x,y);
			List<double[]> series6 = loadr6.getSeries(x,y);
			System.out.println("Point: "+Arrays.toString(new double[] {x,y})
//					+"Length 4 = "+series4.size()+" Length 6 = "+series6.size());
					+" Length 6 = "+series6.size());
			System.out.println("max "+series6.get(series6.size() - 1)[0]);
			for (int t=0; t<series6.size(); t++) {
//				System.out.println("\tloadr4:"+Arrays.toString(series4.get(t))+" loadr6:"+Arrays.toString(series6.get(t)));
				System.out.println("\t loadr6:"+Arrays.toString(series6.get(t)));
			}
			
			double[][] xy = TSUtils.getSeriesAsArray(series6);
			// graph = new Graph(xy);
			//Spline rSpline = TSUtils.duchonSpline(xy[0], xy[1]);
			TreeMap<Double, Double> map = TSUtils.getPieceWise(series6, 64);
			for (Double d : map.keySet()) {
				System.out.println(d+", "+map.get(d));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
}
