package cn.edu.tsinghua.timeseries;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import cn.edu.tsinghua.timeseries.TrainingProcessr;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddID;
import weka.filters.unsupervised.attribute.Remove;



public class PredictionTestr {
	
	/*
	 * Test the filtering method
	 */
	public static Instances addLink (Instances instances) {
			
		AddID filter = new AddID();
		Instances outInstances = null;
		try {
			// initializing the filter once with training set
			filter.setInputFormat(instances);
			// create new training set
			outInstances = Filter.useFilter(instances, filter);  
		} catch (Exception e) {
			e.printStackTrace();
		}  
		
		return outInstances;
		
	}

	
	public static void writePredictions (Instances instances, String predictTable, Classifier c) {

		BufferedWriter predictWriter = null;
		
		// filter the classifier in order to remove the ID field
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(c);
		Remove filter = new Remove();
		int[] idIndex = {instances.attribute("ID").index()};
		filter.setAttributeIndicesArray(idIndex);
		fc.setFilter(filter);
		
		// make a new evaluation and write the results
		Evaluation evaluation = null;
		int folds = instances.numInstances();
		try {
			evaluation = new Evaluation(instances);
			evaluation.crossValidateModel(fc, instances, folds, new Random(1));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Percent Correct = " +evaluation.pctCorrect());
		System.out.println("Kappa = " +evaluation.kappa());
		
		FastVector predictions = evaluation.predictions();
		
		System.out.println("Writing predictions...");
		
		try {	
			predictWriter = new BufferedWriter(new FileWriter(new File(predictTable)));
			// Data Labels for header row NOTE COMMA DELIMITED
			// TODO: check the following
			String dataLabels = "instNum, actual, predicted, prob0, prob1, prob2, prob3";
			predictWriter.write(dataLabels);

			// insert prediction column headers here
			
			predictWriter.newLine();
			predictWriter.flush();	

			for (int i = 0; i<predictions.size(); i++) {
				NominalPrediction np = (NominalPrediction) predictions.elementAt(i);
				double[] dist = np.distribution();
				predictWriter.write(i+","+np.actual()+","+np.predicted()+","+
									dist[0]+","+dist[1]+","+dist[2]+","+dist[3]);
				predictWriter.newLine();
				predictWriter.flush();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				predictWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		CSVLoader loader = new CSVLoader();
		Instances numericInstances = null;
		Instances idInstances = null;
		try {
			// load the numeric instances, the id is the non-unique point id
			loader.setSource(new File("F:\\NASA_Ames\\training2\\training5b_combined_with_id.csv"));
			numericInstances = loader.getDataSet();
			// this file will have all the original information, plus an ID field
			idInstances = addLink(numericInstances);
			
			String idFile = "F:\\NASA_Ames\\training2\\link_train5b_combo.arff";
			TrainingProcessr.saveInstances(idInstances, idFile);
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		// nominalize
		Attribute percent = idInstances.attribute("percent");
		Instances nomInstances = TrainingProcessr.nominalize(idInstances, percent);
		
		// write, just in case
		TrainingProcessr.saveInstances(nomInstances, "F:\\NASA_Ames\\training2\\link_train5b_combo_nom.arff");
		
		Attribute abundance = nomInstances.attribute("abundance");
		nomInstances.setClass(abundance); 
		
		// prepare the instances for prediction
		Attribute id = nomInstances.attribute("id");
		nomInstances.deleteAttributeAt(id.index());
		Attribute x = nomInstances.attribute("x");
		nomInstances.deleteAttributeAt(x.index());
		Attribute y = nomInstances.attribute("y");
		nomInstances.deleteAttributeAt(y.index());	

		// write, just in case
		TrainingProcessr.saveInstances(nomInstances, "F:\\NASA_Ames\\training2\\train5b_combo_nom_id.arff");
		
		// parameterize the classifier
		SMO svm = new SMO();
		// constant parameters
		svm.setNumFolds(-1); // use training data
		svm.setRandomSeed(1);
		svm.setBuildLogisticModels(true);
		RBFKernel rbfKernel = new RBFKernel();
		// use the optimal parameters
		svm.setC(5.0);
		rbfKernel.setGamma(0.675);
		svm.setKernel(rbfKernel);
		
		// this will be the prediction table
		String predictTable = "F:\\NASA_Ames\\training2\\predictions_train5b_combo.txt";
		writePredictions(nomInstances, predictTable, svm);
		
		
		
		
		
		
		
		
	}

}
