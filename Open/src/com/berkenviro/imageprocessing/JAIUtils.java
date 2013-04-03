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
package com.berkenviro.imageprocessing;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.SortedSet;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.RealVector;

import com.berkenviro.gis.GISUtils;
import com.sun.media.imageio.plugins.tiff.GeoTIFFTagSet;
import com.sun.media.imageio.stream.RawImageInputStream;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.SeekableOutputStream;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFEncodeParam;
import com.sun.media.jai.codec.TIFFField;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.util.AffineTransformation;

/**
 * 
 * Static JAI utilities.
 * 
 * @author Nicholas Clinton
 *
 */
public class JAIUtils {

	// set for x64 systems
	static {
		System.setProperty("com.sun.media.jai.disableMediaLib", "true");
	}
	
	/**
	 * Write an image as a float Tiff.
	 * 
	 * @param data is a WritableRaster containing the data to write
	 * @param outFile is the absolute path of the output filename
	 */
	public static void writeFloatTiff(WritableRaster data, String outFile) {
		writeTiff(data, outFile, DataBuffer.TYPE_FLOAT);
	}
	
	
	/**
	 * Write an image with the specified data buffer type.  
	 * @param data is a WritableRaster containing the data to write
	 * @param outFile outFile is the absolute path of the output filename
	 * @param bufferType is the DataBuffer type
	 */
	public static void writeTiff(WritableRaster data, String outFile, int bufferType) {
		System.out.println("writing image file: "+outFile);
		int width = data.getWidth();
		int height = data.getHeight();
		int bands = data.getNumBands();
		
		SampleModel sModel = RasterFactory.createBandedSampleModel(
	    								bufferType,
	    								width,
	    								height,
	    								bands);
	   
		// create a compatible ColorModel
		ColorModel cModel = PlanarImage.createColorModel(sModel);
	    
		//System.out.println("Sample model: ");
		//System.out.println(sModel.toString());
		//System.out.println("Color model: ");
		//System.out.println(cModel.toString());
		
		// Create TiledImages using the float SampleModel.
		TiledImage tImage = new TiledImage(0,0,width,height,0,0,sModel,cModel);
		// Set the data of the tiled images to be the rasters.
		tImage.setData(data);
		JAI.create("filestore",tImage,outFile,"TIFF");
	}
	
	/**
	 * Write an image with the specified data buffer type.  
	 * @param data is a WritableRaster containing the data to write
	 * @param outFile outFile is the absolute path of the output filename
	 * @param bufferType is the DataBuffer type
	 */
	public static void writeTiff(WritableRaster data, String outFile) {
		writeTiff(data, outFile, data.getDataBuffer().getDataType());
	}
	
	/**
	 * Not working Geotiff writer.  Probably should be replaced by a GDAL method. 
	 * @param width
	 * @param height
	 * @param outFile
	 * @param geoFields
	 * @param data
	 */
	public static void writeFloatGeoTiff(int width, int height, String outFile, TIFFField[] geoFields, WritableRaster data) {
		
		BufferedOutputStream buff = null;
		try {
			OutputStream out = new SeekableOutputStream(new RandomAccessFile(new File(outFile), "rw"));
			buff = new BufferedOutputStream(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		// Create the ParameterBlock.
		TIFFEncodeParam param = new TIFFEncodeParam();
		//param.setWriteTiled(true);
		param.setExtraFields(geoFields);
		//param.setCompression(param.COMPRESSION_NONE);

		// float sample model
		SampleModel sModel = RasterFactory.createBandedSampleModel(
											DataBuffer.TYPE_FLOAT,
											width,
											height,
											1);

		// create a compatible ColorModel
		ColorModel cModel = PlanarImage.createColorModel(sModel);
		
		// Create the TIFF image encoder.
		ImageEncoder encoder = ImageCodec.createImageEncoder("TIFF", buff, param);
		try {
			encoder.encode(data, cModel);
			buff.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * Create an image of the specified normalized index.  The index is of the form
	 * (posBand - negBAnd) / (posBand + negBand) as in NDVI.
	 * 
	 * @param image is the input image
	 * @param posBand is the positive band in the numerator
	 * @param negBand is the negative band in the numerator
	 * @return a PlanarImage of the specified index
	 */
	public static PlanarImage normIndex(PlanarImage image, int posBand, int negBand) {
		TiledImage tImage = null;
		try {
			RandomIter iterator = RandomIterFactory.create(image, null);
	    	
	    	// make the output image
	    	int width = image.getWidth();
			int height = image.getHeight();
			WritableRaster indexRas = RasterFactory.createBandedRaster(
	    											DataBuffer.TYPE_FLOAT,
	    											width,
	    											height,
	    											1,
	    											new Point(0,0));
			
			// iterate over the inputs, set the output raster value
			for (int y=0; y<height; y++) {  	// each line
				for (int x=0; x<width; x++){	// each pixel
					
					double pos = iterator.getSampleDouble(x,y,posBand);
					double neg = iterator.getSampleDouble(x,y,negBand);
					double index = (pos - neg)/(pos + neg);
					indexRas.setSample(x,y,0,(float) index);
					
				}
			}
			
			// free up some memory
			image = null;
			iterator = null;
			System.gc();
			
			// write
			// create a float (32 bit) sample model
			SampleModel sModel = RasterFactory.createBandedSampleModel(
	    											DataBuffer.TYPE_FLOAT,
	    											width,
	    											height,
	    											1);
	    
			// create a compatible ColorModel
			ColorModel cModel = PlanarImage.createColorModel(sModel);
	    
			// Create a TiledImage using the float SampleModel.
			tImage = new TiledImage(0,0,width,height,0,0,sModel,cModel);
			// Set the data of the tiled image to be the raster.
			tImage.setData(indexRas);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tImage;
	}
	
	
	/**
	 * Converter from JAI message board archive.  Reads the image from a file.
	 * @param filename is the absolute path of the file
	 * @return a PlanarImage
	 */
	public static PlanarImage readImage(String filename) {
	     
		RenderedOp im = JAI.create("fileload", filename);
	
		// set the filename property
		im.setProperty("fileName", filename);
		//System.out.println("Loaded: "+im.getProperty("fileName"));
		
	    // For now ... make x & y tiles 512 **Note:  shouldn't hardcode this
	    SampleModel sampleModel = im.getSampleModel().createCompatibleSampleModel(512,512);
	
	    // Create the tiled image
	    TiledImage tm = new TiledImage(im.getMinX(),
	    		 						im.getMinY(),
	    		 						im.getWidth(),
	    		 						im.getHeight(),
	    		 						im.getTileGridXOffset(),
	    		 						im.getTileGridYOffset(),
	    		 						sampleModel,
	    		 						im.getColorModel());
	    tm.set(im);
	
	    // Request tiles from tiledimage thereby forcing them 
	    // to be loaded from the RenderedImaged.
	    // Commented due to taking forever and eventually causing a memory error.
	    /*
	    for (int ty=0; ty<tm.getNumYTiles(); ty++) {
	    	for (int tx=0; tx<tm.getNumXTiles(); tx++) {
	    		tm.getTile(tx,ty);
	    	}
	    }
	    */
	    
	    // Let's get rid of the duplicate image
	    im = null;
	    // garbage collect
	    runGc();
	    // Return a tiled image
	    return tm;
	}

	/**
	 * Read an image into a matrix.
	 * 
	 * @param filename is the full path of the input image.
	 * @param band is the index of the band to read.
	 * @return the specified band as a Matrix with coordinates (x,y)
	 */
	public static Array2DRowRealMatrix readImageMatrix(String filename, int band) {
	    // input
		PlanarImage pi = readImage(filename);
		int width = pi.getWidth();
		int height = pi.getHeight();
		// output
		Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(width, height);
		// all pixels
		RandomIter iterator = RandomIterFactory.create(pi, null);
		for (int y=0; y<height; y++) {  	// each line
			for (int x=0; x<width; x++){	// each pixel
				matrix.setEntry(x, y, iterator.getSampleDouble(x, y, band));
			}
		}
		return matrix;
	}
	
	/**
	 * Make a tiled image??
	 * @param image
	 * @return
	 */
	public static TiledImage createDisplayImage(PlanarImage image){
		//SampleModel sampleModel = image.getSampleModel();
		SampleModel sampleModel = image.getSampleModel().createCompatibleSampleModel(512,512);
		ColorModel colorModel = image.getColorModel();
	
		TiledImage ti = new TiledImage(image.getMinX(), 
									   image.getMinY(),
									   image.getWidth(), 
									   image.getHeight(),
									   image.getTileGridXOffset(),
									   image.getTileGridYOffset(),
									   sampleModel, 
									   colorModel);
		//ti.setData(image.copyData());
		// try it the other way
		ti.set(image);
		return ti;
	}

	
	/**
	 * Get the GeoTiff Fields.
	 * @param imageFileName is the absolute path of the file
	 * @return com.sun.media.jai.codec.TIFFField[]
	 */
	public static TIFFField[] getGeoTiffFields(String imageFileName) {
		TIFFDirectory dir = getTiffDirectory(imageFileName);
		
		GeoTIFFTagSet geoTags = GeoTIFFTagSet.getInstance();
		SortedSet ss = geoTags.getTagNumbers();
		Object[] ints = ss.toArray();
		ArrayList fieldList = new ArrayList();
		TIFFField geoField = null;
		int tag;
		for (int i=0; i<ints.length; i++) {
			tag = ((Integer)ints[i]).intValue();
			// works with JAI TIFFField
			geoField = dir.getField(tag);
			if (geoField != null) {
				fieldList.add(geoField);
			}

		}
		TIFFField[] geoFields = new TIFFField[fieldList.size()];
		for (int j=0; j<fieldList.size(); j++) {
			geoFields[j] = (TIFFField) fieldList.get(j);
		}
		return geoFields;
	}
	
	
	/**
	 * This method gets all the header information from the 
	 * GeoTIFF directories and prints it to the screen.  See GeoTIFF
	 * specification for the meaning of the codes that are printed. 
	 * @param iFileName is the absolute path of the file
	 */
	public static void describeGeoTiff(String iFileName) {
	
		System.out.println("Reading GeoInfo from "+iFileName);
		
		TIFFDirectory dir = getTiffDirectory(iFileName);
		
		 TIFFField[] tFields = dir.getFields();
		 System.out.println("TIFF 6.0 tags present:");
		 for (int i=1; i<tFields.length; i++) {
			 // dump field ID's to terminal
			 System.out.println("tag ID: "+tFields[i].getTag());
		 }
		 
		 // look for particular fields associated with the GeoTIFF spec
		 System.out.println("Checking GeoTags:");
		 System.out.println("TAG_GEO_KEY_DIRECTORY="+GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY);
		 System.out.println("TAG_GEO_ASCII_PARAMS="+GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS);
		 System.out.println("TAG_GEO_DOUBLE_PARAMS="+GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS);
		 System.out.println("TAG_MODEL_PIXEL_SCALE="+GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
		 System.out.println("TAG_MODEL_TIE_POINT="+GeoTIFFTagSet.TAG_MODEL_TIE_POINT);
		 System.out.println("TAG_MODEL_TRANSFORMATION="+GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION);
		 System.out.println("TAG_MODEL_TRANSFORMATION="+GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION);
		 // look for the geoTags, report
		 GeoTIFFTagSet geoTags = GeoTIFFTagSet.getInstance();
		 SortedSet ss = geoTags.getTagNumbers(); // tag numbers
		 Object[] ints = ss.toArray();  // tag numbers
		 TIFFField f;
		 int tag;
		 for (int i=0; i<ints.length; i++) {
			 tag = ((Integer)ints[i]).intValue();  // tag number
			 // works with JAI TIFFField
			 f = dir.getField(tag); // field associated with tag
			 if (f == null) {
				 System.out.println("No Directory:"+geoTags.getTag(tag).getName());
				 System.out.println();
			 }
			 else {
				 System.out.println("Tag: "+geoTags.getTag(tag).getName());
				 System.out.println("Tag type: "+printType(f));
				 System.out.println("Number of keys = "+f.getCount());
				 
				 // extract info from the TIFFField
				 int j, val;
				 double d;
				 switch (f.getTag()) {
				 	case GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY:
				 		// KeyEntry = { KeyID, TIFFTagLocation, Count, Value_Offset }
				 		// every 4 values
				 		for (j=0; j<f.getCount(); j++) {
				 			val = f.getAsInt(j); // int conversion apparently necessary for the JAI TIFFField
					 		System.out.println("key: "+j+", value: "+val);
					 		
				 		}
				 		break;
				 	case GeoTIFFTagSet.TAG_GEO_ASCII_PARAMS:
				 		// there is only one ASCII value
				 		System.out.println(f.getAsString(0));
				 		break;
				 	case GeoTIFFTagSet.TAG_GEO_DOUBLE_PARAMS:
				 		for (j=0; j<f.getCount(); j++) {
				 			d = f.getAsDouble(j);
					 		System.out.println("key: "+j+", value: "+d);
				 		}
				 		break;
				 	case GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE:
				 		for (j=0; j<f.getCount(); j++) {
				 			d = f.getAsDouble(j);
					 		System.out.println("key: "+j+", value: "+d);
				 		}
				 		break;
				 	case GeoTIFFTagSet.TAG_MODEL_TIE_POINT:
				 		for (j=0; j<f.getCount(); j++) {
				 			d = f.getAsDouble(j);
					 		System.out.println("key: "+j+", value: "+d);
				 		}
				 		break;
				 }
				 System.out.println();
				 
			 }
		 }
	}

	/**
	 * This method takes a double[] [x,y] in the coordinate space of the 
	 * projected image and the reference image and returns the [x,y]
	 * indices of the pixel containing the projected point.
	 * 
	 * @param ImageProjXY is the projected coordinate array {x,y}
	 * @param pImage is the PlanarImage to search
	 * @return an int[] of pixel {x,y}
	 */
	public static int[] getPixelXY(double[] ImageProjXY, PlanarImage pImage) throws Exception {
		// check to make sure the tiff is registered
		if (!isRegistered(pImage)) { register(pImage); }
		
		AffineTransformation at = GISUtils.raster2proj(pImage);
		//System.out.println("JAI affine: "+at);
		AffineTransformation inv = GISUtils.proj2raster(at);
		Coordinate pix = new Coordinate();
		inv.transform(new Coordinate(ImageProjXY[0], ImageProjXY[1]), pix);
		if ((int)pix.x < 0 || (int)pix.x >= pImage.getWidth() || 
			(int)pix.y < 0 || (int)pix.y >= pImage.getHeight()) {
			throw new Exception("Impossible coordinates: "+pix);
		}
		return new int[] {(int)pix.x, (int)pix.y};
	}

	/**
	 * This method takes an int[] [x,y] of ZERO indexed pixel coordinates and returns the 
	 * center of the pixel in projected coordinates.
	 * 
	 * @param pixelXY is the pixel indices
	 * @param pImage is the image to use
	 * @return a double[] {x,y} projected coordinates of the pixel centroid
	 */
	public static double[] getProjectedXY(int[] pixelXY, PlanarImage pImage) throws Exception {
		Coordinate pix = new Coordinate(pixelXY[0]+0.5, pixelXY[1]+0.5);
		if (pix.x<0 | pix.y<0 | pix.x>pImage.getWidth() | pix.y>pImage.getHeight()) {
			throw new Exception("Outside image bounds: "+pix);
		}
		if (!isRegistered(pImage)) { register(pImage); }
		AffineTransformation at = GISUtils.raster2proj(pImage);
		// projected pixel, empty for now
		Coordinate projPix = new Coordinate();
		at.transform(pix, projPix);
		return new double[] {projPix.x, projPix.y};
	}

	/**
	 * Helper method.  Simply uses JAI to open a TIFF and get its
	 * directory, exposing its tags.
	 * @param iFileName is the full path to the image file
	 */	 
	public static TIFFDirectory getTiffDirectory(String iFileName) {
		/*
		 * this is from javax.media.jai.operator.TIFFDescriptor
		 * but the seekable stream code try/catch is added
		 */ 
		ParameterBlock pb = new ParameterBlock();
		
		SeekableStream s = null;
		try {
			s = new FileSeekableStream(iFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		pb.add(s);
	
		TIFFDecodeParam param = new TIFFDecodeParam();
		pb.add(param);
		 
		RenderedOp op = JAI.create("tiff", pb);
		return (TIFFDirectory)op.getProperty("tiff_directory");
	}


	/**
	 * Not working???
	 * @param pi is a PlanarImage
	 * @param roi is an ROIShape in raster coordinates
	 */
	public static void imageStats(PlanarImage pi, ROIShape roi) {
		
		ParameterBlock pb = new ParameterBlock();
	    pb.addSource(pi);
	    pb.add(roi);
	    pb.add(1);
	    pb.add(1);
	
	    // Perform the extrema operation.
	    RenderedOp extrem = JAI.create("extrema", pb);
	    double[][] extrema = (double[][]) extrem.getProperty("extrema");
		System.out.println("Min = "+extrema[0][0]);
		System.out.println("Max = "+extrema[1][0]);
	    
	    // mean
		RenderedOp mean = JAI.create("mean", pb);
		double[] meanA = (double[]) mean.getProperty("mean");
		System.out.println("Mean = "+meanA[0]);
		System.out.println();
	}

	/**
	 * This method is for overlaying a georeferenced point on a referenced image.  
	 * Assumes that the projections match.
	 * 
	 * @param pt is the point in projected coordinates
	 * @param pImage is the image on which to overlay
	 * @return the pixel value
	 */
	public static double imageValue(com.vividsolutions.jts.geom.Point pt, PlanarImage pImage) throws Exception {
		if (pImage.getProperty("isReferenced").getClass() != Boolean.class ||
			(Boolean)pImage.getProperty("isReferenced").equals(false) ) {
				register(pImage); 
		}
		double[] xy = {pt.getX(), pt.getY()};
		int[] pixelXY = getPixelXY(xy, pImage);
		RandomIter iterator = null;
		iterator = RandomIterFactory.create(pImage, null);
		return iterator.getSampleDouble(pixelXY[0],pixelXY[1],0);
	}

	/**
	 * This method is for overlaying a georeferenced point on a referenced image.  
	 * Assumes that the projections match.
	 * Caller needs to instantiate the RandomIter and call JAIUtils.register(pImage) first.
	 * 
	 * @param pt is the point in projected coordinates
	 * @param pImage is the image on which to overlay
	 * @param iterator is a RandomIter for pImage
	 * @return the pixel value
	 */
	public static double imageValue(com.vividsolutions.jts.geom.Point pt, PlanarImage pImage, RandomIter iterator) throws Exception {
		double[] xy = {pt.getX(), pt.getY()};
		int[] pixelXY = getPixelXY(xy, pImage);
		return iterator.getSampleDouble(pixelXY[0],pixelXY[1],0);
	}
	
	/**
	 * Checks for GeoTIFF tags.  For our purposes, these are necessary:
	 * GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE
	 * GeoTIFFTagSet.TAG_MODEL_TIE_POINT
	 * The GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY is necessary for projection
	 * information, but is not checked by this method.
	 * @param iFileName is the full path file name
	 */
	public static boolean isGeoTiff(String iFileName) {
		TIFFDirectory dir = getTiffDirectory(iFileName);
		if (dir.isTagPresent(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE)) {
			if (dir.isTagPresent(GeoTIFFTagSet.TAG_MODEL_TIE_POINT)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the registration properties have been set.
	 * @param pi is the PlanarImage to check
	 * @return true if georeferenced
	 */
	public static boolean isRegistered(PlanarImage pi) {
		if ((pi.getProperty("isReferenced").getClass() != Boolean.class) || ((Boolean)pi.getProperty("isReferenced").equals(false)) ) {
				return false; 
		}
		return true;
	}

	
	/**
	 * Utility method for transferring georeferenceing 
	 * @param source
	 * @param dest
	 */
	public static void transferGeo(PlanarImage source, PlanarImage dest) {
		if (!JAIUtils.isRegistered(source)) { JAIUtils.register(source); }
		
		dest.setProperty("ulX", source.getProperty("ulX"));
		dest.setProperty("ulY", source.getProperty("ulY"));
		dest.setProperty("deltaX", source.getProperty("deltaX"));
		dest.setProperty("deltaY", source.getProperty("deltaY"));
		
		dest.setProperty("isReferenced", new Boolean(true));
	}
	
	
	/**
	 * Get a pixel.	
	 * @param pImage
	 * @param x
	 * @param y
	 * @param band
	 * @return
	 */
	public static double pixelValue(PlanarImage pImage, int x, int y, int band) {
		RandomIter iterator = null;
		iterator = RandomIterFactory.create(pImage, null);
		// will throw an exception if out of bounds
		return iterator.getSampleDouble(x, y, band);
	}

	/**
	 * Invokes geoReader() for a GeoTIFF, worldReader() otherwise.
	 * @param pImage is the image to reference.
	 */
	public static void register(PlanarImage pImage) {
		if (isGeoTiff((String)pImage.getProperty("fileName"))){
			geoReader(pImage);
		}
		else {
			worldReader(pImage);
		}
	}
	
	/**
	 * From Rodrigues.  Extra-strength garbage collection.
	 */
	public static void runGc() {
		Runtime rt = Runtime.getRuntime();
		rt.gc();
		rt.gc();
		rt.gc();
		long mem = rt.freeMemory();
		//System.out.println("Free memory = "+mem);
	}

	/**
	 * This method takes a PlanarImage, with its fileName property set
	 * to a string of its complete path name, and reads its corresponding 
	 * world file, putting the parameters in the image properties.
	 * line 0 = pixel x_change
	 * line 1 = row rotation
	 * line 2 = column rotation
	 * line 3 = pixel y_change
	 * line 4 = upper left pixel center x_coordinate
	 * line 5 = upper left pixel center y_coordinate
	 * @param pImgage is the tif image to check.  Will look for a ".tfw"
	 */
	public static void worldReader(PlanarImage pImage) {
		try {
			// look for the matching world file.
			String fileName = (String) pImage.getProperty("fileName");
			File worldFile = new File(fileName.replace(".tif", ".tfw"));
			// open a file reader and read the values
			BufferedReader reader = new BufferedReader(new FileReader(worldFile));
			int i = 0;
			String line = null;
			while ( (line = reader.readLine()) != null ) {
	
				switch (i) {
				case 0: pImage.setProperty("deltaX", Double.valueOf(line.trim()));
						break;
				case 3: pImage.setProperty("deltaY", Double.valueOf(line.trim()));
						break;
				case 4: pImage.setProperty("ulX", Double.valueOf(line.trim()));
						break;
				case 5: pImage.setProperty("ulY", Double.valueOf(line.trim()));
						break;
				}
				i++;
			}
		} 
		catch (Exception e) {
			System.err.println("Can not register image.");
			e.printStackTrace();
		}
		pImage.setProperty("isReferenced", new Boolean(true));
	}

	/**
	 * This method takes a PlanarImage, with its fileName property set
	 * to a string of its complete path name, reads its referencing,
	 * and writes to a world file
	 * line 0 = pixel x_change
	 * line 1 = row rotation
	 * line 2 = column rotation
	 * line 3 = pixel y_change
	 * line 4 = upper left pixel center x_coordinate
	 * line 5 = upper left pixel center y_coordinate
	 * @param pImage is the geo-referenced image to write a world file for
	 */
	public static void worldWriter(PlanarImage pImage) {
		String worldName = ((String) pImage.getProperty("fileName")).replace(".tif", ".tfw");
		worldWriter(pImage, worldName);
	}

	/**
	 * Write a world file for the specified PlanarImage and write it to the specified filename.
	 * 
	 * @param pImage is the input image
	 * @param worldName is the full path output name
	 */
	public static void worldWriter(PlanarImage pImage, String worldName) {
		BufferedWriter writer = null;
		try {
			// register the geoImage
			register(pImage);
			File worldFile = new File(worldName);
			writer = new BufferedWriter(new FileWriter(worldFile));
	
			for (int i=0; i<6; i++) {
				switch (i) {
					case 0: writer.write(((Double)pImage.getProperty("deltaX")).toString());
							writer.newLine();
							break;
					case 3: writer.write(((Double)pImage.getProperty("deltaY")).toString());
							writer.newLine();
							break;
					case 4: writer.write(((Double)pImage.getProperty("ulX")).toString());
							writer.newLine();
							break;
					case 5: writer.write(((Double)pImage.getProperty("ulY")).toString());
							writer.newLine();
							break;
					default: writer.write("0.0");
							writer.newLine();
				}
	
			}
		} 
		catch (Exception e) {
			System.err.println("Can not write worldfile");
			e.printStackTrace();
		}
	    finally {
	    	try {
	    		writer.close();
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    }
	}
	
	
	/**
	 * From Rodrigues.  Also see pg 502 and MakeTiledImage(), pg 500.
	 * 
	 * @param x
	 * @param y
	 * @param pixelValue
	 * @param image
	 */
	public static void writePixel(int x, int y, double pixelValue, TiledImage image) {
		   int xIndex = image.XToTileX(x);
		   int yIndex = image.YToTileY(y);
		   WritableRaster tileRaster = image.getWritableTile(xIndex, yIndex);
		   if (tileRaster != null) {
			   //tileRaster.setPixel(x,y, pixelValue);
			   tileRaster.setSample(x,y,0, (float)pixelValue);
		   }
		   image.releaseWritableTile(xIndex, yIndex);
	}

	/**
	 * Helper method for info.  Simply returns a string of the TIFFField type.
	 * @param f is the TIFFField
	 */
	private static String printType(TIFFField f){
		int type = f.getType();
		 switch (type) {
		 	case TIFFField.TIFF_ASCII:
		 		return "TIFF_ASCII";
		 	case TIFFField.TIFF_BYTE:
		 		return "TIFF_BYTE";
		 	case TIFFField.TIFF_DOUBLE:
		 		return "TIFF_DOUBLE";
		 	case TIFFField.TIFF_FLOAT:
		 		return "TIFF_FLOAT";
		 	case TIFFField.TIFF_LONG:
		 		return "TIFF_LONG";
		 	case TIFFField.TIFF_RATIONAL:
		 		return "TIFF_RATIONAL";
		 	case TIFFField.TIFF_SBYTE:
		 		return "TIFF_SBYTE";
		 	case TIFFField.TIFF_SHORT:
		 		return "TIFF_SHORT";
		 	case TIFFField.TIFF_SLONG:
		 		return "TIFF_SLONG";
		 	case TIFFField.TIFF_SRATIONAL:
		 		return "TIFF_SRATIONAL";
		 	case TIFFField.TIFF_SSHORT:
		 		return "TIFF_SSHORT";
		 	case TIFFField.TIFF_UNDEFINED:
		 		return "TIFF_UNDEFINED";
		 	default:
		 		return "Unrecognized type!";
		 }
	 }

	/**
	 * This method reads a GeoTIFF and uses the tags to 
	 * set the image properties needed by other methods.
	 * This is the equivalent method to worldReader.  See GeoTIFF 
	 * spec for implementation details.
	 * @param pImage is the GeoTiff to register
	 */
	public static void geoReader(PlanarImage pImage) {
		String iFileName = (String)pImage.getProperty("fileName");
		if (!isGeoTiff(iFileName)) {
			System.err.println(iFileName+" is not a GeoTIFF!  Try worldReader().");
			return;
		}
		 TIFFDirectory dir = getTiffDirectory(iFileName);
		 TIFFField[] tFields = dir.getFields();
		 double xScale, yScale;
		 TIFFField scaleField = dir.getField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
	
		 xScale = scaleField.getAsDouble(0);
		 yScale = scaleField.getAsDouble(1);
		 pImage.setProperty("deltaX", new Double(xScale));
		 pImage.setProperty("deltaY", new Double(-yScale));
		 
		 // This assumes that the first tie point is for upper left corner
		 TIFFField tpField = dir.getField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT);
		 
		 // check the Raster Space coordinate system used, GTRasterTypeGeoKey
		 boolean pixelIsArea = false;
		 TIFFField geoKeyField = dir.getField(GeoTIFFTagSet.TAG_GEO_KEY_DIRECTORY);
		 for (int j=0; j<geoKeyField.getCount(); j++) {
 			if (geoKeyField.getAsInt(j) == 1025) {
 				if (geoKeyField.getAsInt(j+3) == 2) {
 					pixelIsArea = true; 
 				}
 			}
		 }
		 
		 double ulX, ulY;
		 if (pixelIsArea) { // the UL coordinate in the TIFFfield is for the center of the pixel
			 double halfX = 0.5*xScale;
			 double halfY = 0.5*yScale;
			 ulX = tpField.getAsDouble(3)-halfX;
			 ulY = tpField.getAsDouble(4)+halfY;
			 pImage.setProperty("pixelIsArea", new Boolean(true));
		 } else {
			 ulX = tpField.getAsDouble(3);
			 ulY = tpField.getAsDouble(4);
		 }
		 
		 pImage.setProperty("ulX", new Double(ulX));
		 pImage.setProperty("ulY", new Double(ulY)); 
		 pImage.setProperty("isReferenced", new Boolean(true));
	}

	
	/**
	 * Illumination according to modified Phong model of Ashikhmin and Shirley (2000).
	 * Each pixel is 2pi/3000 for 716 pixel line scanner data.
	 * TODO: implement DEM based illumination from constant sensor zenith and azimuth.
	 * 
	 * @param sun_z is sun-zenith angle in radians
	 * @param sunaz sun azimuth in radians
	 * @param heading is the heading of the sensor (aircraft or satellite)
	 * @param height of the output image
	 * @param width of the output image
	 * @return a PlanarImage of illumination
	 */
	public static PlanarImage illumination(double sun_z, double sunaz, double heading, int height, int width) {
		// make the output raster
		WritableRaster illOut = RasterFactory.createBandedRaster(
    											DataBuffer.TYPE_FLOAT,
    											width,
    											height,
    											1,
    											new Point(0,0));
		// iterate over the input, set the output raster value
		for (int y=0; y<height; y++) {  	// each line
			for (int x=0; x<width; x++){	// each pixel
				double sens_z = 0;
				double sensaz = 0;
				
				// compute the geometry
				if (x == 358) { // nadir
					sens_z = 0;
					sensaz = heading;
				}
				else {
					if (x<358) {  // looking left
						sensaz = (heading-90.0 < 0) ? heading-90.0+360.0 : heading-90.0;
						sens_z = (double)(Math.abs(x-358))*(2.0*Math.PI/3000.0);
					}
					if (x>358) { // looking right
						sensaz = (heading+90.0 > 360) ? heading+90.0-360.0 : heading+90.0;
						sens_z = (double)(x-358)*(2.0*Math.PI/3000.0);
					}	
				}
				
				// modified Phong model from Michael Ashikhmin and Peter Shirley 2000
				// u=x, v=y, n=z
				double sunx = Math.cos(sunaz)*Math.sin(sun_z);
				double suny = Math.sin(sunaz)*Math.sin(sun_z);
				double sunz = Math.cos(sun_z);
				ArrayRealVector k1 = new ArrayRealVector(new double[] {sunx, suny, sunz});
				// u=x, v=y, n=z
				double sensx = Math.cos(sensaz)*Math.sin(sens_z);
				double sensy = Math.sin(sensaz)*Math.sin(sens_z);
				double sensz = Math.cos(sens_z);
				ArrayRealVector k2 = new ArrayRealVector(new double[] {sensx, sensy, sensz});
				// normalized half vector
				RealVector h = (k1.add(k2)).unitVector();
				// surface normal
				ArrayRealVector n = new ArrayRealVector(new double[]{0, 0, 1});
				// parameters
				double nv = 10;
				double nu = 10;
				// specular component
				double coeff = Math.sqrt((nu+1)*(nv+1))/8.0*Math.PI;
//				double exponent = (nu*Math.pow(dot(h, new double[] {1, 0, 0}), 2) + 
//									nv*Math.pow(dot(h, new double[] {0, 1, 0}), 2)) /
//									(1.0 - dot(h, n));
				double exponent = (nu*Math.pow(h.dotProduct(new double[] {1, 0, 0}), 2) + 
						nv*Math.pow(h.dotProduct(new double[] {0, 1, 0}), 2)) /
						(1.0 - h.dotProduct(n));
				// assume 0.5 specular reflectance
				double rs = 0.5;
				double f = rs + (1.0-rs)*(Math.pow((1.0-k1.dotProduct(h)), 5));
				double spec = coeff * f * Math.pow(n.dotProduct(h), exponent) / 
				h.dotProduct(k1)*Math.max(n.dotProduct(k1), n.dotProduct(k2));
				// diffuse component, assume 0.5
				double rd = 0.5;
				double dif = 28.0*rd*(1.0-rs)/23*Math.PI*(1-Math.pow((1-n.dotProduct(k1)/2), 5))*
				(1-Math.pow((1-n.dotProduct(k2)/2), 5));
				illOut.setSample(x, y, 0, spec+dif);
			}
		}		
		SampleModel sModel = RasterFactory.createBandedSampleModel(
				DataBuffer.TYPE_FLOAT,
				width,
				height,
				1);

		// create a compatible ColorModel
		ColorModel cModel = PlanarImage.createColorModel(sModel);
		
		System.out.println("Sample model: ");
		System.out.println(sModel.toString());
		System.out.println("Color model: ");
		System.out.println(cModel.toString());
		
		// Create TiledImages using the float SampleModel.
		TiledImage tImage = new TiledImage(0,0,width,height,0,0,sModel,cModel);
		// Set the data of the tiled images to be the rasters.
		tImage.setData(illOut);
		
		return tImage;
	}
	
	/**
	 * 
	 * @param image
	 * @return
	 */
	public static PlanarImage byteScale(PlanarImage image) {
		ParameterBlock pbMaxMin = new ParameterBlock();
	    pbMaxMin.addSource(image);
	    RenderedOp extrem = JAI.create("Extrema", pbMaxMin);
	    double[][] extrema = (double[][]) extrem.getProperty("Extrema");

	    // Rescale the image with the parameters
	    double[] scale = new double[image.getNumBands()];
	    double[] offset = new double[image.getNumBands()];
	    for (int b=0; b<image.getNumBands(); b++) {
	    	scale[b] = 255.0 / (extrema[1][b] - extrema[0][b]);
		    offset[b] = (255.0 * extrema[0][b]) / (extrema[0][b] - extrema[1][b]);
	    }
	    
	    ParameterBlockJAI pbRescale = new ParameterBlockJAI("Rescale");
	    pbRescale.addSource(image);
	    pbRescale.setParameter("constants", scale);
	    pbRescale.setParameter("offsets", offset);
	    PlanarImage surrogateImage = (PlanarImage)JAI.create("Rescale", pbRescale, null);

	    ParameterBlock pbConvert = new ParameterBlock();
	    pbConvert.addSource(surrogateImage);
	    pbConvert.add(DataBuffer.TYPE_BYTE);
	    return JAI.create("format", pbConvert);
	}
	
	/**
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return
	 */
	public static PlanarImage makeRGB(PlanarImage red, PlanarImage green, PlanarImage blue) {
		ParameterBlock pb = new ParameterBlock();
	    pb.addSource(red);
	    pb.addSource(green);
	    pb.addSource(blue);
	    return JAI.create("bandmerge", pb);
	}
	
	/**
     * Adjust to a Uniform distribution CDF.
     * @return the stretched image.
     */
    public static PlanarImage linearStretch(PlanarImage source) {
    	// From JAI programming guide
    	int numBands = source.getNumBands();
    	int binCount = 256;
    	// Create an equalization CDF.
    	float[][] CDFeq = new float[numBands][];
    	for(int b = 0; b < numBands; b++) {
    		CDFeq[b] = new float[binCount];
    		for(int i = 0; i < binCount; i++) {
    			CDFeq[b][i] = (float)(i+1)/(float)binCount;
    		}
    	}
    	int[] bins = { 256 };
        double[] low = { 0.0D };
        double[] high = { 256.0D };

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add(null);
        pb.add(1);
        pb.add(1);
        pb.add(bins);
        pb.add(low);
        pb.add(high);

        RenderedOp fmt = JAI.create("histogram", pb, null);
    	// Create a histogram-equalized image.
    	return JAI.create("matchcdf", fmt, CDFeq);
    }

    
    /**
     * Adjust to a normal distribution CDF, mean=128, SD=64.
     * @return the stretched image.
     */
    public static PlanarImage gaussianStretch(PlanarImage source) {
    	int numBands = source.getNumBands();
    	int binCount = 256;
    	float[][] CDFnorm = new float[numBands][binCount];
    	NormalDistributionImpl norm = new NormalDistributionImpl(128.0, 64.0);
    	for(int b = 0; b < numBands; b++) {
    		for(int i = 0; i < binCount-1; i++) {
    			try {
					CDFnorm[b][i] = (float)norm.cumulativeProbability(i);
				} catch (MathException e) {
					e.printStackTrace();
				}
    		}
    		// the cumulative probability must equal one
    		CDFnorm[b][binCount-1] = 1;
    	}

        int[] bins = { 256 };
        double[] low = { 0.0D };
        double[] high = { 256.0D };

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add(null);
        pb.add(1);
        pb.add(1);
        pb.add(bins);
        pb.add(low);
        pb.add(high);

        RenderedOp fmt = JAI.create("histogram", pb, null);
        return JAI.create("matchcdf", fmt, CDFnorm);
    }
	
    
	/**
	 * Normalize an image to zero-one.
	 * @param input is the full pathname of the image input.
	 * @param band is the input band
	 */
	public static void normalize(String input, int band) {
		try {
			// load the image
			PlanarImage pImage = JAIUtils.readImage(input);
			// make the output image
        	int width = pImage.getWidth();
			int height = pImage.getHeight();
			WritableRaster outIm = RasterFactory.createBandedRaster(
	    											DataBuffer.TYPE_FLOAT,
	    											width,
	    											height,
	    											band,
	    											new Point(0,0));
			// get stats from the entire image
			ParameterBlock pb = new ParameterBlock();
		    pb.addSource(pImage);   // The source image
		    pb.add(null);
		    pb.add(1);
		    pb.add(1);
		    // Perform the extrema operation on the source image
		    RenderedOp extrem = JAI.create("extrema", pb);
		    double[][] extrema = (double[][]) extrem.getProperty("extrema");
			double min = extrema[0][0];
			double max = extrema[1][0];
			
			// iterate over the input, set the output raster value
			RandomIter iterator = RandomIterFactory.create(pImage, null);
			for (int y=0; y<height; y++) {  	// each line
				for (int x=0; x<width; x++){	// each pixel
					double in = iterator.getSampleDouble(x,y,0);
					double out = (in-min)/max;
					outIm.setSample(x, y, 0, out);
				}
			}
			String outFileName = input.replace(".tif", "_norm.tif");
			System.out.println("Creating file "+outFileName);
			JAIUtils.writeFloatTiff(outIm, outFileName);
		} catch(Exception e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * Not working.
	 * See http://java.sun.com/products/java-media/jai/forDevelopers/samples/MultiPageRead.java
	 * @param filename
	 * @param band
	 * @return
	 */
	public static PlanarImage readMultiBandTiff(String filename, int band) {
		
		TiledImage tm = null;
        try {
        	File file = new File(filename);
            SeekableStream s = new FileSeekableStream(file);

            TIFFDecodeParam param = null;

            ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);

            System.out.println("Number of images in this TIFF: " +
                               dec.getNumPages());

            // Which of the multiple images in the TIFF file do we want to load
            // 0 refers to the first, 1 to the second and so on.
			RenderedImage im = dec.decodeAsRenderedImage(band);
            //RenderedImage im = dec.decodeAsRenderedImage();
		    // For now ... make x & y tiles 512 **Note:  shouldn't hardcode this
		    //SampleModel sampleModel = im.getSampleModel().createCompatibleSampleModel(512,512);
			SampleModel sampleModel = im.getSampleModel();
		    // Create the tiled image
		    tm = new TiledImage(im.getMinX(),
		    		 						im.getMinY(),
		    		 						im.getWidth(),
		    		 						im.getHeight(),
		    		 						im.getTileGridXOffset(),
		    		 						im.getTileGridYOffset(),
		    		 						sampleModel,
		    		 						im.getColorModel());
		    tm.set(im);
		    
		    
		    // Let's get rid of the duplicate image
		    im = null;
		    // garbage collect
		    runGc();
		    // Return a tiled image
			// set the filename property
			tm.setProperty("fileName", filename);
			System.out.println("Loaded: "+im.getProperty("fileName"));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return tm;
	}
	
	/**
	 * Create a single banded raster from the specified double[][]
	 * @param in is the input array, where the first index is x, the second is y
	 * @return the raster
	 */
	public static WritableRaster createRaster(double[][] in) {
		int width = in.length;
		int height = in[0].length;
		int numBands = 1;
		Point origin = new Point(0,0);
		
		WritableRaster out = RasterFactory.createBandedRaster(
				DataBuffer.TYPE_DOUBLE,
				width,
				height,
				numBands,
				origin);
		
		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				out.setSample(x, y, 0, in[x][y]);
			}
		}
		return out;
	}

	
	/**
	 * Method to print to screen some stats for an entire image.
	 * @param pi is the input PlanarImage
	 */
	public static void imageStats(PlanarImage pi) {
		
		// print some info about the data type
		System.out.println(pi.getSampleModel());
		int dataType = pi.getSampleModel().getDataType();
		Field[] fields = DataBuffer.class.getFields();
		for (Field f: fields) {
			try {
				if (f.getInt(pi.getSampleModel().createDataBuffer())==dataType){
					System.out.println("Data type is: "+f.getName());
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
			
		System.out.println("Height = "+pi.getHeight()+" lines");
		System.out.println("Width = "+pi.getWidth()+" pixels");
		System.out.println("Bands = "+pi.getNumBands());
		// get stats from the entire image
		ParameterBlock pb = new ParameterBlock();
	    pb.addSource(pi);   // The source image
	    pb.add(null); // ROI
	   	pb.add(1); // horizontal sampling rate
	   	pb.add(1); // vertical sampling rate
	   	
	    // Perform the extrema operation on the source image
	    RenderedOp extrem = JAI.create("extrema", pb);
	    double[][] extrema = (double[][]) extrem.getProperty("extrema");
		RenderedOp mean = JAI.create("mean", pb);
		double[] meanA = (double[]) mean.getProperty("mean");
		
		for (int b=0; b<pi.getNumBands(); b++) {
			System.out.println("band, min, max, mean");
			System.out.println(String.format("%d, %7.2f, %7.2f, %7.2f", b, extrema[0][b],extrema[1][b],meanA[b]));
		}
	}
	
	/**
	 * Write a JPEG at the highest quality.
	 * @param image
	 * @param filename
	 */
	public static void writeJPEG(PlanarImage image, String filename) {
		IIOImage outputImage = new IIOImage(image, null, null);
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();        
        try {
			writer.setOutput(new FileImageOutputStream(new File(filename)));
			ImageWriteParam writeParam = writer.getDefaultWriteParam();
	        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	        writeParam.setCompressionQuality(1.0f); // float between 0 and 1, 1 for max quality.
	        writer.write( null, outputImage, writeParam);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param filename
	 * @param dataType
	 * @param w
	 * @param h
	 * @param bands
	 * @param scanlineStride
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static PlanarImage readRaw(String filename, int dataType, int w, int h, int bands, int scanlineStride) throws FileNotFoundException, IOException {
		SampleModel model = new BandedSampleModel(dataType, w, h, scanlineStride, new int[] {0}, new int[] {0});
		ImageInputStream source = new FileImageInputStream(new File(filename));
		ImageInputStream stream = new RawImageInputStream(source, model, new long[] {0}, new Dimension[] {new Dimension(w, h)});
		ImageReader reader = ImageIO.getImageReaders(stream).next();
		reader.setInput(stream);
		BufferedImage bimage = reader.read(0);
		return PlanarImage.wrapRenderedImage(bimage);
	}
	
	
	/**
	 * Test code and procesing log.
	 * @param args
	 */
	public static void main(String[] args) {
		// test of the illumination
		//System.out.println(Calendar.getInstance().getTime());
		// 2003 line 2
		//PlanarImage ill = illumination(30.2, 111.6, 126.33, 1000, 716);
		//String testFile = "C:/Documents and Settings/nick/My Documents/testIll.tif";
		//JAI.create("filestore", ill, testFile, "TIFF");
		
		//System.out.println(Calendar.getInstance().getTime());
		
		// not working
		//String iFile = "C:/Documents and Settings/nick/My Documents/Costa_Rica_2009/imagery3/sierpe_subset_20090604.tif";
		//PlanarImage pi = readMultiBandTiff(iFile, 0);
		//imageStats(pi);
		
		//String image = "E:/atm04-920/MASL1B_04920_05_20040409_043522_043804_V01_b1_geo.tif";
		//describeGeoTiff(image);
		
		// testing large image reading 20110805
		// image input
//		String imageFile = "C:/Users/owner/Documents/MASTER_imagery/SF_high_res/MASTERL1B_0800510_06_20080826_2154_2157_V02_utm10_wgs84_2m_subset_";
//		//String imageFile = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_09_20110630_2334_2341_V01_tir";
//		//String imageFile = "C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/MASTERL1B_1165100_04_20110608_2305_2310_V00_vis_swir";
//		ImageReader reader;
//		try {
//			// Read the input
//			reader = new ENVIHdrImageReaderSpi().createReaderInstance();
//			final ParameterBlockJAI pbjImageRead;
//			pbjImageRead = new ParameterBlockJAI("ImageRead");
//			pbjImageRead.setParameter("Input", new File(imageFile));
//			pbjImageRead.setParameter("reader", reader);
//			PlanarImage image = JAI.create("ImageRead", pbjImageRead);
//			RandomIter inputIter = RandomIterFactory.create(image, null);
//			image.getHeight();
//			image.getWidth();
//			image.getNumBands();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		//String waterName = "C:/Users/Nicholas/Documents/GlobalLandCover/modis/2009_igbp_wgs84.tif";
//		String file = "D:/MOD13A2/2010/2010.01.01/EVI/2010.01.01_EVI_mosaic_geo.1_km_16_days_EVI.tif";
//		describeGeoTiff(file);
		
		// GEE lecture present:
//		String b1 = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/beijing_airport_pan_sharpened/LE71230322012234EDC00/LE71230322012234EDC00.10.tif";
//		String b2 = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/beijing_airport_pan_sharpened/LE71230322012234EDC00/LE71230322012234EDC00.20.tif";
//		String b3 = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/beijing_airport_pan_sharpened/LE71230322012234EDC00/LE71230322012234EDC00.30.tif";
//		String out = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/beijing_airport_pan_sharpened/LE71230322012234EDC00/LE71230322012234EDC00_rgb.jpg";
//		PlanarImage rgb = makeRGB(readImage(b3), readImage(b2), readImage(b1));
//		PlanarImage stretched = gaussianStretch(rgb);
//		writeJPEG(stretched, out);
//		
//		
//		b1 = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/beijing_airport_pan_sharpened/979dcee377ee8278f834a9d0c6ac07bc/979dcee377ee8278f834a9d0c6ac07bc.blue.tif";
//		b2 = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/beijing_airport_pan_sharpened/979dcee377ee8278f834a9d0c6ac07bc/979dcee377ee8278f834a9d0c6ac07bc.green.tif";
//		b3 = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/beijing_airport_pan_sharpened/979dcee377ee8278f834a9d0c6ac07bc/979dcee377ee8278f834a9d0c6ac07bc.red.tif";
//		out = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/lecture_images/beijing_airport_pan_sharpened/979dcee377ee8278f834a9d0c6ac07bc/979dcee377ee8278f834a9d0c6ac07bc_rgb.jpg";
//
//		rgb = makeRGB(readImage(b3), readImage(b2), readImage(b1));
//		ParameterBlockJAI pbRescale = new ParameterBlockJAI("Rescale");
//	    pbRescale.addSource(rgb);
//	    pbRescale.setParameter("constants", new double[] {1.4, 1.4, 1.1});
//	    pbRescale.setParameter("offsets", new double[] {0,0,0});
//	    PlanarImage rescaled = (PlanarImage)JAI.create("Rescale", pbRescale, null);
//		ParameterBlockJAI format = new ParameterBlockJAI("Format");
//		format.addSource(rescaled);
//		format.setParameter("dataType", DataBuffer.TYPE_BYTE);
//	    PlanarImage formatted = (PlanarImage)JAI.create("Format", format, null);
//		stretched = gaussianStretch(formatted);
//		writeJPEG(formatted, out);
        
		
	}


}
