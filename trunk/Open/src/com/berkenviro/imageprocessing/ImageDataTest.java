package com.berkenviro.imageprocessing;

import java.awt.Point;

import org.gdal.gdal.Dataset;
import org.junit.*;

/**
 * @file ImageDataTest.java
 * @brief It is a JUnit test class for class ImageData.
 *
 * @author Conghui He
 * @version 0.1
 * @date 2013-10-12
 */
public class ImageDataTest {

	@Test
    /**
     * @brief test reading from tiff file, whose internal data type is byte
     *
     */
	public void testPixelValueForTypeByte() {
		String fileOfTypeByte = "/home/nick/images/Trp88.tiff";
		Point ul = new Point(0, 0);
		Point bd = new Point(499, 630);
		compareRegion(fileOfTypeByte, ul, bd);
	}
	
	@Test
    /**
     * @brief test reading from tiff file, whose internal data type is 
     * int16
     *
     * @return 
     */
	public void testPixelValueForTypeInt16() {
		String fileOfTypeInt16 = "/home/nick/MOD13A2/2010/2010.01.01/EVI/2010.01.01_EVI_mosaic_geo.1_km_16_days_EVI.tif";
		Point ul = new Point(1000, 1000);
		Point bd = new Point(2000, 2000);
		compareRegion(fileOfTypeInt16, ul, bd);
	}
	
	private void compareRegion(String filename, Point upper_left, Point bottom_right) {
		final int band_index = 1;
		final int image_width = (int)(bottom_right.getX() - upper_left.getX());
		final int image_height = (int)(bottom_right.getY() - upper_left.getY());
		final double epsilon = 1e-10;
		double [] expecteds = new double[image_width];
		double [] actuals = new double[image_width];
		
		Dataset gdalData = GDALUtils.getDataset(filename);
		ImageData imageData = new ImageData(filename, band_index);
		for (int y = 0; y < image_height; y++) {
			for (int x = 0; x < image_width; x++) {
				// the following two functions are to be tested
                
                // the |GDALUtils.pixelValue| method is authored by nick 
                // and it is considered as the correct version.
				expecteds[x] = GDALUtils.pixelValue(gdalData, x, y, band_index);

                // |imageData.pixelValue| is to be tested
				actuals[x] = imageData.pixelValue(x, y, band_index);
			}
			Assert.assertArrayEquals(expecteds, actuals, epsilon);
		}
	}
}
