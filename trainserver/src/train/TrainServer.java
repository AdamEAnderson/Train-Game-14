package train;
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
import map.MilepostTypeAdapter;
import map.TrainMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.GlobalRail;
import player.Player;
import player.Rail;
import player.Stats;
import player.Train;
import player.TurnData;
import reference.Card;
import reference.City;
import reference.UpgradeType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
		public Card[] hand;
		public Stats stats;
		public Map<MilepostId, Set<MilepostId>> rail;
		PlayerStatus() {}
		PlayerStatus(Player p, Map<MilepostId, Set<MilepostId>> r) {
			pid = p.name;
			color = p.color;
			trains = p.getTrains();
			money = p.getMoney();
			stats = p.stats();
			hand = p.getCards();
			rail = r;
		}
	}
	
	static class GameStatus {
		public String gid;
		public TurnData turnData;
		public String lastid;
		public String geography;
		public boolean ended;
		public int turns;
		public List<PlayerStatus> players; //in turn order beginning with the active player
		public int transaction;
		GameStatus() {}
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
		gsonBuilder.registerTypeAdapter(Milepost.class, new MilepostTypeAdapter());
		GameStatus status = new GameStatus();
		status.gid = gid;
		status.players = new ArrayList<PlayerStatus>();
		status.geography = game.gameData.getGeography();
		status.transaction = game.transaction();
		status.turnData = game.getTurnData();
		status.ended = game.isOver();
		status.turns = game.getTurns();
		status.lastid = game.getLastPid();
		
		for(String pid : game.getPids()){
			PlayerStatus p = new PlayerStatus(game.getPlayer(pid), game.getGlobalRail().getRail(pid).getRail());
			status.players.add(p);
		}
		
		statusCache = gsonBuilder.serializeNulls().create().toJson(status);
		statusGid = gid;
		statusTransaction = game.transaction();
		
		return statusCache;
	}
	
	/** List all available game geographies (map boards)
	 * 
	 */
	static public String listGeographies(String requestText) throws GameException {		
		List<String> geographies = GameData.getGeographies();
		return new GsonBuilder().create().toJson(geographies);
	}
	
	/** Returns a list of the colors in use in a given game */
	static public String listColors(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		StatusRequest data = gson.fromJson(requestText, StatusRequest.class);
		String gid = data.gid;
		Game game = getGame(gid);
		if (game == null)
			return "{}";
		
		List<String> colors = new ArrayList<String>();
		for (Player p: game.getPlayers()) 
			colors.add(p.color);
		return new GsonBuilder().create().toJson(colors);
	}
	
	static class ListRequest {
		public String listType;
		ListRequest() {}
	}
	
	static class ListResponse {
		public Map<String, String> gidNames;
		ListResponse() { gidNames = new HashMap<String, String>(); }
	}
	
	/** List games that can be joined, or resumed.
	 * Games may be joined if they have been created but not yet started.
	 * Games may be resumed once they have started.
	 */
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
		response.mapData = gameData.getMap().getSerializeData();
		response.cities = gameData.getCities().values();
		response.geography = gameData.getGeography();
		// Convert from loads to set of cities to loads to set of city names
		response.loadset = new TreeMap<String, Set<String>>();
		for (String load: gameData.getLoads().keySet()) {
			Set<String> cities = new HashSet<String>();
			for (City city:gameData.getLoads().get(load))
				cities.add(city.name);
			response.loadset.put(load, cities);
		}
		response.gid = gid;
		return response;
	}
	
	static public String buildNewGameResponse(String gid, GameData gameData) {
		// Build a JSON string that has gid, serialized map data, list of cities and loads
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Milepost.class, new MilepostTypeAdapter());

		return gsonBuilder.serializeNulls().create().toJson(newGameResponse(gid, gameData));
	}
	
	/** Create a new game */
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
	
	/** Join a game */
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

	/** Resume a game
	 * Typically used when a player has dropped has dropped a connection to a game in progress,
	 * and wants to restart the client connection to the in progress game.
	 * @param requestText
	 * @return NewGameData, as a serialized JSON string
	 * @throws GameException if the requested doesn't exist, or the player wasn't part of the game
	 */
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
		
		return buildNewGameResponse(data.gid, game.gameData);
	}

	static class StartGameData {
		public String gid;
		public String pid;
		boolean ready;
	}

	/** Start playing the game.
	 * Game will order players, and start building turns. Once this is done, new players cannot join.
	 * @param requestText
	 * @throws GameException if game doesn't exist
	 */
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

	/** Query the server to see if the specified track can be built
	 * 
	 * @param requestText
	 * @throws GameException if the track cannot be buit, game or player is unknown
	 */
	static public void testBuildTrack(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		BuildTrackData data = gson.fromJson(requestText, BuildTrackData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!game.testBuildTrack(data.pid, data.mileposts))
			throw new GameException(GameException.INVALID_TRACK);
	}

	/** Build the track
	 * 
	 * @param requestText
	 * @throws GameException if the track cannot be buit, game or player is unknown
	 */
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

	/** Upgrade the player's train, either to go faster or to carry more loads
	 */
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

	/** Start the train at a location on the board.
	 * This is typically called at the start of a game, on the first regular (post-building) turn.
	 * If the train has already been placed, it cannot be placed again. A train must
	 * be placed before it can be moved.
	 * @param requestText
	 * @throws GameException
	 */
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
		public String rid; //rail id
		public int train;
		public MilepostId[] mileposts;
	}

	/** Check to see if the train can be moved through the set of mileposts.
	 * 
	 * @param requestText
	 * @throws GameException
	 */
	static public void testMoveTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		MoveTrainData data = gson.fromJson(requestText, MoveTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (game.testMoveTrain(data.pid, data.train, data.mileposts) == null)
			throw new GameException(GameException.INVALID_MOVE);
	}

	/** Move the train through a set of mileposts
	 * 
	 * @param requestText
	 * @throws GameException
	 */
	static public void moveTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		MoveTrainData data = gson.fromJson(requestText, MoveTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.moveTrain(data.pid, data.rid, data.train, data.mileposts);
	}

	static class PickupLoadData {
		public String gid;
		public String pid;
		public int train;
		public String load;
	}

	/** Pickup a load.
	 * Load must be available at the current train location.
	 * @param requestText
	 * @throws GameException
	 */
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

	/** Deliver a load
	 * Load must be requested on a card the player has, it must be on the player's train,
	 * and the train must be at the location that the load is for.
	 * @param requestText
	 * @throws GameException
	 */
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

	/** Remove a load from the train
	 * 
	 * @param requestText
	 * @throws GameException
	 */
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
	
	/** Replace all of a players cards
	 * Player may turn in all of their cards, and get a complete new set. Player may not move, 
	 * upgrade, or build track during the same turn.
	 * @param requestText
	 * @throws GameException
	 */
	static public void turnInCards(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		TurnInCardsData data = gson.fromJson(requestText, TurnInCardsData.class);
		Game game = games.get(data.gid);
		if(game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.turnInCards(data.pid);
	}
	
	static class UndoData {
		public String gid;
		public String pid;
	}

	/** Undo the previous action
	 * Player may undo previous action they did this turn. Player may keep undoing to 
	 * undo multiple actions. Player may not undo actions taken by other players, or
	 * their own actions during previous turns.
	 * @param requestText
	 * @throws GameException
	 */
	static public void undo(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		UndoData data = gson.fromJson(requestText, UndoData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (game.getActivePlayer() == null)
			throw new GameException(GameException.NOTHING_TO_UNDO);
		if (!data.pid.equals(game.getActivePlayer().name))
			throw new GameException(GameException.PLAYER_NOT_ACTIVE);
		Game newGame = game.undo();
		if (newGame != null)
			games.replace(data.gid, newGame);
	}
	
	/** Redo the previous action
	 * Player may redo if the previous action was undone. Player may keep redoing to 
	 * redo multiple actions. Player may not redo actions taken by other players, or
	 * their own actions during previous turns.
	 * @param requestText
	 * @throws GameException
	 */
	static public void redo(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		UndoData data = gson.fromJson(requestText, UndoData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (game.getActivePlayer() == null)
			throw new GameException(GameException.NOTHING_TO_REDO);
		if (!data.pid.equals(game.getActivePlayer().name))
			throw new GameException(GameException.PLAYER_NOT_ACTIVE);
		Game newGame = game.redo();
		if (newGame != null)
			games.replace(data.gid, newGame);
	}
	
	static class EndTurnData {
		public String gid;
		public String pid;
	}

	/* Player declares their turn is over, and control goes to the next player
	 * 
	 */
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
	
	/** Player is quitting the game
	 * 
	 * @param requestText
	 * @throws GameException
	 */
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

	/** Player declares they are ready to end the game. 
	 * When all players are ready to end, the game will be over.
	 */
	static public void endGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		EndGame data = gson.fromJson(requestText, EndGame.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.endGame(data.pid, data.ready);
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
