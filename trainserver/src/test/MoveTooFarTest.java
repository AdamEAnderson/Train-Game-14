package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import train.Game;
import train.GameException;

public class MoveTooFarTest extends GameTest {

	/** Test move error, move further than train can go in one turn */
	@Test
	public void testMoveTooFar() {
		Game game = null;;
		try {
			String gid = newGame("Louie", "blue", "africa");
	        game = trainServer.getGame(gid);
	        assertTrue(game != null);
	        game.startGame("Louie", true);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		} 
        
        // Build 
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
        };
        MilepostId[] moveMileposts;
        moveMileposts = new MilepostId[]{ 
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
	        //new MilepostId(24,47),
	        //new MilepostId(25,46),
        };

		try {
        	game.buildTrack(game.getActivePlayer().name, mileposts);
			skipPastBuildingTurns(game);
			game.placeTrain("Louie", 0, new MilepostId(34,56));
		} catch (GameException e) {
			log.error("Unexpected exception {}", e);
			fail("Unexpected exception");
		} 
		try {
			game.moveTrain("Louie", 0, moveMileposts);
			fail("Expected move error from moving too far");
		} catch (GameException e) {
		} 
		try {
	        game.endGame("Louie", true);
		} catch (GameException e) {
			log.error("Unexpected exception {}", e);
			fail("Unexpected exception");
		} 
	}

}
