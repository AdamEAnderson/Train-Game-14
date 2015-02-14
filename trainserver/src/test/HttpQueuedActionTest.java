package test;

import static org.junit.Assert.*;

import org.junit.Test;

import train.HttpTrainServer;


public class HttpQueuedActionTest extends HttpTest {

	
	// Send a stream of requests to the server check the results
	@Test
	public void testQueuedActions() throws Exception {
		Thread serverThread = startServer();
		
        String huey = "Huey";
        String louie = "Louie";
        String gid = newGame(huey, "red");
        joinGame(gid, louie, "green");
        startGame(gid, huey, true);
        startGame(gid, louie, true);
        String firstPlayer = getActivePlayer(gid);
        String secondPlayer = getActivePlayer(gid).equals(huey) ? louie : huey;
        
        String mileposts = "[{\"x\":34,\"y\":58},{\"x\":33,\"y\":58},{\"x\":32,\"y\":58},{\"x\":31,\"y\":59}]";
        buildTrack(gid, secondPlayer, mileposts);	// queued action
        endTurn(gid, firstPlayer);
                	
        endTurn(gid, secondPlayer);		// endTurn should have triggered the building action

        upgradeTrain(gid, firstPlayer, "Speed");	// queued action

        // turnaround
        endTurn(gid, secondPlayer);
        
        // skip past building turns
        endTurn(gid, firstPlayer);
        endTurn(gid, firstPlayer);
        endTurn(gid, secondPlayer);

        placeTrain(gid, secondPlayer, 0, "{\"x\":34, \"y\":58}");	// queued action

        String moveMileposts = "[ {\"x\":33,\"y\":58},{\"x\":32,\"y\":58},{\"x\":31,\"y\":59}]";
        pickupLoad(gid, secondPlayer, 0, "Diamonds");
        pickupLoad(gid, secondPlayer, 0, "Arms");
        moveTrain(gid, secondPlayer, secondPlayer, 0, moveMileposts);
        // deliverLoad(gid, currentPlayer, "turnips");
        dumpLoad(gid, secondPlayer, 0, "Arms");
        endTurn(gid, firstPlayer);
        endTurn(gid, secondPlayer);		// should force queued actions
        endGame(gid, huey);
        endGame(gid, louie);
        
        //join game
        HttpTrainServer.stopServer();
        serverThread.join();
    }

}
