package cn.edu.tsinghua.modis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;

/**
 * 
 */

/**
 * @author Nicholas
 *
 */
public class MRTRunner {

	static String MOD11_LST_DAY = 			"1 0 0 0 0 0 0 0 0 0 0 0";
	static String MOD11_QC_DAY = 			"0 1 0 0 0 0 0 0 0 0 0 0";
	static String MOD11_VIEW_TIME_DAY = 	"0 0 1 0 0 0 0 0 0 0 0 0";
	static String MOD11_VIEW_ANGLE_DAY = 	"0 0 0 1 0 0 0 0 0 0 0 0";
	static String MOD11_LST_NIGHT = 		"0 0 0 0 1 0 0 0 0 0 0 0";
	static String MOD11_QC_NIGHT = 			"0 0 0 0 0 1 0 0 0 0 0 0";
	static String MOD11_VIEW_TIME_NIGHT = 	"0 0 0 0 0 0 1 0 0 0 0 0";
	static String MOD11_VIEW_ANGLE_NIGHT = 	"0 0 0 0 0 0 0 1 0 0 0 0";
	static String EMIS_31 = 				"0 0 0 0 0 0 0 0 1 0 0 0";
	static String EMIS_32 = 				"0 0 0 0 0 0 0 0 0 1 0 0";
	static String CLEAR_SKY_DAY = 			"0 0 0 0 0 0 0 0 0 0 1 0";
	static String CLEAR_SKY_NIGHT = 		"0 0 0 0 0 0 0 0 0 0 0 1";	
	
	static String MOD13_NDVI = 				"1 0 0 0 0 0 0 0 0 0 0 0";
	static String MOD13_EVI = 				"0 1 0 0 0 0 0 0 0 0 0 0";
	static String MOD13_VI_QUALITY = 		"0 0 1 0 0 0 0 0 0 0 0 0";
	static String MOD13_RED = 				"0 0 0 1 0 0 0 0 0 0 0 0";
	static String MOD13_NIR = 				"0 0 0 0 1 0 0 0 0 0 0 0";
	static String MOD13_BLUE = 				"0 0 0 0 0 1 0 0 0 0 0 0";
	static String MOD13_MIR = 				"0 0 0 0 0 0 1 0 0 0 0 0";
	static String MOD13_VIEW_ZENITH = 		"0 0 0 0 0 0 0 1 0 0 0 0";
	static String MOD13_SUN_ZENITH = 		"0 0 0 0 0 0 0 0 1 0 0 0";
	static String RELATIVE_AZIMUTH = 		"0 0 0 0 0 0 0 0 0 1 0 0";
	static String COMPOSITE_DOY = 			"0 0 0 0 0 0 0 0 0 0 1 0";
	static String PIXEL_RELIABILITY = 		"0 0 0 0 0 0 0 0 0 0 0 1";	
	
	static String MOD16_ET = 				"1 0 0 0";
	static String MOD16_LE = 				"0 1 0 0";
	static String MOD16_PET = 				"0 0 1 0";
	static String MOD16_PLE = 				"0 0 0 1";
	
	static String MOD44_TREE = 				"1 0 0 0";
	static String MOD44_QC = 				"0 1 0 0";
	static String MOD44_TREE_SD = 			"0 0 1 0";
	static String MOD44_CLOUD = 			"0 0 0 1";
	
	static int MOD11 = 11;
	static int MOD13 = 13;
	static int MOD16 = 16;
	static int MOD44 = 44;
	
	private int modnum;
	
	/**
	 * 
	 * @param mod
	 */
	public MRTRunner(int mod) {
		modnum = mod;
	}
	
	/**
	 * 
	 * @param paramFile pathless??
	 * @param logFile pathless??
	 */
	private void runResample(String dir, String paramFile, String logFile) {
		try {
			
			String exec = "C:/Users/Nicholas/Downloads/MRT_download_Win/MRT/bin/resample.exe";
			String arg1 = "-p"; // flag to use the parameter file
			String arg3 = "-g"; // flag to write the log file
			ProcessBuilder pb = new ProcessBuilder(exec, arg1, paramFile, arg3, logFile);
			Map<String, String> env = pb.environment();
			env.put("MRT_HOME", "C:/Users/Nicholas/Downloads/MRT_download_Win/MRT");
			env.put("MRT_DATA_DIR", "C:/Users/Nicholas/Downloads/MRT_download_Win/MRT/data");
			env.put("Path", "C:/Users/Nicholas/Downloads/MRT_download_Win/MRT/bin;%Path%");
			pb.directory(new File(dir));
			
			File log = new File( dir+"/"+"java_process_builder.log");
			pb.redirectErrorStream(true);
			pb.redirectOutput(Redirect.appendTo(log));
			Process p = pb.start();
			assert pb.redirectInput() == Redirect.PIPE;
			assert pb.redirectOutput().file() == log;
			assert p.getInputStream().read() == -1;

			p.waitFor();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param paramFile pathless??
	 * @param outputFile full path
	 */
	private void runMosaic(String dir, String fileList, String outputFile, String subset) {
		try {
			
			String exec = "C:/Users/Nicholas/Downloads/MRT_download_Win/MRT/bin/mrtmosaic.exe";
			String arg1 = "-i"; // flag to use the fileList ??
			String arg3 = "-s"; // spectral subset
			String arg4 = "\""+subset+"\""; // quoted subset array
			String arg5 = "-o"; // output filename flag ??
			ProcessBuilder pb = new ProcessBuilder(exec, arg1, fileList, arg3, arg4, arg5, outputFile);
			Map<String, String> env = pb.environment();
			env.put("MRT_HOME", "C:/Users/Nicholas/Downloads/MRT_download_Win/MRT");
			env.put("MRT_DATA_DIR", "C:/Users/Nicholas/Downloads/MRT_download_Win/MRT/data");
			env.put("Path", "C:/Users/Nicholas/Downloads/MRT_download_Win/MRT/bin;%Path%");
			pb.directory(new File(dir));

			File log = new File( dir+"/"+"java_process_builder.log");
			pb.redirectErrorStream(true);
			pb.redirectOutput(Redirect.appendTo(log));
			Process p = pb.start();
			assert pb.redirectInput() == Redirect.PIPE;
			assert pb.redirectOutput().file() == log;
			assert p.getInputStream().read() == -1;
			
			p.waitFor();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param paramFileName
	 * @param inputFileName
	 * @param outputFileName
	 * @param spectralSubset
	 * @throws Exception
	 */
	public void writeParamFile(String paramFileName, String inputFileName, String outputFileName) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(paramFileName));
		writer.write("INPUT_FILENAME = "+inputFileName);
		writer.newLine();
		writer.write("OUTPUT_FILENAME = "+outputFileName);
		writer.newLine();
		writer.write("OUTPUT_PROJECTION_TYPE = GEOGRAPHIC");
		writer.newLine();
		writer.write("OUTPUT_PROJECTION_PARAMETERS = (");
		writer.newLine();
		writer.write(" 0.0 0.0 0.0");
		writer.newLine();
		writer.write(" 0.0 0.0 0.0");
		writer.newLine();
		writer.write(" 0.0 0.0 0.0");
		writer.newLine();
		writer.write(" 0.0 0.0 0.0");
		writer.newLine();
		writer.write(" 0.0 0.0 0.0 )");
		writer.newLine();
		writer.write("RESAMPLING_TYPE = NEAREST_NEIGHBOR");
		writer.newLine();
		writer.write("DATUM = WGS84");
		writer.newLine();
		writer.close();
	}
	
	/**
	 * 
	 * @param dirPath
	 * @throws Exception
	 */
	public String buildMosaicList(String dirPath) throws Exception {
		String listFile = dirPath+"/"+"fileList.prm";
		BufferedWriter writer = new BufferedWriter(new FileWriter(listFile));
		File dir = new File(dirPath);
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File f : files) {
				if (f.getName().endsWith(".hdf")) {
					writer.write(f.getAbsolutePath().replace("\\", "/"));
					writer.newLine();
				}
			}
		}
		writer.close();
		return listFile;
	}
	
	/**
	 * Process a single time directory of tiles, mosaicking everything in there.
	 * @param dir
	 * @param subset
	 * @throws Exception
	 */
	public void processDir(String dir, String subset) throws Exception {
		System.out.println("Processing directory: "+dir);
		File dataDir = new File(dir);
		if (!dataDir.isDirectory()) {
			throw new Exception("processDir error: dir is not a directory");
		}
		
		String name = variableName(subset);
		String newDir = dataDir.getAbsolutePath()+"/"+name;
		System.out.println("Creating directory "+newDir);
		new File(newDir).mkdir();
		
		System.out.println("\t Building list of files to mosaic...");
		String fileList = buildMosaicList(dir);
		
		// MUST have an hdf output
		String mosaicName = dataDir.getName()+"_"+name+"_mosaic.hdf";
		System.out.println("\t Mosaicking ...");
		runMosaic(newDir, fileList, mosaicName, subset);
		
		// now, reproject and reformat
		String outputFileName = mosaicName.replace(".hdf", "_geo.tif");
		// this is the default log already written by mosaic, append to this
		String logFile = newDir+"/"+"resample.log";
		String paramFileName = newDir+"/"+"resample.prm";
		System.out.println("\t Writing param file...");
		writeParamFile(paramFileName, mosaicName, outputFileName);
		System.out.println("\t Resampling and projecting...");
		runResample(newDir, paramFileName, logFile);
		
		// clean up
		File list = new File(dir+"/"+"fileList.prm");
		list.renameTo(new File(newDir+"/"+"fileList.prm"));
		File log = new File( newDir+"/"+"java_process_builder.log");
		log.delete();
		File mosaic = new File(newDir+"/"+mosaicName);
		mosaic.delete();
	}
	
	/**
	 * Process the parent directory of a MODIS product, where subdirectories are times
	 * and inside each time directory is a set of tiles as HDF files.
	 * @param parentDir
	 * @param subset
	 */
	public void processDirs(String parentDir, String subset) {
		
		File dir = new File(parentDir);
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File f : files) {
				try {
					if (f.isDirectory()) {
						processDir(f.getAbsolutePath(), subset);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 * 
	 * @param subset
	 * @return
	 */
	public String variableName (String subset) {
		if (modnum == MOD11) {
			if (subset.equals(MRTRunner.MOD11_LST_DAY)) {
				return "LST_DAY";
			}
			if (subset.equals(MRTRunner.MOD11_LST_NIGHT)) {
				return "LST_NIGHT";
			}
			if (subset.equals(MRTRunner.CLEAR_SKY_DAY)) {
				return "CLEAR_DAY";
			}
			if (subset.equals(MRTRunner.CLEAR_SKY_NIGHT)) {
				return "CLEAR_NIGHT";
			}
			if (subset.equals(MRTRunner.MOD11_QC_DAY)) {
				return "QC_DAY";
			}
			if (subset.equals(MRTRunner.MOD11_QC_NIGHT)) {
				return "QC_NIGHT";
			}
		}
		else if (modnum == MOD13) {
			if (subset.equals(MRTRunner.MOD13_NDVI)) {
				return "NDVI";
			}
			if (subset.equals(MRTRunner.MOD13_EVI)) {
				return "EVI";
			}
			if (subset.equals(MRTRunner.COMPOSITE_DOY)) {
				return "COMPOSITE_DOY";
			}
			if (subset.equals(MRTRunner.MOD13_VI_QUALITY)) {
				return "VI_QC";
			}
			if (subset.equals(MRTRunner.PIXEL_RELIABILITY)) {
				return "VI_RELIABILITY";
			}
		}
		else if (modnum == MOD16) {
			if (subset.equals(MRTRunner.MOD16_ET)) {
				return "ET";
			}
			if (subset.equals(MRTRunner.MOD16_LE)) {
				return "LE";
			}
			if (subset.equals(MRTRunner.MOD16_PET)) {
				return "PET";
			}
			if (subset.equals(MRTRunner.MOD16_PLE)) {
				return "PLE";
			}
		}
		else if (modnum == MOD44) {
			if (subset.equals(MRTRunner.MOD44_TREE)) {
				return "TREE";
			}
			if (subset.equals(MRTRunner.MOD44_QC)) {
				return "TREE_QC";
			}
			if (subset.equals(MRTRunner.MOD44_TREE_SD)) {
				return "TREE_SD";
			}
			if (subset.equals(MRTRunner.MOD44_CLOUD)) {
				return "CLOUD";
			}
		}
		
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// MOD 11 mosaic each time point
//		String mod11TestDir = "C:/Users/Nicholas/Documents/MOD11A2/2010.01.01";
//		String outFile = "2010_01_01_test_LST_night.hdf";
//		MRTRunner runner = new MRTRunner(MRTRunner.MOD11);
//		try {
//			runner.buildMosaicList(mod11TestDir);
//			runner.runMosaic(mod11TestDir, 
//							 mod11TestDir+"/fileList.prm",
//							 outFile, 
//							 MRTRunner.MOD11_LST_NIGHT);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// mosaic test OK.
		
		// now, reproject and reformat
//		String inputFileName = mod11TestDir+"/"+outFile;
//		String outputFileName = inputFileName.replace(".hdf", "_geo.tif");
//		// this is the default log name written by mosaic
//		String logFile = mod11TestDir+"/"+"resample.log";
//		String paramFileName = mod11TestDir+"/"+"resample.prm";
//		// output of mosaic is single band
//		try {
//			runner.writeParamFile(paramFileName, inputFileName, outputFileName);
//			runner.runResample(mod11TestDir, paramFileName, logFile);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// resample OK.
		
		// test
//		String dir = "C:/Users/Nicholas/Documents/MOD11A2/2010.01.01";
//		try {
//			new MRTRunner(MRTRunner.MOD11).processDir(dir, MRTRunner.MOD11_LST_NIGHT);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// OK.  Runs (and waits) with Java 7
		
		
//		String productDir = "D:/MOD11A2";
//		new MRTRunner(MRTRunner.MOD11).processDirs(productDir, MRTRunner.MOD11_LST_NIGHT);
		// 20120331 OK
		
//		String dir = "D:/MOD11A2/2010.01.01";
//		try {
//			new MRTRunner(MRTRunner.MOD11).processDir(dir, MRTRunner.MOD11_QC_NIGHT);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// OK
		
		// clean up
//		String dir = "D:/MOD11A2/";
//		for (File f: new File(dir).listFiles()) {
//			if (f.getName().contains("2010.01.")) { continue; }
//			String newDir = f.getAbsolutePath()+"/LST_NIGHT";
//			new File(newDir).mkdir();
//			File mosaic = new File(f.getAbsolutePath()+"/"+f.getName()+"_"+"LST_NIGHT"+"_mosaic.hdf");
//			File tif = new File(f.getAbsolutePath()+"/"+f.getName()+"_LST_NIGHT_mosaic_geo.LST_Night_1km.tif");
//			File log = new File(f.getAbsolutePath()+"/"+"resample.log");
//			File prm = new File(f.getAbsolutePath()+"/"+"resample.prm");
//			File list = new File(f.getAbsolutePath()+"/"+"fileList.prm");
//			File jLog = new File(f.getAbsolutePath()+"/"+"java_process_builder.log");
//			// moves
//			tif.renameTo(new File (newDir+"/"+tif.getName()));
//			log.renameTo(new File (newDir+"/"+log.getName()));
//			prm.renameTo(new File (newDir+"/"+prm.getName()));
//			list.renameTo(new File (newDir+"/"+list.getName()));
//			// deletes
//			mosaic.delete();
//			jLog.delete();
//		}
		
//		String dir = "D:/MOD11A2/2010.01.25";
//		try {
//			new MRTRunner(MRTRunner.MOD11).processDir(dir, MRTRunner.MOD11_QC_NIGHT);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		String productDir = "D:/MOD11A2";
//		new MRTRunner(MRTRunner.MOD11).processDirs(productDir, MRTRunner.MOD11_QC_NIGHT);
		
		// Daytime 20120408
//		String productDir = "D:/MOD11A2";
//		new MRTRunner(MRTRunner.MOD11).processDirs(productDir, MRTRunner.MOD11_LST_DAY);
//		new MRTRunner(MRTRunner.MOD11).processDirs(productDir, MRTRunner.MOD11_QC_DAY);
		
		// EVI 20120616
//		String productDir = "D:/MOD13A2";
//		new MRTRunner(MRTRunner.MOD13).processDirs(productDir, MRTRunner.MOD13_EVI);
//		new MRTRunner(MRTRunner.MOD13).processDirs(productDir, MRTRunner.MOD13_VI_QUALITY);
		
//		try {
//			new MRTRunner(MRTRunner.MOD13).processDir("D:/MOD13A2/2010.04.23", MRTRunner.MOD13_EVI);
//			new MRTRunner(MRTRunner.MOD13).processDir("D:/MOD13A2/2010.04.23", MRTRunner.MOD13_VI_QUALITY);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// 20120607
//		try {
//			String productDir = "C:/Users/Nicholas/Documents/MOD16A2/";
//			new MRTRunner(MRTRunner.MOD16).processDirs(productDir, MRTRunner.MOD16_ET);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
//		try {
//			String productDir = "C:/Users/Nicholas/Documents/MOD44B.005/";
//			new MRTRunner(MRTRunner.MOD44).processDirs(productDir, MRTRunner.MOD44_TREE);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20121011
		String productDir = "D:/MOD13A2/2011";
		new MRTRunner(MRTRunner.MOD13).processDirs(productDir, MRTRunner.MOD13_EVI);
		new MRTRunner(MRTRunner.MOD13).processDirs(productDir, MRTRunner.MOD13_VI_QUALITY);
		
		
	}
	
}
