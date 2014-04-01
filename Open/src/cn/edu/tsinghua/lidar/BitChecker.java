/**
 * 
 */
package cn.edu.tsinghua.lidar;

/**
 * @author Nicholas
 *
 */
public class BitChecker {
	
	/**
	 * Check if bit n is on
	 * @param check
	 * @param n is zero indexed bit number
	 * @return
	 */
	public static boolean bitOn(int check, int n) {
		return ( (  check >>> n ) & 1 ) != 0;
	}
	
	/**
	 * Check if bit n is off
	 * @param check
	 * @param n is zero indexed bit number
	 * @return
	 */
	public static boolean bitOff(int check, int n) {
		return ( (  check >>> n ) & 1 ) == 0;
	}
	
	/**
	 * Return the integer representation of m bits starting at bit n
	 * @param check
	 * @param n is zero indexed bit number
	 * @param m is the number of bits in the field
	 * @return
	 */
	public static int getBits(int check, int m, int n) {
		return ( check & (int)Math.pow(2, m+n)-1 ) >>> n ;
	}
	
	/*
	 * MOD13 methods
	 */
	
	public static int mod13qa(int check) {
		return getBits(check, 2, 0);
	}
	
	public static int mod13usefulness(int check) {
		return getBits(check, 4, 2);
	}
	
	public static int mod13aerosols(int check) {
		return getBits(check, 2, 6);
	}
	
	public static int mod13landwater(int check) {
		return getBits(check, 3, 11);
	}
	
	/**
	 * 
	 * @param check
	 * @return
	 */
	public static boolean mod13ok(int check) {
		// land, shallow inland water or ephemeral water
		if (mod13landwater(check)==1 || mod13landwater(check)==3 || mod13landwater(check)==4) {
			if (mod13qa(check) == 0) { // no problems
				return true;
			}
			if (mod13qa(check) == 1) { // other quality
				if (mod13usefulness(check) < 8) { // value of 9 was criticized by reviewers
					return true;
				}
			}
		}
		return false;
	}
	
	/*
	 * MOD11 methods
	 */
	
	public static int mod11qa(int check) {
		return getBits(check, 2, 0);
	}
	
	public static int mod11quality(int check) {
		return getBits(check, 2, 2);
	}
	
	public static int mod11emis(int check) {
		return getBits(check, 2, 4);
	}
	
	public static int mod11temp(int check) {
		return getBits(check, 2, 6);
	}
	
	/**
	 * Return true if the LST error is <1K
	 * @param check
	 * @return
	 */
	public static boolean mod11ok(int check) {
		if (mod11qa(check) == 0) { // no problems
			return true;
		}
		if (mod11qa(check) == 1) { // other quality
			if (mod11temp(check) == 0) { // <1K error
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 16 bit unsigned integer
//		System.out.println(bitOn(1,0));
//		System.out.println(bitOn(3,0));
//		System.out.println(bitOn(81,4));
//		System.out.println(bitOff(17,6));
		
//		System.out.println(mod11ok(0));
//		System.out.println(mod11ok(17));
//		System.out.println(mod11ok(81));
//		System.out.println(mod11ok(33));
		
//		System.out.println(Integer.toBinaryString(2185));
//		System.out.println(Integer.toBinaryString(mod13qa(2185)));
//		System.out.println(Integer.toBinaryString(mod13usefulness(2185)));
//		System.out.println(Integer.toBinaryString(mod13landwater(2185)));
//		System.out.println();
//		System.out.println(mod13qa(2185));
//		System.out.println(mod13usefulness(2185));
//		System.out.println(mod13landwater(2185));
//		System.out.println(mod13ok(2185));
		
	}

}
