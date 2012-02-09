/**
 * 
 */
package cn.edu.tsinghua.gee;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author nclinton
 *
 */
public class Unzip {
	
	static final int BUFFER = 2048;
	
	/**
	 * 
	 * @param file
	 */
	public static void unzip(String fileName) {
		try {
			File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);
            CheckedInputStream checksum = new CheckedInputStream(fis, new Adler32());
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(checksum));
            ZipEntry entry;
            while((entry = zis.getNextEntry()) != null) {
               System.out.println("Extracting: " +entry);
               int count;
               byte[] data = new byte[BUFFER];
               // write the files to the disk
               FileOutputStream fos = new FileOutputStream(file.getParent()+file.separator+entry.getName());
               BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
               while ((count = zis.read(data, 0, BUFFER)) != -1) {
                  dest.write(data, 0, count);
               }
               dest.flush();
               dest.close();
            }
            zis.close();
            System.out.println("Checksum: "+checksum.getChecksum().getValue());
         } catch(Exception e) {
            e.printStackTrace();
         }
	}
	
	/**
	 * 
	 * @param argv
	 */
    public static void main (String argv[]) {
    	//String test = "/Users/nclinton/Documents/GEE/MODIS_EVI_test/MCD43A4_005_2009_09_14.zip";
    	//unzip(test);
    	// OK
   }


}
