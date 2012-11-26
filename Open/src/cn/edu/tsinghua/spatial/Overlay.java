/**
 * 
 */
package cn.edu.tsinghua.spatial;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.gdal.gdal.Dataset;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import cn.edu.tsinghua.timeseries.Loadr;
import cn.edu.tsinghua.timeseries.PERSIANNLoadr;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 *
 */
public class Overlay {

	/**
	 * 
	 * @param imageName
	 * @param ptShpFile
	 * @param outTableName
	 * @throws Exception
	 */
	public static void overlay(String imageName, String ptShpFile, String outTableName) throws Exception {
		// image
		PlanarImage image = JAIUtils.readImage(imageName);
		JAIUtils.register(image);
		RandomIter iter = RandomIterFactory.create(image, null);
		// points
		FeatureCollection<SimpleFeatureType, SimpleFeature> pts =  GISUtils.getFeatureCollection(new File(ptShpFile));
		List<AttributeDescriptor> attributes = pts.getSchema().getAttributeDescriptors();

		BufferedWriter writer = new BufferedWriter(new FileWriter(outTableName));
		// header
		String header = "";
		for (AttributeDescriptor attribute : attributes) {
			header += attribute.getName().toString()+",";
		}
		File imageFile = new File(imageName);
		header += imageFile.getName();
		writer.write(header);
		writer.newLine();
		
		// iterate over the lattice
		FeatureIterator<SimpleFeature> features = pts.features();
		System.out.println("Iterating over pts..."+pts.toString());
		while (features.hasNext()) {
			SimpleFeature feature = features.next();
			String line = "";
			
			// write the feature attributes
			for (AttributeDescriptor attribute : attributes) {
				line += feature.getAttribute(attribute.getName().toString()) +",";
			}
			
			// image value
			Geometry pt = (Point)feature.getDefaultGeometry();
			// longitude 
			double x = pt.getCoordinate().x;
			if (x > 180.0) { x = x-360.0; }
			double[] xy = {x, pt.getCoordinate().y};
			int[] dataXY = null;
			try {
				dataXY = JAIUtils.getPixelXY(xy, image);
			} catch (Exception e) {
				System.err.println("\t Can't get pixel coordinates...");
				continue;
			}
			double data = iter.getSampleDouble(dataXY[0], dataXY[1], 0);
			line += String.format("%.4f", data);
			
			System.out.println(line);
			writer.write(line);
			writer.newLine();
			
		}
		writer.close();
	}
	
	/**
	 * 
	 * @param imageName
	 * @param tableFileName
	 * @param outTableName
	 * @param xIndex is zero indexed
	 * @param yIndex is zero indexed
	 * @throws Exception
	 */
	public static void overlay(String imageName, String tableFileName, String outTableName, int xIndex, int yIndex) throws Exception {
		// image
		PlanarImage image = JAIUtils.readImage(imageName);
		JAIUtils.register(image);
		RandomIter iter = RandomIterFactory.create(image, null);
		// points
		BufferedReader reader = new BufferedReader(new FileReader(tableFileName));
		// output
		BufferedWriter writer = new BufferedWriter(new FileWriter(outTableName));
		// header
		String inLine = reader.readLine();
		File imageFile = new File(imageName);
		writer.write(inLine+","+imageFile.getName());
		writer.newLine();
		
		// iterate over the lattice
		while ((inLine = reader.readLine()) != null) {
			String line = inLine;
			
			// image value
			String[] toks = inLine.split(",");
			// longitude 
			double x = Double.parseDouble(toks[xIndex]);
			if (x > 180.0) { x = x-360.0; }
			double y = Double.parseDouble(toks[yIndex]);
			int[] dataXY = null;
			try {
				dataXY = JAIUtils.getPixelXY(new double[] {x,y}, image);
			} catch (Exception e) {
				System.err.println("\t Can't get pixel coordinates...");
				continue;
			}
			double data = iter.getSampleDouble(dataXY[0], dataXY[1], 0);
			line += ","+String.format("%.4f", data);
			
			System.out.println(line);
			writer.write(line);
			writer.newLine();
			
		}
		writer.close();
	}
	
	/**
	 * Custom method. 20121114. Overlay of urban pixels and PERSIANN.
	 * @param loadr
	 * @param ptShpFile
	 * @param outTableName
	 * @throws Exception
	 */
	public static void overlay(String imageName, Loadr loadr, String ptShpFile, String outTableName) throws Exception {
		// image
		Dataset dataset = GDALUtils.getDataset(imageName);
		// points
		FeatureCollection<SimpleFeatureType, SimpleFeature> pts =  GISUtils.getFeatureCollection(new File(ptShpFile));
		List<AttributeDescriptor> attributes = pts.getSchema().getAttributeDescriptors();

		BufferedWriter writer = new BufferedWriter(new FileWriter(outTableName));
		// header
		String header = "";
		for (AttributeDescriptor attribute : attributes) {
			header += attribute.getName().toString()+",";
		}
		header += "x,y,";
		header += "area,";
		File imageFile = new File(imageName);
		header += imageFile.getName()+",";
		header += "rain";
		writer.write(header);
		writer.newLine();
		
		// iterate over the lattice
		FeatureIterator<SimpleFeature> features = pts.features();
		System.out.println("Iterating over pts..."+pts.toString());
		while (features.hasNext()) {
			SimpleFeature feature = features.next();
			String line = "";
			
			// write the feature attributes
			for (AttributeDescriptor attribute : attributes) {
				line += feature.getAttribute(attribute.getName().toString()) +",";
			}
			
			// image value
			Geometry pt = (Point)feature.getDefaultGeometry();
			if (Math.abs(pt.getCoordinate().y) > 60 ) {continue;} // outside PERSIANN range
			//System.out.println(pt);
			// longitude 
			double x = pt.getCoordinate().x;
			if (x > 180.0) { x = x-360.0; }
			// converted to [-180,180]
			double[] xy = {x, pt.getCoordinate().y};
			line+=pt.getCoordinate().x+","+pt.getCoordinate().y+",";
			
			// pixel area
			double delta = 0.0083333333;
			double area = Math.cos(Math.abs(pt.getCoordinate().y*Math.PI/180.0))*Math.abs(delta*delta)*60.0*60.0*1.852*1.852;
			line += area+",";
			// sample the image
			try {
				double data = GDALUtils.imageValue(dataset, GISUtils.makePoint(xy[0], xy[1]), 1);
				line += String.format("%.4f", data);
			} catch (Exception e) {
				e.printStackTrace();
			}
			line += ",";
			// sample the rainfall series
			try {
				double rain = 0;
				List<double[]> rains = loadr.getSeries((Point)pt);
				for (double[] d : rains) {
					if (d[1] > 6.0) {
						rain+=d[1];
					}
				}
				line += rain;	
			} catch (Exception e) {
				e.printStackTrace();
			}
			// sample the 
			System.out.println(line);
			writer.write(line);
			writer.newLine();
			
		}
		writer.close();
	}
	
	/**
	 * Custom method. 20121120.  Overlay of urban pixels and WorldClim mean temperature.
	 * @param loadr
	 * @param ptShpFile
	 * @param outTableName
	 * @throws Exception
	 */
	public static void overlay(String imageName, String dir, String ptShpFile, String outTableName) throws Exception {
		// directory 
		File directory = new File(dir);
		if (!directory.isDirectory()) {
			throw new Exception("Jackass! Not a directory: "+directory.getAbsolutePath());
		}
		// image
		Dataset dataset = GDALUtils.getDataset(imageName);
		// points
		FeatureCollection<SimpleFeatureType, SimpleFeature> pts =  GISUtils.getFeatureCollection(new File(ptShpFile));
		List<AttributeDescriptor> attributes = pts.getSchema().getAttributeDescriptors();

		BufferedWriter writer = new BufferedWriter(new FileWriter(outTableName));
		// header
		String header = "";
		for (AttributeDescriptor attribute : attributes) {
			header += attribute.getName().toString()+",";
		}
		header += "x,y,";
		header += "area,";
		File imageFile = new File(imageName);
		header += imageFile.getName();
		for (int m=1; m<=12; m++) {
			header += ",temp_"+m;
		}
		
		writer.write(header);
		writer.newLine();
		
		// iterate over the lattice
		FeatureIterator<SimpleFeature> features = pts.features();
		System.out.println("Iterating over pts..."+pts.toString());
		while (features.hasNext()) {
			SimpleFeature feature = features.next();
			String line = "";
			
			// write the feature attributes
			for (AttributeDescriptor attribute : attributes) {
				line += feature.getAttribute(attribute.getName().toString()) +",";
			}
			
			// image value
			Geometry pt = (Point)feature.getDefaultGeometry();
			if (Math.abs(pt.getCoordinate().y) > 60 ) {continue;} // outside PERSIANN range
			//System.out.println(pt);
			// longitude 
			double x = pt.getCoordinate().x;
			if (x > 180.0) { x = x-360.0; }
			// converted to [-180,180]
			double[] xy = {x, pt.getCoordinate().y};
			line+=pt.getCoordinate().x+","+pt.getCoordinate().y+",";
			
			// pixel area
			double delta = 0.0083333333;
			double area = Math.cos(Math.abs(pt.getCoordinate().y*Math.PI/180.0))*Math.abs(delta*delta)*60.0*60.0*1.852*1.852;
			line += area+",";
			// sample the image
			try {
				double data = GDALUtils.imageValue(dataset, GISUtils.makePoint(xy[0], xy[1]), 1);
				line += String.format("%.4f", data);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// sample the temperatures
			double[] temps = new double[12];
			for (File f : directory.listFiles()) {
				if (!f.getName().endsWith(".tif")) { continue; }
				int index = Integer.parseInt(f.getName().substring(f.getName().length()-6, f.getName().length()-4))-1;
				Dataset d = GDALUtils.getDataset(f.getAbsolutePath());
				temps[index] = GDALUtils.imageValue(d, GISUtils.makePoint(xy[0], xy[1]), 1);
			}
			for (int m=0; m<temps.length; m++) {
				if (temps[m] == -32768.0) { // no data
					line += ",";
				}
				else if (temps[m]/10 < 18) { // heating degree day
					line += ","+String.format("%.4f", (temps[m]/10 - 18.0)); // negative gradient
				}
				else if (temps[m]/10 > 20) { // cooling degree day
					line += ","+String.format("%.4f", (temps[m]/10 - 20.0)); // positive gradient 
				}
				else { // neutral, no heating or cooling
					line += ",";
				}
			}
			
			System.out.println(line);
			writer.write(line);
			writer.newLine();
			
		}
		writer.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Overlay of gpt2lattice and population
//		String lattice = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2id_lattices_joined2.shp";
//		String image = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/gpt2pop_.tif";
//		String outTableName = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/gpt2id_lattices_joined2_pop.csv";
//		try {
//			overlay(image, lattice, outTableName);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// overlay of nightlights
//		String table = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/gpt2id_lattices_joined2_pop.csv";
//		String image = "C:/Users/Nicholas/Documents/Night_Lights/F182010.v4c_web.stable_lights.tif";
//		String outTableName = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/gpt2id_lattices_joined2_pop_nightlights.csv";
//		int xIndex = 10;
//		int yIndex = 11;
//		try {
//			overlay(image, table, outTableName, xIndex, yIndex);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// GLA14 already has population in it
//		String lattice = "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/shapefiles/GLA14_r33_mssu_points_gpt2id_joined.shp";
//		String image = "C:/Users/Nicholas/Documents/Night_Lights/F182010.v4c_web.stable_lights.tif";
//		String outTableName = "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/GLA14_r33_mssu_points_gpt2id_joined_nightlights.csv";
//		try {
//			overlay(image, lattice, outTableName);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20121114
//		try {
//			String pop = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/gpt2pop_.tif";
//			String lattice = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2id_lattices.shp";
//			String out = "C:/Users/Nicholas/Documents/urban/agriculture/gpt2id_lattice_pop_rainfall6_area.csv";
//			String[] directories = {"C:/Users/Public/Documents/PERSIANN/8km_daily/2010"};
//			PERSIANNLoadr loadr = new PERSIANNLoadr(directories);
//			overlay(pop, loadr, lattice, out);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		try {
//			String pop = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/gpt2pop_.tif";
//			String lattice = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2id_lattices.shp";
//			String out = "C:/Users/Nicholas/Documents/urban/agriculture/gpt2id_lattice_pop_temp_area.csv";
//			String directory = "C:/Users/Nicholas/Documents/WorldClim/tmean";
//			overlay(pop, directory, lattice, out);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

}
