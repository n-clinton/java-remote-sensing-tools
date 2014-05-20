/**
 * 
 */
package cn.edu.tsinghua.timeseries;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * @author nclinton
 *
 */
public class DataFunction implements UnivariateFunction {

	Double[] data;
	
	/**
	 * 
	 */
	public DataFunction(List<double[]> data, int capacity) {
		this.data = new Double[capacity];
		for (double[] d : data) {
			this.data[(int)d[0]] = d[1];
		}
	}

	/** Returns a double if x is contained in this dataset.  Returns NaN otherwise.
	 * @see org.apache.commons.math3.analysis.UnivariateFunction#value(double)
	 */
	@Override
	public double value(double x) {
		if (x < 0 || x >= data.length) {
			return Double.NaN;
		}
		if (data[(int)x] != null) {
			return data[(int)x].doubleValue();
		}
		return Double.NaN;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<double[]> test = new ArrayList<double[]>();
		test.add(new double[] {1.0, 200});
		test.add(new double[] {10.0, 700});
		test.add(new double[] {13.0, 350});
		DataFunction f = new DataFunction(test, 14);
		System.out.println("200? "+f.value(1));
		System.out.println("700? "+f.value(10));
		System.out.println("350? "+f.value(13));
		System.out.println("NaN? "+f.value(0));
		System.out.println("NaN? "+f.value(-1));
		System.out.println("NaN? "+f.value(2));
		System.out.println("NaN? "+f.value(11));
		System.out.println("NaN? "+f.value(20));
		if (Double.isNaN(f.value(2))) {
			System.out.println("Hot damn!");
		}
	}

}
