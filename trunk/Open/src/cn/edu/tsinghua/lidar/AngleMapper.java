/**
 * 
 */
package cn.edu.tsinghua.lidar;

import java.util.LinkedList;
import java.util.List;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;


/**
 * @author nclinton
 *
 */
public class AngleMapper {

	public static List<Coordinate>getCircle(double radius, double lat, double lon, int numPts) {
		List<Coordinate> coords = new LinkedList();
		double angleDelta = 2.0*Math.PI/numPts;
		for (int a=0; a<numPts; a++) {
			Coordinate c = new Coordinate(lon, lat);
			double angle = a*angleDelta;
			
		}
		return coords;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
