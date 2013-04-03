/**
 * 
 */
package com.berkenviro.imageprocessing;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
/**
 * @author nclinton
 *
 */
public class AttributeClassifier extends Classifier {

	private Attribute att;
	/**
	 * 
	 */
	public AttributeClassifier(Attribute a) {
		att = a;
	}
	
	@Override
	public double classifyInstance(Instance instance) {
		return instance.value(att);
	}

	@Override
	public void buildClassifier(Instances data) throws Exception {
		if (data.attribute(att.name()) == null) {
			throw new Exception ("No attribute for prediction.");
		}
	}

}
