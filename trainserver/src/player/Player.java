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
	private int movesMade;
	private ArrayList<String> rentingFrom;
	private boolean readyToStart;
	private boolean readyToEnd;
	private boolean turnInProgress;
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
		rentingFrom = new ArrayList<String>();
		nextPlayer = next;
		readyToStart = false;
		readyToEnd = false;
		turnInProgress = false;
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
	
	public void moveTrain(int t, Queue<Milepost> moves) throws GameException {
		turnInProgress = true;
		if(moves.isEmpty()) return;
		if(movesMade >= trains[t].getSpeed()) throw new GameException("InvalidMove");
		Milepost l = trains[t].getLocation();
		Milepost next = moves.poll();
		if(l.isNeighbor(next)){
			if(rail.connects(l, next) || l.isSameCity(next)) trains[t].moveTrain(next);
			else {
				String s = rail.anyConnects(l, next);
				if(s.equals("")) throw new GameException("InvalidMove");
				if(!rentingFrom.contains(s)){
					rentingFrom.add(s);
					money -= 4;
					for(Player p = nextPlayer; p != this; p.getNextPlayer()){
						if(p.name.equals(s)){
							p.money += 4;
							break;
						}
					}
				}
				trains[t].moveTrain(next);
			}
		}
		else throw new GameException("InvalidMove");
		if(rail.connectsByFerry(l, next)){
			if(movesMade == 0){
				movesMade = (trains[t].getSpeed())/2;
			} else{
				trains[t].moveTrain(l);
				throw new GameException("InvalidMove");
			}
		}
		movesMade++;
		moveTrain(t, moves);
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
	
	public void buildTrack(Queue<Milepost> mileposts) throws GameException {
		turnInProgress = true;
		if(mileposts.isEmpty()) return;
		Milepost origin = mileposts.poll();
		if(mileposts.isEmpty()) return;
		Milepost next = mileposts.peek();
		
		// We can only start building from the end of our track, or from a major city
		if(!rail.contains(origin) && origin.type != Milepost.Type.MAJORCITY){
			throw new GameException("InvalidTrack");
		}
		
		// Cannot build over track that has already been built
		if(rail.anyConnects(origin, next) != "") {
			log.warn("Track is already built there ({}, {}) and ({}, {})", origin.x, origin.y, next.x, next.y);
			throw new GameException("InvalidTrack");
		}
		if(!origin.isNeighbor(next)) {
			log.warn("Mileposts are not contiguous ({}, {}) and ({}, {})", origin.x, origin.y, next.x, next.y);
			throw new GameException("InvalidTrack");
		}
		if(origin.type == Milepost.Type.BLANK || next.type == Milepost.Type.BLANK) {
			if (origin.type == Milepost.Type.BLANK)
				log.warn("Mileposts is blank ({}, {})", origin.x, origin.y);
			if (next.type == Milepost.Type.BLANK) 
				log.warn("Mileposts is blank ({}, {})", next.x, next.y);
			throw new GameException("InvalidTrack");
		}
		
		int cost = rail.build(origin, next);
		if(spendings + cost <= 20){
			spendings += cost;
		} else {
			log.warn("Cost {} exceeded maximum of 20", spendings + cost);
			rail.erase(origin, next);
			throw new GameException("ExceededAllowance");
		}
		buildTrack(mileposts);
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
		int delivery = t.cost;
		if(money < 0){
			delivery += 2 * money;
			money = 0;
		}
		money += delivery;
	}
	
	private Trip canDeliver(int ti, Card c){
		turnInProgress = true;
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
		money -= spendings;
		spendings = 0;
		movesMade = 0;
		rentingFrom.clear();
		turnInProgress = false;
		return nextPlayer;
	}
	
	public void readyToStart(boolean ready) { readyToStart = ready;}
	
	public boolean readyToStart() { return readyToStart; }
	
	public void readyToEnd(boolean ready) { readyToEnd = ready;}
	
	public boolean readyToEnd() { return readyToEnd; }
	
	public boolean turnInProgress() { return turnInProgress; }
	
	public void resign() {hasResigned = true; }
	
	public boolean hasResigned() {return hasResigned; }
	
	public void resetNextPlayer(Player p){ nextPlayer = p; }
	
	public String getPid(){ return name; }
	
	public Player getNextPlayer(){ return nextPlayer; }
	
	public int getMoney() {	return money; }
	
	public int getSpending(){ return spendings; }
	
	public int getMovesMade(){ return movesMade; }
	
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
	public void turnInCards(Card[] cards)  {
		this.cards = cards;
	}
}
