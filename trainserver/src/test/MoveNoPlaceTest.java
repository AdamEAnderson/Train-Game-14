package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import train.Game;
import train.GameException;
import train.TrainServer;

public class MoveNoPlaceTest extends GameTest {


	
	/** Test moving a train without having placed it */
	@Test
	public void testMoveNoPlace() {
		Game game = null;
		try {
			String gid = newGame("Louie", "blue", "africa");
	        game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Tim", "black");
	        game.joinGame("Ann", "green");
	        startGame(game);

	        // First player builds from Dakar to Abidjan
	        MilepostId[] buildMileposts;
	        buildMileposts = new MilepostId[]{ 
	        	new MilepostId(2,20),
	        	new MilepostId(2,21),
	        	new MilepostId(3,22),
	        	new MilepostId(3,23),
	        	new MilepostId(4,24),
	        	new MilepostId(4,25),
	        	new MilepostId(5,26),
	        	new MilepostId(5,27),
	        	new MilepostId(6,27),
	        	new MilepostId(7,27),
	        	new MilepostId(8,27), 
	        	new MilepostId(9,27), 
	        	//new MilepostId(10,27),
	        };
        	game.buildTrack(game.getActivePlayer().name, buildMileposts);
        	game.endTurn(game.getActivePlayer().name);

        	skipPastBuildingTurns(game);
        	
        	// Move from Abidjan, skip placing train
        	//game.placeTrain(game.getActivePlayer().name, 0, new MilepostId(10,27));
	        MilepostId[] moveMileposts;
	        moveMileposts = new MilepostId[]{ 
	        	new MilepostId(10,27),
	        	new MilepostId(9,27), 
	        	new MilepostId(8,27), 
	        	new MilepostId(7,27),
	        	new MilepostId(6,27),
	        	new MilepostId(5,27),
	        	new MilepostId(5,26),
	        	new MilepostId(4,25),
	        	new MilepostId(4,24),
	        	new MilepostId(3,23),
	        	new MilepostId(3,22),
	        	new MilepostId(2,21),
	        };
        	game.moveTrain(game.getActivePid(), game.getActivePid(), 0, moveMileposts);
			fail("Expected invalid move exception");
		} catch (GameException e) {
		} 
	}

}
