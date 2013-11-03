package cn.edu.tsinghua.gui;
/*
 * @(#)HistogramDemo.java	1.17 01/03/19 13:54:26
 *
 * Copyright (c) 2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

/*
 * Disparaging my ass.  This was so riddled with errors it needed to be completely rewritten.
 * Now it works as it should. 20130130. Nicholas Clinton.
 */

import java.io.File;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.DataBuffer;
import java.awt.event.*;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.Histogram;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.RenderedOp;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;


/**
 * Rewritten to handle multi-band input and correct implementation of probability distributions.
 * @author nclinton
 *
 */
public class HistogramDemo extends JPanel implements ActionListener {

    private PlanarImage source = null;
    private PlanarImage target = null;
    private Panner panner;
    private JButton reset;
    private JButton equal;
    private JButton norm;
    private JButton piece;
    private ImageDisplay canvas;
    private XYPlot graph;

    /**
     * Initialize display.
     * @author Sun Microsystems
     * @param filename is the image filename
     */
    public HistogramDemo(String filename) {
        File f = new File(filename);

        if ( f.exists() && f.canRead() ) {
            source = JAI.create("fileload", filename);
        } else {
            return;
        }

        canvas = new ImageDisplay(source);
        canvas.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 2));

        panner = new Panner(canvas, source, 128);
        panner.setBackground(Color.red);
        panner.setBorder(new EtchedBorder());
        canvas.add(panner);

        Font font = new Font("SansSerif", Font.BOLD, 12);
        JLabel title = new JLabel(" Histogram");
        title.setFont(font);
        title.setLocation(0, 32);

        setOpaque(true);
        setLayout(new BorderLayout());
        setBackground(Color.white);

        graph = new XYPlot();
        graph.setBackground(Color.black);
        graph.setBorder(new LineBorder(new Color(0,0,255), 1));

        Colorbar cbar = new Colorbar();
        cbar.setBackground(Color.black);
        cbar.setPreferredSize(new Dimension(256, 25));
        cbar.setBorder(new LineBorder(new Color(255,0,255),2));

        JPanel hist_panel = new JPanel();
        hist_panel.setLayout(new BorderLayout());
        hist_panel.setBackground(Color.white);
        hist_panel.add(graph, BorderLayout.CENTER);
        hist_panel.add(cbar, BorderLayout.SOUTH);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2,1,5,5));
        panel.setBackground(Color.white);
        panel.add(canvas);
        panel.add(hist_panel);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        reset = new JButton("Reset");
        equal = new JButton("Uniform");
        norm  = new JButton("Gaussian");
        piece = new JButton("Piecewise");

        reset.addActionListener(this);
        equal.addActionListener(this);
        norm.addActionListener(this);
        piece.addActionListener(this);

        controlPanel.add(reset);
        controlPanel.add(equal);
        controlPanel.add(norm);
        controlPanel.add(piece);

        add(title, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // original histogram (remains unmodified)
        // graph.plot( getHistogram(source) );
        graph.plot( getMultiHistogram(source) );
    }

    /**
     * Old one.
     * @param image
     * @return
     */
    public int[] getHistogram(PlanarImage image) {
        // set up the histogram
        int[] bins = { 256, 256, 256 };
        double[] low = { 0.0D, 0.0D, 0.0D };
        double[] high = { 256.0D, 256.0D, 256.0D };

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(null);
        pb.add(1);
        pb.add(1);
        pb.add(bins);
        pb.add(low);
        pb.add(high);

        RenderedOp op = JAI.create("histogram", pb, null);
        Histogram histogram = (Histogram) op.getProperty("histogram");

        // get histogram contents
        int[] local_array = new int[histogram.getNumBins(0)];
        for ( int i = 0; i < histogram.getNumBins(0); i++ ) {
            local_array[i] = histogram.getBinSize(0, i);
        }

        return local_array;
    }

    /**
     * New one.  Assumes an 8-bit image.
     * @param image
     * @return
     */
    public int[][] getMultiHistogram(PlanarImage image) {
        // set up the histogram
    	int[] bins = { 256, 256, 256 };
        double[] low = { 0.0D, 0.0D, 0.0D };
        double[] high = { 256.0D, 256.0D, 256.0D };

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(null);
        pb.add(1);
        pb.add(1);
        pb.add(bins);
        pb.add(low);
        pb.add(high);

        RenderedOp op = JAI.create("histogram", pb, null);
        Histogram histogram = (Histogram) op.getProperty("histogram");

        // get histogram contents
        int[][] local_array = new int[source.getNumBands()][histogram.getNumBins(0)];
        for (int b=0; b<source.getNumBands(); b++) {
	        for ( int i = 0; i < histogram.getNumBins(b); i++ ) {
	            local_array[b][i] = histogram.getBinSize(b, i);
	            //System.out.print(local_array[b][i] + " ");
	        }
	        //System.out.println();
        }
        return local_array;
    }
    
    /**
     * Adjust to a Uniform distribution CDF.
     * @return the stretched image.
     */
    public PlanarImage equalize() {
    	// From JAI programming guide
    	int numBands = source.getNumBands();
    	int binCount = 256;
    	// Create an equalization CDF.
    	float[][] CDFeq = new float[numBands][];
    	for(int b = 0; b < numBands; b++) {
    		CDFeq[b] = new float[binCount];
    		for(int i = 0; i < binCount; i++) {
    			CDFeq[b][i] = (float)(i+1)/(float)binCount;
    		}
    	}
    	int[] bins = { 256 };
        double[] low = { 0.0D };
        double[] high = { 256.0D };

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add(null);
        pb.add(1);
        pb.add(1);
        pb.add(bins);
        pb.add(low);
        pb.add(high);

        RenderedOp fmt = JAI.create("histogram", pb, null);
    	// Create a histogram-equalized image.
    	return JAI.create("matchcdf", fmt, CDFeq);
    }

    /**
     * Adjust to a normal distribution CDF, mean=128, SD=64.
     * @return the stretched image.
     */
    public PlanarImage normalize() {
    	int numBands = source.getNumBands();
    	int binCount = 256;
    	float[][] CDFnorm = new float[numBands][binCount];
    	NormalDistributionImpl norm = new NormalDistributionImpl(128.0, 64.0);
    	for(int b = 0; b < numBands; b++) {
    		for(int i = 0; i < binCount-1; i++) {
    			try {
					CDFnorm[b][i] = (float)norm.cumulativeProbability(i);
				} catch (MathException e) {
					e.printStackTrace();
				}
    		}
    		// the cumulative probability must equal one
    		CDFnorm[b][binCount-1] = 1;
    	}

        int[] bins = { 256 };
        double[] low = { 0.0D };
        double[] high = { 256.0D };

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add(null);
        pb.add(1);
        pb.add(1);
        pb.add(bins);
        pb.add(low);
        pb.add(high);

        RenderedOp fmt = JAI.create("histogram", pb, null);
        return JAI.create("matchcdf", fmt, CDFnorm);
    }

    /**
     * From Sun.  Piecewise linear mapping.
     * @return
     */
    public PlanarImage piecewise() {
    	
    	// Create a piecewise-mapped image emphasizing low values.
    	int numBands = source.getNumBands();
    	float[][][] bp = new float[numBands][2][];
    	for(int b = 0; b < numBands; b++) {
    		bp[b][0] = new float[] {0.0F, 32.0F, 64.0F, 255.0F};
    		bp[b][1] = new float[] {0.0F, 64.0F, 112.0F, 255.0F};
    	}

        return JAI.create("piecewise", source, bp);
    }

    /**
     * Handle button clicks.
     */
    public void actionPerformed(ActionEvent e) {
        JButton b = (JButton)e.getSource();

        if ( b == reset ) {
            target = source;
        } else if ( b == equal ) {
            target = equalize();
        } else if ( b == norm ) {
            target = normalize();
        } else if ( b == piece ) {
            target = piecewise();
        }

        canvas.set(target);
        graph.plot( getMultiHistogram(target) );
    }
    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private void createAndShowGUI() {
        JFrame frame = new JFrame("Java Advanced Imaging (JAI) Widget");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setVisible(true);
    }
    
    /**
     * 
     * @param args is the file path of an image
     */
    public static void main(String[] args) {
    	final HistogramDemo histo = new HistogramDemo(args[0]);
  		javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
        	  histo.createAndShowGUI();
          }
  		});
    }
}
