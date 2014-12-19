package test;

import static org.junit.Assert.*;

import org.junit.Test;

import train.Game;
import train.GameException;
import train.TrainServer;

public class ListTests extends GameTest{

	@Test
	public void testListGeographies() {
		try {
	        String request = String.format("{\"messageType\":\"listGeographies\"}");
	        String result = TrainServer.listGeographies(request);
	        log.info("listGeographies result {}", result);
	        assertTrue(result.startsWith("[\""));
	        assertTrue(result.contains("africa"));
		} catch (GameException e) {
			fail("Unexpected exception");
		} 
	}
	
	@Test
	public void testListColors() {
		try {
			String gid = newGame("Louie", "blue", "africa");
			Game game = TrainServer.getGame(gid);
	        game.joinGame("Tim", "black");
	        game.joinGame("Ann", "green");
	        game.joinGame("Julio", "red");
	        game.joinGame("Xavier", "mauve");
	        String request = String.format("{\"messageType\":\"listColors\", \"gid\":\"%s\"}", gid);
	        String result = TrainServer.listColors(request);
	        log.info("listColor result {}", result);
	        startGame(game);
	        String result2 = TrainServer.listColors(request);
	        log.info("listColor result {}", result2);
	        assertTrue(result.startsWith("[\""));
	        assertTrue(result2.startsWith("[\""));
		} catch (GameException e) {
			fail("Unexpected exception");
		} 
	}

}
