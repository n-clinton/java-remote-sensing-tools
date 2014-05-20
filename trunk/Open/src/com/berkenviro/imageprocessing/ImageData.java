package com.berkenviro.imageprocessing;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;

/**
 * This class is used to access/get the pixel values from image file. 
 *
 * A instance of the class should be created to represent each file. Thus, 
 * there is a pixel buffer/dataset that is private to each file, so that 
 * the buffer can reduce the number of read operation while reading the 
 * image,  which will boost the program.
 * 
 * On the other hand, it requires a large amount of memory as each file 
 * should be equipped with a buffer/dataset.  
 *
 * @author Cong Hui He
 * 
 * logs:
 * 20131007. Added comments, formatted, made GDAL init static. nc.  Cong Hui 
 * 20131012. Make the code clean and more readable. CongHui
 * 20140120. Changed _block_xsize to be the number of pixels in a line, per the variable declaration
 */
public class ImageData {
	private int        _block_xsize; 	// the length of a row of the image
	private int        _buffer_size; 	// size of the buffer allocated for holding the image
	private Dataset    _image;			// a dataset holding the information of image
	private int        _band_index;
	private ByteBuffer _pixel_buffer;	// buffer containing the data read from image file
	private int        _data_type;
	private int        _y_index      = -1;
	private Band       _band;

	/**
	 * Initialize GDAL.
	 */
	static { 
		gdal.AllRegister();
		// not sure what advantage these config options may confer
//		gdal.SetConfigOption("GDAL_MAX_DATASET_POOL_SIZE", "512");
//		gdal.SetCacheMax(1074000000);
	}

	/**
	 * Just do the initialization, such as initialize the data member and  
	 * allocate memory for the buffer
	 * 
	 * @param filename the file where the dataset is related to
	 * @param band_index the index of the band the dataset will cover
	 */
	public ImageData(String filename, int band_index) {
		File image_file = new File(filename);
		_image = gdal.Open(image_file.getAbsolutePath(), gdalconst.GA_ReadOnly);
		reconfigBand(band_index);
		_buffer_size = gdal.GetDataTypeSize(_data_type) / 8 * _block_xsize;
		//_pixel_buffer = ByteBuffer.allocate(_buffer_size);
		_pixel_buffer = ByteBuffer.allocateDirect(_buffer_size);
		_pixel_buffer.order(ByteOrder.nativeOrder());
	}

	@Override
	public String toString() {
		return _pixel_buffer.toString();
	}
 	
	/***
	 * reconfigure some data member that is band relevant
	 * @param band_index
	 */
	private void reconfigBand(int band_index) {
		_band_index = band_index;
		_band = _image.GetRasterBand(_band_index);
		/*
		 * In the class variable declaration, it says that _block_xsize is
		 * "the length of a row of the image".  While it may coincidentally
		 * correspond to the line length, it is not necessarily true.
		 */
		//System.out.println("\t BlockXSize: "+_band.GetBlockXSize());
		//System.out.println("\t New BlockXSize: "+_image.GetRasterXSize());
		//_block_xsize = _band.GetBlockXSize();
		_block_xsize = _image.GetRasterXSize();
		_data_type = _band.getDataType();
	}


	/**
	 * Get the value of image data point given the point in GEO image.
	 *
	 * Implementation: first transform the coordinates from GEO to image, 
	 * then pass the coordinate of the pixel to get the pixel value.
	 *
	 * @param x_index X-coordinate in GEO
	 * @param y_index Y-coordinate in GEO
	 * @param band_index band index
	 * @return the value of image data point given the point in GEO image.
	 */
	public double imageValue(double x_index, double y_index, int band_index)  {
		int[] pixelXY = null; // pixel in image coordinate
		try {
			// do the coordinate transformation from GEO to image
			pixelXY = GDALUtils.getPixelXY(new double[] {x_index, y_index}, _image);
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		} 

		// get the value given the pixel coordinate
		return pixelValue(pixelXY[0], pixelXY[1], band_index); 
	}

	/**
	 * Get the value of the pixel index at (x_index, y_index) from the 
	 * buffer.  
	 *
	 * Implementation: A whole line of the image will be read into the 
	 * buffer. Thus, if you the pixel you are reading stays at the same 
	 * line as the last pixel you read, the pixel will be get directly from 
	 * the buffer. However, if the pixel you are about to read jumps to a 
	 * different line, that line will be read into the buffer, which will 
	 * replace the previous line.  Therefore, to gain a better performance, 
	 * it is a good idea to read the pixels line by line, from left to 
	 * right and then top to bottom.
	 *
	 * @param x_index X-coordinate of the pixel
	 * @param y_index Y-coordinate of the pixel
	 * @param band_index band number of the pixel
	 * @return the value of the pixel
	 */
	public double pixelValue(int x_index, int y_index, int band_index) {
		if (band_index != _band_index || y_index != _y_index) {
			// if the |band_index| or |y_index| are not the same as the previous 
			// ones, we need to read a new line.

			// 1. do some configuration
			reconfigBand(band_index);
			_y_index = y_index;
//			_pixel_buffer = ByteBuffer.allocateDirect(_buffer_size);
//			_pixel_buffer.order(ByteOrder.nativeOrder());
			_pixel_buffer.clear(); // I don't see why this buffer would need to be allocated again

			// 2. perform a read operation
//			_band.ReadRaster(
//					0, y_index,       // (x_begin, y_begin), it reads from begining of the line
//					_block_xsize, 1,  // (x size; y size), |_block_size| is the length of a line
//					_block_xsize, 1,  // buffer x size; buffer y size
//					_data_type, 
//					_pixel_buffer.array()); 
			_band.ReadRaster_Direct(
					0, y_index,       // (x_begin, y_begin), it reads from begining of the line
					_block_xsize, 1,  // (x size; y size), |_block_size| is the length of a line
					_block_xsize, 1,  // buffer x size; buffer y size
					_data_type, 
					_pixel_buffer);
		}

		return getValueFromRightDataType(x_index);
	}


	/***
	 * 
	 * @param offset
	 * @return
	 */
	private double getValueFromRightDataType(int offset) {
		// the offset is a magic number. I have no idea why the |offset| 
		// should be doubled for type |GDT_UInt16| and |GDT_Int16|, but 
		// it does result in correct value by several experiments.
		
		if (_data_type == gdalconstConstants.GDT_Byte) {
			return ((int)_pixel_buffer.get(offset)) & 0xff; // verified
		} else if(_data_type == gdalconstConstants.GDT_Int16) {
			return _pixel_buffer.getShort(offset * 2);    	// verified
		} else if(_data_type == gdalconstConstants.GDT_Int32) {
			return _pixel_buffer.getInt(offset);
		} else if(_data_type == gdalconstConstants.GDT_Float32) {
			return _pixel_buffer.getFloat(offset);
		} else if(_data_type == gdalconstConstants.GDT_Float64) {
			return _pixel_buffer.getDouble(offset);
		} else if(_data_type == gdalconstConstants.GDT_UInt16) {
			return _pixel_buffer.getChar(offset * 2);		// verified
		}
		return Double.NaN;
	}

	/***
	 * reclaim the resources
	 */
	public void deleteDataSet() {
		_image.delete();
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
//		String image = "/Users/nclinton/Documents/XJY_carbon/prec/prec_01.tif";
//		Dataset data = GDALUtils.getDataset(image);
//		try {
//			System.out.println("Image value: "+GDALUtils.imageValue(data, -120.0, 38.0, 1));
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		// OK
		
//		ImageData data = new ImageData(image, 1);
//		double check = data.imageValue(120.0, 38.0, 1);
//		System.out.println("Image value: "+ check);
		// OK after change in ImageData.reconfigBand() to make the width correct.  Check w/ Cong Hui
		
	}
}



