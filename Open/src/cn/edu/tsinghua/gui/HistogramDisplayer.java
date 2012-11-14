/**
 * 
 */
package cn.edu.tsinghua.gui;

import JSci.awt.DefaultGraph2DModel;
import JSci.awt.Graph2DModel;
import JSci.awt.DefaultGraph2DModel.DataSeries;
import JSci.swing.JHistogram;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import cn.edu.tsinghua.timeseries.TrainingProcessr;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

/**
 * @author Nicholas Clinton
 *
 */
public class HistogramDisplayer extends JFrame implements ActionListener {

	
	private JHistogram histo;
	private Graph2DModel model;
	private int numSeries;
	
	/**
	 * @param title
	 * @throws HeadlessException
	 */
	public HistogramDisplayer(String title, Graph2DModel gm) {
		super(title);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		model = gm;
		histo = new JHistogram(model);
		
		this.getContentPane().add(histo, BorderLayout.CENTER);
		
		pack();
        setVisible(true);
	}
	
	/*
	 * Helper method
	 */
	public static DefaultGraph2DModel makeFromData(double[] x, double[] y) {
		DefaultGraph2DModel model = new DefaultGraph2DModel();
        model.setXAxis((float)x[Utils.minIndex(x)], 
        			   (float)x[Utils.maxIndex(x)], 
        				10);
        model.addSeries(new DataSeries(x, y));

        return model;
	}
	
	/*
	 * Make frequency in bins
	 */
	public static double[][] makeBins(double[] x, int  nBins) {
		double[] y = new double[nBins+1];
		double[] xBins = new double[nBins+1];
		double minX  = x[Utils.minIndex(x)];
		double maxX = x[Utils.maxIndex(x)];
		// From JSci API:
		/* The y-values are the counts for each bin. 
		 * Each bin is specified by an interval. 
		 * So that y[i] contains the counts for the bin from x[i-1] to x[i]. 
		 * The value of y[0] is disregarded.
		 */
		double increment = (maxX - minX) / (double) nBins;
		for (int i=0; i<x.length; i++) {
			for (int j=1; j<nBins+1; j++) {
				xBins[j-1] = minX + (j-1)*increment;
				xBins[j] = minX + j*increment;
				if (x[i] >= xBins[j-1] && x[i] <= xBins[j]) {
					y[j]++;
				}
				
			}
		}
		double[][] returnArr = {xBins, y};
		return returnArr;
	}
	
	/**
	 * @throws HeadlessException
	 */
	public HistogramDisplayer() throws HeadlessException {
		super();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		model = new DefaultGraph2DModel();
		histo.setModel(model);
		pack();
        setVisible(true);
	}

	/**
	 * @param gc
	 */
	public HistogramDisplayer(GraphicsConfiguration gc) {
		super(gc);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param title
	 * @throws HeadlessException
	 */
	public HistogramDisplayer(String title) throws HeadlessException {
		super(title);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		model = new DefaultGraph2DModel();
		histo.setModel(model);
		
		pack();
        setVisible(true);
	}

	/**
	 * @param title
	 * @param gc
	 */
	public HistogramDisplayer(String title, GraphicsConfiguration gc) {
		super(title, gc);
		// TODO Auto-generated constructor stub
	}

	/*
	 * For ActionListener interface
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// following is the implementation of this class for the training data
		String fileString = "F:\\NASA_Ames\\training\\combo_train1_nom.arff";
		Instances train1 = TrainingProcessr.loadInstances(fileString);
		train1.setClass(train1.attribute("abundance"));
		Instances present = new Instances(train1, 0);
		Instances absent = new Instances(train1, 0);
		for (int i=0; i<train1.numInstances(); i++) {
			// looks like this long thing is necessary
			if (train1.classAttribute().value((int)train1.instance(i).classValue()).equals("none")) {
				absent.add((Instance)train1.instance(i).copy());
			}
			else {
				present.add((Instance)train1.instance(i).copy());
			}
		}
		
		// these hold the values to histogram
		double[] presData = new double[present.numInstances()];
		double[] absData = new double[absent.numInstances()];
		
		// this is the attribute to use
		Attribute a = train1.attribute("whiteX");
		
		// fill the arrays with the values of the histogram
		for (int i=0; i<present.numInstances(); i++) {
			presData[i] = present.instance(i).value(a);
		}
		for (int i=0; i<absent.numInstances(); i++) {
			absData[i] = absent.instance(i).value(a);
			//System.out.println(absData[i]);
		}
		
		// convert to counts
		double[][] histDataAbs = makeBins(absData, 20);
		double[][] histDataPres = makeBins(presData, 20);
		// make a Graph2DModel
		DefaultGraph2DModel model = makeFromData(histDataAbs[0], histDataAbs[1]);
		// instantiate the displayer
		HistogramDisplayer hd = new HistogramDisplayer(a.name()+ " : absent", model);
		model = makeFromData(histDataPres[0], histDataPres[1]);
		HistogramDisplayer hd2 = new HistogramDisplayer(a.name()+ " : present", model);
		
	}


}
