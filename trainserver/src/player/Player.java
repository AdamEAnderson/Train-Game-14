package player;

import java.util.Queue;

import reference.*;
import reference.Trip;
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
	
	public Player(int startMoney, Card[] hand, String name, String color, Player next){
		train = null;
		money = startMoney;
		rail = new Rail();
		cards = hand;
		this.name = name;
		this.color = color;
		spendings = 0;
		nextPlayer = next;
	}
	
	public void placeTrain(Milepost m){
		train = new Train(m);
	}
	
	public void moveTrain(Milepost m) throws GameException {
		Milepost l = train.getLocation();
		if(l.isNeighbor(m) && rail.connects(l, m)) train.moveTrain(m);
		else throw new GameException("InvalidMove");
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
}
