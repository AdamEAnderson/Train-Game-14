package train;
import reference.UpgradeType;
import map.MilepostId;


interface AbstractGame {
	
	/** New player joins an existing game, requesting a color for drawing track.
	 * @param player	Player joining the game
	 * @param color		Color for new player's track and tokens
	 * @throws GameException
	 * Exceptions:
	 * COLOR_NOT_AVAILABLE
	 * PLAYER_ALREADY_JOINED
	 * GAME_NOT_FOUND
	 **/
	void joinGame(String player, String color) throws GameException;
	
	/** Start the game. 
	 * Players signal when they are ready to start the game. When all players are ready,
	 * the game is started.
	 * @param pid Player who started the game
	 * @throws GameException
	 * 
	 * Exceptions:
	 * GAME_ALREADY_STARTED
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void startGame(String player, boolean ready) throws GameException;
	
	/** Check to see if building a set of mileposts is possible
	 * @param player	Player who is building
	 * @param mileposts	Where track is being built
	 * @throws GameException
	 * 
	 * Exceptions:
	 * INVALID_TRACK
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void testBuildTrack(String pid, MilepostId[] mileposts) throws GameException;

	/** Player builds new track
	 * @param player	Player who is building
	 * @param mileposts	Where track is being built
	 * @throws GameException
	 * 
	 * Exceptions:
	 * INVALID_TRACK
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void buildTrack(String pid, MilepostId[] mileposts) throws GameException;

	/** Upgrade a train, either to be faster or to carry more loads
	 * @param pid		Player whose train is being upgraded
	 * @param upgrade	Whether upgrade is for speed or capacity
	 * @param train		Which train is being upgraded
	 * @throws GameException
	 * 
	 * Exceptions:
	 * INVALID_UPGRADE
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void upgradeTrain(String player, int train, UpgradeType upgrade) throws GameException;
	
	/** Player positions their train to a city milepost, ready for the first full turn
	 * @param pid		Player whose train is being started
	 * @param train		Which train
	 * @param milepost	Train's starting position
	 * @throws GameException
	 * 
	 * Exceptions:
	 * INVALID_TRACK (train must start on a city milepost)
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void placeTrain(String player, int train, MilepostId where) throws GameException; 
	
	/** Check to see if move is possible
	 * @param pid		Player whose train is being moved
	 * @param train		Which train
	 * @param milepost	Train's path along mileposts
	 * @throws GameException
	 * 
	 * Exceptions:
	 * INVALID_TRACK (train must start on a city milepost)
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void testMoveTrain(String player, int train, MilepostId[] mileposts) throws GameException;
	
	/** Player moves their train
	 * @param pid		Player whose train is being moved
	 * @param train		Which train
	 * @param milepost	Train's path along mileposts
	 * @throws GameException
	 * 
	 * Exceptions:
	 * INVALID_TRACK (train must start on a city milepost)
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void moveTrain(String player, int train, MilepostId[] mileposts) throws GameException;
	
	/** Player picks up a new load
	 * @param pid		Player whose train is being moved
	 * @param train		Which train
	 * @param load		Load being picked up
	 * @throws GameException
	 * 
	 * Exceptions:
	 * INVALID LOAD
	 * TRAIN_FULL
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void pickupLoad(String player, int train, String load) throws GameException;
	
	/** Player delivers a load, turning in a card
	 * @param pid		Player whose train is being moved
	 * @param train		Which train
	 * @param load		Load being picked up
	 * @param card		Card being turned in
	 * @throws GameException
	 * 
	 * Exceptions:
	 * INVALID LOAD
	 * INVALID_DELIVERY
	 * INVALID_CARD
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void deliverLoad(String player, int train, String load, int card) throws GameException;
	
	/** Player releases a load, making room for more cargo
	 * @param pid		Player whose train is being moved
	 * @param train		Which train
	 * @param load		Load being dropped
	 * @throws GameException
	 * 
	 * Exceptions:
	 * INVALID LOAD
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void dumpLoad(String player, int train, String load) throws GameException;
	
	/** Player exchanges their turn for the chance to turn in 
	 * their entire hand for a new set of cards
	 * 
	 * @param player
	 * @throws GameException 
	 */
	void turnInCards(String player) throws GameException;
	
	/** Player declares their turn is over.
	 * @param pid		Player whose train is being moved
	 * @throws GameException
	 * 
	 * Exceptions:
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void endTurn(String player) throws GameException;
	
	/** Player leaves the game permanently.
	 * 
	 * @param player
	 * @throws GameException
	 */
	void resign(String player) throws GameException;
	
	/** Game is over.
	 * @param pid		Player whose train is being moved
	 * @throws GameException
	 * 
	 * Exceptions:
	 * GAME_NOT_FOUND
	 * PLAYER_NOT_FOUND
	 */
	void endGame(String player, boolean ready) throws GameException;
}