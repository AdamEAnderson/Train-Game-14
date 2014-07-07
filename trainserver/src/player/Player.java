package player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import reference.Card;
import reference.Card.Trip;
import reference.City;
import map.Edge;
import map.Milepost;


public class Player {
	public final String name;
	public final String color;
	private Train train;
	private int money;
	private Rail rail;
	private Card[] cards;
	
	public Player(int startMoney, Card[] hand, String name, String color){
		train = null;
		money = startMoney;
		rail = new Rail();
		cards = hand;
		this.name = name;
		this.color = color;
	}
	
	public void placeTrain(Milepost m){
		train = new Train(m);
	}
	
	public void buildTrack(Queue<Milepost> mileposts){
		if(mileposts.isEmpty()) return;
		Milepost origin = mileposts.poll();
		if(mileposts.isEmpty()) return;
		Milepost next = mileposts.peek();
		int cost = rail.build(origin, next);
		money -= cost; //does not check for affordability or the twenty that can be legally built
		buildTrack(mileposts);
	}
	
	/** Delivers a load on the given card.
	 * @param index is the location of the card in the player's hand, array-wise
	 * Does Not replace the old card with a new one
	 */
	public void deliverLoad(int index){
		Card c = cards[index];
		Trip t = canDeliver(c);
		if(t == null) return; //throw GameException 
		train.dropLoad(t.load);
		cards[index] = null; //draw a new card
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
}
