package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reference.Card;
import reference.Trip;
import train.Game;
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
        game.startGame("Adam");
        
        String activePlayer = game.getActivePlayer().name;
        log.info("Active player is {}", activePlayer);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        
        game.startTrain(activePlayer, 0, new MilepostId(34,58));	// Johannesburg!
        // Stack the player's hand with cards we can deliver
        Trip[] trips = new Trip[3];
        trips[0] = new Trip(game.gameData.getCities().get("Kimberley"), "Diamonds", 12);
        trips[1] = new Trip(game.gameData.getCities().get("Asmera"), "Corn", 42);
        trips[2] = new Trip(game.gameData.getCities().get("Douala"), "Books", 45);
        int handSize = game.getRuleSet().handSize;
        Card cards[] = new Card[handSize];
        for (int i = 0; i < handSize; ++i)
        	cards[i] = new Card(trips);
        game.getActivePlayer().testReplaceCards(cards);
        game.pickupLoad(activePlayer, 0, "Diamonds");
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
        game.endGame(lastPlayer);
    }
	



}
