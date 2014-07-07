package map;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import reference.City;

public final class TrainMap {
	private final Map<MilePostId, Milepost> milepostIndex;
	
	/** Initialize the map data from a csv formatted string, where mileposts
	 * are stored as fields in a 2-dimensional array, where each entry is one of:
	 * b - blank (milepost does not exist here)
	 * m - normal milepost
	 * d - desert milepost
	 * h - mountain milepost
	 * a - alpine milepost
	 * f - forest milepost
	 * j - jungle milepost
	 * cName - city milepost, where Name is name of the city
	 * ccName - major city milepost, where Name is name of the city
	 * 
	 * @param mapData
	 * @throws IOException  
	 */
	public TrainMap(BufferedReader mapDataReader) throws IOException {
		milepostIndex = new HashMap<MilePostId, Milepost>();

		int y = 0;
		String line;
		while ((line = mapDataReader.readLine()) != null) {
		   // process the line.
			String [] fields = line.split(",");
			int x = 0;
			for (String field: fields) {
				Milepost.Type mpType = Milepost.Type.BLANK;
				City city = null;
				switch (field)  {
				case "b":
					mpType = Milepost.Type.BLANK;
					break;
				case "d":
					mpType = Milepost.Type.DESERT;
					break;
				case "j": 
					mpType = Milepost.Type.JUNGLE;
					break;
				case "f":
					mpType = Milepost.Type.FOREST;
					break;
				case "h":
					mpType = Milepost.Type.MOUNTAIN;
					break;
				case "a":
					mpType = Milepost.Type.ALPINE;
					break;
				case "m":
					mpType = Milepost.Type.NORMAL;
					break;
				default:
					String cityName;
					if (field.startsWith("cc")) {
						mpType = Milepost.Type.CITY;
						cityName = field.substring(1);
					} 
					else if (field.startsWith("c")) {
						mpType = Milepost.Type.MAJORCITY;
						cityName = field.substring(2);
					}
				}
				Milepost mp = new Milepost(x, y, city, mpType);
				++x;
			}
			++y;
		}
		// need to generate edges!!!
	}
}
