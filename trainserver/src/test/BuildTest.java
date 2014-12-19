package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import player.Player;
import train.Game;
import train.GameException;
import train.TrainServer;

public class BuildTest extends GameTest {

	/** Test that normal building works as expected */
	@Test
	public void testBuild() throws GameException {
		int expectedTotalSpent = 0;
		int accumulatedTotal = 70;
		String gid = newGame("Louie", "blue", "africa");
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Huey", "green");
        game.startGame("Louie", true);
        game.startGame("Huey", true);
        
        Player firstPlayer = game.getActivePlayer();

        // First player builds from Cairo to Luxor - check building into a city & over a river
        log.info("Active player is {}", game.getActivePid());
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(37, 8), new MilepostId(37, 9), new MilepostId(38, 10) };
        game.buildTrack(game.getActivePid(), mileposts);
        expectedTotalSpent += 6;	// Incremental cost of 6
        assertEquals(expectedTotalSpent, game.getTurnData().getSpending());
        
        // Build into minor & major cities
        // Build back into Cairo from Luxor
        // Tests that player can build from end of their track into major city
        mileposts = new MilepostId[]{ new MilepostId(38, 10), new MilepostId(37, 10), new MilepostId(36, 9), new MilepostId(36, 8) };
        game.buildTrack(game.getActivePid(), mileposts);
        expectedTotalSpent += 3;
        assertEquals(expectedTotalSpent, game.getTurnData().getSpending());
        
        // Sea inlet crossing - build from Cairo to the Sinai
        mileposts = new MilepostId[]{ new MilepostId(37, 7), new MilepostId(38, 6) };
        game.buildTrack(game.getActivePid(), mileposts);
        expectedTotalSpent += 4;
        assertEquals(expectedTotalSpent, game.getTurnData().getSpending());
        
        // Build to a jungle
        mileposts = new MilepostId[]{ new MilepostId(2, 20), new MilepostId(3, 20), new MilepostId(3, 21) };
        game.buildTrack(game.getActivePid(), mileposts);
        expectedTotalSpent += 4;
        assertEquals(expectedTotalSpent, game.getTurnData().getSpending());
        
        // Build to a mountain, extended from current track end
        mileposts = new MilepostId[]{ new MilepostId(3, 21), new MilepostId(4, 22), new MilepostId(5, 22) };
        game.buildTrack(game.getActivePid(), mileposts);
        expectedTotalSpent += 3;
        assertEquals(expectedTotalSpent, game.getTurnData().getSpending());
           
        game.endTurn(game.getActivePid());
        
        // Check that the total was adjusted correctly
        assertEquals(accumulatedTotal - expectedTotalSpent, firstPlayer.getMoney());
        accumulatedTotal = firstPlayer.getMoney();
//        assertEquals(0, firstPlayer.getSpending());
        
        expectedTotalSpent = 0;
        
        // Build to an alpine milepost from Nairobi
        mileposts = new MilepostId[]{ new MilepostId(41, 35), new MilepostId(41, 36) };
        game.buildTrack(game.getActivePlayer().name, mileposts);
        expectedTotalSpent += 5;
        assertEquals(expectedTotalSpent, game.getTurnData().getSpending());	

        skipPastBuildingTurns(game);
        
        game.endGame("Louie", true);
        game.endGame("Huey", true);
	}

}
