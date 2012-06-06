/**
 * 
 */
package cn.edu.tsinghua.lidar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import com.berkenviro.imageprocessing.JAIUtils;

/**
 * @author Nicholas
 * 
 * This class summarizes shots within pixels of the supplied image, writing various tables
 * of the averaged parameters of choice. The various methods average different things.  Uses 
 * a Hashtable to store a GLA14Summarizer.Pixel key with a double[][] of the interesting parameters.
 *
 */
public class GLA14Summarizer {

	private PlanarImage image;
	private BufferedWriter writer;
	File[] files;
	Hashtable<Pixel, double[][]> pixels;
	
	/**
	 * 
	 * @param imageName
	 * @param outName
	 * @throws Exception
	 */
	public GLA14Summarizer(String imageName, String[] fileNames, String outName) throws Exception {
		// overlay image
		image = JAIUtils.readImage(imageName);
		JAIUtils.register(image);
		// overlay points
		files = new File[fileNames.length];		
		for (int s=0; s<fileNames.length; s++) {
			files[s] = new File(fileNames[s]);
		}
		pixels = new Hashtable<Pixel, double[][]>();
		// output
		writer = new BufferedWriter(new FileWriter(outName));
	}
	
	/**
	 *  
	 * @param fileName
	 * @throws Exception
	 */
	public void summarize() throws Exception {
		// iterate over the input files
		for (int f=0; f<files.length; f++) {
			System.out.println("Processing file "+files[f].getName());
			BufferedReader reader = new BufferedReader(new FileReader(files[f]));
			// skip the header
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				try {
					GLA14shot shot = new GLA14shot(line);
					// don't bother with high latitudes
					if (shot.lat < -60.0 || shot.lat > 70) { continue; }
					
					// get the pixel coordinates
					int[] pixelXY = JAIUtils.getPixelXY(new double[] {shot.lon, shot.lat}, image);
					Pixel pixel = new Pixel();
					pixel.x = pixelXY[0];
					pixel.y = pixelXY[1];
					double[][] vals;
					if (pixels.containsKey(pixel)) {
						//System.out.println("Yay! I found "+pixel);
						vals = pixels.get(pixel);
					}
					else {
						vals = new double[2][files.length];
						pixels.put(pixel, vals);
					}
					vals[0][f]++;
					vals[1][f]+=shot.SigBegHt;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		reader.close();
		}
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void write() throws Exception {
		writer.write("lat,lon,ht2003,n2003,ht2004,n2004,ht2005,n2005,ht2006,n2006,ht2007,n2007,ht2008,n2008,ht2009,n2009,average,n\n");
		String line = "";
		
		Enumeration<Pixel> keys = pixels.keys();
		while (keys.hasMoreElements()) {
			Pixel pix = keys.nextElement();
			double[][] vals = pixels.get(pix);
			double[] coords = JAIUtils.getProjectedXY(new int[] {pix.x,pix.y}, image);
			line = String.format("%.4f,%.4f,",coords[1], coords[0]);
			double avg=0;
			int n=0;
			for (int f=0; f<files.length; f++) {
				line+=(vals[0][f]!=0 ? String.format("%.4f,",vals[1][f]/vals[0][f]): ",");
				line+=String.format("%.0f,",vals[0][f]);
				avg+=(vals[1][f]);
				n+=vals[0][f];
			}
			line+=String.format("%.4f,%d", avg/n, n);
			System.out.println(line);
			writer.write(line);
			writer.newLine();
		}
		writer.close();
	}
	
	
	/**
	 *  
	 * @param fileName
	 * @throws Exception
	 */
	public void summarize3() throws Exception {
		// iterate over the input files
		for (int f=0; f<files.length; f++) {
			System.out.println("Processing file "+files[f].getName());
			BufferedReader reader = new BufferedReader(new FileReader(files[f]));
			// skip the header
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				try {
					
					GLA14shot shot = new GLA14shot(line);
					// don't bother with high latitudes
					if (shot.lat < -60.0 || shot.lat > 70) { continue; }

					// get the pixel coordinates
					int[] pixelXY = JAIUtils.getPixelXY(new double[] {shot.lon, shot.lat}, image);
					Pixel pixel = new Pixel();
					pixel.x = pixelXY[0];
					pixel.y = pixelXY[1];
					
					// vals is the array of running sums
					double[][] vals;
					if (pixels.containsKey(pixel)) {
						//System.out.println("Yay! I found "+pixel);
						vals = pixels.get(pixel);
					}
					else {
						// column 1 is n, column 2 is parameter sum, column 3 is sum of weights
						vals = new double[3][7];
						pixels.put(pixel, vals);
					}
					
					// these will always exist, no weight possible
					vals[0][0]++;
					vals[1][0]+=shot.SigBegHt;
					// the following may or may not exist
					// top
					vals[0][1]+=shot.hasTop();
					vals[1][1]+=shot.getTop();
					vals[2][1]+=shot.topWeight();
					// second
					vals[0][2]+=shot.hasSecond();
					vals[1][2]+=shot.getSecond();
					vals[2][2]+=shot.secondWeight();
					// third
					vals[0][3]+=shot.hasThird();
					vals[1][3]+=shot.getThird();
					vals[2][3]+=shot.thirdWeight();
					// weighted
					// top
					vals[0][4]+=shot.hasTop();
					vals[1][4]+=shot.getTop()*shot.topWeight();
					vals[2][4]+=shot.topWeight();
					// second
					vals[0][5]+=shot.hasSecond();
					vals[1][5]+=shot.getSecond()*shot.secondWeight();
					vals[2][5]+=shot.secondWeight();
					// third
					vals[0][6]+=shot.hasThird();
					vals[1][6]+=shot.getThird()*shot.thirdWeight();
					vals[2][6]+=shot.thirdWeight();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		reader.close();
		}
	}
	
	
	
	/**
	 * 
	 * @throws Exception
	 */
	public void write3() throws Exception {
		writer.write("lat,lon,sigBeg,top,second,third,avg1,avg2,avg3,wtd1,wtd2,wtd3");
		writer.newLine();
		
		String line = "";
		
		Enumeration<Pixel> keys = pixels.keys();
		while (keys.hasMoreElements()) {
			Pixel pix = keys.nextElement();
			// coordinates
			double[] coords = JAIUtils.getProjectedXY(new int[] {pix.x,pix.y}, image);
			line = String.format("%.4f,%.4f,",coords[1], coords[0]);
			// averages
			double[][] vals = pixels.get(pix);
			// sigBegHt
			line+=String.format("%.1f,", vals[1][0]/vals[0][0]);

			// TODO: redo all this to be null rather than zero
			
			// top
			line+= (vals[0][1]>0 ? String.format("%.1f,", vals[1][1]/vals[0][1]) : ",");
			// second
			line+= (vals[0][2]>0 ? String.format("%.1f,", vals[1][2]/vals[0][2]) : ",");
			// third
			line+= (vals[0][3]>0 ? String.format("%.1f,", vals[1][3]/vals[0][3]) : ",");
			// avg1
			line+= String.format("%.1f,", (vals[1][0]+vals[1][1]+vals[1][2]+vals[1][3])/(vals[0][0]+vals[0][1]+vals[0][2]+vals[0][3]));
			// avg2
			line+=String.format("%.1f,", (vals[1][0]+vals[1][1]+vals[1][2])/(vals[0][0]+vals[0][1]+vals[0][2]));
			// avg3
			line+=String.format("%.1f,", (vals[1][0]+vals[1][1])/(vals[0][0]+vals[0][1]));
			// wtd1
			line+= (vals[0][4]>0 ? String.format("%.1f,", vals[1][4]/vals[2][4]) : ",");
			// wtd2
			if (vals[0][4]>0 || vals[0][5]>0) {
				line+=String.format("%.1f,", (vals[1][4]+vals[1][5])/(vals[2][4]+vals[2][5]));
			}
			else {
				line+=",";
			}
			// wtd3
			if (vals[0][4]>0 || vals[0][5]>0 || vals[0][6]>0) {
				line+=String.format("%.1f", (vals[1][4]+vals[1][5]+vals[1][6])/(vals[2][4]+vals[2][5]+vals[2][6]));
			}
			
			System.out.println(line);
			writer.write(line);
			writer.newLine();
		}
		writer.close();
	}
	
	
	/**
	 *  
	 * @param fileName
	 * @throws Exception
	 */
	public void summarize4() throws Exception {
		
		RandomIter iter = RandomIterFactory.create(image, null);
		// iterate over the input files
		for (int f=0; f<files.length; f++) {
			System.out.println("Processing file "+files[f].getName());
			BufferedReader reader = new BufferedReader(new FileReader(files[f]));
			// skip the header
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				try {
					GLA14shot shot = new GLA14shot(line);
					// don't bother with high latitudes
					if (shot.lat < -60.0 || shot.lat > 70) { continue; }
					//System.out.println("lat "+shot.lat+" lon "+shot.lon);
					// get the pixel coordinates
					int[] pixelXY = JAIUtils.getPixelXY(new double[] {shot.lon<0 ? shot.lon+360 : shot.lon, shot.lat}, image);
					Pixel pixel = new Pixel();
					pixel.x = pixelXY[0];
					pixel.y = pixelXY[1];
					pixel.id = iter.getSample(pixel.x, pixel.y, 0);
					
					double[][] vals;
					if (pixels.containsKey(pixel)) {
						//System.out.println("Yay! I found "+pixel);
						vals = pixels.get(pixel);
					}
					else {
						vals = new double[2][6];
						pixels.put(pixel, vals);
					}
					
					// retrieval of lidar parameters /*******************/
					if (shot.SigBegHt != 0) {
						vals[1][0]+=shot.SigBegHt;
						vals[0][0]++;
					}
					double ht1 = shot.getWeightedHt1();
					if(ht1 != 0) {
						vals[1][1]+=ht1;
						vals[0][1]++;
					}
					double ht2 = shot.getWeightedHt2();
					if (ht2 != 0) {
						vals[1][2]+=ht2;
						vals[0][2]++;
					}
					double density = shot.getDensity();
					if (density != 0) {
						vals[1][3]+=density;
						vals[0][3]++;
					}
					double hw1 = shot.hw1();
					if (hw1 != 0) {
						vals[1][4]+=hw1;
						vals[0][4]++;
					}
					double hw2 = shot.hw2();
					if (hw2 != 0) {
						vals[1][5]+=hw2;
						vals[0][5]++;
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		reader.close();
		}
	}
	
	
	/**
	 * 
	 * @throws Exception
	 */
	public void write4() throws Exception {
		writer.write("id,lon,lat,n,sigBegHt,ht1,ht2,density,hw1,hw2");
		writer.newLine();
		
		String line = "";
		
		Enumeration<Pixel> keys = pixels.keys();
		while (keys.hasMoreElements()) {
			Pixel pix = keys.nextElement();
			// coordinates
			double[] coords = JAIUtils.getProjectedXY(new int[] {pix.x,pix.y}, image);
			line = (int)pix.id+",";
			line += String.format("%.4f,%.4f,",coords[1], coords[0]);
			
			// averages
			double[][] vals = pixels.get(pix);
			line += (int)vals[0][0]+",";
			for (int i=0;i<6;i++) {
				if (vals [0][i] > 0) {
					line+=String.format("%.4f,", vals[1][i]/vals[0][i]);
				} else {
					line+=",";
				}
			}

			System.out.println(line);
			writer.write(line);
			writer.newLine();
		}
		writer.close();
	}
	
	
	
	/**
	 * Pixel thingy.
	 * @author Nicholas
	 *
	 */
	class Pixel {
		int x;
		int y;
		int id;
		
		@Override
		public String toString() {
			return "("+x+","+y+")";
		}
		
		@Override
		public boolean equals(Object other) {
			if (other instanceof Pixel) {
				if (x==((Pixel)other).x && y==((Pixel)other).y) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return (x * 33) ^ y;
		}
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// check these
//		String[] files = {"C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2003.csv",
//						  "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2004.csv",
//						  "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2005.csv",
//						  "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2006.csv",
//						  "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2007.csv",
//						  "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2008.csv",
//						  "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2009.csv"
//						  };
//		String image = "C:/Users/Nicholas/Documents/urban/Landscan2010/derived_data/gpt2pop_.tif";
//		String output = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/overlay_2003_2009_pixels.csv";
//		try {
//			GLA14Summarizer sum = new GLA14Summarizer(image, files, output);
//			sum.summarize();
//			sum.write();
//			System.out.println(Calendar.getInstance().getTime());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		

//		String[] files = {
//				  "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu2_2009.csv"
//				  };
//		String[] files = {
//				  "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/GLA14_r31_mssu.csv"
//				  };
//		String image = "C:/Users/Nicholas/Documents/urban/Landscan2010/derived_data/gpt2pop_.tif";
//		//String output = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/overlay_2009_pixels.csv";
//		String output = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/overlay_2003_2009_pixels.csv";
//		try {
//			GLA14Summarizer sum = new GLA14Summarizer(image, files, output);
//			sum.summarize3();
//			sum.write3();
//			System.out.println(Calendar.getInstance().getTime());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20120526
//		String[] files = {
//				  "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/GLA14_r31_mssu2_2005.csv",
//				  "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/GLA14_r31_mssu2_2006.csv",
//				  "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/GLA14_r31_mssu2_2007.csv",
//				  "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/GLA14_r31_mssu2_2008.csv",
//				  "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/GLA14_r31_mssu2_2009.csv"
//				  };
//
//		String image = "C:/Users/Nicholas/Documents/urban/Landscan/derived_data/lat_id1.tif";
//		String output = "C:/Users/Nicholas/Documents/urban/Landscan/overlay_with_GLA14/overlay_2005_2009_heights_density_hw.csv";
//		try {
//			GLA14Summarizer sum = new GLA14Summarizer(image, files, output);
//			sum.summarize4();
//			sum.write4();
//			System.out.println(Calendar.getInstance().getTime());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
	}
}
