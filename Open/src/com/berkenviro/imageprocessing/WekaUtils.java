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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Various utilities using the Weka API.
 * 
 * @author Nicholas Clinton
 */
public class WekaUtils {

	/**
	 * Combine the datasets, checking compatibility.
	 * 
	 * @param in1 Instances to be combined with in2
	 * @param in2 Instances to be combined with in1
	 * @return the combined Instances
	 */
	public static Instances combineInstances(Instances in1, Instances in2) {
		
		// copy the first input
		Instances out = new Instances(in1);
		Enumeration enum2 = in2.enumerateInstances();
		Instance curInst;
		while (enum2.hasMoreElements()) {
			curInst = (Instance) enum2.nextElement();
			// if it's not compatible, return null and print an error
			if (!out.checkInstance(curInst)) {
				System.err.println("Instances are not compatible!!");
				return null;
			}
			out.add(curInst);
		}
		return out;
	}
	
	/**
	 * Load an Arff file
	 * @param filename is the full path to the input file
	 * @return the Instances loaded from disk.
	 */
	public static Instances loadArff(String filename) {
		Instances out = null;
		try {
			File f = new File(filename);
			out = new Instances(new FileReader(f));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}
	
	/**
	 * Load instances from a CSV file
	 * @param filename is the full path to the input file
	 * @return the Instances loaded from disk.
	 */
	public static Instances loadCSV(String filename) {
		
		CSVLoader loader = new CSVLoader();
		Instances out = null;
		try {		
			File f = new File(filename);
			loader.setSource(f);
			out = loader.getDataSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}
	
	/**
	 * Saves an Arff to filename.
	 * @param in is the Instances to be saved
	 * @param filename is the full path of the output
	 */
	public static void writeArff(Instances in, String filename) {

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			writer.write(in.toString());
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove attributes, the only good way as far as I can tell since 
	 * Instances.removeAttributeAt() is apparently broken.  Observe that methods
	 * invoked on rm must be applied EXACTLY in this order, or it won't work.
	 * @param in
	 * @param toRemove is the attribute indices to remove
	 */
	public static Instances removeAttributes(Instances in, int[] toRemove) {
		Instances out = null;
		
		Remove rm = new Remove();
		rm.setAttributeIndicesArray(toRemove);

		try {
			rm.setInputFormat(in);
			rm.setInvertSelection(true);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		try {
			out = Filter.useFilter(in, rm);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return out;
	}
	
	
	
	
	/**
	 * Test code and processing log.
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		Instances inst1 = loadArff("F:\\Cheatgrass2008\\training\\p37r32_2005.arff");
		Instances inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r34_2005.arff");
		Instances combo = combineInstances(inst1, inst2);
		
		inst1 = combo;
		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r31_2006.arff");
		combo = combineInstances(inst1, inst2);
		
		inst1 = combo;
		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r32_2006.arff");
		combo = combineInstances(inst1, inst2);
		
		inst1 = combo;
		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r33_2006.arff");
		combo = combineInstances(inst1, inst2);
		
		inst1 = combo;
		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r34_2006.arff");
		combo = combineInstances(inst1, inst2);
		
		writeArff(combo, "F:\\Cheatgrass2008\\training\\Makr_0506_combo.arff");
		*/
		
		Instances inst1 = loadArff("F:\\Cheatgrass2008\\training\\p37r32_2005_id.arff");
		Instances inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r34_2005_id.arff");
		Instances combo = combineInstances(inst1, inst2);
		
		inst1 = combo;
		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r31_2006_id.arff");
		combo = combineInstances(inst1, inst2);
		
		inst1 = combo;
		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r32_2006_id.arff");
		combo = combineInstances(inst1, inst2);
		
		inst1 = combo;
		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r33_2006_id.arff");
		combo = combineInstances(inst1, inst2);
		
		inst1 = combo;
		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r34_2006_id.arff");
		combo = combineInstances(inst1, inst2);
		
		writeArff(combo, "F:\\Cheatgrass2008\\training\\Makr_0506_combo_id.arff");
		
		
		
	}

}
