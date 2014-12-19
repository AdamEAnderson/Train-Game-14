package test;

import static org.junit.Assert.*;
import map.MilepostId;

import org.junit.Test;

import player.Player;
import reference.UpgradeType;
import train.Game;
import train.GameException;
import train.TrainServer;

public class RentalTest extends GameTest {


	@Test
	public void testRental() {
		Game game = null;
		try {
			String gid = newGame("Louie", "blue", "africa");
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
        	reverse(moveMileposts);
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
        	reverse(moveMileposts);
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
        	
        	// Third player starts on player2's track, goes to Dakar, and 
        	// heads out to Abidjan on player1's track, then goes back to 
        	// player2's track to Kano.
        	// Test rental to 2 different players in one turn
        	// Also tests returning to track that was rented this turn already
        	moveMileposts = new MilepostId[] {
    			new MilepostId(2,20),
	        	new MilepostId(2,21),
	        	new MilepostId(3,22),
	        	new MilepostId(3,23),
	        	new MilepostId(4,24), // Freetown
	        	new MilepostId(3,23),
	        	new MilepostId(3,22),
	        	new MilepostId(2,21),
    			new MilepostId(2,20),
    			new MilepostId(3,20),
    			new MilepostId(4,20),
    			new MilepostId(5,20),
    			new MilepostId(6,20),
    			new MilepostId(7,20),
    			new MilepostId(8,20),
    			new MilepostId(9,20),
    			new MilepostId(10,20),
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

        	String jsonStatusPayload = "{\"gid\":\"" + gid + "\"}";
	        String statusMsg = TrainServer.status(jsonStatusPayload);
	        log.info("endGame status message {}", statusMsg);
		} catch (GameException e) {
			fail("Unexpected exception in test setup");
		}
	} 

}
