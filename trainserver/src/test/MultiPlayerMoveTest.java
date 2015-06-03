package test;


import org.junit.Test;

import train.HttpTrainServer;

public class MultiPlayerMoveTest extends HttpTest {

	// Play a short game with two trains
	@Test
	public void testMove() throws Exception {
		Thread serverThread = startServer();
		
		endRecording();

        playRecording("testMultiPlayerMove.tr");

        //join game
        HttpTrainServer.stopServer();
        serverThread.join();
		}
}
