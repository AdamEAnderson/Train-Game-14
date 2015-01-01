package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import reference.Card;
import reference.Trip;
import train.Game;
import train.TrainServer;

public class TrainTest extends GameTest {

	@Test
	public void testTrain() throws Exception {
		String gid = newGame("Louie", "blue", "africa");
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Huey", "green");
        game.joinGame("Esmeralda", "red");
        game.joinGame("Dewey", "purple");
        startGame(game);
        
        String activePlayer = game.getActivePid();
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        
        // Stack the player's hand with cards we can deliver
        Trip[] trips = new Trip[3];
        trips[0] = new Trip("Kimberley", "Diamonds", 12);
        trips[1] = new Trip("Asmera", "Corn", 42);
        trips[2] = new Trip("Douala", "Books", 45);
        int handSize = game.getRuleSet().handSize;
        Card cards[] = new Card[handSize];
        for (int i = 0; i < handSize; ++i)
        	cards[i] = new Card(trips);
        game.getActivePlayer().turnInCards(cards);
        
        skipPastBuildingTurns(game);

        game.placeTrain(activePlayer, 0, new MilepostId(34,58));	// Johannesburg!

        game.pickupLoad(activePlayer, 0, "Diamonds");
        game.pickupLoad(activePlayer, 0, "Arms");
        mileposts = new MilepostId[]{ new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.moveTrain(activePlayer, activePlayer, 0, mileposts);				// arrive in Kimberley
        game.deliverLoad(activePlayer, 0, "Diamonds", 0);
        game.dumpLoad(activePlayer, 0, "Arms");
        game.endTurn(activePlayer);
        game.endTurn(game.getActivePlayer().name);
        game.endTurn(game.getActivePlayer().name);
        endGame(game);
    }

}
