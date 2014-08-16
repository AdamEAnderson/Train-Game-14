package train;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import map.Milepost;
import map.MilepostId;
import map.TrainMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.Player;
import player.Train;

import reference.Card;
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
	
	private static String statusCache = null;
	private static int statusTransaction = 0;	// transaction that was current when statusCache was created
	private static String statusGid = null;		// GID used for generating statusCache

	private static RandomString gameNamer = new RandomString(8); // use for
																	// generating
																	// (semi)unique
																	// gameIds

	static Map<String, Game> games = new HashMap<String, Game>(); // games currently in progress;
	
	static public Game getGame(String gid) {
		return games.get(gid);		
	}
	
	static class PlayerStatus {
		public String pid;
		public String color;
		public Train[] trains;
		public int money;
		public Map<MilepostId, Set<MilepostId>> rail;
		public Card[] hand;
		public int spendings;
		public int movesMade;
		PlayerStatus() {}
	}
	
	static class GameStatus {
		public String gid;
		public String activeid;
		public String lastid;
		public String geography;
		public boolean joinable;
		public boolean ended;
		public int turns;
		public List<PlayerStatus> players; //in turn order beginning with the active player
		public int transaction;
		GameStatus() {}
	}
	
	private static class MilepostSerializer implements JsonSerializer<Milepost> {
		  public JsonElement serialize(Milepost src, Type typeOfSrc, JsonSerializationContext context) {
		    return new JsonPrimitive(src.toString());
		  }		
	}
	
	static class StatusRequest {
		public String gid;
		StatusRequest() {}
	}
	
	synchronized static public String status(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		StatusRequest data = gson.fromJson(requestText, StatusRequest.class);
		String gid = data.gid;
		Game game = getGame(gid);
		if (game == null)
			return "{}";

		// Status hasn't changed since the last time we sent a response -- just resend
		if (gid.equals(statusGid) && game.transaction() == statusTransaction && statusCache != null)  
			return statusCache;
			
		// Generate a new status message
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Milepost.class, new MilepostSerializer());
		GameStatus status = new GameStatus();
		status.gid = gid;
		status.players = new ArrayList<PlayerStatus>();
		status.geography = game.gameData.geography;
		status.transaction = game.transaction();
		status.joinable = game.getJoinable();
		status.ended = game.isOver();
		status.turns = game.getTurns();
		Player p = game.getActivePlayer();
		if(game.getLastPlayer() == null) {
			status.activeid = "";
			status.lastid = "";
		} else {
			status.activeid = game.getActivePlayer().name;
			status.lastid = game.getLastPlayer().name;
		}
		do {
			PlayerStatus pstatus = new PlayerStatus();
			pstatus.pid = p.name;
			pstatus.color = p.color;
			pstatus.trains = p.getTrains();
			pstatus.money = p.getMoney();
			pstatus.spendings = p.getSpending();
			pstatus.movesMade = p.getMovesMade();
			
			Map<Milepost, Set<Milepost>> railMileposts = p.getRail().getRail();
			Map<MilepostId, Set<MilepostId>> railIds = new HashMap<MilepostId, Set<MilepostId>>();
			for(Milepost outer : railMileposts.keySet()){
				Set<MilepostId> inner = new HashSet<MilepostId>();
				railIds.put(new MilepostId(outer.x, outer.y), inner);
				for(Milepost m : railMileposts.get(outer)){
					inner.add(new MilepostId(m.x, m.y));
				}
			}
			pstatus.rail = railIds;
			pstatus.hand = p.getCards();
			status.players.add(pstatus);
			p = p.getNextPlayer();
		}while(p != game.getActivePlayer());
		
		statusCache = gsonBuilder.create().toJson(status);
		statusGid = gid;
		statusTransaction = game.transaction();
		
		return statusCache;
	}
	
	static class ListRequest {
		public String listType;
		ListRequest() {}
	}
	
	static class ListResponse {
		public Set<String> gids;
		ListResponse() { gids = new HashSet<String>(); }
	}
	
	synchronized static public String list(String requestText) throws GameException {
		log.info("list requestText: {}", requestText);
		Gson gson = new GsonBuilder().create();
		ListRequest data = gson.fromJson(requestText, ListRequest.class);
		ListResponse responseData = new ListResponse();
		if (data.listType.equals("joinable")) {
			for (String gid : games.keySet())
				if (games.get(gid).isJoinable())
					responseData.gids.add(gid);
		}
		else if (data.listType.equals("resumeable")) {
			for (String gid : games.keySet())
				if (!games.get(gid).isJoinable())
					responseData.gids.add(gid);
		}
		else if (data.listType == "all")
			responseData.gids = games.keySet();
		String result = gson.toJson(responseData);
		log.info("list response {}", result);
		return gson.toJson(responseData);
	}
	
	static class NewGameData {
		//public String messageType;
		public String pid; // host playerId
		public String color; // color for track building
		public RuleSet ruleSet; // name for rules of the game
		public String gameType; // which game (Africa, Eurasia, etc.)
		
		NewGameData() {}
	}
	
	static class NewGameResponse {
		public String gid;
		public String geography;
		public TrainMap.SerializeData mapData;
		public Collection<City> cities;	/** Cities indexed by city name, contains loads found in each city */
		public Map<String, Set<String>> loadset; /** Key=load, Value= cities where loads can be obtained */
		NewGameResponse() {}
	}
	
	static public String buildNewGameResponse(String gid, GameData gameData) {
		// Build a JSON string that has gid, serialized map data, list of cities and loads
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Milepost.class, new MilepostSerializer());
		NewGameResponse response = new NewGameResponse();
		response.mapData = gameData.map.getSerializeData();
		response.cities = gameData.cities.values();
		response.geography = gameData.geography;
		// Convert from loads to set of cities to loads to set of city names
		response.loadset = new HashMap<String, Set<String>>();
		for (String load: gameData.loads.keySet()) {
			Set<String> cities = new HashSet<String>();
			for (City city:gameData.loads.get(load))
				cities.add(city.name);
			response.loadset.put(load, cities);
		}
		response.gid = gid;
		return gsonBuilder.create().toJson(response);
	}
	
	synchronized static public String newGame(String requestText) throws GameException {			
		String gameId = null;
		Gson gson = new GsonBuilder().create();
		NewGameData data = gson.fromJson(requestText, NewGameData.class);
		
		GameData gameData = new GameData(data.gameType);
		if (data.ruleSet == null)
			data.ruleSet = new RuleSet(4, 70, 1);
		Game game = new Game(gameData, data.ruleSet);
		gameId = gameNamer.nextString();
		games.put(gameId, game);
		game.joinGame(data.pid, data.color);
		return buildNewGameResponse(gameId, gameData);
	}

	static class JoinGameData {
		public String gid;
		public String pid;
		public String color;
		}
	
	synchronized static public String joinGame(String requestText) throws GameException {
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
		return buildNewGameResponse(data.gid, game.gameData);
	}

	synchronized static public String resumeGame(String requestText) throws GameException {
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
		return buildNewGameResponse(data.gid, game.gameData);
	}

	static class StartGameData {
		public String gid;
		public String pid;
		boolean ready;
	}

	synchronized static public void startGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		StartGameData data = gson.fromJson(requestText, StartGameData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.startGame(data.pid, data.ready);
	}

	static class BuildTrackData {
		public String gid;
		public String pid;
		public MilepostId[] mileposts;
	}

	synchronized static public void buildTrack(String requestText) throws GameException {
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
		public int train;
		
		public UpgradeTrainData() {
			train = 0;
		}
	}

	synchronized static public void upgradeTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		UpgradeTrainData data = gson.fromJson(requestText,
				UpgradeTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!data.upgradeType.equals("Capacity") && !data.upgradeType.equals("Speed"))
			throw new GameException(GameException.INVALID_UPGRADE);
		game.upgradeTrain(data.pid, data.train,
				data.upgradeType.equals("Capacity") ? UpgradeType.CAPACITY
						: UpgradeType.SPEED);
	}

	static class PlaceTrainData {
		public String gid;
		public String pid;
		public int train;
		public MilepostId where;
	}

	synchronized static public void placeTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		PlaceTrainData data = gson.fromJson(requestText, PlaceTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.placeTrain(data.pid, data.train, data.where);
	}

	static class MoveTrainData {
		public String gid;
		public String pid;
		public int train;
		public MilepostId[] mileposts;
	}

	synchronized static public void moveTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		MoveTrainData data = gson.fromJson(requestText, MoveTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.moveTrain(data.pid, data.train, data.mileposts);
	}

	static class PickupLoadData {
		public String gid;
		public String pid;
		public int train;
		public String load;
	}

	synchronized static public void pickupLoad(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		PickupLoadData data = gson.fromJson(requestText, PickupLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.pickupLoad(data.pid, data.train, data.load);
	}

	static class DeliverLoadData {
		public String gid;
		public String pid;
		public int train;
		public String load;
		public int card;
	}

	synchronized static public void deliverLoad(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		DeliverLoadData data = gson
				.fromJson(requestText, DeliverLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.deliverLoad(data.pid, data.train, data.load, data.card);
	}

	static class DumpLoadData {
		public String gid;
		public String pid;
		public int train;
		public String load;
	}

	synchronized static public void dumpLoad(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		DumpLoadData data = gson.fromJson(requestText, DumpLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.dumpLoad(data.pid, data.train, data.load);
	}

	static class TurnInCardsData{
		public String gid;
		public String pid;
	}
	
	synchronized static public void turnInCards(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		TurnInCardsData data = gson.fromJson(requestText, TurnInCardsData.class);
		Game game = games.get(data.gid);
		if(game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.turnInCards(data.pid);
	}
	
	static class EndTurnData {
		public String gid;
		public String pid;
	}

	synchronized static public void endTurn(String requestText) throws GameException {
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
		public boolean ready;
	}

	synchronized static public void endGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		EndGame data = gson.fromJson(requestText, EndGame.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.endGame(data.pid, data.ready);
	}
	
}
