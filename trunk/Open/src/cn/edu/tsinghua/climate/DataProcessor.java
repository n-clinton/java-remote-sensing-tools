/**
 * 
 */
package cn.edu.tsinghua.climate;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.media.jai.PlanarImage;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.JAIUtils;


/**
 * @author nclinton
 *
 */
public class DataProcessor {

	/**
	 * 
	 * @param filename
	 * @return
	 */
	private static double[] getScaleOffset(String filename) {
		Dataset data = GDALUtils.getDataset(filename);
		String sString = data.GetMetadataItem("SCALE");
		String oString = data.GetMetadataItem("OFFSET");
		System.out.println("slope="+sString+", offset="+oString);
		return new double [] {
			Double.parseDouble(sString), Double.parseDouble(oString)
		};
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// geotiffs:
//		String monthFile = "/Users/nclinton/Documents/"
//				+ "GlobalPhenology/climate_anomalies/precip/NCCCSM_1PTO2X_1_pr-change_o0001-0030/"
//				+ "NCCCSM_1PTO2X_1_pr-change_o0001-0030_01.tif";
//		double[] so = getScaleOffset(monthFile);
//		PlanarImage in = JAIUtils.readImage(monthFile);
//		PlanarImage res = JAIUtils.rescale(in, so);
//		JAIUtils.writeImage(res, monthFile.replace(".tif", "_rescale.tif"));
		
		File dir = new File("/Users/nclinton/Documents/"
				+ "GlobalPhenology/climate_anomalies/precip/NCCCSM_1PTO2X_1_pr-change_o0001-0030/");
		dir = new File("/Users/nclinton/Documents/"
				+ "GlobalPhenology/climate_anomalies/temp/NCCCSM_1PTO2X_1_tas-change_o0001-0030");
		File[] files = dir.listFiles();
		ArrayList<PlanarImage> toProcess = new ArrayList<PlanarImage>();
		String file0 = null;
		for (File f : files) {
			if (f.getName().endsWith(".tif")) {
				if (f.getName().endsWith("_01.tif")) {
					file0 = f.getPath();
				}
				PlanarImage pi = JAIUtils.readImage(f.getAbsolutePath());
				toProcess.add(JAIUtils.rescale(pi, getScaleOffset(f.getAbsolutePath())));
			}
		}
		
		PlanarImage mean = JAIUtils.mean(toProcess.toArray(new PlanarImage[toProcess.size()]));
		String outName = dir.getAbsolutePath()+"/"+dir.getName()+"_mean.tif";
		JAIUtils.writeImage(mean, outName);
		GDALUtils.transferGeo(file0, outName);
	}

}
