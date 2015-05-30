package test;

import static org.junit.Assert.*;

import org.junit.Test;

import train.GameException;

public class ResumeTest extends GameTest {

	@Test
	public void testResume() {
		String gid = null;
		try {
			String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Louie\", \"color\":\"blue\", \"gameType\":\"africa\", \"name\":\"TestGame\"}";
	        String newGameData = trainServer.newGame(jsonPayload);
	        gid = newGameData.substring(8, 16);
			String resumePayload = String.format("{\"messageType\":\"resumeGame\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, "Louie");
	        String resumeGameData = trainServer.resumeGame(resumePayload);
	        assertEquals(newGameData, resumeGameData);
		} catch (GameException e) {
	    	fail("GameException not expected here");
	    }
		
		// Check that resuming a game with a bad player string throws an exception
		try {
			String resumePayload = String.format("{\"messageType\":\"resumeGame\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, "BADPLAYER");
			trainServer.resumeGame(resumePayload);
	    	fail("GameException expected -- should have thrown player not found");
		} catch (GameException e) {
	    }
	}
}
