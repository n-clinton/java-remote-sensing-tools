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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 * @author Nicholas Clinton 
 *  Legacy class.  Basically just automated processing from static methods.
 *	
 */
public class Intersector {
	
    /**
     * Write a big table according to each file in dir, if dir is a directory.  If dir is a file,
     * write a one line table for dir.  Legacy method.
     * @param dir
     * @param tableName
     * @param trainFile
     */
	public static void processDir2(File dir, String tableName, File trainFile) {

		File[] f = null;
		if (dir.isDirectory()) {
			f = dir.listFiles(new ShapefileNameFilter());
		}
		else {
			f = new File[] {dir};
		}
//		 set up the output file and iterate
		BufferedWriter writer = null;
		double[] wStats = null;
		double[] stats = null;
		try {
			writer = new BufferedWriter(new FileWriter(tableName));
			writer.write("Filename"+"\t"
							+"overSegmentation"
							+"\t"+"wOverSegmentation"
							+"\t"+"underSegmentation"
							+"\t"+"wUnderSegmentation"
							+"\t"+"avgModDb"
							+"\t"+"avgAFI"
							+"\t"+"countOver"
							+"\t"+"countUnder"
							+"\t"+"avgRPso"
							+"\t"+"wAvgRPso"
							+"\t"+"avgRPsub"
							+"\t"+"wAvgRPsub"
							+"\t"+"avgRAsuper"
							+"\t"+"wAvgRAsuper"
							+"\t"+"avgRAsub"
							+"\t"+"wAvgRAsub"
							+"\t"+"avgSimSize"
							+"\t"+"wAvgSimSize"
							+"\t"+"avgSDSimSize"
							+"\t"+"sdSimSize"
							+"\t"+"avgQLoq"
							+"\t"+"wAvgQLoq"
							+"\t"+"avgSDQLoc"
							+"\t"+"sdQLoc"
							// added 4/15
							+"\t"+"underMerge"
							+"\t"+"overMerge"
							// added 71308
							+"\t"+"qr"
							+"\t"+"wQR"
							);
			writer.newLine();
			writer.flush();
			for (File segs : f) {
				// after build, ready to report stats
				Intersection iSector = new Intersection(trainFile, segs);
				
				writer.write(segs.toString()+"\t");
				// Clinton stats.
				stats = iSector.averageSegStats();
				wStats = iSector.segStats();
				writer.write(stats[0]+"\t");
				writer.write(wStats[0]+"\t");
				writer.write(stats[1]+"\t");
				writer.write(wStats[1]+"\t");
				// Other metrics
				double[] lsStats = iSector.averageLucieerSteinStats();
				//{Average ModDb, Average AFI, countOver, countUnder}
				// write the Lucieer and Stein stats
				writer.write(lsStats[0]+"\t");
				writer.write(lsStats[1]+"\t");
				writer.write(lsStats[2]+"\t");
				writer.write(lsStats[3]+"\t");
				double[] mStats = iSector.averageMollerStats();
				//Average of {AvgRPso, AvgRPsub, AvgRAsuper, AvgRAsub}
				double[] zStats = iSector.averageZhanStats();
				//Average of {AvgSimSize, SDSimSize, AvgQLoq, SDQLoc}
				double[] wStats2 = iSector.segStats2();
//				{meanRAsub, meanRAsuper, meanRPsub, meanRPso, meanQLoq, sdQLoq, meanSimSize, sdSimSize, qr}
				//{0		, 1			 , 2		, 3		  , 4		, 5		, 6			 , 7	  , 8	}
				// Moller
				writer.write(mStats[0]+"\t");
				writer.write(wStats2[3]+"\t");
				writer.write(mStats[1]+"\t");
				writer.write(wStats2[2]+"\t");
				writer.write(mStats[2]+"\t");
				writer.write(wStats2[1]+"\t");
				writer.write(mStats[3]+"\t");
				writer.write(wStats2[0]+"\t");
				// Zhan
				writer.write(zStats[0]+"\t");
				writer.write(wStats2[6]+"\t");
				writer.write(zStats[1]+"\t");
				writer.write(wStats2[7]+"\t");
				writer.write(zStats[2]+"\t");
				writer.write(wStats2[4]+"\t");
				writer.write(zStats[3]+"\t");
				writer.write(wStats2[5]+"\t");
				// added 4/14
				double[] yStats = iSector.sumYang();
				writer.write(yStats[0]+"\t");
				writer.write(yStats[1]+"\t");
				// 071308
				double wStat = iSector.avgWeidner();
				writer.write(wStat+"\t");
				writer.write(wStats2[8]+"\t");
				
				writer.newLine();
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
    
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// ecog merged, 071408
		/*
		// The following accidentally overwritten.  Text file deleted.
		String tableName = "merged_eCog_071408.txt";
		String dirName = "C:/Documents and Settings/Nicholas Clinton/My Documents/BETI/segmentation_paper/paperV2/Ashley_shp_export_sf";
		String toString = "C:/Documents and Settings/Nicholas Clinton/My Documents/BETI/segmentation_files/training_shapes/merged_shapes_sp.shp";
		processDir(dirName, tableName, toString);
		// cars
		tableName = "cars_eCog_071408.txt";
		toString = "C:/Documents and Settings/Nicholas Clinton/My Documents/BETI/segmentation_files/training_shapes/seg_samples_005009101_state_plane_nick_edit.shp";
		processDir(dirName, tableName, toString);
		// trees
		tableName = "trees_eCog_071408.txt";
		toString = "C:/Documents and Settings/Nicholas Clinton/My Documents/BETI/segmentation_files/training_shapes/trees_sp.shp";
		processDir(dirName, tableName, toString);
		// buildings
		tableName = "buildings_eCog_071408.txt";
		toString = "C:/Documents and Settings/Nicholas Clinton/My Documents/BETI/segmentation_files/training_shapes/buildings_sp.shp";
		processDir(dirName, tableName, toString);
		
		// BIS071 merged, 071408 **********************************************************************************
		tableName = "merged_bis071_071408.txt";
		dirName = "C:/Documents and Settings/Nicholas Clinton/My Documents/BETI/segmentation_paper/paperV2/BerkeleyImageSeg071";
		toString = "C:/Documents and Settings/Nicholas Clinton/My Documents/BETI/segmentation_files/training_shapes/merged_shapes_sp.shp";
		processDir(dirName, tableName, toString);
		// cars
		tableName = "cars_bis07_071408.txt";
		toString = "C:/Documents and Settings/Nicholas Clinton/My Documents/BETI/segmentation_files/training_shapes/seg_samples_005009101_state_plane_nick_edit.shp";
		processDir(dirName, tableName, toString);
		// trees
		tableName = "trees_bis07_071408.txt";
		toString = "C:/Documents and Settings/Nicholas Clinton/My Documents/BETI/segmentation_files/training_shapes/trees_sp.shp";
		processDir(dirName, tableName, toString);
		// buildings
		tableName = "buildings_bis07_071408.txt";
		toString = "C:/Documents and Settings/Nicholas Clinton/My Documents/BETI/segmentation_files/training_shapes/buildings_sp.shp";
		processDir(dirName, tableName, toString);
		*/
		
		
		// 7/25/08 Esperanza runs
		/*
		String tableName = "cell_test_080808.txt";
		String dirName = "C:\\Esperanza_paper\\images\\cells_RNA_DNA3_T0";
		String toString = "C:\\Esperanza_paper\\training\\RNA_DNA_T0_cells.shp";
		processDir(dirName, tableName, toString);
		
		tableName = "greenie_test_080808.txt";
		dirName = "C:\\Esperanza_paper\\images\\chromosomes_RNA_DNA3_T0";
		toString = "C:\\Esperanza_paper\\training\\RNA_DNA_T0_greenies.shp";
		processDir(dirName, tableName, toString);
		
		tableName = "yellowie_test_080808.txt";
		dirName = "C:\\Esperanza_paper\\images\\chromosomes_RNA_DNA3_T0";
		toString = "C:\\Esperanza_paper\\training\\RNA_DNA_T0_yellowies.shp";
		processDir(dirName, tableName, toString);
		
		tableName = "reddie_test_080808.txt";
		dirName = "C:\\Esperanza_paper\\images\\chromosomes_RNA_DNA3_T0";
		toString = "C:\\Esperanza_paper\\training\\RNA_DNA_T0_reddies.shp";
		processDir(dirName, tableName, toString);
		*/
		
		///*
		// pyshapelib test runs
		/*
		Intersector iSector = new Intersector();
		double[] stats = iSector.averageSegStats();
		double[] wStats = iSector.segStats();
		System.out.println("overSegmentation= "+stats[0]);
		System.out.println("wOverSegmentation= "+wStats[0]);
		System.out.println("underSegmentation= "+stats[1]);
		System.out.println("wUnderSegmentation= "+wStats[1]);
		// Other metrics
		System.out.println("Computing Lucieer and Stein stats...");
		double[] lsStats = iSector.averageLucieerSteinStats();
		//{Average ModDb, Average AFI, countOver, countUnder}
		// write the Lucieer and Stein stats
		System.out.println("avgModDb= "+lsStats[0]);
		System.out.println("avgAFI= "+lsStats[1]);
		System.out.println("countOver= "+lsStats[2]);
		System.out.println("countUnder= "+lsStats[3]);
		System.out.println("Computing Moller et al. stats...");
		double[] mStats = iSector.averageMollerStats();
		//Average of {AvgRPso, AvgRPsub, AvgRAsuper, AvgRAsub}
		System.out.println("Computing Zhan et al. stats...");
		double[] zStats = iSector.averageZhanStats();
		//Average of {AvgSimSize, SDSimSize, AvgQLoq, SDQLoc}
		System.out.println("Computing weighted stats of Moller, Zhan... ");
		double[] wStats2 = iSector.segStats2();
//		{meanRAsub, meanRAsuper, meanRPsub, meanRPso, meanQLoq, sdQLoq, meanSimSize, sdSimSize, qr}
		//{0		, 1			 , 2		, 3		  , 4		, 5		, 6			 , 7	  , 8	}
		// Moller
		System.out.println("avgRPso= "+mStats[0]);
		System.out.println("wAvgRPso= "+wStats2[3]);
		System.out.println("avgRPsub= "+mStats[1]);
		System.out.println("wAvgRPsub= "+wStats2[2]);
		System.out.println("avgRAsuper= "+mStats[2]);
		System.out.println("wAvgRAsuper= "+wStats2[1]);
		System.out.println("avgRAsub= "+mStats[3]);
		System.out.println("wAvgRAsub= "+wStats2[0]);
		// Zhan
		System.out.println("avgSimSize= "+zStats[0]);
		System.out.println("wAvgSimSize= "+wStats2[6]);
		System.out.println("avgSDSimSize= "+zStats[1]);
		System.out.println("sdSimSize= "+wStats2[7]);
		System.out.println("avgQLoq= "+zStats[2]);
		System.out.println("wAvgQLoq= "+wStats2[4]);
		System.out.println("avgSDQLoc= "+zStats[3]);
		System.out.println("sdQLoc= "+wStats2[5]);
		// added 4/14
		System.out.println("Computing Zhan et al. stats...");
		double[] yStats = iSector.sumYang();
		System.out.println("underMerge= "+yStats[0]);
		System.out.println("overMerge= "+yStats[1]);
		// 071308
		System.out.println("Computing Weidner stat...");
		double wStat = iSector.avgWeidner();
		System.out.println("qr= "+wStat);
		System.out.println("wQR= "+wStats2[8]);
		//*/
		
		//String tableName = "cell_test_090308.txt";
		//String dirName = "C:\\Esperanza_paper\\images\\_84cells";
		//String toString = "C:\\Esperanza_paper\\training\\RNA_DNA_T0_cells.shp";
		//processDir(dirName, tableName, toString);
		
		// 20100425
		//String tableName = "mean_shift_whole_area_20100425.txt";
		//String dirName = "C:/Documents and Settings/Nicholas Clinton/Desktop/Asli/NewResults/MeanShift/wholearea/result";
		//String toString = "C:/Documents and Settings/Nicholas Clinton/Desktop/Asli/NewResults/MeanShift/wholearea/digitizedpolygons/digitized_polygons.shp";
		
		//String tableName = "BIS_pca_155_01_09_20100425.txt";
		//String dirName = "C:/Documents and Settings/Nicholas Clinton/Desktop/Asli/NewResults/BerkeleyImageSeg";
		//String toString = "C:/Documents and Settings/Nicholas Clinton/Desktop/Asli/NewResults/BerkeleyImageSeg/train/pca_train.shp";
		//processDir(dirName, tableName, toString);
		
		/*
		String tableName = "mean_shift_20100428.txt";
		String dirName = "C:/Users/nick/Documents/Asli_segmentation/orgMeanShiftseg";
		String toString = "C:/Users/nick/Documents/Asli_segmentation/NewResults/BerkeleyImageSeg/pca_train.shp";
		processDir(dirName, tableName, toString);
		*/
		
		// 20100610 and 20100615
		/*
		String tableName = "mean_shift_20100615_1.txt";
		String dirName = "C:/Users/nick/Documents/Asli_segmentation/MeanShiftObjects";
		//String dirName = "C:/Users/nick/Documents/Asli_segmentation/newsegment";
		String toString = "C:/Users/nick/Documents/Asli_segmentation/TrainingFiles/pca_train1.shp";
		processDir(dirName, tableName, toString);
		
		tableName = "mean_shift_20100615_2.txt";
		toString = "C:/Users/nick/Documents/Asli_segmentation/TrainingFiles/pca_train2.shp";
		processDir(dirName, tableName, toString);
		
		tableName = "mean_shift_20100615_3.txt";
		toString = "C:/Users/nick/Documents/Asli_segmentation/TrainingFiles/pca_train3.shp";
		processDir(dirName, tableName, toString);
		*/
		
		/*
		String tableName = "BIS_20100616_1.txt";
		String dirName = "C:/Users/nick/Documents/Asli_segmentation/newBISresults";
		String toString = "C:/Users/nick/Documents/Asli_segmentation/TrainingFiles/pca_train1.shp";
		processDir(dirName, tableName, toString);
		
		tableName = "BIS_20100616_2.txt";
		toString = "C:/Users/nick/Documents/Asli_segmentation/TrainingFiles/pca_train2.shp";
		//processDir(dirName, tableName, toString);
		
		tableName = "BIS_20100616_3.txt";
		toString = "C:/Users/nick/Documents/Asli_segmentation/TrainingFiles/pca_train3.shp";
		//processDir(dirName, tableName, toString);
		*/
		
		// 20100622 Still fails
		/*
		String tableName = "mean_shift_20100622_export.txt";
		String dirName = "C:/Users/nick/Documents/Asli_segmentation/MeanShiftObjects";
		String toString = "C:/Users/nick/Documents/Asli_segmentation/trainings1_2/Export_train1.shp";
		processDir(dirName, tableName, toString);
		
		tableName = "mean_shift_20100622_2.txt";
		toString = "C:/Users/nick/Documents/Asli_segmentation/trainings1_2/pca_train2.shp";
		processDir(dirName, tableName, toString);
		

		
		tableName = "BIS_20100622_export.txt";
		dirName = "C:/Users/nick/Documents/Asli_segmentation/newBISresults";
		toString = "C:/Users/nick/Documents/Asli_segmentation/trainings1_2/Export_train1.shp";
		processDir(dirName, tableName, toString);
		
		tableName = "BIS_20100622_2.txt";
		toString = "C:/Users/nick/Documents/Asli_segmentation/trainings1_2/pca_train2.shp";
		processDir(dirName, tableName, toString);
		*/
		
	}

}
