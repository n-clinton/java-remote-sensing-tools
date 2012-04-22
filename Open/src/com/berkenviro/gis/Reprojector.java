/**
 * 
 */
package com.berkenviro.gis;

import java.io.File;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author Nicholas
 *
 */
public class Reprojector {

	/**
	 * 
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public CoordinateReferenceSystem getCRSFromFeatures(String fileName) throws Exception {
		// shapefile or other?
		File sourceFile = new File(fileName);
		FileDataStore store = FileDataStoreFinder.getDataStore(sourceFile);
		SimpleFeatureSource featureSource = store.getFeatureSource();
		SimpleFeatureType schema = featureSource.getSchema();
		return schema.getCoordinateReferenceSystem();
	}
	
	/**
	 * 
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public CoordinateReferenceSystem getCRSFromImage(String fileName) throws Exception {
		Dataset poDataset = (Dataset) gdal.Open(fileName, gdalconst.GA_ReadOnly);
		if (poDataset.GetProjectionRef() != null) {
			SpatialReference ref = new SpatialReference(poDataset.GetProjection());
			String epsg = "EPSG:"+ref.GetAuthorityName("EPSG");
			return getCRSFromEPSG(epsg);
		}
		return null;
	}
	
	/**
	 * 
	 * @param epsg is a string of the form "EPSG:4326"
	 * @return
	 * @throws Exception
	 */
	public CoordinateReferenceSystem getCRSFromEPSG(String epsg) throws Exception {
		return CRS.decode(epsg);
	}
	
	/**
	 * 
	 * @param source
	 * @param target
	 * @param geom
	 * @return
	 * @throws Exception
	 */
	public Geometry project(CoordinateReferenceSystem source, CoordinateReferenceSystem target, Geometry geom) throws Exception {
		boolean lenient = true; // allow for some error due to different datums
        MathTransform transform = CRS.findMathTransform(source, target, lenient);
        return JTS.transform(geom, transform);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		
		Reprojector pj = new Reprojector();
		try {
			
			// target of analysis:
			CoordinateReferenceSystem wgs84 = pj.getCRSFromEPSG("EPSG:4326");
			System.out.println(wgs84);
//			CoordinateReferenceSystem modisSin = pj.getCRSFromEPSG("EPSG:6965");
//			System.out.println(modisSin);
			// FAIL
			// Sinusoidal
//			String sinImage = "C:/Users/Nicholas/Documents/MOD11A2/2010.12.19/MOD11A2.A2010353.h25v02.005.2011037151457.hdf";
//			CoordinateReferenceSystem sin = pj.getCRSFromImage(sinImage);
//			System.out.println(sin);
			// FAIL
			// Sinusoidal shapefile?
//			String sinShp = "C:/Users/Nicholas/Documents/shapefiles/modis_sinusoidal/modis_sinusoidal_grid_world.shp";
//			CoordinateReferenceSystem sin2 = pj.getCRSFromFeatures(sinShp);
//			System.out.println(sin2);
			// FAIL
//			String geoShp = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/shapefiles/GLA14_r31_mssu_points.shp";
//			CoordinateReferenceSystem geo = pj.getCRSFromFeatures(sinShp);
//			System.out.println(geo);
			// FAIL
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		

	}

}
