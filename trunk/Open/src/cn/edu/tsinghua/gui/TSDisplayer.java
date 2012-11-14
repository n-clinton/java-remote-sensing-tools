/**
 * 
 */
package cn.edu.tsinghua.gui;

import java.awt.Button;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.stat.StatUtils;

import com.berkenviro.imageprocessing.SplineFunction;

import cn.edu.tsinghua.timeseries.Extremum;
import cn.edu.tsinghua.timeseries.TSUtils;

import ru.sscc.spline.Spline;
import ru.sscc.spline.polynomial.PSpline;

import JSci.awt.DefaultGraph2DModel;
import JSci.awt.Graph2D;
import JSci.awt.DefaultGraph2DModel.DataSeries;
import JSci.swing.JLineGraph;

/**
 * @author Nicholas Clinton
 *
 */
public class TSDisplayer extends JFrame implements ActionListener {

	private JTextArea stats;
	private JLineGraph graph;
	private DefaultGraph2DModel model;
    private Button splineButton = new Button("dSpline");
    private Button splineButton2 = new Button("pSpline");
    private Button clearButton = new Button("Clear Last");
    private Button calcButton = new Button("Calculate");
    private Button meanButton = new Button("Mean");
    private Button ratioButton = new Button("Ratio");
    private int numSeries;
    
    private double[][] series;
    private double[][] scaledSeries;
     
    /**
     * 
     * @param minX
     * @param maxX
     * @param minY
     * @param maxY
     */
    public TSDisplayer(double[][] timeseries) {
    	super("Displayer");
    	setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            
    	series = timeseries;
    	numSeries = 0;
    	
    	model = new DefaultGraph2DModel();
    	model.addSeries(new DataSeries(new double[] {0}, new double[] {0})); // dummy series
        graph = new JLineGraph(model);
        graph.setGridLines(true);
        graph.setMarker(new Graph2D.DataMarker.Circle(3));
        add(graph);
        
    	stats = new JTextArea(10,20);
        add(stats,"South");
            
        Panel buttonPanel = new Panel();
        buttonPanel.add(splineButton);
        splineButton.addActionListener(this);
        buttonPanel.add(splineButton2);
        splineButton2.addActionListener(this);
        buttonPanel.add(clearButton);
        clearButton.addActionListener(this);
        buttonPanel.add(calcButton);
        calcButton.addActionListener(this);
        buttonPanel.add(meanButton);
        meanButton.addActionListener(this);
        buttonPanel.add(ratioButton);
        ratioButton.addActionListener(this);
        add(buttonPanel,"North");
       
        pack();
        setVisible(true);
    }
    
    /**
	 * 
	 * @param minX
	 * @param maxX
	 * @param minY
	 * @param maxY
	 */
    private void initGraph(double minX, double maxX, double minY, double maxY) {
    	graph.setXExtrema((float)Math.min(graph.getXMinimum(), minX), (float)Math.max(graph.getXMaximum(), maxX));
		graph.setYExtrema((float)Math.min(graph.getYMinimum(), minY), (float)Math.max(graph.getYMaximum(), maxY));

	    graph.setXIncrement((graph.getXMaximum() - graph.getXMinimum()) / 10.0f);
	    graph.setYIncrement((graph.getYMaximum() - graph.getYMinimum()) / 10.0f);
		model.setXAxis(graph.getXMinimum(), graph.getXMaximum(), 10);
    }
    
	/**
	 * 
	 */
	public void graphSeries() {
		double minX = StatUtils.min(series[0]);
		double maxX = StatUtils.max(series[0]);
		double minY = StatUtils.min(series[1]);
		double maxY = StatUtils.max(series[1]);

		initGraph(minX, maxX, minY, maxY);
		
		model.addSeries(new DataSeries(series[0], series[1]));
		numSeries++;
		graph.redraw();
		repaint();
	}
    
	/**
	 * 
	 */
	public void graphScaledSeries() {
		
		scaledSeries = new double[2][series[0].length];
		for (int i=0; i<series[0].length; i++) {
			scaledSeries[0][i] = series[0][i];
			scaledSeries[1][i] = series[1][i]/10000.0;
		}
		
		double minX = StatUtils.min(scaledSeries[0]);
		double maxX = StatUtils.max(scaledSeries[0]);
		double minY = StatUtils.min(scaledSeries[1]);
		double maxY = StatUtils.max(scaledSeries[1]);
		
		initGraph(minX, maxX, minY, maxY);
		
		model.addSeries(new DataSeries(scaledSeries[0], scaledSeries[1]));
		numSeries++;
		graph.redraw();
		repaint();
	}
    
	/**
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == clearButton) {
			if (numSeries > 0) {
				// remove the last series
				model.removeSeries(numSeries);
				numSeries--;
				graph.redraw();
				repaint();
			}
		}
		else if (e.getSource() == splineButton) {
			fitSpline();
		}
		else if (e.getSource() == splineButton2) {
			fitSpline2();
		}
//		else if (e.getSource() == calcButton) {
//			calcStats();
//		}
//		else if (e.getSource() == meanButton) {
//			calcMean();
//		}
//		else if (e.getSource() == ratioButton) {
//			calcRatio();
//		}
	}
	
	/**
	 * 
	 */
	private void fitSpline() {
		try {
			// splines on the original data, un-scaled
			Spline dSpline = TSUtils.duchonSpline(series[0], series[1]);
			double[] minMax = getXRange();
			// smooth with an FFT
			double[][] smooth1 = TSUtils.smoothFunction(dSpline, minMax[0], minMax[1], 0.1);
			// smooth with Savitzky-Golay
			double[][] smooth2 = TSUtils.sgSmooth(series, 5, 2);
            // for display
			if (scaledSeries != null) { // display only
				graphSpline(TSUtils.duchonSpline(scaledSeries[0], scaledSeries[1]), true);
				//graphSpline(TSUtils.polynomialSpline(scaledSeries[0], scaledSeries[1], 1), true);
			} else {
	            graphSpline(dSpline, true);
	            graphSeries(smooth1);
	            graphSeries(smooth2);
	            //graphSpline(pSpline, true);
			}
            // derivatives
//            Spline myDerivative = PSpline.derivative(pSpline);
//            this.graphSpline(myDerivative, false);
//            // graph the second derivative, too
//            splineVals = TSUtils.splineValues(myDerivative, xRange);
//            pSpline = TSUtils.polynomialSpline(splineVals[0], splineVals[1], 1);
//            Spline secondDeriv = PSpline.derivative(pSpline);
//            this.graphSpline(secondDeriv, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Fit a spline to the last series
	 */
	private void fitSpline2() {
		try {
			SplineFunction polySpline = new SplineFunction(series);
            // for display
			double[] minMax = getXRange();
			// smooth with an FFT
			double[][] smooth1 = TSUtils.smoothFunction(polySpline, minMax[0], minMax[1], 0.1);
			// smooth with Savitzky-Golay
			double[][] smooth2 = TSUtils.sgSmooth(series, 5, 2);
			if (scaledSeries != null) { // display only
				graphSpline(new SplineFunction(scaledSeries), true);
			} else {
	            graphSpline(polySpline, true);
	            graphSeries(smooth1);
	            graphSeries(smooth2);
			}
            // derivatives
//            UnivariateRealFunction myDerivative = polySpline.derivative();
//            graphSpline(myDerivative, false);
//            // graph the second derivative, too ????????????????????
//            double[][] derivativeVals = TSUtils.splineValues(myDerivative, getXRange());
//            SplineFunction derivSpline = new SplineFunction(derivativeVals);
//            UnivariateRealFunction secDerivative = derivSpline.derivative();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Graph a Spline or a SplineFunction.
	 * @param spline
	 */
	public void graphSpline(Object spline, boolean rescale) {
		double[] xRange = getXRange();
		double[][] splineVals = null;
		try {
			splineVals = TSUtils.splineValues(spline, xRange);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (rescale) {
			double minX = StatUtils.min(splineVals[0]);
			double maxX = StatUtils.max(splineVals[0]);
			double minY = StatUtils.min(splineVals[1]);
			double maxY = StatUtils.max(splineVals[1]);
			initGraph(minX, maxX, minY, maxY);
		}
		graphSeries(splineVals);
	}
	
	/**
	 * 
	 * @param series
	 */
	public void graphSeries(double[][] series) {
		model.addSeries(new DataSeries(series[0], series[1]));
		numSeries++;
		graph.redraw();
		repaint();
	}
	
	/**
	 * 
	 * @param text
	 */
	public void addText(String text) {
		stats.append(text+"\n");
		this.repaint();
	}
	
	/*
	 * This method calculates various phenology stats from the 
	 * Spline instance variable.
	 */
//	private void calcStats() {
//		if (mySpline != null) {
//			stats.append("Roots: \n");
//			// set up the display strings
//			String roots = "{";
//			String maxima = "{";
//			String minima = "{";
//			// get the List of extrema
//			List extrema = TSUtils.getExtrema(myDerivative, getXRange());
//			Extremum e;
//			// iterate over the List
//			for(int i=0; i<extrema.size(); i++) {
//				e = (Extremum)extrema.get(i);
//				roots += doubleString(e.getX())+", ";
//				if (e.getType() == Extremum.EXTREMUM_TYPE_MAX) {
//					maxima += doubleString(e.getX())+", ";
//				}
//				else {
//					minima += doubleString(e.getX())+", ";
//				}
//			}
//			
//			// finish the strings and append
//			roots = roots.substring(0, roots.length()-2) + "}";
//			if (maxima.length() > 1) {
//				maxima = maxima.substring(0, maxima.length()-2) + "}";
//			}
//			else { maxima+="}"; }
//			if (minima.length() > 1) {
//				minima = minima.substring(0, minima.length()-2) + "}";
//			}
//			else { minima+="}"; }
//			
//			stats.append(roots + "\n");
//			stats.append("Maxima: \n");
//			stats.append(maxima + "\n");
//			stats.append("Minima: \n");
//			stats.append(minima + "\n");
//			
//			// add the X values at which the max and min NDVI occur
//			double[][] minmax = TSUtils.absoluteExtrema(mySpline, getXRange());
//			stats.append("Absolute minimum = " + doubleString(minmax[0][0]) + "\n");
//			stats.append("Absolute maximum = " + doubleString(minmax[1][0]) + "\n");
//			
//			// calculate and display the largest two fluctuations
//			stats.append("MinX1, MinY1, MaxX1, MaxY1, MinX2, MinY2, MaxX2, MaxY2} \n");
//			double[] extremaArr = TSUtils.evaluateExtrema(extrema, mySpline, getXRange());
//			String fluctuations = "";
//			for (int j=0; j<extremaArr.length; j++) {
//				fluctuations+= doubleString(extremaArr[j])+", ";
//			}
//			stats.append(fluctuations+"\n");
//			this.repaint();
//		}
//	}
	
	/*
	 * This method adds the running mean to the graph and reports the
	 * Reed et al. onset of greenness metric.
	 */
//	private void calcMean() {
//		if (mySpline != null) {
//			double[][] mean = TSUtils.runningMean(mySpline, 42, getXRange());
//			graphSeries(mean[0], mean[1]);
//			
//			double xGreen = TSUtils.greenUpReed(mySpline, getXRange());
//			stats.append("Reed green-up index = "+doubleString(xGreen)+"\n");
//			this.repaint();
//		}
//	}
	
	/*
	 * This method adds the ratio to the graph and reports the 
	 * White et al. onset of greeness metric.
	 */
//	private void calcRatio() {
//		if (mySpline != null) {
//			double[][] ratio = TSUtils.ndviRatio(mySpline, getXRange());
//			graphSeries(ratio[0], ratio[1]);
//			
//			double xWhite = TSUtils.greenUpWhite(mySpline, getXRange());
//			stats.append("White green-up index = "+doubleString(xWhite)+"\n");
//			this.repaint();
//		}
//	}
	
	/*
	 * Helper method to format a Double
	 */
	private String doubleString(double d) {
		String dString = String.valueOf(d);
		int dotIndex = dString.indexOf(".");
		int maxIndex = (dotIndex+4 > dString.length()-1) ? dString.length()-1 : dotIndex+4;
		return dString.substring(0, dotIndex+1)+dString.substring(dotIndex+1, maxIndex);
	}
	
	/*
	 * Helper method to return the xRange
	 */
	private double[] getXRange() {
		return new double[] {StatUtils.min(series[0]), StatUtils.max(series[0])};
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	}

}
