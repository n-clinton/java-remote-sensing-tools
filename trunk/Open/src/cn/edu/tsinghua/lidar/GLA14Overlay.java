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

import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 *
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
	
	public GLA14Overlay(String imageName, String outName) throws Exception {
		// overlay image
		image = JAIUtils.readImage(imageName);
		JAIUtils.register(image);
		deltaX = ((Double) image.getProperty("deltaX")).doubleValue();
		deltaY = ((Double) image.getProperty("deltaY")).doubleValue();
		iterator = RandomIterFactory.create(image, null);
		
		// output
		writer = new BufferedWriter(new FileWriter(outName));
		String header = "rec_ndx,lat,lon,elev,SigEndOff," +
				"SigBegHt,gpCntRngOff2,gpCntRngOff3,gpCntRngOff4,gpCntRngOff5,gpCntRngOff6," +
				"gAmp1,gAmp2,gAmp3,gAmp4,gAmp5,gAmp6," +
				"gArea1,gArea2,gArea3,gArea4,gArea5,gArea6," +
				"population,areakm,popperkm";
		writer.write(header+"\n");
	}
	
	/**
	 * 
	 * @param dirName
	 * @param imageName
	 * @throws Exception
	 */
	public void processDir(String dirName) throws Exception {
		// input(s)
		File dir = new File(dirName);
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File f : files) {
				try {
					// catch these exceptions, try the next file
					if (f.getName().endsWith(".csv")) {
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
	 * 
	 * @param file
	 * @throws Exception
	 */
	public void processFile(File file) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		// skip the header
		String line = reader.readLine();
		while ((line = reader.readLine()) != null) {
			try {
				/*
				 * rec_ndx,lat,lon,elev,SigEndOff,SigBegHt,gpCntRngOff2,gpCntRngOff3,gpCntRngOff4,gpCntRngOff5,gpCntRngOff6,gAmp1,gAmp2,gAmp3,gAmp4,gAmp5,gAmp6,gArea1,gArea2,gArea3,gArea4,gArea5,gArea6
				 */
				String[] toks = line.split(",");
				double lat = Double.parseDouble(toks[1]);
				// don't bother with high latitudes
				if (lat < -60.0 || lat > 70) { continue; }
				double lon = Double.parseDouble(toks[2]);
				// convert to [-180, 180] scale
				if (lon > 180.0) { lon = lon-360.0; }
				// overlay
				Point pt = (new GeometryFactory()).createPoint(new Coordinate(lon, lat));
				double val;
				val = JAIUtils.imageValue(pt, image, iterator);
				// no need to record these non-data points
				if (val <= 0.0) { continue; }
				// compute approximate pixel area in square kilometers
				double area = Math.cos(Math.abs(lat*Math.PI/180.0))*Math.abs(deltaX*deltaY)*60.0*60.0*1.852*1.852;
				System.out.println(line+val+","+String.format("%.4f", area)+","+String.format("%.4f", val/area));
				writer.write(line+val+","+String.format("%.4f", area)+","+String.format("%.4f", val/area)+"\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		reader.close();
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
		
		String gpt2pop = "C:/Users/Nicholas/Documents/urban/Landscan2010/derived_data/gpt2pop_.tif";
		String glaDir = "C:/Users/Nicholas/Documents/GLA14.031.out/";
		String outFile = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2.csv";
		try {
			(new GLA14Overlay(gpt2pop, outFile)).processDir(glaDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
