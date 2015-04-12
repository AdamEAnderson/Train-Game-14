package test;

import static org.junit.Assert.*;

import org.junit.Test;

import train.Game;
import train.GameException;
import train.TrainServer;

public class OnePlayerGameTest extends GameTest {

	@Test
	public void testOnePlayerGame() throws GameException {
		String gid = newGame("Louie", "blue", "africa");
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.startGame("Louie", true);
        game.endTurn("Louie");
        game.endGame("Louie", true);
	}
}
