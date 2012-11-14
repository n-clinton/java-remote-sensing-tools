package cn.edu.tsinghua.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.DataBuffer;
import java.awt.image.renderable.ParameterBlock;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import cn.edu.tsinghua.timeseries.ImageLoadr2;
import cn.edu.tsinghua.timeseries.TSUtils;

import com.berkenviro.gis.GISUtils;
import com.berkenviro.imageprocessing.JAIUtils;
import com.sun.media.jai.widget.DisplayJAI;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.util.AffineTransformation;

/**
 * @author Nicholas Clinton
 * modified form of DisplayDEM by Raphael Santos
 */
public class ImageDisplayer extends DisplayJAI implements MouseMotionListener, MouseListener {
	
	static {
		System.setProperty("com.sun.media.jai.disableMediaLib", "true");
	}
	
	private ImageLoadr2 loadr;
	private PlanarImage myImage; // the display image
	protected StringBuffer pixelInfo; // the pixel information (formatted as a StringBuffer).
	protected List<double[]> pixelValues; // the time series provided by the ImageLoadr on a mouse click, 20121027
	private int width, height;

	/**
	 * The constructor of the class, which creates the arrays and instances needed
	 * to obtain the image data and registers the class to listen to mouse motion events.
	 * @param image a RenderedImage for display
	 	*/
	public ImageDisplayer(ImageLoadr2 iLoadr, int bandToDisplay) {
		pixelValues = null;
		loadr = iLoadr;
		pixelInfo = new StringBuffer(100);
		
		// set the display image
		String displayImage = loadr.getI(bandToDisplay).getImage();
		System.out.println("Loading display image: "+displayImage+"...");
		myImage = JAIUtils.readImage(displayImage);
		System.out.println("\t Done!");
		
		width = myImage.getWidth();
		height = myImage.getHeight();
		
//		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
//		ColorModel cm = new ComponentColorModel(cs, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_SHORT);
//		this.set(myImage.getAsBufferedImage(myImage.getBounds(), cm));

	    ParameterBlock pbMaxMin = new ParameterBlock();
	    pbMaxMin.addSource(myImage);
	    RenderedOp extrem = JAI.create("Extrema", pbMaxMin);
	    double[][] extrema = (double[][]) extrem.getProperty("Extrema");
		double min = extrema[0][0];
		double max = extrema[1][0];

	    // Rescale the image with the parameters
	    double[] scale = {255.0 / (max - min)};
	    double[] offset = {(255.0 * min) / (min - max)};
	    ParameterBlockJAI pbRescale = new ParameterBlockJAI("Rescale");
	    pbRescale.addSource(myImage);
	    pbRescale.setParameter("constants", scale);
	    pbRescale.setParameter("offsets", offset);
	    PlanarImage surrogateImage = (PlanarImage)JAI.create("Rescale", pbRescale, null);

	    ParameterBlock pbConvert = new ParameterBlock();
	    pbConvert.addSource(surrogateImage);
	    pbConvert.add(DataBuffer.TYPE_BYTE);
	    surrogateImage = JAI.create("format", pbConvert);
	    
	    set(surrogateImage);
	    
		addMouseMotionListener(this);
		addMouseListener(this);
	}


	public void mouseDragged(MouseEvent e) { }
	
	/**
	 * 
	 */
	public void mouseMoved(MouseEvent me) {
		int x = me.getX();
		int y = me.getY();
		try {
			double[] projXY = JAIUtils.getProjectedXY(new int[] {x, y}, myImage);
			pixelInfo.setLength(0); // clear the StringBuffer
			pixelInfo.append("Coordinates: "+x+","+y+" ("+projXY[0]+", "+projXY[1]+") series length: ");
			List<double[]> pixels = loadr.getSeries(GISUtils.makePoint(projXY[0], projXY[1]));
			pixelInfo.append(pixels.size()); // append to the StringBuffer
			for (double[] pixel : pixels) { // pixel values, slow
				// convert to a [0,1] scale before writing
				pixelInfo.append("("+pixel[0]+","+pixel[1]+"), "); // append to the StringBuffer
			}
		} catch (Exception e) {
			e.printStackTrace();
			pixelInfo.append("Bad coordinates: ("+x+", "+y+")");
		}	
	}

	public void updatePixelInfo() {
		
	}
	
	/**
	 * 20121027.  Fill the pixelValue array.
	 */
	public void mouseClicked(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		try {
			double[] projXY = JAIUtils.getProjectedXY(new int[] {x,y}, myImage);
			pixelValues = loadr.getSeries(GISUtils.makePoint(projXY[0], projXY[1]));
			
		} catch (Exception e1) {
			e1.printStackTrace();
			pixelInfo.append("Bad coordinates: ("+x+", "+y+")");
			pixelValues = null;
		}
		
	}
	
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	

	/**
	 * This method allows external classes access to the pixel info which was
	 * obtained in the mouseMoved method.
	 * @return the pixel information, formatted as a string
	 */
	public String getPixelInfo() {
		return pixelInfo.toString();
	}

	/**
	 * Called on a mouse click. Returns the time series of values.
	 * @return
	 */
	public double[][] getPixelValues() {
		return TSUtils.getSeriesAsArray(pixelValues);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	}

}
