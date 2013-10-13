package cn.edu.tsinghua.timeseries;

import java.util.List;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.junit.*;
import static org.junit.Assert.*;

import cn.edu.tsinghua.lidar.BitChecker;
import cn.edu.tsinghua.modis.BitCheck;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.vividsolutions.jts.geom.Point;

/**
 * @file ImageLoadr4Test.java
 * @brief It is a JUnit test class for class ImageLoadr4.
 *
 * @author Conghui He
 * @version 0.1
 * @date 2013-10-12
 */
public class ImageLoadr4Test {
	private final double EPSILON = 1;

	@Test
	/***
	 * test a single point, compare the result generated from 
	 * ImageLoadr4 and ImageLoadr2 
	 */
	public void testSmallCase() {
		String dir1 = "/home/nick/MOD13A2/2010";
		String dir2 = "/home/nick/MOD13A2/2011";
		try {
			ImageLoadr4 loadr4 = new ImageLoadr4(
					new String[] {dir2, dir1}, 
					"EVI", "VI_QC", 
					new BitCheck() {
						@Override
						public boolean isOK(int check) {
							return BitChecker.mod13ok(check);
						} 
					}
					);

			ImageLoadr2 loadr2 = new ImageLoadr2(new String[] {dir2, dir1});
			Point point = GISUtils.makePoint(-121.0, 38.0);
			
			List<double[]> expecteds = loadr2.getSeries(point.getX(), point.getY());
			List<double[]> actuals = loadr4.getSeries(point.getX(), point.getY());

			for (int i = 0; i < expecteds.size(); i++) {
				assertArrayEquals(expecteds.get(i), actuals.get(i), EPSILON);
			}
			loadr2.close();
			loadr4.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	/**
	 * a set of points defined in width_begin, width_end, height_begin,
	 * height_end, compared the result generated from ImageLoadr2 and ImageLoadr4
	 * 
	 * @throws Exception
	 */
	public void largeTest() throws Exception {
		// pixel size
		final double delta = 0.008365178679831331;
		// center of the upper left 
		final double ulx = -179.9815962788889;
		final double uly = 69.99592926598972;

		final int width_begin = 11000;
		final int width_end = 11010;
		final int height_begin = 4500;
		final int height_end = 4510;
		String dir1 = "/home/nick/MOD13A2/2010";
		String dir2 = "/home/nick/MOD13A2/2011";

		String reference = "/home/nick/workspace/CLINTON/lib/dfg/land_mask.tif";
		PlanarImage ref = JAIUtils.readImage(reference);
		RandomIter iter = RandomIterFactory.create(ref, null);

		ImageLoadr4 loadr4 = new ImageLoadr4(
				new String[] {dir2, dir1}, 
				"EVI", "VI_QC", 
				new BitCheck() {
					@Override
					public boolean isOK(int check) {
						return BitChecker.mod13ok(check);
					} 
				}
				);

		ImageLoadr2 loadr2 = new ImageLoadr2(new String[] {dir2, dir1});
				
		/**
		 * Please enumerate the pixel line by line 
		 * (that is, counting from y, and then x)
		 */
		for (int  y = height_begin; y < height_end; y++) {
			for (int x = width_begin; x < width_end; x++) {
				double _x = ulx + x * delta;
				double _y = uly - y * delta;
				if (Math.abs(_y) > 60.0) {
					continue; // outside bounds of PERSIANN
				}

				if (iter.getSample(x, y, 0) == 0) {
					continue; // not land
				}

				List<double[]> expecteds = loadr2.getSeries(_x, _y);
				List<double[]> actuals = loadr4.getSeries(_x, _y);
				
				for (int i = 0; i < actuals.size(); i++) {
					Assert.assertArrayEquals(expecteds.get(i), actuals.get(i), EPSILON);
				}
			}
		}
		
		loadr2.close();
		loadr4.close();
	}

}
