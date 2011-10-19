/*
 *  Copyright (C) 2011  Nicholas Clinton
 *	All rights reserved.  
 *
 *	Redistribution and use in source and binary forms, with or without modification, 
 *	are permitted provided that the following conditions are met:
 *
 *	1. Redistributions of source code must retain the above copyright notice, 
 *	this list of conditions and the following disclaimer.  
 *	2. Redistributions in binary form must reproduce the above copyright notice, 
 *	this list of conditions and the following disclaimer in the documentation 
 *	and/or other materials provided with the distribution. 
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 *	AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 *	THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 *	PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
 *	BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 *	DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 *	THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN 
 *	IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.berkenviro.segmentation;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;


/**
 *	@author Nicholas Clinton
 *	
 */
public class IntersectorGUI extends JFrame implements ActionListener, PropertyChangeListener {
	
	File training;
	File segments;

    static private final String newline = "\n";
    JButton trainButton, segButton, clearButton, goButton, stopButton;
    private final JTextArea log;
    JFileChooser fc;
    
    ExecutorService executor;
    BufferedWriter tableWriter;
    
    /**
     * 
     */
    public IntersectorGUI() {
        super("Intersector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(5,20);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);

        //Create a file chooser
        fc = new JFileChooser();

        trainButton = new JButton("Open training File");
        trainButton.addActionListener(this);

        segButton = new JButton("Open segmentation File or Directory");
        segButton.addActionListener(this);
        
        clearButton = new JButton("Clear");
        clearButton.addActionListener(this);
        
        goButton = new JButton("Go!");
        goButton.addActionListener(this);
        
        stopButton = new JButton("Stop!");
        stopButton.addActionListener(this);
        
        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(trainButton);
        buttonPanel.add(segButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(goButton);
        buttonPanel.add(stopButton);

        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
      
        //Display the window.
        pack();
        setVisible(true);
		
	}
    
    /** @param e */
    @Override
    public void actionPerformed(ActionEvent e) {
    	FileNameExtensionFilter filter = new FileNameExtensionFilter("Shapefiles", "shp");
        
    	//Handle open button action.
        if (e.getSource() == trainButton) {
        	fc.setFileFilter(filter);
        	fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fc.showOpenDialog(IntersectorGUI.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                training = fc.getSelectedFile();
                log.append("Training Objects: " + training.getPath() + "." + newline);
            } else {
                log.append("Open training command cancelled by user." + newline);
            }
        //Handle save button action.
        } else if (e.getSource() == segButton) {
        	fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int returnVal = fc.showSaveDialog(IntersectorGUI.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                segments = fc.getSelectedFile();
                //This is where a real application would save the file.
                log.append("Segments location(s): " + segments.getPath() + "." + newline);
            } else {
                log.append("Open segments command cancelled by user." + newline);
            } 
        // nullify the selections
        } else if (e.getSource() == clearButton) {
            segments = null;
            training = null;
            log.append("Selections cleared by user." + newline);
        // process
        } else if (e.getSource() == goButton) {
        	// if selected and exists
        	if (segments != null && training != null && training.exists() && segments.exists()) {
        		String tableName = "";
        		if (segments.isDirectory()) {
        			tableName = segments.getPath() + "/" + training.getName().replace(".shp", "_rank.txt");
        		}
        		else {
        			tableName = segments.getParent() + "/" + training.getName().replace(".shp", "_rank.txt");
        		}
        		log.append("Processing started at "+Calendar.getInstance().getTime()+ newline);
        		log.append("Writing table: " + tableName + newline);
        		//log.setCaretPosition(log.getDocument().getLength());
        		try {
					write(tableName);
				} catch (Exception e1) {
					log.append(e1.getMessage() + newline);
					log.append("Error condition at "+Calendar.getInstance().getTime()+ newline);
				}
        	}
        	// stop everything
        	else if (e.getSource() == stopButton) {
        		// stop everything
        		try {
        			executor.shutdownNow();
        			tableWriter.close();
        			log.append("Operation stopped by user." + newline);
        		} catch (Exception ex) {
        			log.append(ex.getMessage() + newline);
        		}
        	}
        	else {
        		if (segments == null || !segments.exists()) {
        			log.append("The segment file is not set or does not exist."+newline);
        		}
        		if (training == null || !training.exists()) {
        			log.append("The training file is not set or does not exist."+newline);
        		}
        		log.append("Please try again." + newline);
        	}
        }
        
    }
    

    /**
     * Listen for completion of the SwingWorkers
     */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		try {
			if (((SwingWorker)evt.getSource()).isDone()) {  // if it's done
				if (executor.isTerminated()) {  // if everything is done
					tableWriter.close();
					log.append("Completed at "+Calendar.getInstance().getTime()+ newline);
				}
			}
			// otherwise do nothing
		} catch (IOException e) {
			e.printStackTrace();
			log.append(e.getMessage() + newline);
		}
	}
	
    
	/**
	 * Initialize the FileWriter, submit tasks to a completion service.
	 * @param tableName
	 * @throws Exception
	 */
	private void write(String tableName) throws Exception {
		tableWriter = new BufferedWriter(new FileWriter(tableName));
		tableWriter.write(getStatsHeader());
		tableWriter.newLine();

		executor = Executors.newSingleThreadExecutor();
		
		File[] files;
		if (segments.isDirectory()) {
			files = segments.listFiles(new ShapefileNameFilter());
		}
		else {
			files = new File[] {segments};
		}
		for (File f : files) {
			IntersectionWorker worker = new IntersectionWorker(training, f);
			worker.addPropertyChangeListener(this);
			executor.submit(worker);
		}
		// execute all the worker threads, one after another
		executor.shutdown();
	}
	
	
	/**
	 * Helper method.
	 * @return a header relative to the order in which the stats will be published.
	 */
	private String getStatsHeader() {
		return 
		// Clinton et al.
		"Filename,overSegmentation,wOverSegmentation,underSegmentation,wUnderSegmentation,"+
		// Lucieer and Stein
		"avgModDb,avgAFI,countOver,countUnder,"+
		// Moller
		"avgRPso,wAvgRPso,avgRPsub,wAvgRPsub,"+
		// weighted Moller
		"avgRAsuper,wAvgRAsuper,avgRAsub,wAvgRAsub,"+
		// Zhan
		"avgSimSize,wAvgSimSize,avgSDSimSize,sdSimSize,avgQLoq,wAvgQLoq,avgSDQLoc,sdQLoc,"+
		// Yang
		"underMerge,overMerge,"+
		// Weidner
		"qr,wQR";
	}
	
	/**
	 * Executes process intensive intersection of training and testing polygons, stats computation.
	 * @author owner
	 *
	 */
	class IntersectionWorker extends SwingWorker<String, String> {

		private File train;
		private File segmentation;
		
		// initialize
		public IntersectionWorker(File train, File segmentation) {
			this.train = train;
			this.segmentation = segmentation;
		}
		
		@Override
		protected String doInBackground() throws Exception {
			// first, construct the intersection
			Intersection intersection = new Intersection(train, segmentation);
			// compute statistics
			// Clinton stats.
			double[] stats = intersection.averageSegStats();
			double[] wStats = intersection.segStats();
			// Lucieer and Stein
			double[] lsStats = intersection.averageLucieerSteinStats();
			// Moller
			double[] mStats = intersection.averageMollerStats();
			// Zhan
			double[] zStats = intersection.averageZhanStats();
			// wieghted Zhan
			double[] wStats2 = intersection.segStats2();
			// Yang
			double[] yStats = intersection.sumYang();
			// Weidner
			double wStat = intersection.avgWeidner();
			
			return segmentation.getAbsolutePath()+","+
			// Clinton et al.
			stats[0]+","+wStats[0]+","+stats[1]+","+wStats[1]+","+
			// Lucieer and Stein
			lsStats[0]+","+lsStats[1]+","+lsStats[2]+","+lsStats[3]+","+
			// Moller
			mStats[0]+","+wStats2[3]+","+mStats[1]+","+wStats2[2]+","+mStats[2]+","+wStats2[1]+","+mStats[3]+","+wStats2[0]+","+
			// Zhan
			zStats[0]+","+wStats2[6]+","+zStats[1]+","+wStats2[7]+","+zStats[2]+","+wStats2[4]+","+zStats[3]+","+wStats2[5]+","+
			// Yang
			yStats[0]+","+yStats[1]+","+
			// Weidner
			wStat+","+wStats2[8];
		}
		
		@Override
	    protected void done() {
	        String text = null;
	        try {
	            text = get();
	            log.append(text+newline);
		        tableWriter.write(text);
	        } catch (Exception e) {
	            e.printStackTrace();
	            log.append("Whoops! No results.");
	        }
	    }
		
	}  // end class
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// run it
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new IntersectorGUI();
            }
        });
	}

}

