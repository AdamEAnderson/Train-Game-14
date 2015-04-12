package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import train.Game;
import train.GameException;
import train.TrainServer;

public class BuildOverExistingTrackTest extends GameTest{

	/** Test that building over track that has already been built fails */
	@Test
	public void testBuildOverExistingTrack()  {
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
        mileposts = new MilepostId[]{ new MilepostId(37, 8), new MilepostId(37, 9), new MilepostId(38, 10) };
        try {
        	game.buildTrack(game.getActivePlayer().name, mileposts);
        	game.endTurn(game.getActivePlayer().name);
        	game.buildTrack(game.getActivePlayer().name, mileposts);	// next player builds same track
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
