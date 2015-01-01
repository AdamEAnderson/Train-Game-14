package train;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import map.Milepost;
import map.MilepostId;
import map.MilepostShortFormTypeAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import player.GlobalRail;
import player.Player;
import player.TurnData;
import reference.Card;
import reference.UpgradeType;


// Train game implementation class.
public class Game implements AbstractGame {

	public transient GameData gameData;
	private transient RuleSet ruleSet;
	
	//joinable can be represented by "turnData == null"
	private boolean ended; // game has ended
	private int turns; //the number of completed turns; 0, 1, and 2 are building turns

	//new structures
	private TurnData turnData; //data for the active player
	private int activeIndex;  //index of the active player in the pids list
	private List<String> pids; //list of the player-ids in order
	private Map<String, Player> players; //maps pid to player
	private GlobalRail globalRail; //all of the track drawn for this game, organized by pid
	
	//useful for undo and deletion
	private transient int transaction;
	private transient Date lastChange;
	private String name;
	private transient UndoRedoStack undoStack;
	private transient UndoRedoStack redoStack;
		
	private static Logger log = LoggerFactory.getLogger(Game.class);
	
	/** Default constructor for gson */
	public Game() {
		undoStack = new UndoRedoStack(GameException.NOTHING_TO_UNDO);
		redoStack = new UndoRedoStack(GameException.NOTHING_TO_REDO);
	}
	
	/** Constructor. 
	 * @param map
	 * @param ruleSet
	 */
	public Game(String name, GameData gameData, RuleSet ruleSet){
		this.gameData = gameData;
		this.ruleSet = ruleSet;
		this.name = name;
		
		//we don't construct a TurnData until the game starts 
		pids = new ArrayList<String>();
		players = new HashMap<String, Player>();
		globalRail = new GlobalRail(name);
		
		turns = 0;
		ended = false;
		transaction = 1;
		lastChange = new Date();
		undoStack = new UndoRedoStack(GameException.NOTHING_TO_UNDO);
		redoStack = new UndoRedoStack(GameException.NOTHING_TO_REDO);
	}
	
	@Override
	public void joinGame(String pid, String color) throws GameException {
		log.info("joinGame(pid={}, color={})", pid, color);		
		for (String oid : pids) {
			if (oid.equals(pid))
				throw new GameException(GameException.PLAYER_ALREADY_JOINED);
		}
		for(Player p : players.values()){
			if (p.color.equals(color))
				throw new GameException(GameException.COLOR_NOT_AVAILABLE);
		}

		pids.add(pid);
		
		Card[] cards = new Card[ruleSet.handSize];
		for(int i = 0; i < cards.length; i++){
			cards[i] = dealCard();
		}
		Player p = new Player(ruleSet, pid, color, cards, this);
		players.put(pid, p);
		globalRail.join(pid);
		registerTransaction();
	}

	@Override
	public boolean startGame(String pid, boolean ready) throws GameException {
		log.info("startGame(pid={}, ready={})", pid, ready);
		Player p = getPlayer(pid);
		p.readyToStart(ready);
		
		boolean start = true;
		for (Player player: players.values())
			if (!player.readyToStart())
				start = false;
		
		if (start) {	// All players are ready to start - start the game
			log.info("Starting game");
			
			// Turn starts with player with the highest cards
			// Reorder players list so it matches playering order
			String startid = pids.get(0);
			for (int i = 1; i < pids.size(); i++){
				if (players.get(startid).getMaxTrip() < players.get(pids.get(i)).getMaxTrip()) 
					startid = pids.get(i);
			}

			// Add all the players to the new players list, starting with the first player
			List<String> newPids = new ArrayList<String>();
			newPids.add(startid);
			for (String oid: pids){
				if (oid != startid)
					newPids.add(oid);
			}
			pids = newPids;
			turnData = new TurnData(name, ruleSet, startid);
			registerTransaction();
		}
		return start;
	}

	@Override
	public boolean testBuildTrack(String pid, MilepostId[] mileposts)
			throws GameException {
		log.info("testBuildTrack(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
		
		checkActive(pid);
		Milepost[] mps = convert(mileposts);
		int cost = globalRail.checkBuild(pid, mps);
		return ((cost != -1) && turnData.checkSpending(cost));
	}

	@Override
	public void buildTrack(String pid, MilepostId[] mileposts)
			throws GameException {
		log.info("testBuildTrack(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
		checkActive(pid);
		if(!testBuildTrack(pid, mileposts)){
			throw new GameException("Invalid Track");
		}
		String originalGameState = toString();
		turnData.startTurn();
		Milepost[] mps = convert(mileposts);
		int cost = globalRail.checkBuild(pid, mps);
		turnData.spend(cost);
		globalRail.build(pid, mps);
		registerTransaction(originalGameState);
	}

	@Override
	public void upgradeTrain(String pid, int train, UpgradeType upgrade)
			throws GameException {
		log.info("upgradeTrain(pid={}, train={}, upgradeType={})", pid, upgrade, train);
		checkActive(pid);
		String originalGameState = toString();
		turnData.startTurn();
		if(!turnData.checkSpending(20)) throw new GameException("ExceededAllowance");
		if(!getActivePlayer().testUpgradeTrain(train, upgrade)) throw new GameException("InvalidUpgrade");

		turnData.upgrade(train, upgrade);
		registerTransaction(originalGameState);
		turnData.startTurn();
	}

	@Override
	public void placeTrain(String pid, int train, MilepostId where)
			throws GameException {
		log.info("placeTrain(pid={}, train={}, where={})", pid, train, where);
		checkActive(pid);
		checkBuilding();
		String originalGameState = toString();
		turnData.startTurn();
		getPlayer(pid).placeTrain(gameData.getMap().getMilepost(where), train);
		registerTransaction(originalGameState);
	}

	private boolean testMoveTrain(String pid, String rid, int train, MilepostId[] mileposts) 
			throws GameException{
		Player p = getPlayer(pid);
		if(p.getTrain(train) == null || p.getTrain(train).getLocation() == null) return false;
		MilepostId[] mps = new MilepostId[mileposts.length + 1];
		
		mps[0] = p.getTrain(train).getLocation().id;
		for(int i = 0; i < mileposts.length; i++){ 
			mps[i + 1] = mileposts[i];
		}
		if(! globalRail.testMove(rid, mps)) return false;
		
		int max = p.getMaxSpeed(train);
		if(turnData.hasFerried()) max /= 2;
		if(!turnData.checkMovesLength(train, mileposts.length, max)) return false;
		return true;
	}
	
	@Override
	public String testMoveTrain(String pid, int train,
			MilepostId[] mileposts) throws GameException {
		String rid = globalRail.getPlayer(mileposts[0], mileposts[1]);
		boolean ferry = gameData.getMilepost(mileposts[0]).isNeighborByFerry(mileposts[1]);
		if(rid == null) return null;
		if(testMoveTrain(pid, rid, train, mileposts)) return rid;
		return null;
	}

	@Override
	public void moveTrain(String pid, String rid, int train,
			MilepostId[] mileposts) throws GameException {
		log.info("moveTrain(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
		checkActive(pid);
		checkBuilding();
		int maxMoves = getPlayer(pid).getMaxSpeed(train);
		if(!testMoveTrain(pid, rid, train, mileposts)){
			throw new GameException("InvalidMove");
		}
		
		String originalGameState = toString();
		turnData.startTurn();
		
		getPlayer(pid).moveTrain(train, gameData.getMilepost(mileposts[mileposts.length - 1]), getPlayer(rid));
		turnData.move(train, mileposts.length, maxMoves);
		registerTransaction(originalGameState);
	}

	@Override
	public void pickupLoad(String pid, int train, String load)
			throws GameException {
		log.info("pickupLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		checkBuilding();
		String originalGameState = toString();
		turnData.startTurn();
		getActivePlayer().pickupLoad(train, load);
		registerTransaction(originalGameState);
	}

	@Override
	public void deliverLoad(String pid, int train, String load, int card)
			throws GameException {
		log.info("deliverLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		checkBuilding();
		String originalGameState = toString();
		turnData.startTurn();
		int deposit = getActivePlayer().deliverLoad(card, train, dealCard());
		turnData.deliver(deposit);
		registerTransaction(originalGameState);
	}

	@Override
	public void dumpLoad(String pid, int train, String load)
			throws GameException {
		log.info("dumpLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		String originalGameState = toString();
		turnData.startTurn();
		getActivePlayer().dropLoad(train, load);
		registerTransaction(originalGameState);
		
	}

	@Override
	public void turnInCards(String pid) throws GameException {
		log.info("turnInCards requestText: pid={}", pid);
		checkActive(pid);
		if(turnData.turnInProgress()) 
			throw new GameException("TurnAlreadyStarted");
		
		String origState = toString();

		Card[] cards = new Card[ruleSet.handSize];
		for(int i = 0; i < cards.length; i++){
			cards[i] = dealCard();
		}
		getActivePlayer().turnInCards(cards);
		endTurn(pid);
		registerTransaction(origState);
	}
	
	/*
	@Override
	public void buildTrack(String pid, MilepostId[] milepostIds) throws GameException {
		log.info("buildTrack(pid={}, length={}, mileposts=[", pid, milepostIds.length);
		for (int i = 0; i < milepostIds.length; ++i)
			log.info("{}, ", milepostIds[i]);
		log.info("])");
		checkActive(pid);
		String originalGameState = toString();

		Milepost[] mileposts = new Milepost[milepostIds.length];
		for(int i = 0; i < mileposts.length; i++){
			mileposts[i] = gameData.getMap().getMilepost(milepostIds[i]);
		}
		active.buildTrack(mileposts);	
		registerTransaction(originalGameState);
	}

	@Override
	public boolean testMoveTrain(String pid, int train, MilepostId[] mileposts)
			throws GameException {
		log.info("moveTrain(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
		checkActive(pid);
		checkBuilding();
		Milepost[] mps = new Milepost[mileposts.length + 1];
		mps[0] = active.getTrains()[train].getLocation();
		for(int i = 0; i < mileposts.length; i++){
			mps[i + 1] = gameData.getMap().getMilepost(mileposts[i]);
		}
		return active.testMoveTrain(train, mps);
	}
	
	@Override
	public void moveTrain(String pid, int train, MilepostId[] mileposts)
			throws GameException {
		log.info("moveTrain(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
		checkActive(pid);
		checkBuilding();
		String originalGameState = toString();

		Milepost[] mps = new Milepost[mileposts.length + 1];
		mps[0] = active.getTrains()[train].getLocation();
		for(int i = 0; i < mileposts.length; i++){
			mps[i + 1] = gameData.getMap().getMilepost(mileposts[i]);
		}
		active.moveTrain(train, mps);
		registerTransaction(originalGameState);
	}
	
	@Override
	public void pickupLoad(String pid, int train, String load) throws GameException {
		log.info("pickupLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		checkBuilding();
		String originalGameState = toString();
		active.pickupLoad(train, load);
		registerTransaction(originalGameState);
	}

	@Override
	public void deliverLoad(String pid, int train,
			String load, int card) throws GameException {
		log.info("deliverLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		checkBuilding();
		String originalGameState = toString();
		active.deliverLoad(card, train, dealCard());
		registerTransaction(originalGameState);
	}*/
	
	/** To undo, clients should first save off the original game state, 
	 * then do whatever action they're doing, then call saveForUndo. That
	 * way if the action throws an exception, it won't go on the undo stack.
	 * @param gameState
	 */
	private void saveForUndo(String gameState) {
		redoStack.clear();
		undoStack.push(gameState);
	}
	
	@Override
	public Game undo() throws GameException {
		log.info("undo");
		String originalGameState = toString();
		String gameState = undoStack.pop();
		log.debug("Serialized game: {}", gameState);
		Game newGame = Game.fromString(gameState, this);
		newGame.registerTransaction();
		newGame.redoStack.push(originalGameState);
		return newGame;
	}
	
	@Override
	public Game redo() throws GameException {
		log.info("redo");
		String originalGameState = toString();
		String gameString = redoStack.pop();
		Game newGame = Game.fromString(gameString, this);
		newGame.registerTransaction();
		newGame.undoStack.push(originalGameState);
		return newGame;
	}

	@Override
	public void endTurn(String pid) throws GameException {
		log.info("endTurn(pid={},activeIndex={})", pid, activeIndex);

		String next = pid;
		switch(turns){
		case 0:
			if (activeIndex == players.size() - 1){	// player goes again
				turns++;
			}else{
				next = pids.get(activeIndex + 1);
				activeIndex++;
			}
			break;
		case 1:
			if(activeIndex == 0){
				turns++;
			}else{
				next = pids.get(activeIndex - 1);
				activeIndex--;
			}
			break;
		default:
			if (activeIndex == pids.size() - 1) {
				turns++;
				next = pids.get(0);
			}
			else {
				next = (pids.get(activeIndex + 1));
				activeIndex++;
			}
		}
		
		turnData.endTurn(next, players);
			
		registerTransaction();
		undoStack.clear();
		
		// If the player has resigned, skip their turn.
//		if (active != null && active.hasResigned())
//			endTurn(active.getPid());
	}

	public void resign(String pid) throws GameException { 
		log.info("resign requestText: pid={}", pid);
		if (pid.equals(turnData.getPid()))
			endTurn(pid);
		getPlayer(pid).resign();
		pids.remove(pid);
		undoStack.clear();
		redoStack.clear();
	}
	
	@Override
	public boolean endGame(String pid, boolean ready) throws GameException {
		log.info("endGame(pid={}, ready={})", pid, ready);
		Player p = getPlayer(pid);
		p.readyToEnd(ready);
		
		boolean end = true;
		for (Player player: players.values())
			if (!player.readyToEnd())
				end = false;
		
		if (end) {	// All players are ready to end - end the game
			log.info("ending game");
			ended = true;
//			setActive(null);
		}
		registerTransaction();
		return ended;
	}
	
	public String toString() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Milepost.class, new MilepostShortFormTypeAdapter(this));
//		gsonBuilder.registerTypeAdapter(Rail.class, new RailTypeAdapter(this));
		Gson gson = gsonBuilder.create();
		return gson.toJson(this);
	}
	
	//serialization
	public static Game fromString(String gameString, Game refGame) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Milepost.class, new MilepostShortFormTypeAdapter(refGame));
		//gsonBuilder.registerTypeAdapter(Rail.class, new RailTypeAdapter(refGame));
		Gson gson = gsonBuilder.create();
		Game newGame = gson.fromJson(gameString, Game.class);
		
		// Must do fixups on the new game, since not all fields are serialized.
		newGame.gameData = refGame.gameData;
		newGame.ruleSet = refGame.ruleSet;
		newGame.transaction = refGame.transaction;
		newGame.lastChange = refGame.lastChange;
		newGame.undoStack = refGame.undoStack;
		newGame.redoStack = refGame.redoStack;
//		newGame.globalRail = new HashMap<Milepost, Set<Rail.Track>>();
//		for (Player p: newGame.players) 
//			p.fixup(newGame, newGame.globalRail);
//		newGame.setActive(newGame.players.get(refGame.activeIndex));
		
		return newGame;
	}

	public Player getPlayer(String pid) throws GameException {
		if(players.containsKey(pid)) return players.get(pid);
		throw new GameException(GameException.PLAYER_NOT_FOUND);
	}
	
	public String getLastPid(){	return pids.get(pids.size() - 1); }
	
	public Collection<Player> getPlayers(){ return players.values(); }
	
	public Set<String> getPids(){ return players.keySet(); }
	
	public int getTurns() { return turns; }
	
	private void checkActive(String pid) throws GameException {
		if(turnData == null)
			throw new GameException("GameNotStarted");
		if (!(pid.equals(turnData.getPid()))) 
			throw new GameException(GameException.PLAYER_NOT_ACTIVE);
	}

	private void checkBuilding() throws GameException{
		if(turns < 3) throw new GameException(GameException.BUILDING_TURN);
	}

	/** Returns player whose turn it is */
	public String getActivePid() { 
		if(turnData == null) return pids.get(0);
		return turnData.getPid();
	}
	
	public Player getActivePlayer(){
		return players.get(getActivePid());
	}
	
	public TurnData getTurnData(){ return turnData; }
	
	public GlobalRail getGlobalRail() {return globalRail; }
	
	public RuleSet getRuleSet() { return ruleSet; }
	
	public boolean isJoinable() { return turnData != null; }
	
	public boolean isOver() { return ended; }

	public int transaction() { return transaction; }
	
	public Date lastChangeDate() { return lastChange; }
	
	public String name() { return name; }
	
	private void registerTransaction() {
		lastChange = new Date();
		++transaction;
	}
	
	private void registerTransaction(String originalGameState) {
		saveForUndo(originalGameState);
		registerTransaction();
	}
	
	private Card dealCard() {
		return gameData.draw();
	}

	private Milepost[] convert(MilepostId[] m){
		Milepost[] mps = new Milepost[m.length];
		for(int i = 0; i < m.length; i++){
			mps[i] = gameData.getMap().getMilepost(m[i]);
		}	
		return mps;
	}
	
}
