package train;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import map.TrainMap;
import reference.Card;
import reference.City;
import reference.Trip;

public class GameData {

	Queue<Card> deck;
	TrainMap map;
	Map<String, City> cities;
	
	static private final String dataDirectoryPath = "../data";

	private static Logger log = LoggerFactory.getLogger(GameData.class);

	/** Returns a list of the supported game types. All game types must have a folder
	 * in the data directory that contains all the data for the game. Name of the type 
	 * is the name of the folder.
	 * @return List of game types
	 */
	static public List<String> getGameTypes() {
		List<String> gameTypes = new ArrayList<String>();
		File dataDir = new File(dataDirectoryPath);
		File dataChildren[] = dataDir.listFiles(new DirectoryFileFilter());
		for (File child: dataChildren)
			gameTypes.add(child.getName());
		return gameTypes;
	}
	
	public GameData(String gameType) throws GameException {
		cities = getCityData(gameType);
		deck = getCardData(gameType);
		map = getMapData(gameType);
	}
	
	public Queue<Card> getDeck() { return deck; }
	public Map<String,City> getCities() { return cities; }
	public TrainMap getMap() { return map; }
	
	/** Return the path name of the directory containing data files for the game */
	static private String getDataPath(String gameType) throws GameException {
	    log.info("Working Directory = " + System.getProperty("user.dir"));
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
	private Queue<Card> getCardData(String gameType) throws GameException {
		Queue<Card> deck = new ArrayDeque<Card>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(getDataFile(gameType, "cards.csv")));
			String line = reader.readLine();	// skip header row
			Trip[] cardData = new Trip[3];
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split(",");
				if (fields.length != 9) {
					log.error("Expected 9 fields in card, found only {}", fields.length);
					reader.close();
					throw new GameException(GameException.BAD_CARD_DATA);
				}
				
				cardData[0] = new Trip(cities.get(fields[0]), fields[1], Integer.parseInt(fields[2]));			
				cardData[1] = new Trip(cities.get(fields[3]), fields[4], Integer.parseInt(fields[5]));			
				cardData[2] = new Trip(cities.get(fields[6]), fields[7], Integer.parseInt(fields[8]));
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
	 * in the map.csv file, the river crossings list, and sea inlets and lakes
	 * crossing list. Cost for building from each milepost to its connecting mileposts
	 * are calculated using the milespost types in the map file, and adding in the 
	 * extra cost if the track crosses either a river or sea inlet/lake.
	 * 
	 * @param gameType
	 * @return
	 * @throws GameException
	 */
	static private TrainMap getMapData(String gameType) throws GameException {

		TrainMap map = null;
		try {
			BufferedReader mapDataReader = new BufferedReader(new FileReader(getDataFile(gameType, "map.csv")));
			BufferedReader riverDataReader = new BufferedReader(new FileReader(getDataFile(gameType, "rivers.csv")));
			BufferedReader seaDataReader = new BufferedReader(new FileReader(getDataFile(gameType, "seas.csv")));
			map = new TrainMap(mapDataReader, riverDataReader, seaDataReader);
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
	
	static private Map<String, City> getCityData(String gameType) throws GameException {
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
				List<String> loads = new ArrayList<String>();
				for (int i = 0; i < fields.length && fields[i].length() > 0; ++i)
					loads.add(fields[i]);
				cities.put(cityName, new City(cityName, loads, majorCity));
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

}
