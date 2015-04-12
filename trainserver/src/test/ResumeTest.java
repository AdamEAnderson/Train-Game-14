package test;

import static org.junit.Assert.*;

import org.junit.Test;

import train.GameException;
import train.TrainServer;

public class ResumeTest extends GameTest {

	@Test
	public void testResume() {
		String gid = null;
		try {
			String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Louie\", \"color\":\"blue\", \"gameType\":\"africa\", \"name\":\"TestGame\"}";
	        String newGameData = TrainServer.newGame(jsonPayload);
	        gid = newGameData.substring(8, 16);
			String resumePayload = String.format("{\"messageType\":\"resumeGame\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, "Louie");
	        String resumeGameData = TrainServer.resumeGame(resumePayload);
	        assertEquals(newGameData, resumeGameData);
		} catch (GameException e) {
	    	fail("GameException not expected here");
	    }
		
		// Check that resuming a game with a bad player string throws an exception
		try {
			String resumePayload = String.format("{\"messageType\":\"resumeGame\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, "BADPLAYER");
	        TrainServer.resumeGame(resumePayload);
	    	fail("GameException expected -- should have thrown player not found");
		} catch (GameException e) {
	    }
	}
}
