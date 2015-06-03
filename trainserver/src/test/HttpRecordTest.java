package test;


import org.junit.Test;

import train.HttpTrainServer;

public class HttpRecordTest extends HttpTest {

	// Play a short game with two trains
	@Test
	public void testRecord() throws Exception {
		Thread serverThread = startServer();
		
		endRecording();

		String recordName = "httpRecordTest.tr";
		startRecording(recordName);
		
        String gid = newGame("Huey", "red");
        list("joinable", gid);	// the new game should appear in the list of joinable games
        joinGame(gid, "Louie", "green");
        startGame(gid, "Huey", true);
        startGame(gid, "Louie", true);
        String currentPlayer = getActivePlayer(gid);
        
        String mileposts = "[{\"x\":34,\"y\":58},{\"x\":33,\"y\":58},{\"x\":32,\"y\":58},{\"x\":31,\"y\":59}]";
        buildTrack(gid, currentPlayer, mileposts);
        
        // skip past building turns
        endTurn(gid, currentPlayer);
        upgradeTrain(gid, getActivePlayer(gid), "Speed");
        endTurn(gid, getActivePlayer(gid));
        endTurn(gid, getActivePlayer(gid));
        endTurn(gid, getActivePlayer(gid));
        endTurn(gid, getActivePlayer(gid));
        endTurn(gid, getActivePlayer(gid));
                
        placeTrain(gid, currentPlayer, 0, "{\"x\":34, \"y\":58}");

        String moveMileposts = "[ {\"x\":33,\"y\":58},{\"x\":32,\"y\":58},{\"x\":31,\"y\":59}]";
        pickupLoad(gid, currentPlayer, 0, "Diamonds");
        pickupLoad(gid, currentPlayer, 0, "Arms");
        moveTrain(gid, currentPlayer, currentPlayer, 0, moveMileposts);
        // deliverLoad(gid, currentPlayer, "turnips");
        dumpLoad(gid, currentPlayer, 0, "Arms");
        endTurn(gid, currentPlayer);
        
		endRecording();
		
        //restart server
        HttpTrainServer.stopServer();
        serverThread.join();
		serverThread = startServer();
		
		Thread.sleep(2000);

        playRecording(recordName);

        //endGame(gid, "Huey");
        //endGame(gid, "Louie");
        
        //join game
        HttpTrainServer.stopServer();
        serverThread.join();
		}

}
