

package cn.edu.tsinghua.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import cn.edu.tsinghua.timeseries.ImageLoadr2;

/**
 * 
 */

/**
 * @author Nicholas Clinton
 *
 */
public class TimeSeriesViewer extends JFrame implements MouseMotionListener, MouseListener {

	private ImageDisplayer displayer; // an instance of the DisplayJAI component
	private JLabel label; // the label used to display information about the image
	private TSDisplayer fitter;
	
	
	/**
	 * Constructor takes an ImageLoadr2.  20121027
	 * @param loadr
	 * @param displayBand
	 */
	public TimeSeriesViewer(ImageLoadr2 iLoadr, int displayBand) {
		this.setTitle("Move the mouse over the image !");
		Container myPane = this.getContentPane();
	    myPane.setLayout(new BorderLayout());
	    displayer = new ImageDisplayer(iLoadr, displayBand);
	    myPane.add(new JScrollPane(displayer),BorderLayout.CENTER);
	    // this  JLabel displayes the pixel values.
	    label = new JLabel("---");
	    getContentPane().add(label,BorderLayout.SOUTH);
	    // register this with the Displayer
	    displayer.addMouseMotionListener(this);
	    displayer.addMouseListener(this);
	    // Set the closing operation so the application is finished.
	    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    this.setSize(700, 500); // adjust the frame size using preferred dimensions.
	    setVisible(true); // show the frame.
	}
	
	
	// MouseMotion events
	public void mouseDragged(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {
		// just update the label wiDisplaJAI instance info.
		label.setText(displayer.getPixelInfo());
	}

	/**
	 * 
	 */
	public void mouseClicked(MouseEvent e) {
		
		double[][] xy = displayer.getPixelValues();
		
		// just get rid of the old one
		if (fitter != null) {
			fitter.dispose();
		}
		
		// and replace with a new one
		fitter = new TSDisplayer(xy);
		fitter.setSize(800, 700);
		fitter.graphSeries();
	}
	
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		PlanarImage image = JAIUtils.readImage(args[0]);
//	    new TimeSeriesViewer(image, 0);
		
		// 2010, 2011 EVI
		String dir1 = "D:/MOD13A2/2010/";
		String dir2 = "D:/MOD13A2/2011/";
		try {
			final ImageLoadr2 loadr = new ImageLoadr2(new String[] {dir2, dir1});
			SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	            	new TimeSeriesViewer(loadr, 10);
	            }
	        });
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	    
	}


}
