package test;

import static org.junit.Assert.*;

import java.util.Arrays;

import map.MilepostId;

import org.junit.Test;

import train.Game;
import train.GameException;
import train.TrainServer;

public class FerryTest extends GameTest{

	@Test
	public void testFerry() {
		Game game = null;
		try {
			String gid = newGame("Louie", "blue", "africa");
	        game = TrainServer.getGame(gid);
	        startGame(game);
	        
	        // Build to Tenarife
			MilepostId[] buildMileposts = new MilepostId[] {
					new MilepostId(2,18),
					new MilepostId(2,17), 
					new MilepostId(3,16), 
					new MilepostId(3,15),
					new MilepostId(4,14),
					new MilepostId(4,13),
					new MilepostId(5,12),
					new MilepostId(5,11),
					new MilepostId(6,10),
					new MilepostId(6,9),
					new MilepostId(6,8),
					new MilepostId(5,7),
					new MilepostId(0,6)		// Tenarife via ferry
					};
			game.buildTrack(game.getActivePlayer().name, buildMileposts);
			assertEquals(19, game.getActivePlayer().getSpending());
			skipPastBuildingTurns(game);
			
			// Start in Dakar and go to port for Tenarife
			MilepostId[] moveMileposts = new MilepostId[] {
					new MilepostId(2,17), 
					new MilepostId(3,16), 
					new MilepostId(3,15),
					new MilepostId(4,14),
					new MilepostId(4,13),
					new MilepostId(5,12),
					new MilepostId(5,11),
					new MilepostId(6,10),
					new MilepostId(6,9),
					new MilepostId(6,8),
					new MilepostId(5,7),	// stop in port
					};
			game.placeTrain(game.getActivePlayer().name, 0, new MilepostId(2,18));
			game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
			game.endTurn(game.getActivePlayer().name);

			// Cross to Tenarife
			moveMileposts = new MilepostId[] {
					new MilepostId(0,6)		// Tenarife via ferry
					};
			game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
			assertEquals(6, game.getActivePlayer().getMovesMade(0));
			assertEquals("Tenerife", game.getActivePlayer().getTrains()[0].getLocation().city.name);
			game.endTurn(game.getActivePlayer().name);
			
			// Cross back to mainland, try to return to Dakar (should fail because 
			// after ferry crossing goes half speed)
			moveMileposts = new MilepostId[] {
					new MilepostId(2,17), 
					new MilepostId(3,16), 
					new MilepostId(3,15),
					new MilepostId(4,14),
					new MilepostId(4,13),
					new MilepostId(5,12),
					new MilepostId(5,11),
					};
        	reverse(moveMileposts);
        	try {
        		game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
        		fail("Expected move exception here -- didn't get one");
        	} catch (GameException e) {
        	}
        	
        	// Try the same again, but moving in increments -- should fail
			moveMileposts = new MilepostId[] {
					new MilepostId(2,17), 
					new MilepostId(3,16), 
					new MilepostId(3,15),
					new MilepostId(4,14),
					new MilepostId(4,13),
					new MilepostId(5,12),
					new MilepostId(5,11),
					new MilepostId(6,10),
					new MilepostId(6,9),
					new MilepostId(6,8),
					new MilepostId(5,7),	// stop in port
					};
        	reverse(moveMileposts);
        	try {
        		game.moveTrain(game.getActivePlayer().name, 0, Arrays.copyOfRange(moveMileposts, 0, 4));
        		game.moveTrain(game.getActivePlayer().name, 0, Arrays.copyOfRange(moveMileposts, 4, moveMileposts.length));
        		fail("Expected move exception here -- didn't get one");
        	} catch (GameException e) {
        		// Undo first move (which succeeded) so we return to ferry stop
        		game = undo(gid, game.getActivePlayer().name);
        	}
        	
        	// Now try moving legal amount (6)
			moveMileposts = new MilepostId[] {
					new MilepostId(5,12),
					new MilepostId(5,11),
					new MilepostId(6,10),
					new MilepostId(6,9),
					new MilepostId(6,8),
					new MilepostId(5,7),	// stop in port
					};
        	reverse(moveMileposts);
    		game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
			game.endTurn(game.getActivePlayer().name);
        	
			endGame(game);
		} catch (GameException e) {
			fail("Unexpected exception");
		}
	}

}
