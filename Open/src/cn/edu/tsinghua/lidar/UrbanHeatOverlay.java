/**
 * 
 */
package cn.edu.tsinghua.lidar;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 * Overlay of a points shapefile with 2010 MOD11 thermal data at 1km.
 * The shapefile needs to have fields called GRID_CODE and POINTID.
 * POINTID is the join field to be used with the output tables.  
 * GRID_CODE is the field corresponding to the ID of the polygons used to estimate
 * the buffer of the urban area. 
 *
 */
public class UrbanHeatOverlay {
	
	public static String DAY = "DAY";
	public static String NIGHT = "NIGHT";
	
	PlanarImage data;
	PlanarImage qc;
	FeatureCollection<SimpleFeatureType, SimpleFeature> polys;
	FeatureCollection<SimpleFeatureType, SimpleFeature> id;
	File tempTable;
	
	/**
	 * 
	 * @param gptName
	 * @param dataName
	 * @param qcName
	 * @param good
	 * @param polygonName
	 */
	public UrbanHeatOverlay(String dataName, String qcName, String polygonName) throws Exception {
		data = JAIUtils.readImage(dataName);
		JAIUtils.register(data);
		qc = JAIUtils.readImage(qcName);
		JAIUtils.register(qc);
		polys =  GISUtils.getFeatureCollection(new File(polygonName));
	}
	
	/**
	 * 
	 * @param dataName
	 * @param qcName
	 * @param latticeName
	 * @param tableName
	 */
	public UrbanHeatOverlay(String dataName, String qcName, String latticeName, String tableName) {
		data = JAIUtils.readImage(dataName);
		JAIUtils.register(data);
		qc = JAIUtils.readImage(qcName);
		JAIUtils.register(qc);
		id =  GISUtils.getFeatureCollection(new File(latticeName));
		tempTable = new File(tableName);
		
	}
	
	
	/**
	 * 
	 * @param outTable
	 * @throws Exception
	 */
	public void makeUHI(String outTable) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outTable));
		writer.write("POINTID,pttemp,bufftemp");
		writer.newLine();
		
		RandomIter dataIter = RandomIterFactory.create(data, null);
		RandomIter qcIter = RandomIterFactory.create(qc, null);
		
		// read in the buffer information
		BufferedReader reader = new BufferedReader(new FileReader(tempTable));
		double[] temps = new double[193090];
		String line = reader.readLine();
		while ((line=reader.readLine()) != null) {
			String[] toks = line.split(",");
			int id = Integer.parseInt(toks[0]);
			try {
				temps[id-1] = Double.parseDouble(toks[1]);
			} catch (Exception e) {
				// catch nulls, NaN gets through
			}
		}
		
		// iterate over the lattice
		FeatureIterator<SimpleFeature> iter=id.features();
		System.out.println("Iterating over lattice..."+id);
		while (iter.hasNext()) {
			SimpleFeature feature = iter.next();
			//System.out.println("Checking "+feature);
			Geometry pt = (Point)feature.getDefaultGeometry();
			// longitude 
			double x = pt.getCoordinate().x;
			if (x > 180.0) { x = x-360.0; }
			double[] xy = {x, pt.getCoordinate().y};
			int[] dataXY = null;
			try {
				dataXY = JAIUtils.getPixelXY(xy, data);
			} catch (Exception e) {
				//System.err.println("\t Can't get pixel coordinates...");
				continue;
			}
			int qc = qcIter.getSample(dataXY[0], dataXY[1], 0);
			if (!BitChecker.mod11ok(qc)) { // if LST error >1K or other problem
				//System.out.println("\t QC: "+qc);
				continue;
			}
			double temp = dataIter.getSampleDouble(dataXY[0], dataXY[1], 0)*0.02;
			if (temp == 0 || temp < 183.95 || temp > 343.55) {
				//System.out.println("\t Temp: "+temp);
				continue;
			}

			// the following doesn't compile with Java 6, Java 7 is OK
			//int id = (int)(long)feature.getAttribute("GRID_CODE");
			// w/GLA14, really is an Integer?
			int id = (int)feature.getAttribute("GRID_CODE");
			int pointID = (int)feature.getAttribute("POINTID");
			if (temps[id-1] == 0 || temps[id-1] == Double.NaN) {
				//System.out.println("\t Buffer: "+temps[id-1]);
				continue;
			}
			
			//System.out.println("ID: "+id+" Pixel temp: "+temp+" Suburb temp: "+temps[id-1]);
			writer.write(pointID+","+temp+","+temps[id-1]);
			writer.newLine();
		}
		writer.close();
	}
	
	/**
	 * 
	 * @param fc
	 * @param outName
	 * @param image
	 * @param band
	 * @param mask
	 * @param maskValue
	 */
	public void polygonStatsTable(String outName) throws Exception {
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outName));
		writer.write("id,avg"+"\n");
		
		FeatureIterator<SimpleFeature> iter=polys.features();
		//System.out.println("Iterating over features...");
		while (iter.hasNext()) {
			String line = "";
			SimpleFeature feature = iter.next();
			// Id = FID ?= feature.getID()-1
			line+=feature.getAttribute("ID");
			Geometry p = (Geometry) feature.getDefaultGeometry();
			SummaryStatistics stats = null;
			try {
				stats = polygonStatsMasked(p);
			} catch (Exception e) {
				e.printStackTrace();
				stats = new SummaryStatistics();
			}
			line+=","+stats.getMean();
			//System.out.println(stats);
			writer.write(line);
			writer.newLine();
		}
		writer.close();
	}
	
	/**
	 * 
	 * @param p
	 * @param image
	 * @param band
	 * @param mask
	 * @param maskValue is the value that must be TRUE
	 * @return
	 * @throws Exception
	 */
	public SummaryStatistics polygonStatsMasked(Geometry p) throws Exception {
		SummaryStatistics stats = new SummaryStatistics();
		
		// bounding box
		Envelope bb = p.getEnvelopeInternal();
		// these will throw Exception if outside image bounds
		int[] ul = JAIUtils.getPixelXY(new double[] {bb.getMinX(), bb.getMaxY()}, data);
		int[] lr = JAIUtils.getPixelXY(new double[] {bb.getMaxX(), bb.getMinY()}, data);
		int minX = Math.max(ul[0]-1, 0);
		int minY = Math.max(ul[1]-1, 0);
		int maxX = Math.min(lr[0]+1, data.getWidth()-1);
		int maxY = Math.min(lr[1]+1, data.getWidth()-1);
		Rectangle bounds = new Rectangle(minX, minY, maxX-minX+1, maxY-minY+1);
		RandomIter iter = RandomIterFactory.create(data, bounds);
		RandomIter qcIter = RandomIterFactory.create(qc, bounds);
		GeometryFactory ptMakr = new GeometryFactory();
		for (int x=minX; x<=maxX; x++) {
			for (int y=minY; y<=maxY; y++) {
				// pixel centroid in projected coords
				double[] coords = JAIUtils.getProjectedXY(new int[] {x, y}, data);
				Point check = ptMakr.createPoint(new Coordinate(coords[0], coords[1]));
				//System.out.println("\t "+check);
				// if the pixel centroid is in the polygon, count it
				if (p.intersects(check)) {
					int qc = qcIter.getSample(x, y, 0);
					if (BitChecker.mod11ok(qc)) {
						double temp = iter.getSampleDouble(x, y, 0)*0.02;
						if (temp > 183.95 && temp < 343.55) { // min and max surface temperatures
							stats.addValue(temp);
						}
						//System.out.println("\t\t Adding: "+temp);
					}
//					else {
//						System.err.println("\t\t Bad data: "+maskIter.getSampleDouble(x, y, 0));
//					}
				}
			}
		}
		
		return stats;
	}
	
	/**
	 * 
	 * @param dir
	 * @param product
	 * @param polygonName
	 * @param latticeName
	 * @throws Exception
	 */
	public static void processDir(String dir, String product, String polygonName, String latticeName) throws Exception  {
		System.out.println(Calendar.getInstance().getTime());
		System.out.println("Processing directory "+dir);
		File lstDir = new File(dir+"/LST_"+product);
		File qcDir = new File(dir+"/QC_"+product);
		File[] lstList = lstDir.listFiles();
		String lstName = null;
		for (File f : lstList) {
			if (f.getName().endsWith(".tif")) {
				lstName = f.getAbsolutePath();
			}
		}
		System.out.println("\t Found LST image  "+lstName);
		File[] qcList = qcDir.listFiles();
		String qcName = null;
		for (File f : qcList) {
			if (f.getName().endsWith(".tif")) {
				qcName = f.getAbsolutePath();
			}
		}
		System.out.println("\t Found QC image  "+qcName);
		
		UrbanHeatOverlay overlay;
		
		String outMeans = lstDir+"/"+(new File(polygonName)).getName().replace(".shp", "_mean_temps.csv");
		// check for existence, continue if already there
		if (!(new File(outMeans)).exists()) {
			System.out.println("\t writing  "+outMeans);
			overlay = new UrbanHeatOverlay(lstName, qcName, polygonName);
			overlay.polygonStatsTable(outMeans);
			System.out.println("\t"+Calendar.getInstance().getTime());
		}
		
		
		String outComp = lstDir+"/"+(new File(latticeName)).getName().replace(".shp", "_mean_temps.csv");
		// check for existence, delete if already present
		if (!(new File(outComp)).exists()) {
			System.out.println("\t writing  "+outComp);
			overlay = new UrbanHeatOverlay(lstName, qcName, latticeName, outMeans);
			overlay.makeUHI(outComp);
			System.out.println(Calendar.getInstance().getTime());
		}
	}
	
	/**
	 * 
	 * @param parent
	 * @param product
	 * @param polygonName
	 * @param latticeName
	 */
	public static void processDirs(String parent, String product, String polygonName, String latticeName) {
		File parentDir = new File(parent);
		File[] dirs = parentDir.listFiles();
		for (File dir : dirs) {
			if (!(dir.isDirectory())) { continue; }
			try {
				processDir(dir.getAbsolutePath(), product, polygonName, latticeName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @param parentDir
	 * @param outTable
	 * @throws Exception
	 */
	public static void combineUHI(String parentDir, String outTable, String product, String baseTableName) throws Exception {
		combineUHI(parentDir, outTable, product, baseTableName, 0);
	}
	
	
	/**
	 * 
	 * @param parentDir
	 * @param outTable
	 * @param product
	 * @param cdd
	 * @throws Exception
	 */
	public static void combineUHI(String parentDir, String outTable, String product, String baseTableName, double cdd) throws Exception {
		File[] dirs = (new File(parentDir)).listFiles();
		
		float[][] uhis = new float[523238][2]; // column 1 is uhi, column 2 is uhs
		int[][] dates = new int[523238][2]; // dates corresponding to uhi
		
		for (File dir : dirs) {
			if (!(dir.isDirectory())) { continue; }
			System.out.println("Processing dir: " +dir.getName());
			//File latticeTab = new File(dir.getAbsolutePath()+"/LST_"+product+"/gpt2id_lattices_mean_temps.csv");
			//File latticeTab = new File(dir.getAbsolutePath()+"/LST_"+product+"/gpt2id_LA_lattice_mean_temps.csv");
			File latticeTab = new File(dir.getAbsolutePath()+"/LST_"+product+"/"+baseTableName);
			BufferedReader reader = new BufferedReader(new FileReader(latticeTab));
			reader.readLine(); // header
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] toks = line.split(",");
				int id = Integer.parseInt(toks[0]);
				float pttemp = Float.parseFloat(toks[1]);
				// check of cooling degree days
				if (pttemp < cdd) { continue; }
				float buftemp = Float.parseFloat(toks[2]);
				// compute heat island or sink
				float uh = pttemp-buftemp;
				// compute day of year
				String[] date = dir.getName().split("\\.");
				Calendar calendar = Calendar.getInstance();
				calendar.set(Integer.parseInt(date[0]), // year
							 Integer.parseInt(date[1])-1, // month
						     Integer.parseInt(date[2])); // day
				int doy = calendar.get(Calendar.DAY_OF_YEAR);
				if (uh > 0) { // heat island
					if (uh > uhis[id-1][0]) {
						// it's hotter than the buffer
						uhis[id-1][0] = uh;
						dates[id-1][0] = doy;
						System.out.println("\t Found new heat ISLAND (id, uhi, doy): "+(id-1)+", "+uh+", "+doy);
					}
				}
				else if (uh < 0) { // heat sink
					if (uh < uhis[id-1][1]) { 
						// it's colder than the buffer
						uhis[id-1][1] = uh;
						dates[id-1][1] = doy;
						System.out.println("\t Found new heat SINK (id, uhi, doy): "+(id-1)+", "+uh+", "+doy);
					}
				}
				else { // zero, error or very unlikely situation
					System.err.println("\t Buffer is the same temp as the core: "+pttemp+", "+buftemp);
				}
			}
			reader.close();
		}
		// Arrays should be full, write out

		BufferedWriter writer = new BufferedWriter(new FileWriter(outTable));
		writer.write("id,uhi,uhs,idate,sdate");
		writer.newLine();
		
		for (int i=0; i<uhis.length; i++) {
			writer.write((i+1)+","+uhis[i][0]+","+uhis[i][1]+","+dates[i][0]+","+dates[i][1]);
			writer.newLine();
		}
		writer.close();
	}
	
	
	/**
	 * 
	 * @param parentDir
	 * @param outTable
	 * @throws Exception
	 */
	public static void averageUHI(String parentDir, String outTable, String product) throws Exception {
		File[] dirs = (new File(parentDir)).listFiles();
		
		float[][] uhis = new float[523238][2]; // column 1 is sum, column 2 is ss
		int[] n = new int[523238]; // n
		
		for (File dir : dirs) {
			if (!(dir.isDirectory())) { continue; }
			System.out.println("Processing dir: " +dir.getName());
			File latticeTab = new File(dir.getAbsolutePath()+"/LST_"+product+"/gpt2id_lattices_mean_temps.csv");
			//File latticeTab = new File(dir.getAbsolutePath()+"/LST_"+product+"/gpt2id_LA_lattice_mean_temps.csv");
			BufferedReader reader = new BufferedReader(new FileReader(latticeTab));
			reader.readLine(); // header
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] toks = line.split(",");
				int id = Integer.parseInt(toks[0]);
				float pttemp = Float.parseFloat(toks[1]);
				float buftemp = Float.parseFloat(toks[2]);
				// compute heat island or sink
				float uh = pttemp-buftemp;
				uhis[id-1][0]+=uh; // sum
				uhis[id-1][1]+=uh*uh; // sum of squares
				n[id-1]++;
			}
			reader.close();
		}
		// Arrays should be full, write out
		BufferedWriter writer = new BufferedWriter(new FileWriter(outTable));
		writer.write("id,uhimean,uhisd,n");
		writer.newLine();
		
		for (int i=0; i<uhis.length; i++) {
			if (n[i]-1 > 0) {
				double mean = uhis[i][0]/n[i];
				double ms = uhis[i][1]/n[i];
				double sd = Math.sqrt(n[i]/(n[i]-1))*Math.sqrt(ms-Math.pow(mean, 2));
				writer.write((i+1)+","+mean+","+sd+","+n[i]);
			}
			else {
				writer.write((i+1)+","+0+","+0+","+0);
			}
			writer.newLine();
		}
		writer.close();
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// test
//		String urbanPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2buf3k_polygons_simple_dissolve_geo_multi_erase.shp";
//		FeatureCollection features = GISUtils.getFeatureCollection(new File(urbanPolys));
//		String lst = "D:/MOD11A2/2010.07.04/LST_NIGHT/2010.07.04_LST_NIGHT_mosaic_geo.LST_Night_1km.tif";
//		PlanarImage image = JAIUtils.readImage(lst);
//		String mask = "D:/MOD11A2/2010.07.04/QC_NIGHT/2010.07.04_QC_NIGHT_mosaic_geo.QC_Night.tif";
//		PlanarImage maskImage = JAIUtils.readImage(mask);
//		String checkTable = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/" +
//				"gpt2buf3k_polygons_simple_dissolve_geo_multi_erase_2010_07_04_mean_temp.csv";
//		try {
//			polygonStatsTable(features, checkTable, image, 0, maskImage, 0);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20120403 Reprojected 5 km polygons, erased
//		String urbanPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2_polygons_geo_buffer_5k_erased.shp";
//		String lst = "D:/MOD11A2/2010.07.04/LST_NIGHT/2010.07.04_LST_NIGHT_mosaic_geo.LST_Night_1km.tif";
//		String mask = "D:/MOD11A2/2010.07.04/QC_NIGHT/2010.07.04_QC_NIGHT_mosaic_geo.QC_Night.tif";
//		String checkTable = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/" +
//				"gpt2_polygons_geo_buffer_5k_erased_2010_07_04_mean_temp.csv";
//		try {
//			UrbanOverlay overlay = new UrbanOverlay(lst, mask, 0, urbanPolys);
//			overlay.polygonStatsTable(checkTable);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// works.  Lots of NaNs.
		
//		String latticeName = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2id_lattices.shp";
//		String lst = "D:/MOD11A2/2010.07.04/LST_NIGHT/2010.07.04_LST_NIGHT_mosaic_geo.LST_Night_1km.tif";
//		String mask = "D:/MOD11A2/2010.07.04/QC_NIGHT/2010.07.04_QC_NIGHT_mosaic_geo.QC_Night.tif"; 
//		String tempTable = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/" +
//				"gpt2_polygons_geo_buffer_5k_erased_2010_07_04_mean_temp.csv";
//		try {
//			UrbanOverlay overlay = new UrbanOverlay(lst, mask, latticeName, tempTable);
//			overlay.makeUHI("");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20120412 Day and Night reprocess w/ QC 1,17 and fixed longitude
//		String latticeName = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2id_lattices.shp";
//		String buffPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2_polygons_geo_buffer_5k_erased.shp";
//		String parentDir = "D:/MOD11A2";
//		
//		//processDirs(parentDir, UrbanOverlay.NIGHT, buffPolys, latticeName);
//		
//		String outTable = "D:/MOD11A2/gpt2id_lattices_LST_NIGHT_mean.csv";
//		try {
//			//combineUHI(parentDir, outTable, UrbanOverlay.NIGHT);
//			averageUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		// Daytime 20120409, 20120412
//		//processDirs(parentDir, UrbanOverlay.DAY, buffPolys, latticeName);
//		
//		outTable = "D:/MOD11A2/gpt2id_lattices_LST_DAY_mean.csv";
//		try {
//			//combineUHI(parentDir, outTable, UrbanOverlay.DAY);
//			averageUHI(parentDir, outTable, UrbanHeatOverlay.DAY);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// LA test
//		String buffPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2_LA_geo_buffer_5k_erased.shp";
//		String latticeName = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2id_LA_lattice.shp";
//		String parentDir = "D:/MOD11A2";
//		processDirs(parentDir, UrbanOverlay.NIGHT, buffPolys, latticeName);
		
//		String parentDir = "D:/MOD11A2";
//		String outTable = "D:/MOD11A2/LA_lattice_LST_NIGHT.csv";
//		try {
//			combineUHI(parentDir, outTable, UrbanOverlay.NIGHT);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// OK.  Still some gaps.  Will miss a lot w/o relaxing the QC (e.g. including 65 nad 81, dT<=2K.)

		
		// 20120425 Day and Night process with GLA14 and improved QC
//		String latticeName = "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/shapefiles/GLA14_r33_mssu_points_gpt2id.shp";
//		String buffPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2_polygons_geo_buffer_5k_erased.shp";
//		String parentDir = "D:/MOD11A2";
//		
//		processDirs(parentDir, UrbanHeatOverlay.NIGHT, buffPolys, latticeName);
//		
//		String outTable = "D:/MOD11A2/GLA14_r33_mssu_points_LST_NIGHT_mean.csv";
//		try {
//			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		// Daytime
//		processDirs(parentDir, UrbanHeatOverlay.DAY, buffPolys, latticeName);
//		
//		outTable = "D:/MOD11A2/GLA14_r33_mssu_points_LST_DAY_mean.csv";
//		try {
//			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// reprocess of GLA14, and do gpt2id_lattices w/293.73 cooling degree threshold 20120503
		String parentDir = "D:/MOD11A2";
		try {
			// re-do of GLA14
			String baseTableName = "GLA14_r33_mssu_points_gpt2id_mean_temps.csv";
			
			String outTable = "D:/MOD11A2/GLA14_r33_mssu_points_LST_NIGHT.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
			outTable = "D:/MOD11A2/GLA14_r33_mssu_points_LST_DAY.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
			// with cooling degree day > 20 degrees C
			outTable = "D:/MOD11A2/GLA14_r33_mssu_points_LST_NIGHT_ccd.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			outTable = "D:/MOD11A2/GLA14_r33_mssu_points_LST_DAY_ccd.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			
			// original w/ cooling degree day
			baseTableName = "gpt2id_lattices_mean_temps.csv";
			outTable = "D:/MOD11A2/gpt2id_lattices_LST_NIGHT_ccd.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			outTable = "D:/MOD11A2/gpt2id_lattices_LST_DAY_ccd.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
