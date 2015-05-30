package train;

public class GameException extends Exception {
	public static String GAME_NOT_FOUND = "GameNotFound";
	public static String PLAYER_NOT_FOUND = "PlayerNotFound";
	public static String RULESET_NOT_FOUND = "RulesetNotFound";
	public static String GAMETYPE_NOT_FOUND = "GameTypeNotFound";
	public static String COLOR_NOT_AVAILABLE = "ColorNotAvailable";
	public static String PLAYER_ALREADY_JOINED = "PlayerAlreadyJoined";
	public static String GAME_ALREADY_STARTED = "GameAlreadyStarted";
	public static String PLAYER_NOT_ACTIVE = "PlayerNotActive";
	public static String BUILDING_TURN = "BuildingTurn";
	public static String INVALID_TRACK = "InvalidTrack";
	public static String INVALID_UPGRADE = "InvalidUpgrade";
	public static String CITY_NOT_FOUND = "CityNotFound";
	public static String INVALID_LOAD = "InvalidLoad";
	public static String TRAIN_FULL = "TrainFull";
	public static String INVALID_DELIVERY = "InvalidDelivery";		// delivering a load with no card
	public static String INVALID_MESSAGE_TYPE = "InvalidMessageType";// message type not recognised
	public static String INVALID_MOVE = "InvalidMove";
	public static String TURN_ALREADY_STARTED = "TurnAlreadyStarted";
	public static String BAD_MAP_DATA = "BadMapData";
	public static String BAD_CARD_DATA = "BadCardData";
	public static String EXCEEDED_ALLOWANCE = "ExceededAllowance";
	public static String INVALID_CARD = "InvalidCard";
	public static String TRAIN_ALREADY_STARTED = "TrainAlreadyStarted";
	public static String NOTHING_TO_UNDO = "NothingToUndo";
	public static String NOTHING_TO_REDO = "NothingToRedo";
	public static String CANNOT_SAVE_FILE = "CannotSaveFile";
	public static String CANNOT_OPEN_FILE = "CannotOpenFile";
	public static String ALREADY_RECORDING = "AlreadyRecording";
	
	public static final long serialVersionUID = 1;
	
	public GameException(String exceptionType) {
		super(exceptionType);
	}
}
