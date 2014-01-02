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

import java.io.File;

import org.apache.commons.math.stat.StatUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.berkenviro.gis.GISUtils;

/**
 * 
 * @author Nicholas Clinton
 *
 */
public class Intersection {

	private TrainingObject[] ta;
	private int totalSegs;
	
	/**
	 * Load the features and build the intersection.  Calling application is responsible for 
	 * checking that the files exist.
	 * @param training
	 * @param segs
	 */
	public Intersection(File training, File segmentation) {
		FeatureCollection<SimpleFeatureType, SimpleFeature> trainingObjects = GISUtils.getFeatureCollection(training);
		FeatureCollection<SimpleFeatureType, SimpleFeature> segments = GISUtils.getFeatureCollection(segmentation);
		makeArray(trainingObjects, segments);
	}
	
	/**
	 * 
	 * @param training
	 * @param segmentation
	 */
	public Intersection(FeatureCollection<SimpleFeatureType, SimpleFeature> training, 
					    FeatureCollection<SimpleFeatureType, SimpleFeature> segmentation) {
		makeArray(training, segmentation);
	}
	
	
	/*
	 * This method to do the iteration over all the objects, intitialize everything.
	 * Updated to GeoTools 2.7, 20100615  
	 */
	private void makeArray(FeatureCollection<SimpleFeatureType, SimpleFeature> trainingObjects, 
						   FeatureCollection<SimpleFeatureType, SimpleFeature> segmentation) {
		
		// instance variable initialization
		ta = new TrainingObject[trainingObjects.size()];
		totalSegs = 0;
		// iterate over the training objects
		FeatureIterator<SimpleFeature> iterator = trainingObjects.features();
		try {
			int i = 0;
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				ta[i] = new TrainingObject(feature, segmentation);
				totalSegs += ta[i].getNumIntersected();
				i++;
			}
		} catch (Exception e) {
				e.printStackTrace();
		} finally {
			trainingObjects.close(iterator);
		}
	}
		
	/*
	 * This method returns {overSegmentation, underSegmentation} when calculated 
	 * FROM EACH SEGMENT!!  WEIGHTED BY NUMBER OF SEGMENTS!!
	 */
	public double[] segStats() {
		double underSum = 0;
		double overSum = 0;
		int count = 0;
		double[][] segmentationStatArr;
		// iterate over the training objects
		for (int i=0; i<ta.length; i++) {
			segmentationStatArr = ta[i].getSegmentationStats();
			for (int j=0; j<segmentationStatArr[0].length; j++) {
				overSum += segmentationStatArr[0][j];
				underSum += segmentationStatArr[1][j];
				count++;
			}
		}
		double[] stats = {overSum/(double)count, underSum/(double)count};
		return stats;
		
	}
	
	/*
	 * returns {meanRAsub, meanRAsuper, meanRPsub, meanRPso, meanQLoq, sdQLoq, meanSimSize, sdSimSize, meanQR}
	 * WEIGHTED BY NUMBER OF SEGMENTS!!
	 */
	public double[] segStats2() {

		System.out.println("totalSegs= "+totalSegs);
		double[][] segmentationStatArr = new double[7][totalSegs];
		// iterate over the training objects
		int index = 0;
		for (int i=0; i<ta.length; i++) {
			double[][] toStats = ta[i].getSegmentationStats2();
			for (int j=0; j<toStats[0].length; j++) {
				for (int m=0; m<7; m++) {
					segmentationStatArr[m][index] = toStats[m][j];
				}
				index++;
			}
		}
		//double[] stats = {overSum/(double)count, underSum/(double)count};
		return new double[] {StatUtils.mean(segmentationStatArr[0]),
							 StatUtils.mean(segmentationStatArr[1]),
							 StatUtils.mean(segmentationStatArr[2]),
							 StatUtils.mean(segmentationStatArr[3]),
							 StatUtils.mean(segmentationStatArr[4]),
							 Math.sqrt(StatUtils.variance(segmentationStatArr[4])),
							 StatUtils.mean(segmentationStatArr[5]),
							 Math.sqrt(StatUtils.variance(segmentationStatArr[5])),
							 StatUtils.mean(segmentationStatArr[6])
							 };
	}
	
	/*
	 * This method returns {overSegmentation, underSegmentation} when calculated 
	 * FROM EACH TRAINING OBJECT!!
	 */
	public double[] averageSegStats() {
		double underSum = 0;
		double overSum = 0;
		int count = 0;
		double[] segmentationStatArr;
		for (int i=0; i<ta.length; i++) {
			segmentationStatArr = ta[i].getSegmentationAverages();
			overSum += segmentationStatArr[0];
			underSum += segmentationStatArr[1];
			count++;
		}
		return new double[] {overSum/(double)count, underSum/(double)count};	
	}
	
	/*
	 * Returns {Average ModDb, Average AFI, countOver, countUnder}
	 * WARNING!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 */
	public double[] averageLucieerSteinStats() {
		double ModDbSum = 0;
		double AFISum = 0;
		double countOver = 0;
		double countUnder = 0;
		int modDbcount = 0;
		int afiCount = 0;
		for (int i=0; i<ta.length; i++) {
			modDbcount++;
			afiCount++;
			
			// WARNING!! 	
			try {
				ModDbSum += ta[i].getModDb();
			} catch (Exception e1) {
				modDbcount--;
				e1.printStackTrace();
			}
			try {
				AFISum += ta[i].getAFI();
			} catch (Exception e1) {
				afiCount--;
				e1.printStackTrace();
			}
			
			try {
				if (ta[i].isOverSegmented()) { countOver++; }
				if (ta[i].isUnderSegmented()) { countUnder++; }
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
		}
		return new double[] {ModDbSum/(double)modDbcount, 
							 AFISum/(double)afiCount, 
							 countOver, 
							 countUnder
							 };
	}
	
	/*
	 * Returns Average of {AvgRPso, AvgRPsub, AvgRAsuper, AvgRAsub}
	 */
	public double[] averageMollerStats() {
		double AvgRPsoSum = 0;
		double AvgRPsubSum = 0;
		double AvgRAsuperSum = 0;
		double AvgRAsubSum = 0;
		int count = 0;
		for (int i=0; i<ta.length; i++) {
			// {AvgRPso, AvgRPsub, AvgRAsuper, AvgRAsub}
			double[] moller = ta[i].getMollerAvgs();
			AvgRPsoSum += moller[0];
			AvgRPsubSum += moller[1];
			AvgRAsuperSum += moller[2];
			AvgRAsubSum += moller[3];
			count++;
		}
		return new double[] {AvgRPsoSum/(double)count,
							 AvgRPsubSum/(double)count,
							 AvgRAsuperSum/(double)count,
							 AvgRAsubSum/(double)count,
							 };
	}
	
	/*
	 * Returns Average of {AvgSimSize, SDSimSize, AvgQLoq, SDQLoc}
	 */
	public double[] averageZhanStats() {
		double AvgSimSizeSum = 0;
		double SDSimSizeSum = 0;
		double AvgQLoqSum = 0;
		double SDQLocSum = 0;
		int count = 0;
		for (int i=0; i<ta.length; i++) {
			// {AvgSimSize, SDSimSize, AvgQLoq, SDQLoc}
			double[] zhan = ta[i].getZhanStats();
			AvgSimSizeSum += zhan[0];
			SDSimSizeSum += zhan[1];
			AvgQLoqSum += zhan[2];
			SDQLocSum += zhan[3];
			count++;
		}
		return new double[] {AvgSimSizeSum/(double)count,
							 SDSimSizeSum/(double)count,
							 AvgQLoqSum/(double)count,
							 SDQLocSum/(double)count,
							 };
	}
	
	/*
	 * Yang
	 */
	public double[] sumYang() {
		double underMerge = 0;
		double overMerge = 0;
		for (int i=0; i<ta.length; i++) {
			double[] yang = ta[i].getYangStats();
			underMerge+=yang[0];
			overMerge+=yang[1];
		}
		return new double[] {underMerge, overMerge};
	}
	
	/*
	 * Wiedner
	 */
	public double avgWeidner() {
		double qr = 0;
		int count = 0;
		for (int i=0; i<ta.length; i++) {
			qr+= ta[i].getWeidner();
			count++;
		}
		return qr/(double)count;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
	}
	
}
