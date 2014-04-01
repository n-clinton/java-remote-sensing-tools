/**
 * 
 */
package cn.edu.tsinghua.gee;


import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageReader;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.math.DimensionMismatchException;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.EigenDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;
import org.apache.commons.math.random.JDKRandomGenerator;
import org.apache.commons.math.random.UncorrelatedRandomVectorGenerator;
import org.apache.commons.math.random.UniformRandomGenerator;
import org.apache.commons.math.stat.descriptive.MultivariateSummaryStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedMultivariateSummaryStatistics;
import org.apache.commons.math.stat.descriptive.moment.VectorialCovariance;
import org.apache.commons.math.util.MathUtils;

/**
 * @author nclinton
 *
 */
public class IterativePCATestr {
	
	protected PlanarImage image;
	protected RandomIter iter;

	/**
	 * 
	 * @param imageFileName
	 */
	public IterativePCATestr (String imageFileName) {
		System.out.println("Using input: "+imageFileName);
		ImageReader reader = null;
		try {
			// TODO: fix
			reader = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		final ParameterBlockJAI pbjImageRead;
		pbjImageRead = new ParameterBlockJAI("ImageRead");
		pbjImageRead.setParameter("Input", new File(imageFileName));
		pbjImageRead.setParameter("reader", reader);
		image = JAI.create("ImageRead", pbjImageRead);
		image.setProperty("fileName", imageFileName);
		
		iter = RandomIterFactory.create(image, null);
	}
	
	/**
	 * Initialize an array with random values, using the Ng rule of thumb.
	 * @param n
	 * @return
	 */
	public static double[][] randomInit (int n) {
		double[][] output = new double[n][n];
		double epsilon = Math.sqrt(6.0)/n;
		for (int i=0; i<n; i++) {
			for (int j=0; j<n; j++) {
				output[i][j] = Math.random()*epsilon*2.0 - epsilon;
			}
		}
		return output;
	}
	
	/**
	 * Get the means.  Not sure how to avoid this.
	 * @return
	 */
	private ArrayRealVector getMean() {
		// get stats from the entire image
		ParameterBlock pb = new ParameterBlock();
	    pb.addSource(image);   // The source image
	    pb.add(null); // ROI
	   	pb.add(1); // horizontal sampling rate
	   	pb.add(1); // vertical sampling rate
		RenderedOp mean = JAI.create("mean", pb);
		double[] meanA = (double[]) mean.getProperty("mean");
		return new ArrayRealVector(meanA);
	}
	
	
	/**
	 * 
	 * @param i (x-dimension)
	 * @param j (y-dimension)
	 * @return the pixel at (i,j) as a vector
	 */
	private ArrayRealVector getPixel(int i, int j){
		ArrayRealVector pixel = new ArrayRealVector(image.getNumBands());
		for (int b=0; b<image.getNumBands(); b++) {
			double value = iter.getSampleDouble(i, j, b);
			pixel.setEntry(b, value <= -28672 ? Double.NaN : value);
		}
		return pixel;
	}
	
	
	/**
	 * Set all the elements above the diagonal to zero.
	 * @param m
	 */
	private static RealMatrix lt(RealMatrix m) {
		// if the matrix isn't square, don't do anything
		if (m.getRowDimension() != m.getColumnDimension()) { return null; }
		for (int i=0; i<m.getRowDimension(); i++) {
			for (int j=i+1; j<m.getColumnDimension(); j++) {
				m.setEntry(i, j, 0);
			}
		}
		return m;
	}
	
	/**
	 * Garden variety PCA
	 * @return
	 */
	public double[][] traditionalPCA() {
		int m = image.getNumBands();

		VectorialCovariance cov = new VectorialCovariance(m, false);
		
		for (int i=0; i<image.getWidth(); i++) {
			for (int j=0; j<image.getHeight(); j++){
				try {
					ArrayRealVector pixel = getPixel(i,j);
					if (pixel.isNaN()) {
						System.out.println("Skipping: ("+i+","+j+"): "+pixel);
						continue;
					}
					cov.increment(pixel.getDataRef());
					//System.out.println("("+i+","+j+"): "+getPixel(i,j));
				} catch (DimensionMismatchException e) {
					e.printStackTrace();
				}
			}
		}
		RealMatrix sigma = cov.getResult();
		
//		RealVector x_bar = getMean();
//		RealMatrix sigma = new Array2DRowRealMatrix(new double[m][m]);
//		int n = image.getHeight()*image.getWidth();
//		double nRecip = 1.0/n;
//		for (int i=0; i<image.getWidth(); i++) {
//			for (int j=0; j<image.getHeight(); j++){
//				System.out.println("("+i+","+j+")");
//				RealVector x = getPixel(i,j).subtract(x_bar);
//				sigma.add(x.outerProduct(x).scalarMultiply(nRecip));
//			}
//		}
		
		System.out.println(cov.getN()+" pixels processed.");
		System.out.println(sigma);
		EigenDecompositionImpl eigs = new EigenDecompositionImpl(sigma, MathUtils.SAFE_MIN);
		System.out.println(new ArrayRealVector(eigs.getRealEigenvalues()));
		// not sure why the signs differ from the ENVI impl
		RealMatrix V = eigs.getV();
		System.out.println(V);
		return V.getData();
	}
	
	
	/**
	 * Iterative PCA based on Kim et al. 2005.  See eqn. 3.
	 * @param imageFileName
	 * @return
	 */
	public double[][] generalizedHebbian() {
		RealVector x_bar = getMean();
		System.out.println(x_bar);
		// randomly initialize the matrix of eigenvectors
		double[][] output = IterativePCATestr.randomInit(image.getNumBands());
		// initialize W with random values.  This is W_0
		RealMatrix W = new Array2DRowRealMatrix(output, false);
		// learning rate.  Not clear how to optimize this.
		//double rate = MathUtils.SAFE_MIN;
		double rate = 0.000001;
		try {
			
			for (int i=500; i<image.getWidth()-500; i++) {
				for (int j=500; j<image.getHeight()-500; j++){
					System.out.println("("+i+","+j+")");
//			for (int i=2000; i<2005; i++) {
//				for (int j=2000; j<2005; j++){
					// centered pixel vector
					RealVector x = getPixel(i,j).subtract(x_bar);
					//System.out.println(x);
					RealVector y = W.transpose().preMultiply(x); // also row vector
					//System.out.println(y);
					RealMatrix xy = x.outerProduct(y);
					//System.out.println("\t"+xy);
					RealMatrix ltyy = lt(y.outerProduct(y));
					//System.out.println("\t"+ltyy);
					//System.out.println("\t"+W.preMultiply(ltyy));
					// intermediate product
					RealMatrix update = xy.subtract( W.preMultiply(ltyy) );
					//System.out.println("\t\t" + update);
					// Kim et al. 2005 equation 3.
					W = W.add(update.scalarMultiply(rate));
					System.out.println("\t\t\t" + W);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return output;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// test landsat
		//String test = "/Users/nclinton/Documents/landsat_test_image/127036-20000629cloud_removecloud_remove";
		String test = "/Users/nclinton/Documents/GEE/modis_test_image/MOD09GA.A2010163.h12v10.005.2010165131337_stack";
		IterativePCATestr pca = new IterativePCATestr(test);
		//double[][] pcs = pca.generalizedHebbian();
		double[][] pcs = pca.traditionalPCA();
		// eigenvectors in columns
		//RealMatrix pc = new Array2DRowRealMatrix(pcs);
		//System.out.println(pc);
		
//		double[][] matrixData2 = { {1d,2d}, {2d,5d}, {1d, 7d}};
//		RealMatrix n = new Array2DRowRealMatrix(matrixData2);
//		System.out.println(n);
//		System.out.println(n.getRowDimension()+"x"+n.getColumnDimension());
//		double[] rowData = {1.0, 2.0};
//		RealVector v = new ArrayRealVector(rowData);
//		System.out.println(n.transpose().preMultiply(v));
			
//		double[][] matrixData3 = { {1d,2d,3d}, {2d,5d,7d}, {1d,7d,11d}};
//		RealMatrix n = new Array2DRowRealMatrix(matrixData3);
//		lt(n);
//		System.out.println(n);
		
		// check inversion of a 100x100 matrix
//		RealMatrix n = new Array2DRowRealMatrix(100, 1000);
//		UncorrelatedRandomVectorGenerator generator = new UncorrelatedRandomVectorGenerator(100, 
//													  new UniformRandomGenerator(
//													  new JDKRandomGenerator()));
//		for (int c=0; c<n.getColumnDimension(); c++) {
//			n.setColumn(c, generator.nextVector());
//		}
//		
//		RealMatrix sig = n.transpose().preMultiply(n);
//		//System.out.println(sig);
//		EigenDecompositionImpl eigs = new EigenDecompositionImpl(sig, MathUtils.SAFE_MIN);
//		RealMatrix V = eigs.getV();
//		System.out.println(V);

		
		
	}

}
