/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;

import ru.sscc.spline.Spline;

/**
 * @author nclinton
 *
 */
public class SplineTester {

	/*
	 * Reads the file into arrays
	 */
	public static double[][] readFile(String filename) {
		double[][] returnArr = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = null;
			// first line is header, read # of tokens
			line = br.readLine();
			StringTokenizer st = new StringTokenizer(line, ",", false);
			int count = 0;
			System.out.println("Processing dates: ");
			while (st.hasMoreTokens()) {
				// blow through the header
				System.out.print(st.nextToken() + " ");
				count++;
			}
			System.out.println();
			
			double[] curArray = null;
			// now do the series, one for each line
			// keep them in an arrayList for now
			ArrayList seriesList = new ArrayList();
			while ( (line=br.readLine()) != null ) {
				curArray = new double[count];
				st = new StringTokenizer(line, ",", false);
				int index = 0;
				while (st.hasMoreTokens()) {
					curArray[index] = Double.parseDouble(st.nextToken());
					//System.out.print(curArray[index] + " ");
					index++;
				}
				//System.out.println();
				seriesList.add(curArray);
			}
			// turn the list of double[]'s into a double[][]
			returnArr = new double[seriesList.size()][];
			returnArr = (double[][])seriesList.toArray(returnArr);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnArr;
	}
	
	/*
	 * Leave one out RMSE.  Observe dual methods for checking.
	 */
	public static double[] rmseOne(double[][] censored) {
		double mse = 0;
		int count = 0;
		for (int t=0; t<censored[0].length; t++) {
			// update the mse
			mse+=sqErrI(t, censored);
			count++;
		}

		//	adjust for mean
		mse = mse / count;
		//System.out.println("Count for rmseOne = "+count);
		return new double[] {count, Math.sqrt(mse)};
	}
	
	/*
	 * Leave n out RMSE.  Slides a leaving out window over the series.
	 */
	public static double[] rmseN(double[][] censored, int n) {
		double mse = 0;
		int count = 0;
		double[] errs;
		
		// slide a window of size n, starting at the end
		for (int l=censored[0].length-1; l>=0; l--) {
			// stay inbounds
			int lowbound = (l-n+1 < 0) ? 0 : l-n+1;
			errs = sqErrsIJ(lowbound, l, censored);
			for (int p=0; p<errs.length; p++) {
				mse+=errs[p];
				count++;
			}
		}
		
		mse = mse/count;
		System.out.println("count for rmseN = "+ count);
		return new double[] {count, Math.sqrt(mse)};
	}
	
	
	/*
	 * Permute, then use the window of size n method.
	 */
	public static double[] rmseSampleN(double[][] censored, int n) {

		// first, permute
		double[][] shuffled = permute(censored);
		
		double mse = 0;
		int count = 0;
		double[] errs;
		for (int l=shuffled[0].length-1; l>=0; l--) {
			int lowbound = (l-n+1 < 0) ? 0 : l-n+1;
			errs = sqErrsIJ(lowbound, l, shuffled);
			for (int p=0; p<errs.length; p++) {
				mse+=errs[p];
				count++;
			}
		}

		mse = mse/count;
		//System.out.println("count for rmseSampleN = "+ count);
		return new double[] {count, Math.sqrt(mse)};
		
	}
	
	/*
	 * Leave n out RMSE.  Repeatedly permute.
	 */
	public static double[] rmseSampleN2(double[][] censored, int n) {
		
		double mse = 0;
		int count = 0;
		double[] errs = new double[n];
		
		// take many permutations\
		for(int r = 0; r < 100; r++) {
			errs = sqErrsSampleN(n, censored);
			for (int p=0; p<n; p++) {
				mse+=errs[p];
				count++;
			}
		}

		mse = mse/count;
		//System.out.println("count for rmseSampleN2 = "+ count);
		return new double[] {count, Math.sqrt(mse)};
		
	}
	
	/*
	 * Returns the square error resulting from a random sample 
	 * of size n w/o replacement. Moving window method.
	 */
	public static double[] sqErrsSampleN (int n, double[][] censored) {
		// first, permute
		double[][] shuffled = permute(censored);
		return sqErrsIJ(0, n, shuffled);
	}
	
	
	/*
	 * Helper returns a pseudo-random permutation of input arrays
	 */
	 public static double[][] permute(double[][] shuffleMe) {
		 
		 // length
		 int n = shuffleMe[0].length;
		 // the return
		 double[][] shuffled = new double[shuffleMe.length][n];
		 ArrayList list = new ArrayList();
		 // fill with indices in order
		 for(int i=0; i<n; i++) {
			 list.add(i, new Integer(i));
		 }
		 
		 // fill up the shuffled one by sampling from the list
		 Random rand = new Random();
		 int count = 0;
		 while (list.size()>0) {
			 //System.out.println("list.size() = "+list.size());
			 int r = rand.nextInt(list.size());
			 //System.out.println("r = "+r);
			 int newIndex = ((Integer) list.get(r)).intValue();
			 list.remove(r);
			 shuffled[0][count] = shuffleMe[0][newIndex];
			 shuffled[1][count] = shuffleMe[1][newIndex];
			 count++;
		 }
		 
		 return shuffled;
	}

	
	/*
	 * Leave index==i out Squared Error
	 */
	public static double sqErrI(int i, double[][] censored) {

		double[] xCens = censored[0];
		double[] yCens = censored[1];
			
		// remove i, fit a spline to the remaining, compute error
		double[] xFit, yFit;
		// remove
		xFit = remove(i, xCens);
		yFit = remove(i, yCens);

		// fit a Spline to the non-hold outs
		Spline mySpline = TSUtils.duchonSpline(xFit, yFit);

		// compute error
		double err = Math.pow((mySpline.value(xCens[i]) - yCens[i]), 2);
		return err;
	}
	
	/*
	 * Leave i to j out Squared Error
	 */
	public static double[] sqErrsIJ(int i, int j, double[][] censored) {
		
		double[] xCens = censored[0];
		double[] yCens = censored[1];
			
		// remove. the original data is not changed
		double[] xFit, yFit;
		xFit = removeIJ(i, j, xCens);
		yFit = removeIJ(i, j, yCens);
		
		// fit a Spline to the non-hold outs
		Spline mySpline = TSUtils.duchonSpline(xFit, yFit);
		
		// compute the error
		int n = j-i+1;
		double[] errs = new double[n];
		
		// iterate 
		for (int index=0; index<n; index++) {
			errs[index] = Math.pow((mySpline.value(xCens[index+i]) - yCens[index+i]), 2);
		}

		return errs;
	}
	
	/*
	 * Helper from Sun forum, modded to remove index==i, and...
	 */
	public static double[] remove(int i, double[] a) {	
		// basic check:
		if(a.length == 1) {
			return null;
		}
		double[] b = new double[a.length-1];

		if (i!=0) { // not the first one
			System.arraycopy( a, 0, b, 0, i );
		}
		if (i!=a.length-1) { // not last one
			System.arraycopy(a, i+1, b, i, b.length-i );
		}
		return b;
    }

	/*
	 * Modded helper.  i is starting index and j is end index.
	 */
	public static double[] removeIJ(int i, int j, double[] a) {	
		// check for trivial
		if ( i == j ) {
			return remove(i, a);
		}
		
		double[] b = new double[a.length-(j-i+1)];
		
		if (i==0) { // i is first one, just copy the last part
			System.arraycopy( a, j+1, b, 0, b.length );
			return b;
		}
		else if (j==a.length-1) {  // j is last one, just copy the beginning
			System.arraycopy( a, 0, b, 0, b.length );
			return b;
		}
		
		// somewhere in the middle
		System.arraycopy( a, 0, b, 0, i);
		System.arraycopy( a, j+1, b, i, a.length-(j+1) );
		
		return b;
    }
	
	/*
	 * Write out a table of stats.
	 */
	public static void writeTable(String inData, String outTable, int _n) {
		// load the input
		System.out.println("Reading from file "+inData);
		double[][] d = readFile(inData);
		
		BufferedWriter writer = null;
		try {
			
			writer = new BufferedWriter(new FileWriter(new File(outTable)));
			// Data Labels for header row
			String dataLabels = "series"+"\t"+"n"+"\t"+"count1"+"\t"+"rmse1"
												 +"\t"+"countN"+"\t"+"rmseN"
												 +"\t"+"countNS"+"\t"+"rmseNS"
												 +"\t"+"countNS2"+"\t"+"rmseNS2";
			writer.write(dataLabels);
			writer.newLine();
			writer.flush();	
			
			for (int i=0; i<d.length; i++) {
				
				// censor
				double[][] censored = TrainingMakr5.censorZero(d[i]);
				double[] rmse, rmse1;
				rmse1 = rmseOne(censored); // use this as reference
				
				// iterate over many n
				for(int n=1; n<=_n; n++) {
					String line = "";
					line += i+"\t"+n+"\t"+rmse1[0]+"\t"+rmse1[1]+"\t";
					rmse = rmseN(censored, n);
					line += rmse[0]+"\t"+rmse[1]+"\t";
					rmse = rmseSampleN(censored, n);
					line += rmse[0]+"\t"+rmse[1]+"\t";
					rmse = rmseSampleN2(censored, n);
					line += rmse[0]+"\t"+rmse[1];
					writer.write(line);
					writer.newLine();
					writer.flush();	
					System.out.println(line);
				}
			
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Test code
		/*
		double[] test1 = {1, 2};
		double[] test2 = {1, 2, 3, 4, 5, 6, 7};
		
		System.out.println("test1");
		for(double i : test1) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 0 from test 1");
		double[] check = remove(0, test1);
		for(double i : check) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 1 from test 1");
		check = remove(1, test1);
		for(double i : check) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("test2");
		for(double i : test2) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 0 from test 2");
		check = remove(0, test2);
		for(double i : check) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 1 from test 2");
		check = remove(1, test2);
		for(double i : check) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 2 from test 2");
		check = remove(2, test2);
		for(double i : check) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 6 from test 2");
		check = remove(6, test2);
		for(double i : check) {
			System.out.print(i+", ");
		}
		//
		System.out.println();
		System.out.println("test2");
		for(double i : test2) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 0-1 from test 2");
		check = removeIJ(0, 1, test2);
		for(double i : check) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 0-2 from test 2");
		check = removeIJ(0, 2, test2);
		for(double i : check) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 2-3 from test 2");
		check = removeIJ(2, 3, test2);
		for(double i : check) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 4-6 from test 2");
		check = removeIJ(4, 6, test2);
		for(double i : check) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 2-4 from test 2");
		check = removeIJ(2, 4, test2);
		for(double i : check) {
			System.out.print(i+", ");
		}
		System.out.println();
		System.out.println("check remove 1-5 from test 2");
		check = removeIJ(1, 5, test2);
		for(double i : check) {
			System.out.print(i+", ");
		}
		*/
		// end tests for removal methods

		
		String inTable = "I:\\NASA_Ames\\documents\\ndvi_test_pts.csv";
		String outTable = "I:\\NASA_Ames\\documents\\spline_tests_12108.txt";
		//writeTable(inTable, outTable, 20);
		outTable = "I:\\NASA_Ames\\documents\\spline_tests_12208b.txt";
		writeTable(inTable, outTable, 35);
		
	}

}
