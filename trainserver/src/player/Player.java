package player;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reference.*;
import train.Game;
import train.GameException;
import train.RuleSet;
import map.Milepost;


public class Player {
	public final String name;
	public final String color;
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
	private Stats stats;
	private transient Game game;
	
	private static Logger log = LoggerFactory.getLogger(Player.class);

	public Player(RuleSet ruleSet, Card[] hand, String name, String color, 
			Game game, Map<Milepost, Set<Rail.Track>> globalRail){
		trains = new Train[ruleSet.numTrains];
		for (int i = 0; i < ruleSet.numTrains; ++i) {
			trains[i] = new Train(i);
		}
		this.game = game;
		money = ruleSet.startingMoney;
		rail = new Rail(globalRail, name);
		cards = hand;
		this.name = name;
		this.color = color;
		spendings = 0;
		turnInProgress = false;
		movesMade = new int[ruleSet.numTrains];
		rentingFrom = new ArrayList<String>();
		readyToStart = false;
		readyToEnd = false;
		hasResigned = false;
		stats = new Stats();
	}
	
	public void placeTrain(Milepost m, int t) throws GameException{
		turnInProgress = true;
		if (trains[t].getLocation() == null) 
			trains[t].moveTrain(m);
		else 
			throw new GameException("TrainAlreadyPlaced");
		log.info("after place");
	}
	
	public boolean testMoveTrain(int tIndex, Milepost[] mps){
		if(mps[0] == null) 
			return false;
		for(int i = 0, m = movesMade[tIndex]; i < mps.length - 1; i++, m++){
			if(m >= trains[tIndex].getSpeed()) {
				log.warn("Train cannot move {} mileposts, limit is {}", m + 1, trains[tIndex].getSpeed());
				return false;
			}
			String ownerId = rail.anyConnects(mps[i], mps[i + 1]);
			if(ownerId.equals("") && !mps[i].isSameCity(mps[i + 1])) {
				log.warn("Cannot move - missing track from {} to {}", mps[i].getMilepostId().toString(), 
					mps[i+1].getMilepostId().toString());
				return false;
			}
			if(mps[i].isNeighborByFerry(mps[i + 1])){
				if(m != 0) {
					log.warn("Travelling by ferry must be at start of turn");
					return false;
				}
				else
					m = trains[tIndex].getSpeed()/2;
			}
		}
		return true;
	}
	
	public void moveTrain(int t, Milepost[] mps) throws GameException{
		if(!testMoveTrain(t, mps)) 
			throw new GameException("Invalid Move");
		trains[t].moveTrain(mps[mps.length - 1]);
		for(int i = 0; i < mps.length - 1; i++){
			String ownerID = rail.anyConnects(mps[i], mps[i + 1]);
			Player owner = ownerID.length() > 0 ? game.getPlayer(ownerID) : null;
			if(!rentingFrom.contains(ownerID) && !ownerID.equals(name) && owner != null){
				rentingFrom.add(ownerID);
				money -= 4;
				stats.rentalExpense += 4;
				owner.deposit(4);
				owner.stats.rentalIncome += 4;
			}
			if(mps[i].isNeighborByFerry(mps[i + 1])){
				movesMade[t] = trains[t].getSpeed()/2;
			} else{
				movesMade[t] ++;
			}
		}
		stats.milesTravelled += mps.length;
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
		
		// Cannot build where there is no milepost
		if (mileposts[0].type == Milepost.Type.BLANK) {
			log.warn("Cannot build from blank milepost");
			return false;
		}

		// Building must start from track that has been built by this player previously,
		// or from a major city. 
		if (!rail.contains(mileposts[0]) && mileposts[0].type != Milepost.Type.MAJORCITY) {
			log.warn("Building must start from player's track or from major city");
			return false;
		}
		int projectSpending = getSpending();
		
		for(int i = 0; i < mileposts.length -1 ; i++){
			projectSpending += rail.getCost(mileposts[i], mileposts[i + 1]);
			if(projectSpending > 20) return false;
			if(mileposts[i].isSameCity(mileposts[i + 1])) {
				log.info("Cannot build through major city");
				return false;
			}
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
	}
	
	
	public void buildTrack(Milepost[] mileposts) throws GameException{
		Milepost[] tester = mileposts.clone();
		if(!testBuildTrack(tester)){
			throw new GameException(GameException.INVALID_TRACK);
		}
		int cost = 0;
		turnInProgress = true;
		for(int i = 0; i < mileposts.length - 1; i++)
			cost += rail.build(mileposts[i], mileposts[i + 1]);
		stats.milepostsBuilt += mileposts.length;
		stats.trackExpense += cost;
		spendings += cost;
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
		++stats.deliveryCount;
		stats.deliveryIncome += t.cost;
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
	
	public void endTurn(){
		turnInProgress = true;
		money -= spendings;
		spendings = 0;
		rentingFrom.clear();
		for(int i = 0; i < movesMade.length; i++){
			movesMade[i] = 0;
		}
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
		
	public String getPid(){ return name; }
		
	public int getMoney() {	return money; }
	
	public int getSpending(){ return spendings; }
	
	public int[] getMovesMade(){ return movesMade; }
	
	public int getMovesMade(int tIndex){ return movesMade[tIndex]; }
	
	public Card[] getCards(){ return cards; }
	
	public Train[] getTrains(){ return trains; }
	
	public Rail getRail(){ return rail; }
	
	public Stats stats() { 
		stats.money = money;
		return stats; 
		}
	
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
	
	public void fixup(Game game, Map<Milepost, Set<Rail.Track>> globalRail) {
		this.game = game;
		rail.fixup(globalRail);
	}
}
