/**
 * 
 */
package cn.edu.tsinghua.lidar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.geotools.referencing.datum.DefaultGeodeticDatum;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 *
 * This class is for overlaying points stored in a table on an image.  The various write methods
 * all write different headers.  This is a legacy of the fact the original class was designed to operate
 * on multiple files, with the header getting skipped every time.  TODO: fix that header problem.
 */
public class GLA14Overlay {

	static
	{
		System.setProperty("com.sun.media.jai.disableMediaLib", "true");
	}
	
	private PlanarImage image;
	private double deltaX;
	private double deltaY;
	private RandomIter iterator;
	private BufferedWriter writer;
	private String year;
	
	public GLA14Overlay(String imageName, String outName) throws Exception {
		// overlay image
		image = JAIUtils.readImage(imageName);
		JAIUtils.register(image);
		deltaX = ((Double) image.getProperty("deltaX")).doubleValue();
		deltaY = ((Double) image.getProperty("deltaY")).doubleValue();
		iterator = RandomIterFactory.create(image, null);
		// output
		writer = new BufferedWriter(new FileWriter(outName));
	}
	
	/**
	 * Add on.
	 * @param theYear should be one of {"2003", "2004", "2005", "2006", "2007", "2008", "2009"}
	 */
	public void setYear(String theYear) {
		year = theYear;
	}
	
	/**
	 * 
	 * @param dirName
	 * @param imageName
	 * @throws Exception
	 */
	public void processDir(String dirName) throws Exception {
		String header = "rec_ndx,lat,lon,elev,SigEndOff," +
				"SigBegHt,gpCntRngOff2,gpCntRngOff3,gpCntRngOff4,gpCntRngOff5,gpCntRngOff6," +
				"gAmp1,gAmp2,gAmp3,gAmp4,gAmp5,gAmp6," +
				"gArea1,gArea2,gArea3,gArea4,gArea5,gArea6," +
				"population,areakm,popperkm,year,month,day";
		writer.write(header+"\n");
		// input(s)
		File dir = new File(dirName);
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File f : files) {
				try {
					// catch these exceptions, try the next file
					if (f.getName().endsWith(".csv")) {
						// if the year variable has been set
						if (year != null) { 
							// if the filename is not prefixed by year, skip
							if (!f.getName().startsWith(year)) { 
								continue;
							}
						}
						processFile(f);
						writer.flush();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		else {
			// if problem, exception will be thrown to the caller
			processFile(dir);
		}
		writer.close();
	}
		
	
	/**
	 * Original method.  Custom for GLA14 output and Landscan.
	 * @param file
	 * @throws Exception
	 */
	private void processFile(File file) throws Exception {
		processFile(file, 2, 1);
	}
		
	
	/**
	 * Generic method for Landscan 
	 * @param file
	 * @throws Exception
	 */
	private void processFile(File file, int lonInd, int latInd) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		// skip the header
		String line = reader.readLine();
		while ((line = reader.readLine()) != null) {
			try {
				String[] toks = line.split(",");
				double lat	= Double.parseDouble(toks[latInd]);
				double lon	= Double.parseDouble(toks[lonInd]);
				// convert to [-180, 180] scale
				if (lon > 180.0) { lon = lon-360.0; }
				// don't bother with high latitudes
				if (lat < -60.0 || lat > 70) { continue; }
				// overlay
				int[] pixelXY = JAIUtils.getPixelXY(new double[] {lon, lat}, image);
				double val = iterator.getSampleDouble(pixelXY[0],pixelXY[1],0);
				// no need to record these non-data points
				if (val <= 0.0) { continue; }
				// compute approximate pixel area in square kilometers
				double[] pixelProj = JAIUtils.getProjectedXY(pixelXY, image);
				double area = Math.cos(Math.abs(pixelProj[1]*Math.PI/180.0))*Math.abs(deltaX*deltaY)*60.0*60.0*1.852*1.852;
				// append population data (already an extra comma at the end of these files)
				line = line + val + "," + String.format("%.4f,%.4f,", area, val/area);
				// append date information
				String[] fileToks = file.getName().split("_");
				line = line + fileToks[0] + "," + fileToks[1] + "," + fileToks[2];
				System.out.println(line);
				writer.write(line);
				writer.newLine();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		reader.close();
	}
	
	/**
	 * 
	 * @param fileName
	 * @throws Exception
	 */
	public void processGLA14Summary(String fileName) throws Exception {
		String header = "lat,lon,ht2003,n2003,ht2004,n2004,ht2005,n2005,ht2006,n2006,ht2007,n2007,ht2008,n2008,ht2009,n2009,average,n," +
				"population,areakm,popperkm";
		writer.write(header+"\n");
		
		File file = new File(fileName);
		processFile(file, 1, 0);
		writer.close();
	}
	
	/**
	 * 
	 * @param fileName
	 * @throws Exception
	 */
	public void processGLA14Summary2(String fileName) throws Exception {
		String header = "lat,lon,sigBeg,top,second,third,avg1,avg2,avg3,wtd1,wtd2,wtd3," +
				"population,areakm,popperkm";
		writer.write(header+"\n");
		
		File file = new File(fileName);
		processFile(file, 1, 0);
		writer.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 20120219 check
//		String gpt2pop = "C:/Users/Nicholas/Documents/urban/Landscan2010/derived_data/gpt2pop_.tif";
//		String glaFile = "C:/Users/Nicholas/Documents/GLA14.031.out/2009_10_10_GLA14_531_2131_002_0071_0_01_0001_out.csv";
//		String outCheck = "C:/Users/Nicholas/Documents/urban/Landscan2010/derived_data/shapefiles/" +
//				"2009_10_10_GLA14_531_2131_002_0071_0_01_0001_test_compare.csv";
//		try {
//			(new GLA14Overlay(gpt2pop, outCheck)).processDir(glaFile);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		String gpt2pop = "C:/Users/Nicholas/Documents/urban/Landscan2010/derived_data/gpt2pop_.tif";
//		String glaDir = "C:/Users/Nicholas/Documents/GLA14.031.out/";
//		String outFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu.csv";
//		try {
//			(new GLA14Overlay(gpt2pop, outFile)).processDir(glaDir);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		String gpt2pop = "C:/Users/Nicholas/Documents/urban/Landscan2010/derived_data/gpt2pop_.tif";
//		String glaDir = "C:/Users/Nicholas/Documents/GLA14.031.out/";
//		
//		String outFile;
//		try {
//			outFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2003.csv";
//			GLA14Overlay overlay = new GLA14Overlay(gpt2pop, outFile);
//			overlay.setYear("2003");
//			overlay.processDir(glaDir);
//			
//			outFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2004.csv";
//			overlay = new GLA14Overlay(gpt2pop, outFile);
//			overlay.setYear("2004");
//			overlay.processDir(glaDir);
//			
//			outFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2005.csv";
//			overlay = new GLA14Overlay(gpt2pop, outFile);
//			overlay.setYear("2005");
//			overlay.processDir(glaDir);
//			
//			outFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2006.csv";
//			overlay = new GLA14Overlay(gpt2pop, outFile);
//			overlay.setYear("2006");
//			overlay.processDir(glaDir);
//			
//			outFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2007.csv";
//			overlay = new GLA14Overlay(gpt2pop, outFile);
//			overlay.setYear("2007");
//			overlay.processDir(glaDir);
//			
//			outFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2008.csv";
//			overlay = new GLA14Overlay(gpt2pop, outFile);
//			overlay.setYear("2008");
//			overlay.processDir(glaDir);
//			
//			outFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2009.csv";
//			overlay = new GLA14Overlay(gpt2pop, outFile);
//			overlay.setYear("2009");
//			overlay.processDir(glaDir);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	
		// overlay pixel averages with population
//		String gpt2pop = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/gpt2pop_.tif";
//		String overlayFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/overlay_2003_2009_pixels.csv";
//		String outFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/overlay_2003_2009_pixels_overlayed2.csv";
//		try {
//			GLA14Overlay overlay = new GLA14Overlay(gpt2pop, outFile);
//			overlay.processGLA14Summary2(overlayFile);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// Release 33, 20120328
		String gpt2pop = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/gpt2pop_.tif";
		String glaDir = "C:/Users/Nicholas/Documents/GLA14.033.out/";
		String outFile = "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/GLA14_r33_mssu.csv";
		try {
			(new GLA14Overlay(gpt2pop, outFile)).processDir(glaDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

	
}
