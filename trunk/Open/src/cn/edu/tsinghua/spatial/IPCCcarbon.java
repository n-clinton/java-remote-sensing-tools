package cn.edu.tsinghua.spatial;


import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.math.stat.StatUtils;

import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.neighboursearch.BallTree;
import cn.edu.tsinghua.timeseries.ImageLoadr5;

import com.berkenviro.imageprocessing.GDALUtils;
import com.berkenviro.imageprocessing.ImageClassifier2;
import com.berkenviro.imageprocessing.JAIUtils;
import com.berkenviro.imageprocessing.WekaUtils;


/**
 * This class outputs predicted carbon using IPCC methods given landcover and ecoregion inputs.
 * Calling classes are responsible for using IPCC recognized landcover and ecoregion codes as
 * represented by static variables.
 * 
 * @author jiayi
 * @author nclinton
 * 
 */

public class IPCCcarbon {

	/*
	 * Instance variables defining IPCC's understanding of landcover types.
	 */
	static int CROPLAND = 2;
	static int FOREST = 1;
	static int GRASSLAND = 3;
	static int SETTLEMENT = 4;
	static int WETLAND = 5;
	static int NOCarbon = 0;	
	static int OTHER = 9;  // unknown type
	
	/*
	 * Types not explicitly handled by IPCC
	 */
	static int SHRUBLAND = 10;
	static int SAVANNA = 11;

	static int TROPICAL_ZONE = 1;
	static int SUB_TROPICAL_ZONE = 2;
	static int TEMPERATE_ZONE = 3;
	static int BOREAL_ZONE = 4;
	static int POLAR_ZONE = 5;

	private boolean print;

	/**
	 * Given the data necessary to generate dry months 
	 * @param temperatures is (12 months) of mean monthly temp (C)
	 * @param precipitation is annual (12 months) of precip (mm)
	 * @return number of dry months
	 */
	public static int drymonth(double[] prec, double[] temp) {
		int y=0;
		for (int i=1; i<12; i++) {
			if (prec[i]<=2.0*temp[i]) {
				y++; 
			}
		}
		return y;
	}

	/**
	 * Given the data necessary to decide semi-arid region
	 * These climates are characterized as actual precipitation < a threshold value (in millimeters) set equal to the potential evapotranspiration
	 * McKnight, Tom L; Hess, Darrel (2000). "Climate Zones and Types". Physical Geography: A Landscape Appreciation. Upper Saddle River, NJ: Prentice Hall. ISBN 0-13-020263-0.
	 * @param precipitation is annual (12 months) of precip (mm)
	 * @param ET is evapotranspiration
	 * @return boolean
	 */
	public static boolean semiarid(double[] prec, double ET) {
		int y=0;
		for (int i=1; i<12; i++) {
			if (prec[i]<=ET) {
				y++; 
			}
		}
		if (y==0)
			return true;
		else
			return false;
	}

	/**
	 * Given the data necessary to generate temperature zone 
	 * @param temperatures are (12 months) of mean monthly temp (C)
	 * @return tempzone code: 1-tropical, 2-sub-tropical, 3-temperate, 4-boreal, 5-polar
	 */
	public static int tempZone(double[] temp) {
		int count1=0;
		int count2=0;
		int tempZ=0;
		for (int i=1; i<12; i++) {
			if(temp[i]>10)
				count1++;
			if(temp[i]<0)
				count2++;
		}
		if(count1==0){
			tempZ=5;
		}			
		else if(count1<=3){
			tempZ=4;
		}
		else if(count1<=8){
			tempZ=3;
		}
		else if(count2==0){
			tempZ=2;
		}
		else{
			tempZ=1;
		}

		return tempZ;
	}
	
	
	/**
	 * Generate ecoregion using FAO method and/or data in IPCC table 4.1
	 * @param precipitation is annual (12 months) of precip (mm)
	 * @param temperatures is (12 months) of mean monthly temp (C)
	 * @return 
	 */
	public static int ecoregion(double[] prec, double[] temp,  double elev, double ET, int lc) {
		int tempZ = tempZone(temp);
		//	System.out.println("\t Temp zone: "+tempZ);
		int dryM = drymonth(prec, temp);
		//	System.out.println("\t Number of dry months: "+dryM);
		//Tropical
		if (tempZ==1) {
			if(elev >1000){
				//Tropical mountain
				return 106;
			}
			else if(dryM==12){
				//Tropical desert
				return 105;
			}
			else if(semiarid(prec, ET)){
				//Tropical semi-arid (shrubland)
				return 104;
			}
			else if(dryM>=6){
				//Tropical dry/wet
				return 103;
			}
			else if(dryM>=4){
				//Tropical wet/dry
				return 102;
			}
			else {
				//Tropical wet
				return 101;
			}
		}
		//Sub-tropical
		if (tempZ==2) {
			if(elev>800){
				//Sub-tropical mountain
				return 205;
			}
			else if(dryM==12){
				//Sub-tropical desert
				return 204;
			}
			else if(dryM==0){
				//Sub-tropical humid (forest)
				return 201;
			}
			else if(semiarid(prec, ET)){
				//Sub-tropical semi-arid
				return 203;
			}
			else {
				//Sub-tropical steepe
				return 202;
			}
		}		
		//Temperate
		if (tempZ==3) {
			int count=0;
			for (int i=1; i<12; i++) {
				if(temp[i]<0)
					count++;
			}

			if(elev>800){
				//Temperate mountain
				return 305;
			}
			else if(dryM==12){
				//Temperate desert
				return 304;
			}
			else if(semiarid(prec, ET)){
				//Temperate semi-arid
				return 303;
			}
			else if(count==0){
				//Temperate oceanic
				return 301;
			}
			else {
				//Temperate continental forest
				return 302;
			}
		}	

		//Boreal
		if (tempZ==4) {
			if(elev>600){
				//Boreal mountain
				return 403;
			}

			if(lc==1||lc==3){
				//coniferous dense forest dominant (with IGBP Landcover)
				return 401;
			}
			else {
				//woodland and sparse forest dominant
				return 402;
			}	
		}
		//Polar
		if (tempZ==5) {
			return 501;

		}	
		//		System.out.println("\t Ecoregion: "+ecor);
		return 0;
	}


	/**
	 * Using IPCC table 4.4.
	 * TODO: review CDIAC's shrub handling method.
	 * TODO: improve crop handling.  As-is, they are all given default 0.3
	 * @param ecoregion
	 * @return ratio
	 */
	public static double ratioAboveBelow(int ecoregion, int landcover) {
		double def = 0.0;
		if(landcover==FOREST){
			switch(ecoregion){
			case	101	: return	0.27	;
			case	102	: return	0.22	;
			case	103	: return	0.56	;
			case	104	: return	0.4	;
			// Tropical desert 
			case	105	: return	def	;
			case	106	: return	0.27	;
			case	201	: return	0.22	;
			case	202	: return	0.42	;
			case	203	: return	0.32	;
			// Sub-tropical desert
			case	204	: return	def	;
			case	205	: return	0.27	;
			case	301	: return	0.3	;
			case	302	: return	0.29	;
			case	303	: return	0.32	;
			// Temperate desert 
			case	304	: return	def	;
			case	305	: return	0.27	;
			case	401	: return	0.32	;
			case	402	: return	0.32	;
			case	403	: return	0.32	;
			case	501	: return	0.0	;	
			default : 
				return 0.3;
			}
		}
		else if (landcover==GRASSLAND) {
			//Tropical - Moist & Wet 
			if(ecoregion<200){
				return 1.6;
			}
			//Boreal - Dry & Wet or Cold Temperate - Wet or	Warm Temperate - Wet
			else if(ecoregion==201 || ecoregion ==301|| ecoregion ==302|| ecoregion ==401 ){
				return 4.0;
			}

			//Cold Temperate - Dry or Warm Temperate - Dry  or Tropical - Dry
			else if(ecoregion<205 ||ecoregion<305||ecoregion==402 ){
				return 2.8;
			}
			// Place holders from table 6.1. Not explicitly handled by IPCC
			else if (landcover == SHRUBLAND) {
				return 2.8;
			}
			else if (landcover == SAVANNA) {
				return 0.5;
			}
			//Mtn - Take the average of the other categories
			else {
				return 3.4;
			}
		}
		//System.err.println("Ratio WARNING: Unknown ecoregion - landcover combination: "+ecoregion+" - "+landcover);
		return 0.3;					
	}

	/**
	 * Using IPCC table 4.3
	 * @param ecoregion
	 * @return ratio
	 */
	public static double carbonFracion(int ecoregion, int LC) {
		//Table 4.3: CARBON FRACTION OF ABOVEGROUND FOREST BIOMASS 
		if(ecoregion < 300)	{// meaning subtropical or tropical
			return 0.47;
		}
		else if((int)(ecoregion/100) == 4){
			return 0.48;
		}
		else{
			return 0.47;
		}	
	}

	/**
	 * Using IPCC methods in tables 4.7, 5.1, 6.4
	 * @param ecoregion,landcover
	 * @return Biomass carbon stock 
	 */
	public static double abovebiomassStock(int ecoregion, int landcover) {		
		if (landcover==FOREST){
			// Table TABLE 4.7 	ABOVE-GROUND BIOMASS IN FORESTS 
			double def = 0.0; 
			switch(ecoregion){
			case	101	: return	280.0	;
			case	102	: return	180.0	;
			case	103	: return	130.0	;
			case	104	: return	60.0	;
			case	105	: return	def	;
			case	106	: return	135.0	;
			case	201	: return	180.0	;
			case	202	: return	130.0	;
			case	203	: return	60.0	;
			case	204	: return	def	;
			case	205	: return	135.0	;
			case	301	: return	def	;
			case	302	: return	70.0	;
			case	303	: return	def	;
			case	304	: return	def	;
			case	305	: return	115.0	;
			case	401	: return	50.0	;
			case	402	: return	15.0	;
			case	403	: return	30.0	;
			case	501	: return	0.0	;
			default : 
				System.err.println("Above Forest WARNING: Unknown ecoregion: "+ecoregion);
				return 0.0;
			}
		}
		else if(landcover==CROPLAND){
			//TABLE 5.1 :DEFAULT COEFFICIENTS FOR ABOVE-GROUND WOODY BIOMASS AND HARVEST CYCLES IN CROPPING SYSTEMS CONTAINING PERENNIAL SPECIES 
			//Tropical wet
			if(ecoregion==101 ){
				return 50.0;
			}
			//Tropical moist
			if(ecoregion==102 ){
				return 21.0;
			}
			//Tropical dry
			else if((int)(ecoregion/100) == 1 ){
				return 9.0;
			}
			else {
				return 63.0;
			}	
		}
		/*
		 * TABLE 6.4 DEFAULT BIOMASS STOCKS PRESENT ON GRASSLAND, AFTER CONVERSION FROM OTHER LAND USE(tonnes d.m. ha-1)
		 * This code should never be used due to the biomassStock() implementation of the same table.
		 */
		else if(landcover==GRASSLAND){
			//Tropical moist
			if(ecoregion==101 ||ecoregion==102 ){
				return 6.2;
			}
			//Tropical dry
			else if((int)ecoregion/100 ==1 ){
				return 2.3;
			}
			//Warm Temperate - wet 
			else if(ecoregion ==201 ){
				return 2.7;
			}
			//Warm Temperate - Dry 
			else if((int)ecoregion/100 ==2 ){
				return 1.6;
			}
			//Cold Temperate -moist
			else if(ecoregion == 301||ecoregion== 302 ){
				return 2.4;
			}
			//Cold Temperate - Dry 
			else if(ecoregion <305 ){
				return 1.7;
			}
			//Mtn Temperate - Average
			else if(ecoregion == 305 ){
				return 2.05;
			}
			//Boreal - Dry & Wet
			else if((int)(ecoregion/100)== 4 ){
				return 1.7;
			}				
		}	
		System.err.println("WARNING: Zero biomass ecoregion - landcover combination: "+ecoregion+" - "+landcover);
		return 0.0;
	}

	/**
	 * Uses lookups for various IPCC tables.
	 * @param ecoregion
	 * @param LC
	 * @return
	 */
	public static double biomassStock(int ecoregion, int landcover) {
		// IPCC table 6.4 "Total above and below ground non-woody biomass (tonnes / ha)"
		if (landcover==GRASSLAND){ 
			//Tropical moist
			if(ecoregion==101 ||ecoregion==102 ){
				return 16.1;
			}
			//Tropical dry
			else if((int)ecoregion/100 ==1 ){
				return 8.7;
			}
			//Warm Temperate - wet 
			else if(ecoregion ==201 ){
				return 13.5;
			}
			//Warm Temperate - Dry 
			else if((int)ecoregion/100 ==2 ){
				return 6.1;
			}
			//Cold Temperate - moist
			else if(ecoregion == 301||ecoregion== 302 ){
				return 13.6;
			}
			//Cold Temperate - Dry 
			else if(ecoregion <305 ){
				return 6.5;
			}
			//Mtn Temperate - Average
			else if(ecoregion == 305 ){
				return 10.05;
			}
			//Boreal - Dry & Wet
			else if((int)(ecoregion/100)== 4 ){
				return 8.5;
			}	
			return 0.0;				
		}
		else if (landcover==NOCarbon){
			return 0.0;
		}
		else{
			double above = abovebiomassStock(ecoregion, landcover);
			//	System.out.println("\t\t Above ground biomass: "+above);
			double carbonFraction = carbonFracion(ecoregion, landcover);
			//	System.out.println("\t\t Carbon fraction: "+carbonFraction);
			double ratio = ratioAboveBelow(ecoregion, landcover);
			//	System.out.println("\t\t Ratio of below to above: "+carbonFraction);	
			return above*carbonFraction*(1.0+ratio);
		}
	}		

	
	/**
	 * TABLE  2.3  DEFAULT REFERENCE (UNDER NATIVE VEGETATION) SOIL ORGANIC C STOCKS (SOCREF) FOR MINERAL SOILS  (TONNES C HA-1 IN 0-30 CM DEPTH) 
	 * 
	 * Soils with high activity clay (HAC) vs Soils with low activity clay (LAC)?
	 * 
	 * @param ecoregion
	 * @param landcover
	 * @return Soil Reference carbon stock
	 */
	public static double soilReferenceStock(int ecoregion, int landcover) {
		if (landcover == IPCCcarbon.NOCarbon || landcover == IPCCcarbon.OTHER) {
			return 0;
		}
		//Tropical wet
		if(ecoregion==101 ){
			return 77.2;
		}
		//Tropical moist
		else if(ecoregion==102 ){
			return 61.4;
		}
		//Tropical dry
		else if(ecoregion<200 ){
			return 48;
		}
		//Tropical Mtn
		else if(ecoregion ==106 ){
			return 70.2;
		}
		//Warm Temperate - wet 
		else if(ecoregion ==201 ){
			return 70.6;
		}
		//Warm Temperate - Dry 
		else if(ecoregion < 300 ){
			return 47.8;
		}
		//Cold Temperate - moist
		else if(ecoregion == 301||ecoregion== 302 ){
			return 97;
		}
		//Cold Temperate - Dry 
		else if(ecoregion == 303||ecoregion== 304 ){
			return 44.8;
		}
		//Temperate Mtn
		else if(ecoregion == 305){
			return 71;
		}
		//Boreal  
		else {
			return 72.2;
		}
	}

	/**
	 *  TABLE 2.2 carbon stocks in dead organic matter
	 * @param ecoregion
	 * @param landcover
	 * @return
	 */
	public static double deadStock(int ecoregion, int landcover) {

		if (landcover == IPCCcarbon.NOCarbon || landcover == IPCCcarbon.OTHER) {
			return 0;
		}
		//Tropical
		if(ecoregion<=106 ){
			return 3.65;
		}

		//Warm Temperate - wet 
		else if(ecoregion ==201 ){
			return 17.5;
		}
		//Warm Temperate - Dry 
		else if(ecoregion < 300 ){
			return 24.25;
		}
		//Cold Temperate - moist
		else if(ecoregion == 301||ecoregion== 302 ){
			return 21;
		}
		//Cold Temperate - Dry 
		else if(ecoregion == 303||ecoregion== 304 ){
			return 27.5;
		}
		//Boreal - Dry 
		else if(ecoregion== 402 ){
			return 28;
		}
		//Boreal - moist 
		else if(ecoregion <500){
			return 47;
		}
		return 0;
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO: write test code here
		System.out.println("Tropical rainforest cropland litter = "+
				deadStock(101, IPCCcarbon.CROPLAND));
	}
	
}