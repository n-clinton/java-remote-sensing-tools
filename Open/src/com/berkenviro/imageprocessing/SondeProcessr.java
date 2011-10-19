/*
 *  Copyright (C) 2011  Nicholas Clinton
 *	All rights reserved.  
 *
 *	Redistribution and use in source and binary forms, with or without modification, 
 *	are permitted provided that the following conditions are met:
 *
 *	1. Redistributions of source code must retain the above copyright notice, 
 *	this list of conditions and the following disclaimer.  
 *	2. Redistributions in binary form must reproduce the above copyright notice, 
 *	this list of conditions and the following disclaimer in the documentation 
 *	and/or other materials provided with the distribution. 
 *
 *	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 *	AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 *	THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 *	PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
 *	BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 *	DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 *	THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN 
 *	IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.berkenviro.imageprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.StringTokenizer;

/**
 * @author nick
 *
 */
public class SondeProcessr {

	/**
	 * Conversion from FSL format (http://www.esrl.noaa.gov/raobs/intl/fsl_format-new.cgi) 
	 * to Modtran Card 2C1 format.  Uses 1976 US standard for unknown constituents.  
	 * Currently dumps the result to the console (outFile is placeholder and will do nothing).
	 * @param inFile is the path name of the FSL formatted file
	 * @param outFile is a dummy.  No output is written.
	 */
	public static void txt2tp5(String inFile, String outFile) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inFile));
			//BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outFile)));
			String line = null;
			String newLine = null;
			StringTokenizer st = null;
			// read past the first "254"
			reader.readLine();
			int count = 0;
			while ((line = reader.readLine()) != null) {
				st = new StringTokenizer(line);
				String firstToken = st.nextToken();
				if (firstToken.equals("1") || // header
					firstToken.equals("2") || // header
					firstToken.equals("3") || // header
					firstToken.equals("6") || // no temperatures
					line.contains("99999")) { // missing data
					continue;
				}
				if (firstToken.equals("254")) { break; } // done
				count++;
				// otherwise, parse the data
				int marker = Integer.parseInt(firstToken);
				double mb = Double.parseDouble(st.nextToken());
				double alt = Double.parseDouble(st.nextToken());
				double temp = Double.parseDouble(st.nextToken());
				double dp = Double.parseDouble(st.nextToken());

				String newline = null;
				StringBuilder stringBuilder = new StringBuilder();
			    Formatter formatter = new Formatter(stringBuilder);
			    //System.out.print(formatter.format("%5.3f", alt/1000.0));
			    System.out.print(formatter.format("    %6.3f% 5.3E% 5.3E% 5.3E %5.3E %5.3EABG 6666666666 6", 
			    		alt/1000.0, mb/10.0, temp/10.0, dp/10.0, 0.0, 0.0));
			    System.out.println();
			}
			System.out.println("There are "+count+" lines.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Test code and processing log.
	 * @param args
	 */
	public static void main(String[] args) {

		//String sonde = "C:/Documents and Settings/nick/My Documents/atm0990200/OAK_sonde_10292008.txt";
		//String sonde = "C:/Documents and Settings/nick/My Documents/atm04-920/reno_sonde_20040409.txt";
		//String sonde = "E:/0901000/20090723_OAK_sondes.txt";
		//String sonde = "C:/Documents and Settings/nick/My Documents/atm11-801-02/VBG_sonde.txt";
		//String sonde = "C:/Users/owner/Documents/ASTL/Ivanpah_2011/Radiosonde_20110608_FSL.txt";
		//String sonde = "C:/Users/owner/Documents/ASTL/Ivanpah_2011/Radiosonde_20110609_FSL.txt";
		String sonde = "C:/Users/owner/Documents/ASTL/Ivanpah_2011/Radiosonde_20110608_FSL_long.txt";
		String out = "junk";
		txt2tp5(sonde, out);
		
		
	}

}
