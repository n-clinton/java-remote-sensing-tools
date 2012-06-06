/**
 * 
 */
package com.berkenviro.imageprocessing;

import weka.core.Instances;

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
		
	}

}
