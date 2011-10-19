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
package com.berkenviro.imageprocessing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import weka.classifiers.Classifier;
import weka.classifiers.SingleClassifierEnhancer;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.meta.Bagging;
import weka.classifiers.trees.J48;
import weka.core.Instances;

/**
 * Experimental skeleton class.  Incomplete.
 * 
 * @author Nicholas Clinton
 */
public class ThreadUtils {
	
	/**
	 * Make a linked list of J48 tree based classifiers 
	 * with all the parameter combinations to be tested.
	 * Returns a LinkedList of Classifiers 
	 */
	public static LinkedList J48List() {
		
		LinkedList list = new LinkedList();
		J48 tree; 
		Classifier c = null;
		boolean[] trueFalse = {true, false};
		
		for (double conf=0.05; conf<0.8; conf+=0.05) {
		//for (double conf=0.05; conf<0.11; conf+=0.05) {
			
			for (int minNum=1; minNum<11; minNum++) {
			//for (int minNum=1; minNum<2; minNum++) {
				
				for (boolean pruning : trueFalse) {
					
					for (boolean bagging : trueFalse) {
						if (!bagging) {
							for (boolean boosting : trueFalse) {
								if (boosting) { // boosting
									tree = new J48();
									tree.setConfidenceFactor((float)conf);
									tree.setMinNumObj(minNum);
									tree.setUnpruned(!pruning);
									
									AdaBoostM1 boost = new AdaBoostM1();
									boost.setNumIterations(10);
									boost.setClassifier(tree);
									c = boost;
								}
								else { // no enhancement
									tree = new J48();
									tree.setConfidenceFactor((float)conf);
									tree.setMinNumObj(minNum);
									tree.setUnpruned(!pruning);
									c = tree;
								}

								list.add(c);
							}
						}
						else { // bagging
							tree = new J48();
							tree.setConfidenceFactor((float)conf);
							tree.setMinNumObj(minNum);
							tree.setUnpruned(!pruning);
							
							Bagging bag = new Bagging();
							bag.setNumIterations(100);
							bag.setClassifier(tree);
							c = bag;
							list.add(c);
						}
					}
				}
			}
		}
		
		return list;
		
	}
	
	/**
	 * Helper method for J48List().  Get info about the classifier.
	 * @param c is a parameterized classifier
	 * @return a string representation of the parameters
	 */
	public static String getClassifierInfo(Classifier c) {
		//
		String s = "";
		J48 tree;
		
		if (c instanceof Bagging || c instanceof AdaBoostM1) {
			s+=c.getClass().getSimpleName()+"\t";
			tree = (J48) ((SingleClassifierEnhancer)c).getClassifier();
		}
		else {
			tree = (J48) c;
		}		
		String pruning = (tree.getUnpruned() ? "" : "pruning");
		
		s+="\t"+tree.getConfidenceFactor()+"\t"+tree.getMinNumObj()+"\t"+pruning;
		return s;
	}
	
	/**
	 * Test code and processing log.
	 * @param args
	 */
	public static void main(String[] args) {
		
		Instances metaTraining = WekaUtils.loadArff("F:\\cheatgrass2008_testing\\Makr_0506_cross_appended_id_meta.arff");
		
		// Make an ExecutorService
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService service = Executors.newFixedThreadPool(threads);
		// Give it to a completion service
		CompletionService ecs = new ExecutorCompletionService(service);

		
		// LinkedList of Classifiers, J48 based 
		LinkedList list = J48List();
		

		// submit everything to the service, 
		// o is a parameterized Classifier to be tested using the CallableCross
		for (Object o : list) {
			
			System.out.println(getClassifierInfo((Classifier) o));
			///*
			Callable myTask = null;
			ecs.submit(myTask);
			//*/
		}
		// Done.  
		///*
		// set up the FileWriter.
		String shortRep = "F:\\cheatgrass2008_testing\\060208_J48_report.txt";
		BufferedWriter shortWriter = null;
		try {
			shortWriter = new BufferedWriter(new FileWriter(shortRep));
			// set up the header
			shortWriter.write("enhance"+"\t"+"conf"+"\t"+"minNum"+"\t"+"pruning"+"\t"+"rmse"+"\t"+"corr");
			shortWriter.newLine();
			shortWriter.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Now, get all the results
		for (int i=0; i < list.size(); i++) {
			String line = "";
			try {
				// the CallableCross.CrossResult
				Object result = null;
				
				if (result != null) {

					try {
						shortWriter.write(line);
						shortWriter.newLine();
						shortWriter.flush();
						System.out.println(line);
					} catch (IOException e) {
						e.printStackTrace();
					}
		        }
				else {
					System.out.println("Whoops!  result = null");
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
	    }

//*/
	}

}
