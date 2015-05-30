package test;

import static org.junit.Assert.*;

import java.util.Set;

import map.MilepostId;

import org.junit.Test;

import train.Game;
import train.GameException;

public class BuildMultiPlayerTest extends GameTest {

	/** Test that if multi-player track is enabled, multiple
	 * players may build the same set of mileposts. */
	@Test
	public void testBuild() throws GameException {
		String ruleSet = ruleSet(4, 70, 1, true);
		String gid = newGame("TestGame", "Louie", "blue", "africa", ruleSet);
        Game game = trainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Huey", "green");
        game.startGame("Louie", true);
        game.startGame("Huey", true);
        
        // First player builds from Nairobi north
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(42, 33), new MilepostId(42, 32) };
        game.buildTrack(game.getActivePid(), mileposts);           
        game.endTurn(game.getActivePid());
        
        // Second player builds the same milepost
        game.buildTrack(game.getActivePlayer().name, mileposts);
        game.endTurn(game.getActivePid());

        // Check who is listed as building this pair of milepost
        Set<String> playersBuilt = game.getGlobalRail().getPlayers(mileposts[0],  mileposts[1]);
        assertEquals(2, playersBuilt.size());
        assertTrue(playersBuilt.contains("Louie"));
        assertTrue(playersBuilt.contains("Huey"));

        skipPastBuildingTurns(game);
        
        game.endGame("Louie", true);
        game.endGame("Huey", true);
	}

}
