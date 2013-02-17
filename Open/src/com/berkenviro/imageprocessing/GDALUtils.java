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
import java.util.Enumeration;
import java.util.Hashtable;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;

import com.berkenviro.gis.GISUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;

/**
 * @author owner
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
					String[] toks = Utils.tokenize(line);
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
		AffineTransformation inv = GISUtils.proj2raster(at);
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
		double[] xy = {pt.getX(), pt.getY()};
		int[] pixelXY = getPixelXY(xy, data);
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
		//ByteBuffer pixel = ByteBuffer.allocateDirect(buf_size);
		ByteBuffer pixel = ByteBuffer.allocate(buf_size);
		pixel.order(ByteOrder.nativeOrder());
		// offset by pixel-1 to start reading at pixel
		//band.ReadRaster_Direct(x, y, 1, 1, 1, 1, buf_type, pixel); 
		band.ReadRaster(x, y, 1, 1, 1, 1, buf_type, pixel.array());

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
	 * 
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
		String filename = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/Delta_Hymap_12.img";
		Dataset poDataset = getDataset(filename);
		BufferedImage buff = getBufferedImage(poDataset, 1);
	}

}
