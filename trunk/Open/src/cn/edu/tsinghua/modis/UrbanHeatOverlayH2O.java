/**
 * 
 */
package cn.edu.tsinghua.modis;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.gdal.gdal.Dataset;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import cn.edu.tsinghua.lidar.BitChecker;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 *
 */
public class UrbanHeatOverlayH2O extends UrbanHeatOverlay {

	Dataset water;
	
	/**
	 * @param dataName
	 * @param qcName
	 * @param polygonName
	 * @throws Exception
	 */
	public UrbanHeatOverlayH2O(String dataName, String qcName, String polygonName, String waterName) throws Exception {
		super(dataName, qcName, polygonName);
		water = GDALUtils.getDataset(waterName);
	}

	/**
	 * @param dataName
	 * @param qcName
	 * @param latticeName
	 * @param tableName
	 */
	public UrbanHeatOverlayH2O(String dataName, String qcName, String latticeName, String tableName,  String waterName) {
		super(dataName, qcName, latticeName, tableName);
		water = GDALUtils.getDataset(waterName);
	}

	
	/**
	 * Overriden for water check.  Compute composite urban heat island.
	 * @param outTable
	 * @throws Exception
	 */
	@Override
	public void makeUHI(String outTable) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outTable));
		writer.write("POINTID,pttemp,bufftemp");
		writer.newLine();
		
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
				dataXY = GDALUtils.getPixelXY(xy, data);
			} catch (Exception e) {
				//System.err.println("\t Can't get pixel coordinates...");
				continue;
			}
			int qcVal = (int)GDALUtils.pixelValue(qc, dataXY[0], dataXY[1], 1);
			if (!BitChecker.mod11ok(qcVal)) { // if LST error >1K or other problem
				//System.out.println("\t QC: "+qc);
				continue;
			}
			double temp = GDALUtils.pixelValue(data, dataXY[0], dataXY[1], 1)*0.02;
			if (temp == 0 || temp < 183.95 || temp > 343.55) {
				//System.out.println("\t Temp: "+temp);
				continue;
			}
			
			// water check 20120510
			int[] waterXY = GDALUtils.getPixelXY(xy, water); // should always be in bounds
			double h2o = GDALUtils.pixelValue(water, waterXY[0], waterXY[1], 1);
			if (h2o == 0) {
				//System.out.println("\t Water: "+feature);
				continue;
			}

			// the following doesn't compile with Java 6, Java 7 is OK
			int id = (int)(long)feature.getAttribute("GRID_CODE");
			// w/GLA14, really is an Integer?
			//int id = (int)feature.getAttribute("GRID_CODE");
			int pointID = (int)feature.getAttribute("POINTID");
			if (temps[id-1] == 0 || temps[id-1] == Double.NaN) {
				//System.out.println("\t Buffer: "+temps[id-1]);
				continue;
			}
			
			//System.out.println("ID: "+id+" Pixel temp: "+temp+" Suburb temp: "+temps[id-1]);
			writer.write(pointID+","+temp+","+temps[id-1]);
			writer.newLine();
		}
		iter.close();
		writer.close();
	}
	
	
	/**
	 * 
	 * @param p
	 * @return
	 * @throws Exception
	 */
	@Override
	public SummaryStatistics polygonStatsMasked(final Geometry p) throws Exception {
		final SummaryStatistics stats = new SummaryStatistics();
		
		// bounding box
		Envelope bb = p.getEnvelopeInternal();
		// these will throw Exception if outside image bounds, don't process this polygon
		int[] ul = GDALUtils.getPixelXY(new double[] {bb.getMinX(), bb.getMaxY()}, data);
		int[] lr = GDALUtils.getPixelXY(new double[] {bb.getMaxX(), bb.getMinY()}, data);
		int minX = Math.max(ul[0]-1, 0);
		int minY = Math.max(ul[1]-1, 0);
		int maxX = Math.min(lr[0]+1, data.getRasterXSize()-1);
		int maxY = Math.min(lr[1]+1, data.getRasterYSize()-1);

		for (int x=minX; x<=maxX; x++) {
			for (int y=minY; y<=maxY; y++) {
				// pixel centroid in projected coords
				double[] coords = GDALUtils.getProjectedXY(new int[] {x, y}, data);
				// projected point
				Point check = GISUtils.makePoint(coords[0], coords[1]);
				// if the pixel centroid is in the polygon, count it
				if (p.intersects(check)) {
					int qcVal = (int)GDALUtils.pixelValue(qc, x, y, 1);
					if (BitChecker.mod11ok(qcVal)) {
						double temp = GDALUtils.pixelValue(data, x, y, 1)*0.02;
						if (temp > 183.95 && temp < 343.55) { // min and max surface temperatures
							// water check 20120510
							double h2o = GDALUtils.imageValue(water, check, 1);
							if (h2o != 0) {
								stats.addValue(temp);
							}
						}
					}
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
	 * @param waterName
	 * @throws Exception
	 */
	public static void processDir(String dir, String product, String polygonName, String latticeName, String waterName) throws Exception  {
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
		
		UrbanHeatOverlayH2O overlay;
		
		String outMeans = lstDir+"/"+(new File(polygonName)).getName().replace(".shp", "_mean_temps_h2o.csv");
		// check for existence, continue if already there
		if (!(new File(outMeans)).exists()) {
			System.out.println("\t writing  "+outMeans);
			overlay = new UrbanHeatOverlayH2O(lstName, qcName, polygonName, waterName);
			overlay.polygonStatsTable(outMeans);
			System.out.println("\t"+Calendar.getInstance().getTime());
		}
		
		String outComp = lstDir+"/"+(new File(latticeName)).getName().replace(".shp", "_mean_temps_h2o.csv");
		if (!(new File(outComp)).exists()) { // if it does NOT already exist, make it
			System.out.println("\t writing  "+outComp);
			overlay = new UrbanHeatOverlayH2O(lstName, qcName, latticeName, outMeans, waterName);
			overlay.makeUHI(outComp);
			System.out.println(Calendar.getInstance().getTime());
		}
		/*
		 *  20121130 added the else to account for second polygon processing, AQUA.
		 *  Should have named this table after both lattice and polygons input files.
		 */
		else { // if it DOES exist, make a new filename
			if (outMeans.contains("_10k_")) {
				outComp = lstDir+"/"+(new File(latticeName)).getName().replace(".shp", "_mean_temps_h2o_big.csv");
				System.out.println("\t writing  "+outComp);
				overlay = new UrbanHeatOverlayH2O(lstName, qcName, latticeName, outMeans, waterName);
				overlay.makeUHI(outComp);
				System.out.println(Calendar.getInstance().getTime());
			}
		}
	}
	
	
	/**
	 * 
	 * @param parent
	 * @param product
	 * @param polygonName
	 * @param latticeName
	 * @param waterName
	 */
	public static void processDirs(String parent, String product, String polygonName, String latticeName, String waterName) {
		File parentDir = new File(parent);
		File[] dirs = parentDir.listFiles();
		for (File dir : dirs) {
			if (!(dir.isDirectory())) { continue; }
			try {
				processDir(dir.getAbsolutePath(), product, polygonName, latticeName, waterName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 20120511 Day and Night process with MSSU, waterbodies, improved QC
//		String latticeName = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2id_lattices.shp";
//		String buffPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2_polygons_geo_buffer_5k_erased.shp";
//		String waterName = "C:/Users/Nicholas/Documents/GlobalLandCover/modis/2009_igbp_wgs84.tif";
//		String parentDir = "D:/MOD11A2";
//		// Night time
//		processDirs(parentDir, UrbanHeatOverlay.NIGHT, buffPolys, latticeName, waterName);
//		// Day time
//		processDirs(parentDir, UrbanHeatOverlay.DAY, buffPolys, latticeName, waterName);
		
		// reprocess w/ water body mask, 20120513
//		String parentDir = "D:/MOD11A2";
//		try {
//			// re-do of GLA14
//			String baseTableName = "gpt2id_latticesmean_temps_h2o.csv";
//			
//			String outTable = "D:/MOD11A2/gpt2id_lattices_LST_NIGHT_h2o.csv";
//			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
//			outTable = "D:/MOD11A2/gpt2id_lattices_LST_DAY_h2o.csv";
//			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
//			// with cooling degree day > 20 degrees C
//			outTable = "D:/MOD11A2/gpt2id_lattices_LST_NIGHT_ccd_h2o.csv";
//			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
//			outTable = "D:/MOD11A2/gpt2id_lattices_LST_DAY_ccd_h2o.csv";
//			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// 20120513 Day and Night process with GLA14, waterbodies, improved QC
//		String latticeName = "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/shapefiles/GLA14_r33_mssu_points_gpt2id.shp";
//		String buffPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2_polygons_geo_buffer_5k_erased.shp";
//		String waterName = "C:/Users/Nicholas/Documents/GlobalLandCover/modis/2009_igbp_wgs84.tif";
//		String parentDir = "D:/MOD11A2";
//		// Night time
//		processDirs(parentDir, UrbanHeatOverlay.NIGHT, buffPolys, latticeName, waterName);
//		// Day time
//		processDirs(parentDir, UrbanHeatOverlay.DAY, buffPolys, latticeName, waterName);
		
		// reprocess GLA14 w/ water body mask, 20120514
//		String parentDir = "D:/MOD11A2";
//		try {
//			// re-do of GLA14
//			String baseTableName = "GLA14_r33_mssu_points_gpt2id_mean_temps_h2o.csv";
//			
//			String outTable = "D:/MOD11A2/GLA14_r33_mssu_points_gpt2id_LST_NIGHT_h2o.csv";
//			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
//			outTable = "D:/MOD11A2/GLA14_r33_mssu_points_gpt2id_LST_DAY_h2o.csv";
//			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
//			// with cooling degree day > 20 degrees C
//			outTable = "D:/MOD11A2/GLA14_r33_mssu_points_gpt2id_LST_NIGHT_ccd_h2o.csv";
//			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
//			outTable = "D:/MOD11A2/GLA14_r33_mssu_points_gpt2id_LST_DAY_ccd_h2o.csv";
//			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		

		
		// 20120618 count and average UH
//		String parentDir = "D:/MOD11A2";
//		try {
			// original w/ cooling degree day
//			String baseTableName = "gpt2id_latticesmean_temps_h2o.csv";
//			String outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_NIGHT_mean_cdd.csv";
//			averageUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
//			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_NIGHT_count_cdd.csv";
//			countUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
//			
//			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_DAY_mean_ccd.csv";
//			averageUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
//			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_DAY_count_ccd.csv";
//			countUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
//			
//			String baseTableName = "gpt2id_latticesmean_temps_h2o.csv";
//			String outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_NIGHT_mean.csv";
//			averageUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
//			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_NIGHT_count.csv";
//			countUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
//			
//			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_DAY_mean.csv";
//			averageUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
//			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_DAY_count.csv";
//			countUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
				
			
		// 20121129 post-review re-do.  Small polygons, AQUA
//		String latticeName = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2id_lattices.shp";
//		String buffPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2_polygons_geo_buffer_5k_erased.shp";
//		String waterName = "C:/Users/Nicholas/Documents/GlobalLandCover/modis/2009_igbp_wgs84.tif";
//		String parentDir = "D:/MYD11A2/2010";  // AQUA
//		// Night time
//		//processDirs(parentDir, UrbanHeatOverlay.NIGHT, buffPolys, latticeName, waterName);
//		// Day time
//		processDirs(parentDir, UrbanHeatOverlay.DAY, buffPolys, latticeName, waterName);
//		
//		// 20121129 post-review re-do.  BIG polygons, AQUA 
//		buffPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2_polygons_geo_buffer_10k_erased.shp";
//		// Night time
//		processDirs(parentDir, UrbanHeatOverlay.NIGHT, buffPolys, latticeName, waterName);
//		// Day time
//		processDirs(parentDir, UrbanHeatOverlay.DAY, buffPolys, latticeName, waterName);
//		
//		// 20121130 BIG polygons, TERRA
//		parentDir = "D:/MOD11A2/2010";  // TERRA
//		// Night time
//		processDirs(parentDir, UrbanHeatOverlay.NIGHT, buffPolys, latticeName, waterName);
//		// Day time
//		processDirs(parentDir, UrbanHeatOverlay.DAY, buffPolys, latticeName, waterName);
		
		
		
		// 20121220
		try {
			// TERRA with big polygons
			String parentDir = "D:/MOD11A2/2010";
			// cooling degree day
			// NIGHT
			//String baseTableName = "gpt2id_latticesmean_temps_h2o.csv";  // Old name, small polygons
			String baseTableName = "gpt2id_lattices_mean_temps_h2o.csv";  // New name, BIG polygons
			
			String outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_NIGHT_mean_cdd_big.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_NIGHT_count_cdd_big.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			outTable = "D:/MOD11A2/gpt2id_lattices_LST_NIGHT_ccd_big.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			// DAY
			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_DAY_mean_ccd_big.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_DAY_count_ccd_big.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			outTable = "D:/MOD11A2/gpt2id_lattices_LST_DAY_ccd_big.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			
			// not cooling degree day
			// NIGHT
			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_NIGHT_mean_big.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_NIGHT_count_big.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
			outTable = "D:/MOD11A2/gpt2id_lattices_LST_NIGHT_big.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
			// DAY
			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_DAY_mean_big.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
			outTable = "D:/MOD11A2/gpt2id_lattices_h2o_LST_DAY_count_big.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
			outTable = "D:/MOD11A2/gpt2id_lattices_LST_DAY_big.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
			
			/*//////////////////////////////////////////////////////////////////////////
			 * AQUA
			 */////////////////////////////////////////////////////////////////////////
			parentDir = "D:/MYD11A2/2010";
			baseTableName = "gpt2id_lattices_mean_temps_h2o.csv";  // small polygons
			// cooling degree day
			// NIGHT 
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_NIGHT_mean_cdd.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_NIGHT_count_cdd.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			outTable = "D:/MYD11A2/gpt2id_lattices_LST_NIGHT_ccd.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			// DAY
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_DAY_mean_ccd.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_DAY_count_ccd.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			outTable = "D:/MYD11A2/gpt2id_lattices_LST_DAY_ccd.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			
			// not cooling degree day
			// NIGHT
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_NIGHT_mean.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_NIGHT_count.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
			outTable = "D:/MYD11A2/gpt2id_lattices_LST_NIGHT.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
			// DAY
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_DAY_mean.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_DAY_count.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
			outTable = "D:/MYD11A2/gpt2id_lattices_LST_DAY.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
			
			//////////////////////////////////////////////////////////////////////////
			baseTableName = "gpt2id_lattices_mean_temps_h2o_big.csv";  // BIG polygons
			// cooling degree day
			// NIGHT 
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_NIGHT_mean_cdd_big.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_NIGHT_count_cdd_big.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			outTable = "D:/MYD11A2/gpt2id_lattices_LST_NIGHT_ccd_big.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 293.15);
			// DAY
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_DAY_mean_ccd_big.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_DAY_count_ccd_big.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			outTable = "D:/MYD11A2/gpt2id_lattices_LST_DAY_ccd_big.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 293.15);
			
			// not cooling degree day
			// NIGHT
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_NIGHT_mean_big.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_NIGHT_count_big.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
			outTable = "D:/MYD11A2/gpt2id_lattices_LST_NIGHT_big.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.NIGHT, baseTableName, 0);
			// DAY
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_DAY_mean_big.csv";
			averageUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
			outTable = "D:/MYD11A2/gpt2id_lattices_h2o_LST_DAY_count_big.csv";
			countUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);
			outTable = "D:/MYD11A2/gpt2id_lattices_LST_DAY_big.csv";
			combineUHI(parentDir, outTable, UrbanHeatOverlay.DAY, baseTableName, 0);	
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
