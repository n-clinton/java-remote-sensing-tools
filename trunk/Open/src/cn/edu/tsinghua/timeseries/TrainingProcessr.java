package cn.edu.tsinghua.timeseries;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.RBFNetwork;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.meta.Bagging;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.CSVLoader;

/**
 * 
 */

/**
 * @author Nicholas Clinton
 *
 */
public class TrainingProcessr {

	/*
	 * Helper method to read instances from a file.
	 */
	public static Instances loadInstances(String fileName) {
		System.out.println("Loading... "+fileName);
		Instances instances = null;
		try {
			File theFile = new File(fileName);
			FileReader fReader = new FileReader(theFile);
			BufferedReader bReader = new BufferedReader(fReader);
			// build an Instances from the (Reader) bReader
			instances = new Instances(bReader);
			// close the readers
			bReader.close();
			fReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return instances;
	}
	
	/*
	 * Helper method to write instances to a file.
	 */
	public static void saveInstances(Instances instances, String fileName) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			writer.write(instances.toString());
			writer.newLine();
			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Method that replaces -9999 with "missing" in the instances
	 */
	public static Instances cleanInstances(Instances instances) {
		// clone the input
		Instances out = new Instances(instances);
		// iterate over the instances
	    for (int i=0; i<out.numInstances(); i++) {
	    	// iterate over the attributes
	    	for (int n=0; n<out.numAttributes(); n++) {
	    		// check for the -9999 value
	    		if (out.instance(i).value(n) == -9999) {
	    			// set the value to missing
	    			out.instance(i).setMissing(n);
	    		}
	    	}
	    }
	    return out;
	}
	
	/*
	 * Convert from numeric to nominal
	 */
	public static Instances nominalize(Instances instances, Attribute classAttr) {
		// clone the input
		Instances out = new Instances(instances);
		FastVector responses = new FastVector(4);
		responses.insertElementAt("none", 0);
		responses.insertElementAt("low", 1);
		responses.insertElementAt("moderate", 2);
		responses.insertElementAt("high", 3);
		Attribute abundance = new Attribute("abundance", responses);
		out.insertAttributeAt(abundance, out.numAttributes());
		
		//System.out.println("classAttr index = "+classAttr.index());
		
		// iterate over the instances
		Instance inst;
	    for (int i=0; i<out.numInstances(); i++) {
	    	inst = out.instance(i);
	    	// evaluate relative to the 8-bit abundance
	    	if (inst.value(classAttr) < 2.55) {
	    		inst.setValue(out.numAttributes()-1, "none");
	    	}
	    	else if (inst.value(classAttr) >= 2.55 && inst.value(classAttr) < 12.75) {
	    		inst.setValue(out.numAttributes()-1, "low");
	    	}
	    	else if (inst.value(classAttr) >= 12.75 && inst.value(classAttr) < 51) {
	    		inst.setValue(out.numAttributes()-1, "moderate");
	    	}
	    	else if (inst.value(classAttr) >= 51 && inst.value(classAttr) <= 255) {
	    		inst.setValue(out.numAttributes()-1, "high");
	    	}
	    	else {
	    		System.err.println("Abundance must be on an 8-bit scale!  Bad data:"+inst.value(classAttr));
	    	}
	    }
		
		// get rid of the old class variable
		out.deleteAttributeAt(classAttr.index());
		
		return out;
		
	}
	
	
	/*
	 * Convert from numeric to binary (presence/absence)
	 */
	public static Instances binarize(Instances instances, Attribute classAttr) {
		// clone the input
		Instances out = new Instances(instances);
		FastVector responses = new FastVector(2);
		responses.insertElementAt("absent", 0);
		responses.insertElementAt("present", 1);
		Attribute abundance = new Attribute("abundance", responses);
		out.insertAttributeAt(abundance, out.numAttributes());
		
		//System.out.println("classAttr index = "+classAttr.index());
		
		// iterate over the instances
		Instance inst;
	    for (int i=0; i<out.numInstances(); i++) {
	    	inst = out.instance(i);
	    	// evaluate relative to the 8-bit abundance
	    	if (inst.value(classAttr) < 2.55) {
	    		inst.setValue(out.numAttributes()-1, "absent");
	    	}
	    	else if (inst.value(classAttr) >= 2.55) {
	    		inst.setValue(out.numAttributes()-1, "present");
	    	}
	    	else {
	    		System.err.println("Abundance must be on an 8-bit scale!  Bad data:"+inst.value(classAttr));
	    	}
	    }
		
		// get rid of the old class variable
		out.deleteAttributeAt(classAttr.index());
		
		return out;
		
	}
	
	
	/*
	 * Code repository for testing the SVM.  Re-tooled for Weka 3.5.6, 9/26/7
	 * Perform grid search on c and gamma.  RBF kernel only per Hsu et al.
	 */
	public static void testSMO(String shortRep, Instances instances) {
		
		System.out.println("Testing "+instances.toSummaryString());
		// set up the FileWriter.
		BufferedWriter shortWriter = null;
		try {
			System.out.println("Initializing file writer for "+shortRep);
			shortWriter = new BufferedWriter(new FileWriter(shortRep));
			// set up the header
			shortWriter.write("c"+"\t"+"gamma"+"\t"+"kappa"+"\t"+"pctCorr");
			shortWriter.newLine();
			shortWriter.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// make a default support vector machine
		SMO svm = new SMO();
		// constant parameters
		svm.setNumFolds(-1); // use training data
		svm.setRandomSeed(1);
		svm.setBuildLogisticModels(true);
		RBFKernel rbfKernel = new RBFKernel();
		
		Evaluation evaluation = null;
		// set the folds to be the number of instances: leave-one-out
		int folds = instances.numInstances();	
		try {
			
			String line = "";
			
			for (double i = -7; i <= 13; i++) {
			// local:
			//for (double c = 4.0; c <= 16.0; c++) {
				// exponential adjustment of C
				double c = Math.pow(2.0, i);
				
				svm.setC(c);
				
				// adjust the gamma parameter
				for (double j = -7; j <= 13; j++) {
				// local:
				//for (double gamma = 0.0105; gamma <= 0.06; gamma=gamma+0.0005) {
				
					double gamma = Math.pow(2, j);
					rbfKernel.setGamma(gamma);
					// the classifier should initialize this kernel????
					svm.setKernel(rbfKernel);
					
					// make a new evaluation and write the results
					evaluation = new Evaluation(instances);
					// always use a seed of 1
					evaluation.crossValidateModel(svm, instances, folds, new Random(1));
					// write the parameter values, skip the polynomial stuff
					line = c+"\t"+gamma+"\t";
					line += evaluation.kappa() + "\t" + evaluation.pctCorrect();
					shortWriter.write(line);
					shortWriter.newLine();
					shortWriter.flush();
					System.out.println(line);
				}
						
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				shortWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	/*
	 * Code repository for testing the Neural Network
	 */
	public static void testNN(String shortRep, Instances instances) {
		
		// set up the FileWriter.
		BufferedWriter shortWriter = null;
		try {
			shortWriter = new BufferedWriter(new FileWriter(shortRep));
			// set up the header
			shortWriter.write("iterations"+"\t"+"rate"+"\t"+"momentum"+"\t"+"kappa"+"\t"+"pctCorr");
			shortWriter.newLine();
			shortWriter.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// the classifier
		MultilayerPerceptron net = new MultilayerPerceptron();
		// constant settings
		net.setSeed(1);
		net.setAutoBuild(true);
		net.setDecay(true);
		net.setReset(false);
		net.setNormalizeAttributes(true);
		net.setNormalizeNumericClass(true);
		net.setValidationSetSize(0);
		net.setGUI(false);
		net.setHiddenLayers("t"); // nodes = number attributes+number of classes
		
		// parameters to adjust
		double rate;
		double momentum;
		
		Evaluation evaluation = null;
		// set the folds to be the number of instances: leave-one-out
		int folds = instances.numInstances();	
		try {
			
			// loop over many combinations of parameters
			String line = "";
			for (double i=2; i<=12; i++) {
			// local
			//for (int iterations = 34; iterations<128; iterations++) {
				// exponential adjustment of iterations
				int iterations = (int) Math.pow(2.0, i);
				
				net.setTrainingTime(iterations);
			
				// iterate over rate and momentum
				for (double r=0.05; r<1.0; r+=0.05) {
					net.setLearningRate(r);
					for (double m=0.05; m<1.0; m+=0.05) {
						net.setMomentum(m);
						// make a new evaluation and write the results
						evaluation = new Evaluation(instances);
						evaluation.crossValidateModel(net, instances, folds, new Random(1));

						// write the parameter values
						line = iterations+"\t"+r+"\t"+m+"\t";
						line += evaluation.kappa() + "\t" + evaluation.pctCorrect();
						shortWriter.write(line);
						shortWriter.newLine();
						shortWriter.flush();
						System.out.println(line);
					}
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				shortWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
		
	
	/*
	 * Code repository for testing the J48
	 */
	public static void testJ48(String shortRep, Instances instances) {
		
		// set up the FileWriter.
		BufferedWriter shortWriter = null;
		try {
			shortWriter = new BufferedWriter(new FileWriter(shortRep));
			// set up the header
			shortWriter.write("conf"+"\t"+"minNum"+"\t"+"pruning"+"\t"+"bagging"+"\t"+"boosting"+"\t"+"kappa"+"\t"+"pctCorr");
			shortWriter.newLine();
			shortWriter.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// the classifier
		J48 tree = new J48();
		// enhanced
		Classifier c = null;

		boolean[] trueFalse = {true, false};
		String bagString = "";
		String boostString = "";
		
		Evaluation evaluation = null;
		// set the folds to be the number of instances: leave-one-out
		int folds = instances.numInstances();	
		try {
			
			// loop over many combinations of parameters
			String line = "";
			for (double conf=0.05; conf<0.8; conf+=0.05) {
				tree.setConfidenceFactor((float)conf);
				for (int minNum=1; minNum<11; minNum++) {
					tree.setMinNumObj(minNum);
					for (boolean pruning : trueFalse) {
						tree.setUnpruned(!pruning);
						String pruneString = pruning ? "pruning" : "";
						for (boolean bagging : trueFalse) {
							bagString = bagging ? "bagging" : "";
							if (!bagging) {
								for (boolean boosting : trueFalse) {
									boostString = boosting ? "boosting" : "";
									if (boosting) { // boosting
										AdaBoostM1 boost = new AdaBoostM1();
										boost.setNumIterations(10);
										boost.setClassifier(tree);
										c = boost;
									}
									else { // no enhancement
										c = tree;
									}
									// make a new evaluation and write the results
									evaluation = new Evaluation(instances);
									evaluation.crossValidateModel(c, instances, folds, new Random(1));

									// write the parameter values
									line = conf+"\t"+minNum+"\t"+pruneString+"\t"+bagString+"\t"+boostString+"\t";
									line += evaluation.kappa() + "\t" + evaluation.pctCorrect();
									shortWriter.write(line);
									shortWriter.newLine();
									shortWriter.flush();
									System.out.println(line);
								}
							}
							else { // bagging
								Bagging bag = new Bagging();
								bag.setNumIterations(100);
								bag.setClassifier(tree);
								c = bag;
								// make a new evaluation and write the results
								evaluation = new Evaluation(instances);
								evaluation.crossValidateModel(c, instances, folds, new Random(1));

								// write the parameter values
								line = conf+"\t"+minNum+"\t"+pruneString+"\t"+bagString+"\t"+boostString+"\t";
								line += evaluation.kappa() + "\t" + evaluation.pctCorrect();
								shortWriter.write(line);
								shortWriter.newLine();
								shortWriter.flush();
								System.out.println(line);
							}
						}
						
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				shortWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	
	/*
	 * Test a Random Forest
	 */
	public static void testRandomForest (String shortRep, Instances instances) {
		// set up the FileWriter.
		BufferedWriter shortWriter = null;
		try {
			shortWriter = new BufferedWriter(new FileWriter(shortRep));
			// set up the header
			shortWriter.write("trees"+"\t"+"features"+"\t"+"kappa"+"\t"+"pctCorr");
			shortWriter.newLine();
			shortWriter.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// the classifier
		RandomForest forest = new RandomForest();
		
		Evaluation evaluation = null;
		// set the folds to be the number of instances: leave-one-out
		int folds = instances.numInstances();	
		try {
			
			// loop over many combinations of parameters
			String line = "";
			for (int trees=10; trees<=200; trees+=10) {
				forest.setNumTrees(trees);
				for (int features=2; features<=instances.numAttributes(); features++) {
					forest.setNumFeatures(features);
					// make a new evaluation and write the results
					evaluation = new Evaluation(instances);
					evaluation.crossValidateModel(forest, instances, folds, new Random(1));

					// write the parameter values
					line = trees+"\t"+features+"\t";
					line += evaluation.kappa() + "\t" + evaluation.pctCorrect();
					shortWriter.write(line);
					shortWriter.newLine();
					shortWriter.flush();
					System.out.println(line);
				}

			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				shortWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	
	/*
	 * Test LibSVM according to Hsu et al.
	 */
	public static void testLibSVM (String shortRep, Instances instances) {
		// set up the FileWriter.
		BufferedWriter shortWriter = null;
		try {
			shortWriter = new BufferedWriter(new FileWriter(shortRep));
			// set up the header
			shortWriter.write("c"+"\t"+"kernel"+"\t"+"prob"+"\t"+"gamma"+"\t"+"kappa"+"\t"+"pctCorr");
			shortWriter.newLine();
			shortWriter.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// the classifier
		LibSVM svm = new LibSVM();
		// set the constant parameters
		svm.setNormalize(true);
		svm.setShrinking(true);
		svm.setDegree(3);
		
		// set up the tags for the kernel type
		SelectedTag[] kernels = {new SelectedTag(LibSVM.KERNELTYPE_LINEAR, LibSVM.TAGS_KERNELTYPE),
								 new SelectedTag(LibSVM.KERNELTYPE_RBF, LibSVM.TAGS_KERNELTYPE),
								 new SelectedTag(LibSVM.KERNELTYPE_POLYNOMIAL, LibSVM.TAGS_KERNELTYPE)};
		SelectedTag[] rbfOnly = {new SelectedTag(LibSVM.KERNELTYPE_RBF, LibSVM.TAGS_KERNELTYPE)};
		
		boolean[] trueFalse = {true, false};
		boolean[] justTrue = {true};
		
		Evaluation evaluation = null;
		// set the folds to be the number of instances: leave-one-out
		int folds = instances.numInstances();	
		try {
			
			// Grid search a la Hsu et al.  
			String line = "";
			for (double j = -7; j <= 13; j++) {
			// local search:
			//for (double c = 4050; c <= 4150; c++) {
				// the cost parameter
				double c = Math.pow(2, j);
				svm.setCost(c);
				
				for (SelectedTag st: kernels) {
				// local:
				//for (SelectedTag st: rbfOnly) {
					// the kernel type
					svm.setKernelType(st);
					
					for (boolean probability : trueFalse) {
					// local:
					//for (boolean probability : justTrue) {
						// toggle the probability estimation
						svm.setProbabilityEstimates(probability);
						String probStr = probability ? "prob" : "";
						
						if (svm.getKernelType().equals(kernels[1])) {
							for (double k = -7; k <= 13; k++) {
							// local:
							//for (double gamma= 0.0005; gamma<2; gamma = gamma + 0.0005) {
								// the gamma for an RBF kernel
								double gamma = Math.pow(2, k);
								svm.setGamma(gamma);
								// make a new evaluation and write the results
								evaluation = new Evaluation(instances);
								// set the random seed to 1, ALWAYS!!
								evaluation.crossValidateModel(svm, instances, folds, new Random(1));
								// write the parameter values
								line = c+"\t"+st.getSelectedTag().getIDStr()+"\t"+probStr+"\t"+gamma+"\t";
								line += evaluation.kappa() + "\t" + evaluation.pctCorrect();
								shortWriter.write(line);
								shortWriter.newLine();
								shortWriter.flush();
								System.out.println(line);		
							} // end gamma
						}
						else {
							// make a new evaluation and write the results
							evaluation = new Evaluation(instances);
							// set the random seed to 1, ALWAYS!!
							evaluation.crossValidateModel(svm, instances, folds, new Random(1));
							// write the parameter values
							line = c+"\t"+st.getSelectedTag().getIDStr()+"\t"+probStr+"\t"+""+"\t";
							line += evaluation.kappa() + "\t" + evaluation.pctCorrect();
							shortWriter.write(line);
							shortWriter.newLine();
							shortWriter.flush();
							System.out.println(line);	
						}
						
					} // end probability estimation toggle
				} // end kernel type
			} // end cost parameter
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				shortWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	/*
	 * Code repository for testing an RBFNetwork.  
	 * Adjusts minStdDev and NumClusters.
	 */
	public static void testRBFNet(String shortRep, Instances instances) {
		
		System.out.println("Testing "+instances.toSummaryString());
		// set up the FileWriter.
		BufferedWriter shortWriter = null;
		try {
			System.out.println("Initializing file writer for "+shortRep);
			shortWriter = new BufferedWriter(new FileWriter(shortRep));
			// set up the header
			shortWriter.write("numClusters"+"\t"+"minStdDev"+"\t"+"kappa"+"\t"+"pctCorr");
			shortWriter.newLine();
			shortWriter.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// make a default RBFNetwork
		RBFNetwork rbfNet = new RBFNetwork();
		// constant parameters
		rbfNet.setClusteringSeed(1);
		rbfNet.setMaxIts(-1);
		
		Evaluation evaluation = null;
		// set the folds to be the number of instances: leave-one-out
		int folds = instances.numInstances();	
		try {
			
			String line = "";
			// adjust number of clusters
			for (int numClusters = 1; numClusters <= 10; numClusters++) {
				
				rbfNet.setNumClusters(numClusters);
				
				// adjust the minStdDev
				for (double minStdDev = 0.05; minStdDev <= 1.0; minStdDev=minStdDev+0.05) {

					rbfNet.setMinStdDev(minStdDev);
					
					// make a new evaluation and write the results
					evaluation = new Evaluation(instances);
					// always use a seed of 1
					evaluation.crossValidateModel(rbfNet, instances, folds, new Random(1));
					// write the parameter values, skip the polynomial stuff
					line = numClusters + "\t" + minStdDev + "\t";
					line += evaluation.kappa() + "\t" + evaluation.pctCorrect();
					shortWriter.write(line);
					shortWriter.newLine();
					shortWriter.flush();
					System.out.println(line);
				}
						
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				shortWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		/*
		Instances inInstances = null;
		Instances clnInstances = null;
		Instances nomInstances = null;
		Attribute percent = null;
		
		clnInstances = cleanInstances(inInstances);
		saveInstances(clnInstances, "F:\\NASA_Ames\\training\\combo_train_select_clean.arff");
		nomInstances = nominalize(inInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training\\combo_train1_nom_unclean.arff");
		// make a clean version
		nomInstances = nominalize(clnInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training\\combo_train12_nom.arff");

		inInstances = loadInstances("F:\\NASA_Ames\\training\\combo_train2_noIDxy.arff");
		clnInstances = cleanInstances(inInstances);
		saveInstances(clnInstances, "F:\\NASA_Ames\\training\\combo_train2_clean.arff");
		percent = clnInstances.attribute("percent");
		nomInstances = nominalize(clnInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training\\combo_train2_nom.arff");

		Instances inInstances1 = loadInstances("F:\\NASA_Ames\\training\\combo_train1_nom_sel.arff");
		Attribute abundance = inInstances1.attribute("abundance");
		inInstances1.setClass(abundance);
		String shortRep = "F:\\NASA_Ames\\training\\svm_train1_Rep072407.txt";
		//testSVM(shortRep, inInstances1);
		shortRep = "F:\\NASA_Ames\\training\\NN_train1_Rep072407.txt";
		testNN(shortRep, inInstances1);
		shortRep = "F:\\NASA_Ames\\training\\J48_train1_Rep072407.txt";
		testJ48(shortRep, inInstances1);
		
		Instances inInstances2 = loadInstances("F:\\NASA_Ames\\training\\combo_train2_nom_sel.arff");
		abundance = inInstances2.attribute("abundance");
		inInstances2.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training\\svm_train2_Rep072407.txt";
		//testSVM(shortRep, inInstances2);
		shortRep = "F:\\NASA_Ames\\training\\NN_train2_Rep072407.txt";
		testNN(shortRep, inInstances2);
		shortRep = "F:\\NASA_Ames\\training\\J48_train2_Rep072407.txt";
		testJ48(shortRep, inInstances2);
		
		Instances inInstances12 = loadInstances("F:\\NASA_Ames\\training\\combo_train12_nom_sel.arff");
		abundance = inInstances12.attribute("abundance");
		inInstances12.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training\\svm_train12_Rep072407.txt";
		//testSVM(shortRep, inInstances12);
		shortRep = "F:\\NASA_Ames\\training\\NN_train12_Rep072407.txt";
		testNN(shortRep, inInstances12);
		shortRep = "F:\\NASA_Ames\\training\\J48_train12_Rep072407.txt";
		testJ48(shortRep, inInstances12);
		

		//This CSVLoader sort of works.  Throws errors.  Not sure why.
		// Read the new .csv training data directly
		CSVLoader loader = new CSVLoader();
		Instances corrInstances = null;
		Instances linearInstances = null;
		Instances seasInstances = null;
		Instances statsInstances = null;
		Instances predictInstances = null;
		Instances train5bInstances = null;
		Instances subsetInstances = null;
		
		try {
			loader.setSource(new File("F:\\NASA_Ames\\training2\\training_corr.csv"));
			corrInstances = loader.getDataSet();
			loader.reset();
			
			loader.setSource(new File("F:\\NASA_Ames\\training2\\training_linear.csv"));
			linearInstances = loader.getDataSet();
			loader.reset();
			loader.setSource(new File("F:\\NASA_Ames\\training2\\training_seas.csv"));
			seasInstances = loader.getDataSet();
			loader.reset();
			loader.setSource(new File("F:\\NASA_Ames\\training2\\training_stats.csv"));
			statsInstances = loader.getDataSet();
			loader.reset();

			loader.setSource(new File("F:\\NASA_Ames\\training2\\training_predictors2.csv"));
			predictInstances = loader.getDataSet();
			loader.reset();

			loader.setSource(new File("F:\\NASA_Ames\\training2\\training5b_combined.csv"));
			train5bInstances = loader.getDataSet();
			loader.reset();
			loader.setSource(new File("F:\\NASA_Ames\\training2\\training5b_subset.csv"));
			subsetInstances = loader.getDataSet();
			
			loader.setSource(new File("F:\\NASA_Ames\\training2\\training5b_combined_with_id.csv"));
			train5bInstances = loader.getDataSet();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Instances inInstances123 = loadInstances("G:\\NASA_Ames\\training\\combo_train123_nom_sel.arff");
		inInstances123.setClass(inInstances123.attribute("abundance"));
		shortRep = "G:\\NASA_Ames\\training\\J48_train123_Rep073107_test.txt";
		//testJ48(shortRep, inInstances123);
		shortRep = "G:\\NASA_Ames\\training\\RF_train123_Rep073107_test.txt";
		testRandomForest(shortRep, inInstances123);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\train2_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\svm_train2_Rep081407.txt";
		//testSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\j48_train2_Rep081407.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\rf_train2_Rep081407.txt";
		testRandomForest(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\nn_train2_Rep081407.txt";
		testNN(shortRep, inInstances);

		inInstances = loadInstances("F:\\NASA_Ames\\training2\\train3_nominal_attr_subset.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\svm_train3_subset_Rep082207.txt";
		//testSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\j48_train3_subset_Rep082207.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\rf_train3_subset_Rep082207.txt";
		testRandomForest(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\nn_train3_subset_Rep082207.txt";
		testNN(shortRep, inInstances);

		Instances binInstances;
	
		percent = corrInstances.attribute("percent");
		nomInstances = nominalize(corrInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training2\\training_corr_nominal.arff");
		percent = linearInstances.attribute("percent");
		nomInstances = nominalize(linearInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training2\\training_linear_nominal.arff");
		percent = seasInstances.attribute("percent");
		nomInstances = nominalize(seasInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training2\\training_seas_nominal.arff");
		percent = statsInstances.attribute("percent");
		nomInstances = nominalize(statsInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training2\\training_stats_nominal.arff");

		percent = predictInstances.attribute("percent");
		nomInstances = nominalize(predictInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training2\\training_predictors2_nominal.arff");

		percent = train5bInstances.attribute("percent");
		binInstances = binarize(train5bInstances, percent);
		saveInstances(binInstances, "F:\\NASA_Ames\\training2\\training5b_combined_binary.arff");
		percent = subsetInstances.attribute("percent");
		binInstances = binarize(subsetInstances, percent);
		saveInstances(binInstances, "F:\\NASA_Ames\\training2\\training5b_subset_binary.arff");
		
		// 10/4/7
		percent = train5bInstances.attribute("percent");
		nomInstances = nominalize(train5bInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training2\\training5b_combined_with_id_nom.arff");
		binInstances = binarize(train5bInstances, percent);
		saveInstances(binInstances, "F:\\NASA_Ames\\training2\\training5b_combined_with_id_bin.arff");
		
		// finished 8/30, but should probably be replaced by the new training
		// see train5b.  9/20
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\train5_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\svm_train5_Rep082707.txt";
		//testSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\j48_train5_Rep082707.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\rf_train5_Rep082707.txt";
		testRandomForest(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\nn_train5_Rep082707.txt";
		testNN(shortRep, inInstances);

		// finished 8/30
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training_corr_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\svm_training_corr_Rep082707.txt";
		//testSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\j48_training_corr_Rep082707.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\rf_training_corr_Rep082707.txt";
		testRandomForest(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\nn_training_corr_Rep082707.txt";
		testNN(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training_linear_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\svm_training_linear_Rep082707.txt";
		//testSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\j48_training_linear_Rep082707.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\rf_training_linear_Rep082707.txt";
		testRandomForest(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\nn_training_linear_Rep082707.txt";
		testNN(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training_seas_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\svm_training_seas_Rep082707.txt";
		//testSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\j48_training_seas_Rep082707.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\rf_training_seas_Rep082707.txt";
		testRandomForest(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\nn_training_seas_Rep082707.txt";
		testNN(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training_stats_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\svm_training_stats_Rep082707.txt";
		//testSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\j48_training_stats_Rep082707.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\rf_training_stats_Rep082707.txt";
		testRandomForest(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\nn_training_stats_Rep082707.txt";
		testNN(shortRep, inInstances);

		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training_predictors2_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\svm_training_predict2_Rep090607.txt";
		//testSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\j48_training_predict2_Rep090607.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\rf_training_predict2_Rep090607.txt";
		testRandomForest(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\nn_training_predict2_Rep090607.txt";
		testNN(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training5b_combined_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\svm_train5b_Rep090607.txt";
		//testSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\j48_train5b_Rep090607.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\rf_train5b_Rep090607.txt";
		testRandomForest(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\nn_train5b_Rep090607.txt";
		testNN(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training5b_subset_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\svm_subset_Rep090607.txt";
		//testSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\j48_subset_Rep090607.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\rf_subset_Rep090607.txt";
		testRandomForest(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training2\\nn_subset_Rep090607.txt";
		testNN(shortRep, inInstances);

		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training5b_combined_nominal.arff");
		//System.out.println(inInstances.toString());
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\LibSVM_combo5b_local_092407.txt";
		testLibSVM(shortRep, inInstances);

		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training5b_subset_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\LibSVM_subset5b_Rep092007.txt";
		testLibSVM(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training_linear_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\LibSVM_linear_Rep092007.txt";
		testLibSVM(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training_seas_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\LibSVM_seas_Rep092007.txt";
		testLibSVM(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training_stats_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\LibSVM_stats_Rep092007.txt";
		testLibSVM(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training_predictors2_nominal.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\LibSVM_predict2_Rep092007.txt";
		testLibSVM(shortRep, inInstances);

		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training5b_combined_nominal.arff");
		//System.out.println(inInstances.toString());
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\SMO_combo5b_local_092607.txt";
		testSMO(shortRep, inInstances);

		inInstances = loadInstances("F:\\NASA_Ames\\training2\\training5b_combined_binary.arff");
		//System.out.println(inInstances.toString());
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training2\\RBF_combo5b_binary_092707.txt";
		testRBFNet(shortRep, inInstances);
		*/
		// end bad PRIZM processing
		
		//**********************************************************************
		
		// begin second round processing 11/25/07 with updated PRISM
		// 11/25 done
		
		// third round 1/17/08
		Attribute percent = null;
		Instances nomInstances = null;
		Instances binInstances = null;
		
		/*
		CSVLoader loader = new CSVLoader();
		Instances statsInstances = null;
		//Instances predictInstances = null;
		Instances comboInstances = null;

		try {
			
			//loader.setSource(new File("F:\\NASA_Ames\\training3\\train11_24_stats.csv"));
			loader.setSource(new File("F:\\NASA_Ames\\training4\\train1_17_08_stats.csv"));
			statsInstances = loader.getDataSet();
			loader.reset();

			//loader.setSource(new File("F:\\NASA_Ames\\training3\\train11_24_predicts.csv"));
			//predictInstances = loader.getDataSet();
			//loader.reset();
			
			//loader.setSource(new File("F:\\NASA_Ames\\training3\\train11_24_combo.csv"));
			loader.setSource(new File("F:\\NASA_Ames\\training4\\train1_17_08_combo.csv"));
			comboInstances = loader.getDataSet();
			loader.reset();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		percent = statsInstances.attribute("percent");
		nomInstances = nominalize(statsInstances, percent);
		//saveInstances(nomInstances, "F:\\NASA_Ames\\training3\\train11_24_stats_nom.arff");
		saveInstances(nomInstances, "F:\\NASA_Ames\\training4\\train1_17_08_stats_nom.arff");
		binInstances = binarize(statsInstances, percent);
		//saveInstances(binInstances, "F:\\NASA_Ames\\training3\\train11_24_stats_bin.arff");
		saveInstances(binInstances, "F:\\NASA_Ames\\training4\\train1_17_08_stats_bin.arff");
		
		// already done 11/24
		percent = predictInstances.attribute("percent");
		nomInstances = nominalize(predictInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training3\\train11_24_predicts_nom.arff");
		binInstances = binarize(predictInstances, percent);
		saveInstances(binInstances, "F:\\NASA_Ames\\training3\\train11_24_predicts_bin.arff");
		
		percent = comboInstances.attribute("percent");
		nomInstances = nominalize(comboInstances, percent);
		saveInstances(nomInstances, "F:\\NASA_Ames\\training4\\train1_17_08_combo_nom.arff");
		binInstances = binarize(comboInstances, percent);
		saveInstances(binInstances, "F:\\NASA_Ames\\training4\\train1_17_08_combo_bin.arff");
		*/
		
		
		
		Instances inInstances = null;
		Attribute abundance = null;
		String shortRep = null;
		
		// stats
		inInstances = loadInstances("F:\\NASA_Ames\\training4\\train1_17_08_stats_nom.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		/*
		shortRep = "F:\\NASA_Ames\\training4\\j48_stats_Rep11708.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training4\\rf_stats_Rep11708.txt";
		testRandomForest(shortRep, inInstances);
		*/
		shortRep = "F:\\NASA_Ames\\training4\\nn_stats_Rep11708.txt";
		//testNN(shortRep, inInstances);
		/*
		shortRep = "F:\\NASA_Ames\\training4\\LibSvm_stats_Rep11708.txt";
		testLibSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training4\\SMO_stats_Rep11708.txt";
		testSMO(shortRep, inInstances);
		*/
		
		
		
		// combo
		inInstances = loadInstances("F:\\NASA_Ames\\training4\\train1_17_08_combo_nom.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		/*
		shortRep = "F:\\NASA_Ames\\training4\\j48_combo_Rep11708.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training4\\rf_combo_Rep11708.txt";
		testRandomForest(shortRep, inInstances);
		*/
		shortRep = "F:\\NASA_Ames\\training4\\nn_combo_Rep11708b.txt";
		//testNN(shortRep, inInstances);
		/*
		shortRep = "F:\\NASA_Ames\\training4\\LibSvm_combo_Rep11708.txt";
		testLibSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training4\\SMO_combo_Rep11708.txt";
		testSMO(shortRep, inInstances);
		*/
		
		// do this last
		// predictors
		
		inInstances = loadInstances("F:\\NASA_Ames\\training4\\train11_24_predicts_nom.arff");
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		/*
		shortRep = "F:\\NASA_Ames\\training4\\j48_predicts_Rep11708.txt";
		testJ48(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training4\\rf_predicts_Rep11708.txt";
		testRandomForest(shortRep, inInstances);
		*/
		shortRep = "F:\\NASA_Ames\\training4\\nn_predicts_Rep11708.txt";
		//testNN(shortRep, inInstances);
		/*
		shortRep = "F:\\NASA_Ames\\training4\\LibSvm_predicts_Rep11708.txt";
		testLibSVM(shortRep, inInstances);
		shortRep = "F:\\NASA_Ames\\training4\\SMO_predicts_Rep11708.txt";
		testSMO(shortRep, inInstances);
		*/
		/*
		inInstances = loadInstances("F:\\NASA_Ames\\training4\\train1_17_08_combo_bin.arff");
		//System.out.println(inInstances.toString());
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training4\\RBF_combo_binary_031408.txt";
		testRBFNet(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training4\\train1_17_08_stats_bin.arff");
		//System.out.println(inInstances.toString());
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training4\\RBF_stats_binary_031408.txt";
		testRBFNet(shortRep, inInstances);
		*/
		
		inInstances = loadInstances("F:\\NASA_Ames\\training4\\train1_17_08_combo_bin.arff");
		//System.out.println(inInstances.toString());
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training4\\libsvm_combo_binary_031408.txt";
		testLibSVM(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training4\\train1_17_08_stats_bin.arff");
		//System.out.println(inInstances.toString());
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training4\\libsvm_stats_binary_031408.txt";
		testLibSVM(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training4\\train1_17_08_combo_bin.arff");
		//System.out.println(inInstances.toString());
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training4\\nn_combo_binary_031408.txt";
		testNN(shortRep, inInstances);
		
		inInstances = loadInstances("F:\\NASA_Ames\\training4\\train1_17_08_stats_bin.arff");
		//System.out.println(inInstances.toString());
		abundance = inInstances.attribute("abundance");
		inInstances.setClass(abundance);
		shortRep = "F:\\NASA_Ames\\training4\\nn_stats_binary_031408.txt";
		testNN(shortRep, inInstances);
		
		
		
	}

	
	
}
