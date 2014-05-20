/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.util.Calendar;

/**
 * @author Nicholas
 *
 */
public class DatedQCImage implements Comparable {

	String imageName;
	String qcImageName;
	String dateName;
	final Calendar cal;
	
	public DatedQCImage(Calendar cal) {
		this.cal = cal;
	}
	
	public DatedQCImage(String imageFileName, String qcFileName, Calendar c) {
		imageName = imageFileName;
		qcImageName = qcFileName;
		cal = c;
	}
	
	public String getImage() {
		return imageName;
	}
	
	@Override
	public String toString() {
		return cal.getTime()+" : "+imageName+" : "+qcImageName;
	}
	
	@Override
	public int compareTo(Object o) {
		return this.cal.compareTo(((DatedQCImage)o).cal);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
}
