package cn.edu.tsinghua.gui;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.renderable.ParameterBlock;
import java.text.NumberFormat;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.gdal.gdal.Dataset;

import weka.core.Utils;

import JSci.awt.DefaultGraph2DModel;
import JSci.awt.Graph2D;
import JSci.awt.DefaultGraph2DModel.DataSeries;
import JSci.awt.Graph2DModel;
import JSci.swing.JLineGraph;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.sun.media.jai.widget.DisplayJAI;

/**
 * 
 */

/**
 * Widget to display the JAI demos.
 * @author nclinton
 *
 */
public class ImageDisplay extends JFrame implements MouseMotionListener, MouseListener {
	
	// image
	DisplayJAI display;
	private Dataset dataset;
	double[] wavelengths;
	
	// spectrum
	JFrame plot;
	JLineGraph graph;
	DefaultGraph2DModel model;
	
	JTextArea info; // pixel location
	
	/**
	 * 
	 * @param aJAIComponent
	 */
	public ImageDisplay(String filename, int b, int g, int r) {
		super("Image display. Click mouse to see spectrum.");
		
		// image data
		dataset = GDALUtils.getDataset(filename);
		wavelengths = GDALUtils.getWavelengths(filename);
		
		// display image setup
		BufferedImage blue = GDALUtils.getBufferedImage(dataset, b);
		BufferedImage green = GDALUtils.getBufferedImage(dataset, g);
		BufferedImage red = GDALUtils.getBufferedImage(dataset, r);
		// merge to RGB
		PlanarImage combined = JAIUtils.makeRGB(PlanarImage.wrapRenderedImage(red), 
				PlanarImage.wrapRenderedImage(green), PlanarImage.wrapRenderedImage(blue));
		// scale to byte
		PlanarImage scaled = JAIUtils.byteScale(combined);
		// stretch
		PlanarImage surrogateImage = JAIUtils.linearStretch(scaled);
		display = new DisplayJAI(surrogateImage);
		display.addMouseListener(this);
		display.addMouseMotionListener(this);
		
		// plot setup
		model = new DefaultGraph2DModel();
		DefaultGraph2DModel.DataSeries series = new DefaultGraph2DModel.DataSeries(new double[] {0}, new double[] {0});
		model.addSeries(series); // dummy series
		graph = new JLineGraph(model);
		graph.setGridLines(true);
        graph.setMarker(new Graph2D.DataMarker.Circle(3));
        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumIntegerDigits(1);
        format.setMinimumFractionDigits(2);
        graph.setYNumberFormat(format);
        graph.setYExtrema(0.0f, 0.6f);
        graph.setYIncrement(0.1f);
        
        // info
        info = new JTextArea(1,20);
        
        createAndShowGUI();
	}
 
	/**
	 * 
	 */
    private void createAndShowGUI() {
    	// image display
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	getContentPane().setLayout(new BorderLayout());
    	getContentPane().add(new JScrollPane(display),BorderLayout.CENTER);
    	pack();
        setVisible(true);
        
        // spectrum display
        plot = new JFrame("Spectrum display.");
        plot.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        plot.getContentPane().setLayout(new BorderLayout());
        plot.getContentPane().add(graph, BorderLayout.NORTH);
        plot.getContentPane().add(info, BorderLayout.SOUTH);
        plot.pack();
        plot.setVisible(true);
    }

	@Override
	public void mouseClicked(MouseEvent e) {
		mouseMoved(e);
		double[] pixelValues = GDALUtils.pixelValues(dataset, e.getX(), e.getY());
		weka.core.Utils.normalize(pixelValues, 10000.0);
		DefaultGraph2DModel.DataSeries series = new DefaultGraph2DModel.DataSeries(wavelengths, pixelValues);
		model.changeSeries(0, series);
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		try {
			double[] projXY = GDALUtils.getProjectedXY(new int[] {x,y}, dataset);
			info.setText("("+x+", "+y+"): ("+projXY[0]+", "+projXY[1]+")");
		} catch (Exception e1) {
			e1.printStackTrace();
			info.setText("Bad coordinates: ("+x+", "+y+")");
		}
		info.repaint();
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
        
    	final String filename = "/Users/nclinton/Documents/Tsinghua/remote_sensing_class/Delta_Hymap_12.img";
		
  		javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {	
        	  new ImageDisplay(filename, 1, 7, 14);
          }
  		});
  		
    }

	
	
}
