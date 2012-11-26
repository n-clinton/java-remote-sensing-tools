/**
 * 
 */
package cn.edu.tsinghua.modis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.math.DimensionMismatchException;
import org.apache.commons.math.linear.EigenDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.descriptive.moment.VectorialCovariance;
import org.apache.commons.math.util.MathUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import cn.edu.tsinghua.timeseries.DatedQCImage;
import cn.edu.tsinghua.timeseries.ImageLoadr2;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.GDALUtils;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Nicholas
 *
 */
public class PCA {

	final ImageLoadr2 loadr;
	FeatureCollection<?, SimpleFeature> polygons;
	
	/**
	 * 
	 * @param imageDirs
	 * @param polyFile
	 * @throws Exception
	 */
	public PCA(String[] imageDirs, String polyFile) throws Exception {
		loadr = new ImageLoadr2(imageDirs);
		//System.out.println(loadr);
		polygons = GISUtils.getFeatureCollection(new File(polyFile));
	}
	
	/**
	 * MEMORY LEAK!!!!!!!!!!!!  GDAL.
	 * @param p
	 * @return
	 */
	private VectorialCovariance getCovariance(Geometry poly) throws Exception {
		// bounding box
		Envelope bb = poly.getEnvelopeInternal();
		// first image, use as pixel centroid reference
		DatedQCImage dImage = loadr.getI(0);
		Dataset image = GDALUtils.getDataset(dImage.getImage());
		// throw an Exception here if the polygon BB extends outside the image bounds
		int[] ul = GDALUtils.getPixelXY(new double[] {bb.getMinX(), bb.getMaxY()}, image);
		int[] lr = GDALUtils.getPixelXY(new double[] {bb.getMaxX(), bb.getMinY()}, image);
		int minX = Math.max(ul[0]-1, 0);
		int minY = Math.max(ul[1]-1, 0);
		int maxX = Math.min(lr[0]+1, image.getRasterXSize()-1);
		int maxY = Math.min(lr[1]+1, image.getRasterYSize()-1);
		
		// covariance object
		final VectorialCovariance cov = new VectorialCovariance(loadr.getLengthImages(), false);
		// asynchronous way
		int threads = Runtime.getRuntime().availableProcessors()-1;
		ExecutorService service = Executors.newFixedThreadPool(threads);
		// Give it to a completion service
		CompletionService<Boolean> ecs = new ExecutorCompletionService<Boolean>(service);
		
		// iterate over pixels in the bounding box
		int count = 0;
		for (int x=minX; x<=maxX; x++) {
			for (int y=minY; y<=maxY; y++) {
				try {
					// pixel centroid in projected coords
					double[] coords = GDALUtils.getProjectedXY(new int[] {x, y}, image);
					final Point pt = GISUtils.makePoint(coords[0], coords[1]);
					if (poly.intersects(pt)) {
						//System.out.println("processing pt: "+pt);
						
						// synchronous way, unbearably slow, no memory leak
						//cov.increment(loadr.getY(pt));
						//System.gc();  // will leak w/o this
						
						// DUMB!! Same as synchronous since the Loadr read methods are synchronized.  Must decouple read and update.
						// asynchronous way, runs only the data getting, which is synchronized
						count++;
						ecs.submit(new Callable<Boolean>() {
							@Override
							public Boolean call() {
								try {
									cov.increment(loadr.getY(pt));
									System.gc();
								} catch (Exception e) {
									e.printStackTrace();
									return new Boolean(false);
								}
								return new Boolean(true);
							}
							
						});
						
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		int check = 0;
		for (int i=0; i<count; i++) {
			if(ecs.take().get().booleanValue()) {
				check++;
			}
		}
		if (cov.getN() != check) {
			System.out.println("Jackass! N="+cov.getN()+", check="+check);
		}
		gdal.Unlink(dImage.getImage());
		return cov;
	}
	
	/**
	 * 
	 */
	public void findPCs() {
		FeatureIterator<SimpleFeature> iter = polygons.features();
		while (iter.hasNext()) {
			final SimpleFeature feature = iter.next();
			System.out.println(feature.toString());
			Geometry poly = (Geometry)feature.getDefaultGeometry();
			try {
				VectorialCovariance cov = getCovariance(poly);
				RealMatrix sigma = cov.getResult();
				RealMatrix eigs = eigenVecs(sigma);
				//System.out.println(eigs.toString());
				final double[] x = loadr.getX();
				final double[] y = eigs.getColumn(0);
//				for (int t=0; t<x.length; t++) {
//					System.out.println("\t (t,y) = ("+x[t]+","+y[t]+")");
//				}
//				SwingUtilities.invokeAndWait(new Runnable() {
//		            public void run() {
//		            	TSDisplayer disp = new TSDisplayer(new double[][] {x, y});
//		            	disp.graphSeries();
//		            	disp.addText(feature.toString());
//		            }
//		        });
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		iter.close();
	}
	
	/**
	 * 
	 * @param outTableName
	 * @throws Exception
	 */
	public void writeFirstPC(String outTableName) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outTableName));
		// header
		writer.write("id,n");
		for (int i=0; i<loadr.getLengthImages(); i++) {
			writer.write(","+i);
		}
		writer.newLine();
		// iterate over polygons
		FeatureIterator<SimpleFeature> iter = polygons.features();
		while (iter.hasNext()) {
			SimpleFeature feature = iter.next();
			//System.out.println(feature.toString());
			String line = feature.getID();
			Geometry poly = (Geometry)feature.getDefaultGeometry();
			try {
				VectorialCovariance cov = getCovariance(poly);
				line+=(","+cov.getN());
				RealMatrix sigma = cov.getResult();
				RealMatrix eigs = eigenVecs(sigma);
				//System.out.println(eigs.toString());
				double[] y = eigs.getColumn(0);
				for (int i=0; i<y.length; i++) {
					line+=(","+y[i]);
				}
				// cleanup
				cov = null;
				sigma = null;
				eigs = null;
				System.gc();
				// write out
				System.out.println(line);
				writer.write(line);
				writer.newLine();
				writer.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		iter.close();
		writer.close();
	}
	
	/**
	 * 
	 * @param sigma
	 * @return
	 */
	private RealMatrix eigenVecs(RealMatrix sigma) {
		EigenDecompositionImpl eigs = new EigenDecompositionImpl(sigma, MathUtils.SAFE_MIN);
		return eigs.getV();
	}
	
	/**
	 * 
	 * @param eigenVecs
	 * @param pcIndex
	 * @param y
	 * @return
	 */
	private double pc(RealMatrix eigenVecs, int pcIndex, double[] y) {
		double[][] eigs = eigenVecs.getData();
		double pcValue = 0;
		for (int i=0; i<eigs.length; i++) {
			pcValue+=eigs[i][pcIndex]*y[i];
		}
		return pcValue;
	}
 	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		// check
//		int threads = Runtime.getRuntime().availableProcessors()-1;
//		ExecutorService service = Executors.newFixedThreadPool(threads);
//		// Give it to a completion service
//		ExecutorCompletionService<Long> ecs = new ExecutorCompletionService<Long>(service);
//		for (int i=0; i<100; i++) {
//			ecs.submit(new Callable<Long>() {
//				public Long call() {
//					return new Long(System.currentTimeMillis());
//					
//				}
//			});
////			if (!service.isTerminated()) {
////				System.out.println("It's still running...");
////			}
//		}
//
////		if (service.isShutdown()) {
////			System.out.println("It's been shutdown.");
////		}
////		if (service.isTerminated()) {
////			System.out.println("It's been terminated.");
////		}
////		for (int i = 0; i < 100; i++) {
////	         try {
////				Long it = ecs.take().get();
////				System.out.println(it.longValue());
////			} catch (InterruptedException e) {
////				e.printStackTrace();
////			} catch (ExecutionException e) {
////				e.printStackTrace();
////			}
////	        
////	    }
////		if (service.isShutdown()) {
////			System.out.println("It's been shutdown.");
////		}
////		if (service.isTerminated()) {
////			System.out.println("It's been terminated.");
////		}
		
		// 2010, 2011 EVI
		String dir1 = "I:/MOD13A2/2010/";
		String dir2 = "I:/MOD13A2/2011/";
		String polys = "C:/Users/Nicholas/Documents/shapefiles/wwf_biomes.shp";
		String pca_check = "C:/Users/Nicholas/Documents/global_pca/wwf_biomes_pc1_parallel.txt";
		try {
			PCA pca = new PCA(new String[] {dir1, dir2}, polys);
			//pca.findPCs();
			pca.writeFirstPC(pca_check);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
