/**
 * 
 */
package com.berkenviro.imageprocessing;

import java.util.Enumeration;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericTransform;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Processing log
 * @author Nicholas
 *
 */
public class WekaUtilsProcessing extends WekaUtils {

	/**
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
//		String classatt = "uhi"; 
//		String out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/MSSU_uhi_continuous_20120517.csv";
//		String in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_h2o_no_zeros_70_uhi.arff";
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
//		classatt = "uhi_cdd";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhi_cdd_continuous_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhi.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhi_cdd_discrete_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhi_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// UHS ***********************************************
//		classatt = "uhs_1"; 
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhs_1_continuous_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70_uhs.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhs_1_discrete_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_joined_h2o_no_zeros_70_uhs_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// CDD
//		classatt = "uhs_cdd_1";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhs_cdd_1_continuous_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhs.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_uhs_cdd_1_discrete_20120517.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_r33_mssu_points_gpt2id_cdd_joined_h2o_no_zeros_70_uhs_discrete.arff";
//		rankDiscrete(in, out, classatt);

		
		// 20120526 GLA14 pixel averaging method.  Overlaid on waterbody masked MSSU (gpt2id)
//		String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros.csv";
//		Instances input = loadCSV(filename);
//		try {
//			Instances[] split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_70.arff");
//			writeArff(split[1], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_30.arff");
//			// uhi
//			Instances filt = removeAttributes(split[0], new int[] {0,1,10,11,12,13,14,15,16,17}); 
//			Instances rem = filterMissing(filt, "uhi");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_70_uhi.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhi");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_70_uhi_discrete.arff");
//			// uhs
//			filt = removeAttributes(split[0], new int[] {0,1,9,10,11,12,13,15,16,17}); 
//			rem = filterMissing(filt, "uhs_1");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_70_uhs.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_1");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_70_uhs_discrete.arff");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		// GLA14 waterbody masked CDD
//		filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd.csv";
//		input = loadCSV(filename);
//		try {
//			Instances[] split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd_70.arff");
//			writeArff(split[1], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd_30.arff");
//			// uhi
//			Instances filt = removeAttributes(split[0], new int[] {0,1,10,11,12,13,14,15,16,17}); 
//			Instances rem = filterMissing(filt, "uhi_cdd");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd_70_uhi.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhi_cdd");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd_70_uhi_discrete.arff");
//			// uhs
//			filt = removeAttributes(split[0], new int[] {0,1,9,10,11,12,13,15,16,17}); 
//			rem = filterMissing(filt, "uhs_cdd_1");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd_70_uhs.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_cdd_1");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd_70_uhs_discrete.arff");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20120526 pixel scale GLA14 ***********************************************************
		// UHI ***********************************************
//		String classatt = "uhi"; 
//		String out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhi_continuous_20120526.csv";
//		String in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_70_uhi.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhi_discrete_20120526.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_70_uhi_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// CDD
//		classatt = "uhi_cdd";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhi_cdd_continuous_20120526.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd_70_uhi.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhi_cdd_discrete_20120526.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd_70_uhi_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// UHS ***********************************************
//		classatt = "uhs_1"; 
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhs_1_continuous_20120526.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_70_uhs.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhs_1_discrete_20120526.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_70_uhs_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// CDD
//		classatt = "uhs_cdd_1";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhs_cdd_1_continuous_20120526.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd_70_uhs.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhs_cdd_1_discrete_20120526.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_cdd_70_uhs_discrete.arff";
//		rankDiscrete(in, out, classatt);

		
		// 20120527 K-means clustering with transformed data
//		String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_h2o_no_zeros_sin.csv";
//		Instances input = loadCSV(filename);
//		try {
//			Instances[] split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_h2o_no_zeros_sin_70.arff");
//			writeArff(split[1], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_h2o_no_zeros_sin_30.arff");
//			// uhi
//			Instances rem = filterMissing(split[0], "uhi");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_h2o_no_zeros_sin_70_uhi.arff");
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_cdd_h2o_no_zeros_sin.csv";
//		Instances input = loadCSV(filename);
//		try {
//			Instances[] split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_cdd_h2o_no_zeros_sin_70.arff");
//			writeArff(split[1], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_cdd_h2o_no_zeros_sin_30.arff");
//			// uhi
//			Instances rem = filterMissing(split[0], "uhi_cdd");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined4_cdd_h2o_no_zeros_sin_70_uhi.arff");
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// GLA14 waterbody masked CDD
//		String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50.csv";
//		Instances input = loadCSV(filename);
//		try {
//			
//			// uhi
//			Instances filt = removeAttributes(input, new int[] {0,1,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25}); 
//			Instances rem = filterMissing(filt, "uhi");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhi.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhi");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhi_discrete.arff");
//			// uhs
//			filt = removeAttributes(input, new int[] {0,1,9,10,11,12,13,15,16,17,18,19,20,21,22,23,24,25}); 
//			rem = filterMissing(filt, "uhs_1");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhs.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_1");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhs_discrete.arff");
//			
//			// CDDs
//			// uhi
//			filt = removeAttributes(input, new int[] {0,1,9,10,11,12,13,14,15,16,18,19,20,21,22,23,24,25}); 
//			rem = filterMissing(filt, "uhi_cdd");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhi_cdd.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhi_cdd");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhi_cdd_discrete.arff");
//			// uhs
//			filt = removeAttributes(input, new int[] {0,1,9,10,11,12,13,14,15,16,17,18,19,20,21,23,24,25}); 
//			rem = filterMissing(filt, "uhs_cdd_1");
//			writeArff(rem, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhs_cdd.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_cdd_1");
//			writeArff(disc, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhs_cdd_discrete.arff");
//			
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		
		// 20120526 pixel scale GLA14 BIG CITIES***********************************************************
		// UHI ***********************************************
//		String classatt = "uhi"; 
//		String out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhi_continuous_20120530.csv";
//		String in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhi.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhi_discrete_20120530.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhi_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// CDD
//		classatt = "uhi_cdd";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhi_cdd_continuous_20120530.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhi_cdd.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhi_cdd_discrete_20120530.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhi_cdd_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// UHS ***********************************************
//		classatt = "uhs_1"; 
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhs_1_continuous_20120530.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhs.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhs_1_discrete_20120530.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhs_discrete.arff";
//		rankDiscrete(in, out, classatt);
//		
//		// CDD
//		classatt = "uhs_cdd_1";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhs_cdd_1_continuous_20120530.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhs_cdd.arff";
//		rankContinuous(in, out, classatt);
//		
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_lattices_joined5_GLA14_2005_2009_uhs_cdd_1_discrete_20120530.csv";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_dn_gt_50_uhs_cdd_discrete.arff";
//		rankDiscrete(in, out, classatt);
		
//		String filename = "/Users/nclinton/Documents/urban/tables/gpt2_GLA14_pixel_averaging/" +
//				"gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_all.csv";
//		Instances input = loadCSV(filename);
//		System.out.println(input.toSummaryString());
//		try {
//			Instances[] split = subset(70, input);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "/Users/nclinton/Documents/urban/tables/gpt2_GLA14_pixel_averaging/" +
//					"gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_all_70.arff");
//			writeArff(split[1], "/Users/nclinton/Documents/urban/tables/gpt2_GLA14_pixel_averaging/" +
//					"gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_all_30.arff");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// from Mac.  Not sure what this is:
//		String filename = "/Users/nclinton/Documents/urban/tables/gpt2_GLA14_pixel_averaging/" +
//		"gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_all.csv";
//Instances input = loadCSV(filename);
//System.out.println(input.toSummaryString());
//try {
//	Instances[] split = subset(70, input);
//	System.out.println(split[0].toSummaryString());
//	System.out.println(split[1].toSummaryString());
//	writeArff(split[0], "/Users/nclinton/Documents/urban/tables/gpt2_GLA14_pixel_averaging/" +
//			"gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_all_70.arff");
//	writeArff(split[1], "/Users/nclinton/Documents/urban/tables/gpt2_GLA14_pixel_averaging/" +
//			"gpt2id_lattices_joined5_GLA14_2005_2009_heights_density_hw_no_zeros_all_30.arff");
//} catch (Exception e) {
//	e.printStackTrace();
//}
		
		// HP:
		// added response variables
//		String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros.csv";
//		Instances input = loadCSV(filename);
//		try {
//			Instances filt = removeAttributes(input, new int[] {0,1,27,34}); 
//			Instances[] split = subset(70, filt);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70.arff");
//			writeArff(split[1], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_30.arff");
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
//		try {
//			String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70.arff";
//			Instances input = loadArff(filename);
//			SimpleKMeans kMeans = new SimpleKMeans();
//			kMeans.setNumClusters(20);
//			kMeans.setMaxIterations(200);
//			kMeans.buildClusterer(input);
//			Instances clusters = kMeans.getClusterCentroids();
//			writeArff(clusters, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_k20.arff");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		try {
//			String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70.arff";
//			Instances input = loadArff(filename);
//			Instances filt = removeAttributes(input, new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,24,25,26,27,28,29,30,31,32,33,34}); 
//			Instances rem = filterMissing(filt, "uhbar");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhbar.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhbar");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhbar_discrete.arff");
			
//			Instances filt = removeAttributes(input, new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,
//					30,31,32,33,34}); 
//			Instances rem = filterMissing(filt, "uhbar_1");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhbar_d.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhbar_1");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhbar_d_discrete.arff");
			
//			
//			filt = removeAttributes(input, new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,26,27,28,29,30,31,32,33,34}); 
//			rem = filterMissing(filt, "uhi_12");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhi_12.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhi_12");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhi_12_discrete.arff");
//			
//			filt = removeAttributes(input, new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,33,34}); 
//			rem = filterMissing(filt, "uhs_12_13");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhs_12_13.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_12_13");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhs_12_13_discrete.arff");
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// ranking
		// added responses.  All CDD.
//		String classatt;
//		String out;
//		String in;
//		classatt = "uhbar";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhbar.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhbar_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhbar_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhbar_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		classatt = "uhi_12";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhi_12.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhi_12_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhi_12_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhi_12_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		classatt = "uhs_12_13";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhs_12_13.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhs_12_13_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhs_12_13_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhs_12_13_disc.csv";
//		rankDiscrete(in, out, classatt);
		
		// Non-CDD*****************************************************************************************************
//		String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros.csv";
//		Instances input = loadCSV(filename);
//		try {
//			Instances filt = removeAttributes(input, new int[] {0,1,27,34}); 
//			Instances[] split = subset(70, filt);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70.arff");
//			writeArff(split[1], "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_30.arff");
//			
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,24,25,26,27,28,29,30,31,32,33,34}); 
//			Instances rem = filterMissing(filt, "uhbar");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhbar.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhbar");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhbar_discrete.arff");
		
//		String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70.arff";
//		Instances input = loadArff(filename);
//		Instances filt = removeAttributes(input, new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,
//				30,31,32,33,34}); 
//		Instances rem = filterMissing(filt, "uhbar_1");
//		writeArff(rem, 
//				"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhbar_d.arff");
//		System.out.println("Discretizing...");
//		Instances disc = discretize(rem, "uhbar_1");
//		writeArff(disc, 
//				"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhbar_d_discrete.arff");
		
//			
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,26,27,28,29,30,31,32,33,34}); 
//			rem = filterMissing(filt, "uhi_12");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_70_uhi_12.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhi_12");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhi_12_discrete.arff");
//			
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,33,34}); 
//			rem = filterMissing(filt, "uhs_12_13");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhs_12_13.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_12_13");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhs_12_13_discrete.arff");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// ranking 
//		String classatt;
//		String out;
//		String in;
//		classatt = "uhbar_1";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhbar_d.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhbar_d_ann_cont.csv";
//		rankContinuous(in, out, classatt);
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhbar_d_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhbar_d_ann_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		classatt = "uhbar_1";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhbar_d.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhbar_d_cdd_cont.csv";
//		rankContinuous(in, out, classatt);
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_uhbar_d_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhbar_d_cdd_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhbar.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhbar_ann_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhbar_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhbar_ann_disc.csv";
//		rankDiscrete(in, out, classatt);
		
//		classatt = "uhi_12";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhi_12.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhi_12_ann_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhi_12_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhi_12_ann_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		classatt = "uhs_12_13";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhs_12_13.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhs_12_13_ann_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_ann_no_zeros_70_uhs_12_13_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/gpt2id_joined7_cumulative_responses_uhs_12_13_ann_disc.csv";
//		rankDiscrete(in, out, classatt);
		
		
		// GLA14 overlay *****************************************************************************************************
//		String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros.csv";
//		Instances input = loadCSV(filename);
//		try {
//			Instances filt = removeAttributes(input, new int[] {0,1,25,34,41,48,55}); 
//			Instances[] split = subset(70, filt);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70.arff");
//			writeArff(split[1], "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_30.arff");
//			
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,
//					30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52}); 
//			Instances rem = filterMissing(filt, "uhbar_n");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_n.arff");
//			System.out.println("Discretizing...");
//			Instances disc = discretize(rem, "uhbar_n");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_n_discrete.arff");
//			
//			/*
//			 * Haven't done this for the GPT2 lattices, above.
//			 */
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,
//					29,30,31,32,33,34,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52}); 
//			rem = filterMissing(filt, "uhbar_d");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_d.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhbar_d");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_d_discrete.arff");
//			/*
//			 * */
//			
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,
//					29,30,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52}); 
//			rem = filterMissing(filt, "uhi_n");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhi_n.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhi_n");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhi_n_discrete.arff");
//			
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,
//					29,30,31,32,33,34,35,36,37,39,40,41,42,43,44,45,46,47,48,49,50,51,52}); 
//			rem = filterMissing(filt, "uhs_d");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhs_d.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_d");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhs_d_discrete.arff");
//	
//			// CDD----------------------------------------
//
//
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,
//					29,30,31,32,33,34,35,36,37,38,39,40,42,43,44,45,46,47,48,49,50,51,52}); 
//			rem = filterMissing(filt, "uhbar_n_cdd");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_n_cdd.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhbar_n_cdd");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_n_cdd_discrete.arff");
//			
//			/*
//			 * Haven't done this for the GPT2 lattices, above.
//			 */
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,
//					29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,48,49,50,51,52}); 
//			rem = filterMissing(filt, "uhbar_d_cdd");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_d_cdd.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhbar_d_cdd");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_d_cdd_discrete.arff");
//			/*
//			 * */
//			
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,
//					29,30,31,32,33,34,35,36,37,38,39,40,41,42,44,45,46,47,48,49,50,51,52}); 
//			rem = filterMissing(filt, "uhi_n_cdd");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhi_n_cdd.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhi_n_cdd");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhi_n_cdd_discrete.arff");
//			
//			filt = removeAttributes(split[0], new int[] {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,
//					29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,51,52}); 
//			rem = filterMissing(filt, "uhs_d_cdd");
//			writeArff(rem, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhs_d_cdd.arff");
//			System.out.println("Discretizing...");
//			disc = discretize(rem, "uhs_d_cdd");
//			writeArff(disc, 
//					"C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhs_d_cdd_discrete.arff");
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// ranking

//		String classatt;
//		String out;
//		String in;
//		classatt = "uhbar_n";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_n.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhbar_n_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_n_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhbar_n_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		classatt = "uhbar_d";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_d.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhbar_d_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_d_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhbar_d_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		classatt = "uhi_n";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhi_n.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhi_n_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhi_n_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhi_n_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		classatt = "uhs_d";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhs_d.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhs_d_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhs_d_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhs_d_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		// CDD
//		classatt = "uhbar_n_cdd";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_n_cdd.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhbar_n_cdd_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_n_cdd_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhbar_n_cdd_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		classatt = "uhbar_d_cdd";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_d_cdd.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhbar_d_cdd_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhbar_d_cdd_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhbar_d_cdd_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		classatt = "uhi_n_cdd";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhi_n_cdd.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhi_n_cdd_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhi_n_cdd_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhi_n_cdd_disc.csv";
//		rankDiscrete(in, out, classatt);
//		
//		classatt = "uhs_d_cdd";
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhs_d_cdd.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhs_d_cdd_cont.csv";
//		rankContinuous(in, out, classatt);
//		
//		in = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_uhs_d_cdd_discrete.arff";
//		out = "C:/Users/Nicholas/Documents/urban/uhi/rankings/GLA14_2005_2009_responses_no_zeros_70_uhs_d_cdd_disc.csv";
//		rankDiscrete(in, out, classatt);
		
		
		// 20120717
//		try {
//			//String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70.arff";
//			String filename = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70.arff";
//			Instances input = loadArff(filename);
//			SimpleKMeans kMeans = new SimpleKMeans();
//			kMeans.setNumClusters(50);
//			kMeans.setMaxIterations(500);
//			kMeans.buildClusterer(input);
//			Instances clusters = kMeans.getClusterCentroids();
//			//writeArff(clusters, "C:/Users/Nicholas/Documents/urban/uhi/tables/GLA14_pix_avg_responses/GLA14_2005_2009_responses_no_zeros_70_k20.arff");
//			writeArff(clusters, "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_responses/gpt2id_joined7_cumulative_responses_no_zeros_no_zeros_70_k50.arff");
//			int[] membership = kMeans.getAssignments();
//			for (int i=0; i<membership.length; i++) {
//				System.out.println(i+", "+membership[i]);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		// 20130103
//		String fileName = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses.csv";
//		Instances instances = loadCSV(fileName);
//		
//		// TERRA big
//		// 
//		try {
//			String classatt = "uhi_max_n_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhs_min_d_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhi_max_n_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhs_min_d_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhbar_n_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhbar_d_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhbar_d_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhbar_n_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhi_sum_n_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhs_sum_d_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhi_sum_n_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhs_sum_d_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		// AQUA big
//		// 
//		try {
//			String classatt = "uhi_max_n_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhs_min_d_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhi_max_n_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhs_min_d_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhbar_n_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhbar_d_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhi_sum_n_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhs_sum_d_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhi_sum_n_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhs_sum_d_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhbar_n_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhbar_d_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/gpt2id_lattices_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
//		// GLA14, 20130117
//		String fileName = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses.csv";
//		Instances instances = loadCSV(fileName);
//		
//		// TERRA big
//		// 
//		try {
//			String classatt = "uhi_max_n_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhs_min_d_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhi_max_n_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhs_min_d_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhbar_n_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhbar_d_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhbar_d_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhbar_n_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhi_sum_n_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhs_sum_d_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhi_sum_n_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		// 
//		try {
//			String classatt = "uhs_sum_d_cdd_big_ter"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		// AQUA big
//		// 
//		try {
//			String classatt = "uhi_max_n_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhs_min_d_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhi_max_n_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhs_min_d_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhbar_n_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhbar_d_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhi_sum_n_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhs_sum_d_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhi_sum_n_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,74,75,76,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhs_sum_d_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,77,78,79,80,81,82,83,84,85,86,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhbar_n_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,87};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		try {
//			String classatt = "uhbar_d_cdd_big_aq"; // to keep
//			int[] toRemove = {7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86};
//			Instances filtered = removeAttributes(instances, toRemove); 
//			Instances continuous = filterMissing(filtered, classatt);
//			String out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_continuous.csv";
//			rankContinuous(continuous, out, classatt);
//			
//			Instances discrete = discretize(continuous, classatt);
//			out = "C:/Users/Nicholas/Documents/urban/uhi/tables/gpt2lattices_GLA14_terra_aqua/GLA14_joined8_2_responses_"+classatt+"_discrete.csv";
//			rankDiscrete(discrete, out, classatt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		// 20130321 global landcover meta-prediction
//		String filename = "/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc.csv";
//		Instances input = loadCSV(filename);
//		System.out.println(input.toSummaryString());
		try {
//			int[] toRemove = {0,11,12,13,14};
//			Instances removed = removeAttributes(input, toRemove); 
//			System.out.println(removed.toSummaryString());
//			String output = "/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc_removed.arff";
//			writeArff(removed, output);
			
			// nominalize with GUI
			// manually edit header to have complete list of classes
			// manually add codes 19,29,39,49,99,83,120 which are what??
//			filename = "/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc_removed_nominalized.arff";
//			Instances instances = loadArff(filename);
//			System.out.println(instances.toSummaryString());
//			instances.setClassIndex(2);
//			
//			Instances[] split = subset(70, instances);
//			System.out.println(split[0].toSummaryString());
//			System.out.println(split[1].toSummaryString());
//			writeArff(split[0], "/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc_removed_nominalized_70.arff");
//			writeArff(split[1], "/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc_removed_nominalized_30.arff");
//
//			Attribute[] predictors = {split[0].attribute("FROMGLCJ48"),
//					split[0].attribute("FROMGLCMLC"),
//					split[0].attribute("FROMGLCRF"),
//					split[0].attribute("FROMGLCSVM"),
//					split[0].attribute("FROMGLCseg"),
//					split[0].attribute("FROMGLCagg"),
//					split[0].attribute("FROMGLCaggv2")};
//			
//			double[] costs = new double[predictors.length];
//			int i=0;
//			for (Attribute a : predictors) {
//				System.out.println(a);
//				AttributeClassifier classy = new AttributeClassifier(a);
//				Evaluation evaluation = new Evaluation(split[0]);
//				evaluation.evaluateModel(classy, split[0]);
//				//System.out.println(evaluation.toSummaryString());
//				costs[i] = evaluation.errorRate();
//				//System.out.println(costs[i]);
//				i++;
//			}
//			Instances meta = makeTraining(split[0], predictors, costs);
//			writeArff(meta, "/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc_removed_nominalized_70_meta.arff");
			
			// now there is a meta-training
			// remove the true response, train a classifer
//			Instances instances70 = loadArff("/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc_removed_nominalized_70_meta.arff");
//			System.out.println(instances70.toSummaryString());
//			// get rid of the true (interpreted) response
//			instances70.deleteAttributeAt(2);
//			System.out.println(instances70.toSummaryString());
//			instances70.setClassIndex(instances70.numAttributes()-1);
//			Attribute classAtt = instances70.classAttribute(); // "response"
//			
//			RandomForest rf = new RandomForest();
//			rf.setNumTrees(50);
//			rf.buildClassifier(instances70);
//			
//			// trained.  Now, predict on the test
//			Instances unlabeled = loadArff("/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc_removed_nominalized_30.arff");
//			unlabeled.insertAttributeAt(classAtt, unlabeled.numAttributes());
//			unlabeled.setClassIndex(unlabeled.numAttributes()-1);
//			System.out.println("Class: "+unlabeled.classAttribute());
//			System.out.println(unlabeled.toSummaryString());
//			
//			Instances test = new Instances(unlabeled);
//			System.out.println(test.toSummaryString());
//			
//			Remove rm = new Remove();
//			rm.setAttributeIndicesArray(new int[] {2}); // "interpreted"
//			rm.setInputFormat(test);
//			rm.setInvertSelection(true);
//			FilteredClassifier fc = new FilteredClassifier();
//			fc.setClassifier(rf);
//			fc.setFilter(rm);
//			
//			for (int i = 0; i < unlabeled.numInstances(); i++) {
//				double clsLabel = fc.classifyInstance(unlabeled.instance(i));
//				test.instance(i).setClassValue(clsLabel);
//			}
//
//			writeArff(test, "/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc_removed_nominalized_removed_nominalized_30_meta.arff");
			// name change
			Instances test = loadArff("/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc_removed_nominalized_30_meta.arff");
//			Attribute[] predictors = {test.attribute("FROMGLCJ48"),
//					test.attribute("FROMGLCMLC"),
//					test.attribute("FROMGLCRF"),
//					test.attribute("FROMGLCSVM"),
//					test.attribute("FROMGLCseg"),
//					test.attribute("FROMGLCagg"),
//					test.attribute("FROMGLCaggv2")};
//			System.out.println(test.toSummaryString());
//			test.setClassIndex(2);
//			
//			double[] costs = new double[predictors.length];
//			int i=0;
//			for (Attribute a : predictors) {
//				System.out.println(a);
//				AttributeClassifier classy = new AttributeClassifier(a);
//				Evaluation evaluation = new Evaluation(test);
//				evaluation.evaluateModel(classy, test);
//				//System.out.println(evaluation.toSummaryString());
//				costs[i] = evaluation.errorRate();
//				System.out.println(costs[i]);
//				i++;
//			}
//			Attribute a = test.attribute("response");
//			System.out.println(a);
//			MetaAttributeClassifier classy = new MetaAttributeClassifier(a);
//			Evaluation evaluation = new Evaluation(test);
//			evaluation.evaluateModel(classy, test);
//			System.out.println(evaluation.toSummaryString());
//			double cost = evaluation.errorRate();
//			System.out.println(cost);
			
			Instances labeled = new Instances(test);
			Attribute interp = test.attribute(2).copy("meta_pred");
			labeled.insertAttributeAt(interp, labeled.numAttributes());
			labeled.setClassIndex(labeled.numAttributes()-1);
			
			Attribute a = test.attribute("response");
			MetaAttributeClassifier classy = new MetaAttributeClassifier(a);
			classy.buildClassifier(test);
			for (int i = 0; i < test.numInstances(); i++) {
				double clsLabel = classy.classifyInstance(test.instance(i));
				labeled.instance(i).setClassValue(clsLabel);
			}
			writeArff(labeled, "/Users/nclinton/Documents/data/WorkingPaperMetaPredictionAllMethodAcc_30_meta_labeled.arff");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
