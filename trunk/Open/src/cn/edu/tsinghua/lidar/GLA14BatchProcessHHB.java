/**
 * 
 */
package cn.edu.tsinghua.lidar;

import java.io.File;

/**
 * @author Nicholas
 *
 */
public class GLA14BatchProcessHHB extends GLA14BatchProcess {

	public GLA14BatchProcessHHB(String parentDir, String outputDir, int r) {
		super(parentDir, outputDir, r);
	}
	
	/**
	 * 
	 * @param file
	 */
	@Override
	public void processDir(File file) {
		if (file.isFile()) {
			// if it's a GLA14 file
			if (file.getName().endsWith(".DAT") || file.getName().endsWith(".dat")) {
				String out = output.getPath() + output.separator; // base output directory
				out += file.getParentFile().getName().replace(".", "_") + "_"; // add the year and date
				out += file.getName().substring(0, file.getName().length()-4) + "_out.csv"; // input filename
				// should be set to go
				try {
					System.out.println("Input: "+file.getAbsolutePath());
					System.out.println("Output: "+out);
					GLA14Reader reader = new GLA14ReaderHHB(file.getAbsolutePath());
					reader.r33write(out);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		else if (file.isDirectory()) { // recurse
			File[] subDirs = file.listFiles();
			for (File f : subDirs) {
				processDir(f);
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// r33, HHB custom run
		String allGLA14 = "D:/GLA14.033/";
		String outDir = "C:/Users/Nicholas/Documents/GLA14.033.hhb/";
		new GLA14BatchProcessHHB(allGLA14, outDir, GLA14BatchProcessHHB.GLA14_R33);

	}

}
