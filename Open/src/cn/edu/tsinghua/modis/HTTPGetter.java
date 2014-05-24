package cn.edu.tsinghua.modis;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 
 */

/**
 *
 */
public class HTTPGetter {
	
	static {
		//System.setProperty("java.net.preferIPv4Stack", "true");
	}
	
	int year;
	String productFolder;
	
	public HTTPGetter(String productFolder, int year) throws MalformedURLException {
		this.productFolder = productFolder;
		// check:
		new URL(productFolder);
		this.year = year;
	}
	
	
	private void download(ArrayList<String> tiles, File localDir) throws Exception {
		// get the sub-directories in the product directory
		Document doc = Jsoup.connect(productFolder).get();
		// get the links on this page
		Elements elements = doc.getElementsByAttribute("href");
		for (Element link : elements) {
			if (link.text().startsWith(String.valueOf(year))) {
				System.out.println("Examining link: "+link.absUrl("href"));
				// reproduce this directory structure locally
				File dir = new File(localDir.getAbsoluteFile()+"/"+link.text());
				if (!dir.exists()) { // if the directory doesn't already exist
					System.out.println("\t Creating local directory: "+dir.getAbsolutePath());
					if (!dir.mkdir()) { // make it, or...
						throw new Exception("Can't make: "+dir.getAbsolutePath()); 
					}
				}
				Document sub = Jsoup.connect(link.absUrl("href")).get();
				Elements images = sub.getElementsByAttribute("href");
				System.out.println("\t Building download URL list");
				List<Element> downloads = getDownloadList(images, tiles);
				for (Element element : downloads) {
					File local = new File(dir.getAbsolutePath()+"/"+element.text());
					if (!local.exists()) {
						System.out.println("\t\t local file: "+local.getAbsolutePath());
						URL url = new URL(element.absUrl("href"));
						System.out.println("\t\t downloading: "+url.toString());
						long start = System.currentTimeMillis();
						URLConnection con = url.openConnection();
						BufferedInputStream bis = new BufferedInputStream(con.getInputStream());
						BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(local));
						int i;
						while ((i = bis.read()) != -1) {
							bos.write(i);
						}
						bos.flush();
						bos.close();
						bis.close();
						long end = System.currentTimeMillis();
						System.out.println("\t\t\t Elapsed time: "+((end-start)/1000.0));
					} else {
						System.err.println("\t\t"+local.getAbsolutePath()+" exists!  Not downloading!");
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param list
	 * @param tiles
	 * @return the elements that match the tiles and are hdf files
	 * @throws MalformedURLException
	 */
	private List<Element> getDownloadList(Elements list, ArrayList<String> tiles) throws MalformedURLException {
		List<Element> downloads = new LinkedList<Element>();
		for (String s : tiles) {
			for (Element link : list) {
				if (link.text().endsWith(".hdf") && link.text().contains(s)) {
					downloads.add(link);
				}
			}
		}
		return downloads;
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
	 * @param args
	 */
	public static void main(String[] args) {
		
		// 201040520
		String productFolder = "http://e4ftl01.cr.usgs.gov/MOLA/MYD11A2.005/";
		String localFolder = "/data/MYD11A2/2008";
		int year = 2008;
		String tileFile = "/data/shapefiles/modis_sinusoidal/Continental_tiles_20120305.txt";
		
		try {
			
			HTTPGetter getter = new HTTPGetter(productFolder, year);
			ArrayList<String> tiles = readTilesFromFile(tileFile, 2, 3);
			getter.download(tiles, new File(localFolder));
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
}
