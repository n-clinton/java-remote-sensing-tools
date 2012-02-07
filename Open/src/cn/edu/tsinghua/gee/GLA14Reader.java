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
	
	public static byte badByte = 127;
	public static short badShort = 32767;
	public static int badInt = 2147483647;
	public static float badFloat = Float.MAX_VALUE;
	public static double badDouble = Double.MAX_VALUE;
	
	private RandomAccessFile glasFile;
	private String fileName;
	private int recl; // record length in bytes
	private int start; // length of header in bytes
	
	/**
	 * Make a reader, if possible.  If not, an exception will be thrown.
	 * @param fileName
	 * @throws Exception
	 */
	public GLA14Reader(String fileName) throws Exception {
		this.fileName = fileName;
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line = null;
		String [] toks = null;
		// recl = Record length in bytes
		line = reader.readLine();
		toks = line.split("=");
		recl = Integer.parseInt(toks[1].replace(";", "").trim());
		// numhead = Number of header records preceding product data records
		line = reader.readLine();
		toks = line.split("=");
		int numhead = Integer.parseInt(toks[1].replace(";", "").trim());
		start = numhead*recl;
		reader.close();
		
		glasFile = new RandomAccessFile(fileName, "r");
	}
	
	/**
	 * Write the .csv formatted output.
	 * @param outName
	 * @throws Exception
	 */
	public void write(String outName) throws Exception {
		
		// array of lines, each line representing one shot
		String[] lines = new String[40];
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outName));
		String header = "rec_ndx,lat,lon,elev,SigEndOff," +
				"SigBegHt,gpCntRngOff2,gpCntRngOff3,gpCntRngOff4,gpCntRngOff5,gpCntRngOff6," +
				"gAmp1,gAmp2,gAmp3,gAmp4,gAmp5,gAmp6," +
				"gArea1,gArea2,gArea3,gArea4,gArea5,gArea6";
		writer.write(header+"\n");
		
		glasFile.seek(0);
		glasFile.skipBytes(start); // skip the header
		
		boolean[] write = new boolean[40]; // if one of the quality flags is bad, reset this
		
		// while there are still records to read,
		while ( (glasFile.length() - glasFile.getFilePointer()) > recl) {
			
			System.out.println("\t" + fileName + ": "+Math.floor((glasFile.getFilePointer()-start)/recl));
			
			// initialize write array
			for (int b=0; b<lines.length; b++) {
				write[b] = true;
			}
			
			// initialize with index
			int i_rec_ndx = glasFile.readInt();
			for (int s=0; s<lines.length; s++) {
				lines[s] = i_rec_ndx+",";
			}
			
			// go to latitude
			glasFile.skipBytes(176-4);
			//System.out.println("should be 176: "+(glasFile.getFilePointer()-start)%recl);
			// i_lat micro-degrees
			for (int i=0; i<40; i++) {
				int i_lat = glasFile.readInt();
				if (i_lat == badInt) {
					write[i] = false;
				}
				lines[i] += (String.format("%.5f", i_lat*Math.pow(10, -6))+",");
			}
			// i_lon micro-degrees
			for (int i=0; i<40; i++) {
				int i_lon = glasFile.readInt();
				if (i_lon == badInt) {
					write[i] = false;
				}
				lines[i] += (String.format("%.5f", i_lon*Math.pow(10, -6))+",");
			}
			// i_elev mm, uncorrected
			for (int i=0; i<40; i++) {
				int i_elev = glasFile.readInt();
				if (i_elev == badInt) {
					write[i] = false;
				}
				lines[i] += (String.format("%.1f", i_elev*Math.pow(10, -3))+",");
			}
			
			// QA/QC Attitude Quality Indicator
			glasFile.skipBytes(2576-656);
			//System.out.println("Should be 2576: "+(glasFile.getFilePointer()-start)%recl);
			for (int i=0; i<40; i++) {
				int i_sigmaatt = glasFile.readUnsignedShort();
				if (i_sigmaatt != 0) {
					//System.err.println("\t Bad Attitude, i= "+i+": "+i_sigmaatt);
					write[i] = false; 
				}
			}
			
			// skip to Signal Begin Range Increment
			glasFile.skipBytes(3112-2656);
			//System.out.println("Should be 3112: "+(glasFile.getFilePointer()-start)%recl);
			// i_SigBegOff mm
			int[] i_SigBegOff = new int[40];
			for (int i=0; i<40; i++) {
				i_SigBegOff[i] = glasFile.readInt();
				if (i_SigBegOff[i]==badInt || i_SigBegOff[i]>0) {
					write[i] = false;
				}
				//lines[i] += (i_SigBegOff[i]+",");
			}
			
			// skip to i_SigEndOff
			glasFile.skipBytes(3432-3272);
			//System.out.println("Should be 3432: "+(glasFile.getFilePointer()-start)%recl);
			// i_SigEndOff mm
			for (int i=0; i<40; i++) {
				int i_SigEndOff = glasFile.readInt();
				if (i_SigEndOff==badInt || i_SigEndOff>0) {
					write[i] = false;
				}
				// compute height, convert to meters, write
				lines[i] += ( String.format("%.2f",(i_SigEndOff - i_SigBegOff[i])*0.001) + ",");
				//lines[i] += i_SigEndOff+",";
			}
			
			// at the Gaussian peaks
			//System.out.println("Should be 3592: "+(glasFile.getFilePointer()-start)%recl);
			// i_gpCntRngOff mm, Centroid range increment for all six peaks
			for (int i=0; i<40; i++) {
				double height; // computed relative to first Gaussian return
				// g==0, first Gaussian, assumed ground
				int i_gpCntRngOff_0 = glasFile.readInt();
				// if there is no first Gaussian, don't write
				if (i_gpCntRngOff_0==badInt || i_gpCntRngOff_0>0) {
					write[i] = false;
				}
				else { // compute height from first Gaussian to the beginning of the waveform
					height = ((i_gpCntRngOff_0 - i_SigBegOff[i])*0.001);
					lines[i] += (height < 0 ? 0 : String.format("%.2f", height)) +",";
				}
				// other, possible Gaussians
				for (int g=1; g<6; g++) {
					int i_gpCntRngOff = glasFile.readInt();
					// if bad, write a zero
					if (i_gpCntRngOff==badInt || i_gpCntRngOff>0) {
						lines[i] += 0 + ",";
					}
					else { // compute the height from the first Gaussian to this one
						height = ((i_gpCntRngOff_0 - i_gpCntRngOff)*0.001);
						lines[i] += (height < 0 ? 0 : String.format("%.2f", height)) +",";
					}
				}
			}
			
			// amplitude of Gaussian peaks
			glasFile.skipBytes(5236-4552);
			//System.out.println("Should be 5236: "+(glasFile.getFilePointer()-start)%recl);
			// i_Gamp 0.01 volts
			for (int i=0; i<40; i++) {
				for (int g=0; g<6; g++) {
					int i_Gamp = glasFile.readInt();
					if (i_Gamp==badInt) {
						if (g==0) {
							write[i] = false;
						}
						i_Gamp=0;
					}
					lines[i] += i_Gamp+",";
				}
			}
			
			// area under Gaussian peaks
			// i_Garea 0.01 volts * ns
			for (int i=0; i<40; i++) {
				for (int g=0; g<6; g++) {
					int i_Garea = glasFile.readInt();
					if (i_Garea==badInt) {
						if (g==0) {
							write[i] = false;
						}
						i_Garea=0;
					}
					lines[i] += i_Garea+",";
				}
			} // at 7156
			
			// skip the SD of Gaussians
			glasFile.skipBytes(960);
			// number of peaks
			//System.out.println("Should be 8116: "+(glasFile.getFilePointer()-start)%recl);
			for (int i=0; i<40; i++) {
				int i_nPeaks1 = glasFile.readUnsignedByte();
				if (i_nPeaks1 < 1) {
					//System.err.println("\t No peaks, i= "+i+": "+i_nPeaks1);
					write[i] = false; 
				}
			}
			
			// QA/QC SD of the land fit
			//System.out.println("Should be 8156: "+(glasFile.getFilePointer()-start)%recl);
			for (int i=0; i<40; i++) {
				int i_LandVar = glasFile.readUnsignedShort();
				if (i_LandVar > 20000) { // anything close to the badShort is bad
					//System.out.println("\t SD of land Gaussian fit too high, i= "+i+": "+i_LandVar);
					write[i] = false; 
				}
			}
			
			// QA/QC Saturation
			glasFile.skipBytes(8708-8236);
			//System.out.println("Should be 8708: "+(glasFile.getFilePointer()-start)%recl);
			for (int i=0; i<40; i++) {
				int i_satCorrFlg = glasFile.readUnsignedByte();
				// get low order nibble, 4 bits.
				int loNibble = i_satCorrFlg & 0xf;
				if (loNibble < 2) { } // ok
				else if (loNibble == 2) { // correction needed
					// do something??
					//System.err.println("Saturation correction needed, i= "+i+": "+loNibble);
				}
				else { // saturated
					write[i] = false; //don't use
					//System.err.println("\t Saturated, i= "+i+": "+loNibble);
				}
			}
			
			// QA/QC gain
			glasFile.skipBytes(8908-8748);
			//System.out.println("Should be 8908: "+(glasFile.getFilePointer()-start)%recl);
			for (int i=0; i<40; i++) {
				int i_gval_rcv = glasFile.readUnsignedShort();
				if (i_gval_rcv > 200) {
					write[i] = false; // don't use
					//System.err.println("\t Gain is too high, i= "+i+": "+i_gval_rcv);
				}
			}
			
			// QA/QC clouds
			glasFile.skipBytes(9148-8988);
			//System.out.println("Should be 9148: "+(glasFile.getFilePointer()-start)%recl);
			for (int i=0; i<40; i++) {
				int i_FRir_qaFlag = glasFile.readUnsignedByte();
				if (i_FRir_qaFlag <= 12) {
					write[i] = false; 
					//System.err.println("\t Too cloudy, i= "+i+": "+i_FRir_qaFlag);
				}
			}
			
			// QA/QC atm corr
			// wrong byte offsets???
			
			// QA/QC snr
			int[] i_maxRecAmp = new int[40];
			glasFile.skipBytes(9434-9188);
			//System.out.println("Should be 9434: "+(glasFile.getFilePointer()-start)%recl);
			for (int i=0; i<40; i++) {
				i_maxRecAmp[i] = glasFile.readUnsignedShort(); // fill with signal
			}
			for (int i=0; i<40; i++) {
				double snr = i_maxRecAmp[i]/glasFile.readUnsignedShort(); // divide by noise
				//System.out.println ("SNR, i= "+i+": "+snr);
				if (snr < 15) { // sort of arbitrary snr threshold
					write[i] = false; // don't use
					//System.err.println ("\t SNR too low, i= "+i+": "+snr);
				}
			}
			
			// skip to end of record
			glasFile.skipBytes((int)(recl-(glasFile.getFilePointer()-start)%recl));
			
			// write
			for (int l=0; l<40; l++) {
				if (write[l]) {
					//System.out.println(lines[l]);
					writer.write(lines[l]+"\n");
				}
			}
		} // end while
		writer.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		GLA14Reader reader;
		try {
			String testFile = "/Users/nclinton/Documents/urban_ag/lidar/GLA14/GLA14_05021720_r3240_428_L3B.P1655_01_00";
			//String testFile = "/Users/nclinton/Documents/urban_ag/lidar/2005.06.10/GLA14_531_2111_003_0225_0_01_0001.DAT";
			reader = new GLA14Reader(testFile);
			reader.write("/Users/nclinton/Documents/urban_ag/lidar/GLA14/GLA14_05021720_r3240_428_L3B.P1655_01_00_out.txt");
			//reader.write("/Users/nclinton/Documents/urban_ag/lidar/2005.06.10/GLA14_531_2111_003_0225_0_01_0001_out.txt");
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Looks OK.  20120207.
	}

}
