package train;
import java.util.ArrayList;
import java.util.List;

import map.MilePostId;
import map.TrainMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.Player;
import reference.Card;
import reference.UpgradeType;


// Train game implementation class.
public class Game implements AbstractGame {

	private final TrainMap map;
	private List<Card> deck;
	private List<Card> discard;
	private List<Player> players;
	
	private static Logger log = LoggerFactory.getLogger(Game.class);

	public Game(TrainMap map, String ruleSet){
		this.map = map;
		//deck = Card.init();
		discard = new ArrayList<Card>();
		//players = Player.init();
	}
	
	@Override
	public void joinGame(String pid, String color)
			throws GameException {
		log.info("joinGame(pid={}, color={})", pid, color);
	}

	@Override
	public void startGame(String pid) throws GameException {
		log.info("startGame(pid={})", pid);
	}

	@Override
	public void buildTrack(String pid,
			MilePostId[] mileposts) throws GameException {
		log.info("buildTrack(pid={}, length={}, mileposts=[", pid, mileposts.length);
		for (int i = 0; i < mileposts.length; ++i)
			log.info("{}, ", mileposts[i]);
		log.info("])");
		
	}

	@Override
	public void upgradeTrain(String pid, UpgradeType upgrade)
			throws GameException {
		log.info("upgradeTrain(pid={}, upgradeType={})", pid, upgrade);
	}

	@Override
	public void startTrain(String pid, String city) {
		log.info("startTrain(pid={}, city={})", pid, city);
	}

	@Override
	public void moveTrain(String pid, MilePostId[] mileposts)
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
	public void dumpLoad(String pid, String load) {
		log.info("dumpLoad(pid={}, load={})", pid, load);
	}

	@Override
	public void endTurn(String pid) throws GameException {
		log.info("endTurn(pid={})", pid);
	}

	@Override
	public void endGame(String pid) throws GameException {
		log.info("endGame(pid={})", pid);
	}

}
