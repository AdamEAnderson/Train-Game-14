package test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import map.MilepostId;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import player.Player;
import reference.Card;
import reference.Trip;
import reference.UpgradeType;
import train.Game;
import train.GameException;
import train.TrainServer;


public class GameTest {

	private static Logger log = LoggerFactory.getLogger(GameTest.class);
	
	// Send a stream of requests to the server check the results
	@Test
	public void testTrain() throws Exception {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
        String responseMessage = TrainServer.newGame(jsonPayload);
        log.info("newGame response {}", responseMessage);
        String gid = responseMessage.substring(8, 16);
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Sandra", "green");
        game.joinGame("Sandy", "red");
        game.joinGame("Robin", "purple");
        game.startGame("Adam", true);
        game.startGame("Robin", true);
        game.startGame("Sandy", true);
        game.startGame("Sandra", true);
        
        String activePlayer = game.getActivePlayer().name;
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        
        game.placeTrain(activePlayer, 0, new MilepostId(34,58));	// Johannesburg!
        // Stack the player's hand with cards we can deliver
        Trip[] trips = new Trip[3];
        trips[0] = new Trip("Kimberley", "Diamonds", 12);
        trips[1] = new Trip("Asmera", "Corn", 42);
        trips[2] = new Trip("Douala", "Books", 45);
        int handSize = game.getRuleSet().handSize;
        Card cards[] = new Card[handSize];
        for (int i = 0; i < handSize; ++i)
        	cards[i] = new Card(trips);
        game.getActivePlayer().turnInCards(cards);
        
        skipPastBuildingTurns(game);

        game.pickupLoad(activePlayer, 0, "Diamonds");
        game.pickupLoad(activePlayer, 0, "Arms");
        mileposts = new MilepostId[]{ new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.moveTrain(activePlayer, 0, mileposts);				// arrive in Kimberley
        game.deliverLoad(activePlayer, 0, "Diamonds", 0);
        game.dumpLoad(activePlayer, 0, "Arms");
        game.endTurn(activePlayer);
        game.endTurn(game.getActivePlayer().name);
        game.endTurn(game.getActivePlayer().name);
        game.endGame("Adam", true);
        game.endGame("Robin", true);
        game.endGame("Sandy", true);
        game.endGame("Sandra", true);
    }
	
	@Test
	public void testResume() {
		String gid = null;
		try {
			String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Louie\", \"color\":\"blue\", \"gameType\":\"africa\"}";
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
	
	
	// Upgrade to a 3-hauler
	@Test
	public void testUpgradeCapacity() throws Exception {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
        String responseMessage = TrainServer.newGame(jsonPayload);
        log.info("newGame response {}", responseMessage);
        String gid = responseMessage.substring(8, 16);
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Sandra", "green");
        game.joinGame("Sandy", "red");
        game.joinGame("Robin", "purple");
        game.startGame("Adam", true);
        game.startGame("Robin", true);
        game.startGame("Sandy", true);
        game.startGame("Sandra", true);
        
        String activePlayer = game.getActivePlayer().name;
        log.info("Active player is {}", activePlayer);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        game.endTurn(activePlayer);
        game.endTurn(game.getActivePlayer().name);	// skip over other players
        game.endTurn(game.getActivePlayer().name);
        game.endTurn(game.getActivePlayer().name);
        activePlayer = game.getActivePlayer().name;
        game.upgradeTrain(activePlayer, 0, UpgradeType.CAPACITY);	// upgrade during building turn
        game.endTurn(game.getActivePlayer().name);
        skipPastBuildingTurns(game);
        
        game.upgradeTrain(game.getActivePlayer().name, 0, UpgradeType.SPEED);	// upgrade during standard turn

        game.endGame("Adam", true);
        game.endGame("Robin", true);
        game.endGame("Sandy", true);
        game.endGame("Sandra", true);
    }
	
	// Play a short game with two trains
	@Test
	public void testTwoTrain() throws Exception {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"ruleSet\":{\"handSize\":4, \"startingMoney\":100, \"numTrains\":2}, \"gameType\":\"africa\"}";
        String responseMessage = TrainServer.newGame(jsonPayload);
        log.info("newGame response {}", responseMessage);
        String gid = responseMessage.substring(8, 16);
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Sandra", "green");
        game.startGame("Adam", true);
        game.startGame("Sandra", true);
        
        String activePlayer = game.getActivePlayer().name;
        log.info("Active player is {}", activePlayer);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        assertTrue(game.getActivePlayer().getSpending() == 5);
        game.placeTrain(activePlayer, 0, new MilepostId(34,58));	// Johannesburg!
        game.placeTrain(activePlayer, 1, new MilepostId(31, 59));	// Kimberley!
        game.endTurn(game.getActivePlayer().name);

        skipPastBuildingTurns(game);
        
        log.info("Active player is {}", game.getActivePlayer().name);

        // Stack the player's hand with cards we can deliver
        Trip[] trips = new Trip[3];
        trips[0] = new Trip("Kimberley", "Diamonds", 12);
        trips[1] = new Trip("Johannesburg", "Gold", 6);
        trips[2] = new Trip("Johannesburg", "Uranium", 8);
        int handSize = game.getRuleSet().handSize;
        Card cards[] = new Card[handSize];
        for (int j = 0; j < handSize; ++j)
        	cards[j] = new Card(trips);
        game.getActivePlayer().turnInCards(cards);
        game.pickupLoad(activePlayer, 0, "Diamonds");
        game.pickupLoad(activePlayer, 0, "Arms");
        game.pickupLoad(activePlayer, 1, "Gold");
        game.pickupLoad(activePlayer, 1, "Uranium");
        mileposts = new MilepostId[]{ new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.moveTrain(activePlayer, 0, mileposts);				// arrive in Kimberley
        game.deliverLoad(activePlayer, 0, "Diamonds", 0);
        game.dumpLoad(activePlayer, 0, "Arms");
        mileposts = new MilepostId[]{ new MilepostId(32, 58), new MilepostId(33, 58), 
        		new MilepostId(34,58) };
        game.moveTrain(activePlayer, 1, mileposts);				// arrive in Johannesburg
        game.deliverLoad(activePlayer, 1, "Gold", 1);
        game.deliverLoad(activePlayer, 1, "Uranium", 2);
        Player movingPlayer = game.getActivePlayer();
        game.endTurn(activePlayer);
        assertTrue(movingPlayer.getMoney() == 121);	// total should includes deliveries - building
        game.endTurn(game.getActivePlayer().name);
        game.endTurn(game.getActivePlayer().name);
        game.endGame("Adam", true);
        game.endGame("Sandra", true);
    }
	
	@Test
	public void testOnePlayerGame() throws GameException {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
        String responseMessage = TrainServer.newGame(jsonPayload);
        log.info("newGame response {}", responseMessage);
        String gid = responseMessage.substring(8, 16);
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.startGame("Adam", true);
        game.endTurn("Adam");
        game.endGame("Adam", true);
	}
	
	@Test
	public void testResign() {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
		try {
			String responseMessage = TrainServer.newGame(jsonPayload);
	        log.info("newGame response {}", responseMessage);
	        String gid = responseMessage.substring(8, 16);
	        Game game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Sandra", "green");
	        game.startGame("Sandra", true);
	        game.startGame("Adam", true);
	        skipPastBuildingTurns(game);
	        String resignedPlayer = game.getActivePlayer().name;
	        game.resign(resignedPlayer);
	        
	        // Player now should be the other one
	        assertFalse(resignedPlayer.equals(game.getActivePlayer().name));
	        game.endTurn(game.getActivePlayer().name);
	        // Player should still be the other one
	        assertFalse(resignedPlayer.equals(game.getActivePlayer().name));
	        
	        game.endGame("Adam", true);
	        game.endGame("Sandra", true);
		} catch (GameException e) {
			e.printStackTrace();
			fail("Unexpected GameException");
		}
	}
	
	/** Test that normal building works as expected */
	@Test
	public void testBuild() throws GameException {
		int expectedTotalSpent = 0;
		int accumulatedTotal = 70;
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
        String responseMessage = TrainServer.newGame(jsonPayload);
        log.info("newGame response {}", responseMessage);
        String gid = responseMessage.substring(8, 16);
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Sandra", "green");
        game.startGame("Adam", true);
        game.startGame("Sandra", true);
        
        Player firstPlayer = game.getActivePlayer();

        // First player builds from Cairo to Luxor - check building into a city & over a river
        log.info("Active player is {}", game.getActivePlayer().name);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(37, 8), new MilepostId(37, 9), new MilepostId(38, 10) };
        game.buildTrack(game.getActivePlayer().name, mileposts);
        expectedTotalSpent += 6;	// Incremental cost of 6
        assertEquals(expectedTotalSpent, game.getActivePlayer().getSpending());
        
        // Build into minor & major cities
        // Build back into Cairo from Luxor
        // Tests that player can build from end of their track into major city
        mileposts = new MilepostId[]{ new MilepostId(38, 10), new MilepostId(37, 10), new MilepostId(36, 9), new MilepostId(36, 8) };
        game.buildTrack(game.getActivePlayer().name, mileposts);
        expectedTotalSpent += 3;
        assertEquals(expectedTotalSpent, game.getActivePlayer().getSpending());
        
        // Sea inlet crossing - build from Cairo to the Sinai
        mileposts = new MilepostId[]{ new MilepostId(37, 7), new MilepostId(38, 6) };
        game.buildTrack(game.getActivePlayer().name, mileposts);
        expectedTotalSpent += 4;
        assertEquals(expectedTotalSpent, game.getActivePlayer().getSpending());
        
        // Build to a jungle
        mileposts = new MilepostId[]{ new MilepostId(2, 20), new MilepostId(3, 20), new MilepostId(3, 21) };
        game.buildTrack(game.getActivePlayer().name, mileposts);
        expectedTotalSpent += 4;
        assertEquals(expectedTotalSpent, game.getActivePlayer().getSpending());
        
        // Build to a mountain, extended from current track end
        mileposts = new MilepostId[]{ new MilepostId(3, 21), new MilepostId(4, 22), new MilepostId(5, 22) };
        game.buildTrack(game.getActivePlayer().name, mileposts);
        expectedTotalSpent += 3;
        assertEquals(expectedTotalSpent, game.getActivePlayer().getSpending());
           
        game.endTurn(game.getActivePlayer().name);
        
        // Check that the total was adjusted correctly
        assertEquals(accumulatedTotal - expectedTotalSpent, firstPlayer.getMoney());
        accumulatedTotal = firstPlayer.getMoney();
        assertEquals(0, firstPlayer.getSpending());
        
        expectedTotalSpent = 0;
        
        // Build to an alpine milepost from Nairobi
        mileposts = new MilepostId[]{ new MilepostId(41, 35), new MilepostId(41, 36) };
        game.buildTrack(game.getActivePlayer().name, mileposts);
        expectedTotalSpent += 5;
        assertEquals(expectedTotalSpent, game.getActivePlayer().getSpending());	

        skipPastBuildingTurns(game);
        
        game.endGame("Adam", true);
        game.endGame("Sandra", true);
	}
	
	/** Test building too much (cost more than 20) */
	@Test
	public void testBuildOverCost() {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
		Game game = null;;
		try {
			String responseMessage = TrainServer.newGame(jsonPayload);
	        String gid = responseMessage.substring(8, 16);
	        game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.startGame("Adam", true);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		} 
        
        // Build more than 20
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ 
        	new MilepostId(34,56),
        	new MilepostId(33,55),
	        new MilepostId(33,54),
	        new MilepostId(32,53),
	        new MilepostId(31,53),
	        new MilepostId(31,52),
	        new MilepostId(30,52),
	        new MilepostId(29,52),
	        new MilepostId(28,52),
	        new MilepostId(27,52),
	        new MilepostId(26,51),
	        new MilepostId(26,50),
	        new MilepostId(25,49),
	        new MilepostId(25,48),
	        new MilepostId(24,47),
	        new MilepostId(25,46),
	        new MilepostId(25,45)
        };
        try {
        	game.buildTrack(game.getActivePlayer().name, mileposts);
        	fail("Build track should have thrown");
        } catch (GameException e) {
        }

		try {
	        game.endGame("Adam", true);
		} catch (GameException e) {
			fail("Unexpected exception in test cleanup");
		} 
	}
	
	/** Test that building from a place that is not a mojor city, and not already on the player's track fails */
	@Test
	public void testBuildFromUnconnectedTrack()  {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
		Game game = null;;
		try {
			String responseMessage = TrainServer.newGame(jsonPayload);
	        String gid = responseMessage.substring(8, 16);
	        game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Sandra", "green");
	        game.startGame("Adam", true);
	        game.startGame("Sandra", true);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		} 
        
        // First player builds from Luxor to Cairo - should fail because player hasn't build to Luxor
        log.info("Active player is {}", game.getActivePlayer().name);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(38, 10), new MilepostId(37, 10), new MilepostId(36, 9), new MilepostId(36, 8) };
        try {
        	game.buildTrack(game.getActivePlayer().name, mileposts);
        	fail("Build track should have thrown");
        } catch (GameException e) {
        }

		try {
	        game.endGame("Adam", true);
	        game.endGame("Sandra", true);
		} catch (GameException e) {
			fail("Unexpected exception in test cleanup");
		} 
	}
	
	/** Test that building over track that has already been built fails */
	@Test
	public void testBuildOverExistingTrack()  {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
		Game game = null;;
		try {
			String responseMessage = TrainServer.newGame(jsonPayload);
	        String gid = responseMessage.substring(8, 16);
	        game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Sandra", "green");
	        game.startGame("Adam", true);
	        game.startGame("Sandra", true);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		} 
        
        // First player builds from Luxor to Cairo - should fail because player hasn't build to Luxor
        log.info("Active player is {}", game.getActivePlayer().name);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(37, 8), new MilepostId(37, 9), new MilepostId(38, 10) };
        try {
        	game.buildTrack(game.getActivePlayer().name, mileposts);
        	game.endTurn(game.getActivePlayer().name);
        	game.buildTrack(game.getActivePlayer().name, mileposts);	// next player builds same track
        	fail("Build track should have thrown");
        } catch (GameException e) {
        }

		try {
	        game.endGame("Adam", true);
	        game.endGame("Sandra", true);
		} catch (GameException e) {
			fail("Unexpected exception in test cleanup");
		} 
	} 
	
	@Test
	public void testStatusMsg() throws GameException{
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
        String responseMessage = TrainServer.newGame(jsonPayload);
        log.info("newGame response {}", responseMessage);
        String gid = responseMessage.substring(8, 16);
        String jsonRequestPayload = "{\"gid\":\"" + gid + "\"}";
        String statusMsg = TrainServer.status(jsonRequestPayload);
        log.info("empty status message {}", statusMsg);
        Game game = TrainServer.getGame(gid);
        assertTrue(game != null);
        game.joinGame("Sandra", "green");
        game.joinGame("Sandy", "red");
        game.joinGame("Robin", "purple");
        log.info("post-join status message {}", TrainServer.status(jsonRequestPayload));
        game.startGame("Adam", true);
        game.startGame("Robin", true);
        game.startGame("Sandy", true);
        game.startGame("Sandra", true);
        log.info("status after starting the game {}", TrainServer.status(jsonRequestPayload));
        
        String activePlayer = game.getActivePlayer().name;
        log.info("Active player is {}", activePlayer);
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ new MilepostId(34, 58), new MilepostId(33, 58), new MilepostId(32, 58),
            	new MilepostId(31, 59) };
        game.buildTrack(activePlayer, mileposts);
        log.info("post track building {}", TrainServer.status(jsonRequestPayload));
	}
	
	/** Test standard move */
	@Test
	public void testMove() {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
		Game game = null;;
		try {
			String responseMessage = TrainServer.newGame(jsonPayload);
	        String gid = responseMessage.substring(8, 16);
	        game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.startGame("Adam", true);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		} 
        
        // Build 
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ 
        	new MilepostId(34,56),
        	new MilepostId(33,55),
	        new MilepostId(33,54),
	        new MilepostId(32,53),
	        new MilepostId(31,53),
	        new MilepostId(31,52),
	        new MilepostId(30,52),
	        new MilepostId(29,52),
	        new MilepostId(28,52),
	        new MilepostId(27,52),
	        new MilepostId(26,51),
	        new MilepostId(26,50),
	        new MilepostId(25,49),
	        new MilepostId(25,48),
	        new MilepostId(24,47),
	        new MilepostId(25,46),
        };
        MilepostId[] moveMileposts;
        moveMileposts = new MilepostId[]{ 
        	new MilepostId(33,55),
	        new MilepostId(33,54),
	        new MilepostId(32,53),
	        new MilepostId(31,53),
	        new MilepostId(31,52),
	        new MilepostId(30,52),
	        new MilepostId(29,52),
	        new MilepostId(28,52),
	        new MilepostId(27,52),
	        new MilepostId(26,51),
	        new MilepostId(26,50),
	        new MilepostId(25,49),
        };

		try {
        	game.buildTrack(game.getActivePlayer().name, mileposts);
			skipPastBuildingTurns(game);
			game.placeTrain("Adam", 0, new MilepostId(34,56));
			game.moveTrain("Adam", 0, moveMileposts);
	        game.endGame("Adam", true);
		} catch (GameException e) {
			log.error("Unexpected exception {}", e);
			fail("Unexpected exception");
		} 
	}
	
	/** Test move error, move further than train can go in one turn */
	@Test
	public void testMoveTooFar() {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Adam\", \"color\":\"blue\", \"gameType\":\"africa\"}";
		Game game = null;;
		try {
			String responseMessage = TrainServer.newGame(jsonPayload);
	        String gid = responseMessage.substring(8, 16);
	        game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.startGame("Adam", true);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		} 
        
        // Build 
        MilepostId[] mileposts;
        mileposts = new MilepostId[]{ 
        	new MilepostId(34,56),
        	new MilepostId(33,55),
	        new MilepostId(33,54),
	        new MilepostId(32,53),
	        new MilepostId(31,53),
	        new MilepostId(31,52),
	        new MilepostId(30,52),
	        new MilepostId(29,52),
	        new MilepostId(28,52),
	        new MilepostId(27,52),
	        new MilepostId(26,51),
	        new MilepostId(26,50),
	        new MilepostId(25,49),
	        new MilepostId(25,48),
	        new MilepostId(24,47),
	        new MilepostId(25,46),
        };
        MilepostId[] moveMileposts;
        moveMileposts = new MilepostId[]{ 
        	new MilepostId(33,55),
	        new MilepostId(33,54),
	        new MilepostId(32,53),
	        new MilepostId(31,53),
	        new MilepostId(31,52),
	        new MilepostId(30,52),
	        new MilepostId(29,52),
	        new MilepostId(28,52),
	        new MilepostId(27,52),
	        new MilepostId(26,51),
	        new MilepostId(26,50),
	        new MilepostId(25,49),
	        new MilepostId(25,48),
	        new MilepostId(24,47),
	        new MilepostId(25,46),
        };

		try {
        	game.buildTrack(game.getActivePlayer().name, mileposts);
			skipPastBuildingTurns(game);
			game.placeTrain("Adam", 0, new MilepostId(34,56));
		} catch (GameException e) {
			log.error("Unexpected exception {}", e);
			fail("Unexpected exception");
		} 
		try {
			game.moveTrain("Adam", 0, moveMileposts);
			fail("Expected move error from moving too far");
		} catch (GameException e) {
		} 
		try {
	        game.endGame("Adam", true);
		} catch (GameException e) {
			log.error("Unexpected exception {}", e);
			fail("Unexpected exception");
		} 
	}
	
	/** Test moving a train without having placed it */
	@Test
	public void testMoveNoPlace() {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Julie\", \"color\":\"blue\", \"gameType\":\"africa\"}";
		Game game = null;
		try {
			String responseMessage = TrainServer.newGame(jsonPayload);
	        String gid = responseMessage.substring(8, 16);
	        game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Tim", "black");
	        game.joinGame("Ann", "green");
	        startGame(game);

	        // First player builds from Dakar to Abidjan
	        MilepostId[] buildMileposts;
	        buildMileposts = new MilepostId[]{ 
	        	new MilepostId(2,20),
	        	new MilepostId(2,21),
	        	new MilepostId(3,22),
	        	new MilepostId(3,23),
	        	new MilepostId(4,24),
	        	new MilepostId(4,25),
	        	new MilepostId(5,26),
	        	new MilepostId(5,27),
	        	new MilepostId(6,27),
	        	new MilepostId(7,27),
	        	new MilepostId(8,27), 
	        	new MilepostId(9,27), 
	        	//new MilepostId(10,27),
	        };
        	game.buildTrack(game.getActivePlayer().name, buildMileposts);
        	game.endTurn(game.getActivePlayer().name);

        	skipPastBuildingTurns(game);
        	
        	// Move from Abidjan, skip placing train
        	//game.placeTrain(game.getActivePlayer().name, 0, new MilepostId(10,27));
	        MilepostId[] moveMileposts;
	        moveMileposts = new MilepostId[]{ 
	        	new MilepostId(10,27),
	        	new MilepostId(9,27), 
	        	new MilepostId(8,27), 
	        	new MilepostId(7,27),
	        	new MilepostId(6,27),
	        	new MilepostId(5,27),
	        	new MilepostId(5,26),
	        	new MilepostId(4,25),
	        	new MilepostId(4,24),
	        	new MilepostId(3,23),
	        	new MilepostId(3,22),
	        	new MilepostId(2,21),
	        };
        	game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
			fail("Expected invalid move exception");
		} catch (GameException e) {
		} 
	}
	
	@Test
	public void testRental() {
		String jsonPayload = "{\"messageType\":\"newGame\", \"pid\":\"Julie\", \"color\":\"blue\", \"gameType\":\"africa\"}";
		Game game = null;
		try {
			String responseMessage = TrainServer.newGame(jsonPayload);
	        String gid = responseMessage.substring(8, 16);
	        game = TrainServer.getGame(gid);
	        assertTrue(game != null);
	        game.joinGame("Tim", "black");
	        game.joinGame("Ann", "green");
	        startGame(game);

	        // First player builds from Dakar to Abidjan
	        MilepostId[] buildMileposts;
	        buildMileposts = new MilepostId[]{ 
	        	new MilepostId(2,20),
	        	new MilepostId(2,21),
	        	new MilepostId(3,22),
	        	new MilepostId(3,23),
	        	new MilepostId(4,24),
	        	new MilepostId(4,25),
	        	new MilepostId(5,26),
	        	new MilepostId(5,27),
	        	new MilepostId(6,27),
	        	new MilepostId(7,27),
	        	new MilepostId(8,27), 
	        	new MilepostId(9,27), 
	        	//new MilepostId(10,27),
	        };
        	game.buildTrack(game.getActivePlayer().name, buildMileposts);
        	game.endTurn(game.getActivePlayer().name);

        	// Second player builds from Dakar to Kano
        	buildMileposts = new MilepostId[] {
    			new MilepostId(2,20),
    			new MilepostId(3,20),
    			new MilepostId(4,20),
    			new MilepostId(5,20),
    			new MilepostId(6,20),
    			new MilepostId(7,20),
    			new MilepostId(8,20),
    			new MilepostId(9,20),
    			new MilepostId(10,20),
    			new MilepostId(11,20),
    			new MilepostId(12,20),
    			new MilepostId(13,20),
    			new MilepostId(13,21),
    			new MilepostId(14,21),
    		};
        	game.buildTrack(game.getActivePlayer().name, buildMileposts);
        	game.endTurn(game.getActivePlayer().name);

        	// Third player upgrades for speed
        	game.upgradeTrain(game.getActivePlayer().name, 0, UpgradeType.SPEED);
        	game.endTurn(game.getActivePlayer().name);
        	
        	// Turnaround turn - third player upgrades for speed again
        	game.upgradeTrain(game.getActivePlayer().name, 0, UpgradeType.SPEED);
        	game.endTurn(game.getActivePlayer().name);

        	// Second player completes track to Kano
        	buildMileposts = new MilepostId[] {
    			new MilepostId(14,21),
    			new MilepostId(15,21),
    			new MilepostId(16,21),
    			new MilepostId(17,21),
    			new MilepostId(18,21),
    			new MilepostId(19,21),
    			new MilepostId(20,21)
    		};
        	game.buildTrack(game.getActivePlayer().name, buildMileposts);
        	game.endTurn(game.getActivePlayer().name);

        	// First player completes track to Abidjan
        	buildMileposts = new MilepostId[] {
	        	new MilepostId(9,27), 
	        	new MilepostId(10,27)
        	};
        	game.buildTrack(game.getActivePlayer().name, buildMileposts);
        	game.endTurn(game.getActivePlayer().name);

        	skipPastBuildingTurns(game);
        	
        	// First player starts train in Abidjan, heads to Dakar
        	game.placeTrain(game.getActivePlayer().name, 0, new MilepostId(10,27));
	        MilepostId[] moveMileposts;
	        moveMileposts = new MilepostId[]{ 
	        	new MilepostId(10,27),
	        	new MilepostId(9,27), 
	        	new MilepostId(8,27), 
	        	new MilepostId(7,27),
	        	new MilepostId(6,27),
	        	new MilepostId(5,27),
	        	new MilepostId(5,26),
	        	new MilepostId(4,25),
	        	new MilepostId(4,24),
	        	new MilepostId(3,23),
	        	new MilepostId(3,22),
	        	new MilepostId(2,21),
	        	//new MilepostId(2,20),
	        };
	        Player player1 = game.getActivePlayer();
        	game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
        	game.endTurn(game.getActivePlayer().name);
        	
        	// Second player starts in Kano, heads to Dakar
        	moveMileposts = new MilepostId[] {
        			new MilepostId(9,20),
        			new MilepostId(10,20),
        			new MilepostId(11,20),
        			new MilepostId(12,20),
        			new MilepostId(13,20),
        			new MilepostId(13,21),
        			new MilepostId(14,21),
        			new MilepostId(15,21),
        			new MilepostId(16,21),
        			new MilepostId(17,21),
        			new MilepostId(18,21),
        			new MilepostId(19,21),
    		};
        	Arrays.sort(moveMileposts, Collections.reverseOrder());
	        Player player2 = game.getActivePlayer();
	        game.placeTrain(game.getActivePlayer().name, 0, new MilepostId(20, 21));
        	game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
        	game.upgradeTrain(game.getActivePlayer().name, 0, UpgradeType.SPEED);
        	game.endTurn(game.getActivePlayer().name);
        	
        	// Third player starts in Kano, heads to Dakar
        	// Rents from second player
        	// Check that third player pays rent, and second player receives rent
        	moveMileposts = new MilepostId[] {
    			//new MilepostId(2,20),
    			new MilepostId(3,20),
    			new MilepostId(4,20),
    			new MilepostId(5,20),
    			new MilepostId(6,20),
    			new MilepostId(7,20),
    			new MilepostId(8,20),
    			new MilepostId(9,20),
    			new MilepostId(10,20),
    			new MilepostId(11,20),
    			new MilepostId(12,20),
    			new MilepostId(13,20),
    			new MilepostId(13,21),
    			new MilepostId(14,21),
    			new MilepostId(15,21),
    			new MilepostId(16,21),
    			new MilepostId(17,21),
    			new MilepostId(18,21),
    			new MilepostId(19,21),
    		};
        	Player player3 = game.getActivePlayer();
        	int renterMoneyAtStartOfTurn = player3.getMoney();
        	int landlordMoneyAtStartOfTurn = player2.getMoney();
        	Arrays.sort(moveMileposts, Collections.reverseOrder());
        	game.placeTrain(game.getActivePlayer().name, 0, new MilepostId(20,21));
        	game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
        	game.endTurn(game.getActivePlayer().name);
        	assertEquals(renterMoneyAtStartOfTurn - 4, player3.getMoney());	// check that rent was deducted from total
        	assertEquals(landlordMoneyAtStartOfTurn + 4, player2.getMoney()); // check that rent money is received

        	// First player starts on their own track, moves off and rents from second player
	        moveMileposts = new MilepostId[]{ 
	        	new MilepostId(2,20),
    			new MilepostId(3,20),
    			new MilepostId(4,20),
    			new MilepostId(5,20),
    			new MilepostId(6,20),
    			new MilepostId(7,20),
    			new MilepostId(8,20),
		        };
        	renterMoneyAtStartOfTurn = player1.getMoney();
        	landlordMoneyAtStartOfTurn = player2.getMoney();
        	game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
        	game.endTurn(game.getActivePlayer().name);
        	assertEquals(renterMoneyAtStartOfTurn - 4, player1.getMoney());	// check that rent was deducted from total
        	assertEquals(landlordMoneyAtStartOfTurn + 4, player2.getMoney()); // check that rent money is received
        	
        	// Second player continues to Dakar, heads towards Abidjan on player1's track, then returns to own track
        	moveMileposts = new MilepostId[] {
    			new MilepostId(8,20),
    			new MilepostId(7,20),
    			new MilepostId(6,20),
    			new MilepostId(5,20),
    			new MilepostId(4,20),
    			new MilepostId(3,20),
    			new MilepostId(2,20),	 // Dakar!
	        	new MilepostId(2,21),
	        	new MilepostId(3,22),
	        	new MilepostId(2,21),
    			new MilepostId(2,20),	 // Dakar!
    			new MilepostId(3,20),
    			new MilepostId(2,20),	 // Dakar!
	        	new MilepostId(2,21),
	        	new MilepostId(3,22),
        		};
        	renterMoneyAtStartOfTurn = player2.getMoney();
        	landlordMoneyAtStartOfTurn = player1.getMoney();
        	game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
        	game.endTurn(game.getActivePlayer().name);
        	assertEquals(renterMoneyAtStartOfTurn - 4, player2.getMoney());	// check that rent was deducted from total
        	assertEquals(landlordMoneyAtStartOfTurn + 4, player1.getMoney()); // check that rent money is received
        	
        	// Third player starts on player2's track, goes to Dakar, and heads out to Abidjan on player1'a track
        	// Test rental to 2 different players in one turn
        	moveMileposts = new MilepostId[] {
    			new MilepostId(2,20),
	        	new MilepostId(2,21),
	        	new MilepostId(3,22),
	        	new MilepostId(3,23),
	        	new MilepostId(4,24),
	        	new MilepostId(4,25),
	        	new MilepostId(5,26),
	        	new MilepostId(5,27),
	        	new MilepostId(6,27),
	        	new MilepostId(7,27),
	        	new MilepostId(8,27), 
	        	new MilepostId(9,27), 
	        	new MilepostId(10,27),
        	};
        	int player1MoneyAtStartOfTurn = player1.getMoney();
        	int player2MoneyAtStartOfTurn = player2.getMoney();
        	renterMoneyAtStartOfTurn = player3.getMoney();
        	game.moveTrain(game.getActivePlayer().name, 0, moveMileposts);
        	game.endTurn(game.getActivePlayer().name);
        	assertEquals(renterMoneyAtStartOfTurn - 8, player3.getMoney());	// check that rent was deducted from total
        	assertEquals(player1MoneyAtStartOfTurn + 4, player1.getMoney()); // check that rent money is received
        	assertEquals(player2MoneyAtStartOfTurn + 4, player2.getMoney()); // check that rent money is received

        	endGame(game);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		}
	} 
	
	private void skipPastBuildingTurns(Game game) throws GameException {
        // Skip past building turns
        for(Player p = game.getActivePlayer(); game.getTurns() < 3; p = game.getActivePlayer()){
        	game.endTurn(p.name);
        	log.info("Active player is {}", p.name);
        	log.info("Turn count is {}", game.getTurns());
        }
	}
	
	private void startGame(Game game) throws GameException {
		for (Player p: game.getPlayers())
			game.startGame(p.name, true);
	}

	private void endGame(Game game) throws GameException {
		for (Player p: game.getPlayers())
			game.endGame(p.name, true);
	}
}
