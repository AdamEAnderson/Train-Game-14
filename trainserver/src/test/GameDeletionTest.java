package test;

import static org.junit.Assert.*;

import org.junit.Test;

import train.GameException;

public class GameDeletionTest extends GameTest{


	@Test
	public void testGameDeletion() {
		long expiration = 500;
		trainServer.resetExpirations(expiration, expiration, expiration);

		// Test deletion of games created and not started
		try {
			String gid = newGame("Louie", "blue", "africa");
	        assertNotEquals(null, trainServer.getGame(gid));
			Thread.sleep(expiration * 2);
			assertEquals(null, trainServer.getGame(gid));
		} catch (GameException e) {
			fail("Unexpected exception");
		} catch (InterruptedException e) {
			
		}
		
		// Test deletion of ended games
		try {
			String gid = newGame("Louie", "blue", "africa");
	        assertNotEquals(null, trainServer.getGame(gid));
	        endGame(trainServer.getGame(gid));
			Thread.sleep(expiration * 2);
			assertEquals(null, trainServer.getGame(gid));
		} catch (GameException e) {
			fail("Unexpected exception");
		} catch (InterruptedException e) {
			
		}

		// Test deletion of abandoned games
		try {
			String gid = newGame("Louie", "blue", "africa");
	        assertNotEquals(null, trainServer.getGame(gid));
	        startGame(trainServer.getGame(gid));
			Thread.sleep(expiration * 2);
			assertEquals(null, trainServer.getGame(gid));
		} catch (GameException e) {
			fail("Unexpected exception");
		} catch (InterruptedException e) {
			
		}
		trainServer.resetExpirations();
}

}
