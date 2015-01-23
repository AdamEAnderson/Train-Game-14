package player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import map.MilepostId;
import reference.UpgradeType;
import train.GameException;
import train.RuleSet;

public class TurnData {
	public final String gid;
	
	private String pid;
	private int moneyMade;
	private int moneySpent;
	private int[] movesMade;
	private List<String> rentedFrom;
	private boolean turnInProgress;
	private boolean ferried;
	private UpgradeType upgrade;
	private int upgradedTrain;
	private List<List<MilepostId>> trainMoves;	// For each train, the list of mileposts it has moved through
															// 0'th milepost is starting position
	
	public TurnData(String gid, RuleSet r, Player startingPlayer){
		this.gid = gid;
		rentedFrom = new ArrayList<String>();
		upgradedTrain = -1;
		pid = startingPlayer.getPid();
		movesMade = new int[r.numTrains];
		trainMoves = new ArrayList<List<MilepostId>>(r.numTrains);
		for (int train = 0; train < r.numTrains; ++train) 
			trainMoves.add(new ArrayList<MilepostId>());
	}
	
	public boolean rentedFrom(String pid) {
		return rentedFrom.contains(pid);
	}
	
	public boolean checkMovesLength(int t, int moves, int limit) throws GameException{
		if (ferried)
			limit /= 2;
		return (moves + movesMade[t] <= limit);
	}
	
	public void move(int train, int limit, MilepostId startLocation, MilepostId[] mileposts)  throws GameException{
		if(!checkMovesLength(train, mileposts.length, limit))
			throw new GameException("InvalidMove");
		List<MilepostId> moves = trainMoves.get(train);
		if (moves.isEmpty())	// if this is the train's first move this turn, register the starting location
			moves.add(startLocation);
		for (int m = 0; m < mileposts.length; ++m)
			moves.add(mileposts[m]);
		movesMade[train] += mileposts.length;
	}
	
	public boolean rent(String trackOwner) {
		boolean newRental = !rentedFrom.contains(trackOwner);
		if (newRental) 
			rentedFrom.add(trackOwner);
		return newRental;
	}
	
	public void upgrade(int t, UpgradeType u) throws GameException{
		if(!checkSpending(20)){
			throw new GameException("ExceededAllowance");
		}
		spend(20);
		upgrade = u;
		upgradedTrain = t;
	}
	
	
	//need something to indicate end of a round ?
	public void endTurn(String next, Map<String, Player> players) throws GameException{
		Player player = players.get(pid);
		player.spend(moneySpent);
		player.deposit(moneyMade); //this one needs to handle debt
		if(upgradedTrain != -1)
			player.upgradeTrain(upgradedTrain, upgrade);
		
		pid = next;
		
		moneyMade = 0;
		moneySpent = 0;
		movesMade = new int[movesMade.length];
		for (String landlord: rentedFrom) 
			player.rent(players.get(landlord));	
		rentedFrom.clear();
		turnInProgress = false;
		ferried = false;
		upgrade = null;
		upgradedTrain = -1;
		for (List<MilepostId> mileposts: trainMoves)
			mileposts.clear();
	}
	
	/** Adds money to the turn's spendings 
	 * 
	 * @param money
	 * @throws GameException if you have exceeded your allowance of $20
	 */
	public void spend(int money) throws GameException{
		if(!checkSpending(money))
			throw new GameException("ExceededAllowance");
		moneySpent += money;
	}
	
	public void deliver(int money){ moneyMade += money; }
	public boolean checkSpending(int money){ return (money + moneySpent <= 20);}
	public void startTurn(){ turnInProgress = true;	}
	public void ferry() { ferried = true; }
	
	public String getPid() {return pid;}
	public int getSpending() {return moneySpent;}
	public int getMoneyMade() {return moneyMade;}
	public int getMovesMade(int tIndex){ return movesMade[tIndex]; }
	public boolean turnInProgress(){ return turnInProgress;}
	public boolean hasFerried() { return ferried; }
	public List<List<MilepostId>> getTrainMoves() { return trainMoves; }
}
