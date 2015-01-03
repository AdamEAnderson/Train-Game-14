package player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	
	public TurnData(String gid, RuleSet r, String startingPid){
		this.gid = gid;
		movesMade = new int[r.numTrains];
		rentedFrom = new ArrayList<String>();
		upgradedTrain = -1;
		pid = startingPid;
	}
	
	public boolean checkMovesLength(int t, int moves, int limit) throws GameException{
		if (ferried)
			limit /= 2;
		return (moves + movesMade[t] <= limit);
	}
	
	//only increments move counter -> trains are accessed through player
	private void move(int t, int moves, int limit) throws GameException{
		if(!checkMovesLength(t, moves, limit)){
			throw new GameException("InvalidMove");
		}
		movesMade[t] += moves;
	}
	
	/** Returns true iff the renter must pay the rentee, and false otherwise
	 * 
	 */
	public boolean move(int t, int moves, int limit, String pid) throws GameException{
		checkMovesLength(t, moves, limit);
		if(!rentedFrom.contains(pid)){
			rentedFrom.add(pid);
			move(t, moves, limit);
			return true;
		}else{
			move(t, moves, limit);
			return false;
		}
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
		for (String landlord: rentedFrom) {
			player.spend(4);
			players.get(landlord).deposit(4);
		}
		rentedFrom.clear();
		turnInProgress = false;
		ferried = false;
		upgrade = null;
		upgradedTrain = -1;
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
}
