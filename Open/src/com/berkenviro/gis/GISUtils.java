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


import com.berkenviro.imageprocessing.JAIUtils;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.stat.StatUtils;
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
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.datum.Ellipsoid;

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

	
	
	
	/*
	 * Method to create stats from a polygon.  Assumes same projection.
	 * Converts the Polygon to a Geometry in input image coords first.
	 */
	public static void polyStats(Polygon p, PlanarImage pi) {
		
		AffineTransformation at = raster2proj(pi);
		AffineTransformation inv = proj2raster(at);
		// transform the Shape to raster coordinates
		p.apply(inv);
		
		// iterate over all the pixels in the area, put in a List
		LinkedList<Double> pixelVals = new LinkedList<Double>();
		Envelope env  = p.getEnvelopeInternal();
		RandomIter iterator = RandomIterFactory.create(pi, null);
		
		// compute bounds from the extent of the shape and the image
		int maxX = Math.min((int)Math.round(env.getMaxX()), pi.getWidth());
		int maxY = Math.min((int)Math.round(env.getMaxY()), pi.getHeight());
		int minX = Math.max((int)env.getMinX(), 0);
		int minY = Math.max((int)env.getMinY(), 0);
		for (int x=minX; x<=maxX; x++) {
			for (int y=minY; y<=maxY; y++) {
				Coordinate[] ca = {new Coordinate(x,y)};
				CoordinateArraySequence cas = new CoordinateArraySequence(ca);
				PrecisionModel pm = new PrecisionModel();
				if (p.contains(new Point(cas, new GeometryFactory(pm)))){ 
					//System.out.println("Processing pixel ("+x+", "+y+")");
					pixelVals.add(new Double(iterator.getSampleDouble(x,y,0)));
				}
				else {
					continue;
				}
			}
		}

		// take a Double from the list, put it into a double[]
		double[] pixels = new double[pixelVals.size()];
		for (int k=0; k<pixels.length; k++) {
			pixels[k]=pixelVals.get(k).doubleValue();
		}
		
		Arrays.sort(pixels);
		// generate some stats
		System.out.println("mean= "+StatUtils.mean(pixels));
		System.out.println("variance= "+StatUtils.variance(pixels));
		System.out.println("minimum= "+pixels[0]);
		System.out.println("maximum= "+pixels[pixels.length-1]);
		// approximate, but close enough for a large dataset
		int median = (int)pixels.length/2;
		System.out.println("median= "+pixels[median]);
	}
	
	/*
	 * 
	 */
	public static Polygon transform(Polygon p, AffineTransformation at) {
		Coordinate[] coords = p.getCoordinates();
		for (Coordinate c : coords) {
			at.transform(c, c);
		}
		// update
		p.geometryChanged();
		return p;
	}
	
	/*
	 * Method to create stats from a polygon.  Assumes same projection.
	 * Converts the Polygon to a Geometry in input image coords first.
	 * Return array is {mean, variance, min, max, median}
	 */
	public static double[] polyStatsArray(Polygon p, PlanarImage pi) {
		
		AffineTransformation at = raster2proj(pi);
		AffineTransformation inv = proj2raster(at);
		// transform the Shape to raster coordinates
		//p.apply(inv);
		p = GISUtils.transform(p, inv);
		//System.out.println(p);
		
		// iterate over all the pixels in the area, put in a List
		LinkedList<Double> pixelVals = new LinkedList<Double>();
		Envelope env  = p.getEnvelopeInternal();
		RandomIter iterator = RandomIterFactory.create(pi, null);
		
		// compute bounds from the extent of the shape and the image
		int maxX = Math.min((int)Math.round(env.getMaxX()), pi.getWidth());
		int maxY = Math.min((int)Math.round(env.getMaxY()), pi.getHeight());
		int minX = Math.max((int)env.getMinX(), 0);
		int minY = Math.max((int)env.getMinY(), 0);
		for (int x=minX; x<=maxX; x++) {
			for (int y=minY; y<=maxY; y++) {
				Coordinate[] ca = {new Coordinate(x,y)};
				CoordinateArraySequence cas = new CoordinateArraySequence(ca);
				PrecisionModel pm = new PrecisionModel();
				if (p.contains(new Point(cas, new GeometryFactory(pm)))){ 
					//System.out.println("Processing pixel ("+x+", "+y+")");
					double pixelAbundance = iterator.getSampleDouble(x,y,0);
					pixelVals.add(new Double(pixelAbundance));
				}
				else {
					continue;
				}
			}
		}

		// take a Double from the list, put it into a double[]
		double[] pixels = new double[pixelVals.size()];
		for (int k=0; k<pixels.length; k++) {
			pixels[k]=pixelVals.get(k).doubleValue();
		}
		
		// approximate, but close enough for a large dataset
		int median = (int)pixels.length/2;
		// generate the stats
		Arrays.sort(pixels);
		return new double[] {StatUtils.mean(pixels), 
							 StatUtils.variance(pixels),
							 pixels[0],
							 pixels[pixels.length-1],
							 pixels[median]};
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
			// TODO Auto-generated catch block
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
			System.out.println("Att "+desc.getName().toString());
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
		Iterator iter = collection.iterator();
		while (iter.hasNext()) {
			SimpleFeature feature = (SimpleFeature) iter.next();
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
		String footprints = "C:/Users/owner/Documents/MASTER_imagery/SF_high_res/shapefiles/SF_Building_Footprints.shp";
		//printAttributes(footprints);
		//printFeatures(footprints);
		//printFeatureDesc(footprints);
		//FeatureCollection<SimpleFeatureType,SimpleFeature> foots = getFeatureCollection(new File(footprints));
		//FeatureCollection<SimpleFeatureType,SimpleFeature>[] subs = wReplSubsets(foots, 100, 100);
		
		
	}
	
}
