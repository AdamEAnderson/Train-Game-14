package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import train.Game;
import train.GameException;

public class StatusMsgTest extends GameTest {


	@Test
	public void testStatusMsg() throws GameException{
		String gid = newGame("Louie", "blue", "africa");
        String jsonRequestPayload = "{\"gid\":\"" + gid + "\"}";
        String statusMsg = trainServer.status(jsonRequestPayload);
        log.info("empty status message {}", statusMsg);
        Game game = trainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Huey", "green");
        game.joinGame("Esmeralda", "red");
        game.joinGame("Dewey", "purple");
        log.info("post-join status message {}", trainServer.status(jsonRequestPayload));
        game.startGame("Louie", true);
        game.startGame("Dewey", true);
        game.startGame("Esmeralda", true);
        game.startGame("Huey", true);
        log.info("status after starting the game {}", trainServer.status(jsonRequestPayload));
        
        String activePlayer = game.getActivePlayer().name;
        log.info("Active player is {}", activePlayer);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        log.info("post track building {}", trainServer.status(jsonRequestPayload));
	}
}
