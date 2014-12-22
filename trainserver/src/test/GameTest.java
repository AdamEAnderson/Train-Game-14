package test;

import static org.junit.Assert.*;

import map.MilepostId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.Player;
import train.Game;
import train.GameException;
import train.TrainServer;

//super-class with collected utility methods
public class GameTest {

	protected static Logger log = LoggerFactory.getLogger(GameTest.class);
	
	
	// Reverse the array (Arrays.sort can't be used because it doesn't work on primitive types)
	protected static void reverse(MilepostId[] b) {
	   int left  = 0;          // index of leftmost element
	   int right = b.length-1; // index of rightmost element
	  
	   while (left < right) {
	      // exchange the left and right elements
		  MilepostId temp = b[left]; 
	      b[left]  = b[right]; 
	      b[right] = temp;
	     
	      // move the bounds toward the center
	      left++;
	      right--;
	   }
	}
	
	protected Game undo(String gid, String pid) throws GameException {
    	String undoPayload = String.format("{\"messageType\":\"undo\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, pid);
		TrainServer.undo(undoPayload);
		return TrainServer.getGame(gid);
	}
	
	protected Game redo(String gid, String pid) throws GameException {
    	String redoPayload = String.format("{\"messageType\":\"redo\", \"gid\":\"%s\", \"pid\":\"%s\"}", gid, pid);
		TrainServer.redo(redoPayload);
		return TrainServer.getGame(gid);
	}
	
	protected void skipPastBuildingTurns(Game game) throws GameException {
        // Skip past building turns
        for(Player p = game.getActivePlayer(); game.getTurns() < 3; p = game.getActivePlayer()){
        	game.endTurn(p.name);
        	log.info("Active player is {}", p.name);
        	log.info("Turn count is {}", game.getTurns());
        }
	}
	
	protected void takeTurn(Game game, String playerName) throws GameException {
		if (!game.getActivePlayer().name.equals(playerName))
			fail("Too bad");
    	assertTrue(game.getActivePlayer().name.equals(playerName));
    	game.endTurn(playerName);

	}
	
	protected void startGame(Game game) throws GameException {
		for (Player p: game.getPlayers())
			game.startGame(p.name, true);
	}

	protected void endGame(Game game) throws GameException {
		for (Player p: game.getPlayers()) {
			String jsonPayload = String.format("{\"messageType\":\"endGame\", \"gid\":\"%s\", \"pid\":\"%s\", \"ready\":true}", 
				TrainServer.getGameId(game), p.name);
			TrainServer.endGame(jsonPayload);
		}
	}
	
	protected static String newGame(String pid, String color, String gameType) throws GameException {
		return newGame("TestGame", pid, color, gameType);
	}
	
	private static String newGame(String name, String pid, String color, String gameType) throws GameException {
		String jsonPayload = String.format("{\"messageType\":\"newGame\", \"pid\":\"%s\", \"color\":\"%s\", \"gameType\":\"%s\", \"name\":\"%s\"}", pid, color, gameType, name);
		String responseMessage = TrainServer.newGame(jsonPayload);
        //log.info("Got response message: {}", responseMessage);
        assertTrue(responseMessage.startsWith("{\"gid\":\""));
        String gid = responseMessage.substring(8, 16);
        return gid;
	}
	

}
