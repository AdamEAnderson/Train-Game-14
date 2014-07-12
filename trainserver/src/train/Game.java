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

	public final TrainMap map;
	private final RuleSet ruleSet;
	private Queue<Card> deck;
	private List<Player> players;
	private Player active;
	private Player last;
	private Map<Milepost, Set<Milepost>> globalRail;
	
	private static Logger log = LoggerFactory.getLogger(Game.class);
	
	/** Constuctor. 
	 * @param map
	 * @param ruleSet
	 */
	public Game(GameData gameData, RuleSet ruleSet){
		this.map = gameData.map;
		this.deck = gameData.deck;
		this.ruleSet = ruleSet;
		players = new ArrayList<Player>();
		globalRail = new HashMap<Milepost, Set<Milepost>>();
	}
	
	/** Returns player whose turn it is */
	public Player getActivePlayer() { return active; }
	
	@Override
	public void joinGame(String pid, String color) throws GameException {
		log.info("joinGame(pid={}, color={})", pid, color);
		Player p = null;
		try { 
			p = getPlayer(pid);
		} catch(GameException e){ }
		if(p != null) throw new GameException("PlayerAlreadyJoined");
		Card [] hand = new Card[ruleSet.handSize];
		for(int i = 0; i < hand.length; i++){
			hand[i] = deck.poll();
		}
		Player nextPlayer = (players.size() == 0) ? null : players.get(players.size() - 1);
		p = new Player(ruleSet.startingMoney, hand, pid, color, nextPlayer, globalRail);
		players.add(p);
		players.get(0).resetNextPlayer(p);
	}

	@Override
	public void startGame(String pid) throws GameException {
		log.info("startGame(pid={})", pid);
		Player first = players.get(0);
		for(int i = 1; i < players.size(); i++){
			if(first.getMaxTrip() < players.get(i).getMaxTrip()) first = players.get(i);
		}
		active = first;
		for(Player temp = first.getNextPlayer(); !(temp == active); temp = temp.getNextPlayer()){
			last = temp;
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
	}

	@Override
	public void upgradeTrain(String pid, int train, UpgradeType upgrade)
			throws GameException {
		log.info("upgradeTrain(pid={}, upgradeType={})", pid, upgrade);
		checkActive(pid);
		active.upgradeTrain(upgrade);
	}

	@Override
	public void startTrain(String pid, int train, MilepostId where) throws GameException {
		log.info("startTrain(pid={}, city={})", pid, where);
		checkActive(pid);
		active.startTrain(map.getMilepost(where));
	}

	@Override
	public void moveTrain(String pid, int train, MilepostId[] mileposts)
			throws GameException {
		log.info("moveTrain(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
		checkActive(pid);
		Queue<Milepost> moves = new ArrayDeque<Milepost>();
		for(int i = 0; i < mileposts.length; i++){
			moves.add(map.getMilepost(mileposts[i]));
		}
		active.moveTrain(moves);
	}
	
	@Override
	public void pickupLoad(String pid, int train, String load) throws GameException {
		log.info("pickupLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
		active.pickupLoad(load);
	}

	@Override
	public void deliverLoad(String pid, int train,
			String load, int card) throws GameException {
		log.info("deliverLoad(pid={}, train={}, load={})", pid, train, load);
		checkActive(pid);
	//	int index = -1; //will throw ArrayIndexOutOfBounds; we need a good way of testing
	//	active.deliverLoad(index, deck.poll());
	}

	@Override
	public void dumpLoad(String pid, int train, String load) throws GameException {
		log.info("dumpLoad(pid={}, load={})", pid, load);
		checkActive(pid);
		active.dropLoad(load);
	}

	@Override
	public void endTurn(String pid) throws GameException {
		log.info("endTurn(pid={})", pid);
		active = active.endTurn();
	}

	@Override
	public void endGame(String pid) throws GameException {
		log.info("endGame(pid={})", pid);
		if(!(active == last)) throw new GameException("PlayerNotActive");
	}

	private Player getPlayer(String pid) throws GameException {
		for(Player p : players){
			if (p.getPid().equals(pid)) return p;
		}
		throw new GameException("PlayerNotFound");
	}

	private void checkActive(String pid) throws GameException {
		if (!(getPlayer(pid) == active)) 
			throw new GameException("PlayerNotActive");
	}

}
