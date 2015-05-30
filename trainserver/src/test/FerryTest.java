package test;

import static org.junit.Assert.*;

import java.util.Arrays;

import map.MilepostId;

import org.junit.Test;

import train.Game;
import train.GameException;

public class FerryTest extends GameTest{

	@Test
	public void testFerry() {
		Game game = null;
		try {
			String gid = newGame("Louie", "blue", "africa");
	        game = trainServer.getGame(gid);
	        startGame(game);
	        String pid = game.getActivePid();
	        
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
			game.buildTrack(pid, buildMileposts);
			assertEquals(19, game.getTurnData().getSpending());
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
			game.moveTrain(pid, 0, moveMileposts);
			game.endTurn(game.getActivePlayer().name);

			// Cross to Tenarife
			moveMileposts = new MilepostId[] {
					new MilepostId(0,6)		// Tenarife via ferry
					};
			game.moveTrain(pid, 0, moveMileposts);
			assertEquals(1, game.getTurnData().getMovesMade(0));
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
        		game.moveTrain(pid, 0, moveMileposts);
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
        		game.moveTrain(pid, 0, Arrays.copyOfRange(moveMileposts, 0, 4));
        		game.moveTrain(pid, 0, Arrays.copyOfRange(moveMileposts, 4, moveMileposts.length));
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
    		game.moveTrain(pid, 0, moveMileposts);
			game.endTurn(game.getActivePid());
    		
    		// Try moving with the ferry crossing in the middle of the move. This should fail
			moveMileposts = new MilepostId[] {
					new MilepostId(5,11),
					new MilepostId(6,10),
					new MilepostId(6,9),
					new MilepostId(6,8),
					new MilepostId(5,7),	// port
					new MilepostId(0,6)		// Tenarife via ferry
					};
        	try {
        		game.moveTrain(pid, 0, moveMileposts);
        		fail("Expected move exception here -- didn't get one");
        	} catch (GameException e) {
        		// Undo the move (which succeeded) so we return to ferry stop
        		// game = undo(gid, game.getActivePlayer().name);
        	}
        	
        	// Try the same again in two separate moves
        	MilepostId[] moveMileposts1 = new MilepostId[] {
					new MilepostId(5,11),
					new MilepostId(6,10),
					new MilepostId(6,9),
					new MilepostId(6,8),
					new MilepostId(5,7),	// port
					};
        	MilepostId[] moveMileposts2 = new MilepostId[] {
					new MilepostId(0,6)		// Tenarife via ferry
					};
        	try {
        		game.moveTrain(pid, 0, moveMileposts1);
        		game.moveTrain(pid, 0, moveMileposts2);
        		fail("Expected move exception here -- didn't get one");
        	} catch (GameException e) {
        		// Undo the move (which succeeded) so we return to ferry stop
        		game = undo(gid, game.getActivePlayer().name);
        	}
    		
			game.endTurn(game.getActivePid());
        	
			endGame(game);
		} catch (GameException e) {
			fail("Unexpected exception");
		}
	}

}
