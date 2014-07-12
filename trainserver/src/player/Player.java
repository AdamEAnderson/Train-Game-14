package player;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

import reference.*;
import train.GameException;
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
	
	public Player(int startMoney, int numTrain, Card[] hand, String name, String color, 
			Player next, Map<Milepost, Set<Milepost>> globalRail){
		trains = new Train[numTrain];
		money = startMoney;
		rail = new Rail(globalRail);
		cards = hand;
		this.name = name;
		this.color = color;
		spendings = 0;
		nextPlayer = next;
	}
	
	public void startTrain(Milepost m, int t) throws GameException{
		if(trains[t] == null) trains[t] = new Train(m);
		else throw new GameException("TrainAlreadyStarted");
	}
	
	public void moveTrain(int t, Queue<Milepost> moves) throws GameException {
		if(moves.isEmpty()) return;
		Milepost l = trains[t].getLocation();
		Milepost next = moves.poll();
		if(l.isNeighbor(next) && rail.connects(l, next)) trains[t].moveTrain(next);
		else throw new GameException("InvalidMove");
		moveTrain(t, moves);
	}
	
	public void upgradeTrain(int t, UpgradeType u) throws GameException {
		if(spendings > 0) throw new GameException("ExceededAllowance");
		switch (u) {
			case SPEED:
				trains[t].upgradeSpeed();
				break;
			case CAPACITY:
				trains[t].upgradeLoads();
				break;
		}
		spendings -= 20;
		
	}
	
	public void buildTrack(Queue<Milepost> mileposts) throws GameException {
		if(mileposts.isEmpty()) return;
		Milepost origin = mileposts.poll();
		if(mileposts.isEmpty()) return;
		Milepost next = mileposts.peek();
		
		if(!rail.contains(origin) && origin.type != Milepost.Type.MAJORCITY){
			throw new GameException("InvalidTrack");
		}
		if(rail.anyConnects(origin, next)) throw new GameException("InvalidTrack"); 
		if(!origin.isNeighbor(next)) throw new GameException("InvalidTrack");
		if(origin.type == Milepost.Type.BLANK || next.type == Milepost.Type.BLANK) 
			throw new GameException("InvalidTrack");
		
		int cost = rail.build(origin, next);
		if(spendings + cost <= 20){
			spendings += cost;
		} else {
			rail.erase(origin, next);
			throw new GameException("ExceededAllowance");
		}
		buildTrack(mileposts);
	}
	
	public void pickupLoad(int t, String load) throws GameException{
		trains[t].addLoad(load);
	}
	
	public void dropLoad(int t, String load) throws GameException{
		trains[t].dropLoad(load);
	}
	
	/** Delivers a load on the given card.
	 * @param index is the location of the card in the player's hand, array-wise
	 * @param next is the card drawn to replace that one
	 */
	public void deliverLoad(int cIndex, int tIndex, Card next) throws GameException {
		Card c = cards[cIndex];
		Trip t = canDeliver(tIndex, c);
		if(t == null) throw new GameException("InvalidDelivery");
		trains[tIndex].dropLoad(t.load);
		cards[cIndex] = next; 
		money += t.cost;
	}
	
	private Trip canDeliver(int ti, Card c){
		if(trains[ti] == null) return null;
		City city = trains[ti].getLocation().city;
		if(city == null) return null;
		for(int i = 0; i < c.trips.length; i ++){
			Trip t = c.trips[i];
			if(trains[ti].containsLoad(t.load) && t.dest == city) return t;
		}
		return null;
	}
	
	public Player endTurn(){
		money -= spendings;
		spendings = 0;
		return nextPlayer;
	}
	
	public void resetNextPlayer(Player p){
		nextPlayer = p;
	}
	
	public String getPid(){
		return name;
	}
	
	public Player getNextPlayer(){
		return nextPlayer;
	}
	
	public int getMoney() {
		return money;
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
}
