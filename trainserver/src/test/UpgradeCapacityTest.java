package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import reference.UpgradeType;
import train.Game;

public class UpgradeCapacityTest extends GameTest {

	// Upgrade to a 3-hauler
		@Test
		public void testUpgradeCapacity() throws Exception {
			String gid = newGame("Louie", "blue", "africa");
	        Game game = trainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Huey", "green");
	        game.joinGame("Esmeralda", "red");
	        game.joinGame("Dewey", "purple");
	        startGame(game);
	        
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
	        game.upgradeTrain(activePlayer, 0, UpgradeType.CAPACITY);	// upgrade during building turn
	        game.endTurn(game.getActivePlayer().name);
	        skipPastBuildingTurns(game);
	        
	        game.upgradeTrain(game.getActivePlayer().name, 0, UpgradeType.SPEED);	// upgrade during standard turn

	        endGame(game);
	    }

}
