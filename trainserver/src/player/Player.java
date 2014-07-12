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
	private Train train;
	private int money;
	private Rail rail;
	private Card[] cards;
	private int spendings;
	
	public Player(int startMoney, Card[] hand, String name, String color, 
			Player next, Map<Milepost, Set<Milepost>> globalRail){
		train = null;
		money = startMoney;
		rail = new Rail(globalRail);
		cards = hand;
		this.name = name;
		this.color = color;
		spendings = 0;
		nextPlayer = next;
	}
	
	public void startTrain(Milepost m){
		train = new Train(m);
	}
	
	public void moveTrain(Queue<Milepost> moves) throws GameException {
		if(moves.isEmpty()) return;
		Milepost l = train.getLocation();
		Milepost next = moves.poll();
		if(l.isNeighbor(next) && rail.connects(l, next)) train.moveTrain(next);
		else throw new GameException("InvalidMove");
		moveTrain(moves);
	}
	
	public void upgradeTrain(UpgradeType u) throws GameException {
		if(spendings > 0) throw new GameException("ExceededAllowance");
		switch (u) {
			case SPEED:
				train.upgradeSpeed();
				break;
			case CAPACITY:
				train.upgradeLoads();
				break;
		}
		spendings -= 20;
		
	}
	
	public void buildTrack(Queue<Milepost> mileposts) throws GameException {
		if(mileposts.isEmpty()) return;
		Milepost origin = mileposts.poll();
		if(mileposts.isEmpty()) return;
		Milepost next = mileposts.peek();
		int cost = rail.build(origin, next);
		if(spendings + cost <= 20){
			spendings += cost;
		} else {
			rail.erase(origin, next);
			throw new GameException("ExceededAllowance");
		}
		buildTrack(mileposts);
	}
	
	public void pickupLoad(String load) throws GameException{
		train.addLoad(load);
	}
	
	public void dropLoad(String load) throws GameException{
		train.dropLoad(load);
	}
	
	/** Delivers a load on the given card.
	 * @param index is the location of the card in the player's hand, array-wise
	 * @param next is the card drawn to replace that one
	 */
	public void deliverLoad(int index, Card next) throws GameException {
		Card c = cards[index];
		Trip t = canDeliver(c);
		if(t == null) throw new GameException("InvalidDelivery");
		train.dropLoad(t.load);
		cards[index] = next; 
		money += t.cost;
	}
	
	private Trip canDeliver(Card c){
		if(train == null) return null;
		City city = train.getLocation().city;
		if(city == null) return null;
		for(int i = 0; i < c.trips.length; i ++){
			Trip t = c.trips[i];
			if(train.containsLoad(t.load) && t.dest == city) return t;
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
