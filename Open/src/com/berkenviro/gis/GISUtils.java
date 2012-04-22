/*
 *  Copyright (C) 2011  Nicholas Clinton
 *	All rights reserved.  
 *
 *	Redistribution and use in source and binary forms, with or without modification, 
 *	are permitted provided that the following conditions are met:
 *
 *	1. Redistributions of source code must retain the above copyright notice, 
 *	this list of conditions and the following disclaimer.  
 *	2. Redistributions in binary form must reproduce the above copyright notice, 
 *	this list of conditions and the following disclaimer in the documentation 
 *	and/or other materials provided with the distribution. 
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 *	AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 *	THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 *	PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
 *	BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 *	DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 *	THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN 
 *	IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.berkenviro.gis;


import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;

import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.geom.util.NoninvertibleTransformationException;

/**
 * @author Nicholas Clinton
 * Contains miscellaneous utilities for data extraction, overlay, 
 * georegistration, etc.  Bundled here as static convenience methods.
 */
public class GISUtils {
	
	/**
	 * Return ESRI world Mollweide according to Spatial-reference.org
	 * @return
	 * @throws FactoryException
	 */
	public static CoordinateReferenceSystem getMollweide() throws FactoryException {
		String wkt = "PROJCS[\"World_Mollweide\"," +
						"GEOGCS[\"GCS_WGS_1984\",DATUM[\"WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]]," +
						"PROJECTION[\"Mollweide\"],PARAMETER[\"False_Easting\",0],PARAMETER[\"False_Northing\",0],PARAMETER[\"Central_Meridian\",0],UNIT[\"Meter\",1],AUTHORITY[\"EPSG\",\"54009\"]]";
		return CRS.parseWKT(wkt);
	}
	
	/**
	 * 
	 * @param geoFeature
	 * @return
	 */
	public static Geometry geoBuffer(Geometry geoFeature, int meters) throws Exception {
		CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
		//CoordinateReferenceSystem wgs84 = DefaultGeographicCRS.WGS84 // less complete
		CoordinateReferenceSystem moll = getMollweide();
		MathTransform transform = CRS.findMathTransform(wgs84, moll, true);
		// move to Mollweide
		Geometry mollGeom = JTS.transform(geoFeature, transform);
		// buffer with meters
		Geometry buff = mollGeom.buffer(meters);
		// transform back to geographic
		transform = CRS.findMathTransform(moll, wgs84, true);
		return JTS.transform(buff, transform);
	}
	
	/**
	 * Tested OK for geographic 20120402.
	 * @param fc
	 * @param outName
	 * @param image
	 * @param band
	 * @param mask
	 * @param maskValue
	 */
	public static void polygonStatsTable(FeatureCollection<SimpleFeatureType, SimpleFeature> collection, 
										 String outName, 
										 PlanarImage image, 
										 int band) throws Exception {
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outName));
		writer.write("id,avg"+"\n");
		
		FeatureIterator<SimpleFeature> iter=collection.features();
		while (iter.hasNext()) {
			String line = "";
			SimpleFeature feature = iter.next();
			// Id = FID ?= feature.getID()-1
			line+=feature.getAttribute("Id");
			Geometry p = (Geometry) feature.getDefaultGeometry();
			SummaryStatistics stats = polygonStats(p, image, band);
			line+=","+stats.getMean();
			//System.out.println(line);
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
	 * @return
	 * @throws Exception
	 */
	public static SummaryStatistics polygonStats(Geometry p, PlanarImage image, int band) throws Exception {
		SummaryStatistics stats = new SummaryStatistics();
		
		// bounding box
		Envelope bb = p.getEnvelopeInternal();
		// these will throw Exception if outside image bounds
		int[] ul = JAIUtils.getPixelXY(new double[] {bb.getMinX(), bb.getMaxY()}, image);
		int[] lr = JAIUtils.getPixelXY(new double[] {bb.getMaxX(), bb.getMinY()}, image);
		int minX = Math.max(ul[0]-1, 0);
		int minY = Math.max(ul[1]-1, 0);
		int maxX = Math.min(lr[0]+1, image.getWidth()-1);
		int maxY = Math.min(lr[1]+1, image.getWidth()-1);
		Rectangle bounds = new Rectangle(minX, minY, maxX-minX+1, maxY-minY+1);
		RandomIter iter = RandomIterFactory.create(image, bounds);
		GeometryFactory ptMakr = new GeometryFactory();
		for (int x=minX; x<=maxX; x++) {
			for (int y=minY; y<=maxY; y++) {
				// pixel centroid in projected coords
				double[] coords = JAIUtils.getProjectedXY(new int[] {x, y}, image);
				Point check = ptMakr.createPoint(new Coordinate(coords[0], coords[1]));
				//System.out.println("\t "+check);
				// if the pixel centroid is in the polygon, count it
				if (p.intersects(check)) {
					stats.addValue(iter.getSampleDouble(x, y, band));
				}
			}
		}
		return stats;
	}
	
	
	/*
	 * Method to define an AffineTransform from the georeferencing
	 * parameters found in a world file or GeoTiff header.
	 */
	public static AffineTransformation raster2proj(PlanarImage pi) {
		// check to make sure the tiff is registered
		if (!JAIUtils.isRegistered(pi)) { JAIUtils.register(pi); }
		// these are the AffineTransformation parameters
		double m00, m01, m02, m10, m11, m12;
		m02 = ((Double) pi.getProperty("ulX")).doubleValue();
		m12 = ((Double) pi.getProperty("ulY")).doubleValue();
		m00 = ((Double) pi.getProperty("deltaX")).doubleValue();
		m11 = ((Double) pi.getProperty("deltaY")).doubleValue();
		m01 = 0.0;
		m10 = 0.0;
		return new AffineTransformation(m00, m01, m02, m10, m11, m12);
	}
	
	/*
	 * Method to invert an AffineTransform.
	 */
	public static AffineTransformation proj2raster(AffineTransformation raster2projTrans) {
		AffineTransformation inv = null;
		try {
			inv = raster2projTrans.getInverse();
		} catch (NoninvertibleTransformationException e) {
			e.printStackTrace();
		}
		return inv;
	}
	
	/*
	 * Method to convert an AffineTransformation to an AffineTransform
	 * that can be used on a java.AWT.Shape
	 */
	public static AffineTransform transformTranformer(AffineTransformation at) {
		
		double[] tArray = at.getMatrixEntries();
		return new AffineTransform(tArray[0],
								   tArray[3],
								   tArray[1],
								   tArray[4],
							       tArray[2],
							       tArray[5]);

	}

	
	/*
	 * Helper that returns a Point Feature Type that's ready for action.
	 */
	public static SimpleFeatureType makeBasicPointType(String name) {
		
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(name);
        //builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

        // add attributes in order
        //builder.length(15).add("Name", String.class); // <- 15 chars width for name field
        builder.add("the_geom", Point.class);
        builder.add("id_", Integer.class);
        builder.add("x", Float.class);
        builder.add("y", Float.class);

        return builder.buildFeatureType();
	}
	
	/*
	 * Make a point from the supplied coords
	 */
	public static Point makePoint(double x, double y) {
		GeometryFactory gf = new GeometryFactory();
		return gf.createPoint(new Coordinate(x, y));
	}
	
	
	/*
	 * Helper that returns a Point Feature that's ready for action.  Uses fields as set by
	 * makeBasicPointType()
	 */
	public static SimpleFeature makeBasicPointFeature(Point pt, int id, String name) {
		SimpleFeatureType type = makeBasicPointType(name);
		
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
		
		featureBuilder.set("the_geom", pt);
		featureBuilder.set("id_", id);
		featureBuilder.set("x", pt.getX());
		featureBuilder.set("y", pt.getY());
        
        SimpleFeature feature = featureBuilder.buildFeature(null);
		return feature;
	}
	
	/*
	 * Simple implementation that returns a Polygon Feature Type that's ready for action.
	 */
	public static SimpleFeatureType makeBasicPolyType(String name) {
		
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(name);
        //builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

        // add attributes in order
        //builder.length(15).add("Name", String.class); // <- 15 chars width for name field
        builder.add("the_geom", Polygon.class);
        builder.add("id_", Integer.class);
        builder.add("area", Float.class);
        builder.add("perimeter", Float.class);

        return builder.buildFeatureType();
		
	}
	
	
	/*
	 * Helper that returns a Polygon Feature that's ready for action.
	 */
	public static SimpleFeature makeBasicPolyFeature(Polygon poly, int id, String name) {
		SimpleFeatureType type = makeBasicPolyType(name);
		
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
		
		featureBuilder.set("the_geom", poly);
		featureBuilder.set("id_", id);
		featureBuilder.set("area", poly.getArea());
		featureBuilder.set("perimeter", poly.getLength());
        
        SimpleFeature feature = featureBuilder.buildFeature(null);
		return feature;
	}
	
	/**
	 * 
	 * @param features
	 */
	public static void printAttributes(FeatureCollection<SimpleFeatureType, SimpleFeature> features) {
		List<AttributeDescriptor> attList = features.getSchema().getAttributeDescriptors();
		Iterator<AttributeDescriptor> attIter = attList.iterator();
		while (attIter.hasNext()) {
			AttributeDescriptor desc = attIter.next();
			System.out.println("Attrribute: "+desc.getName().toString() + 
					", "+features.getSchema().getType(desc.getName()));
			
		}
	}
	
	/**
	 * 
	 * @param fileName
	 */
	public static void printAttributes(String fileName) {
		File file = new File(fileName);
		FeatureCollection<SimpleFeatureType, SimpleFeature> features = getFeatureCollection(file);
		printAttributes(features);
	}
	
	
	/**
	 * 
	 * @param fileName
	 */
	public static void printFeatures(String fileName) {
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = getFeatureCollection(new File(fileName));
		FeatureIterator<SimpleFeature> iter=collection.features();
		while (iter.hasNext()) {
			SimpleFeature feature = iter.next();
			System.out.print(feature.getID()+", ");
			List<AttributeDescriptor> attList = collection.getSchema().getAttributeDescriptors();
			Iterator<AttributeDescriptor> attIter = attList.iterator();
			while (attIter.hasNext()) {
				AttributeDescriptor desc = attIter.next();
				//System.out.println("Att "+desc.getName().toString());
				String name = desc.getName().toString();
				System.out.print(feature.getAttribute(name) + ", ");
			}
			System.out.print("\n");
		}
	}
	
	/**
	 * 
	 * @param fileName
	 */
	public static void printFeatureDesc(String fileName) {
		FeatureCollection<SimpleFeatureType, SimpleFeature> collection = getFeatureCollection(new File(fileName));
		System.out.println(collection.getID());
		System.out.println(collection.getSchema().getTypeName());
		System.out.println(collection.getSchema().getDescription());
		System.out.println(collection.getSchema().getName());
		System.out.println(collection.getSchema());
	}
	
	
	/**
	 * Update to GeoTools 2.7
	 * @param file
	 * @return
	 */
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection(File file) {
		FileDataStore store = null;
		FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = null;
		FeatureCollection<SimpleFeatureType, SimpleFeature> features = null;
		try {
			store = FileDataStoreFinder.getDataStore(file);
			featureSource = store.getFeatureSource();
			features = featureSource.getFeatures();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return features;
	}
	
	/*
	 * Returns a ShapeFileDataStore of the specified type and name.
	 */
	public static ShapefileDataStore makeShapefileDataStore(String fileName, SimpleFeatureType type) {
		File newFile = new File(fileName);

        DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();

        ShapefileDataStore newDataStore = null;
		try {
			newDataStore = new ShapefileDataStore(newFile.toURI().toURL());
			newDataStore.createSchema(type);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		return newDataStore;

	}
	
	/*
	 * Writes an entire FeatureCollection to a DataStore;
	 */
	public static void writeFeatures(DataStore store, FeatureCollection fc, String sourceName) {
		try {
			FeatureStore fStore = (FeatureStore)(store.getFeatureSource(sourceName));
			fStore.addFeatures(fc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/*
	 * Makes a polygon bounding box from pixel indices.  
	 * OUTPUT IS IN RASTER COORDINATES!!
	 */
	public static Polygon makePixelPoly(int x, int y) {

		Coordinate[] coordArray = new Coordinate[5];
		// go clockwise from upper left
		coordArray[0] = new Coordinate(x-0.5, y+0.5);
		coordArray[1] = new Coordinate(x+0.5, y+0.5);
		coordArray[2] = new Coordinate(x+0.5, y-0.5);
		coordArray[3] = new Coordinate(x-0.5, y-0.5);
		coordArray[4] = new Coordinate(x-0.5, y+0.5);
		
		CoordinateArraySequence cas = new CoordinateArraySequence(coordArray);
		PrecisionModel pm = new PrecisionModel();
		GeometryFactory gf = new GeometryFactory(pm);
		LinearRing lr = new LinearRing(cas, gf); 
		Polygon p = new Polygon(lr, null, gf);
		
		return p;
	}
	
	
	/*
	 * Extracts the polygons around pixels of image using centroids.
	 */
	public static void extractPolygons(String imageFile, String centroidShpFile, String outShpFile) {
		PlanarImage image = JAIUtils.readImage(imageFile);
		FeatureCollection<SimpleFeatureType, SimpleFeature> centroids = getFeatureCollection(new File(centroidShpFile));
		FeatureIterator <SimpleFeature>centroidIter = centroids.features();
		FeatureCollection pixels = FeatureCollections.newCollection();
		PlanarImage ref = JAIUtils.readImage(imageFile);
		SimpleFeature feature = null;
		Polygon pixelPoly = null;
		int id;
		double[] trainPtXY = new double[2];
		int[] imageXY = null;
		
		// iterate over the centroids
		while (centroidIter.hasNext()) {
			try {
				feature = centroidIter.next();
				// get id
				id = ((Integer) feature.getAttribute("id_")).intValue();
				// get the projected coordinates of the point
				trainPtXY[0] = ((Point)feature.getDefaultGeometry()).getX();
				trainPtXY[1] = ((Point)feature.getDefaultGeometry()).getY();
				// convert to pixel coordinates
				imageXY = JAIUtils.getPixelXY(trainPtXY, image);
				// make a polygon in pixel coordinates
				pixelPoly = makePixelPoly(imageXY[0], imageXY[1]);
				// get a transformation based on the image georeferencing
				AffineTransformation pixel2proj = raster2proj(ref);
				// transform the pixel polygon into a projected polygon
				pixelPoly.apply(pixel2proj);
				// update
				pixelPoly.geometryChanged();
				pixels.add(makeBasicPolyFeature(pixelPoly, id, "pixel"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		DataStore ds = makeShapefileDataStore(outShpFile, 
					   makeBasicPolyType("pixel"));
					   writeFeatures(ds, pixels, "pixel");
	}

	/*
	 * Method to turn pixels into polygons.
	 */
	public static void gridMake(String imageFile, String outShpFile) {
		
		FeatureCollection pixels = FeatureCollections.newCollection();
		
		PlanarImage ref = JAIUtils.readImage(imageFile);
		int width = ref.getWidth();
		int height = ref.getHeight();
		
		// iterate over the pixels 
		int id = 0;
		for (int y=0; y<10; y++) {
			for (int x=0; x<10; x++) {
				Polygon pixelPoly = makePixelPoly(x, y);
				// get a transformation based on the image georeferencing
				AffineTransformation pixel2proj = raster2proj(ref);
				// transform the pixel polygon into a projected polygon
				pixelPoly.apply(pixel2proj);
				// update
				pixelPoly.geometryChanged();
				
				pixels.add(makeBasicPolyFeature(pixelPoly, id, "pixel"));
				
				System.out.println("Making pixel "+x+", "+y);
				id++;
			}
		}
		
		DataStore ds = makeShapefileDataStore(outShpFile, 
								makeBasicPolyType("pixel"));
		writeFeatures(ds, pixels, "pixel");
	}

	/*
	 * Returns a feature collection of centroids from the supplied polygons.
	 * The FeatureSource will be called "centroids".
	 */
	public static FeatureCollection polyCentroids(FeatureCollection polygons) {

		FeatureCollection centroids = FeatureCollections.newCollection();
		FeatureIterator<SimpleFeature> iter = polygons.features();
		Geometry poly = null;
		Point centroid = null;
		try {
			
			// iterate over the polygons
			int id = 0;
			while (iter.hasNext()) {
				SimpleFeature feature = iter.next();
				poly = (Polygon)feature.getDefaultGeometry();
				centroid = poly.getCentroid();
				centroids.add(makeBasicPointFeature(centroid, id, "centroid"));
				id++;
			}
		} catch (Exception e) {
				e.printStackTrace();
		} finally {
			polygons.close(iter);
		}
		return centroids;
	}

	/*
	 * Writes a single feature to a DataStore
	 */
	public static void writeFeature(DataStore store, SimpleFeature f, String sourceName) {
		try {
			FeatureStore fStore = (FeatureStore)(store.getFeatureSource(sourceName));
			//also works:
			//FeatureCollection fc = new PFeatureCollection("temp", f.getFeatureType());
			FeatureCollection fc = FeatureCollections.newCollection();
			fc.add(f);
			fStore.addFeatures(fc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sample without replacement.
	 * See http://eyalsch.wordpress.com/2010/04/01/random-sample/
	 * @param collection
	 * @param n
	 * @param m
	 * @return
	 */
	public static FeatureCollection[] woReplSubsets(FeatureCollection collection, int n, int m) {
		// initialize
		FeatureCollection[] subsets = new FeatureCollection[m];
		for (int i=0; i<m; i++) {
			subsets[i] = FeatureCollections.newCollection();
		}
		// this is an array of indices to keep track of each sub-sample as it grows
		int[] theNs = new int[m];
		Arrays.fill(theNs, n);
		Random rnd = new Random();
		int visited = 0;
		FeatureIterator<SimpleFeature> iter = collection.features();
	    while (iter.hasNext()){
	    	SimpleFeature item = iter.next();
	    	// for each subsample
	    	for (int i=0; i<m; i++) {
	    		if (theNs[i] == 0) { continue; } // already done with this one
		        if (rnd.nextDouble() < ((double)theNs[i])/(collection.size() - visited)){
		        	subsets[i].add(item);
		            theNs[i]--;
		        }
	    	}
	        visited++;
	    }
		return subsets;
	}
	
	
	
	/**
	 * Sample with replacement.
	 * @param collection
	 * @param n
	 * @param m
	 * @return
	 */
	public static FeatureCollection<SimpleFeatureType,SimpleFeature>[] wReplSubsets(
			FeatureCollection<SimpleFeatureType,SimpleFeature> collection, int n, int m) {
		// copy the input into a random access FeatureCollection
		MemoryFeatureCollection memCollection = new MemoryFeatureCollection(collection.getSchema());
		memCollection.addAll(collection);
		// output
		FeatureCollection<SimpleFeatureType,SimpleFeature>[] subsets = new FeatureCollection[m];
		for (int i=0; i<m; i++) {
			// initialize
			subsets[i] = FeatureCollections.newCollection();
			Random rnd = new Random();
			for (int j=0; j<n; j++) {
				int randomID = 1+rnd.nextInt(collection.size()-1);
				String featureID = collection.getSchema().getTypeName()+"."+randomID;
				SimpleFeature randomChoice = memCollection.getFeatureMember(featureID);
				//System.out.println("Adding: i,j,ID: "+i+","+j+","+randomID);
				//System.out.println(randomChoice);
				subsets[i].add(randomChoice);
			}
		}
		return subsets;
	}
	
	
	
	/**
	 * Compute Earth radius at the given latitude. From NIMA WGS84 doc.
	 * @param latitude in decimal degrees.
	 * @return radius in meters
	 */
	public static double earthRadius(double latitude) {
	
		Ellipsoid ellipsoid = DefaultGeodeticDatum.WGS84.getEllipsoid();
		double a = ellipsoid.getSemiMajorAxis();
		double b = ellipsoid.getSemiMinorAxis();
	
		// convert to radians
		double l = latitude*Math.PI/180.0;
		double numerator = Math.pow(Math.pow(a, 2)*Math.cos(l), 2) + Math.pow(Math.pow(b, 2)*Math.sin(l), 2);
		double denominator = Math.pow(a*Math.cos(l), 2) + Math.pow(b*Math.sin(l), 2);	
		// from Wiki
		return Math.sqrt(numerator/denominator);
	}

	
	
	/**
	 * 
	 * @param lat1 latitude [0, 90] in degrees.
	 * @param lon1 longitude [0, 360] in degrees.
	 * @param lat2
	 * @param lon2
	 * @return 
	 */
	public static double arcLength(double lat1, double lon1, double alt1, double lat2, double lon2, double alt2) {
		// position 1 at MSL
		ArrayRealVector p = cartesian(lat1, lon1, alt1);
		// position 2 at MSL
		ArrayRealVector q = cartesian(lat2, lon2, alt2);
		// angle between p and q
		double t = angle(p, q);
		double r = earthRadius(lat1);
		double arc = (t/180.0)*Math.PI*r;
		return arc;
	}

	
	
	/**
	 * 
	 * @param v1
	 * @param v2
	 * @return angle in degrees
	 */
	public static double angle(ArrayRealVector v1, ArrayRealVector v2) {
		return Math.acos((v1.dotProduct(v2)/(v1.getNorm()*v2.getNorm()))) / Math.PI * 180.0;
	}

	
	
	/**
	 * See http://www.ferris.edu/faculty/burtchr/papers/cartesian_to_geodetic.pdf 
	 * @param lat is latitude [0, 90] in degrees.
	 * @param lon is longitude [0, 360] in degrees.
	 * @param alt is height above WGS84 ellipsoid.
	 * @return
	 */
	public static ArrayRealVector cartesian(double lat, double lon, double alt) {
		double latR = lat/180.0*Math.PI;
		double lonR = lon/180.0*Math.PI;
		
		Ellipsoid ellipsoid = DefaultGeodeticDatum.WGS84.getEllipsoid();
		double a = ellipsoid.getSemiMajorAxis();
		//System.out.println("Semi-major = "+a);
		double b = ellipsoid.getSemiMinorAxis();
		//System.out.println("Semi-minor = "+b);
		double recipFlat = ellipsoid.getInverseFlattening();
		//System.out.println("Reciprocal of flattening = "+recipFlat);
		double e_sq = (Math.pow(a, 2)-Math.pow(b, 2)) / Math.pow(a, 2);
		//System.out.println("First eccentricity = "+e_sq);
		double n = a / Math.sqrt(1.0 - (e_sq*Math.pow(Math.sin(latR), 2)));
		
		double x = (n+alt)*Math.cos(latR)*Math.cos(lonR);
		double y = (n+alt)*Math.cos(latR)*Math.sin(lonR);
		double z = (n*(1 - e_sq) + alt)*Math.sin(latR);
		
		return new ArrayRealVector(new double[] {x, y, z});
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// this one was made by MS Paint and is not a GeoTiff
		/*
		String testFile = "F:\\NASA_Ames\\MODIS_NDVI\\ndvi2005_4gdalbf.tif";
		testFile = "F:\\NASA_Ames\\PRISM\\2001\\us_ppt_2001_combo.tif";
		
		// this one was made by Astro
		String testFile2 = "C:\\Documents and Settings\\Nicholas Clinton\\My Documents\\BETI\\segmentation_paper\\paperV2\\images\\inverness_ne_subset.tif";
		// From Arc?  It has a world file.
		String testFile3 = "C:\\Documents and Settings\\Nicholas Clinton\\My Documents\\BETI\\segmentation_paper\\paperV2\\images\\sanfrann_sw_subset.tif";
		
		System.out.println("Describing:");
		System.out.println(testFile2);
		describeGeoTiff(testFile2);
		imageStats(readImage(testFile2));
		System.out.println("Describing:");
		System.out.println(testFile3);
		describeGeoTiff(testFile3);
		imageStats(readImage(testFile3));
		*/
		
		//describeGeoTiff(testFile2);
		//describeGeoTiff(testFile3);
		//isGeoTiff(testFile);
		
		// the following works.
		/* 
		String shpFileString = "G:\\NASA_Ames\\training\\p37r32_2005_buffers.shp";
		File shpFile = new File(shpFileString);
		URL shpURL = null;
		try {
			shpURL = shpFile.toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		FeatureCollection polygons = getFeatureCollection(shpURL);
		FeatureCollection centroids = polyCentroids(polygons);
		
		DataStore ds = makeShapefileDataStore("G:\\NASA_Ames\\training\\p37r32_2005_centroids.shp", 
												makeBasicPointType("centroid"));
		writeFeatures(ds, centroids, "centroid");
		*/
		/*
		String testFile = "C:\\Program Files\\BerkeleyImageSeg\\sample\\ag-test_50-09-05_grid.tif";
		System.out.println("Describing:");
		System.out.println(testFile);
		describeGeoTiff(testFile);
		System.out.println();
		imageStats(readImage(testFile));
		*/
		/*
		String img = "C:\\Documents and Settings\\Nicholas Clinton\\My Documents\\BETI\\segmentation_paper\\paperV2\\images\\sanfrann_sw_subset_st_francis_yc.tif";
		PlanarImage pi = readImage(img);
		worldWriter(pi);
		*/
		
		
		// 20110802
		//String footprints = "C:/Users/owner/Documents/MASTER_imagery/SF_high_res/shapefiles/SF_Building_Footprints.shp";
		//printAttributes(footprints);
		//printFeatures(footprints);
		//printFeatureDesc(footprints);
		//FeatureCollection<SimpleFeatureType,SimpleFeature> foots = getFeatureCollection(new File(footprints));
		//FeatureCollection<SimpleFeatureType,SimpleFeature>[] subs = wReplSubsets(foots, 100, 100);
		
		// 20120331
//		String urbanPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2buf3k_polygons_simple_dissolve_geo_multi_erase.shp";
//		GISUtils.printAttributes(urbanPolys);
//		GISUtils.printFeatures(urbanPolys);
		
		// 20100402 polygon stats test
//		String testPolys = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/zonal_stats_test_shapes.shp";
//		FeatureCollection features = GISUtils.getFeatureCollection(new File(testPolys));
//		String testImage = "D:/MOD11A2/2010.07.04/LST_NIGHT/2010.07.04_LST_NIGHT_mosaic_geo.LST_Night_1km.tif";
//		PlanarImage image = JAIUtils.readImage(testImage);
//		String testOut = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/" +
//				"polygonStatsTable_test_lst0704.csv";
//		try {
//			polygonStatsTable(features, testOut, image, 0);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// OK.
		
		String lattice = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/shapefiles/gpt2id_lattices.shp";
		GISUtils.printAttributes(lattice);
	}
	
}
