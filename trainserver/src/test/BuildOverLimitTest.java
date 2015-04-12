package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import train.Game;
import train.GameException;
import train.TrainServer;

public class BuildOverLimitTest extends GameTest{

	/** Test building too much (cost more than 20) */
	@Test
	public void testBuildOverCost() {
		Game game = null;;
		try {
			String gid = newGame("Louie", "blue", "africa");
	        game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.startGame("Louie", true);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		} 
        
        // Build more than 20
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ 
        	new MilepostId(34,56),
        	new MilepostId(33,55),
	        new MilepostId(33,54),
	        new MilepostId(32,53),
	        new MilepostId(31,53),
	        new MilepostId(31,52),
	        new MilepostId(30,52),
	        new MilepostId(29,52),
	        new MilepostId(28,52),
	        new MilepostId(27,52),
	        new MilepostId(26,51),
	        new MilepostId(26,50),
	        new MilepostId(25,49),
	        new MilepostId(25,48),
	        new MilepostId(24,47),
	        new MilepostId(25,46),
	        new MilepostId(25,45)
        };
        try {
        	game.buildTrack(game.getActivePlayer().name, mileposts);
        	fail("Build track should have thrown");
        } catch (GameException e) {
        }

		try {
	        game.endGame("Louie", true);
		} catch (GameException e) {
			fail("Unexpected exception in test cleanup");
		} 
	}

}
