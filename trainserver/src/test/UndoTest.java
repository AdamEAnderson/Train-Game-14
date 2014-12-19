package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import train.Game;
import train.GameException;
import train.TrainServer;

public class UndoTest extends GameTest{

	@Test
	public void testUndo() {
		Game game = null;
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
				};
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
		String gid = null;
		try {
			gid = newGame("Louie", "blue", "africa");
			game = TrainServer.getGame(gid);
	        game.joinGame("Xavier", "black");
	        startGame(game);
			skipPastBuildingTurns(game);
        	game.buildTrack(game.getActivePlayer().name, buildMileposts);
    		game = undo(gid, game.getActivePlayer().name);
        	game.buildTrack(game.getActivePlayer().name, buildMileposts);	// build was undone, so rebuild should work
			
			game.placeTrain(game.getActivePlayer().name, 0, new MilepostId(2,18));
    		game = undo(gid, game.getActivePlayer().name);
			game.placeTrain(game.getActivePlayer().name, 0, new MilepostId(2,18));
			game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
    		game = undo(gid, game.getActivePlayer().name);	// undo move
    		game = undo(gid, game.getActivePlayer().name);	// undo place
    		game = undo(gid, game.getActivePlayer().name);	// undo building
		} catch (GameException e) {
			fail("Unexpected exception");
		}
		try {
			game.undo();
			fail("Expected a NothingToUndo error");
		} catch (GameException e) {
			// expected case
		}
		try {
    		// we should be able to build and upgrade again
        	game.buildTrack(game.getActivePlayer().name, buildMileposts);	// build was undone, so rebuild should work
		} catch (GameException e) {
			fail("Unexpected exception");
		}
		try {
    		game = redo(gid, game.getActivePlayer().name);
			fail("Expected a NothingToRedo error");
		} catch (GameException e) {
			// expected case
		}
		try {
    		game = undo(gid, game.getActivePlayer().name);	// undo building
    		game = redo(gid, game.getActivePlayer().name);	// redo building
    		game = undo(gid, game.getActivePlayer().name);	// undo building
    		game = redo(gid, game.getActivePlayer().name);	// redo building
			game.placeTrain(game.getActivePlayer().name, 0, new MilepostId(2,18));
			game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
			game.endTurn(game.getActivePlayer().name);
		} catch (GameException e) {
				fail("Unexpected exception");
		}
		try {
    		game = undo(gid, game.getActivePlayer().name);
			fail("Expected a NothingToUndo error");
		} catch (GameException e) {
			// expected case
		}

		// Test undoing & redoing ferry building (requires special fixup)
        // Build to Tenarife
		MilepostId[] ferryMileposts = new MilepostId[] {
				new MilepostId(5,7),
				new MilepostId(0,6)		// Tenarife via ferry
				};
		try {
			game.endTurn(game.getActivePlayer().name);
			game.buildTrack(game.getActivePlayer().name, ferryMileposts);
    		game = undo(gid, game.getActivePlayer().name);	// undo building
			game.buildTrack(game.getActivePlayer().name, ferryMileposts);
    		game = undo(gid, game.getActivePlayer().name);	// undo building
    		game = redo(gid, game.getActivePlayer().name);	// redo building
    		game = undo(gid, game.getActivePlayer().name);	// undo building - check that we can undo the redo
		} catch (GameException e) {
			fail("Unexpected exception");
		}
	}

}
