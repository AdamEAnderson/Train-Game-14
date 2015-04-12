package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import train.Game;
import train.GameException;
import train.TrainServer;

public class BuildFromUnconnectedMpTest extends GameTest {

	/** Test that building from a place that is not a mojor city, and not already on the player's track fails */
	@Test
	public void testBuildFromUnconnectedTrack()  {
		Game game = null;;
		try {
			String gid = newGame("Louie", "blue", "africa");
	        game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Huey", "green");
	        game.startGame("Louie", true);
	        game.startGame("Huey", true);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		} 
        
        // First player builds from Luxor to Cairo - should fail because player hasn't build to Luxor
        log.info("Active player is {}", game.getActivePlayer().name);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(38, 10), new MilepostId(37, 10), new MilepostId(36, 9), new MilepostId(36, 8) };
        try {
        	game.buildTrack(game.getActivePlayer().name, mileposts);
        	fail("Build track should have thrown");
        } catch (GameException e) {
        }

		try {
	        game.endGame("Louie", true);
	        game.endGame("Huey", true);
		} catch (GameException e) {
			fail("Unexpected exception in test cleanup");
		} 
	}
}
