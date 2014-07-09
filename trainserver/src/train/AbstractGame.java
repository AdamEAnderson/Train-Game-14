package train;
import reference.UpgradeType;
import map.MilepostId;


interface AbstractGame {
	
	// New player joins an existing game, requesting a color for drawing track.
	// Error values:
	// COLOR_NOT_AVAILABLE
	// PLAYER_ALREADY_JOINED
	// GAME_NOT_FOUND
	void joinGame(String player, String color) throws GameException;
	
	// Start playing - building turns may follow
	// Error values:
	// GAME_ALREADY_STARTED
	// GAME_NOT_FOUND
	// PLAYER_NOT_FOUND
	void startGame(String player) throws GameException;
	
	// Player declares what track is being built.
	// Error values:
	// INVALID_TRACK
	// GAME_NOT_FOUND
	// PLAYER_NOT_FOUND
	void buildTrack(String player, MilepostId[] mileposts) throws GameException;
	
	// Player declares a train upgrade
	// Error values:
	// INVALID_UPGRADE
	// PLAYER_NOT_FOUND
	// GAME_NOT_FOUND
	void upgradeTrain(String player, UpgradeType upgrade) throws GameException;
	
	// Player positions their train to a city milepost, ready for the first full turn
	// Error values:
	// INVALID_TRACK (train must start on a city milepost)
	// PLAYER_NOT_FOUND
	// GAME_NOT_FOUND
	void startTrain(String player, MilepostId where) throws GameException; 
	
	// Player moves their train
	// Error values:
	// INVALID TRACK
	// PLAYER_NOT_FOUND
	// GAME_NOT_FOUND
	void moveTrain(String player, MilepostId[] mileposts) throws GameException;
	
	// Player picks up a new load
	// Error values:
	// INVALID LOAD
	// TRAIN_FULL
	// CITY_NOT_FOUND
	// PLAYER_NOT_FOUND
	// GAME_NOT_FOUND
	void pickupLoad(String player, String city, String load) throws GameException;
	
	// Player delivers a load, turning in a card
	// Error values:
	// INVALID LOAD
	// INVALID_DELIVERY
	// CITY_NOT_FOUND
	// PLAYER_NOT_FOUND
	// GAME_NOT_FOUND
	void deliverLoad(String player, String city, String load) throws GameException;
	
	// Player releases a load, making room for more cargo
	// Error values:
	// INVALID LOAD
	// PLAYER_NOT_FOUND
	// GAME_NOT_FOUND
	void dumpLoad(String player, String load) throws GameException;
	
	// Player declares their turn is over.
	// Error values:
	// PLAYER_NOT_FOUND
	// GAME_NOT_FOUND
	void endTurn(String player) throws GameException;
	
	// Game is over.
	// Error values:
	// PLAYER_NOT_FOUND
	// GAME_NOT_FOUND
	void endGame(String player) throws GameException;
}