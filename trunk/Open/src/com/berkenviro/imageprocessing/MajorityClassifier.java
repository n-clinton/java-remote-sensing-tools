/**
 * 
 */
package com.berkenviro.imageprocessing;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
/**
 * @author nclinton
 *
 */
public class MajorityClassifier extends Classifier {

	private String[] voters;
	private double[] costs;
	
	/**
	 * 
	 * @param a is the attribute which predicts the label of the other attribute with the best prediction.
	 */
	public MajorityClassifier(String[] voters, double[] costs) {
		this.voters = voters;
		this.costs = costs;
	}
	
	@Override
	public double classifyInstance(Instance instance) {
		Map<Integer, Majority> labels = new TreeMap<Integer, Majority>();
		for (int a=0; a<voters.length; a++) {
			// a voter
			Attribute att = instance.dataset().attribute(voters[a]);
			// a vote
			// debug only: label, not index
			//int label = Integer.parseInt(att.value((int)instance.value(att)));
			// in general, this will not work unless all the voting attributes are indexed the same way!!!!!!
			int label = (int)instance.value(att);
			if (labels.containsKey(label)) {
				labels.get(label).update(costs[a]);
				//System.out.println("\t Updating: "+label);
		    } else {
		    	labels.put(label, new Majority(label, costs[a]));
		    	//System.out.println("\t Putting: "+label);
		    }
		}
		
		List<Majority> majorities = new LinkedList<Majority>();
		int max = 0;
		for (Majority majority : labels.values()) {
			//System.out.println(majority);
			if (majority.size > max) {
				while (majorities.size() > 0) {
					majorities.remove(0);
				}
				majorities.add(majority);
				max = majority.size;
			} 
			else if (majority.size == max) {
				majorities.add(majority);
			}
		}
		//System.out.println("\t There are "+majorities.size()+" majorities");
		if (majorities.size() == 1) {
			return majorities.get(0).label;
		}
		
		Majority min = majorities.remove(0);
		while (majorities.size() > 0) {
			Majority majority = majorities.remove(0);
			if (majority.minCost < min.minCost) {
				min = majority;
			}
		}
		return min.label;
	}

	
	@Override
	public void buildClassifier(Instances data) throws Exception {
		// just check the training to make sure the attributes are present
		for (int a=0; a<voters.length; a++) {
			if (data.attribute(voters[a]) == null) {
				throw new Exception ("No attribute: "+voters[a]);
			}
		}
		
	}
	
	/**
	 */
	class Majority {
		int label;
		int size;
		double minCost;
		
		public Majority(int label, double cost) {
			this.label = label;
			size = 1;
			minCost = cost;
		}
		
		public void update(double cost) {
			minCost = cost < minCost ? cost : minCost;
			size++;
		}
		
		@Override
		public String toString() {
			return "label:"+label+", size:"+size+", cost:"+minCost;
		}
	}

}
