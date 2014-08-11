package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.Player;
import reference.Card;
import reference.Trip;
import reference.UpgradeType;
import train.Game;
import train.GameException;
import train.TrainServer;


public class GameTest {

	private static Logger log = LoggerFactory.getLogger(GameTest.class);
	
	// Send a stream of requests to the server check the results
	@Test
	public void testTrain() throws Exception {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
        String responseMessage = TrainServer.newGame(jsonPayload);
        log.info("newGame response {}", responseMessage);
        String gid = responseMessage.substring(8, 16);
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Sandra", "green");
        game.joinGame("Sandy", "red");
        game.joinGame("Robin", "purple");
        game.startGame("Adam", true);
        game.startGame("Robin", true);
        game.startGame("Sandy", true);
        game.startGame("Sandra", true);
        
        String activePlayer = game.getActivePlayer().name;
        log.info("Active player is {}", activePlayer);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        
        game.startTrain(activePlayer, 0, new MilepostId(34,58));	// Johannesburg!
        // Stack the player's hand with cards we can deliver
        Trip[] trips = new Trip[3];
        trips[0] = new Trip("Kimberley", "Diamonds", 12);
        trips[1] = new Trip("Asmera", "Corn", 42);
        trips[2] = new Trip("Douala", "Books", 45);
        int handSize = game.getRuleSet().handSize;
        Card cards[] = new Card[handSize];
        for (int i = 0; i < handSize; ++i)
        	cards[i] = new Card(trips);
        game.getActivePlayer().testReplaceCards(cards);
        
        
        int i = game.getTurns();
        for(Player p = game.getActivePlayer(); i < 6; p = game.getActivePlayer()){
        	i = game.getTurns();
        	game.endTurn(p.name);
        	log.info("Active player is {}", p.name);
        	log.info("Increment is {}", i);
        }
        /*game.pickupLoad(activePlayer, 0, "Diamonds");
        game.pickupLoad(activePlayer, 0, "Arms");
        mileposts = new MilepostId[]{ new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.moveTrain(activePlayer, 0, mileposts);				// arrive in Kimberley
        game.deliverLoad(activePlayer, 0, "Diamonds", 0);
        game.dumpLoad(activePlayer, 0, "Arms");
        game.endTurn(activePlayer);
        game.endTurn(game.getActivePlayer().name);
        game.endTurn(game.getActivePlayer().name);
        String lastPlayer = game.getActivePlayer().name;
        game.endGame(lastPlayer);*/
    }
	
	// Upgrade to a 3-hauler
	@Test
	public void testUpgradeCapacity() throws Exception {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
        String responseMessage = TrainServer.newGame(jsonPayload);
        log.info("newGame response {}", responseMessage);
        String gid = responseMessage.substring(8, 16);
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Sandra", "green");
        game.joinGame("Sandy", "red");
        game.joinGame("Robin", "purple");
        game.startGame("Adam", true);
        game.startGame("Robin", true);
        game.startGame("Sandy", true);
        game.startGame("Sandra", true);
        
        String activePlayer = game.getActivePlayer().name;
        log.info("Active player is {}", activePlayer);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        game.endTurn(activePlayer);
        game.endTurn(game.getActivePlayer().name);	// skip over other players
        game.endTurn(game.getActivePlayer().name);
        game.endTurn(game.getActivePlayer().name);
        activePlayer = game.getActivePlayer().name;
        game.upgradeTrain(activePlayer, 0, UpgradeType.CAPACITY);
        game.endTurn(game.getActivePlayer().name);
        game.endGame("Adam", true);
        game.endGame("Robin", true);
        game.endGame("Sandy", true);
        game.endGame("Sandra", true);
    }
	
	// Play a short game with two trains
	@Test
	public void testTwoTrain() throws Exception {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"ruleSet\":{\"handSize\":4, \"startingMoney\":100, \"numTrains\":2}, \"gameType\":\"africa\"}";
        String responseMessage = TrainServer.newGame(jsonPayload);
        log.info("newGame response {}", responseMessage);
        String gid = responseMessage.substring(8, 16);
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Sandra", "green");
        game.startGame("Adam", true);
        game.startGame("Sandra", true);
        
        String activePlayer = game.getActivePlayer().name;
        log.info("Active player is {}", activePlayer);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        assertTrue(game.getActivePlayer().getSpending() == 5);
        game.startTrain(activePlayer, 0, new MilepostId(34,58));	// Johannesburg!
        game.startTrain(activePlayer, 1, new MilepostId(31, 59));	// Kimberley!
        game.endTurn(game.getActivePlayer().name);

        // Skip past building turns
        for(Player p = game.getActivePlayer(); game.getTurns() < 3; p = game.getActivePlayer()){
        	game.endTurn(p.name);
        	log.info("Active player is {}", p.name);
        	log.info("Turn count is {}", game.getTurns());
        }
        
        log.info("Active player is {}", game.getActivePlayer().name);

        // Stack the player's hand with cards we can deliver
        Trip[] trips = new Trip[3];
        trips[0] = new Trip("Kimberley", "Diamonds", 12);
        trips[1] = new Trip("Johannesburg", "Gold", 6);
        trips[2] = new Trip("Johannesburg", "Uranium", 8);
        int handSize = game.getRuleSet().handSize;
        Card cards[] = new Card[handSize];
        for (int j = 0; j < handSize; ++j)
        	cards[j] = new Card(trips);
        game.getActivePlayer().testReplaceCards(cards);
        game.pickupLoad(activePlayer, 0, "Diamonds");
        game.pickupLoad(activePlayer, 0, "Arms");
        game.pickupLoad(activePlayer, 1, "Gold");
        game.pickupLoad(activePlayer, 1, "Uranium");
        mileposts = new MilepostId[]{ new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.moveTrain(activePlayer, 0, mileposts);				// arrive in Kimberley
        game.deliverLoad(activePlayer, 0, "Diamonds", 0);
        game.dumpLoad(activePlayer, 0, "Arms");
        mileposts = new MilepostId[]{ new MilepostId(32, 58), new MilepostId(33, 58), 
        		new MilepostId(34,58) };
        game.moveTrain(activePlayer, 1, mileposts);				// arrive in Johannesburg
        game.deliverLoad(activePlayer, 1, "Gold", 1);
        game.deliverLoad(activePlayer, 1, "Uranium", 2);
        Player movingPlayer = game.getActivePlayer();
        game.endTurn(activePlayer);
        assertTrue(movingPlayer.getMoney() == 121);	// total should includes deliveries - building
        game.endTurn(game.getActivePlayer().name);
        game.endTurn(game.getActivePlayer().name);
        game.endGame("Adam", true);
        game.endGame("Sandra", true);
    }
	
	@Test
	public void testStatusMsg() throws GameException{
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
        String responseMessage = TrainServer.newGame(jsonPayload);
        log.info("newGame response {}", responseMessage);
        String gid = responseMessage.substring(8, 16);
        String jsonRequestPayload = "{\"gid\":\"" + gid + "\"}";
        String statusMsg = TrainServer.status(jsonRequestPayload);
        log.info("empty status message {}", statusMsg);
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Sandra", "green");
        game.joinGame("Sandy", "red");
        game.joinGame("Robin", "purple");
        log.info("post-join status message {}", TrainServer.status(jsonRequestPayload));
        game.startGame("Adam", true);
        game.startGame("Robin", true);
        game.startGame("Sandy", true);
        game.startGame("Sandra", true);
        log.info("status after starting the game {}", TrainServer.status(jsonRequestPayload));
        
        String activePlayer = game.getActivePlayer().name;
        log.info("Active player is {}", activePlayer);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        log.info("post track building {}", TrainServer.status(jsonRequestPayload));
	}
	
}
