package train;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import map.MilepostId;
import map.TrainMap;
import map.Edge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reference.City;
import reference.UpgradeType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/** Maps incoming data from JSON strings into calls on a specific game. Maintains the list 
 * of in progress games.
 */
public class TrainServer {
	private static Logger log = LoggerFactory.getLogger(TrainServer.class);

	private static RandomString gameNamer = new RandomString(8); // use for
																	// generating
																	// (semi)unique
																	// gameIds

	static Map<String, Game> games = new HashMap<String, Game>(); // games currently in progress;
	
	static public Game getGame(String gid) {
		return games.get(gid);		
	}
	
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
		public TrainMap.SerializeData mapData;
		public Map<String, City> cities;	/** Cities indexed by city name, contains loads found in each city */
		public Map<String, Set<City>> loads; /** Key=load, Value= cities where loads can be obtained */
		NewGameResponse() {}
	}
	
	private static class EdgeSerializer implements JsonSerializer<Edge> {
		  public JsonElement serialize(Edge src, Type typeOfSrc, JsonSerializationContext context) {
		    return new JsonPrimitive(src.toString());
		  }		
	}
	
	static public String newGame(String requestText) throws GameException {			
		String gameId = null;
		Gson gson = new GsonBuilder().create();
		NewGameData data = gson.fromJson(requestText, NewGameData.class);
		
		
		GameData gameData = new GameData(data.gameType);
		Game game = new Game(gameData, data.ruleSet);
		gameId = gameNamer.nextString();
		games.put(gameId, game);
		game.joinGame(data.pid, data.color);

		// Send a JSON response that has gid, serialized map data, list of cities and loads
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Edge.class, new EdgeSerializer());
		NewGameResponse response = new NewGameResponse();
		response.mapData = gameData.map.getSerializeData();
		response.cities = gameData.cities;
		response.loads = gameData.loads;
		response.gid = gameId;
		return gsonBuilder.create().toJson(response);
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
		public MilepostId[] mileposts;
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
		public MilepostId where;
	}

	static public void startTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		StartTrainData data = gson.fromJson(requestText, StartTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.startTrain(data.pid, data.where);
	}

	static class MoveTrainData {
		public String gid;
		public String pid;
		public MilepostId[] mileposts;
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
	
}
