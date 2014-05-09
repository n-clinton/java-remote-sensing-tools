package cn.edu.tsinghua.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JFrame;

import ru.sscc.spline.Spline;
import ru.sscc.spline.analytic.GSplineCreator;
import ru.sscc.spline.polynomial.PEvenSplineCreator;
import ru.sscc.spline.polynomial.POddSplineCreator;
import ru.sscc.spline.polynomial.PSpline;
import ru.sscc.spline.polynomial.PolynomialSplineCreator;
import ru.sscc.spline.reduction.ReducedMesh;
import ru.sscc.spline.reduction.Reduction;
import ru.sscc.spline.reduction.StrictScatteredMesh;
import ru.sscc.util.CalculatingException;
import ru.sscc.util.data.DoubleVectors;
import ru.sscc.util.data.RealVectors;
import weka.core.Utils;

import JSci.awt.*;
import JSci.maths.*;
import JSci.maths.polynomials.RealPolynomial;
import JSci.swing.JGraphLayout;
import JSci.swing.JLineGraph;

/**
* Sample program demonstrating use of LinearMath.leastSquaresFit method
* and the LineTrace graph class.
* @author Mark Hale
* modified by NC, 3/7/7
* @version 1.0
*/
public final class CurveFitter extends JFrame implements ActionListener {
        
		private Label fnLabel = new Label("P(x) = ?",Label.CENTER);
		private JLineGraph graph;
		private DefaultGraph2DModel model;
        private TextField polyDegreeField = new TextField("6");
        private Button fitButton = new Button("Fit");
        private TextField splineTypeField = new TextField("2");
        private Button splineButton = new Button("Spline");
        private Button clearButton = new Button("Clear");
        //private Button deriveButton = new Button("derive");
        private float minX, maxX;
        
        public CurveFitter() {
        	super();
        }
        
        public CurveFitter(float[] x, float[] y, float xMin, float xMax) {
                
        	super("Curve Fitter");
        	this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
            add(fnLabel,"North");
                
            minX = xMin;
            maxX = xMax;
            // define the series as the x and y double arrays
            model = new DefaultGraph2DModel();
            model.setXAxis(min(x),max(x),x.length);
            model.addSeries(y);

            graph = new JLineGraph(model);
            graph.setGridLines(true);
            graph.setMarker(new Graph2D.DataMarker.Circle(5));
            //graph.setYExtrema(0.0f, 1.0f);
            //Panel lineGraphPanel = new Panel(new JGraphLayout());
            //lineGraphPanel.add(graph, JGraphLayout.GRAPH);
            //lineGraphPanel.setVisible(true);
            //add(lineGraphPanel);
            add(graph);
                
            Panel buttonPanel = new Panel();
            buttonPanel.add(new Label("Poly Degree:"));
            buttonPanel.add(polyDegreeField);
            buttonPanel.add(fitButton);
            fitButton.addActionListener(this);
            buttonPanel.add(new Label("Type:"));
            buttonPanel.add(splineTypeField);
            buttonPanel.add(splineButton);
            splineButton.addActionListener(this);
            //buttonPanel.add(deriveButton);
            //deriveButton.addActionListener(this);
            buttonPanel.add(clearButton);
            clearButton.addActionListener(this);
            add(buttonPanel,"South");
                
            setSize(400,300);
            setVisible(true);
            
        }
        
        /*
         * Helper method to return the orginal series from the graph.
         */
        private double[][] getDataPoints() {
        	double[][] data = new double[2][model.seriesLength()];
            for(int i=0;i<data[0].length;i++) {
                    data[0][i]=model.getSeries(0).getXCoord(i);
                    data[1][i]=model.getSeries(0).getValue(i);
            }
            return data;
        }
        
        private void fitCurve() {
        	
        		double[][] data = getDataPoints();
                
                int degree = Integer.parseInt(polyDegreeField.getText());
                RealPolynomial poly = LinearMath.leastSquaresFit(degree, data);
                fnLabel.setText(poly.toString());
                
                float[] polyvals = new float[model.seriesLength()];
                for(int i=0; i<polyvals.length; i++) {
                    polyvals[i]= (float)poly.map(Integer.valueOf(i+1).doubleValue());
                }
        		
                model.addSeries(polyvals);
        }
        /*
         * This basically does nothing except fit linear functions between the 
         * data points.  However, as a spline, there are methods for differentiating
         * which might prove useful.
         */
        private void fitSpline() {
        	
        	double[][] data = getDataPoints();
        	int type = Integer.parseInt(splineTypeField.getText());
        	
            fnLabel.setText("spline");
            
            // JSci implementation (can't figure out the interpolation)
            /*
            double[] svals = ls.interpolate(0);
            float[] newX = new float[data[0].length];
            for (int i=0; i<newX.length; i++) {
            	newX[i] = (float)data[0][i];
            }
            */
            
            
            // JSpline implementation:
            RealVectors dataPts = new DoubleVectors(1, data[0].length);
            for (int i=0; i<dataPts.size; i++) {
            	dataPts.set(i,0,data[0][i]);
            }
            
            Spline spline = null;
            Spline pSpline = null;
            Spline derivative = null;
            try {
				spline = GSplineCreator.createSpline(type, dataPts, data[1]);
				// use a low order polynomial spline to get a derivative ???
				pSpline = (new POddSplineCreator()).createSpline(2, data[0], data[1]);
            	// the polynomial splines look weird: very odd behavior outside range
            	// construct a polynomial spline based on degree in the "type" field
            	/*
            	if ( type%2 == 0) { // even spline
            		spline = (new PEvenSplineCreator()).createSpline(type, data[0], data[1]);
            	}
            	else { // odd spline
            		spline = (new POddSplineCreator()).createSpline(type, data[0], data[1]);
            	}
            	*/
				derivative = PSpline.derivative(pSpline);
			} catch (CalculatingException e) {
				e.printStackTrace();
			}
			// densify the spline to illustrate its shape
			int numVals = 100;
            double[] sVals = new double[numVals];
            double[] dVals = new double[numVals];
            float[] newX = new float[numVals];
            float increment = (maxX - minX)/(numVals-4);
            // start one increment lower
            float x = minX - 2*increment;
            for (int j=0; j<numVals; j++) {
            	sVals[j] = spline.value(x);
            	//dVals[j] = derivative.value(x);
            	dVals[j] = pSpline.value(x);
            	newX[j] = x;
            	System.out.println("x = "+newX[j]);
            	System.out.println("thin plate spline = "+sVals[j]);
            	System.out.println("approximate derivative = "+dVals[j]);
            	x += increment;
            }
            
            model.addSeries(newX, sVals);
            //model.addSeries(newX, dVals);
            
            
    }
        
        
        // max
        public static float max(float[] t) {
        	float maximum = t[0];   // start with the first value
            for (int i=1; i<t.length; i++) {
                if (t[i] > maximum) {
                    maximum = t[i];   // new maximum
                }
            }
            return maximum;
        }
        
        // min
        public static float min(float[] t) {
        	float minimum = t[0];   // start with the first value
            for (int i=1; i<t.length; i++) {
                if (t[i] < minimum) {
                	minimum = t[i];   // new min
                }
            }
            return minimum;
        }
        
        
        // this is for the buttons
        public void actionPerformed(ActionEvent e) {
			if (e.getSource() == fitButton) {
				fitCurve();
			}
			if (e.getSource() == splineButton) {
				fitSpline();
			}
			if (e.getSource() == clearButton) {
				// simply removes the last fitted series
				model.removeSeries(1);
				graph.redraw();
				repaint();
			}
			
		}
        
        
        /*
         * Add a series to the graph
         */
        public void addSeries(float[] x, float[] y) {
        	model.addSeries(x, y);
        }
        
        public static void main(String arg[]) {
        	/*
        	float[] x = {1.0f,2.0f,3.0f,4.0f,5.0f,6.0f,7.0f,8.0f,9.0f,10.0f,11.0f,12.0f,
        			13.0f,14.0f,15.0f,16.0f,17.0f};
      
        	float[] y = {108.0f, 107.0f, 106.5f, 111.0f, 113.5f, 115.0f, 114.0f, 112.0f,
        				110.0f, 109.0f, 110.0f, 115.5f, 115.0f, 113.5f, 112.0f, 115.0f, 116.0f};
        	*/
        	float[] x = {1.0f,2.0f,4.0f,5.0f,6.0f,7.0f,8.0f,9.0f,10.0f,11.0f,12.0f,
        			13.0f,14.0f,16.0f,17.0f};
      
        	float[] y = {108.0f, 107.0f, 111.0f, 113.5f, 115.0f, 114.0f, 112.0f,
        				110.0f, 109.0f, 110.0f, 115.5f, 115.0f, 113.5f, 115.0f, 116.0f};
        	
            new CurveFitter(x, y, CurveFitter.min(x), CurveFitter.max(x));
		}
		
        
}

