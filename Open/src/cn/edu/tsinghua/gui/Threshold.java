package cn.edu.tsinghua.gui;
/*
 * @(#)Threshold.java	1.11 01/03/19 13:54:29
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

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.media.jai.*;


public class Threshold extends JPanel
                       implements ChangeListener {

    private PlanarImage source = null;
    private PlanarImage target = null;
    private ImageDisplay display = null;
    private double low[];
    private double high[];
    private double map[];

    public Threshold(String filename) {

        setLayout(new BorderLayout());

        add(new JLabel("Threshold Image"), BorderLayout.NORTH);

        source = JAI.create("fileload", filename);

        display = new ImageDisplay(source);
        add(display, BorderLayout.CENTER);

        low  = new double[1];
        high = new double[1];
        map  = new double[1];

        low[0]  = 0.0F;
        high[0] = 0.0F;
        map[0]  = 0.0F;

        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 255, 0);
        Hashtable labels = new Hashtable();
        labels.put(new Integer(0), new JLabel("0"));
        labels.put(new Integer(255), new JLabel("255"));
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);
        slider.addChangeListener(this);
        slider.setEnabled(true);

        JPanel borderedPane = new JPanel();
        borderedPane.setLayout(new BorderLayout());
        borderedPane.setBorder(BorderFactory.createTitledBorder("Threshold"));
        borderedPane.add(slider, BorderLayout.NORTH);

        add(borderedPane, BorderLayout.SOUTH);
    }

    public final void stateChanged(ChangeEvent e) {
        JSlider slider = (JSlider)e.getSource();
        high[0] = (double) slider.getValue();

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add(low);
        pb.add(high);
        pb.add(map);
        target = JAI.create("threshold", pb, null);
        display.set(target);
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
    	final Threshold thresh = new Threshold(args[0]);
  		javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
        	  thresh.createAndShowGUI();
          }
  		});
    }
}
