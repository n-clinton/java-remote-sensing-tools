/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.util.Calendar;
import java.util.List;

import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 *
 */
public interface Loadr {
	
	/**
	 * Set the zero reference.  
	 * This is essential to compare multiple time series with the same t-dimension.
	 * @param cal
	 */
	public void setDateZero(Calendar cal);
	
	/**
	 * Get the series, of length determined by implementing class, as a list of double[]s.
	 * @param pt is a georeferenced point.
	 * @return
	 */
	public List<double[]> getSeries(Point pt);
	
	/**
	 * @param x is georeferenced
	 * @param y is georeferenced
	 * @return
	 */
	public List<double[]> getSeries(double x, double y);
	
	/**
	 * Get an interpolated series, where missing data is filled in, but not extrapolated.
	 * @param pt is a georeferenced point.
	 * @return
	 * @throws Exception
	 */
	public double[] getY(Point pt) throws Exception;
	
	/**
	 * Get the X-coordinates corresponding to getY.
	 * @return
	 */
	public double[] getX();
	
	/**
	 * Release file resources.
	 */
	public void close();
	
}
