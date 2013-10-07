package com.berkenviro.imageprocessing;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;

/**
 * 
 * @author Cong Hui He
 * 
 * 20131007. Added comments, formatted, made GDAL init static. nc.  Cong Hui TODO: document.
 */
public class ImageData {
	
	/**
	 * Initialize GDAL.
	 */
	static { gdal.AllRegister(); }
	
	private int _block_xsize;
	private int _buffer_size;
	private boolean _initialized = false;
	private Dataset _image;
	private int _band_index;
	private ByteBuffer _pixel_buffer;
	private boolean _buffer_empty = true;
	private int _data_type;
	private int _y_index = -1;
	private Band _band;
	
	/**
	 * 
	 * @param filename
	 * @param band_index
	 */
	public ImageData(String filename, int band_index) {
		if (!_initialized) {
			this.init(filename, band_index);
			_initialized = true;
		}
	}
	
	/**
	 * 
	 * @param filename
	 * @param band_index
	 */
	private void init(String filename, int band_index) {
		File image_file = new File(filename);
		_image = (Dataset) gdal.Open(image_file.getAbsolutePath(), gdalconst.GA_ReadOnly);
		config(band_index);
	}
	
	/**
	 * 
	 * @param band_index
	 */
	private void config(int band_index) {
		_band_index = band_index;
		_band = _image.GetRasterBand(_band_index);
		_block_xsize = _band.GetBlockXSize();
		_data_type = _band.getDataType();
		if (!_initialized) {
			_buffer_size = gdal.GetDataTypeSize(_data_type) / 8 * _block_xsize;
			_pixel_buffer = ByteBuffer.allocate(_buffer_size);
			_pixel_buffer.order(ByteOrder.nativeOrder());
		}
		_buffer_empty = true;
	}
	
	/**
	 * 
	 * @param data
	 * @param x
	 * @param y
	 * @param b
	 * @return
	 * @throws Exception
	 */
	public double imageValue(Dataset data, double x, double y, int b) throws Exception {
		
		int[] pixelXY = GDALUtils.getPixelXY(new double[] {x, y}, data); // constant time
		
		long start2 = System.nanoTime();
		double ret_value = pixelValueBuffered(pixelXY[0], pixelXY[1], b); // It is slow
		long stop2 = System.nanoTime();
		long elapsed2 = (stop2 - start2);
//		System.err.println(elapsed2);
		
		return ret_value;
	}
	
	/**
	 * Return image value from pixel coordinates.
	 * @param data is the Dataset
	 * @param x_index is pixel
	 * @param y_index is line
	 * @param band_index is one-indexed band
	 * @return
	 */
	public double pixelValue(int x_index, int y_index, int band_index) {
		int x_size = 1;
		// for each image file, there is a dataset.
		if (band_index != _band_index || y_index != _y_index) {
			// reset the buffer;
			config(band_index);
			_y_index = y_index;
		}
		
		_pixel_buffer = ByteBuffer.allocate(_buffer_size);
		_pixel_buffer.order(ByteOrder.nativeOrder());
		_band.ReadRaster(x_index, y_index,  
				x_size, 1, // x size; y size
				x_size, 1, // buffer x size; buffer y size
				_data_type, 
				_pixel_buffer.array()); 
		
		if (_data_type == gdalconstConstants.GDT_Byte) {
			return ((int)_pixel_buffer.get()) & 0xff;
		} else if(_data_type == gdalconstConstants.GDT_Int16) {
			return _pixel_buffer.getShort();
		} else if(_data_type == gdalconstConstants.GDT_Int32) {
			return _pixel_buffer.getInt();
		} else if(_data_type == gdalconstConstants.GDT_Float32) {
			return _pixel_buffer.getFloat();
		} else if(_data_type == gdalconstConstants.GDT_Float64) {
			return _pixel_buffer.getDouble();
		} else if(_data_type == gdalconstConstants.GDT_UInt16) {
			double ret = _pixel_buffer.getChar();
			return ret;
		}
		
		System.out.println("no no no ");
		return Double.NaN;
	}
	
	/**
	 * 
	 * @param x_index
	 * @param y_index
	 * @param band_index
	 * @return
	 */
	public double pixelValueBuffered(int x_index, int y_index, int band_index) {
		// for each image file, there is a dataset.
		if (band_index != _band_index || y_index != _y_index) {
			// reset the buffer;
			config(band_index);
			_y_index = y_index;
			
			_pixel_buffer = ByteBuffer.allocate(_buffer_size);
			_pixel_buffer.order(ByteOrder.nativeOrder());
//			System.err.println("Reading the image...");
			_band.ReadRaster(0, y_index,  
					_block_xsize, 1, // x size; y size
					_block_xsize, 1, // buffer x size; buffer y size
					_data_type, 
					_pixel_buffer.array()); 
		}
		
		int offset = (x_index * 2);
		
		if (_data_type == gdalconstConstants.GDT_Byte) {
			return ((int)_pixel_buffer.get()) & 0xff;
		} else if(_data_type == gdalconstConstants.GDT_Int16) {
			return _pixel_buffer.getShort(offset);
		} else if(_data_type == gdalconstConstants.GDT_Int32) {
			return _pixel_buffer.getInt();
		} else if(_data_type == gdalconstConstants.GDT_Float32) {
			return _pixel_buffer.getFloat();
		} else if(_data_type == gdalconstConstants.GDT_Float64) {
			return _pixel_buffer.getDouble();
		} else if(_data_type == gdalconstConstants.GDT_UInt16) {
			double ret = _pixel_buffer.getChar(offset);
			return ret;
		}
		
		System.out.println("no no no ");
		return Double.NaN;
	}
	
	/**
	 * 
	 * @param x_index
	 * @param y_index
	 * @param band_index
	 * @param length
	 * @return
	 */
	public double [] pixelValues(int x_index, int y_index, int band_index, int length) {
		final int x_size = length;
		if (band_index != _band_index || y_index != _y_index) {
			// reset the buffer;
			config(band_index);
			_y_index = y_index;
			_pixel_buffer = ByteBuffer.allocate(_buffer_size);
			_pixel_buffer.order(ByteOrder.nativeOrder());
//			_band.ReadRaster(x_index, y_index,  
//					x_size, 1, // x size; y size
//					x_size, 1, // buffer x size; buffer y size
//					_data_type, 
//					_pixel_buffer.array()); 
			
			_band.ReadRaster(0, y_index,  
					_block_xsize, 1, // x size; y size
					_block_xsize, 1, // buffer x size; buffer y size
					_data_type, 
					_pixel_buffer.array()); 
		}
		
		double [] ret = new double[x_size];
		for (int i = 0; i < x_size; i++) {
			if (_data_type == gdalconstConstants.GDT_Byte) {
				ret[i] =  ((int)_pixel_buffer.get()) & 0xff;
			} else if(_data_type == gdalconstConstants.GDT_Int16) {
				ret[i] = _pixel_buffer.getShort((x_index+i) * 2);
			} else if(_data_type == gdalconstConstants.GDT_Int32) {
				ret[i] = _pixel_buffer.getInt();
			} else if(_data_type == gdalconstConstants.GDT_Float32) {
				ret[i] = _pixel_buffer.getFloat();
			} else if(_data_type == gdalconstConstants.GDT_Float64) {
				ret[i] = _pixel_buffer.getDouble();
			} else if(_data_type == gdalconstConstants.GDT_UInt16) {
				ret[i] =_pixel_buffer.getChar((x_index + i)*2); // we have to double the index, but I don't know why
			}
		}
		
		return ret;
	}

	/**
	 * 
	 * @return
	 */
	public Band band() {
		return _band;
	}
	
	/**
	 * 
	 * @return
	 */
	public Dataset image() {
		return this._image;
	}
	
	/**
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	static boolean doubleArrayEquals(double []a, double [] b) {
		if (a.length != b.length) {
			System.out.println("length of two array are not equal");
			return false;
		}
		
		for (int i = 0; i < a.length; i++) {
			if (!doubleEquals(a[i], b[i])) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	static boolean doubleEquals(double a, double b) {
		double epsilon = 1e-10;
		return Math.abs(a - b) < epsilon ? true : false;
	}
	
	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		
		// TEST:
//		gdal.AllRegister();  // this should no longer be required. nc.
//		String filename = "/home/nick/MOD13A2/2010/2010.01.01/VI_QC/2010.01.01_VI_QC_mosaic_geo.1_km_16_days_VI_Quality.tif";
//		String filename2 = "/home/nick/MOD13A2/2010/2010.01.01/EVI/2010.01.01_EVI_mosaic_geo.1_km_16_days_EVI.tif";
//		ImageData imageData = new ImageData(filename2, 1);
//		
//		// that is expected array
//		final int kSize = 10;
//		final int x = 7051;
//		final int y = 3825;
//		double [] expected = new double[kSize];
//		
//		long start = System.nanoTime();
//		for (int i = 0; i < kSize; i++) {
//			expected[i] = imageData.pixelValue(x + i, y, 1); // that is great
//			
//		}
//		long stop = System.nanoTime();
//		long slowTime = (stop - start) / 1000;
//		System.out.println("slowTime: " + slowTime);
//		
//		// for the test result
//		ImageData testData = new ImageData(filename2, 1);
//		
//		start = System.nanoTime();
//		double [] testArray = testData.pixelValues(x, y, 1, kSize);
//		stop = System.nanoTime();
//		long fastTime = (stop - start) / 1000;
//		System.out.println("fastTime: " + fastTime);
//		
//		if (doubleArrayEquals(expected, testArray)) {
//			System.out.println(Arrays.toString(expected));
//			System.out.println(Arrays.toString(testArray));
//			System.out.println("OK");
//		} else {
//			System.out.println(Arrays.toString(expected));
//			System.out.println(Arrays.toString(testArray));
//			System.err.println("Two array are not equals");
//		}
		
	}

}



