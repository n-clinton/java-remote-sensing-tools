/**
 * 
 */
package cn.edu.tsinghua.gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import javax.media.jai.PlanarImage;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.math.stat.StatUtils;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.berkenviro.imageprocessing.SplineFunction;
import com.sun.media.jai.widget.DisplayJAI;

import cn.edu.tsinghua.timeseries.DuchonSplineFunction;
import cn.edu.tsinghua.timeseries.Extremum;
import cn.edu.tsinghua.timeseries.ImageLoadr2;
import cn.edu.tsinghua.timeseries.Loadr;
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
public class TSDisplayer2 extends JFrame implements ActionListener {

	private JTextArea stats;
	private JLineGraph graph;
	private DefaultGraph2DModel model;
    private int numSeries;
    
    /**
     * 
     * @param minX
     * @param maxX
     * @param minY
     * @param maxY
     */
    public TSDisplayer2() {
    	super("Time series display");
   	 	
    	// graph
   	 	model = new DefaultGraph2DModel();
   	 	DefaultGraph2DModel.DataSeries series = new DefaultGraph2DModel.DataSeries(new double[] {0}, new double[] {0});
   	 	model.addSeries(series); // dummy series
   	 	numSeries = 1;
   	 	graph = new JLineGraph(model);
   	 	graph.setGridLines(true);
        graph.setMarker(new Graph2D.DataMarker.Circle(3));
//        NumberFormat format = NumberFormat.getInstance();
//        format.setMaximumIntegerDigits(1);
//        format.setMinimumFractionDigits(2);
//        graph.setYNumberFormat(format);
        //graph.setYExtrema(0.0f, 0.6f);
        //graph.setYIncrement(0.1f);

        // stats
        stats = new JTextArea(10,20);

        // buttons
        Panel buttonPanel = new Panel();
        Panel topPanel = new Panel();
        Panel bottomPanel = new Panel();


//        topPanel.add(splineButton);
//        splineButton.addActionListener(this);
//        topPanel.add(resetButton);
//        resetButton.addActionListener(this);
//        topPanel.add(calcButton);
//        calcButton.addActionListener(this);
//        topPanel.add(meanButton);
//        meanButton.addActionListener(this);
//        topPanel.add(ratioButton);
//        ratioButton.addActionListener(this);
//        
//        bottomPanel.add(smoothButton);
//        smoothButton.addActionListener(this);
//        bottomPanel.add(widthLabel);
//        widthField.setColumns(2);
//        widthField.setText("3");
//        bottomPanel.add(widthField);
//        bottomPanel.add(degreeLabel);
//        degreeField.setColumns(2);
//        degreeField.setText("2");
//        bottomPanel.add(degreeField);
//        bottomPanel.add(smoothLabel);
//        smoothField.setColumns(4);
//        smoothField.setText("0.85");
//        bottomPanel.add(smoothField);
//        polyBox = new JCheckBox("Polynomial Spline"); 
//        polyBox.setSelected(false);
//        bottomPanel.add(polyBox);
        
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(topPanel, BorderLayout.NORTH);
        buttonPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buttonPanel, BorderLayout.NORTH);
        getContentPane().add(graph, BorderLayout.CENTER);
        getContentPane().add(stats, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }
    
    
	/**
	 * Start over.
	 * @param series
	 */
	public void graphSeries(List<double[]> list) {
		// clear the text
		stats.setText("");
		// clear series, if there are any
		while(numSeries>1) {
			model.removeSeries(numSeries-1);
			numSeries--;
		}
		double[][] series = getSeries(list);
		weka.core.Utils.normalize(series[1], 10000.0);
		DefaultGraph2DModel.DataSeries dataSeries = new DefaultGraph2DModel.DataSeries(series[0], series[1]);
		model.changeSeries(0, dataSeries);
		
		stats.append("Series 1: n="+list.size()+"\n");
		graph.redraw();
		repaint();
	}
    
	/**
	 * 
	 * @param list
	 */
	public void addSeries(List<double[]> list) {
		double[][] series = getSeries(list);
		weka.core.Utils.normalize(series[1], series[1][weka.core.Utils.maxIndex(series[1])]);
		model.addSeries(new DataSeries(series[0], series[1]));
		numSeries++;
		stats.append("Series 2: n="+list.size()+"\n");
		for (double[] t : list) {
			stats.append(Arrays.toString(t)+",");
		}
		stats.append("\n");
		graph.redraw();
		repaint();
	}
    
	
	/**
	 * 
	 * @param x is the mouse x
	 * @param y is the mouse y
	 * @throws Exception
	 */
	private double[][] getSeries(List<double[]> pixelValues) {
			double[][] series = new double[2][pixelValues.size()];
			for (int t=0; t<pixelValues.size(); t++) {
				double[] timePoint = pixelValues.get(t);
				series[0][t] = timePoint[0];
				series[1][t] = timePoint[1];
			}
		return series;
	}
	
	/**
	 * 
	 */
	public void actionPerformed(ActionEvent e) { }
	
	/**
	 * 
	 * @param text
	 */
	public void addText(String text) {
		stats.append(text+"\n");
		this.repaint();
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
	}

}
