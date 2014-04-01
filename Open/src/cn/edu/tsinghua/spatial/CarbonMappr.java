/**
 * 
 */
package cn.edu.tsinghua.spatial;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import cn.edu.tsinghua.timeseries.ImageLoadr5;

import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.JAIUtils;

/**
 * @author nclinton
 *
 */
public class CarbonMappr {

	PlanarImage pimgPET;
	PlanarImage pimgElev;
	PlanarImage pimgLC;
	
	PlanarImage soils;
	
	ImageLoadr5 loadrPrec;
	ImageLoadr5 loadrTmean;
	
	/**
	 * TODO: specify units of all these inputs
	 * @param imgPET
	 * @param imgElev
	 * @param imgLC
	 * @param precipDir
	 * @param tmeanDir
	 */
	public CarbonMappr(String imgPET, String imgElev, String imgLC, String precipDir, String tmeanDir) {

		pimgPET = JAIUtils.readImage(imgPET);
		pimgElev = JAIUtils.readImage(imgElev);
		pimgLC= JAIUtils.readImage(imgLC);		

		try {
			loadrPrec = new ImageLoadr5(new String[] {precipDir});
			loadrTmean = new ImageLoadr5(new String[] {tmeanDir});
		} catch (Exception e1) {
			e1.printStackTrace();
		}		
	}
	
	/**
	 * 
	 * @param cw
	 * @param output
	 */
	public void map(Crosswalkr cw, String output) {
		
		RandomIter iterElev = RandomIterFactory.create(pimgElev , null);
		RandomIter iterPET = RandomIterFactory.create(pimgPET , null);
		RandomIter iterLC = RandomIterFactory.create(pimgLC , null);

		// China
//		int startX = 30430;
//		int startY = 4350;
//		int endX = 37800;
//		int endY = 8650;
		int startX = 0;
		int startY = 0;
		int endX = pimgLC.getWidth();
		int endY = pimgLC.getHeight();
		

		//Output carbon	raster
		WritableRaster rasterCarbon = RasterFactory.createBandedRaster(DataBuffer.TYPE_BYTE, pimgLC.getWidth(),pimgLC.getHeight() , 1, new Point(0,0));

		for (int y=startY; y<endY; y++) {
			for (int x=startX; x<endX; x++) {
				int[] xy = new int[] {x, y};
				System.out.println("Processing pixel: "+Arrays.toString(xy));
				try {
					double[] projXY = JAIUtils.getProjectedXY(xy, pimgLC);
					//System.out.println("Processing coordinate:" + Arrays.toString(projXY));
					//					double PET = iterPET.getSampleDouble(x, y, 0);	
					//					double elev = iterElev.getSampleDouble(x, y, 0);
					//					int LC = (int)iterLC.getSampleDouble(x, y, 0);
					double pet = JAIUtils.imageValue(projXY[0], projXY[1], pimgPET, iterPET);
					//		System.out.println("PET:" + pet);
					double elev = JAIUtils.imageValue(projXY[0], projXY[1], pimgElev, iterElev);
					//System.out.println("Elevation: "+ elev);
					int lc = (int)JAIUtils.imageValue(projXY[0], projXY[1], pimgLC, iterLC);
					// transfer to IPCC code
					lc = cw.crosswalk(lc);
					//System.out.println("Landcover: "+igbp2ipcc(lcigbp));
					
					double tmean[] = loadrTmean.getY(projXY[0], projXY[1]);
					for (int i=0;i<tmean.length;i++) {
						tmean[i] = tmean[i]/10.0;
					}
					double prec[] = loadrPrec.getY(projXY[0], projXY[1]);

					/*
					 * By doing this, you are saying that the registration (ULX,ULY) and the resolution 
					 * for all these images are EXACTLY the same.  They have to be since this program 
					 * does NOT check this.
					 */
					int ecoregion = IPCCcarbon.ecoregion(prec,tmean,elev,pet,lc);
					//System.out.println("\t Ecoregion: "+ecoregion);
					// Calculate carbon output
					double biomass = IPCCcarbon.biomassStock(ecoregion, lc);
					//System.out.println("\t\t\t Estimated biomass carbon " + biomass);
					double dead = IPCCcarbon.deadStock(ecoregion, lc);
					//		System.out.println("\t\t\t Estimated detritus carbon " + dead);
					//					double soil = soilReferenceStock(ecoregion);
					double soil = IPCCcarbon.soilReferenceStock(ecoregion, lc);

					rasterCarbon.setSample(x,y,0, (byte)Math.round(biomass+dead+soil));
					System.out.println("\t Estimated TOTAL CARBON " + (biomass+dead+soil));

				} catch (Exception e) {
					e.printStackTrace();
					rasterCarbon.setSample(x,y,0, (byte)0);
				}
			}
		}
		JAIUtils.writeTiff(rasterCarbon, output);
	}
 	
 	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// input
		String imgPET = "/data/evapotranspiration/pet/pet.tif";	
		String imgElev = "/data/GMTED2010/mn30_grd/mn30_grd1.tif";
		// current setup is for a global run, uncomment for China
		//String imgLC =  "/data/GlobalLandCover/modis/2009_igbp_wgs84.tif"; 
		String precDir = "/data/WorldClim/prec";
		String tmeanDir = "/data/WorldClim/tmean";
		
		Crosswalkr cw = new Crosswalkr(Crosswalkr.FROM_2_IPCC);
		int[] years = {2010, 2020, 2030, 2040, 2050, 2060, 2070, 2080, 2090, 2100};
		for (int year : years) {
			// input
			String imgLC =  "/home/nclinton/Documents/Shenzhen_carbon/"+year+"_landcover.tif"; 
			// output
			String carbonTif = "/home/nclinton/Documents/Shenzhen_carbon/"+year+"_carbon.tif";
			CarbonMappr mappr = new CarbonMappr(imgPET, imgElev, imgLC, precDir, tmeanDir);
			mappr.map(cw, carbonTif);
			GDALUtils.transferGeo(imgLC, carbonTif);
		}
		
	}

}
