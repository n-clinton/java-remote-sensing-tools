/**
 * 
 */
package cn.edu.tsinghua.lidar;

import java.io.File;

/**
 * @author nclinton
 *
 */
public class GLA14BatchProcess {
	public static int GLA14_R33 = 33;
	public static int GLA14_R31 = 31;
	protected int release;

	// directories
	protected File parent;
	protected File output;
	
	/**
	 * 
	 * @param parentDir
	 * @param outputDir
	 * @param r is either GLA14_R31 or GLA14_R33
	 */
	public GLA14BatchProcess(String parentDir, String outputDir, int r) {
		release = r;
		parent = new File(parentDir);
		if (parent.isDirectory()) { // go in there
			// put the output here:
			output = new File(outputDir);
			if (output.isDirectory()) { // set to go
				File[] subDirs = parent.listFiles();
				for (File f : subDirs) {
					processDir(f);
				}
			}
		}
	}
	
	
	/**
	 * 
	 * @param file
	 */
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
					GLA14Reader reader = new GLA14Reader(file.getAbsolutePath());
					if (release == GLA14_R33) {
						reader.r33write(out);
					}
					else {
						reader.write(out);
					}
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
	 * @param args is a String array (space separated strings) or arguments
	 * 			args[0] is the full path of the input directory
	 * 			args[1] is the full path of the output directory
	 */
	public static void main(String[] args) {
		// Mac test
		//String test = "/Users/nclinton/Documents/urban_ag/lidar/";
		//new GLA14BatchProcess(test, test);
		
		// Big process.  All GLA14 data
//		String allGLA14 = "C:\\Users\\Nicholas\\Documents\\GLA14.031\\";
//		String outDir = "C:\\Users\\Nicholas\\Documents\\GLA14.031.out\\";
//		new GLA14BatchProcess(allGLA14, outDir, GLA14BatchProcess.GLA14_R31);
		
		// r33
//		String allGLA14 = "D:/GLA14.033/";
//		String outDir = "C:/Users/Nicholas/Documents/GLA14.033.out/";
//		new GLA14BatchProcess(allGLA14, outDir, GLA14BatchProcess.GLA14_R33);
		
		//new GLA14BatchProcess(args[0], args[1], GLA14BatchProcess.GLA14_R33);
		
		// reprocess due to zero SD divide-by-zero error:
		String allGLA14 = "D:/GLA14.033/";
		String outDir = "C:/Users/Nicholas/Documents/GLA14.033.out/";
		new GLA14BatchProcess(allGLA14, outDir, GLA14BatchProcess.GLA14_R33);
		
	}

}
