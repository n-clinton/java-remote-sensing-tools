package cn.edu.tsinghua.gui;
/*
 * @(#)About.java	1.13 01/03/19 13:54:24
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

import java.io.File;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.border.*;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;


/**
 * 
 * Modified by nicholas.clinton@gmail.com for instructional purposes.
 *
 */
public class About extends JPanel implements ActionListener {

    private PlanarImage source = null;
    private Magnifier mag;
    private JButton nearest, bilinear, cubic;

    public About(String filename) {
        File f = new File(filename);

        if ( f.exists() && f.canRead() ) {
            source = JAI.create("fileload", filename);
        } else {
            return;
        }

        JPanel canvasPanel = new JPanel();
        ImageDisplay canvas = new ImageDisplay(source);
        canvas.setBackground(Color.blue);

        canvas.setBorder(new CompoundBorder(
                            new EtchedBorder(),
                            new LineBorder(Color.gray, 20)
                        ) );
        canvasPanel.add(canvas);
        
        Font font = new Font("SansSerif", Font.BOLD, 12);
        JLabel title = new JLabel(" Use the mouse to position magnifier.");
        title.setFont(font);
        title.setLocation(0, 32);

        setOpaque(true);
        setLayout(new BorderLayout());
        setBackground(Color.white);

        JLabel label = new JLabel(" Magnifier");
        label.setForeground(Color.white);
        
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        nearest = new JButton("Nearest Neighbor");
        bilinear = new JButton("Bilinear Interpolation");
        cubic = new JButton("Cubic Convolution");

        nearest.addActionListener(this);
        bilinear.addActionListener(this);
        cubic.addActionListener(this);

        controlPanel.add(nearest);
        controlPanel.add(bilinear);
        controlPanel.add(cubic);

        this.setLayout(new BorderLayout());
        add(title,  BorderLayout.NORTH);
        add(canvasPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        
        // magnifier
        mag = new Magnifier(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        mag.setSource(canvas);
        mag.setMagnification(4.0F);
        mag.setSize(256, 256);
        mag.setLocation(150, 150);
        mag.setBorder(new LineBorder(Color.white,1));
        mag.setLayout(new BorderLayout());
        mag.add(label, BorderLayout.NORTH);
        canvas.add(mag);
        
        validate();
    }
    
    /**
     * Handle button clicks.
     */
    public void actionPerformed(ActionEvent e) {
        JButton b = (JButton)e.getSource();
        Interpolation interp;
        if ( b == nearest ) {
            interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            mag.setInterpolation(interp);
        } else if ( b == bilinear ) {
            interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            mag.setInterpolation(interp);
        } else if ( b == cubic ) {
            interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
            mag.setInterpolation(interp);
        }
        mag.repaint();
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
    	final About about = new About(args[0]);
  		javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
        	  about.createAndShowGUI();
          }
  		});
    }
    
}
