package test;

import static org.junit.Assert.*;

import org.junit.Test;

import train.GameException;
import train.TrainServer;

public class GameDeletionTest extends GameTest{


	@Test
	public void testGameDeletion() {
		long expiration = 500;
		TrainServer.resetExpirations(expiration, expiration, expiration);

		// Test deletion of games created and not started
		try {
			String gid = newGame("Louie", "blue", "africa");
	        assertNotEquals(null, TrainServer.getGame(gid));
			Thread.sleep(expiration * 2);
			assertEquals(null, TrainServer.getGame(gid));
		} catch (GameException e) {
			fail("Unexpected exception");
		} catch (InterruptedException e) {
			
		}
		
		// Test deletion of ended games
		try {
			String gid = newGame("Louie", "blue", "africa");
	        assertNotEquals(null, TrainServer.getGame(gid));
	        endGame(TrainServer.getGame(gid));
			Thread.sleep(expiration * 2);
			assertEquals(null, TrainServer.getGame(gid));
		} catch (GameException e) {
			fail("Unexpected exception");
		} catch (InterruptedException e) {
			
		}

		// Test deletion of abandoned games
		try {
			String gid = newGame("Louie", "blue", "africa");
	        assertNotEquals(null, TrainServer.getGame(gid));
	        startGame(TrainServer.getGame(gid));
			Thread.sleep(expiration * 2);
			assertEquals(null, TrainServer.getGame(gid));
		} catch (GameException e) {
			fail("Unexpected exception");
		} catch (InterruptedException e) {
			
		}
		TrainServer.resetExpirations();
}

}
