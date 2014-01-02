package cn.edu.tsinghua.gee;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

public class Main {

	public static class FeaturesListResponse {
		public Feature[] features;
	}

	public static class Feature {
		public Geometry geometry;
		public Map properties;
	}

	public static class Geometry {
	}

	public static class PointGeometry extends Geometry {
		public double[] coordinates;
	}

	public static class GeometryDeserializer implements JsonDeserializer {
		public Geometry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			JsonObject g = json.getAsJsonObject();
			JsonElement type = g.get("type");

			if (type.getAsJsonPrimitive().getAsString().equals("Point")) {
				PointGeometry p = new PointGeometry();
				JsonArray coords = g.getAsJsonArray("coordinates");
				p.coordinates = new double[coords.size()];

				for (int i = 0; i < coords.size(); ++i) {
					p.coordinates[i] = coords.get(i).getAsDouble();
				}
				return p;
			}
			return null;
		}
	}

	public static void main(String[] args) {

		try {
			// maps engine hello world example
			URL url = new URL("https://www.googleapis.com/mapsengine/v1/tables/12421761926155747447-06672618218968397709/features?projectId=12421761926155747447&maxResults=500&version=published&key=AIzaSyALl0RC1yYuXJrKDLU7B98m8vuSewLwPGg");
			
			// Earth engine hack-up (doesn't work)
//			String eeString = "https://earthengine.googleapis.com/api/info?id=LANDSAT/L7/LE71230322012234EDC00&key=AIzaSyALl0RC1yYuXJrKDLU7B98m8vuSewLwPGg";
//			URL url = new URL(eeString);
			
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.connect();

			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

			// Deserialize.
			GsonBuilder builder = new GsonBuilder();
			builder.registerTypeAdapter(Geometry.class, new GeometryDeserializer());
			Gson gson = builder.create();

			FeaturesListResponse map = gson.fromJson(reader, FeaturesListResponse.class);
			for (int f=0; f<map.features.length; f++) {
				PointGeometry g = (PointGeometry)map.features[f].geometry;
				System.out.println(g.coordinates[0] + ", " + g.coordinates[1]);
				System.out.println((String)(map.features[f].properties.get("Fcilty_nam")));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			//System.out.println(e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
}