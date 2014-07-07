import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import map.MilePostId;
import map.TrainMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class DirectoryFileFilter implements FileFilter {

	@Override
	public boolean accept(File pathname) {
		return pathname.isDirectory();
	}
	
}


/** Maps incoming data from JSON strings into calls on a specific game. Maintains the list 
 * of in progress games.
 */
public class TrainServer {
	private static Logger log = LoggerFactory.getLogger(HttpTrainServerHandler.class);

	private static RandomString gameNamer = new RandomString(8); // use for
																	// generating
																	// (semi)unique
																	// gameIds

	static Map<String, Game> games = new HashMap<String, Game>(); // games currently in progress;
	
	static private final String dataDirectoryPath = "../../data";


	static class NewGameData {
		//public String messageType;
		public String pid; // host playerId
		public String color; // color for track building
		public String ruleSet; // name for rules of the game
		public String gameType; // which game (Africa, Eurasia, etc.)
		
		NewGameData() {}
	}
	
	static class NewGameResponse {
		public String gid;
		NewGameResponse() {}
	}
	
	static public String newGame(String requestText) throws GameException {			
		String gameId = null;
		Gson gson = new GsonBuilder().create();
		NewGameData data = gson.fromJson(requestText, NewGameData.class);
		
		Game game = new Game(getMapData(data.gameType), data.ruleSet);
		gameId = gameNamer.nextString();
		games.put(gameId, game);
		game.joinGame(data.pid, data.color);
		NewGameResponse response = new NewGameResponse();
		response.gid = gameId;
		return gson.toJson(response);
	}

	static class JoinGameData {
		public String gid;
		public String pid;
		public String color;
		}
	
	static public void joinGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		JoinGameData data = gson.fromJson(requestText, JoinGameData.class);
		Game game = games.get(data.gid);
		if (game == null)
		{
			log.warn("Can't find game {}", data.gid);
			for (String key: games.keySet())
				log.info("found gid {}", key);
			throw new GameException(GameException.GAME_NOT_FOUND);
		}
		game.joinGame(data.pid, data.color);

	}

	static class StartGameData {
		public String gid;
		public String pid;
	}

	static public void startGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		StartGameData data = gson.fromJson(requestText, StartGameData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.startGame(data.pid);
	}

	static class BuildTrackData {
		public String gid;
		public String pid;
		public MilePostId[] mileposts;
	}

	static public void buildTrack(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		BuildTrackData data = gson.fromJson(requestText, BuildTrackData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.buildTrack(data.pid, data.mileposts);
	}

	static class UpgradeTrainData {
		public String gid;
		public String pid;
		public String upgradeType;
	}

	static public void upgradeTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		UpgradeTrainData data = gson.fromJson(requestText,
				UpgradeTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!data.upgradeType.equals("Capacity") && !data.upgradeType.equals("Speed"))
			throw new GameException(GameException.INVALID_UPGRADE);
		game.upgradeTrain(data.pid,
				data.upgradeType.equals("Capacity") ? UpgradeType.CAPACITY
						: UpgradeType.SPEED);
	}

	static class StartTrainData {
		public String gid;
		public String pid;
		public String city;
	}

	static public void startTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		StartTrainData data = gson.fromJson(requestText, StartTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.startTrain(data.pid, data.city);
	}

	static class MoveTrainData {
		public String gid;
		public String pid;
		public MilePostId[] mileposts;
	}

	static public void moveTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		MoveTrainData data = gson.fromJson(requestText, MoveTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.moveTrain(data.pid, data.mileposts);
	}

	static class PickupLoadData {
		public String gid;
		public String pid;
		public String city;
		public String load;
	}

	static public void pickupLoad(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		PickupLoadData data = gson.fromJson(requestText, PickupLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.pickupLoad(data.pid, data.city, data.load);
	}

	static class DeliverLoadData {
		public String gid;
		public String pid;
		public String city;
		public String load;
	}

	static public void deliverLoad(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		DeliverLoadData data = gson
				.fromJson(requestText, DeliverLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.deliverLoad(data.pid, data.city, data.load);
	}

	static class DumpLoadData {
		public String gid;
		public String pid;
		public String load;
	}

	static public void dumpLoad(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		DumpLoadData data = gson.fromJson(requestText, DumpLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.dumpLoad(data.pid, data.load);
	}

	static class EndTurnData {
		public String gid;
		public String pid;
	}

	static public void endTurn(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		EndTurnData data = gson.fromJson(requestText, EndTurnData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.endTurn(data.pid);
	}

	static class EndGame {
		public String gid;
		public String pid;
	}

	static public void endGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		EndGame data = gson.fromJson(requestText, EndGame.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.endGame(data.pid);
	}
	
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
	
	static private TrainMap getMapData(String gameType) throws GameException {
		String mapDataFolderPath = dataDirectoryPath + File.separator + gameType;
		File mapDataDir = new File(mapDataFolderPath);
		if (!mapDataDir.isDirectory())
			throw new GameException(GameException.GAME_NOT_FOUND);
		String mapDataPath = mapDataFolderPath + File.separator + "map.csv";
		File mapDataFile = new File(mapDataPath);
		if (!mapDataFile.isFile())
			throw new GameException(GameException.GAME_NOT_FOUND);
		TrainMap map = null;
		try {
			BufferedReader mapDataReader = new BufferedReader(new FileReader(mapDataFile));
			map = new TrainMap(mapDataReader);
			mapDataReader.close();
		} catch (FileNotFoundException e) {
			log.error("FileNotFoundException reading game map {}", e);
			throw new GameException(GameException.GAME_NOT_FOUND);
		} catch (IOException e) {
			log.error("IOException reading game map {}", e);
			throw new GameException(GameException.GAME_NOT_FOUND);
		}
		return map;
	}

}
