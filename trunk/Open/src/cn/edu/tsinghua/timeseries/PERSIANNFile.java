/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Calendar;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.osr;

import com.berkenviro.gis.GISUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;

/**
 * @author Nicholas Clinton
 * 
 * This is the binary representation of a PERSIANN GCCS file, daily rainfall, global coverage.
 * 
 * Filename:
 * 	rgccs1dYYDDD.bin
 * 
 * Format:
 *   binary in row centric format.
 *   4-byte float
 *   data are big-endian.
 *   units mm/day
 *   NODATA is represented with -9999
 *   
 * Geo:
 *   60N to 60S and 0 to 360 Longitude
 *   3000 rows x 9000 cols
 *   at .04 deg
 *
 *	first value is NW most pixel centered at .02 lon and 59.8 N lat.
 *	next value is N most lat + .04 lon at .06 lon  and so on.
 *	last value is SE corner     
 *
 */
public class PERSIANNFile extends RandomAccessFile implements Comparable {

	private AffineTransformation at, inv;
	private int width, height;
	private String filename;
	Calendar cal;
	
	/**
	 * 
	 * @param name
	 * @throws FileNotFoundException
	 */
	public PERSIANNFile(String name) throws FileNotFoundException {
		super(name, "r");
		filename = name; // complete file path
		// parse the date from the filename
		int dot = filename.lastIndexOf(".");
		int year = Integer.parseInt("20"+filename.substring(dot-5, dot-3));
		int doy = Integer.parseInt(filename.substring(dot-3, dot));
		cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.DAY_OF_YEAR, doy);
		// set the georeferencing, assumed WGS84
		at = raster2proj();
		inv = GISUtils.proj2raster(at);
		width = 9000;
		height = 3000;
	}
	
	@Override
	public int compareTo(Object o) {
		return this.cal.compareTo(((PERSIANNFile)o).cal);
	}
	
	/**
	 * Geographic coordinate system specified by online readme.
	 * @return the transformation from raster to projected coordinates.
	 * Invert this transformation to go the other way.
	 */
	public AffineTransformation raster2proj() {
		// these are the AffineTransformation parameters
		double m00, m01, m02, m10, m11, m12;
		m02 = 0.0; 		// ul x
		m12 = 60.0; 	// ul y
		m00 = 0.04; 	// delta x
		m11 = -0.04; 	// delta y
		m01 = 0.0;
		m10 = 0.0;
		return new AffineTransformation(m00, m01, m02, m10, m11, m12);
	}

	/**
	 * 
	 * @param projXY in geographic coordinates.
	 * @return zero-referenced pixel coordinates
	 * @throws Exception
	 */
	public int[] getPixelXY(double[] projXY) throws Exception {
		Coordinate pix = new Coordinate();
		inv.transform(new Coordinate(projXY[0], projXY[1]), pix);
		if ((int)pix.x < 0 || (int)pix.x >= width || (int)pix.y < 0 || (int)pix.y >= height) {
			throw new Exception("Impossible coordinates: "+pix);
		}
		return new int[] {(int)pix.x, (int)pix.y};
	}
	
	/**
	 * Read the pixel value at the specified location.
	 * @param pixel
	 * @param line
	 * @return
	 */
	public float readPixel(int pixel, int line) throws Exception {
		this.seek(line*width*4 + pixel*4);
		return this.readFloat();
	}
	
	/**
	 * 
	 * @param pt
	 * @return
	 */
	public float imageValue(Point pt) throws Exception {
		return imageValue(pt.getX(), pt.getY());
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 * @throws Exception
	 */
	public float imageValue(double x, double y) throws Exception {
		int[] pixelXY = getPixelXY(new double[] {x, y});
		//System.out.println("("+pixelXY[0]+","+pixelXY[1]+")");
		return readPixel(pixelXY[0], pixelXY[1]);
	}
	
	/**
	 * Write a georeferenced TIFF from this file.
	 * @param outFileName
	 */
	public void writeImage(String outFileName) {
	    gdal.AllRegister();
        Driver driver = gdal.GetDriverByName("GTiff");
        Dataset dataset = driver.Create(outFileName, width, height, 1, gdalconst.GDT_Float32);

        double[] geo = at.getMatrixEntries();
        dataset.SetGeoTransform(new double[] {geo[2],geo[0],geo[1],geo[5],geo[3],geo[4]});
        //dataset.SetProjection("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
        dataset.SetProjection(osr.SRS_WKT_WGS84);
        
        Band band = dataset.GetRasterBand(1);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * width);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
	    
        for( int i = 0; i < height; i++) {
            for( int j = 0; j < width; j++) {
                try {
					floatBuffer.put(j, readPixel(j,i));
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
            band.WriteRaster_Direct(0, i, width, 1, gdalconst.GDT_Float32, byteBuffer);
        }
        // the following replaced unlink, but not tested 20121123
        dataset.FlushCache();
        dataset.delete();
	}
	
	/**
	 * 
	 */
	@Override
	public String toString() {
		return cal.getTime()+" : "+filename;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// test
		PERSIANNFile pf = null;
		try {
			pf = new PERSIANNFile("C:/Users/Public/Documents/PERSIANN/8km_daily/2010/rgccs1d10001.bin");
//			for (int i=1000; i<9000; i++) {
//				for (int j=1000; j<3000; j++) {
//					System.out.println("("+i+","+j+") : "+pf.readPixel(i,j));
//				}
//			}
//			for (int x=0; x<10; x++) {
//				for (int y=60; y>50; y--) {
//					System.out.println("("+x+","+y+") : "+pf.imageValue(GISUtils.makePoint(x, y)));
//				}
//			}
			// this looks OK relative to the 6 hourly thumbnail
			//pf.writeImage("C:/Users/Public/Documents/PERSIANN/8km_daily/rgccs1d10001_check.tif");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				pf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		
	}

}
