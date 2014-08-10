package train;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Map;
import java.util.Set;

import map.Milepost;
import map.MilepostId;
import map.TrainMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.Player;
import reference.Card;
import reference.UpgradeType;


// Train game implementation class.
public class Game implements AbstractGame {

	public final GameData gameData;
	public final TrainMap map;
	private final RuleSet ruleSet;
	private Queue<Card> deck;
	private List<Player> players;
	private Player active;
	private Player last;
	private Map<Milepost, Set<Milepost>> globalRail;
	private int turns; //the number of completed turns; 0, 1, and 2 are building turns
	private boolean joinable;
	private int transaction;
	
	private static Logger log = LoggerFactory.getLogger(Game.class);
	
	/** Constuctor. 
	 * @param map
	 * @param ruleSet
	 */
	public Game(GameData gameData, RuleSet ruleSet){
		this.gameData = gameData;
		this.map = gameData.map;
		this.deck = gameData.deck;
		this.ruleSet = ruleSet;
		transaction = 1;
		players = new ArrayList<Player>();
		globalRail = new HashMap<Milepost, Set<Milepost>>();
		turns = 0;
		joinable = true;
	}
	
	/** Returns player whose turn it is */
	public Player getActivePlayer() { return active; }
	
	public RuleSet getRuleSet() { return ruleSet; }
	
	public boolean isJoinable() { return joinable; }
	
	public int transaction() { return transaction; }
	
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
		Player nextPlayer = (players.size() == 0) ? null : players.get(players.size() - 1);
		Player p = new Player(ruleSet.startingMoney, ruleSet.numTrains, hand, pid, color, nextPlayer, globalRail); 
		players.add(p);
		players.get(0).resetNextPlayer(p);
		active = p;
		++transaction;
	}

	@Override
	public void startGame(String pid, boolean ready) throws GameException {
		log.info("startGame(pid={}, ready={})", pid, ready);
		Player p = getPlayer(pid);
		p.readyToStart(ready);
		
		boolean start = true;
		for (Player player: players)
			if (!player.readyToStart())
				start = false;
		
		if (start) {	// All players are ready to start - start the game
			log.info("Starting game");
			Player first = players.get(0);
			for(int i = 1; i < players.size() && p.readyToStart(); i++){
				if(first.getMaxTrip() < players.get(i).getMaxTrip()) first = players.get(i);
			}
	
			active = first;
			for(Player temp = first.getNextPlayer(); !(temp == active); temp = temp.getNextPlayer()){
				last = temp;
			}
			last.setNextPlayer(first);
			joinable = false;
			++transaction;
		}
	}

	@Override
	public void buildTrack(String pid, MilepostId[] mileposts) throws GameException {
		log.info("buildTrack(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
		checkActive(pid);
		Queue<Milepost> queue = new ArrayDeque<Milepost>();
		for(int i = 0; i < mileposts.length; i++){
			queue.add(map.getMilepost(mileposts[i]));
		}
		active.buildTrack(queue);	
		++transaction;
	}

	@Override
	public void upgradeTrain(String pid, int train, UpgradeType upgrade)
			throws GameException {
		log.info("upgradeTrain(pid={}, upgradeType={})", pid, upgrade);
		checkActive(pid);
		checkBuilding();
		active.upgradeTrain(train, upgrade);
		++transaction;
	}

	@Override
	public void startTrain(String pid, int train, MilepostId where) throws GameException {
		log.info("startTrain(pid={}, city={})", pid, where);
		checkActive(pid);
		active.startTrain(map.getMilepost(where), train);
		++transaction;
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
		Queue<Milepost> moves = new ArrayDeque<Milepost>();
		for(int i = 0; i < mileposts.length; i++){
			moves.add(map.getMilepost(mileposts[i]));
		}
		active.moveTrain(train, moves);
		++transaction;
	}
	
	@Override
	public void pickupLoad(String pid, int train, String load) throws GameException {
		log.info("pickupLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		checkBuilding();
		active.pickupLoad(train, load);
		++transaction;
	}

	@Override
	public void deliverLoad(String pid, int train,
			String load, int card) throws GameException {
		log.info("deliverLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		checkBuilding();
		active.deliverLoad(card, train, deck.poll());
		++transaction;
	}

	@Override
	public void dumpLoad(String pid, int train, String load) throws GameException {
		log.info("dumpLoad(pid={}, load={})", pid, load);
		checkActive(pid);
		active.dropLoad(train, load);
		++transaction;
	}

	@Override
	public void endTurn(String pid) throws GameException {
		log.info("endTurn(pid={})", pid);
		boolean last = (active == getLastPlayer());
		Player p = active.endTurn();
		switch(turns){
		case 0:
			if(last){
				turns++;
			}else{
				active = p;
			}
			break;
		case 1:
			if(p.getNextPlayer() == getLastPlayer()){
				turns++;
			}else{
				active = getPrevPlayer(active);
			}
			break;
		default:
			active = p;
			if(last) turns++;
		}
			
		++transaction;
	}

	@Override
	public void endGame(String pid) throws GameException {
		log.info("endGame(pid={})", pid);
		if(!(active == last)) throw new GameException("PlayerNotActive");
		active.endTurn();
		++transaction;
	}

	private Player getPlayer(String pid) throws GameException {
		for(Player p : players){
			if (p.getPid().equals(pid)) return p;
		}
		throw new GameException("PlayerNotFound");
	}
	
	Player getLastPlayer(){
		return last;
	}
	
	Player getPrevPlayer(Player p){
		Player i = p;
		do{
			i = p.getNextPlayer();
		}while (i.getNextPlayer() != p);
		return i;
	}
	
	List<Player> getPlayers(){
		return players;
	}
	
	boolean getJoinable() {return joinable;}

	private void checkActive(String pid) throws GameException {
		if (!(getPlayer(pid) == active)) 
			throw new GameException("PlayerNotActive");
	}

	private void checkBuilding() throws GameException{
		if(turns < 3) throw new GameException("InvalidMove");
	}
}
