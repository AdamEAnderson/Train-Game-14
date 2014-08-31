package train;

import java.util.ArrayList;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import map.Ferry;
import map.Milepost;
import map.MilepostId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import player.Player;
import player.Rail;
import reference.Card;
import reference.UpgradeType;


// Train game implementation class.
public class Game implements AbstractGame {

	public transient GameData gameData;
	private transient RuleSet ruleSet;
	private Queue<Card> deck;
	private List<Player> players;
	private transient Player active;
	private int activeIndex;		// index of active player in players array
	private Map<Milepost, Set<Rail.Track>> globalRail;
	private Map<Milepost, Ferry> globalFerry;
	private int turns; //the number of completed turns; 0, 1, and 2 are building turns
	private boolean joinable;	// game has not yet started
	private boolean ended; // game has ended
	private transient int transaction;
	private transient Date lastChange;
	private String name;
	
	private static Logger log = LoggerFactory.getLogger(Game.class);
	
	static class UndoStack {
		Stack<String> stack;
		
		UndoStack() {
			this.stack = new Stack<String>();
		}
		
		void push(Game game) { 
			String gameState = game.toString();
			stack.push(gameState); 
		}
		
		Game pop(Game gameStart) throws GameException
		{ 
			Game game = null;
			try {
				String oldGame = stack.pop(); 
				log.debug("oldGame {}", oldGame);
				GsonBuilder gsonBuilder = new GsonBuilder();
				gsonBuilder.registerTypeAdapter(Milepost.class, new map.MilepostSerializer(gameStart));
				Gson gson = gsonBuilder.create();
				game = gson.fromJson(oldGame, Game.class);
			} catch (EmptyStackException e) {
				throw new GameException(GameException.NOTHING_TO_UNDO);
			}
			return game;
		}
		
		void clear() {
			stack.clear();
		}
	}
	static UndoStack undoStack = null;
	
	/** Constuctor. 
	 * @param map
	 * @param ruleSet
	 */
	public Game(String name, GameData gameData, RuleSet ruleSet){
		this.gameData = gameData;
		this.deck = gameData.deck;
		this.ruleSet = ruleSet;
		this.name = name;
		transaction = 1;
		lastChange = new Date();
		players = new ArrayList<Player>();
		globalRail = new HashMap<Milepost, Set<Rail.Track>>();
		globalFerry = new HashMap<Milepost, Ferry>();
		turns = 0;
		joinable = true;
		undoStack = new UndoStack();
	}
	
	@Override
	public void joinGame(String pid, String color) throws GameException {
		log.info("joinGame(pid={}, color={})", pid, color);
		// Check that this player is unique, and color is available
		for (Player p: players) {
			if (p.name.equals(pid))
				throw new GameException(GameException.PLAYER_ALREADY_JOINED);
			else if (p.color.equals(color))
				throw new GameException(GameException.COLOR_NOT_AVAILABLE);
		}
		Card [] hand = new Card[ruleSet.handSize];
		for(int i = 0; i < hand.length; i++){
			hand[i] = deck.poll();
		}
		players.add(new Player(ruleSet, hand, pid, color, this, globalRail, globalFerry));
		registerTransaction();
	}

	@Override
	public boolean startGame(String pid, boolean ready) throws GameException {
		log.info("startGame(pid={}, ready={})", pid, ready);
		Player p = getPlayer(pid);
		p.readyToStart(ready);
		
		boolean start = true;
		for (Player player: players)
			if (!player.readyToStart())
				start = false;
		
		if (start) {	// All players are ready to start - start the game
			log.info("Starting game");
			
			// Turn starts with player with the highest cards
			// Reorder players list so it matches playering order
			Player first = players.get(0);
			for (int i = 1; i < players.size(); i++){
				if (first.getMaxTrip() < players.get(i).getMaxTrip()) 
					first = players.get(i);
			}

			// Add all the players to the new players list, starting with the first player
			List<Player> newPlayers = new ArrayList<Player>();
			newPlayers.add(first);
			for (Player player: players){
				if (player != first)
					newPlayers.add(player);
			}
			players = newPlayers;
			setActive(first);
			
			joinable = false;
			registerTransaction();
		}
		return start;
	}

	@Override
	public boolean testBuildTrack(String pid, MilepostId[] mileposts) throws GameException {
		log.info("testBuildTrack(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
		checkActive(pid);
		Milepost[] array = new Milepost[mileposts.length];
		for(int i = 0; i < mileposts.length; i++){
			array[i] = gameData.map.getMilepost(mileposts[i]);
		}
		return active.testBuildTrack(array);	
	}

	@Override
	public void buildTrack(String pid, MilepostId[] milepostIds) throws GameException {
		log.info("buildTrack(pid={}, length={}, mileposts=[", pid, milepostIds.length);
		for (int i = 0; i < milepostIds.length; ++i)
			log.info("{}, ", milepostIds[i]);
		log.info("])");
		checkActive(pid);
		undoStack.push(this);

		Milepost[] mileposts = new Milepost[milepostIds.length];
		for(int i = 0; i < mileposts.length; i++){
			mileposts[i] = gameData.map.getMilepost(milepostIds[i]);
		}
		active.buildTrack(mileposts);	
		registerTransaction();
	}

	@Override
	public void upgradeTrain(String pid, int train, UpgradeType upgrade)
			throws GameException {
		log.info("upgradeTrain(pid={}, train={}, upgradeType={})", pid, upgrade, train);
		checkActive(pid);
		undoStack.push(this);

		active.upgradeTrain(train, upgrade);
		registerTransaction();
	}

	@Override
	public void placeTrain(String pid, int train, MilepostId where) throws GameException {
		log.info("placeTrain(pid={}, train={}, where={})", pid, train, where);
		checkActive(pid);

		undoStack.push(this);
		active.placeTrain(gameData.map.getMilepost(where), train);
		registerTransaction();
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
			mps[i + 1] = gameData.map.getMilepost(mileposts[i]);
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
		undoStack.push(this);

		Milepost[] mps = new Milepost[mileposts.length + 1];
		mps[0] = active.getTrains()[train].getLocation();
		for(int i = 0; i < mileposts.length; i++){
			mps[i + 1] = gameData.map.getMilepost(mileposts[i]);
		}
		active.moveTrain(train, mps);
		registerTransaction();
	}
	
	@Override
	public void pickupLoad(String pid, int train, String load) throws GameException {
		log.info("pickupLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		checkBuilding();
		undoStack.push(this);
		active.pickupLoad(train, load);
		registerTransaction();
	}

	@Override
	public void deliverLoad(String pid, int train,
			String load, int card) throws GameException {
		log.info("deliverLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		checkBuilding();
		undoStack.push(this);
		active.deliverLoad(card, train, deck.poll());
		registerTransaction();
	}

	@Override
	public void dumpLoad(String pid, int train, String load) throws GameException {
		log.info("dumpLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		undoStack.push(this);
		active.dropLoad(train, load);
		registerTransaction();
	}

	@Override
	public void turnInCards(String player) throws GameException {
		log.info("turnInCards requestText: pid={}", player);
		checkActive(player);
		if(active.turnInProgress()) 
			throw new GameException("TurnAlreadyStarted");
		undoStack.push(this);

		Card[] cards = new Card[ruleSet.handSize];
		for(int i = 0; i < cards.length; i++){
			cards[i] = deck.poll();
		}
		active.turnInCards(cards);
		endTurn(player);
	}
	
	@Override
	public Game undo() throws GameException {
		log.info("undo");
		registerTransaction();
		Game newGame = undoStack.pop(this);
		newGame.gameData = this.gameData;
		newGame.ruleSet = this.ruleSet;
		newGame.transaction = this.transaction;
		newGame.lastChange = this.lastChange;
		for (Player p: newGame.players) 
			p.fixup(newGame, newGame.globalRail, newGame.globalFerry);
		newGame.setActive(newGame.players.get(activeIndex));
		return newGame;
	}

	@Override
	public void endTurn(String pid) throws GameException {
		log.info("endTurn(pid={},activeIndex={})", pid, activeIndex);
		active.endTurn();
		switch(turns){
		case 0:
			if (activeIndex == players.size() - 1)	// player goes again
				turns++;
			else
				setActive(players.get(activeIndex + 1));
			break;
		case 1:
			if(activeIndex == 0)
				turns++;
			else
				setActive(getPrevPlayer(active));
			break;
		default:
			if (activeIndex == players.size() - 1) {
				turns++;
				setActive(players.get(0));
			}
			else setActive(players.get(activeIndex + 1));
		}
			
		registerTransaction();
		undoStack.clear();
		
		// If the player has resigned, skip their turn.
		if (active != null && active.hasResigned())
			endTurn(active.getPid());
	}

	public void resign(String pid) throws GameException { 
		log.info("resign requestText: pid={}", pid);
		if (getPlayer(pid).equals(active))
			endTurn(pid);
		getPlayer(pid).resign(); 
		undoStack.clear();
	}
	
	@Override
	public boolean endGame(String pid, boolean ready) throws GameException {
		log.info("endGame(pid={}, ready={})", pid, ready);
		Player p = getPlayer(pid);
		p.readyToEnd(ready);
		
		boolean end = true;
		for (Player player: players)
			if (!player.readyToEnd())
				end = false;
		
		if (end) {	// All players are ready to end - end the game
			log.info("ending game");
			ended = true;
			setActive(null);
		}
		registerTransaction();
		return ended;
	}

	private void setActive(Player p) {
		active = p;
		activeIndex = players.indexOf(p);
	}
	
	public String toString() {
		/*StringBuilder builder = new StringBuilder(4096);
		builder.append("{");
		builder.append("}");*/
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Milepost.class, new map.MilepostSerializer());
		Gson gson = gsonBuilder.create();
		return gson.toJson(this);
	}

	public Player getPlayer(String pid) throws GameException {
		for(Player p : players){
			if (p.getPid().equals(pid)) return p;
		}
		throw new GameException("PlayerNotFound");
	}
	
	Player getLastPlayer(){	return players.get(players.size() - 1); }
	
	Player getPrevPlayer(Player p){
		int index = players.indexOf(p);
		if (index < 0)	// player not found
			return null;
		if (index == 0)  // wraparound
			return players.get(players.size() - 1);
		return players.get(index - 1);
	}
	
	Player getNextPlayer(Player p){
		int index = players.indexOf(p);
		if (index < 0)	// player not found
			return null;
		if (index >= players.size() - 1)  // wraparound
			return players.get(0);
		return players.get(index + 1);
	}
	
	public List<Player> getPlayers(){ return players; }
	
	boolean getJoinable() {return joinable;}
	
	public int getTurns() { return turns; }

	private void checkActive(String pid) throws GameException {
		if (!(getPlayer(pid) == active)) 
			throw new GameException("PlayerNotActive");
	}

	private void checkBuilding() throws GameException{
		if(turns < 3) throw new GameException("InvalidMove");
	}

	/** Returns player whose turn it is */
	public Player getActivePlayer() { return active; }
	
	public RuleSet getRuleSet() { return ruleSet; }
	
	public boolean isJoinable() { return joinable; }
	
	public boolean isOver() { return ended; }

	public int transaction() { return transaction; }
	
	public Date lastChangeDate() { return lastChange; }
	
	public String name() { return name; }
	
	private void registerTransaction() {
		lastChange = new Date();
		++transaction;
	}
	
}
