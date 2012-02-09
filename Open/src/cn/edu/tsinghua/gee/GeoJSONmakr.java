/**
 * 
 */
package cn.edu.tsinghua.gee;

import java.awt.image.DataBuffer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.media.jai.PlanarImage;

import com.berkenviro.imageprocessing.JAIUtils;

/**
 * @author nclinton
 *
 */
public class GeoJSONmakr {

	BufferedWriter writer;
	
	/**
	 * 
	 * @param jsonName
	 * @throws Exception
	 */
	public GeoJSONmakr(String jsonName) throws Exception {
		writer = new BufferedWriter(new FileWriter(jsonName));
		writer.write("{"+"\n");
	}
	
	/**
	 * 
	 * @param desc
	 * @throws Exception
	 */
	public void writeDescription(String desc) throws Exception {
		writer.write("\t"+"\"description\":["+"\n");
		writer.write("\t\t\""+desc+"\"\n");
		writer.write("\t],\n");
		writer.write("\n");
	}
	
	/**
	 * TODO: something better
	 * @param vals
	 */
	public void writeBandDefault(String[] vals) throws Exception{
		writer.write("\t"+"\"band_default\":{"+"\n");
		for (String s : vals) {
			writer.write("\t\t"+s+"\n");
		}
		writer.write("\t},\n");
		writer.write("\n");
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void openBands() throws Exception {
		writer.write("\t"+"\"bands\":{"+"\n");
	}
	
	/**
	 * 
	 * @param name
	 * @param vals
	 * @param isLast
	 * @throws Exception
	 */
	public void writeBand(String name, String [] vals) throws Exception {
		writer.write("\t\t"+"\""+name+"\":{"+"\n");
		for (String s : vals) {
			writer.write("\t\t\t"+s+"\n");
		}
		writer.write("\t\t},\n");
	}
	
	/**
	 * TODO: Implement JSON objects instead?
	 * @param name
	 * @param image
	 */
	public void writeBand(String name, int band, String imageFile) throws Exception {
		String [] keyVal = {"\"bandnum\": ", "\"file\": ", "\"bb\": ", "\"type\": ", "\"transform\": "};
		//String [] keyVal = {"\"bandnum\": ", "\"file\": "};
		keyVal[0]+=(band+","); // bandnum
		// instantiate the image
		PlanarImage image = JAIUtils.readImage(imageFile);
		String fileName = (new File(imageFile)).getName();
		keyVal[1]+=("\""+fileName+"\","); // base filename
		keyVal[2]+=("["+image.getWidth()+","+image.getHeight()+"],"); // bounding box
		// check the datatype
		int dataType = image.getSampleModel().getDataType();
		// not sure how to handle "int64", "uint16" and "mask"
		String type;
		switch (dataType) {
			case DataBuffer.TYPE_BYTE: type="uint8"; break;
			case DataBuffer.TYPE_DOUBLE: type="double"; break;
			case DataBuffer.TYPE_FLOAT: type="float"; break;
			case DataBuffer.TYPE_INT: type="int32"; break;
			case DataBuffer.TYPE_SHORT: type="int16"; break;
			default: type="";
		}
		keyVal[3]+=("\""+type+"\",");
		// check the registration
		JAIUtils.geoReader(image);
		keyVal[4]+=("["+String.format("%.5f", image.getProperty("deltaX"))+", 0, 0, "+
				String.format("%.5f", image.getProperty("deltaY"))+", "+
						String.format("%.5f", image.getProperty("ulX"))+", "+
								String.format("%.5f", image.getProperty("ulY"))+"],");
		
		// done with reading info from the image, write
		writeBand(name, keyVal);
	}
	
	/**
	 * Do this last.
	 * @throws Exception
	 */
	public void closeBandsAndFile() throws Exception {
		writer.write("\t}\n");
		writer.write("\n");
		writer.write("}");
		writer.flush();
		writer.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// test
//		String check = "/Users/nclinton/Documents/GEE/MODIS_EVI_test/MCD43A4_005_2009_09_14.EVI.tif";
//		try {
//			GeoJSONmakr makr = new GeoJSONmakr(check+".json");
//			makr.writeDescription("Check of the writer.");
//			makr.writeBandDefault(new String[] {"\"proj\": \"SR-ORG:6965\""});
//			// read image
//			PlanarImage checkImage = JAIUtils.readImage(check);
//			makr.openBands();
//			makr.writeBand("check_00", 0, checkImage);
//			makr.closeBandsAndFile();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		// OK
		
		
	}

}
