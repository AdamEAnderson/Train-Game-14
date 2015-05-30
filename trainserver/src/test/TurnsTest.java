package test;

import static org.junit.Assert.*;

import org.junit.Test;

import train.Game;
import train.GameException;

public class TurnsTest extends GameTest {


	/** Test player order in turns */
	@Test
	public void testTurns() {
		Game game = null;
		try {
			String gid = newGame("Louie", "blue", "africa");
	        game = trainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Tim", "black");
	        game.joinGame("Ann", "green");
	        startGame(game);

	        // First round of building goes in forward order
	        String first = game.getActivePlayer().name;
	        takeTurn(game, first);
        	String second = game.getActivePlayer().name;
        	takeTurn(game, second);
        	String third = game.getActivePlayer().name;
        	takeTurn(game, third);
        	
        	// Turnaround turn - goes in reverse order
        	takeTurn(game, third);
        	takeTurn(game, second);
        	takeTurn(game, first);
        	
        	// Last building turn - goes in forward order
	        takeTurn(game, first);
        	takeTurn(game, second);
        	takeTurn(game, third);
        	
        	// Regular turns go in forward order -- run through a few
        	for (int i = 0; i < 5; ++i) {
    	        takeTurn(game, first);
            	takeTurn(game, second);
            	takeTurn(game, third);
        	}
        	endGame(game);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		}
	}

}
