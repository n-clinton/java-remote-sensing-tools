/**
 * 
 */
package com.berkenviro.imageprocessing;

import java.util.Arrays;

import org.apache.commons.math.linear.Array2DRowRealMatrix;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * A class that implements weighted plurality voting in two ways.
 * The first way is that described by Xu et al. 1992, Lam and Suen 1995, and Smits 2002.
 * The second way is using the "recall combiner" described by Kuncheva 2012, 
 * where Pik is estimated by recall.
 * 
 * @author nclinton
 *
 */
public class WeightedMajorityClassifier extends Classifier {

	private String[] voters;
	private Evaluation[] evals;
	boolean belief;
	
	/**
	 * @param voters
	 * @param costs
	 */
	public WeightedMajorityClassifier(String[] voters, boolean belief) {
		this.voters = voters;
		this.belief = belief;
		evals = new Evaluation[voters.length];
	}
	
	
	@Override
	public double classifyInstance(Instance instance) {
		// the index of these arrays is equivalent to the class label index
		double[] weights = new double[instance.numClasses()];
		double[] beliefs = new double[instance.numClasses()];
		for (int a=0; a<voters.length; a++) {
			Attribute att = instance.dataset().attribute(voters[a]);
			//int label = Integer.parseInt(att.value((int)instance.value(att))); // class label
			int label = (int)instance.value(att); // actually index
			
			// Recall combiner, see Kuncheva (2012) equation 19.
			double recall = evals[a].recall(label); // recall
			// the following is part of the "class constant" term
			for (int l=0; l<weights.length; l++) {
				if (a==0) { // only want to initialize with priors once
					weights[l] = Math.log((evals[a].numFalseNegatives(l)+evals[a].numTruePositives(l)) 
							/ evals[a].numInstances());
				}
				weights[l]+=Math.log(1.0 - evals[a].recall(l));
			}
			// weighted vote
			weights[label]+=Math.log(recall/(1.0-recall));
			weights[label]+=Math.log(instance.numClasses()-1);
			
			// Belief
			Array2DRowRealMatrix cm = new Array2DRowRealMatrix(evals[a].confusionMatrix());
			double[] aLabel = cm.getColumn(label); // classifier a has predicted label
			try {
				weka.core.Utils.normalize(aLabel); // normalize by sum
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(att.name()+" for class "+att.value((int)label)+" "+Arrays.toString(aLabel));
			}
			if (a == 0) {
				beliefs = aLabel;
			} else {
				for (int l=0; l<beliefs.length; l++) {
					beliefs[l]*=aLabel[l];
				}
			}
		}
		
		// in general, this will not work unless all the voting attributes are indexed the same way!!!!!!
		if (belief) {
			return weka.core.Utils.maxIndex(beliefs);
		} else {
			return weka.core.Utils.maxIndex(weights);
		}
		
	}

	
	@Override
	public void buildClassifier(Instances data) throws Exception {
		// check the training to make sure the attributes are present
		for (int a=0; a<voters.length; a++) {
			if (data.attribute(voters[a]) == null) {
				throw new Exception ("No attribute: "+voters[a]);
			}
			AttributeClassifier classy = new AttributeClassifier(data.attribute(voters[a]));
			evals[a] = new Evaluation(data);
			evals[a].evaluateModel(classy, data);
		}
	}

}
