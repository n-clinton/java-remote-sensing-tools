/**
 * 
 */
package cn.edu.tsinghua.gui;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;

import javax.media.jai.PlanarImage;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.math.random.RandomDataImpl;

import JSci.awt.DefaultGraph2DModel;
import JSci.awt.Graph2D;
import JSci.awt.DefaultGraph2DModel.DataSeries;
import JSci.swing.JLineGraph;

import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.sun.media.jai.widget.DisplayJAI;

/**
 * @author nclinton
 *
 */
public class Graph extends JFrame {

	JLineGraph graph;
	DefaultGraph2DModel model;

	/**
	 * 
	 */
	public Graph(double[][] dataSeries) {
		super("Data");
		model = new DefaultGraph2DModel();
		DefaultGraph2DModel.DataSeries series = new DefaultGraph2DModel.DataSeries(dataSeries[0], dataSeries[1]);
		model.addSeries(series); // dummy series
		graph = new JLineGraph(model);
		graph.setGridLines(true);
		graph.setMarker(new Graph2D.DataMarker.Circle(3));
//		NumberFormat format = NumberFormat.getInstance();
//		format.setMaximumIntegerDigits(1);
//		format.setMinimumFractionDigits(2);
//		graph.setYNumberFormat(format);
//		graph.setYExtrema(0.0f, 0.6f);
//		graph.setYIncrement(0.1f);
		createAndShowGUI();
	}

	/**
	 * 
	 */
	private void createAndShowGUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(graph, BorderLayout.CENTER);
		pack();
		setVisible(true);
	}
	
	/**
	 * 
	 * @param series
	 */
	public void addSeries(double[][] series){
		model.addSeries(new DataSeries(series[0], series[1]));
		graph.redraw();
		repaint();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final double[][] check = new double[2][200];
		RandomDataImpl gauss = new RandomDataImpl();
		double inc = 2.0*Math.PI/100.0;
		for (int t=0; t<200; t++) {
			check[0][t] = t*inc;
			check[1][t] = 2.0 + Math.cos(check[0][t]) + gauss.nextGaussian(0, 0.3);
		}
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {	
				new Graph(check);
			}
		});

	}

}
