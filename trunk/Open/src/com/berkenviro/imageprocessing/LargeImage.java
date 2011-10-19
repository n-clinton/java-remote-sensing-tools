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

import it.geosolutions.imageio.plugins.envihdr.ENVIHdrImageReaderSpi;
import it.geosolutions.imageio.plugins.geotiff.GeoTiffImageWriterSpi;

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

/**
 * Test class.  Large Image IO experiments.
 * @author Nicholas Clinton
 */
public class LargeImage {

	/**
	 * Copy from input to output.
	 * @param input
	 * @param output
	 */
	public static void copy2tiff(String input, String output) {
		Date start = Calendar.getInstance().getTime();
		
		// input
		final File inFile = new File(input); 
		ImageReader reader;
		// output
		WritableRaster out = null;
		
		try {
			// Read the input
			reader = new ENVIHdrImageReaderSpi().createReaderInstance();
			final ParameterBlockJAI pbjImageRead;
			pbjImageRead = new ParameterBlockJAI("ImageRead");
			pbjImageRead.setParameter("Input", inFile);
			pbjImageRead.setParameter("reader", reader);
			PlanarImage image = JAI.create("ImageRead", pbjImageRead);
			RandomIter inputIter = RandomIterFactory.create(image, null);
			
			// output
//			out = RasterFactory.createBandedRaster(
//					DataBuffer.TYPE_FLOAT,
//					image.getWidth(),
//					image.getHeight(),
//					image.getNumBands(),
//					new Point(0,0));
			
			
			for (int y=0; y<image.getHeight(); y++) { // each line
				System.out.println("Processing line: "+y);
				for (int b=0; b<image.getNumBands(); b++) { // each band
					for (int x=0; x<image.getWidth(); x++){	// each pixel
						//out.setSample(x,y,b, inputIter.getSampleFloat(x, y, b));
						System.out.println(inputIter.getSampleFloat(x, y, b));
					}
				}
			}
			
//			ColorModel orig = image.getColorModel();
//			System.out.println(orig);
//			BufferedImage outIm = new BufferedImage(orig, out, true, null);
//			ImageWriter writer = new GeoTiffImageWriterSpi().createWriterInstance();
//			final File file = new File(output);
//			final ParameterBlockJAI pbjImageWrite = new ParameterBlockJAI("ImageWrite");
//			pbjImageWrite.setParameter("Output", file);
//			pbjImageWrite.setParameter("writer", writer);
//			pbjImageWrite.addSource(outIm);
//			final RenderedOp op = JAI.create("ImageWrite", pbjImageWrite);
			// or
			//JAIUtils.writeFloatTiff(out, output);
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		// write finished
		//System.out.println("output: "+output);
		long elapsed = Calendar.getInstance().getTimeInMillis()-start.getTime();
		System.out.println(String.format("Processing time: %.1f seconds", elapsed/1000.0));
		Utils.time();
	}
	
	/**
	 * Test code and process log.
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		String input = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_09_20110630_2334_2341_V01_vis_nir";
//		String output = "C:/Users/owner/Documents/ASTL/SARP2011/30June2011b/MASTERL1B_1100306_09_20110630_2334_2341_V01_vis_nir_ref.tif";
		String input = "C:/Users/owner/Documents/MASTER_imagery/SF_high_res/MASTERL1B_0800510_06_20080826_2154_2157_V02_utm10_wgs84_2m";
		String output = "C:/Users/owner/Documents/MASTER_imagery/SF_high_res/MASTERL1B_0800510_06_20080826_2154_2157_V02_utm10_wgs84_2m_.tiff";
		copy2tiff(input, output);
		
	}

}
