/**
 * 
 */
package cn.edu.tsinghua.spatial;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import com.berkenviro.imageprocessing.Utils;

/**
 * @author nclinton
 *
 */
public class GetRegionProcessor {

	
	public static void processTable(String filename, String output, int numOut) {
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = new BufferedReader(new FileReader(new File(filename)));
			writer = new BufferedWriter(new FileWriter(new File(output)));
			String header = reader.readLine();
			String[] toks = Utils.tokenize(header);
			// first 3 columns are: id,longitude,latitude
			String newHeader = toks[1]+","+toks[2];
			// make labels for the next columns
			for (int s=0; s<numOut; s++) {
				for (int c=0; c<toks.length-3; c++) {
					newHeader+=","+toks[3+c]+s;
				}
			}
			writer.write(newHeader);
			writer.newLine();
			// data
			String line = reader.readLine();
			toks = Utils.tokenize(line);
			String[] newLine = new String[(2 + numOut*(toks.length-3))];
			// copy the coordinates
			newLine[0] = toks[1];
			newLine[1] = toks[2];
			int id = Integer.parseInt(toks[0]);
			newLine[2+id*(toks.length-3)] = toks[3];
			newLine[2+id*(toks.length-3)+1] = toks[4];
			while ((line = reader.readLine()) != null) {
				toks = Utils.tokenize(line);
				id = Integer.parseInt(toks[0]);
				// if it's a different coordinate
				if (Double.parseDouble(toks[1]) != (Double.parseDouble(newLine[0])) 
						|| Double.parseDouble(toks[2]) != (Double.parseDouble(newLine[1])) ) {
					// write out the old stuff...
					for (int i=0; i<newLine.length; i++) {
						writer.write(newLine[i]+",");
					}
					writer.newLine();
					// ...and start a new one
					newLine = new String[2 + numOut*(toks.length-3)];
					// copy the coordinates
					newLine[0] = toks[1];
					newLine[1] = toks[2];
				}
				newLine[2+id*(toks.length-3)] = toks[3];
				newLine[2+id*(toks.length-3)+1] = toks[4];
			}
			writer.flush();
			writer.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Vegas
		String vegas = "/Users/nclinton/Documents/urban/Las_Vegas_series_from_EE_trends.csv";
		processTable(vegas, 
				"/Users/nclinton/Documents/urban/Las_Vegas_series_from_EE_trends_records.csv", 5);
		

	}

}
