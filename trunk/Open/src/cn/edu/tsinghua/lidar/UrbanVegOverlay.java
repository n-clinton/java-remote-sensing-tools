/**
 * 
 */
package cn.edu.tsinghua.lidar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 *
 */
public class UrbanVegOverlay {

	public static String EVI = "EVI";
	public static String NDVI = "NDVI";
	
	PlanarImage data;
	PlanarImage qc;
	FeatureCollection<SimpleFeatureType, SimpleFeature> id;
	
	/**
	 * @param dataName
	 * @param qcName
	 * @param latticeName
	 * @param tableName
	 */
	public UrbanVegOverlay(String dataName, String qcName, String latticeName) {
		data = JAIUtils.readImage(dataName);
		JAIUtils.register(data);
		qc = JAIUtils.readImage(qcName);
		JAIUtils.register(qc);
		id =  GISUtils.getFeatureCollection(new File(latticeName));
	}

	/**
	 * 
	 * @param outTable
	 * @throws Exception
	 */
	public void makeVI(String outTable) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outTable));
		writer.write("POINTID,vi");
		writer.newLine();
		
		RandomIter dataIter = RandomIterFactory.create(data, null);
		RandomIter qcIter = RandomIterFactory.create(qc, null);
		
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
			
			if (!BitChecker.mod13ok(qc)) {
				//System.out.println("\t QC: "+qc);
				continue;
			}
			
			// https://lpdaac.usgs.gov/content/view/full/6648
			double vi = dataIter.getSampleDouble(dataXY[0], dataXY[1], 0)*0.0001;
			if (vi < -0.2 || vi > 1.0) {
				continue;
			}

			// the following doesn't compile with Java 6, Java 7 is OK
			int pointID = (int)feature.getAttribute("POINTID");

			writer.write(pointID+","+vi);
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
	public static void combineVI(String parentDir, String outTable, String product, String baseTableName) throws Exception {
		File[] dirs = (new File(parentDir)).listFiles();
		
		float[] vis = new float[523238]; // vi
		int[] dates = new int[523238]; // dates corresponding to vi
		
		for (File dir : dirs) {
			if (!(dir.isDirectory())) { continue; }
			System.out.println("Processing dir: " +dir.getName());
			File latticeTab = new File(dir.getAbsolutePath()+"/"+product+"/"+baseTableName);
			BufferedReader reader = new BufferedReader(new FileReader(latticeTab));
			reader.readLine(); // header
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] toks = line.split(",");
				int id = Integer.parseInt(toks[0]);
				float vi = Float.parseFloat(toks[1]);
				
				// compute day of year
				String[] date = dir.getName().split("\\.");
				Calendar calendar = Calendar.getInstance();
				calendar.set(Integer.parseInt(date[0]), // year
							 Integer.parseInt(date[1])-1, // month
						     Integer.parseInt(date[2])); // day
				int doy = calendar.get(Calendar.DAY_OF_YEAR);
				if (vi > -0.2) { // valid point
					if (vi > vis[id-1]) {
						// it's hotter than the buffer
						vis[id-1] = vi;
						dates[id-1] = doy;
						System.out.println("\t Found new vi max (id, uhi, doy): "+(id-1)+", "+vi+", "+doy);
					}
				}
				
			}
			reader.close();
		}
		// Arrays should be full, write out

		BufferedWriter writer = new BufferedWriter(new FileWriter(outTable));
		writer.write("id,"+product+"max,maxdate");
		writer.newLine();
		
		for (int i=0; i<vis.length; i++) {
			writer.write((i+1)+","+vis[i]+","+dates[i]);
			writer.newLine();
		}
		writer.close();
	}
	
	/**
	 * 
	 * @param dir
	 * @param product
	 * @param latticeName
	 * @throws Exception
	 */
	public static void processDir(String dir, String product, String latticeName) throws Exception  {
		System.out.println(Calendar.getInstance().getTime());
		System.out.println("Processing directory "+dir);
		File viDir = new File(dir+"/"+product);
		File qcDir = new File(dir+"/VI_QC");
		File[] viList = viDir.listFiles();
		String lstName = null;
		for (File f : viList) {
			if (f.getName().endsWith(".tif")) {
				lstName = f.getAbsolutePath();
			}
		}
		System.out.println("\t Found VI image  "+lstName);
		File[] qcList = qcDir.listFiles();
		String qcName = null;
		for (File f : qcList) {
			if (f.getName().endsWith(".tif")) {
				qcName = f.getAbsolutePath();
			}
		}
		System.out.println("\t Found QC image  "+qcName);
		
		UrbanVegOverlay overlay;
	
		String outVItab = viDir+"/"+(new File(latticeName)).getName().replace(".shp", "_"+product+".csv");
		// check for existence, delete if already present
		if (!(new File(outVItab)).exists()) {
			System.out.println("\t writing  "+outVItab);
			overlay = new UrbanVegOverlay(lstName, qcName, latticeName);
			overlay.makeVI(outVItab);
			System.out.println(Calendar.getInstance().getTime());
		}
	}
	
	/**
	 * 
	 * @param parent
	 * @param product
	 * @param latticeName
	 */
	public static void processDirs(String parent, String product, String latticeName) {
		File parentDir = new File(parent);
		File[] dirs = parentDir.listFiles();
		for (File dir : dirs) {
			if (!(dir.isDirectory())) { continue; }
			try {
				processDir(dir.getAbsolutePath(), product, latticeName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 20120412 Day and Night reprocess w/ QC 1,17 and fixed longitude
//		String latticeName = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2id_lattices.shp";
		String parentDir = "D:/MOD13A2";
		
		//processDirs(parentDir, UrbanVegOverlay.EVI, latticeName);
		
//		String outTable = "D:/MOD13A2/gpt2id_lattices_EVI_max.csv";
//		try {
//			combineVI(parentDir, outTable, UrbanVegOverlay.EVI, "gpt2id_lattices_EVI.csv");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// GLA14
//		String latticeName = "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/shapefiles/GLA14_r33_mssu_points_gpt2id.shp";
//		String parentDir = "D:/MOD13A2";
//		processDirs(parentDir, UrbanVegOverlay.EVI, latticeName);
//		
		String outTable = "D:/MOD13A2/GLA14_r33_mssu_points_EVI_max.csv";
		try {
			combineVI(parentDir, outTable, UrbanVegOverlay.EVI, "GLA14_r33_mssu_points_gpt2id_EVI.csv");
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}

}
