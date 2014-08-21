package player;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reference.*;
import train.GameException;
import train.RuleSet;
import map.Ferry;
import map.Milepost;


public class Player {
	public final String name;
	public final String color;
	private Player nextPlayer;
	private Train[] trains;
	private int money;
	private Rail rail;
	private Card[] cards;
	private int spendings;
	private int[] movesMade;
	private boolean turnInProgress;
	private ArrayList<String> rentingFrom;
	private boolean readyToStart;
	private boolean readyToEnd;
	private boolean hasResigned;
	
	private static Logger log = LoggerFactory.getLogger(Player.class);

	public Player(RuleSet ruleSet, Card[] hand, String name, String color, 
			Player next, Map<Milepost, Set<Rail.Track>> globalRail, Map<Milepost, Ferry> globalFerries){
		trains = new Train[ruleSet.numTrains];
		for (int i = 0; i < ruleSet.numTrains; ++i) {
			trains[i] = new Train(i);
		}
		money = ruleSet.startingMoney;
		rail = new Rail(globalRail, globalFerries, name);
		cards = hand;
		this.name = name;
		this.color = color;
		spendings = 0;
		turnInProgress = false;
		movesMade = new int[ruleSet.numTrains];
		rentingFrom = new ArrayList<String>();
		nextPlayer = next;
		readyToStart = false;
		readyToEnd = false;
		hasResigned = false;
	}
	
	public void placeTrain(Milepost m, int t) throws GameException{
		turnInProgress = true;
		if (trains[t].getLocation() == null) 
			trains[t].moveTrain(m);
		else 
			throw new GameException("TrainAlreadyPlaced");
		log.info("after place");
	}
	
	public boolean testMoveTrain(int tIndex, Milepost[] mileposts){
		if(!mileposts[0].equals(trains[tIndex].getLocation())) return false;
		return testMoveTrain(mileposts, 0, movesMade[tIndex]);
	}
	
	private boolean testMoveTrain(Milepost[] mileposts, int mIndex, int tIndex){
		if(mileposts.length <= mIndex) return true;
		if(tIndex >= 20) return false;
		Milepost origin = mileposts[mIndex];
		Milepost next = mileposts[mIndex + 1];
		
		String ownerId = rail.anyConnects(origin, next);
		if(ownerId.equals("") && !origin.isSameCity(next)) return false;
		
		return testMoveTrain(mileposts, mIndex + 1, tIndex + 1);
	}
	
	public void moveTrain(int t, Queue<Milepost> moves) throws GameException {
		turnInProgress = true;
		if(moves.isEmpty()) return;
		Milepost l = trains[t].getLocation();
		if(movesMade[t] >= trains[t].getSpeed() || l == null) 
			throw new GameException("InvalidMove");
		Milepost next = moves.poll();
		if(next == null) return;
		moveMileposts(t, l, next);
		moveTrain(t, moves);
	}
	
	private void moveMileposts(int t, Milepost origin, Milepost next) throws GameException{
		String ownerID = rail.anyConnects(origin, next);
		Player owner = null;
		if(ownerID.equals(name)) owner = this;
		for(Player p = nextPlayer; p != this; p = p.nextPlayer){
			if(p.name.equals(ownerID)){
				owner = p;
				break;
			}
		}
		if(owner == null){
			if(origin.isSameCity(next)) {
				trains[t].moveTrain(next);
				return;
			}
			throw new GameException("InvalidMove");
		}
		if(!rentingFrom.contains(ownerID) && !ownerID.equals(name)){
			rentingFrom.add(ownerID);
			money -= 4;
			owner.deposit(4);
		}
		
		if(rail.connectsByFerry(origin, next)){
			moveFerry(t, origin, next);
		} else{
			trains[t].moveTrain(next);
			movesMade[t] ++;
		}
	}
	
	private void moveFerry(int t, Milepost origin, Milepost next) throws GameException{
		if(movesMade[t] != 0){
			throw new GameException("InvalidMove");
		} else{
			movesMade[t] = (trains[t].getSpeed())/2;
			trains[t].moveTrain(next);
		}
	}
	
	public void upgradeTrain(int t, UpgradeType u) throws GameException {
		turnInProgress = true;
		if(spendings > 0) throw new GameException("ExceededAllowance");
		switch (u) {
			case SPEED:
				trains[t].upgradeSpeed();
				break;
			case CAPACITY:
				trains[t].upgradeLoads();
				break;
		}
		spendings += 20;
		
	}

	public boolean testBuildTrack(Milepost[] mileposts){
		if(mileposts.length < 1) return true;
		if((!rail.contains(mileposts[0]) && mileposts[0].type != Milepost.Type.MAJORCITY) 
				|| mileposts[0].type == Milepost.Type.BLANK) return false;
		int projectSpending = getSpending();
		
		for(int i = 0; i < mileposts.length -1 ; i++){
			projectSpending += rail.getCost(mileposts[i], mileposts[i + 1]);
			if(projectSpending > 20) return false;
			if(mileposts[i].isSameCity(mileposts[i + 1])) return false;
			if(rail.anyConnects(mileposts[i], mileposts[i + 1]) != "") {
				log.warn("Track is already built there ({}, {}) and ({}, {})",
						mileposts[i].x, mileposts[i].y, mileposts[i + 1].x, mileposts[i + 1].y);
				return false;
			}
			if(!mileposts[i].isNeighbor(mileposts[i + 1])) {
				log.warn("Mileposts are not contiguous ({}, {}) and ({}, {})", 
						mileposts[i].x, mileposts[i].y, mileposts[i + 1].x, mileposts[i + 1].y);
				return false;
			}
			if(mileposts[i + 1].type == Milepost.Type.BLANK) {
				log.warn("Mileposts is blank ({}, {})", mileposts[i + 1].x, mileposts[i + 1].y);
				return false;
			}
		}
		return true;
		//return testBuildTrack(mileposts, 0, getSpending());
	}
	
	
	public void buildTrack(Milepost[] mileposts) throws GameException{
		Milepost[] tester = mileposts.clone();
		if(!testBuildTrack(tester)){
			throw new GameException("InvalidTrack");
		}
		turnInProgress = true;
		for(int i = 0; i < mileposts.length - 1; i++){
			spendings += rail.build(mileposts[i], mileposts[i + 1]);
		}
	}
	
	
	
	public void pickupLoad(int t, String load) throws GameException{ 
		turnInProgress = true;
		trains[t].addLoad(load); 
	}
	
	
	
	public void dropLoad(int t, String load) throws GameException{ 
		turnInProgress = true;
		trains[t].dropLoad(load); 
	}
	
	
	
	/** Delivers a load on the given card.
	 * @param index is the location of the card in the player's hand, array-wise
	 * @param next is the card drawn to replace that one
	 */
	public void deliverLoad(int cIndex, int tIndex, Card next) throws GameException {
		turnInProgress = true;
		Card c = cards[cIndex];
		Trip t = canDeliver(tIndex, c);
		if(t == null) throw new GameException("InvalidDelivery");
		trains[tIndex].dropLoad(t.load);
		cards[cIndex] = next; 
		deposit(t.cost);
	}
	
	private Trip canDeliver(int ti, Card c){
		if(trains[ti] == null) return null;
		City city = trains[ti].getLocation().city;
		if(city == null) return null;
		for(int i = 0; i < c.trips.length; i ++){
			Trip t = c.trips[i];
			if(trains[ti].containsLoad(t.load) && t.dest.equals(city.name)) return t;
		}
		return null;
	}
	
	public Player endTurn(){
		turnInProgress = true;
		money -= spendings;
		spendings = 0;
		rentingFrom.clear();
		for(int i = 0; i < movesMade.length; i++){
			movesMade[i] = 0;
		}
		return nextPlayer;
	}
	
	public void resign() {
		turnInProgress = true;
		hasResigned = true;
		readyToEnd = true;
		readyToStart = true;
	}
	
	private void deposit(int deposit){ 
		if(deposit < 0) {
			money += deposit; 
			return;
		}
		if(money < 0){
			if((-2) * money > deposit){
				money += deposit / 2;
			} else{
				deposit += 2 * money;
				money = deposit;
			}
		} else {
			money += deposit;
		}
	}
	
	public void readyToStart(boolean ready) { readyToStart = ready;}
	
	public boolean readyToStart() { return readyToStart; }
	
	public void readyToEnd(boolean ready) { readyToEnd = ready;}
	
	public boolean readyToEnd() { return readyToEnd; }
	
	public boolean turnInProgress() { return turnInProgress; }
	
	public boolean hasResigned() {return hasResigned; }
	
	public void resetNextPlayer(Player p){ nextPlayer = p; }
	
	public String getPid(){ return name; }
	
	public Player getNextPlayer(){ return nextPlayer; }
	
	public int getMoney() {	return money; }
	
	public int getSpending(){ return spendings; }
	
	public int[] getMovesMade(){ return movesMade; }
	
	public int getMovesMade(int tIndex){ return movesMade[tIndex]; }
	
	public Card[] getCards(){ return cards; }
	
	public Train[] getTrains(){ return trains; }
	
	public Rail getRail(){ return rail; }
	
	@Override 
	public boolean equals(Object obj){
		if(obj instanceof Player){
			Player p = (Player)obj;
			return p.name.equals(name);
		}
		return false;
	}
	
	/** Returns the money offered for the highest-paying trip in this players
	 * hand of cards.
	 */
	public int getMaxTrip(){
		int max = 0;
		for(int i = 0; i < cards.length; i++){
			Card c = cards[i];
			for(int j = 0; j < c.trips.length; j++){
				int temp = c.trips[j].cost;
				if(temp > max) max = temp;
			}
		}
		return max;
	}
	
	/** Call this from test code only!! Just here for debugging */
	public void turnInCards(Card[] cards)  { this.cards = cards; }
}
