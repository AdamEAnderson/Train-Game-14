package train;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.function.Predicate;

import map.Milepost;
import map.MilepostId;
import map.TrainMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.Player;
import player.Stats;
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

class GameGCTask extends TimerTask {

	@Override
	public void run() {
		TrainServer.removeOldGames();
	}
}

class GameGC  {

	private Timer timer;
	private GameGCTask task;
	
	GameGC(long interval) {
		timer = new Timer();
		task = new GameGCTask();
		timer.schedule(task, interval, interval);
	}
	
	void stop() { task.cancel(); }
}
	
/** Maps incoming data from JSON strings into calls on a specific game. Maintains the list 
 * of in progress games.
 */
public class TrainServer {
	private static Logger log = LoggerFactory.getLogger(TrainServer.class);
	
	private static String statusCache = null;
	private static int statusTransaction = 0;	// transaction that was current when statusCache was created
	private static String statusGid = null;		// GID used for generating statusCache

	private static long hourMilli = 3600000L;		// Number of milliseconds in one hour
	private static long fortnightMilli = 1209600000L; // Number of milliseconds in 14 days
	private static long endedExpiration = hourMilli;// Number of milliseconds before a game that has ended will be removed
	private static long notStartedExpiration = hourMilli;// Number of milliseconds before a game was never started will be removed
	private static long abandonedExpiration = fortnightMilli;// Number of milliseconds since last change before a game will be removed
	
	private static GameGC gameGC = new GameGC(hourMilli);		// garbage collect old games
	private static RandomString gameNamer = new RandomString(8); // use for
																	// generating
																	// (semi)unique
																	// gameIds

	static Map<String, Game> games = new HashMap<String, Game>(); // games currently in progress;
	
	static void stop() { gameGC.stop(); }
	
	static public Game getGame(String gid) {
		return games.get(gid);		
	}
	
	/** TEST ONLY! */
	static public void resetExpirations(long ended, long notStarted, long abandoned) {
		endedExpiration = ended;
		notStartedExpiration = notStarted;
		abandonedExpiration = abandoned;
		gameGC.stop();
		gameGC = new GameGC(endedExpiration);
	}
	
	/** TEST ONLY! */
	static public void resetExpirations() {
		endedExpiration = hourMilli;
		notStartedExpiration = hourMilli;
		abandonedExpiration = fortnightMilli;
		gameGC.stop();
		gameGC = new GameGC(endedExpiration);
	}
	
	
	// For a given game, return its gameId, or null if not found
	static public String getGameId(Game game) {
		for (Map.Entry<String, Game> entry: games.entrySet()) {
			if (entry.getValue() == game)
				return entry.getKey();
		}
		return null;
	}
	
	static class PlayerStatus {
		public String pid;
		public String color;
		public Train[] trains;
		public int money;
		public Map<MilepostId, Set<MilepostId>> rail;
		public Card[] hand;
		public int spendings;
		public int[] movesMade;
		public Stats stats;
		PlayerStatus() {}
		PlayerStatus(Player p) {
			pid = p.name;
			color = p.color;
			trains = p.getTrains();
			money = p.getMoney();
			spendings = p.getSpending();
			movesMade = p.getMovesMade();
			stats = p.stats();
			
			Map<Milepost, Set<Milepost>> railMileposts = p.getRail().getRail();
			Map<MilepostId, Set<MilepostId>> railIds = new HashMap<MilepostId, Set<MilepostId>>();
			for(Milepost outer : railMileposts.keySet()){
				Set<MilepostId> inner = new HashSet<MilepostId>();
				railIds.put(new MilepostId(outer.x, outer.y), inner);
				for(Milepost m : railMileposts.get(outer)){
					inner.add(new MilepostId(m.x, m.y));
				}
			}
			rail = railIds;
			hand = p.getCards();
		}
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
	
	static public String status(String requestText) throws GameException {
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
		Player activePlayer = game.getActivePlayer();
		status.activeid = activePlayer != null ? activePlayer.name : "";
		Player lastPlayer = game.getLastPlayer();
		status.lastid = lastPlayer != null ? lastPlayer.name : "";
		
		// If the game is in progress, report the players in the order in which they are 
		// playing, with the active player first. If the game has ended, there is no active
		// player, and players are reported in the order in which they joined the game
		Player p = game.getActivePlayer();
		if (p != null) {
			do {
				status.players.add(new PlayerStatus(p));
				p = p.getNextPlayer();
			} while(p != game.getActivePlayer());
		} else {
			for (Player player : game.getPlayers()) 
				status.players.add(new PlayerStatus(player));
		}
		
		statusCache = gsonBuilder.serializeNulls().create().toJson(status);
		statusGid = gid;
		statusTransaction = game.transaction();
		
		return statusCache;
	}
	
	static class ListRequest {
		public String listType;
		ListRequest() {}
	}
	
	static class ListResponse {
		public Map<String, String> gidNames;
		ListResponse() { gidNames = new HashMap<String, String>(); }
	}
	
	static public String list(String requestText) throws GameException {
		log.info("list requestText: {}", requestText);
		Gson gson = new GsonBuilder().create();
		ListRequest data = gson.fromJson(requestText, ListRequest.class);
		ListResponse responseData = new ListResponse();
		if (data.listType.equals("joinable")) {
			for (String gid : games.keySet()) {
				Game game = games.get(gid);
				if (game.isJoinable())
					responseData.gidNames.put(gid, game.name());
			}
		}
		else if (data.listType.equals("resumeable")) {
			for (String gid : games.keySet()) {
				Game game = games.get(gid);
				if (!game.isJoinable())
					responseData.gidNames.put(gid, game.name());
			}
		}
		else if (data.listType == "all") {
			for (String gid : games.keySet()) 
				responseData.gidNames.put(gid, games.get(gid).name());
		}
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
		public String name;	// display (human readable) name of game
		
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
	
	static private NewGameResponse newGameResponse(String gid, GameData gameData){
		NewGameResponse response = new NewGameResponse();
		response.mapData = gameData.map.getSerializeData();
		response.cities = gameData.cities.values();
		response.geography = gameData.geography;
		// Convert from loads to set of cities to loads to set of city names
		response.loadset = new TreeMap<String, Set<String>>();
		for (String load: gameData.loads.keySet()) {
			Set<String> cities = new HashSet<String>();
			for (City city:gameData.loads.get(load))
				cities.add(city.name);
			response.loadset.put(load, cities);
		}
		response.gid = gid;
		return response;
	}
	
	static public String buildNewGameResponse(String gid, GameData gameData) {
		// Build a JSON string that has gid, serialized map data, list of cities and loads
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Milepost.class, new MilepostSerializer());

		return gsonBuilder.serializeNulls().create().toJson(newGameResponse(gid, gameData));
	}
	
	static public String newGame(String requestText) throws GameException {			
		String gameId = null;
		Gson gson = new GsonBuilder().create();
		NewGameData data = gson.fromJson(requestText, NewGameData.class);
		
		GameData gameData = new GameData(data.gameType);
		if (data.ruleSet == null)
			data.ruleSet = new RuleSet(4, 70, 1);
		Game game = new Game(data.name, gameData, data.ruleSet);
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
	
	static public String joinGame(String requestText) throws GameException {
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

	static public String resumeGame(String requestText) throws GameException {
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
		game.getPlayer(data.pid);	// throws PLAYER_NOT_FOUND if player not in game
		log.info("resumeGame(pid={})", data.pid);
		
		NewGameResponse response = new NewGameResponse();
		response = newGameResponse(data.gid, game.gameData);
		
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Milepost.class, new MilepostSerializer());

		return gsonBuilder.serializeNulls().create().toJson(response);
	}

	static class StartGameData {
		public String gid;
		public String pid;
		boolean ready;
	}

	static public void startGame(String requestText) throws GameException {
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

	static public void testBuildTrack(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		BuildTrackData data = gson.fromJson(requestText, BuildTrackData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!game.testBuildTrack(data.pid, data.mileposts))
			throw new GameException(GameException.INVALID_TRACK);
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
		public int train;
		
		public UpgradeTrainData() {
			train = 0;
		}
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

	static public void placeTrain(String requestText) throws GameException {
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

	static public void testMoveTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		MoveTrainData data = gson.fromJson(requestText, MoveTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!game.testMoveTrain(data.pid, data.train, data.mileposts))
			throw new GameException(GameException.INVALID_MOVE);
	}

	static public void moveTrain(String requestText) throws GameException {
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

	static public void pickupLoad(String requestText) throws GameException {
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

	static public void deliverLoad(String requestText) throws GameException {
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

	static public void dumpLoad(String requestText) throws GameException {
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
	
	static public void turnInCards(String requestText) throws GameException {
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

	static public void endTurn(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		EndTurnData data = gson.fromJson(requestText, EndTurnData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.endTurn(data.pid);
	}

	static class ResignData {
		String gid;
		String pid;
	}
	
	static public void resignGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		ResignData data = gson.fromJson(requestText, ResignData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.resign(data.pid);
	}
	
	static class EndGame {
		public String gid;
		public String pid;
		public boolean ready;
	}

	/** Handle endGame */
	static public void endGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		EndGame data = gson.fromJson(requestText, EndGame.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.endGame(data.pid, data.ready);
		//if (game.endGame(data.pid, data.ready)) 
		//	games.remove(data.gid);		
	}
	
	/** Delete specified games */
	static public void removeOldGames(Predicate<Game> tester) {
		for(Iterator<Map.Entry<String, Game>> it = games.entrySet().iterator(); it.hasNext(); ) {
		      Map.Entry<String, Game> entry = it.next();
		      Game game = entry.getValue();
			if (tester.test(game)) {
				log.warn("Removing expired game {}", entry.getKey());
		        it.remove();
			}
		}
	}
	
	/** Delete expired games */
	static public void removeOldGames() {
		// Delete games that have ended, and are older than endedExpiration
		Date oldestEnded = new Date(System.currentTimeMillis() - endedExpiration);
		removeOldGames(game -> game.isOver() && game.lastChangeDate().before(oldestEnded));
		
		// Delete games that were never started and are older than notStartedExpiration
		Date oldestNotStarted = new Date(System.currentTimeMillis() - notStartedExpiration);
		removeOldGames(game -> game.isJoinable() && game.lastChangeDate().before(oldestNotStarted));
		
		// Delete games that have been abandoned -- last previous change is older than abandonedExpiration
		Date oldestPlayed = new Date(System.currentTimeMillis() - abandonedExpiration);
		removeOldGames(game -> game.lastChangeDate().before(oldestPlayed));
	}
	
}
