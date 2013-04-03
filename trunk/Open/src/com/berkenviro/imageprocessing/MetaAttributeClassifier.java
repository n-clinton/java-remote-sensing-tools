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
public class MetaAttributeClassifier extends Classifier {

	private Attribute att;
	
	/**
	 * 
	 * @param a is the attribute which predicts the label of the other attribute with the best prediction.
	 */
	public MetaAttributeClassifier(Attribute a) {
		att = a;
	}
	
	@Override
	public double classifyInstance(Instance instance) {
		String attLabel = att.value((int)instance.value(att));
		Attribute predBest = instance.dataset().attribute(attLabel);
		return instance.value(predBest);
	}

	@Override
	public void buildClassifier(Instances data) throws Exception {
		if (data.attribute(att.name()) == null) {
			throw new Exception ("No attribute for prediction.");
		}
	}

}
