package test;

import static org.junit.Assert.*;

import org.junit.Test;

import train.Game;
import train.GameException;
import train.TrainServer;

public class ResignTest extends GameTest {

	@Test
	public void testResign() {
		try {
			String gid = newGame("Louie", "blue", "africa");
	        Game game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Huey", "green");
	        game.startGame("Huey", true);
	        game.startGame("Louie", true);
	        skipPastBuildingTurns(game);
	        String resignedPlayer = game.getActivePlayer().name;
	        game.resign(resignedPlayer);
	        
	        // Player now should be the other one
	        assertFalse(resignedPlayer.equals(game.getActivePlayer().name));
	        game.endTurn(game.getActivePlayer().name);
	        // Player should still be the other one
	        assertFalse(resignedPlayer.equals(game.getActivePlayer().name));
	        
	        game.endGame("Louie", true);
	        game.endGame("Huey", true);
		} catch (GameException e) {
			e.printStackTrace();
			fail("Unexpected GameException");
		}
	}

}
