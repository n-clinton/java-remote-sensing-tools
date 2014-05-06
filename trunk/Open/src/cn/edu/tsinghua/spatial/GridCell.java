/**
 * 
 */
package cn.edu.tsinghua.spatial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author nclinton, Venevsky
 *
 */
public class GridCell {

	/**
	 * 
	 * @author nclinton, Venevsky
	 *
	 */
	class PlantFunctionalType {
		float area;
		float percentBurned;
		float precentCrown; // updated annually for tree types
		
		boolean isTree;
		float litter_ag, litter_bg;
		float dwscal365;
	    float lm_ind; // leaf mass, daily
	    float sm_ind; // stem mass, daily
	    float hm_ind; // hardwood mass, daily
	    float rm_ind; // root mass, daily
	    float[] mnpp; // monthly NPP (gC/m2)  
	    float[] gpp; // monthly NPP (gC/m2)  
	    float app; // annual npp
	    int leafondays,leafoffdays;
	    boolean leafOn;
	    
	    float[] gresp; // monthly growth respiration 
	    float[] lresp; // monthly leaf respiration
	    float[] sresp; // monthly sapwood respiration
	    float[] rresp; // monthly root respiration
	    float[] aresp; // monthly autotrophic respiration
	}
	
	float area;
	float latitude;
	// state variables
	float k_fast_ave,k_slow_ave;
    float litter_decom_ave;
    List<PlantFunctionalType> pfts;
    float fpc_grid; // Foliar Projection Cover is the proportions of PFTs in the grid cell
    
    float w; // water status
    float[] mnpp; // monthly gridcell NPP (gC/m2), sum over functional types
    float anpp; 
    
    float snowpack; // dept of snow

    float[] mw1,mw2; // monthly values of w(2) i.e.fraction of avilable water
    float soilpar; // soil parameters
    float mcica; // monthly for each pft
    float[] mgpp;
    float[] gresp; // monthly growth respiration 
    float[] lresp; // monthly leaf respiration
    float[] sresp; // monthly sapwood respiration
    float[] rresp; // monthly root respiration
    float[] aresp; // monthly autotrophic respiration
    
    float annualRunoff1; // from the first layer
    float annualRunoff2; // from the second layer
    float[] mRunoff1; // monthly, from the first layer
    float[] mRunoff2; // monthly, from the second layer
	
    float percentAg;
    float percentWoodland;
    float percentBurned;
    
    float[] dtemp, dprec, drad, dtmax, dtmin, day1, pconv;
    
    /**
     * 
     */
    public GridCell(float area, float latitude) {
    	this.area = area;
    	pfts = new ArrayList<PlantFunctionalType>();
    	
    }
    
    /**
     * All inputs are length 365
     * @param dtemp is degrees C
     * @param dprec is in millimeters
     * @param drad is in W/sq.m.
     * @param dtmax 
     * @param dtmin
     * @param day1
     * @param pconv
     */
	public void intitializeClimate(
			float[] dtemp, float[] dprec, float[] drad, float[] dtmax, float[] dtmin, float[] day1, float[] pconv) {
		this.dtemp = dtemp;
		this.dprec = dprec;
		this.drad = drad;
		this.dtmax = dtmax;
		this.dtmin = dtmin;
		this.day1 = day1;
		this.pconv = pconv;
	}
	
	public double dayLength(int doy) {
		return 0;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// start
		GridCell test = new GridCell(10000.0f, 50.0f);
		// now we need to initialize climate
		float[] dtemp = new float[365];
		float[] dtmin = new float[365];
		float[] dtmax = new float[365];
		for(int t=0; t<365; t++) {
			dtemp[t] = (float) (20.0*Math.sin(2.0*Math.PI*t/365.0 - Math.PI/2.0));
			dtmin[t] = dtemp[t] - 5.0f;
			dtmax[t] = dtemp[t] + 5.0f;
		}
		System.out.println(Arrays.toString(dtmin));
		System.out.println(Arrays.toString(dtemp));
		System.out.println(Arrays.toString(dtmax));
		float[] dprec = new float[365];
		Arrays.fill(dprec, 1.0f);
		float[] drad = new float[365];
		Arrays.fill(drad, 150);
		float[] day1 = new float[365];
		
	}

}
