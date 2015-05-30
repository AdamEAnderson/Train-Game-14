package train;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.ReferenceCountUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;

import map.Milepost;
import map.MilepostId;
import map.MilepostTypeAdapter;
import map.TrainMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.GlobalRail;
import player.GlobalTrack;
import player.GlobalTrackTypeAdapter;
import player.Player;
import player.Stats;
import player.Train;
import player.TurnData;
import reference.Card;
import reference.City;
import reference.UpgradeType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class GameGCTask extends TimerTask {
	TrainServer trainServer;
	
	GameGCTask(TrainServer trainServer) {
		this.trainServer = trainServer;
	}
	
	@Override
	public void run() {
		trainServer.removeOldGames();
	}
}

class GameGC  {

	private Timer timer;
	private GameGCTask task;
	
	GameGC(TrainServer trainServer, long interval) {
		timer = new Timer();
		task = new GameGCTask(trainServer);
		timer.schedule(task, interval, interval);
	}
	
	void stop() { task.cancel(); }
}
	
/** Maps incoming data from JSON strings into calls on a specific game. Maintains the list 
 * of in progress games.
 */
public class TrainServer  implements Runnable {
	private final static String NEW_GAME = "newGame";
	private final static String JOIN_GAME = "joinGame";
	private final static String RESUME_GAME = "resumeGame";
	private final static String START_GAME = "startGame";
	private final static String BUILD_TRACK = "buildTrack";
	private final static String TEST_BUILD_TRACK = "testBuildTrack";
	private final static String UPGRADE_TRAIN = "upgradeTrain";
	private final static String PLACE_TRAIN = "placeTrain";
	private final static String PICKUP_LOAD = "pickupLoad";
	private final static String DELIVER_LOAD = "deliverLoad";
	private final static String DUMP_LOAD = "dumpLoad";
	private final static String TEST_MOVE_TRAIN = "testMoveTrain";
	private final static String MOVE_TRAIN = "moveTrain";
	private final static String TURN_IN_CARDS = "turnInCards";
	private final static String UNDO = "undo";
	private final static String REDO = "redo";
	private final static String END_TURN = "endTurn";
	private final static String END_GAME = "endGame";
	private final static String RESIGN_GAME = "resignGame";
	
	private final static String LIST = "list";
	private final static String STATUS = "status";
	private final static String LIST_COLORS = "listColors";
	private final static String LIST_GEOGRAPHIES = "listGeographies";
	private final static String START_RECORDING = "startRecording";
	private final static String END_RECORDING = "endRecording";
	private final static String PLAY_RECORDING = "playRecording";
	
	private final static String REMAP_GID = "RemapGID"; // part of hack for recording
	private Logger log = LoggerFactory.getLogger(TrainServer.class);
	
	private FileOutputStream recordingStream = null;
	private String statusCache = null;
	private int statusTransaction = 0;	// transaction that was current when statusCache was created
	private String statusGid = null;		// GID used for generating statusCache

	private long hourMilli = 3600000L;		// Number of milliseconds in one hour
	private long fortnightMilli = 1209600000L; // Number of milliseconds in 14 days
	private long endedExpiration = hourMilli;// Number of milliseconds before a game that has ended will be removed
	private long notStartedExpiration = hourMilli;// Number of milliseconds before a game was never started will be removed
	private long abandonedExpiration = fortnightMilli;// Number of milliseconds since last change before a game will be removed
	
	private GameGC gameGC = new GameGC(this, hourMilli);		// garbage collect old games
	private RandomString gameNamer = new RandomString(8); // use for
																	// generating
																	// (semi)unique
																	// gameIds

	private BlockingQueue<TrainMessage> messageQueue = new ArrayBlockingQueue<TrainMessage>(1000);
	Map<String, Game> games = new HashMap<String, Game>(); // games currently in progress;
	private boolean isStopped = false;		// when true, stop handling new messages
	private Thread messageHandler;
	
	private static class TrainMessage {
		ChannelHandlerContext ctx;
		HttpObject httpMessage;
		String jsonMessage;
		HttpTrainServerHandler handler;
		
		public TrainMessage(HttpTrainServerHandler handler, ChannelHandlerContext ctx, Object httpMessage, String jsonMessage) {
			this.ctx = ctx;
			ReferenceCountUtil.retain(httpMessage);
			this.httpMessage = (HttpObject)httpMessage;
			this.jsonMessage = jsonMessage;
			this.handler = handler;
		}
	}

	public TrainServer() {
		autoRecord(false);

		// start the message loop
		messageHandler = new Thread(this);
		messageHandler.start();
	}
		
	public TrainServer(boolean recordOn) {
		autoRecord(recordOn);
		
		// start the message loop
		messageHandler = new Thread(this);
		messageHandler.start();
	}
		
	private void autoRecord(boolean recordOn) {
		if (recordOn)  {
			try {
				String fileName = "record" + getDateAsString();
				startRecording("{\"messageType\":\"" + START_RECORDING + "\", \"file\":\"" + fileName + "\"}");
			} catch (GameException e) {
				System.out.println("Cannot record game:" + e.getMessage());
			}
		}
	}
	
	public void stop() {
		isStopped = true;
		try {
			messageHandler.join();
			gameGC.stop();
		} catch (InterruptedException e) {
            Thread.currentThread().interrupt();
		}		
	}
	
	public void run() {
		while (!isStopped) {
			try {
				TrainMessage message = (TrainMessage) messageQueue.take();
				handleMessage(message);
				ReferenceCountUtil.release(message.httpMessage);
			} catch (InterruptedException e) {
	             Thread.currentThread().interrupt();
			}
		}
	}
	
	public Game getGame(String gid) {
		return games.get(gid);		
	}
	
	private void handleMessage(TrainMessage message) {
		String requestType = parseMessageType(message.jsonMessage);
		try {
			switch (requestType) {
				case START_RECORDING:
					startRecording(message.jsonMessage);
					break;
				case END_RECORDING:
					endRecording();
					break;
				case PLAY_RECORDING:
					playRecording(message.jsonMessage);
					break;
				default: 
					message.handler.appendToResponse(executeMessage(message.jsonMessage));
					break;
			}
			if (!message.handler.writeResponse(message.httpMessage, message.ctx)) {
				// If keep-alive is off, close the connection once the
				// content is fully written.
				message.ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
						ChannelFutureListener.CLOSE);
			}
		} catch (GameException e) {
			String errorString = e.getMessage();
			log.error("Game exception {}", errorString);
			Gson gson = new Gson();
			String jsonError = gson.toJson(errorString);
			message.handler.sendError(jsonError, message.ctx);
		}
		message.ctx.flush();
	}
	
	public String executeMessage(String message) throws GameException {
		String requestType = parseMessageType(message);
		String response = null;
		switch (requestType) {
			case NEW_GAME:
				response = newGame(message);
				if (recordingStream != null) {
			        String gid = response.substring(8, 16);
			        String annotation = "{\"" + REMAP_GID + "\":\"" + gid + "\"}";
			        try {
						recordingStream.write(annotation.getBytes());
						recordingStream.write('\n');
					} catch (IOException e) {
						log.error("Error writing to redirect file {}", e.getMessage());
					}
				}
				break;
			case JOIN_GAME:
				response = joinGame(message);
				break;
			case RESUME_GAME:
				response = resumeGame(message);
				break;
			case START_GAME:
				startGame(message);
				break;
			case TEST_BUILD_TRACK:
				testBuildTrack(message);
				break;
			case TEST_MOVE_TRAIN:
				testMoveTrain(message);
				break;

			case BUILD_TRACK:
				buildTrack(message, true);
				break;
			case UPGRADE_TRAIN:
				upgradeTrain(message, true);
				break;
			case PLACE_TRAIN:
				placeTrain(message, true);
				break;
			case MOVE_TRAIN:
				moveTrain(message, true);
				break;
			case PICKUP_LOAD:
				pickupLoad(message, true);
				break;
			case DELIVER_LOAD:
				deliverLoad(message, true);
				break;
			case DUMP_LOAD:
				dumpLoad(message, true);
				break;
			case TURN_IN_CARDS:
				turnInCards(message, true);
				break;
			case END_TURN:
				endTurn(message, true);
				break;

			case UNDO:
				undo(message);
				break;
			case REDO:
				redo(message);
				break;
			case END_GAME:
				if (recordingStream != null)		// for testing
					endRecording();
				endGame(message);
				break;
			case RESIGN_GAME:
				resignGame(message);
				break;
			case LIST:
				response = list(message);
				break;
			case STATUS:
				response = status(message);
				break;
			case LIST_COLORS:
				response = listColors(message);
				break;
			case LIST_GEOGRAPHIES:
				response = listGeographies(message);
				break;
			default:
				throw new GameException(GameException.INVALID_MESSAGE_TYPE);
		}
		if (response != null)
			log.debug("newGame response {}", response);

		return response;
	}
	
	
	/** TEST ONLY! */
	public void resetExpirations(long ended, long notStarted, long abandoned) {
		endedExpiration = ended;
		notStartedExpiration = notStarted;
		abandonedExpiration = abandoned;
		gameGC.stop();
		gameGC = new GameGC(this, endedExpiration);
	}
	
	/** TEST ONLY! */
	public void resetExpirations() {
		endedExpiration = hourMilli;
		notStartedExpiration = hourMilli;
		abandonedExpiration = fortnightMilli;
		gameGC.stop();
		gameGC = new GameGC(this, endedExpiration);
	}
	
	
	// For a given game, return its gameId, or null if not found
	public String getGameId(Game game) {
		for (Map.Entry<String, Game> entry: games.entrySet()) {
			if (entry.getValue() == game)
				return entry.getKey();
		}
		return null;
	}
	
	class PlayerStatus {
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
	
	class GameStatus {
		public String gid;
		public TurnData turnData;
		public String lastid;
		public String geography;
		public boolean ended;
		public int turns;
		public GlobalRail globalRail;
		public List<PlayerStatus> players; //in turn order beginning with the active player
		public int transaction;
		GameStatus() {}
	}
	
	class StatusRequest {
		public String gid;
		StatusRequest() {}
	}
	
	public String status(String requestText) throws GameException {
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
		gsonBuilder.registerTypeAdapter(GlobalTrack.class, new GlobalTrackTypeAdapter());
		GameStatus status = new GameStatus();
		status.gid = gid;
		status.players = new ArrayList<PlayerStatus>();
		status.geography = game.gameData.getGeography();
		status.transaction = game.transaction();
		status.turnData = game.getTurnData();
		status.ended = game.isOver();
		status.turns = game.getTurns();
		status.lastid = game.getLastPid();
		status.globalRail = game.getGlobalRail();
		
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
	public String listGeographies(String requestText) throws GameException {		
		List<String> geographies = GameData.getGeographies();
		return new GsonBuilder().create().toJson(geographies);
	}
	
	/** Returns a list of the colors in use in a given game */
	public String listColors(String requestText) throws GameException {
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
	
	class ListRequest {
		public String listType;
		ListRequest() {}
	}
	
	class ListResponse {
		public Map<String, String> gidNames;
		ListResponse() { gidNames = new HashMap<String, String>(); }
	}
	
	/** List games that can be joined, or resumed.
	 * Games may be joined if they have been created but not yet started.
	 * Games may be resumed once they have started.
	 */
	public String list(String requestText) throws GameException {
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
	
	class NewGameData {
		//public String messageType;
		public String pid; // host playerId
		public String color; // color for track building
		public RuleSet ruleSet; // name for rules of the game
		public String gameType; // which game (Africa, Eurasia, etc.)
		public String name;	// display (human readable) name of game
		
		NewGameData() {}
	}
	
	class NewGameResponse {
		public String gid;
		public String geography;
		public TrainMap.SerializeData mapData;
		public Collection<City> cities;	/** Cities indexed by city name, contains loads found in each city */
		public Map<String, Set<String>> loadset; /** Key=load, Value= cities where loads can be obtained */
		NewGameResponse() {}
	}
	
	private NewGameResponse newGameResponse(String gid, GameData gameData){
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
	
	public String buildNewGameResponse(String gid, GameData gameData) {
		// Build a JSON string that has gid, serialized map data, list of cities and loads
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Milepost.class, new MilepostTypeAdapter());

		String s = gsonBuilder.serializeNulls().create().toJson(newGameResponse(gid, gameData));
		return s;
	}
	
	/** Create a new game */
	public String newGame(String requestText) throws GameException {			
		String gameId = null;
		Gson gson = new GsonBuilder().create();
		NewGameData data = gson.fromJson(requestText, NewGameData.class);
		
		GameData gameData = new GameData(data.gameType);
		if (data.ruleSet == null)
			data.ruleSet = new RuleSet(4, 70, 1, false, true);
		Game game = new Game(data.name, gameData, data.ruleSet);
		gameId = gameNamer.nextString();
		games.put(gameId, game);
		game.joinGame(data.pid, data.color);
		return buildNewGameResponse(gameId, gameData);
	}

	class JoinGameData {
		public String gid;
		public String pid;
		public String color;
		}
	
	/** Join a game */
	public String joinGame(String requestText) throws GameException {
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
	public String resumeGame(String requestText) throws GameException {
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

	class StartGameData {
		public String gid;
		public String pid;
		boolean ready;
	}

	/** Start playing the game.
	 * Game will order players, and start building turns. Once this is done, new players cannot join.
	 * @param requestText
	 * @throws GameException if game doesn't exist
	 */
	public void startGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		StartGameData data = gson.fromJson(requestText, StartGameData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.startGame(data.pid, data.ready);
	}

	class BuildTrackData {
		public String gid;
		public String pid;
		public MilepostId[] mileposts;
	}

	/** Query the server to see if the specified track can be built
	 * 
	 * @param requestText
	 * @throws GameException if the track cannot be buit, game or player is unknown
	 */
	public void testBuildTrack(String requestText) throws GameException {
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
	public void buildTrack(String requestText, boolean immediateExecution) throws GameException {
		Gson gson = new GsonBuilder().create();
		BuildTrackData data = gson.fromJson(requestText, BuildTrackData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (data.mileposts.length > 0) {
			if (!queueRequest(game, data.pid, requestText)) {
				if (!immediateExecution)
					runQueuedMessages(game, data.pid);
				game.buildTrack(data.pid, data.mileposts);
			}
		}
	}

	class UpgradeTrainData {
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
	public void upgradeTrain(String requestText, boolean immediateExecution) throws GameException {
		Gson gson = new GsonBuilder().create();
		UpgradeTrainData data = gson.fromJson(requestText,
				UpgradeTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!data.upgradeType.equals("Capacity") && !data.upgradeType.equals("Speed"))
			throw new GameException(GameException.INVALID_UPGRADE);
		if (!queueRequest(game, data.pid, requestText)) {
			if (!immediateExecution)
				runQueuedMessages(game, data.pid);
			game.upgradeTrain(data.pid, data.train,
					data.upgradeType.equals("Capacity") ? UpgradeType.CAPACITY
							: UpgradeType.SPEED);
		}
	}

	class PlaceTrainData {
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
	public void placeTrain(String requestText, boolean immediateExecution) throws GameException {
		Gson gson = new GsonBuilder().create();
		PlaceTrainData data = gson.fromJson(requestText, PlaceTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!queueRequest(game, data.pid, requestText)) {
			if (!immediateExecution)
				runQueuedMessages(game, data.pid);
			game.placeTrain(data.pid, data.train, data.where);
		}
	}

	class MoveTrainData {
		public String gid;
		public String pid;
		public int train;
		public MilepostId[] mileposts;
	}

	/** Check to see if the train can be moved through the set of mileposts.
	 * 
	 * @param requestText
	 * @throws GameException
	 */
	public void testMoveTrain(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		MoveTrainData data = gson.fromJson(requestText, MoveTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.testMoveTrain(data.pid, data.train, data.mileposts);
	}

	/** Move the train through a set of mileposts
	 * 
	 * @param requestText
	 * @throws GameException
	 */
	public void moveTrain(String requestText, boolean immediateExecution) throws GameException {
		Gson gson = new GsonBuilder().create();
		MoveTrainData data = gson.fromJson(requestText, MoveTrainData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!queueRequest(game, data.pid, requestText)) {
			if (!immediateExecution)
				runQueuedMessages(game, data.pid);
			game.moveTrain(data.pid, data.train, data.mileposts);
		}
	}

	class PickupLoadData {
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
	public void pickupLoad(String requestText, boolean immediateExecution) throws GameException {
		Gson gson = new GsonBuilder().create();
		PickupLoadData data = gson.fromJson(requestText, PickupLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!queueRequest(game, data.pid, requestText)) {
			if (!immediateExecution)
				runQueuedMessages(game, data.pid);
			game.pickupLoad(data.pid, data.train, data.load);
		}
	}

	class DeliverLoadData {
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
	public void deliverLoad(String requestText, boolean immediateExecution) throws GameException {
		Gson gson = new GsonBuilder().create();
		DeliverLoadData data = gson
				.fromJson(requestText, DeliverLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!queueRequest(game, data.pid, requestText)) {
			if (!immediateExecution)
				runQueuedMessages(game, data.pid);
			game.deliverLoad(data.pid, data.train, data.load, data.card);
		}
	}

	class DumpLoadData {
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
	public void dumpLoad(String requestText, boolean immediateExecution) throws GameException {
		Gson gson = new GsonBuilder().create();
		DumpLoadData data = gson.fromJson(requestText, DumpLoadData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!queueRequest(game, data.pid, requestText)) {
			if (!immediateExecution)
				runQueuedMessages(game, data.pid);
			game.dumpLoad(data.pid, data.train, data.load);
		}
	}

	class TurnInCardsData{
		public String gid;
		public String pid;
	}
	
	/** Replace all of a players cards
	 * Player may turn in all of their cards, and get a complete new set. Player may not move, 
	 * upgrade, or build track during the same turn.
	 * @param requestText
	 * @throws GameException
	 */
	public void turnInCards(String requestText, boolean immediateExecution) throws GameException {
		Gson gson = new GsonBuilder().create();
		TurnInCardsData data = gson.fromJson(requestText, TurnInCardsData.class);
		Game game = games.get(data.gid);
		if(game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!queueRequest(game, data.pid, requestText)) {
			if (!immediateExecution)
				runQueuedMessages(game, data.pid);
			game.turnInCards(data.pid);
		}
	}
	
	class UndoData {
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
	public void undo(String requestText) throws GameException {
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
	public void redo(String requestText) throws GameException {
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
	
	class EndTurnData {
		public String gid;
		public String pid;
	}

	/* Player declares their turn is over, and control goes to the next player
	 * 
	 */
	public void endTurn(String requestText, boolean immediateExecution) throws GameException {
		Gson gson = new GsonBuilder().create();
		EndTurnData data = gson.fromJson(requestText, EndTurnData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		if (!queueRequest(game, data.pid, requestText)) {
			if (!immediateExecution)
				runQueuedMessages(game, data.pid);
			game.endTurn(data.pid);
		}
	}

	class ResignData {
		String gid;
		String pid;
	}
	
	/** Player is quitting the game
	 * 
	 * @param requestText
	 * @throws GameException
	 */
	public void resignGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		ResignData data = gson.fromJson(requestText, ResignData.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.resign(data.pid);
	}
	
	class EndGame {
		public String gid;
		public String pid;
		public boolean ready;
	}

	/** Player declares they are ready to end the game. 
	 * When all players are ready to end, the game will be over.
	 */
	public void endGame(String requestText) throws GameException {
		Gson gson = new GsonBuilder().create();
		EndGame data = gson.fromJson(requestText, EndGame.class);
		Game game = games.get(data.gid);
		if (game == null)
			throw new GameException(GameException.GAME_NOT_FOUND);
		game.endGame(data.pid, data.ready);
	}
	
	class Redirect {
		public String file;
	}

	public void startRecording(String requestText) throws GameException {
		if (recordingStream != null)
			throw new GameException(GameException.ALREADY_RECORDING);

		log.info("startRecording: {}", requestText);
		Gson gson = new GsonBuilder().create();
		Redirect data = gson.fromJson(requestText, Redirect.class);
		try {
			recordingStream = new FileOutputStream(data.file);
		} catch (FileNotFoundException e) {
			throw new GameException(GameException.CANNOT_SAVE_FILE);
		}
	}
		
	private void endRecording() throws GameException {
		if (recordingStream == null)
			return;
		try {
			recordingStream.close();
			recordingStream = null;
		} catch (IOException e) {
			throw new GameException(GameException.CANNOT_SAVE_FILE);
		}
	}
	
	public void playRecording(String requestText) throws GameException {
		log.info("playRecording: {}", requestText);
		Gson gson = new GsonBuilder().create();
		Redirect data = gson.fromJson(requestText, Redirect.class);
		playRecordingFromFile(data.file);
	}

	public void playRecordingFromFile(String fileName) throws GameException {
		try {
			Path path = FileSystems.getDefault().getPath(fileName);
			Charset charset = Charset.forName("US-ASCII");
			BufferedReader reader = Files.newBufferedReader(path, charset);
			String line = reader.readLine();
			String gid = null;
			Map<String, String> gidMap = new HashMap<String, String>();
			while (line != null) {
				// Following is a hack for remapping gids from old values to new
				int gidIndex = line.indexOf("\"gid\":");
				if (gidIndex >= 0) {
			        String oldgid = line.substring(gidIndex + 7, gidIndex + 15);
			        String newgid = gidMap.get(oldgid);
			        if (newgid != null)
			        	line = line.substring(0, gidIndex + 7) + newgid + line.substring(gidIndex + 15);
				}
				if (line.contains(REMAP_GID)) {
					int remapIndex = line.indexOf(REMAP_GID);
					String oldGid = line.substring(remapIndex + 11, remapIndex + 19);
					gidMap.put(oldGid, gid);
				}
				else {
  					String response = executeMessage(line);
					if (line.contains(NEW_GAME)) 
				        gid = response.substring(8, 16);
				}
				line = reader.readLine();
			}
		} catch (FileNotFoundException e) {
			throw new GameException(GameException.CANNOT_OPEN_FILE);
		} catch (IOException e) {
			throw new GameException(GameException.CANNOT_OPEN_FILE);
		}
		
	}
	
	/** Delete specified games */
	public void removeOldGames(Predicate<Game> tester) {
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
	public void removeOldGames() {
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
	
	
	private boolean queueRequest(Game game, String pid, String requestText) throws GameException {
		if (!game.isActivePlayer(pid) && game.getRuleSet().playAhead) {
			game.queueRequest(pid, requestText);
			return true;
		}
		return false;
	}

	private void runQueuedMessages(Game game, String pid) throws GameException {
		if (!game.isActivePlayer(pid)) 
			return;
		
		String request = null;
		while (true) {
			request = game.getQueuedRequest(pid);
			if (request == null)
				break;
			try {
				executeMessage(request);
			} catch (GameException e) {
				game.getPlayer(pid).clearRequestQueue();
				throw e;
			}
		} 
	}

	
	public void addMessage(HttpTrainServerHandler handler, ChannelHandlerContext ctx, Object message, String requestText) {
		if (recordingStream != null && !requestText.contains(END_RECORDING)) {
			try {
				//redirectStream.write(message.getBytes());
				//redirectStream.write('\t');
				recordingStream.write(requestText.getBytes());
				recordingStream.write('\n');
			} catch (IOException e) {
				log.error("Error writing to redirect file {}", e.getMessage());
			}
		}
		messageQueue.add(new TrainMessage(handler, ctx, message, requestText));
	}
	
	private static int findNthExprInString(String s, String expr, int n)
	{
		int index = -1;
		int findCount = 0;
		while (findCount < n)
		{
			index = s.indexOf(expr, index + 1);
			if (index == -1)
				break;	// expr not found
			++findCount;
		}
		return index;
	}
	
	// Custom parsing of the messageType, so we can dispatch
	private static String parseMessageType(String requestText) {
		// String extends from the 3rd to the fourth 4th quote
		int startIndex = findNthExprInString(requestText, "\"", 3);
		if (startIndex < 0 || startIndex + 1 >= requestText.length())
			return "badMessage";
		int endIndex = findNthExprInString(requestText, "\"", 4);
		if (endIndex < 0 || endIndex >= requestText.length())
			return "badMessage";
		return requestText.substring(startIndex + 1, endIndex);
	}

	private String getDateAsString() {
		// Create an instance of SimpleDateFormat used for formatting 
		// the string representation of date (month/day/year)
		DateFormat df = new SimpleDateFormat("MM_dd_yyyy_HH:mm:ss");

		// Get the date today using Calendar object.
		Date today = Calendar.getInstance().getTime();        
		// Using DateFormat format method we can create a string 
		// representation of a date with the defined format.
		String reportDate = df.format(today);

		// Print what date is today!
		System.out.println("Report Date: " + reportDate);
		return reportDate;
	}
	


}
