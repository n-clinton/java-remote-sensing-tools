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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Random;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.attributeSelection.GreedyStepwise;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.LatentSemanticAnalysis;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.attributeSelection.SymmetricalUncertAttributeEval;
import weka.attributeSelection.WrapperSubsetEval;
import weka.classifiers.trees.REPTree;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.Resample;

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
	public static Instances removeAttributes(Instances in, int[] toRemove) throws Exception {
		Remove rm = new Remove();
		rm.setAttributeIndicesArray(toRemove);
		rm.setInputFormat(in);
		rm.setInvertSelection(true);
		return Filter.useFilter(in, rm);
	}
	
	/**
	 * 
	 * @param percent
	 * @param in
	 * @return
	 * @throws Exception
	 */
	public static Instances[] subset(int percent, Instances in) throws Exception {
		Instances[] out = new Instances[2];
		in.randomize(new Random());
		int subsetSize = (int)Math.round(in.numInstances()*(percent/100.0));
		out[0] = new Instances(in, 0, subsetSize);
		out[1] = new Instances(in, subsetSize, in.numInstances()-subsetSize);
		return out;
	}
	
	/**
	 * 
	 * @param in
	 * @param classatt
	 * @return
	 */
	public static Instances filterMissing(Instances in, String classatt) {
		Instances out = new Instances(in);
		out.setClass(out.attribute(classatt));
		out.deleteWithMissingClass();
		return out;
	}
	
	/**
	 * 
	 * @param in
	 * @param classatt
	 * @return
	 * @throws Exception
	 */
	public static Instances discretize(Instances in, String classatt) throws Exception {
		Discretize dis = new Discretize();
		dis.setAttributeIndicesArray(new int[] {in.attribute(classatt).index()});
		dis.setIgnoreClass(true);
		dis.setInputFormat(in);
		dis.setBins(20);
		dis.setUseEqualFrequency(true);
		return Filter.useFilter(in, dis);
	}
	
	/**
	 * 
	 * @param in
	 * @param eval
	 * @param search
	 * @return
	 * @throws Exception
	 */
	public static int[] getRanking(Instances in, ASEvaluation eval, ASSearch search) throws Exception {
	  AttributeSelection attsel = new AttributeSelection();
	  attsel.setEvaluator(eval);
	  attsel.setSearch(search);
	  attsel.setRanking(true);
	  attsel.SelectAttributes(in);
	  System.out.println(attsel.toResultsString());
	  double[][] ranks = attsel.rankedAttributes();
	  int[] indexrank = new int[in.numAttributes()];
	  for (int i=0; i<ranks.length; i++) {
		 indexrank[(int)ranks[i][0]] = i+1;
		  System.out.println("Attribute "+ranks[i][0]+" is ranked "+indexrank[(int)ranks[i][0]]);
	  }
	  return indexrank;
	}
	
	/**
	 * 
	 * @param in
	 * @param out
	 * @param classatt
	 */
	public static void rankContinuous(String in, String out, String classatt) {
		Instances cont = loadArff(in);
		//System.out.println(cont.toSummaryString());
		cont.setClass(cont.attribute(classatt));
		System.out.println("Evaluating class "+ cont.attribute(cont.classIndex()));
		
		try {
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(out)));
			writer.write("attribute,");
			// fill this one up
			int[][] ranks = new int[3][cont.numAttributes()];
			
			int eval = 0;
			CfsSubsetEval cfs_sub = new CfsSubsetEval();
			GreedyStepwise greedy = new GreedyStepwise();
			greedy.setGenerateRanking(true);
			greedy.setSearchBackwards(false);
			ranks[eval] = getRanking(cont, cfs_sub, greedy);
			writer.write("CfsSubsetEval_GreedyStepwise,");
			
			eval++;
			ReliefFAttributeEval relief = new ReliefFAttributeEval();
			relief.setSampleSize((int)cont.numInstances()/2);
			Ranker ranker = new Ranker();
			ranker.setGenerateRanking(true);
			ranks[eval] = getRanking(cont, relief, ranker);
			writer.write("ReliefF_Ranker,");
			
//			eval++;
//			LatentSemanticAnalysis analysis = new LatentSemanticAnalysis();
//			Ranker ranker = new Ranker();
//			ranker.setGenerateRanking(true);
//			ranks[eval] = getRanking(cont, analysis, ranker);
//			writer.write("LatentSemantic_Ranker,");
			
			eval++;
			WrapperSubsetEval wrapper = new WrapperSubsetEval();
			REPTree tree = new REPTree();
			tree.setMinNum(100); // minimum node size, all else default
			wrapper.setClassifier(tree);
			greedy = new GreedyStepwise();
			greedy.setSearchBackwards(false);
			greedy.setGenerateRanking(true);
			ranks[eval] = getRanking(cont, wrapper, greedy);
			writer.write("WrappedREPTree_GreedyStepwise,");
			
			writer.newLine();
			for (int a=0; a<cont.numAttributes(); a++) {
				writer.write(cont.attribute(a).name()+",");
				for (int c=0; c<ranks.length; c++) {
					writer.write(ranks[c][a]+",");
				}
				writer.newLine();
			}
			
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 
	 * @param in
	 * @param out
	 * @param classatt
	 */
	public static void rankDiscrete(String in, String out, String classatt) {
		Instances cont = loadArff(in);
		cont.setClass(cont.attribute(classatt));
		System.out.println("Evaluating class "+ cont.attribute(cont.classIndex()));
		
		try {
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(out)));
			writer.write("attribute,");
			// fill this one up
			int[][] ranks = new int[4][cont.numAttributes()];
			
			int eval = 0;
			ChiSquaredAttributeEval chi = new ChiSquaredAttributeEval();
			Ranker ranker = new Ranker();
			ranker.setGenerateRanking(true);
			ranks[eval] = getRanking(cont, chi, ranker);
			writer.write("ChiSquared_Ranker,");
			
			eval++;
			GainRatioAttributeEval gain = new  GainRatioAttributeEval();
			ranker = new Ranker();
			ranker.setGenerateRanking(true);
			ranks[eval] = getRanking(cont, gain, ranker);
			writer.write("GainRatio_Ranker,");
			
			eval++;
			InfoGainAttributeEval info = new InfoGainAttributeEval();
			ranker = new Ranker();
			ranker.setGenerateRanking(true);
			ranks[eval] = getRanking(cont, info, ranker);
			writer.write("InfoGain_Ranker,");
			
			eval++;
			SymmetricalUncertAttributeEval sym = new SymmetricalUncertAttributeEval();
			ranker = new Ranker();
			ranker.setGenerateRanking(true);
			ranks[eval] = getRanking(cont, sym, ranker);
			writer.write("SymmetricUncertainty_Ranker,");
			
			writer.newLine();
			for (int a=0; a<cont.numAttributes(); a++) {
				writer.write(cont.attribute(a).name()+",");
				for (int c=0; c<ranks.length; c++) {
					writer.write(ranks[c][a]+",");
				}
				writer.newLine();
			}
			
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		
//		Instances inst1 = loadArff("F:\\Cheatgrass2008\\training\\p37r32_2005_id.arff");
//		Instances inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r34_2005_id.arff");
//		Instances combo = combineInstances(inst1, inst2);
//		
//		inst1 = combo;
//		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r31_2006_id.arff");
//		combo = combineInstances(inst1, inst2);
//		
//		inst1 = combo;
//		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r32_2006_id.arff");
//		combo = combineInstances(inst1, inst2);
//		
//		inst1 = combo;
//		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r33_2006_id.arff");
//		combo = combineInstances(inst1, inst2);
//		
//		inst1 = combo;
//		inst2 = loadArff("F:\\Cheatgrass2008\\training\\p38r34_2006_id.arff");
//		combo = combineInstances(inst1, inst2);
//		
//		writeArff(combo, "F:\\Cheatgrass2008\\training\\Makr_0506_combo_id.arff");
		
		// 20120426 split the urban instances
//		String filename = "/Users/nclinton/Documents/gpt2id_lattices_joined2_pop_nightlights_no_zeros.arff";
//		Instances input = loadArff(filename);
//		Instances[] split = null;
//		try {
//			split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		writeArff(split[0], "/Users/nclinton/Documents/gpt2id_lattices_joined2_pop_nightlights_no_zeros_70.arff");
//		writeArff(split[1], "/Users/nclinton/Documents/gpt2id_lattices_joined2_pop_nightlights_no_zeros_30.arff");
		
		
		// GLA14
//		String filename = "/Users/nclinton/Documents/urban/lidar/GLA14_r33_mssu_points_gpt2id_joined_nightlights_no_zeros.arff";
//		Instances input = loadArff(filename);
//		Instances[] split = null;
//		try {
//			split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		writeArff(split[0], "/Users/nclinton/Documents/urban/lidar/GLA14_r33_mssu_points_gpt2id_joined_nightlights_no_zeros_70.arff");
//		writeArff(split[1], "/Users/nclinton/Documents/urban/lidar/GLA14_r33_mssu_points_gpt2id_joined_nightlights_no_zeros_30.arff");
		
//		String filename = "/Users/nclinton/Documents/urban/gpt2id_lattices_joined3_cdd_attributes_pop_nightlights_no_zeros.arff";
//		Instances input = loadArff(filename);
//		Instances[] split = null;
//		try {
//			split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		writeArff(split[0], "/Users/nclinton/Documents/urban/gpt2id_lattices_joined3_cdd_attributes_pop_nightlights_no_zeros_70.arff");
//		writeArff(split[1], "/Users/nclinton/Documents/urban/gpt2id_lattices_joined3_cdd_attributes_pop_nightlights_no_zeros_30.arff");
		
		
//		String mssu = "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_h2o_no_zeros.csv";
//		Instances inst = loadCSV(mssu);
//		System.out.println(inst.toSummaryString());
//		Instances filtered = filterMissing(inst, "uhi");
//		System.out.println(filtered.toSummaryString());
		
//		String filename = "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_h2o_no_zeros.csv";
//		Instances input = loadCSV(filename);
//		Instances[] split = null;
//		try {
//			split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		writeArff(split[0], "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_h2o_no_zeros_70.arff");
//		writeArff(split[1], "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_h2o_no_zeros_30.arff");
		
//		String filename = "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_h2o_no_zeros_70.arff";
//		Instances in = loadArff(filename);
//		// uhi
//		try {
//			//Instances filt = removeAttributes(in, new int[] {0,1,10,12,13,14,15,16}); // uhi
//			//Instances filt = removeAttributes(in, new int[] {0,1,9,10,11,12,13,15}); // uhs
//			//Instances rem = filterMissing(filt, "uhs_1");
//			//writeArff(rem, "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_h2o_no_zeros_70_uhs.arff");
//			Instances rem = loadArff("/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_h2o_no_zeros_70_uhi.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhi");
//			System.out.println(disc.toSummaryString());
//			writeArff(disc, "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_h2o_no_zeros_70_uhi_discrete.arff");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		String filename = "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_cdd_h2o_no_zeros.csv";
//		Instances input = loadCSV(filename);
//		try {
//			Instances[] split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_cdd_h2o_no_zeros_70.arff");
//			writeArff(split[1], "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_cdd_h2o_no_zeros_30.arff");
//			
//			Instances filt = removeAttributes(split[0], new int[] {0,1,10,12,13,14,15,16}); // uhi
//			Instances rem = filterMissing(filt, "uhi");
//			writeArff(rem, "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_cdd_h2o_no_zeros_70_uhi.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhi");
//			writeArff(disc, "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_cdd_h2o_no_zeros_70_uhi_discrete.arff");
//			
//			filt = removeAttributes(split[0], new int[] {0,1,9,10,11,12,13,15}); // uhs
//			rem = filterMissing(filt, "uhs_1");
//			writeArff(rem, "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_cdd_h2o_no_zeros_70_uhs.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_1");
//			writeArff(disc, "/Users/nclinton/Documents/urban/gpt2id_lattices_joined4_cdd_h2o_no_zeros_70_uhs_discrete.arff");	
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		// GLA14 waterbody masked
//		String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros.csv";
//		Instances input = loadCSV(filename);
//		try {
//			Instances[] split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70.arff");
//			writeArff(split[1], "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_30.arff");
//			// uhi
//			Instances filt = removeAttributes(split[0], new int[] {23,24,30,32,33,34,35,36}); 
//			Instances rem = filterMissing(filt, "uhi");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70_uhi.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhi");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70_uhi_discrete.arff");
//			// uhs
//			filt = removeAttributes(split[0], new int[] {23,24,29,30,31,32,33,35}); 
//			rem = filterMissing(filt, "uhs_1");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70_uhs.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_1");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70_uhs_discrete.arff");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		// GLA14 waterbody masked CDD
//		filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros.csv";
//		input = loadCSV(filename);
//		try {
//			Instances[] split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70.arff");
//			writeArff(split[1], "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_30.arff");
//			// uhi
//			Instances filt = removeAttributes(split[0], new int[] {23,24,30,32,33,34,35,36}); 
//			Instances rem = filterMissing(filt, "uhi_cdd");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhi.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhi_cdd");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhi_discrete.arff");
//			// uhs
//			filt = removeAttributes(split[0], new int[] {23,24,29,30,31,32,33,35}); 
//			rem = filterMissing(filt, "uhs_cdd_1");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhs.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_cdd_1");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhs_discrete.arff");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// 20120517 Attribute Rankings MSSU
		// UHI ***********************************************
		String classatt = "uhi"; 
		String out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/MSSU_uhi_continuous_20120517.csv";
		String in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_h2o_no_zeros_70_uhi.arff";
//		System.out.println(Calendar.getInstance().getTime());
//		rankContinuous(in, out, classatt);
//		System.out.println(Calendar.getInstance().getTime());
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/MSSU_uhi_discrete_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_h2o_no_zeros_70_uhi_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// CDD
//		classatt = "uhi_cdd";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/MSSU_uhi_cdd_continuous_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_cdd_h2o_no_zeros_70_uhi.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/MSSU_uhi_cdd_discrete_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_cdd_h2o_no_zeros_70_uhi_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// UHS ***********************************************
//		classatt = "uhs_1"; 
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/MSSU_uhs_1_continuous_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_h2o_no_zeros_70_uhs.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/MSSU_uhs_1_discrete_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_h2o_no_zeros_70_uhs_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// CDD
//		classatt = "uhs_cdd_1";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/MSSU_uhs_cdd_1_continuous_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_cdd_h2o_no_zeros_70_uhs.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/MSSU_uhs_cdd_1_discrete_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_cdd_h2o_no_zeros_70_uhs_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// GLA14 ***********************************************************
//		// UHI ***********************************************
//		classatt = "uhi"; 
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhi_continuous_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70_uhi.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhi_discrete_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70_uhi_discrete.arff";
//		rankDiscrete(in, out, classatt);
		
		// CDD
		classatt = "uhi_cdd";
		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhi_cdd_continuous_20120517.csv";
		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhi.arff";
		rankContinuous(in, out, classatt);
		
		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhi_cdd_discrete_20120517.csv";
		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhi_discrete.arff";
		rankDiscrete(in, out, classatt);
		
		// UHS ***********************************************
		classatt = "uhs_1"; 
		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhs_1_continuous_20120517.csv";
		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70_uhs.arff";
		rankContinuous(in, out, classatt);
		
		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhs_1_discrete_20120517.csv";
		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70_uhs_discrete.arff";
		rankDiscrete(in, out, classatt);
		
		// CDD
		classatt = "uhs_cdd_1";
		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhs_cdd_1_continuous_20120517.csv";
		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhs.arff";
		rankContinuous(in, out, classatt);
		
		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhs_cdd_1_discrete_20120517.csv";
		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhs_discrete.arff";
		rankDiscrete(in, out, classatt);
				
		
	}

}
