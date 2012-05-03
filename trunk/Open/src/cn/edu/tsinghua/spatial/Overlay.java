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

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.berkenviro.gis.GISUtils;
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
	 * @param args
	 */
	public static void main(String[] args) {
		// Overlay of gpt2lattice
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
		
		
		
		String lattice = "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/shapefiles/GLA14_r33_mssu_points_gpt2id_joined.shp";
		String image = "C:/Users/Nicholas/Documents/Night_Lights/F182010.v4c_web.stable_lights.tif";
		String outTableName = "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/GLA14_r33_mssu_points_gpt2id_joined_nightlights.csv";
		try {
			overlay(image, lattice, outTableName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
