/**
 * 
 */
package cn.edu.tsinghua.spatial;

/**
 * @author nclinton
 *
 */
public class Crosswalkr {

	final static int IGBP_2_IPCC = 0;
	final static int FROM_2_IPCC = 1;
	
	private int mode;
	
	public Crosswalkr(int mode) {
		this.mode = mode;
	}
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	public int crosswalk(int input) {
		switch (mode) {	
			case IGBP_2_IPCC:
				return igbp2ipcc(input);
			case FROM_2_IPCC:
				return from2ipcc(input);
		}
		return IPCCcarbon.OTHER;
	}
	
	/**
	 * TODO: Improve
	 * @param igbp
	 * @return
	 */
	public static int igbp2ipcc(int igbp) {
		if (igbp > 0 && igbp < 6) {
			return IPCCcarbon.FOREST;
		}
		/*
		 * This makes NO SENSE!  TODO: check CDIAC methods for shrub.  Figure out something better.
		 */
		if (igbp > 5 && igbp < 8) {
			return IPCCcarbon.FOREST;
		}
		if (igbp > 7 && igbp < 11) {
			return IPCCcarbon.GRASSLAND;
		}
		if (igbp ==12 || igbp ==14) {
			return IPCCcarbon.CROPLAND;
		}
		if (igbp == 11) {
			return IPCCcarbon.WETLAND;
		}
		else if (igbp == 0 || igbp == 15 || igbp == 16) {
			return IPCCcarbon.NOCarbon;
		}
		return IPCCcarbon.OTHER;
	}
	
	
	/**
	 * 
	 * @param from
	 * @return
	 */
	public static int from2ipcc(int from) {
		if (from == 2 || from == 4) {
			return IPCCcarbon.FOREST;
		}
		/*
		 * This makes NO SENSE!  TODO: check CDIAC methods for shrub.  Figure out something better.
		 */
		else if (from == 3) {
			return IPCCcarbon.GRASSLAND;
		}
		else if (from == 0) {
			return IPCCcarbon.CROPLAND;
		}
		else if (from == 5) {
			return IPCCcarbon.WETLAND;
		}
		else {
			return IPCCcarbon.OTHER;
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
