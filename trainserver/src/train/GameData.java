package train;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import map.Milepost;
import map.MilepostId;
import map.TrainMap;
import reference.Card;
import reference.City;
import reference.Trip;

class DirectoryFileFilter implements FileFilter {

	@Override
	public boolean accept(File pathname) {
		return pathname.isDirectory();
	}
	
}


/** Holds the static description of the game -- the map, the cards. Does not include
 * any data specific to an instance of a game.
 */
public class GameData {

	private List<Card> deck;	/** Cards holding delivery possibilities */
	private int currentCard;
	private TrainMap map;		/** Mileposts, cities, building costs */
	private Map<String, City> cities;	/** Cities indexed by city name, contains loads found in each city */
	private Map<String, Set<City>> loads; /** Key=load, Value= cities where loads can be obtained */
	private String geography;	/** which game is played (africa, russia, china, etc.) */
	
	/** Directory where data for all games is stored */
	static private String dataDirectoryPath = null;

	private static Logger log = LoggerFactory.getLogger(GameData.class);

	public GameData(String gameType) throws GameException {
		try {
			setDataFolder();
		} catch (IOException e) {
			log.error("Cannot find data folder");
			throw new GameException(GameException.BAD_MAP_DATA);
		}
		loads = new HashMap<String, Set<City>>();
		cities = getCityData(gameType);
		deck = getCardData(gameType);
		currentCard = -1;
		Collections.shuffle(deck);
		map = getMapData(gameType, cities);
		geography = gameType;
	}
	
	public GameData(String gameType, List<Card>  deck) throws GameException {
		try {
			setDataFolder();
		} catch (IOException e) {
			log.error("Cannot find data folder");
			throw new GameException(GameException.BAD_MAP_DATA);
		}
		loads = new HashMap<String, Set<City>>();
		cities = getCityData(gameType);
		currentCard = -1;
		map = getMapData(gameType, cities);
		geography = gameType;
		this.deck = deck;
	}
	
	public List<Card> getDeck() { return deck; }
	public Card draw() {
		currentCard++;
		return deck.get(currentCard);
	}
	public Map<String,City> getCities() { return cities; }
	public TrainMap getMap() { return map; }
	public Map<String, Set<City>> getLoads() { return loads; }
	public String getGeography() { return geography; }
	
	/** Looks for the game's data directory starting at the current working directory,
	 * and looking at all children of the directory for a child named "data". If not 
	 * found, go up a level and try again.
	 * @return
	 * @throws IOException 
	 */
	static private void setDataFolder() throws IOException {
	    Path start = Paths.get(System.getProperty("user.dir"));
	    while (start != null && dataDirectoryPath == null) {
    		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
		        @Override
		        public FileVisitResult preVisitDirectory(Path dir,  BasicFileAttributes attrs)
		            throws IOException
		        {
	            	if (dir.getFileName().endsWith("data")) {
	            		File dataFile = dir.toFile();
	            		dataDirectoryPath = dataFile.getAbsolutePath();
	            		return FileVisitResult.TERMINATE;
	            	}
	            	else
	            		return FileVisitResult.CONTINUE;
		        }
		    });
    	    start = start.getParent();
	    }
	}
	
	/** Return the path name of the directory containing data files for the game */
	static private String getDataPath(String gameType) throws GameException {
		String mapDataFolderPath = dataDirectoryPath + File.separator + gameType;
		File mapDataDir = new File(mapDataFolderPath);
		if (!mapDataDir.isDirectory())
			throw new GameException(GameException.GAME_NOT_FOUND);
		return mapDataFolderPath;
	}
	
	/** Return the requested data file */
	static private File getDataFile(String gameType, String fileName) throws GameException {
		String mapDataFolderPath = getDataPath(gameType);
		String mapDataPath = mapDataFolderPath + File.separator + fileName;
		File mapDataFile = new File(mapDataPath);
		if (!mapDataFile.isFile())
			throw new GameException(GameException.GAME_NOT_FOUND);
		return mapDataFile;
	}
	
	/** Read in the data for the deck of cards used for the game. 
	 * @throws IOException */
	private List<Card> getCardData(String gameType) throws GameException {
		List<Card> deck = new ArrayList<Card>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(getDataFile(gameType, "cards.csv")));
			String line = reader.readLine();	// skip header row
			while ((line = reader.readLine()) != null) {
				Trip[] cardData = new Trip[3];
				String[] fields = line.split(",");
				if (fields.length != 9) {
					log.error("Expected 9 fields in card, found only {}", fields.length);
					reader.close();
					throw new GameException(GameException.BAD_CARD_DATA);
				}
				
				// Check that the cities on the card all exist in the city list
				// Check that each load is available in some city
				if (!cities.containsKey(fields[0]))
					log.info("Card delivers to {}, which is not in the city list", fields[0]);
				if (!loads.containsKey(fields[1]))
					log.info("Card uses load {}, which is not available in any city from the city list", fields[1]);
				cardData[0] = new Trip(fields[0], fields[1], Integer.parseInt(fields[2]));			
				if (!cities.containsKey(fields[3]))
					log.info("Card delivers to {}, which is not in the city list", fields[3]);
				if (!loads.containsKey(fields[4]))
					log.info("Card uses load {}, which is not available in any city from the city list", fields[4]);
				cardData[1] = new Trip(fields[3], fields[4], Integer.parseInt(fields[5]));			
				if (!cities.containsKey(fields[6]))
					log.info("Card delivers to {}, which is not in the city list", fields[6]);
				if (!loads.containsKey(fields[7]))
					log.info("Card uses load {}, which is not available in any city from the city list", fields[7]);
				cardData[2] = new Trip(fields[6], fields[7], Integer.parseInt(fields[8]));
				log.debug("Card for delivering {} to {} for {}, {} to {} for {}, or {} to {} for {}",
						cardData[0].load, cardData[0].dest, cardData[0].cost,
						cardData[1].load, cardData[1].dest, cardData[1].cost,
						cardData[2].load, cardData[2].dest, cardData[2].cost);
				deck.add(new Card(cardData));
			}
		} catch (FileNotFoundException e) {
			log.error("FileNotFoundException reading game map {}", e);
			throw new GameException(GameException.GAME_NOT_FOUND);
		} catch (IOException e) {
			log.error("IOException reading game map {}", e);
			throw new GameException(GameException.GAME_NOT_FOUND);
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch(IOException e) {
				log.error("IOException closing reader");
			}
		}
		return deck;
	}
	
	/** Read in the data for the map, which includes the basic milepost information,
	 * in the map.csv file, the river crossings list, sea inlets and lakes
	 * crossing list, and ferry crossings list. 
	 * 
	 * Cost for building from each milepost to its connecting mileposts
	 * are calculated using the milespost types in the map file, and adding in the 
	 * extra cost if the track crosses either a river or sea inlet/lake.
	 * 
	 * @param gameType
	 * @return
	 * @throws GameException
	 */
	static private TrainMap getMapData(String gameType, Map<String, City> cities) 
			throws GameException {
		TrainMap map = null;
		try {
			BufferedReader mapDataReader = new BufferedReader(new FileReader(getDataFile(gameType, "map.csv")));
			BufferedReader riverDataReader = new BufferedReader(new FileReader(getDataFile(gameType, "rivers.csv")));
			BufferedReader seaDataReader = new BufferedReader(new FileReader(getDataFile(gameType, "seas.csv")));
			BufferedReader ferryDataReader = new BufferedReader(new FileReader(getDataFile(gameType, "ferries.csv")));
			map = new TrainMap(mapDataReader, riverDataReader, seaDataReader, ferryDataReader, cities);
			mapDataReader.close();
			riverDataReader.close();
			seaDataReader.close();
		} catch (FileNotFoundException e) {
			log.error("FileNotFoundException reading game map {}", e);
			throw new GameException(GameException.GAME_NOT_FOUND);
		} catch (IOException e) {
			log.error("IOException reading game map {}", e);
			throw new GameException(GameException.GAME_NOT_FOUND);
		}
		return map;
	}
	
	private Map<String, City> getCityData(String gameType) throws GameException {
		Map<String, City> cities = new HashMap<String, City>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(getDataFile(gameType, "city.csv")));
			String line = reader.readLine();	// skip header row
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split(",");
				String cityName = fields[0];
				boolean majorCity = cityName.startsWith("m");
				if (majorCity)
					cityName = cityName.substring(1);
				List<String> cityloads = new ArrayList<String>();
				for (int i = 1; i < fields.length && fields[i].length() > 0; ++i)
					cityloads.add(fields[i]);
				log.debug("{} city {}", majorCity ? "Major" : "Minor", cityName);
				City city = new City(cityName, cityloads, majorCity);
				for (String load: cityloads) {
					// Add it to the loads index
					Set<City> loadLocations = loads.get(load);
					if (loadLocations == null)
						loadLocations = new HashSet<City>();
					loadLocations.add(city);
					loads.put(load, loadLocations);
					log.debug("{}", load);
				}
				cities.put(cityName, city);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			log.error("FileNotFoundException reading city data {}", e);
			throw new GameException(GameException.GAME_NOT_FOUND);
		} catch (IOException e) {
			log.error("IOException reading city data {}", e);
			throw new GameException(GameException.GAME_NOT_FOUND);
		}
		return cities;
	}
	
	/** Returns a list of the supported geographies (game types). All geographies must have a folder
	 * in the data directory that contains all the data for the game. The name of the geography 
	 * is the name of the folder.
	 * @return List of game types
	 */
	public static List<String> getGeographies() throws GameException {
		try {
			setDataFolder();
		} catch (IOException e) {
			log.error("Cannot find data folder");
			throw new GameException(GameException.BAD_MAP_DATA);
		}

		String[] excludedDirs = {"artwork", "icons", "mileposts"};	// subfolders of data that are not game types
		List<String> excluded = Arrays.asList(excludedDirs);
		File dataDir = new File(dataDirectoryPath);
		if (!dataDir.isDirectory())
			throw new GameException(GameException.GAME_NOT_FOUND);
		File[] dataChildren = dataDir.listFiles((File file) -> 
        	file.isDirectory() &&
        	!excluded.contains(file.getName()));
		List<String> geographyList = new ArrayList<String>();
		for (File child: dataChildren) 
			geographyList.add(child.getName());
		return geographyList;
	}

	public Milepost getMilepost(MilepostId mid){
		return map.getMilepost(mid);
	}
}
