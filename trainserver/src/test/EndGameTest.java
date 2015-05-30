package test;

import static org.junit.Assert.*;

import org.junit.Test;

import train.Game;
import train.GameException;

public class EndGameTest extends GameTest{

	@Test
	public void testEndGame() {
		Game game = null;
		try {
			String gid = newGame("Louie", "blue", "africa");
	        game = trainServer.getGame(gid);
	        endGame(game);
	        String jsonStatusPayload = "{\"gid\":\"" + gid + "\"}";
	        String statusMsg = trainServer.status(jsonStatusPayload);
	        log.info("endGame status message {}", statusMsg);
	        assertTrue(statusMsg.contains("\"ended\":true"));
	        assertTrue(statusMsg.contains("stats"));
		} catch (GameException e) {
			fail("Unexpected exception");
		}
	}
}
