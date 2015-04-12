package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import player.Player;
import reference.Card;
import reference.Trip;
import train.Game;
import train.TrainServer;

public class TwoTrainTest extends GameTest {

	// Play a short game with two trains
		@Test
		public void testTwoTrain() throws Exception {
			String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Louie\", \"color\":\"blue\", \"name\":\"TestGame\", \"ruleSet\":{\"handSize\":4, \"startingMoney\":100, \"numTrains\":2}, \"gameType\":\"africa\"}";
	        String responseMessage = TrainServer.newGame(jsonPayload);
	        log.info("newGame response {}", responseMessage);
	        String gid = responseMessage.substring(8, 16);
	        Game game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Huey", "green");
	        startGame(game);

	        String activePlayer = game.getActivePlayer().name;
	        log.info("Active player is {}", activePlayer);
	        MilepostId[] mileposts;
	        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
	            	new MilepostId(31, 59) };
	        game.buildTrack(activePlayer, mileposts);
	        assertTrue(game.getTurnData().getSpending() == 5);
	        game.endTurn(game.getActivePlayer().name);

	        skipPastBuildingTurns(game);
	        
	        game.placeTrain(activePlayer, 0, new MilepostId(34,58));	// Johannesburg!
	        game.placeTrain(activePlayer, 1, new MilepostId(31, 59));	// Kimberley!
	        
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
	        game.getActivePlayer().turnInCards(cards);
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
	        endGame(game);
	    }

}
