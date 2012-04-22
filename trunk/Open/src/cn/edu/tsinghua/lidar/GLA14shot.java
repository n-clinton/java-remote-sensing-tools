/**
 * 
 */
package cn.edu.tsinghua.lidar;

/**
 * @author Nicholas
 *
 */
public class GLA14shot {

	int rec_ndx;
	double lat;
	double lon;
	double elev;
	double SigEndOff;
	double SigBegHt;
	double[] heights;
	double[] amplitudes;
	double[] areas;
	
	// indices for the top return (not including SigBegHt), second and third, -1 if null
	private int top;
	private int second;
	private int third;
	
	String[] toks;
	
	static String header = 
			"rec_ndx,lat,lon,elev,End,BegHt,Off2,Off3,Off4,Off5,Off6," +
			"gAmp1,gAmp2,gAmp3,gAmp4,gAmp5,gAmp6,gArea1,gArea2,gArea3,gArea4,gArea5,gArea6";
	
	/**
	 * Initialize with a string read from a line of the output table.
	 * There might be more tokens, but only these are recognized.
	 * @param record
	 */
	public GLA14shot(String record) {
		/*
		 * 0       1   2   3    4         5        6            7            8            9            10           11    12    13    14    15    16    17     18     19     20     21     22
		 * rec_ndx,lat,lon,elev,SigEndOff,SigBegHt,gpCntRngOff2,gpCntRngOff3,gpCntRngOff4,gpCntRngOff5,gpCntRngOff6,gAmp1,gAmp2,gAmp3,gAmp4,gAmp5,gAmp6,gArea1,gArea2,gArea3,gArea4,gArea5,gArea6
		 */
		toks = record.split(",");
		rec_ndx	= Integer.parseInt(toks[0]);
		lat	= Double.parseDouble(toks[1]);
		lon	= Double.parseDouble(toks[2]);
		// convert to [-180, 180] scale
		if (lon > 180.0) { lon = lon-360.0; }
		
		elev = Double.parseDouble(toks[3]);	
		SigEndOff = Double.parseDouble(toks[4]);	
		SigBegHt = Double.parseDouble(toks[5]);
		heights = new double[] {0.0, // Dummy placeholder corresponding to a null return
							Double.parseDouble(toks[6]), // gpCntRngOff2
							Double.parseDouble(toks[7]), // gpCntRngOff3
							Double.parseDouble(toks[8]), // gpCntRngOff4
							Double.parseDouble(toks[9]), // gpCntRngOff5
							Double.parseDouble(toks[10]) // gpCntRngOff6
		};
		amplitudes = new double[] {Double.parseDouble(toks[11]), // gAmp1
							   Double.parseDouble(toks[12]), // gAmp2
							   Double.parseDouble(toks[13]), // gAmp3
							   Double.parseDouble(toks[14]), // gAmp4
							   Double.parseDouble(toks[15]), // gAmp5
							   Double.parseDouble(toks[16])	 // gAmp6
		};
		areas = new double[] {Double.parseDouble(toks[17]), // gArea1
						  Double.parseDouble(toks[18]), // gArea2
						  Double.parseDouble(toks[19]), // gArea3
						  Double.parseDouble(toks[20]), // gArea4
						  Double.parseDouble(toks[21]), // gArea5
						  Double.parseDouble(toks[22])  // gArea6
		};
		
		// call in this order to find peaks.
		top = top();
		second = second();
		third = third();
	}
	
	private int top() {
		for (int i=5; i>0; i--) {
			if(heights[i] != 0) {
				return i;
			}
		}
		return 0;
	}
	
	private int second() {
		for (int i=top-1; i>0; i--) {
			if(heights[i] != 0) {
				return i;
			}
		}
		return 0;
	}
	
	private int third() {
		for (int i=second-1; i>0; i--) {
			if(heights[i] != 0) {
				return i;
			}
		}
		return 0;
	}

	public void print() {
		String[] head = header.split(",");
		for (int s=4; s<head.length; s++) {
			System.out.print(head[s]+"\t");
		}
		System.out.println();
		for (int s=4; s<head.length; s++) {
			System.out.print(toks[s]+"\t");
		}
		System.out.println();
		System.out.println("Top="+getTop()+", topWeight="+topWeight());
		System.out.println("Second="+getSecond()+", secondWeight="+secondWeight());
		System.out.println("Third="+getThird()+", Third="+thirdWeight());
	}
	
	/**
	 * @return zero if no peak exists
	 */
	public double getTop() {
		return heights[top];
	}
	
	/**
	 * @return zero if no peak exists
	 */
	public double topWeight() {
		if (top > 0) {
			return areas[top];
		}
		return 0.0;
	}
	
	/**
	 * 
	 * @return
	 */
	public double getSecond() {
		return heights[second];
	}
	
	/**
	 * 
	 * @return
	 */
	public double secondWeight() {
		if (second > 0) {
			return areas[second];
		}
		return 0.0;
	}
	
	public double getThird() {
		return heights[third];
	}
	
	/**
	 * 
	 * @return
	 */
	public double thirdWeight() {
		if (third > 0) {
			return areas[third];
		}
		return 0.0;
	}
	
	/**
	 * 
	 * @return
	 */
	public double top2Sum() {
		return getTop()+getSecond();
	}
	
	/**
	 * 
	 * @return
	 */
	public int hasTop() {
		if (top>0) {
			return 1;
		}
		return 0;
	}
	
	public int hasSecond() {
		if (second>0) {
			return 1;
		}
		return 0;
	}
	
	public int hasThird() {
		if (third>0) {
			return 1;
		}
		return 0;
	}
	/**
	 * 
	 * @return
	 */
	public int top2n() {
		return hasTop() + hasSecond();
	}
	
	/**
	 * 
	 * @return
	 */
	public double top3Sum() {
		return top2Sum() + getThird();
	}
	
	/**
	 * 
	 * @return
	 */
	public int top3n() {
		return top2n() + hasThird();
	}
	
	/**
	 * 
	 * @return
	 */
	public double top2weighted() {
		return getTop()*topWeight() + getSecond()*secondWeight();
	}
	
	/**
	 * @return
	 */
	public double top2weight() {
		return topWeight() + secondWeight();
	}
	
	/**
	 * 
	 * @return
	 */
	public double top3weighted() {
		return top2weighted() + getThird()*thirdWeight();
	}
	
	/**
	 * 
	 * @return
	 */
	public double top3weight() {
		return top2weight() + thirdWeight();
	}
 	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
