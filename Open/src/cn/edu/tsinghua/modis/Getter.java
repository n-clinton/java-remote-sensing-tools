package cn.edu.tsinghua.modis;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPCommunicationListener;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * 
 */

/**
 * @author Nicholas Clinton, nclinton@tsinghua.edu.cn
 * This class works with a Java 6 compliance level!!  Java 7 breaks it.
 *
 */
public class Getter {

	static {
		System.setProperty("ftp4j.passiveDataTransfer.useSuggestedAddress", "1");
	}
	
	FTPClient client;
	int year;
	String productFolder;
	
	/**
	 * 
	 * @param baseFolder
	 */
	public Getter(String baseFolder, int yr) throws Exception {
		year = yr;
		productFolder = baseFolder;
		// setup the FTP client
		client = new FTPClient();
		client.addCommunicationListener(new Listener());
		connect();
		// ready
	}
	
	
	/**
	 * UMT connect.
	 * @param baseFolder
	 */
	public Getter(String baseFolder) throws Exception {
		productFolder = baseFolder;
		// setup the FTP client
		client = new FTPClient();
		client.addCommunicationListener(new Listener());
		connectUMT();
		// ready
	}
	
	/**
	 * USGS MODIS archive.
	 * Keep trying until able to connect.
	 * @throws Exception
	 */
	private void connect() throws Exception {
		int attempt = 0;
		while (!client.isConnected()) { // if it's disconnected
			attempt++;
			System.out.println("\t Trying to connect.  Attempt "+attempt+"...");
			try {
				client.connect("e4ftl01.cr.usgs.gov");
				client.login("anonymous", "user@example.com");
				client.setType(FTPClient.TYPE_BINARY);
				client.changeDirectory(productFolder);
				client.getConnector().setReadTimeout(3600);
			} catch (Exception e) { 
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * University of Montana MOD16 archive
	 * Keep trying until able to connect.
	 * @throws Exception
	 */
	private void connectUMT() throws Exception {
		int attempt = 0;
		while (!client.isConnected()) { // if it's disconnected
			attempt++;
			System.out.println("\t Trying to connect.  Attempt "+attempt+"...");
			try {
				client.connect("ftp.ntsg.umt.edu");
				client.login("anonymous", "user@example.com");
				client.setType(FTPClient.TYPE_BINARY);
				client.changeDirectory(productFolder);
				client.getConnector().setReadTimeout(3600);
			} catch (Exception e) { 
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Call this last.
	 */
	public void disconnect() {
		
		try {
			client.getConnector().abortConnectForCommunicationChannel();
			client.disconnect(true);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FTPIllegalReplyException e) {
			e.printStackTrace();
		} catch (FTPException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Perform the download from the USGS archive for the specified year and tiles.
	 * @throws Exception
	 */
	private void download(ArrayList<String> tiles, File localDir) throws Exception {
		// get the sub-directories in the product directory
		FTPFile[] list = client.list();
		for (FTPFile f : list) {
			// if it's in the right year
			if (f.getName().startsWith(String.valueOf(year))) {
				// go into the subdirectory
				client.changeDirectory(f.getName());
				System.out.println("\t Checking: "+f.getName());
				// reproduce this directory structure locally
				File dir = new File(localDir.getAbsoluteFile()+"/"+f.getName());
				if (!dir.exists()) { // if the directory doesn't already exist
					if (!dir.mkdir()) { // make it, or...
						throw new Exception("Can't make: "+dir.getAbsolutePath()); 
					}
				}
				// get only the tiles needed
				System.out.println("\t\t Building tile list...");
				list = client.list();
				ArrayList<FTPFile> downloads = getFileList(list, tiles);
				// check all the files
				for (FTPFile file : downloads) {
					File local = null;
					try { 
						// the local file
						local = new File(dir.getAbsolutePath()+"/"+file.getName());
						if (!local.exists()) {  // if the local file doesn't exist, get it
							System.out.println(Calendar.getInstance().getTime());
							client.download(file.getName(), local);
						}
						else { // otherwise, just print a message
							System.out.println("\t\t\t Skipping: "+local.getAbsoluteFile());
						}
					} catch (Exception e1) {
						e1.printStackTrace();
						// wait a few seconds
						Thread.sleep(10000); 
						if (!client.isConnected()) { // if disconnected in the middle
							connect();
						}
						else { // try reconnecting
							client.disconnect(false);
							connect();
						}
						// go back to the right directory
						client.changeDirectory(f.getName());
						// if the local file matches the remote file, just keep going
						if (local.exists()) {
							if (local.length() != file.getSize()) {
								// try again
								client.download(file.getName(), new File(dir.getAbsolutePath()+"/"+file.getName()));
							}
						}
					}
				}
				// go back to the parent directory
				client.changeDirectoryUp();
			}
		}	
	}
	
	
	/**
	 * Perform the download from the USGS archive for the specified year and tiles.
	 * @throws Exception
	 */
	private void downloadMOD16(ArrayList<String> tiles, File localDir) throws Exception {
		// get the sub-directories in the product directory
		FTPFile[] list = client.list();
		for (FTPFile f : list) {
			// if it's in the right year (form of Y****)
			if (f.getType() == FTPFile.TYPE_DIRECTORY) {
				// go into the month subdirectory
				client.changeDirectory(f.getName());
				System.out.println("\t Checking: "+f.getName());
				// reproduce this directory structure locally
				File dir = new File(localDir.getAbsoluteFile()+"/"+f.getName());
				if (!dir.exists()) { // if the directory doesn't already exist
					if (!dir.mkdir()) { // make it, or...
						throw new Exception("Can't make: "+dir.getAbsolutePath()); 
					}
				}
				// get only the tiles needed
				System.out.println("\t\t Building tile list...");
				list = client.list();
				ArrayList<FTPFile> downloads = getFileList(list, tiles);
				// check all the files
				for (FTPFile file : downloads) {
					File local = null;
					try { 
						// the local file
						local = new File(dir.getAbsolutePath()+"/"+file.getName());
						if (!local.exists()) {  // if the local file doesn't exist, get it
							System.out.println(Calendar.getInstance().getTime());
							client.download(file.getName(), local);
						}
						else { // otherwise, just print a message
							System.out.println("\t\t\t Skipping: "+local.getAbsoluteFile());
						}
					} catch (Exception e1) {
						e1.printStackTrace();
						// wait a few seconds
						Thread.sleep(10000); 
						if (!client.isConnected()) { // if disconnected in the middle
							connectUMT();
						}
						else { // try reconnecting
							client.disconnect(false);
							connectUMT();
						}
						// go back to the right directory
						client.changeDirectory(f.getName());
						// if the local file matches the remote file, just keep going
						if (local.exists()) {
							if (local.length() != file.getSize()) {
								// try again
								client.download(file.getName(), new File(dir.getAbsolutePath()+"/"+file.getName()));
							}
						}
					}
				}
				// go back to the parent directory
				client.changeDirectoryUp();
			}
		}	
	}
	
	
	/**
	 * Helper to get only the files for the specified tiles.
	 * @param list
	 * @param tiles
	 * @return
	 */
	private ArrayList<FTPFile> getFileList(FTPFile[] list, ArrayList<String> tiles) {
		ArrayList<FTPFile> files = new ArrayList<FTPFile>(tiles.size());
		for (String s : tiles) {
			for (FTPFile file : list) {
				if (file.getName().contains(s)) {
					files.add(file);
				}
			}
		}
		return files;
	}
	
	/**
	 * Read tile coordinates from a csv file.
	 * @param fileName
	 * @param h is the field index of the h coordinate
	 * @param v is the field index of the v coordinate
	 * @return
	 */
	private static ArrayList<String> readTilesFromFile(String fileName, int hIndex, int vIndex) throws Exception {
		ArrayList<String> tiles = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
		// skip the header
		String line = reader.readLine();
		while ((line = reader.readLine()) != null) { 
			String[] toks = line.split(",");
			// Convert from String to double to String (to get rid of decimals)
			int h = (int)Double.parseDouble(toks[hIndex]);
			int v = (int)Double.parseDouble(toks[vIndex]);
			String tile = "h"+(h<10 ? "0"+h : h)+"v"+(v<10 ? "0"+v : v);
			tiles.add(tile);
		}
		reader.close();
		return tiles;
	}
	
	/**
	 * Helper class.
	 * @author Nicholas
	 *
	 */
	class Listener implements FTPCommunicationListener {
		@Override
		public void sent(String statement) {
			System.out.println(statement);
		}

		@Override
		public void received(String statement) {
			System.out.println(statement);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// MOD11
//		String remoteFolder = "MODIS_Composites/MOLT/MOD11A2.005/";
//		// Made from the shapefile by selecting tiles that intersect continents
//		String tileFile = "C:/Users/Nicholas/Documents/shapefiles/modis_sinusoidal/Continental_tiles_20120305.txt";
//		Getter getter = null;
//		try {
//			getter = new Getter(remoteFolder, 2010);
//			// header: "FID_","cat","h","v"
//			ArrayList<String> tiles = readTilesFromFile(tileFile, 2, 3);
//			File localDir = new File("C:/Users/Nicholas/Documents/MOD11A2/");
//			getter.download(tiles, localDir);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			getter.disconnect();
//		}
		// OK.  20120311
		
		
		// MOD13
//		String remoteFolder = "MODIS_Composites/MOLT/MOD13A1.005";
//		// Made from the shapefile by selecting tiles that intersect continents
//		String tileFile = "C:/Users/Nicholas/Documents/shapefiles/modis_sinusoidal/Continental_tiles_20120305.txt";
//		Getter getter = null;
//		try {
//			getter = new Getter(remoteFolder, 2010);
//			
//			// header: "FID_","cat","h","v"
//			ArrayList<String> tiles = readTilesFromFile(tileFile, 2, 3);
//			File localDir = new File("C:/Users/Nicholas/Documents/MOD13A1/");
//			getter.download(tiles, localDir);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			getter.disconnect();
//		}
		
		// done 20120328
		
		// 1km, 20120416
//		String remoteFolder = "MODIS_Composites/MOLT/MOD13A2.005";
//		// Made from the shapefile by selecting tiles that intersect continents
//		String tileFile = "C:/Users/Nicholas/Documents/shapefiles/modis_sinusoidal/Continental_tiles_20120305.txt";
//		Getter getter = null;
//		try {
//			getter = new Getter(remoteFolder, 2010);
//			// header: "FID_","cat","h","v"
//			ArrayList<String> tiles = readTilesFromFile(tileFile, 2, 3);
//			File localDir = new File("D:/MOD13A2/");
//			getter.download(tiles, localDir);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			getter.disconnect();
//		}
		
		
		// MOD16 from UMT
//		String remoteFolder = "pub/MODIS/Mirror/MOD16/MOD16A2_MONTHLY.MERRA_GMAO_1kmALB/Y2010";
//		// Made from the shapefile by selecting tiles that intersect continents
//		String tileFile = "C:/Users/Nicholas/Documents/shapefiles/modis_sinusoidal/Continental_tiles_20120305.txt";
//		Getter getter = null;
//		try {
//			getter = new Getter(remoteFolder);
//			// header: "FID_","cat","h","v"
//			ArrayList<String> tiles = readTilesFromFile(tileFile, 2, 3);
//			File localDir = new File("C:/Users/Nicholas/Documents/MOD16A2/");
//			getter.downloadMOD16(tiles, localDir);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			getter.disconnect();
//		}
		// 20120601 Done
		
		
		// MOD44, 250m
//		String remoteFolder = "MODIS_Composites/MOLT/MOD44B.005";
//		// Made from the shapefile by selecting tiles that intersect continents
//		String tileFile = "C:/Users/Nicholas/Documents/shapefiles/modis_sinusoidal/Continental_tiles_20120305.txt";
//		Getter getter = null;
//		try {
//			getter = new Getter(remoteFolder, 2010);
//			// header: "FID_","cat","h","v"
//			ArrayList<String> tiles = readTilesFromFile(tileFile, 2, 3);
//			File localDir = new File("C:/Users/Nicholas/Documents/MOD44B.005/");
//			getter.download(tiles, localDir);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			getter.disconnect();
//		}
		
		URL url = null;
		URLConnection con = null;
		int i;
		try {
			url = new URL("http://e4ftl01.cr.usgs.gov/MOLA/MYD11A2.005/");
			con = url.openConnection();
			System.out.println(con.getContentType());
			File file = new File("/home/nclinton/Documents/BROWSE.MYD11A2.A2008001.h08v07.005.2008010084634.2.jpg");
			BufferedInputStream bis = new BufferedInputStream(
					con.getInputStream());
//			BufferedOutputStream bos = new BufferedOutputStream(
//					new FileOutputStream(file.getPath()));
//			System.out.println(file.getName());
			while ((i = bis.read()) != -1) {
				System.out.println(i);
//				bos.write(i);
			}
//			bos.flush();
//			bos.close();
			bis.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
		
		
	}
	
}
