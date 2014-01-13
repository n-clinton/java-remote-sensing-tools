package cn.edu.tsinghua.spatial;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import com.berkenviro.imageprocessing.Utils;

/**
 * Custom, weird table joins originally created for the urban agriculture project.
 * 
 * @author nclinton
 *
 */
public class TextTableJoins {
	
	
	/**
	 * Don't use this anymore.
	 * @param strings1
	 * @param strings2
	 * @param j1a
	 * @param j1b
	 * @param j2a
	 * @param j2b
	 * @return
	 */
	public static String[][] nestedJoin(String[][] strings1, String[][] strings2, int j1a, int j1b, int j2a, int j2b) {
		// return strings2 joined to strings1
		String[][] joinedStrings = new String[strings1.length+strings2.length][strings1[0].length];
		// 
		for (int i=0; i<strings1[j1a].length; i++) {
			//System.out.println("Processing record "+i);
			// copy to output table whether a match is found or not
			for (int s1=0; s1<strings1.length; s1++) {
				joinedStrings[s1][i] = strings1[s1][i];
				System.out.print(joinedStrings[s1][i]+",");
			}
			for (int j=0; j<strings2[j2a].length; j++) {
				// if right join 'a' field, ...
				if (strings2[j2a][j].equals(strings1[j1a][i])) {
					//System.out.println("\t"+strings2[j2a][j]+" = "+strings1[j1a][i]);
					// look for a matching record
					int k=j;
					while (k<strings2[j2a].length && strings2[j2a][k].equals(strings1[j1a][i])) {
						//System.out.println("\t\t Checking record "+k+" : "+ strings1[j1b][i]);
						if(strings2[j2b][k].trim().toLowerCase().equals(strings1[j1b][i].trim().toLowerCase())) {
							//System.out.println("\t\t\t"+strings2[j2b][k]+" = "+strings1[j1b][i]);
							for (int s2=0; s2<strings2.length; s2++) {
								joinedStrings[strings1.length+s2][i] = strings2[s2][k];
								System.out.print(joinedStrings[strings1.length+s2][i]+",");
							}
							System.out.println();
							break;
						}
						k++;
					}
					break;
				}
			}
		}
		
		return joinedStrings;
	}
	
	/**
	 * Custom join.  Much faster and better than other nesetdJoin().  
	 * All int arguments are zero-indexed field indices.
	 * @param joinTo
	 * @param f1
	 * @param f2
	 * @param dictStrings
	 * @param f1j
	 * @param f2j
	 * @param fYear
	 * @param fPrice
	 * @return
	 */
	public static String[][] nestedJoin(String[][] joinTo, int f1, int f2, 
										String[][] dictStrings, int f1j, int f2j, int fYear, int fPrice) {
		// return the joinTo table plus country, crop, price, n
		String[][] joinedStrings = new String[joinTo.length+4][joinTo[0].length];
		
		// make country Dictionary
		HashMap<String, HashMap<String, double[]>> countryMap = new HashMap<String, HashMap<String, double[]>>();
		for (int j=0; j<dictStrings[0].length; j++) {
			String country = dictStrings[f1j][j];
			String crop = dictStrings[f2j][j];
			double price = Double.parseDouble(dictStrings[fPrice][j]);
			if (!countryMap.containsKey(country)) {
				countryMap.put(country, new HashMap<String, double[]>());
			}
			HashMap<String, double[]> crops = countryMap.get(country);
			if (!crops.containsKey(crop)) {
				// the crops double[] is {price, n}
				crops.put(crop, new double[] {price, 1});
			} else {
				double[] priceN = crops.get(crop);
				priceN[0]+=price; // sum price
				priceN[1]++; // increment n
			}
		}
		
		// write
		HashSet<String> notFound = new HashSet<String>();
		for (int i=0; i<joinTo[0].length; i++) {
			String country = joinTo[f1][i];
			String crop = joinTo[f2][i];
			//System.out.println("Processing record "+i);
			// copy to output table whether a match is found or not
			for (int s=0; s<joinTo.length; s++) {
				joinedStrings[s][i] = joinTo[s][i];
			}
			for (int s=joinTo.length; s<joinedStrings.length; s++) {
				// write the other columns
				if (countryMap.containsKey(country)) {
					if (countryMap.get(country).containsKey(crop)) {
						joinedStrings[joinTo.length][i] = country;
						joinedStrings[joinTo.length+1][i] = crop;
						double[] priceN = countryMap.get(country).get(crop);
						// return mean = sum(price)/n
						joinedStrings[joinTo.length+2][i] = String.valueOf(priceN[0]/priceN[1]);
						// and n
						joinedStrings[joinTo.length+3][i] = String.valueOf(priceN[1]);
					}
				}
				else {
					if (!notFound.contains(country)) {
						notFound.add(country);
					}
				}
			}
		}
		for (String c : notFound) {
			System.err.println("not found: \t"+c);
		}
		return joinedStrings;
	}
	
	
	public static String[][] nestedJoin(String[][] joinTo, int f1, int f2, 
			String[][] dictStrings, int f1j, int f2j, int fYear, int fPrice, String[][] backupDict, int f1b, int f2b) {
		// return the joinTo table plus country, crop, price, n
		String[][] joinedStrings = new String[joinTo.length+4][joinTo[0].length];

		// make country Dictionary
		HashMap<String, HashMap<String, double[]>> countryMap = new HashMap<String, HashMap<String, double[]>>();
		for (int j=0; j<dictStrings[0].length; j++) {
			String country = dictStrings[f1j][j];
			String crop = dictStrings[f2j][j];
			double price = Double.parseDouble(dictStrings[fPrice][j]);
			if (!countryMap.containsKey(country)) {
				countryMap.put(country, new HashMap<String, double[]>());
			}
			HashMap<String, double[]> crops = countryMap.get(country);
			if (!crops.containsKey(crop)) {
				// the crops double[] is {price, n}
				crops.put(crop, new double[] {price, 1});
			} else {
				double[] priceN = crops.get(crop);
				priceN[0]+=price; // sum price
				priceN[1]++; // increment n
			}
		}
		
		// make crop Dictionary
		HashMap<String, Double> cropMap = new HashMap<String, Double>();
		for (int b=0; b<backupDict[0].length; b++) {
			cropMap.put(backupDict[f1b][b], Double.valueOf(backupDict[f2b][b]));
		}

		// write
		HashSet<String> countriesNotFound = new HashSet<String>();
		HashSet<String> cropsNotFound = new HashSet<String>();
		for (int i=0; i<joinTo[0].length; i++) {
			String country = joinTo[f1][i];
			String crop = joinTo[f2][i];
			//System.out.println("Processing record "+i);
			// copy to output table whether a match is found or not
			for (int s=0; s<joinTo.length; s++) {
				joinedStrings[s][i] = joinTo[s][i];
			}
			for (int s=joinTo.length; s<joinedStrings.length; s++) {
				// write the other columns
				if (countryMap.containsKey(country)) {
					if (countryMap.get(country).containsKey(crop)) {
						joinedStrings[joinTo.length][i] = country;
						joinedStrings[joinTo.length+1][i] = crop;
						double[] priceN = countryMap.get(country).get(crop);
						// return mean = sum(price)/n
						joinedStrings[joinTo.length+2][i] = String.valueOf(priceN[0]/priceN[1]);
						// and n
						joinedStrings[joinTo.length+3][i] = String.valueOf(priceN[1]);
					} else {
						// lookup this crop in the global mean Dict instead
						if (cropMap.containsKey(crop)) {
							joinedStrings[joinTo.length][i] = "global_mean";
							joinedStrings[joinTo.length+1][i] = crop;
							double price = cropMap.get(crop);
							// return global mean
							joinedStrings[joinTo.length+2][i] = String.valueOf(price);
							// and zero for the number of country-crop samples
							joinedStrings[joinTo.length+3][i] = String.valueOf(0);
						} else {
							if (!cropsNotFound.contains(crop)) {
								cropsNotFound.add(crop);
							}
						}
					}
				} else {
					// couldn't find this country, so...
					if (!countriesNotFound.contains(country)) {
						countriesNotFound.add(country);
					}
					// lookup this crop in the global mean Dict instead
					if (cropMap.containsKey(crop)) {
						joinedStrings[joinTo.length][i] = "global_mean";
						joinedStrings[joinTo.length+1][i] = crop;
						double price = cropMap.get(crop);
						// return global mean
						joinedStrings[joinTo.length+2][i] = String.valueOf(price);
						// and zero for the number of country-crop samples
						joinedStrings[joinTo.length+3][i] = String.valueOf(0);
					} else {
						if (!cropsNotFound.contains(crop)) {
							cropsNotFound.add(crop);
						}
					}
				}
			}
		}
		for (String c : countriesNotFound) {
			System.err.println("Country not found: \t"+c);
		}
		for (String c : cropsNotFound) {
			System.err.println("Crop not found: \t"+c);
		}
		return joinedStrings;
	}


	/**
	 * Test code and processing log.
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		for (int m=1; m<=12; m++) {
			for (int d=1; d<=28; d++) {
				System.out.println(esDistance(2011, m, d));
			}
		}
		*/ //OK
		
		// yield 1
//		File file = new File("C:/Users/Nicholas/Documents/urban/agriculture/country_yield2009_pop2010_comp.csv");
//		String[][] strings1 = readTextFile(file, 1);
//		file = new File("C:/Users/Nicholas/Documents/urban/agriculture/FAO_producer_price.csv");
//		String[][] strings2 = readTextFile(file, 1);
//		
//		String[][] joinedStrings = nestedJoin(strings1, strings2, 2, 3, 1, 2);
//		writeFile(joinedStrings, "C:/Users/Nicholas/Documents/urban/agriculture/country_yield2009_pop2010_prices.csv");
		
		// yield2
//		File file = new File("C:/Users/Nicholas/Documents/urban/agriculture/country_yield2009_pop2010_comp2.csv");
//		String[][] strings1 = readTextFile(file, 1);
//		file = new File("C:/Users/Nicholas/Documents/urban/agriculture/FAO_producer_price.csv");
//		String[][] strings2 = readTextFile(file, 1);
//		
//		String[][] joinedStrings = nestedJoin(strings1, strings2, 2, 3, 1, 2);
//		writeFile(joinedStrings, "C:/Users/Nicholas/Documents/urban/agriculture/country_yield2009_pop2010_prices2.csv");
		
//		File file = new File("C:/Users/Nicholas/Documents/urban/agriculture/country_yield2009_pop2010_dollar_export.csv");
//		String[][] strings1 = readTextFile(file, 1);
//		file = new File("C:/Users/Nicholas/Documents/urban/agriculture/FAO_producer_price.csv");
//		String[][] strings2 = readTextFile(file, 1);
//		
//		String[][] joinedStrings = nestedJoin(strings1, strings2, 0, 1, 1, 2);
//		writeFile(joinedStrings, "C:/Users/Nicholas/Documents/urban/agriculture/country_yield2009_pop2010_dollar_export_prices.csv");
		
		// 20140107
		File file = new File("/Users/nclinton/Documents/urban/agriculture/version2/FAO_yield_2010_country_gpt2pop_roof_vert_vac_rank_production.csv");
		String[][] strings1 = Utils.readTextFile(file, 1);
		//file = new File("/Users/nclinton/Documents/urban/agriculture/version2/FAO_producer_prices_2010.csv");
		// this one is not as complete as the previous 2009 data:
		//file = new File("/Users/nclinton/Documents/urban/agriculture/version2/FAO_producer_prices_2009-2010.csv");
		// copy the 2011 downloaded 2009 data into the previous table, then synchronize names manually:
		file = new File("/Users/nclinton/Documents/urban/agriculture/version2/FAO_producer_prices_2009-2010_ammended.csv");
		String[][] strings2 = Utils.readTextFile(file, 1);
		
		//String[][] joinedStrings = nestedJoin(strings1, 0, 1, strings2, 0, 1, 3, 5);
//		writeFile(joinedStrings, 
//				"/Users/nclinton/Documents/urban/agriculture/version2/FAO_yield_2010_country_gpt2pop_roof_vert_vac_rank_production_prices.csv");
//		writeFile(joinedStrings, 
//				"/Users/nclinton/Documents/urban/agriculture/version2/FAO_yield_2010_country_gpt2pop_roof_vert_vac_rank_production_prices_mean.csv");
		
		// re-run previous to make correct version 20140109
		// now make a new and improved one, including global averages for missing data
		file = new File("/Users/nclinton/Documents/urban/agriculture/version2/prices/FAO_producer_prices_2009-2011_global_mean.csv");
		String[][] backupStrings = Utils.readTextFile(file, 1);
		String[][] joinedStrings = nestedJoin(strings1, 0, 1, strings2, 0, 1, 3, 5, backupStrings, 0, 1);
		Utils.writeFile(joinedStrings, 
				"/Users/nclinton/Documents/urban/agriculture/version2/FAO_yield_2010_country_gpt2pop_roof_vert_vac_rank_production_prices_mean_global.csv");
		
	}
}
