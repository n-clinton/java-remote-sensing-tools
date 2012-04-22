/**
 * 
 */
package cn.edu.tsinghua.lidar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.ColorMode;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.PolyStyle;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.Style;

/**
 * @author Nicholas
 *
 */
public class Glas2KML {

	static {
		System.setProperty("com.sun.media.jai.disableMediaLib", "true");
	}
	
	final Kml kml;
	final Document document;
	private double offset;
	
	public Glas2KML(String name) throws Exception {
		// compute offset in degrees latitude
		offset = 35.0/(60.0*1852.0);  // NOT correct
		
		kml = new Kml();
		document = new Document();
		kml.setFeature(document);
		document.setName(name);
		document.setOpen(true);

		final Style style = new Style();
		document.getStyleSelector().add(style);
		style.setId("PolyStyle");

		final PolyStyle polyStyle = new PolyStyle();
		style.setPolyStyle(polyStyle);

		polyStyle.setColor("ff0000cc");
		polyStyle.setColorMode(ColorMode.NORMAL);
	}
	
	/**
	 * 
	 * @param outKMLName
	 * @throws Exception
	 */
	public void write(String outKMLName) throws Exception {
		kml.marshal(new File(outKMLName));
	}
	
	/**
	 * 
	 * @param lat
	 * @param lon
	 * @param height
	 * @return
	 */
	private List<Coordinate> linearRing(double lat, double lon, double height) {
		List<Coordinate> list = new LinkedList<Coordinate>();
		double lat1 = lat - offset;
		double lat2 = lat + offset;
		// compute longitude scale
		double lonOffset = Math.cos(Math.abs(Math.PI*lat/180.0))*offset;
		double lon1 = lon - lonOffset;
		if (lon1 > 180.0) { lon1 = lon1-360.0; }
		double lon2 = lon + lonOffset;
		if (lon2 > 180.0) { lon2 = lon2-360.0; }
		
		System.out.println("lon,lat,lon1,lat1,lon2,lat2: "+lon+","+lat+","+lon1+","+lat1+","+lon2+","+lat2);
		list.add(new Coordinate(Math.min(lon1, lon2), Math.min(lat1, lat2), height));
		list.add(new Coordinate(Math.min(lon1, lon2), Math.max(lat1, lat2), height));
		list.add(new Coordinate(Math.max(lon1, lon2), Math.max(lat1, lat2), height));
		list.add(new Coordinate(Math.max(lon1, lon2), Math.min(lat1, lat2), height));
		list.add(new Coordinate(Math.min(lon1, lon2), Math.min(lat1, lat2), height));
		return list;
	}
		
	/**
	 * 
	 * @param file
	 * @throws Exception
	 */
	public void processFile(String csvName) throws Exception {
		File file = new File(csvName);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		// skip the header
		String line = reader.readLine();
		while ((line = reader.readLine()) != null) {
			try {
				/*
				 * 0       1   2   3    4         5        6            7            8            9            10           11    12    13    14    15    16    17     18     19     20     21     22
				 * rec_ndx,lat,lon,elev,SigEndOff,SigBegHt,gpCntRngOff2,gpCntRngOff3,gpCntRngOff4,gpCntRngOff5,gpCntRngOff6,gAmp1,gAmp2,gAmp3,gAmp4,gAmp5,gAmp6,gArea1,gArea2,gArea3,gArea4,gArea5,gArea6
				 */
				String[] toks = line.split(",");
				double rec_ndx = Double.parseDouble(toks[0]);
				double lat = Double.parseDouble(toks[1]);
				double lon = Double.parseDouble(toks[2]);
				double height = Double.parseDouble(toks[5]);
				List<Coordinate> outercoord = linearRing(lat, lon, height);
				
				final Placemark placemark = new Placemark();
				document.getFeature().add(placemark);
				placemark.setStyleUrl("#PolyStyle");
				placemark.setName("rec_ndx: "+rec_ndx);
				placemark.setDescription("Height: "+height+" meters");
				
				final Polygon polygon = new Polygon();
				placemark.setGeometry(polygon);

				polygon.setExtrude(true);
				polygon.setAltitudeMode(AltitudeMode.RELATIVE_TO_GROUND);

				final Boundary outerboundary = new Boundary();
				polygon.setOuterBoundaryIs(outerboundary);

				final LinearRing outerlinearring = new LinearRing();
				outerboundary.setLinearRing(outerlinearring);

				outerlinearring.setCoordinates(outercoord);	
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		reader.close();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// test
		String check = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/2009_10_10_GLA14_531_2131_002_0071_0_01_0001_test_compare.csv";
		String out = "C:/Users/Nicholas/Documents/urban/Landscan2010/overlay_with_GLA14/2009_10_10_GLA14_531_2131_002_0071_0_01_0001_test_compare.kml";
		try {
			Glas2KML g2kml = new Glas2KML(out);
			g2kml.processFile(check);
			g2kml.write(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
