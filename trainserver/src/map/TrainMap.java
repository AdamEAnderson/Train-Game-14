package map;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reference.City;
import train.GameException;

public final class TrainMap {
	private final Map<MilepostId, Milepost> milepostIndex;
	private final Map<String, MilepostId> cityLocations;
	
	private static Logger log = LoggerFactory.getLogger(TrainMap.class);

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
	 * @throws GameException 
	 */
	public TrainMap(BufferedReader mapReader, BufferedReader riverReader, BufferedReader seaReader) throws IOException, GameException {
		milepostIndex = new HashMap<MilepostId, Milepost>();
		cityLocations = new HashMap<String, MilepostId>();

		generateMileposts(mapReader);
		generateEdges(riverReader, seaReader);
	}
	
	public void generateMileposts(BufferedReader mapDataReader) throws IOException, GameException {
		int y = 0;
		String line = mapDataReader.readLine();	// skip over the row header
		while ((line = mapDataReader.readLine()) != null) {
		   // process the line.
			String [] fields = line.split(",");
			int x = 0;
			for (String field: fields) {
				Milepost.Type mpType = Milepost.Type.BLANK;
				City city = null;
				switch (field)  {
				case "b":
				case "l":	/* unbuildable lake equivalent to blank */
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
						mpType = Milepost.Type.MAJORCITY;
						cityName = field.substring(2);
					} 
					else if (field.startsWith("c")) {
						mpType = Milepost.Type.CITY;
						cityName = field.substring(1);
					}
					else {
						log.warn("Unknown milepost type {}", field);
						throw new GameException(GameException.BAD_MAP_DATA);
					}
					cityLocations.put(cityName, new MilepostId(x,y));
					break;
				}
				log.info("Found milepost type {} at [{}, {}]", mpType, x, y);
				Milepost mp = new Milepost(x, y, city, mpType);
				milepostIndex.put(new MilepostId(x,y), mp);
				++x;
			}
			++y;
		}
	}
	
	private boolean isCrossing(MilepostId source, MilepostId destination,
			Map<MilepostId, Set<MilepostId>> crossings) {
		// Source and destination can be any order in the crossings
		Set<MilepostId> crossingMPs = crossings.get(source);
		if (crossingMPs != null && crossingMPs.contains(destination))
			return true;
		crossingMPs = crossings.get(destination);
		if (crossingMPs != null && crossingMPs.contains(source))
			return true;
		
		return false;
	}
	
	private Edge generateEdge(Milepost source, MilepostId destinationId,
			Map<MilepostId, Set<MilepostId>> riverCrossings,
			Map<MilepostId, Set<MilepostId>> seaCrossings) {
		Edge edge = null;
		Milepost destination = milepostIndex.get(destinationId);
		MilepostId sourceId = new MilepostId(source.x, source.y);
		boolean isRiverCrossing = isCrossing(sourceId, destinationId, riverCrossings);
		boolean isSeaCrossing = isCrossing(sourceId, destinationId, seaCrossings);
		
		if (destination != null && destination.type != Milepost.Type.BLANK) {
			edge = new Edge(source, destination, isRiverCrossing, isSeaCrossing);
			log.info("Generating edge from milepost [{}, {}] to milepost [{},{}]", source.y, source.x,
					destination.y, destination.x);
		}
		return edge;
	}
	
	private Map<MilepostId, Set<MilepostId>>  readCrossings(BufferedReader reader) throws IOException, GameException {
		Map<MilepostId, Set<MilepostId>> crossings = new HashMap<MilepostId, Set<MilepostId>>();
		String line;
		while ((line = reader.readLine()) != null) {
			String[] fields = line.split(",");
			if (fields.length != 2) {
				log.error("More than two fields in crossing file");
				throw new GameException(GameException.BAD_MAP_DATA);
			}
			
			String[] mpsSource = fields[0].split(";");
			int ySource = Integer.parseInt(mpsSource[0]);
			int xSource = Integer.parseInt(mpsSource[1]);
			
			String[] mpsDestination = fields[1].split(";");
			int yDestination = Integer.parseInt(mpsDestination[0].trim());
			int xDestination = Integer.parseInt(mpsDestination[1].trim());
			MilepostId mpSource = new MilepostId(xSource, ySource);
			MilepostId mpDestination = new MilepostId(xDestination, yDestination);
			Set<MilepostId> dests = crossings.get(mpSource);
			if (dests == null) 	// add the first mapping for the milepost
				dests = new HashSet<MilepostId>();
			dests.add(mpDestination);
			log.info("Adding river crossing [{},{}] to [{},{}]", ySource, xSource, yDestination, xDestination);
			crossings.put(mpSource, dests);
			//crossings.put(new MilepostId(xSource, ySource), new MilepostId(xDestination, yDestination));
		}
		return crossings;
	}
	
	private void generateEdges(BufferedReader riverReader, BufferedReader seaReader) throws IOException, GameException {
		Map<MilepostId, Set<MilepostId>> riverCrossings = readCrossings(riverReader);
		Map<MilepostId, Set<MilepostId>> seaInletCrossings = readCrossings(seaReader);
		
		Edge[] edges = new Edge[6];
		for (MilepostId mpId: milepostIndex.keySet())  {
			Milepost mp = milepostIndex.get(mpId);
			edges[0] = generateEdge(mp, new MilepostId(mp.x + 1, mp.y - 1), riverCrossings, seaInletCrossings);	// NE
			edges[1] = generateEdge(mp, new MilepostId(mp.x + 1, mp.y), riverCrossings, seaInletCrossings);		// E
			edges[2] = generateEdge(mp, new MilepostId(mp.x + 1, mp.y + 1), riverCrossings, seaInletCrossings);	// SE
			edges[3] = generateEdge(mp, new MilepostId(mp.x, mp.y + 1), riverCrossings, seaInletCrossings);		// SW
			edges[4] = generateEdge(mp, new MilepostId(mp.x - 1, mp.y), riverCrossings, seaInletCrossings);		// W
			edges[5] = generateEdge(mp, new MilepostId(mp.x, mp.y - 1), riverCrossings, seaInletCrossings);		// NW
			mp.updateEdges(edges);
		}
	}
	
	public Milepost getMilepost(MilepostId id){
		return milepostIndex.get(id);
	}
}
