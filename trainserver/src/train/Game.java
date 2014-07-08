package train;
import java.util.List;
import java.util.Queue;

import map.MilepostId;
import map.TrainMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.Player;
import reference.Card;
import reference.UpgradeType;


// Train game implementation class.
public class Game implements AbstractGame {

	private final TrainMap map;
	private final int handSize; //the number of cards in a hand
	private final int startingMoney; //the money each player starts with
	private Queue<Card> deck;
	private List<Player> players;
	private Player active;
	private Player last;
	
	private static Logger log = LoggerFactory.getLogger(Game.class);
	
	private Player getPlayer(String pid) throws GameException {
		for(Player p : players){
			if(p.getPid() == pid) return p;
		}
		throw new GameException("PlayerNotFound");
	}

	public Game(TrainMap map, String ruleSet){
		this.map = map;
		//deck = Card.init();
		//players = Player.init();
		handSize = 4; 
		startingMoney = 70; //Arbitrary values, can be changed as needed
	}
	
	@Override
	public void joinGame(String pid, String color)
			throws GameException {
		log.info("joinGame(pid={}, color={})", pid, color);
		Player p = null;
		try { 
			p = getPlayer(pid);
		} catch(GameException e){ }
		if(p != null) throw new GameException("PlayerAlreadyJoined");
		Card [] hand = new Card[handSize];
		for(int i = 0; i < hand.length; i++){
			hand[i] = deck.poll();
		}
		p = new Player(startingMoney, hand, pid, color, players.get(players.size() - 1));
		players.add(p);
		players.get(0).resetNextPlayer(p);
	}

	@Override
	public void startGame(String pid) throws GameException {
		log.info("startGame(pid={})", pid);
	}

	@Override
	public void buildTrack(String pid,
			MilepostId[] mileposts) throws GameException {
		log.info("buildTrack(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
		
	}

	@Override
	public void upgradeTrain(String pid, UpgradeType upgrade)
			throws GameException {
		log.info("upgradeTrain(pid={}, upgradeType={})", pid, upgrade);
		Player p = getPlayer(pid);
		if(p == active) p.upgradeTrain(upgrade);
		else throw new GameException("PlayerNotActive");
	}

	@Override
	public void startTrain(String pid, MilepostId where) {
		log.info("startTrain(pid={}, city={})", pid, where);
	}

	@Override
	public void moveTrain(String pid, MilepostId[] mileposts)
			throws GameException {
		log.info("moveTrain(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
	}

	@Override
	public void pickupLoad(String pid, String city,
			String load) {
		log.info("pickupLoad(pid={}, city={}, load={})", pid, city, load);
	}

	@Override
	public void deliverLoad(String pid, String city,
			String load) {
		log.info("deliverLoad(pid={}, city={}, load={})", pid, city, load);
	}

	@Override
	public void dumpLoad(String pid, String load) throws GameException {
		log.info("dumpLoad(pid={}, load={})", pid, load);
		if(!(getPlayer(pid) == active)) throw new GameException("PlayerNotActive");
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

}
