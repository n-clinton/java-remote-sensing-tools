package cn.edu.tsinghua.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.List;

import javax.media.jai.PlanarImage;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.gdal.gdal.Dataset;

import JSci.awt.DefaultGraph2DModel;
import JSci.awt.Graph2D;
import JSci.swing.JLineGraph;
import cn.edu.tsinghua.timeseries.ImageLoadr2;
import cn.edu.tsinghua.timeseries.TSUtils;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.sun.media.jai.widget.DisplayJAI;

/**
 * @author Nicholas Clinton
 */
public class ImageDisplayer extends JFrame implements MouseMotionListener, MouseListener {
	// image
	DisplayJAI display;
	private ImageLoadr2 loadr;
	private Dataset dataset;
	private PlanarImage displayImage;
	// pixel location
	JTextArea info; 
	// plot window
	private TSDisplayer fitter;
	
	/**
	 * Multi-file contructor.
	 * @param loadr
	 * @param band
	 */
	public ImageDisplayer(ImageLoadr2 loader, int band) {
		super("Image display. Click mouse to see time series.");
		loadr = loader;
		// set the display image
		String filename = loadr.getI(band).getImage();
		PlanarImage image = JAIUtils.readImage(filename);
		PlanarImage scaled = JAIUtils.byteScale(image);
		displayImage = JAIUtils.linearStretch(scaled);
		JAIUtils.transferGeo(image, displayImage);
		image = null;
		scaled = null;
		System.gc();
		createAndShowGUI(displayImage);
	}
	
	/**
	 * Single file constructor.
	 * @param filename
	 * @param b
	 * @param g
	 * @param r
	 */
	public ImageDisplayer(String filename, int band) {
		super("Image display. Click mouse to see time series.");
		dataset = GDALUtils.getDataset(filename);
		BufferedImage bufferedImage = GDALUtils.getBufferedImage(dataset, band);
		PlanarImage image = PlanarImage.wrapRenderedImage(bufferedImage);
		PlanarImage scaled = JAIUtils.byteScale(image);
		displayImage = JAIUtils.linearStretch(scaled);
        createAndShowGUI(displayImage);
	}
	
	/**
	 * 
	 */
    private void createAndShowGUI(PlanarImage displayImage) {
    	// image setup
        display = new DisplayJAI(displayImage);
		display.addMouseListener(this);
		display.addMouseMotionListener(this);
        // pixel info
        info = new JTextArea(1,20);
    	// this 
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	getContentPane().setLayout(new BorderLayout());
    	getContentPane().add(new JScrollPane(display),BorderLayout.CENTER);
    	getContentPane().add(new JScrollPane(info),BorderLayout.SOUTH);
    	pack();
        setVisible(true);
        // for now, do nothing with this
        fitter = new TSDisplayer();
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
	@Override
	public void mouseClicked(MouseEvent e) {
		mouseMoved(e);
		// get the data from the loadr or dataset
		try {
			double[][] series = getSeries(e.getX(), e.getY());
			weka.core.Utils.normalize(series[1], 10000.0);
			fitter.graphSeries(series);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		try {
			double[] projXY = JAIUtils.getProjectedXY(new int[] {x,y}, displayImage);
			info.setText("("+x+", "+y+"): ("+projXY[0]+", "+projXY[1]+")");
		} catch (Exception e1) {
			e1.printStackTrace();
			info.setText("Bad coordinates: ("+x+", "+y+")");
		}
		info.repaint();
	}
	
	/**
	 * 
	 * @param x is the mouse x
	 * @param y is the mouse y
	 * @throws Exception
	 */
	private double[][] getSeries(int x, int y) throws Exception {
		double[][] series = null;
		if (loadr != null) {
			double[] projXY = JAIUtils.getProjectedXY(new int[] {x,y}, displayImage);
			List<double[]> pixelValues = loadr.getSeries(GISUtils.makePoint(projXY[0], projXY[1]));
			series = new double[2][pixelValues.size()];
			for (int t=0; t<pixelValues.size(); t++) {
				double[] timePoint = pixelValues.get(t);
				series[0][t] = timePoint[0];
				series[1][t] = timePoint[1];
			}
		}
		else if (dataset != null) {
			double[] wavelengths = GDALUtils.getWavelengths(dataset);
			double[] pixelValues = GDALUtils.pixelValues(dataset, x, y);
			weka.core.Utils.normalize(pixelValues, 10000.0);
			series = new double[][] {wavelengths, pixelValues};
		}
		else {
			throw new Exception("Bad initialization!");
		}
		return series;
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
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 2010, 2011 EVI
		String dir1 = "/Volumes/New Volume/MOD13A2/2010/";
		String dir2 = "/Volumes/New Volume/MOD13A2/2011/";
		try {
			final ImageLoadr2 loadr = new ImageLoadr2(new String[] {dir2, dir1});
			SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	            	new ImageDisplayer(loadr, 10);
	            }
	        });
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
