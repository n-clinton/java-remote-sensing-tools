package cn.edu.tsinghua.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Calendar;
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
import cn.edu.tsinghua.lidar.BitChecker;
import cn.edu.tsinghua.modis.BitCheck;
import cn.edu.tsinghua.timeseries.ImageLoadr2;
import cn.edu.tsinghua.timeseries.ImageLoadr4;
import cn.edu.tsinghua.timeseries.Loadr;
import cn.edu.tsinghua.timeseries.PERSIANNLoadr;
import cn.edu.tsinghua.timeseries.TSUtils;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.sun.media.jai.widget.DisplayJAI;

/**
 * @author Nicholas Clinton
 */
public class ImageDisplayer2 extends JFrame implements MouseMotionListener, MouseListener {
	// image
	DisplayJAI display;
	private Loadr response;
	private Loadr covariate;
	private PlanarImage displayImage;
	// pixel location
	JTextArea info; 
	// plot window
	private TSDisplayer2 fitter;
	
	
	/**
	 * Single file constructor.
	 * @param filename
	 * @param b
	 * @param g
	 * @param r
	 */
	public ImageDisplayer2(PlanarImage display, Loadr response, Loadr covariate) {
		super("Image display. Click mouse to see time series.");
		this.response = response;
		this.covariate = covariate;
		JAIUtils.register(display);
		displayImage = JAIUtils.linearStretch(display);
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
        fitter = new TSDisplayer2();
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
	@Override
	public void mouseClicked(MouseEvent e) {
		mouseMoved(e);
		int x = e.getX();
		int y = e.getY();
		// get the data from the loadr or dataset
		try {
			double[] projXY = JAIUtils.getProjectedXY(new int[] {x,y}, displayImage);
			List<double[]> responseValues = response.getSeries(projXY[0], projXY[1]);
			fitter.graphSeries(responseValues);
			//weka.core.Utils.normalize(series[1], 10000.0);
			List<double[]> covValues = covariate.getSeries(projXY[0], projXY[1]);
			fitter.addSeries(covValues);
			
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
		// display land area:
		String reference = "X:/Documents/global_phenology/land_mask.tif";
		final PlanarImage display = JAIUtils.readImage(reference);
		
		
		try {
			// EVI------------------------------
			String[] evi = new String[] {"D:/MOD13A2/2010", "D:/MOD13A2/2011"};
			String eviDir = "EVI";
			String eviQCDir = "VI_QC";
			BitCheck mod13Checker = new BitCheck() {
				@Override
				public boolean isOK(int check) {
					return BitChecker.mod13ok(check);
				}
				
			};
			final ImageLoadr4 responseLoadr = new ImageLoadr4(evi, eviDir, eviQCDir, mod13Checker);
			// PERSIANN rainfall predictor----------------------------------------------------------
//			String[] persiann = new String[] {"D:/PERSIANN/8km_daily/2010/", "D:/PERSIANN/8km_daily/2011/"};
//			final PERSIANNLoadr predictorLoadr = new PERSIANNLoadr(persiann);
			// LST
			String[] lst = new String[] {"D:/MYD11A2/2010/", "D:/MYD11A2/2011/"};
			String tempDir = "LST_DAY";
			String tempQCDir = "QC_DAY";
			BitCheck mod11Checker = new BitCheck() {
				@Override
				public boolean isOK(int check) {
					return BitChecker.mod11ok(check);
				}
				
			};
			final ImageLoadr4 predictorLoadr = new ImageLoadr4(lst, tempDir, tempQCDir, mod11Checker);
			// set date. DO NOT NOT do this!!!!!!!!!!!
			Calendar cal = Calendar.getInstance();
			cal.set(2010, 0, 0);
			System.out.println("Setting zero-reference to: " + cal.getTime());
			responseLoadr.setDateZero(cal);
			predictorLoadr.setDateZero(cal);
			// GO!
			SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	            	new ImageDisplayer2(display, responseLoadr, predictorLoadr);
	            }
	        });
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
