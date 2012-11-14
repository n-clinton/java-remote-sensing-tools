/**
 * 
 */
package cn.edu.tsinghua.timeseries;

/**
 * @author Nicholas Clinton
 * Utility object to hold information about extrema.
 */
public class Extremum {

	private double myXcoord;
	private int myType;
	private boolean ok;
	public static final int EXTREMUM_TYPE_MIN = 1;
	public static final int EXTREMUM_TYPE_MAX = 2;
	 
	public Extremum(double x, int type) {
		ok = true;
		if (x < 0) {
			System.err.println("X coordinate is negative!");
			ok = false;
		}
		myXcoord = x;
		if (type != EXTREMUM_TYPE_MIN && type != EXTREMUM_TYPE_MAX) {
			System.err.println("Unrecognized extremum type!");
			ok = false;
		}
		myType = type;
	}
	
	public double getX() {
		return myXcoord;
	}
	
	public int getType() {
		return myType;
	}
	
	public boolean isOK() {
		return ok;
	}
	
	public String toString() {
		return myXcoord + ((myType == EXTREMUM_TYPE_MIN) ? " EXTREMUM_TYPE_MIN" : " EXTREMUM_TYPE_MAX");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
