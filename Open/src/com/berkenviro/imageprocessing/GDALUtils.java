/*
 *  Copyright (C) 2011, 2012  Nicholas Clinton
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

import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.Utils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;

/**
 * @author Nicholas Clinton
 * 20131007. 
 *
 */
public class GDALUtils {

	/**
	 * Initialize GDAL.
	 */
	static {
		System.out.println("GDAL init...");
		gdal.AllRegister();
	}
	
	/**
	 * Lookup items (which might be wavelengths) in the GDAL metadata dictionary.
	 * This was developed for a particular ENVI file format.  May or may not work with other datasets.
	 * @param fileName is the name of the input file.
	 * @return the metadata values as a double[]
	 */
	public static double[] getWavelengths(String fileName) {
		Dataset poDataset = getDataset(fileName);
		return getWavelengths(poDataset);
	}
	
	/**
	 * Lookup items (which might be wavelengths) in the GDAL metadata dictionary.
	 * This was developed for a particular ENVI file format.  May or may not work with other datasets.
	 * @param poDataset
	 * @return the metadata values as a double[]
	 */
	public static double[] getWavelengths(Dataset poDataset) {
		double[] wavelengths = new double[poDataset.getRasterCount()];
		Hashtable dict = poDataset.GetMetadata_Dict("");
		Enumeration keys = dict.keys();
		//System.out.println(dict.size() + " items of metadata found (via Hashtable dict):");
		while(keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			//System.out.println(" :" + key + ":==:" + dict.get(key) + ":");
			if (key.startsWith("Band_")) {
				String[] band = key.split("_");
				int index = Integer.parseInt(band[1]) - 1;
				wavelengths[index] = Double.parseDouble((String)dict.get(key));
				//System.out.println(" band " + index + " wavelength: " + wavelengths[index]);
			}
		}
		return wavelengths;
	}
	
	/**
	 * Use getAttribute("wavelength") instead.  AVIRIS has a weird metadata key structure.
	 * @param fileName is the AVIRIS filename
	 * @return
	 */
	@Deprecated
	public static double[] getAVIRISWavelengths(String fileName) {
		
		File f = new File(fileName);
		Dataset poDataset = (Dataset) gdal.Open(f.getAbsolutePath(), gdalconst.GA_ReadOnly);
		double[] wavelengths = new double[poDataset.getRasterCount()];
		Hashtable dict = poDataset.GetMetadata_Dict("");
		Enumeration keys = dict.keys();
		//System.out.println(dict.size() + " items of metadata found (via Hashtable dict):");
		while(keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			System.out.println(" :" + key + ":==:" + dict.get(key) + ":");
			if (key.startsWith("Band_")) {
				String[] band = key.split("_");
				int index = Integer.parseInt(band[1]) - 1;
				String[] toks = ((String)dict.get(key)).split("\\(");
				String string = toks[toks.length-1];
				String wave = string.substring(0, (string.length())-2);
				wavelengths[index] = Double.parseDouble(wave);
				//System.out.println(" band " + index + " wavelength: " + wavelengths[index]);
			}
		}
		return wavelengths;
	}
	
	/**
	 * Read an ENVI header file to retrieve the FWHMs.  Return them as a double[].
	 * GDAL is only used to get the number of bands.
	 * @param fileName is the name of the ENVI image file.
	 * @return
	 */
	public static double[] getENVIfwhm(String fileName) {
		return getAttribute(fileName, "fwhm");
	}
	
	
	/**
	 * Read an ENVI header file to retrieve a metadata value with key = attribute.
	 * GDAL is only used to get the number of bands.
	 * @param fileName is the name of the ENVI image file.
	 * @param attribute is the key for the attribute as a string.
	 * @return
	 */
	public static double[] getAttribute(String fileName, String attribute) {
		Dataset poDataset = getDataset(fileName);
		double[] array = new double[poDataset.getRasterCount()];
		String hdrName = fileName+".hdr";
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(hdrName)));
			// read each data line to this list
			ArrayList data = new ArrayList();
			// on/off switch
			boolean record = false;
			int thisrun = 1;
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains(attribute)) { // begin
					record = true; 
					continue;
				}
				if (record) {
					if (line.contains("}")) { // end
						line = line.replace("}", "");
						record = false;
					}
					String[] toks = com.berkenviro.imageprocessing.Utils.tokenize(line);
					for (String s : toks) {
						data.add(s.trim());
					}
				}
			}
			for (int i=0; i<data.size(); i++) {
				array[i] = Double.parseDouble((String)data.get(i));
				//System.out.println("i, fwhm: "+i+", "+array[i]);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return array;
	}
	
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public static Dataset getDataset(String fileName) {
		File f = new File(fileName);
		Dataset data = (Dataset) gdal.Open(f.getAbsolutePath(), gdalconst.GA_ReadOnly);
		return data;
	}
	
	/**
	 * AffineTransformation:
	 * 	|x'| = 	| m00 m01 m02 |*|x|
     	|y'|	| m10 m11 m12 | |y|
     	|1 |	|  0   0   1  | |1|
     		
     * GDAL:
     * 	Xp = padfTransform[0] + P*padfTransform[1] + L*padfTransform[2];
   		Yp = padfTransform[3] + P*padfTransform[4] + L*padfTransform[5];
   		
	 * @param data
	 * @return
	 */
	public static AffineTransformation raster2proj(Dataset data) {	
		double[] padfTransform = data.GetGeoTransform();
		double m02 = padfTransform[0];
		double m00 = padfTransform[1];
		double m01 = padfTransform[2];
		double m12 = padfTransform[3];
		double m10 = padfTransform[4];
		double m11 = padfTransform[5];
		return new AffineTransformation(m00, m01, m02, m10, m11, m12);
	}
	
	/**
	 * 
	 * @param source
	 * @param dest
	 */
	public static void transferGeo(Dataset source, PlanarImage dest) {
		double[] padfTransform = source.GetGeoTransform();
		dest.setProperty("ulX", new Double(padfTransform[0]));
		dest.setProperty("ulY", new Double(padfTransform[3]));
		dest.setProperty("deltaX", new Double(padfTransform[1]));
		dest.setProperty("deltaY", new Double(padfTransform[5]));
	}
	
	/**
	 * This method takes a double[] [x,y] in the coordinate space of the 
	 * projected image and the reference image and returns the [x,y]
	 * indices of the pixel containing the projected point.
	 * 
	 * @param ImageProjXY is the projected coordinate array {x,y}
	 * @param pImage is the PlanarImage to search
	 * @return an int[] of pixel {x,y} ZERO INDEXED
	 */
	public static int[] getPixelXY(double[] ImageProjXY, Dataset data) throws Exception {
		AffineTransformation at = raster2proj(data);
		AffineTransformation inv = com.berkenviro.gis.GISUtils.proj2raster(at);
		Coordinate pix = new Coordinate();
		inv.transform(new Coordinate(ImageProjXY[0], ImageProjXY[1]), pix);
		if ((int)pix.x < 0 || (int)pix.x >= data.getRasterXSize() 
		 || (int)pix.y < 0 || (int)pix.y >= data.getRasterYSize()) {
			throw new Exception("Impossible coordinates: "+pix);
		}
		return new int[] {(int)(pix.x), (int)(pix.y)};
	}

	/**
	 * This method takes an int[] [x,y] ZERO INDEXED of the pixel coordinates and returns the 
	 * center of the pixel in projected coordinates.
	 * 
	 * @param pixelXY is the pixel indices
	 * @param pImage is the image to use
	 * @return a double[] {x,y} projected coordinates of the pixel centroid
	 */
	public static double[] getProjectedXY(int[] pixelXY, Dataset data) throws Exception {
		Coordinate pix = new Coordinate(pixelXY[0]+0.5, pixelXY[1]+0.5);
		if (pix.x<0 | pix.y<0 | pix.x>data.getRasterXSize() | pix.y>data.getRasterYSize()) {
			throw new Exception("Outside image bounds: "+pix);
		}
		AffineTransformation at = raster2proj(data);
		// projected pixel, empty for now
		Coordinate projPix = new Coordinate();
		at.transform(pix, projPix);
		return new double[] {projPix.x, projPix.y};
	}
	
	/**
	 * This method is for overlaying a georeferenced point on a referenced image.  
	 * The coordinate system of the Dataset and Point must match.
	 * 
	 * @param data is the geographic Dataset
	 * @param pt is a Point with georeferenced coordinates
	 * @param band is the one-indexed band to sample
	 * @return
	 */
	public static double imageValue(Dataset data, Point pt, int b) throws Exception {
		return imageValue(data, pt.getX(), pt.getY(), b);
	}
	
	/**
	 * 
	 * @param data
	 * @param x is georeferenced
	 * @param y is georeferenced
	 * @param b is one-indexed
	 * @return
	 * @throws Exception
	 */
	public static double imageValue(Dataset data, double x, double y, int b) throws Exception {
		int[] pixelXY = getPixelXY(new double[] {x, y}, data);
		return pixelValue(data, pixelXY[0], pixelXY[1], b);
	}

	/**
	 * Return image value from pixel coordinates.
	 * @param data is the Dataset
	 * @param x is pixel
	 * @param y is line
	 * @param b is one-indexed band
	 * @return
	 */
	public static double pixelValue(Dataset data, int x, int y, int b) {
		Band band = data.GetRasterBand(b);
		int buf_type = band.getDataType();
		int buf_size = gdal.GetDataTypeSize(buf_type) / 8;
		ByteBuffer pixel = ByteBuffer.allocateDirect(buf_size);
		//ByteBuffer pixel = ByteBuffer.allocate(buf_size);
		pixel.order(ByteOrder.nativeOrder());
		// offset by pixel-1 to start reading at pixel
		band.ReadRaster_Direct(x, y, 1, 1, 1, 1, buf_type, pixel); 
		//band.ReadRaster(x, y, 1, 1, 1, 1, buf_type, pixel.array());

		if (buf_type == gdalconstConstants.GDT_Byte) {
			return ((int)pixel.get()) & 0xff;
		} else if(buf_type == gdalconstConstants.GDT_Int16) {
			return pixel.getShort();
		} else if(buf_type == gdalconstConstants.GDT_Int32) {
			return pixel.getInt();
		} else if(buf_type == gdalconstConstants.GDT_Float32) {
			return pixel.getFloat();
		} else if(buf_type == gdalconstConstants.GDT_Float64) {
			return pixel.getDouble();
		} else if(buf_type == gdalconstConstants.GDT_UInt16) {
			return pixel.getChar(); // ?????????????
		}
		
		return Double.NaN;
	}
	
	/**
	 * Read a line of data
	 * @param data
	 * @param x
	 * @param y
	 * @param b
	 * @param length
	 * @return
	 */
	public static double[] xPixelsValue(Dataset data, int x, int y, int b, int length) {
		Band band = data.GetRasterBand(b);
		int buf_type = band.getDataType();
		int buf_size = length * gdal.GetDataTypeSize(buf_type) / 8;
		//ByteBuffer pixel = ByteBuffer.allocateDirect(buf_size);
		ByteBuffer pixels = ByteBuffer.allocate(buf_size);
		pixels.order(ByteOrder.nativeOrder());
		// offset by pixel-1 to start reading at pixel
		//band.ReadRaster_Direct(x, y, 1, 1, 1, 1, buf_type, pixel); 
		band.ReadRaster(x, y, length, 1, length, 1, buf_type, pixels.array());
		 double[] ret = new double[length];
		 for (int r=0; r<ret.length; r++) {
			 if (buf_type == gdalconstConstants.GDT_Byte) {
					ret[r] = ((int)pixels.get()) & 0xff;
				} else if(buf_type == gdalconstConstants.GDT_Int16) {
					ret[r] = pixels.getShort();
				} else if(buf_type == gdalconstConstants.GDT_Int32) {
					ret[r] = pixels.getInt();
				} else if(buf_type == gdalconstConstants.GDT_Float32) {
					ret[r] = pixels.getFloat();
				} else if(buf_type == gdalconstConstants.GDT_Float64) {
					ret[r] = pixels.getDouble();
				} else if(buf_type == gdalconstConstants.GDT_UInt16) {
					ret[r] = pixels.getChar(); // ?????????????
				}
		 }
		return ret;
	}
	
	/**
	 * Get a pixel vector (spectrum).
	 * @param data
	 * @param x
	 * @param y
	 * @return
	 */
	public static double[] pixelValues(Dataset data, int x, int y) {
		double[] values = new double[data.getRasterCount()];
		for (int b=0; b<values.length; b++) {
			values[b] = pixelValue(data, x, y, b+1);
		}
		return values;
	}
	
	/**
	 * 
	 * @param poDataset
	 * @param band is one-indexed
	 * @return
	 */
	public static BufferedImage getBufferedImage(Dataset poDataset, int band) { 
		int xsize = poDataset.getRasterXSize();
		int ysize = poDataset.getRasterYSize();
		int pixels = xsize * ysize;
		Band poBand = poDataset.GetRasterBand(band);
		int buf_type = poBand.getDataType();
		int buf_size = pixels * gdal.GetDataTypeSize(buf_type) / 8;
		// image data
		ByteBuffer data = ByteBuffer.allocateDirect(buf_size);
		data.order(ByteOrder.nativeOrder());
		// read into the ByteBuffer
		poBand.ReadRaster_Direct(0, 0, poBand.getXSize(), 
				poBand.getYSize(), xsize, ysize,
				buf_type, data);
		DataBuffer buff = null;
		int data_type;
		if(buf_type == gdalconstConstants.GDT_Byte) {  // warning on signed bytes
			data_type = DataBuffer.TYPE_BYTE;
			byte[] bytes = new byte[pixels];
			data.get(bytes);
			buff = new DataBufferByte(bytes, pixels);
		} else if(buf_type == gdalconstConstants.GDT_UInt16) { // warning unsigned
			data_type = DataBuffer.TYPE_USHORT;
			short[] shorts = new short[pixels];
			data.asShortBuffer().get(shorts);
			buff = new DataBufferUShort(shorts, pixels);
		} else if(buf_type == gdalconstConstants.GDT_Int16) {
			data_type = DataBuffer.TYPE_SHORT;
			short[] shorts = new short[pixels];
			data.asShortBuffer().get(shorts);
			buff = new DataBufferShort(shorts, pixels);
		} else if(buf_type == gdalconstConstants.GDT_Int32) {
			data_type = DataBuffer.TYPE_INT;
			int[] ints = new int[pixels];
			data.asIntBuffer().get(ints);
			buff = new DataBufferInt(ints, pixels);
		} else if(buf_type == gdalconstConstants.GDT_Float32) {
			data_type = DataBuffer.TYPE_FLOAT;
			float[] floats = new float[pixels];
			data.asFloatBuffer().get(floats);
			buff = new DataBufferFloat(floats, pixels);
		} else if(buf_type == gdalconstConstants.GDT_Float64) {
			data_type = DataBuffer.TYPE_DOUBLE;
			double[] doubles = new double[pixels];
			data.asDoubleBuffer().get(doubles);
			buff = new DataBufferDouble(doubles, pixels);
		} else {
			data_type = DataBuffer.TYPE_UNDEFINED;
		}
		SampleModel sModel = new BandedSampleModel(data_type, xsize, ysize, 1);
		ColorModel cModel = new ComponentColorModel(
										ColorSpace.getInstance(ColorSpace.CS_GRAY), 
										false, 
										false, 
										ColorModel.OPAQUE, 
										data_type);
		WritableRaster raster = Raster.createWritableRaster(sModel, buff, new java.awt.Point(0,0)); 
		
		return new BufferedImage(cModel, raster, false, null);
	}
	
	/**
	 * 
	 * @param from
	 * @param to
	 */
	public static void transferGeo(String fromName, String toName) {
		Dataset dataset = getDataset(fromName);
		double[] geo = dataset.GetGeoTransform();
		System.out.println("Using geo transform: ");
		for (int i=0; i<geo.length; i++) {
			System.out.print(geo[i]+", ");
		}
		String proj = dataset.GetProjectionRef();
		System.out.println("Using projection: ");
		System.out.println(proj);
		File f = new File(toName);
		Dataset data = (Dataset) gdal.Open(f.getAbsolutePath(), gdalconst.GA_Update);
		data.SetGeoTransform(geo);
		data.SetProjection(proj);
		data.FlushCache();
		data.delete();
	}
	
	/**
	 * 
	 * @param name
	 */
	public static void describeGeo(String name) {
		Dataset dataset = getDataset(name);
		double[] geo = dataset.GetGeoTransform();
		System.out.println("Geo transform: ");
		for (int i=0; i<geo.length; i++) {
			System.out.print(geo[i]+", ");
		}
		String proj = dataset.GetProjectionRef();
		System.out.println("Projection: ");
		System.out.println(proj);
	}
	
	/**
	 * NOTE: OSX throws:
	 * "ERROR 6: Unable to load PROJ.4 library (libproj.dylib), 
	 * creation of OGRCoordinateTransformation failed."
	 * 
	 * @param referenceName
	 */
	public static void writeLatLongImages(String referenceName) {
		Dataset ref = GDALUtils.getDataset(referenceName);
		SpatialReference src = new SpatialReference();
		src.ImportFromWkt(ref.GetProjectionRef());
		SpatialReference dst = new SpatialReference();
		dst.SetWellKnownGeogCS("WGS84");
		CoordinateTransformation ct = new CoordinateTransformation(src, dst);
		WritableRaster xOut = RasterFactory.createBandedRaster(
				DataBuffer.TYPE_FLOAT,
				ref.getRasterXSize(),
				ref.getRasterYSize(),
				1,
				new java.awt.Point(0,0));
		WritableRaster yOut = RasterFactory.createBandedRaster(
				DataBuffer.TYPE_FLOAT,
				ref.getRasterXSize(),
				ref.getRasterYSize(),
				1,
				new java.awt.Point(0,0));
		
		for (int y=0; y<ref.getRasterYSize(); y++) {
			for (int x=0; x<ref.getRasterXSize(); x++) {
				try {
					double[] projXY = GDALUtils.getProjectedXY(new int[] {x, y}, ref);
					//System.out.println(Arrays.toString(projXY));
					projXY = ct.TransformPoint(projXY[0], projXY[1]);
					//System.out.println(Arrays.toString(projXY));
					xOut.setSample(x, y, 0, projXY[0]);
					yOut.setSample(x, y, 0, projXY[1]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		JAIUtils.writeTiff(xOut, referenceName.replace(".tif", "_long.tif"));
		JAIUtils.writeTiff(yOut, referenceName.replace(".tif", "_lat.tif"));
	}
	
	/**
	 * Test code and processing log.
	 * @param args
	 */
	public static void main(String[] args) {
		//String hFile = "H:/headwall/CIRPAS_2010/11-783-02_2010_10_11_19_30_39_9_rad";
//		String aFile = "C:/Users/owner/Documents/ASTL/Ivanpah_2011/Imagery/f110609t01p00r06rdn_a/f110609t01p00r06rdn_a_sc01_ort_img";
//		double[] waves = getENVIfwhm(aFile);
		
		// 20121026 test code
//		try {
//			String file = "D:/MOD13A2/2010/2010.01.01/EVI/2010.01.01_EVI_mosaic_geo.1_km_16_days_EVI.tif";
//			PlanarImage pImage = JAIUtils.readImage(file);
//			System.out.println("UL:");
//			double[] projXY = JAIUtils.getProjectedXY(new int[] {0,0}, pImage);
//			System.out.println("\t x,y = "+projXY[0]+","+projXY[1]);
//			System.out.println("\t data = "+JAIUtils.pixelValue(pImage, 0, 0, 0));
//			System.out.println("LR:");
//			projXY = JAIUtils.getProjectedXY(new int[] {pImage.getWidth()-1, pImage.getHeight()-1}, pImage);
//			System.out.println("\t x,y = "+projXY[0]+","+projXY[1]);
//			System.out.println("\t data = "+JAIUtils.pixelValue(pImage, pImage.getWidth()-1, pImage.getHeight()-1, 0));
//			System.out.println("arbitrary land:");
//			projXY = JAIUtils.getProjectedXY(new int[] {23870, 6388}, pImage);
//			System.out.println("\t x,y = "+projXY[0]+","+projXY[1]);
//			System.out.println("\t data = "+JAIUtils.pixelValue(pImage, 23870, 6388, 0));
//			System.out.println("\t long,lat = -121.0, 38.0");
//			System.out.println("\t data = "+JAIUtils.imageValue(GISUtils.makePoint(-121.0, 38.0), pImage));
//			
//			Dataset data = GDALUtils.getDataset(file);
//			System.out.println("UL:");
//			projXY = GDALUtils.getProjectedXY(new int[] {0,0}, data);
//			System.out.println("\t x,y = "+projXY[0]+","+projXY[1]);
//			System.out.println("\t data = "+GDALUtils.pixelValue(data, 0, 0, 1));
//			System.out.println("LR:");
//			projXY = GDALUtils.getProjectedXY(new int[] {data.getRasterXSize()-1, data.getRasterYSize()-1}, data);
//			System.out.println("\t x,y = "+projXY[0]+","+projXY[1]);
//			System.out.println("\t data = "+GDALUtils.pixelValue(data, data.getRasterXSize()-1, data.getRasterYSize()-1, 1));
//			System.out.println("arbitrary land:");
//			projXY = GDALUtils.getProjectedXY(new int[] {23870, 6388}, data);
//			System.out.println("\t x,y = "+projXY[0]+","+projXY[1]);
//			System.out.println("\t data = "+GDALUtils.pixelValue(data, 23870, 6388, 1));
//			System.out.println("\t long,lat = -121.0, 38.0");
//			System.out.println("\t data = "+GDALUtils.imageValue(data, GISUtils.makePoint(-121.0, 38.0), 1));
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		System.out.println(
//				(getDataset("D:/MOD13A2/2010/2010.01.01/EVI/2010.01.01_EVI_mosaic_geo.1_km_16_days_EVI.tif")).GetProjection());
		//System.out.println(gdal.GetCacheMax());
		
		//Dataset d = getDataset("I:/MOD13A2/2010/2010.01.01/EVI/2010.01.01_EVI_mosaic_geo.1_km_16_days_EVI.tif");
		//d.delete();

		// 20130202
//		String filename = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/Delta_Hymap_12.img";
//		Dataset poDataset = getDataset(filename);
//		BufferedImage buff = getBufferedImage(poDataset, 1);
		
//		String filename = "/Users/nclinton/Documents/GEE/region_merging/test/m37122h2sw_subset.tif";
//		for (int b=0; b<4; b++) {
//			String outfile = filename.replace(".tif", "_b"+b+".tif");
//			transferGeo(filename, outfile);
//		}
		
		// 20130522
		//String filename = "/Users/nclinton/Documents/GEE/region_merging/test/m37122h2sw_subset.tif";
		//String filename = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4-J48.tif";
		//String outfile = "/Users/nclinton/Documents/GEE/region_merging/test/test4_GEE/m37122h2sw_subset_10_05_05_rmf2.tiff";
		//String outfile = "/Users/nclinton/Documents/GEE/region_merging/test/test4_GEE/m37122h2sw_subset_10_05_05_rmf20130522.tiff";
		//String outfile = "/Users/nclinton/Documents/GEE/region_merging/test/test4_GEE/m37122h2sw_subset_10_05_05_rmf2_1.tiff";
		//String outfile = "/Users/nclinton/Documents/GEE/region_merging/test/m37122h2sw_subset_10_05_05_rmf20130522.tiff";
		//String outfile = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4_x.tif";
		//transferGeo(filename, outfile);
		//outfile = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4_y.tif";
		//transferGeo(filename, outfile);
	
		// 20130529
		//String img = "/Users/nclinton/Documents/data/test3/Meta/L5-TM-118-032-20091005-L4-J48.tif"; // doesn't work on mac
		//String img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4-J48.tif";
		//writeLatLongImages(img);
//		String img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_J48.tif";
//		writeLatLongImages(img);
//		img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_J48.tif";
//		writeLatLongImages(img);
//		img = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_J48.tif";
//		writeLatLongImages(img);
		
		
		//String filename = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4-J48.tif";
		//String outfile = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4_lat.tif";
		//String outfile = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4_long.tif";
		//String outfile = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4_meta_sync.tif";
		//String outfile = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5-TM-118-032-20091005-L4_meta_label_sync.tif";
		//transferGeo(filename, outfile);
		
//		String filename = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_J48.tif";
//		transferGeo(filename, "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_lat.tif");
//		transferGeo(filename, "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5042034_03420090829_Rad_Ref_TRC_BYTE_long.tif");
//		filename = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_J48.tif";
//		transferGeo(filename, "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_lat.tif");
//		transferGeo(filename, "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5176039_03920030707_Rad_Ref_TRC_BYTE_long.tif");
//		filename = "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_J48.tif";
//		transferGeo(filename, "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_lat.tif");
//		transferGeo(filename, "C:/Users/Nicholas/Documents/GlobalLandCover/test3/Meta/L5199033_03320100711_Rad_ref_TRC_BYTE_long.tif");
		
		// 20130619
//		String filename = "/Users/nclinton/Documents/GEE/region_merging/test/m37122h2sw_subset.tif";
//		String outfile = "/Users/nclinton/Documents/GEE/region_merging/test/test5_GEE/m37122h2sw_subset_10_05_05_rmf20130625.tiff";
		//String filename = "/Users/nclinton/Documents/MASTER_imagery/MASTERL1B_0800510_06_20080826_2154_2157_V02_b0.tif";
		//String outfile = "/Users/nclinton/Documents/GEE/region_merging/test/MASTERL1B_0800510_06_20080826_2154_2157_V02_30_05_05_20130625.tiff";
		//transferGeo(filename, outfile);
		
		// 20130916
//		String filename = "/Users/nclinton/Documents/GEE/region_merging/test/m37122h2sw_subset.tif";
//		Dataset data = getDataset(filename);
//		for (int y=0; y<10; y++) {
//			double[]xs = new double[10];
//			for (int x=0; x<10; x++) {
//				xs[x] = pixelValue(data,x,y,1);
//			}
//			System.out.println(Arrays.toString(xs));
//			System.out.println(Arrays.toString(xPixelsValue(data,0,y,1,10)));
//			System.out.println();
//		}
		
//		String monthFile = "/Users/nclinton/Documents/"
//				+ "GlobalPhenology/climate_anomalies/precip/NCCCSM_1PTO2X_1_pr-change_o0001-0030/"
//				+ "NCCCSM_1PTO2X_1_pr-change_o0001-0030_01.tif";
//		describeGeo(monthFile);
		
		// 20140321
		String fromFile = "/data/GlobalLandCover/modis/land_mask.tif";
//		String toFile = "/home/nclinton/Documents/evi_persiann_us_20140319_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140319_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140319_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140319_p.tif";
//		transferGeo(fromFile, toFile);
//		String toFile = "/home/nclinton/Documents/evi_persiann_us_20140321_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140321_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140321_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140321_p.tif";
//		transferGeo(fromFile, toFile);
//		String toFile = "/home/nclinton/Documents/evi_persiann_us_20140322_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140322_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140322_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140322_p.tif";
//		transferGeo(fromFile, toFile);
//		String toFile = "/home/nclinton/Documents/evi_persiann_us_20140323_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140323_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140323_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140323_p.tif";
//		transferGeo(fromFile, toFile);
//		String toFile = "/home/nclinton/Documents/evi_persiann_20140324_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_20140324_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_20140324_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_20140324_p.tif";
//		transferGeo(fromFile, toFile);
//		String toFile = "/home/nclinton/Documents/evi_temp_20140401_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temp_20140401_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temp_20140401_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temp_20140401_p.tif";
//		transferGeo(fromFile, toFile);
//		String toFile = "/home/nclinton/Documents/evi_temp_20140407_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temp_20140407_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temp_20140407_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temp_20140407_p.tif";
//		transferGeo(fromFile, toFile);
		
//		String toFile = "/home/nclinton/Documents/evi_persiann_sum_us_20140420_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_sum_us_20140420_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_sum_us_20140420_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_sum_us_20140420_sum.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_sum_us_20140420_p.tif";
//		transferGeo(fromFile, toFile);
		
		
//		String toFile = "/home/nclinton/Documents/evi_persiann_us_20140509_merge.tif";
//		//transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140509_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140509_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_us_20140509_p.tif";
//		transferGeo(fromFile, toFile);
		
//		String toFile = "/home/nclinton/Documents/evi_persiann_sum_20140515_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_sum_20140515_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_sum_20140515_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_sum_20140515_p.tif";
//		transferGeo(fromFile, toFile);
		
//		String toFile = "/home/nclinton/Documents/evi_temperature_sum_20140520_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140520_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140520_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140520_p.tif";
//		transferGeo(fromFile, toFile);
		
//		String toFile = "/home/nclinton/Documents/evi_temperature_sum_20140522_merge.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140522_corr.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140522_days.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140522_p.tif";
//		transferGeo(fromFile, toFile);
		
//		String toFile = "/home/nclinton/Documents/evi_temperature_sum_20140522_sum.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140520_sum.tif";
//		transferGeo(fromFile, toFile);
//		toFile = "/home/nclinton/Documents/evi_persiann_sum_20140515_sum.tif";
//		transferGeo(fromFile, toFile);
		
		String toFile = "/home/nclinton/Documents/evi_temperature_sum_20140523_merge.tif";
		transferGeo(fromFile, toFile);
		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140523_corr.tif";
		transferGeo(fromFile, toFile);
		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140523_days.tif";
		transferGeo(fromFile, toFile);
		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140523_sum.tif";
		transferGeo(fromFile, toFile);
		toFile = "/home/nclinton/Documents/evi_temperature_sum_20140523_p.tif";
		transferGeo(fromFile, toFile);
	}

}
