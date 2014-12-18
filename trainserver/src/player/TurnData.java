package player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import reference.UpgradeType;
import train.GameException;
import train.RuleSet;

public class TurnData {
	private String pid;
	private int pIndex;
	private int moneyMade;
	private int moneySpent;
	private int[] movesMade;
	private List<String> rentedFrom;
	private boolean isBuilding;
	private UpgradeType upgrade;
	private int upgradedTrain;
	
	public TurnData(RuleSet r, String startingPid, int startingIndex){
		movesMade = new int[r.numTrains];
		rentedFrom = new ArrayList<String>();
		isBuilding = true;
		upgradedTrain = -1;
		pid = startingPid;
		pIndex = startingIndex;
	}
	
	void checkMovesLength(int t, int moves, int limit) throws GameException{
		if(moves + movesMade[t] > limit){
			throw new GameException("InvalidMove");
		}
	}
	
	void move(int t, int moves, int limit) throws GameException{
		checkMovesLength(t, moves, limit);
		movesMade[t] += moves;
	}
	
	/** Returns true iff the renter must pay the rentee, and false otherwise
	 * 
	 */
	boolean rent(int t, int moves, int limit, String pid) throws GameException{
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
	
	void deliver(int money){
		moneyMade += money;
	}
	
	void checkSpending(int money) throws GameException{
		if(money + moneySpent > 20) throw new GameException("ExceededAllowance");
	}
	
	/** Adds money to the turn's spendings 
	 * 
	 * @param money
	 * @throws GameException if you have exceeded your allowance of $20
	 */
	void spend(int money) throws GameException{
		checkSpending(money);
		moneySpent += money;
	}
	
	//need something to indicate end of a round
	void endTurn(List<String> pids, Map<String, Player> players){
		Player player = players.get(pid);
		player.subtractMoney(moneySpent);
		player.addMoney(moneyMade); //this one needs to handle debt
		player.upgradeTrain(upgradedTrain, upgrade);
		
		if(pIndex < pids.size() - 1){
			pid = pids.get(pIndex++);
		}else{
			pid = pids.get(0);
			pIndex = 0;
		}
		
		moneyMade = 0;
		moneySpent = 0;
		movesMade = new int[movesMade.length];
		rentedFrom.clear();
		isBuilding = true;
		upgrade = null;
		upgradedTrain = -1;
	}
}
