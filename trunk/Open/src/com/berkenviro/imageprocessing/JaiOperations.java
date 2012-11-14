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

import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;
import javax.media.jai.RegistryMode;

/**
 * Utility class to display all the possible operations registered by JAI.
 * 
 * @author Nicholas Clinton
 */
public class JaiOperations {

	/**
	 * Print JAI Registry Modes.
	 */
	public static void printRegistryModes() {
		String[] modes = RegistryMode.getModeNames();
		for(String s: modes) {
			System.out.println("***"+s+"***");
		}
	}
	
	/**
	 * Print JAI operations.
	 */
	public static void printOperations() {
		JAI jai = JAI.getDefaultInstance();
		OperationRegistry or = jai.getOperationRegistry();
		String[] ops = or.getDescriptorNames(OperationDescriptor.class);
		Arrays.sort(ops);
		for(String s: ops) {
			System.out.println("***"+s+"***");
			try {
				printDescription(s);
			} catch (Exception e) {
				//e.printStackTrace();
				System.out.println("\t Can't enumerate parameters: "+s);
			} 
		}
	}
	
	/**
	 * Print the description of an operation.
	 * @param opName is the OperationDescriptor name.
	 */
	public static void printDescription(String opName) {
		JAI jai = JAI.getDefaultInstance();
		OperationRegistry or = jai.getOperationRegistry();
		RegistryElementDescriptor red = or.getDescriptor(OperationDescriptor.class, opName);
		String[] sources = ((OperationDescriptor)red).getSourceNames();
		System.out.println("Sources: ");
		for (String s : sources) {
			System.out.println("\t"+s);
		}
		System.out.println("Parameters: ");
		String[] modes = red.getSupportedModes();
		for (String m : modes) {
			System.out.println("\t -"+m);
			String[] params = red.getParameterListDescriptor(m).getParamNames();
			for(String s: params) {
				System.out.println("\t\t"+s);
			}
		}
	}
	
	
	/**
	 * Print JAI ImageIO supported formats.
	 */
	public static String[] printImageIOFormats() {
		String[] types = ImageIO.getWriterFormatNames();
		for (String s : types) {
			System.out.println(s);
		}
		return types;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		printOperations();

		//printRegistryModes();
	}

}
