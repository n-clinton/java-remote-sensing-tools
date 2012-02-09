/**
 * 
 */
package cn.edu.tsinghua.gee;

import java.io.File;

/**
 * @author nclinton
 *
 */
public class TimeSeriesJSON {

	/**
	 * One-off for bay area EVI.
	 * @param dirName
	 * @param outJSON
	 * @param zipped
	 */
	public static void processZipDir(String dirName, String outJSON, boolean zipped) {
		File dir = new File(dirName);
		// if zipped, unzip everything
		if (zipped) {
			File[] files = dir.listFiles();
			for (File f : files) {
				Unzip.unzip(f.getPath());
			}
		}
		
		// set up the JSONwriter
		try {
			GeoJSONmakr makr = new GeoJSONmakr(outJSON);
			makr.writeDescription("MODIS EVI time series, SF bay area, downloaded from GEE.  Projection inferred from GeoTiff header.");
			//makr.writeBandDefault(new String[] {"\"proj\": \"SR-ORG:6965\""}); // doesn't like this
			makr.openBands();
			
			// open the directory, list the unzipped tiffs
			File[] files = dir.listFiles();
			int bCount = 1;
			for (File f : files) {
				String fName = f.getPath();
				// if it's a TIFF
				if (fName.endsWith(".tif") || fName.endsWith(".TIF")) {
					// String label for the band
					String bLabel = bCount<10 ? "_0"+bCount : "_"+bCount;
					String bandName = "EVI_"+fName.substring(fName.length()-18, fName.length()-8)+bLabel;
					// index N/A: always zero
					makr.writeBand(bandName, 0, fName);
					bCount++;
				}
			}
			makr.closeBandsAndFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// MODIS EVI for Bay Area
		String eviDir = "/Users/nclinton/Documents/GEE/MODIS_EVI_test/";
		String json = eviDir+"MODIS_250m_EVI_bay_area_2009_2011.json";
		processZipDir(eviDir, json, false);
		
	}

}
