/**
 * 
 */
package cn.edu.tsinghua.gee;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author nclinton
 *
 */
public class GLA14Reader {
	
	RandomAccessFile glasFile;
	int start;
	
	public GLA14Reader(String fileName) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line = null;
			String [] toks = null;
			// recl = Record length in bytes
			line = reader.readLine();
			toks = line.split("=");
			int recl = Integer.parseInt(toks[1].replace(";", "").trim());
			// numhead = Number of header records preceding product data records
			line = reader.readLine();
			toks = line.split("=");
			int numhead = Integer.parseInt(toks[1].replace(";", "").trim());
			start = numhead*recl;
			glasFile = new RandomAccessFile(fileName, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void write(String outName) {
		String line = null;
		
		try {
			
			// skip to the start of the data
			glasFile.seek(0);
			glasFile.skipBytes(start);
			// pointer now at the data start
			System.out.println(glasFile.readInt());
			System.out.println(glasFile.readInt());
			System.out.println(glasFile.readInt());
			glasFile.skipBytes(start); // i_Spare1
			for (int i=0; i<39; i++) {
				System.out.println(glasFile.readInt());
			}
			for (int i=0; i<40; i++) {
				System.out.println(glasFile.readInt());
			}
			for (int i=0; i<40; i++) {
				System.out.println(glasFile.readInt());
			}
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(outName));
			
			int n = 0; // header
			glasFile.skipBytes(n);
			
			//writer.write(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String testFile = "/Users/nclinton/Documents/urban_ag/lidar/GLA14/GLA14_05021720_r3240_428_L3B.P1655_01_00";
		GLA14Reader reader = new GLA14Reader(testFile);
		reader.write("/Users/nclinton/Documents/urban_ag/lidar/GLA14/GLA14_05021720_r3240_428_L3B.P1655_01_00_out.txt");
	}

}
