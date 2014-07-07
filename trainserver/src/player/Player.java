package player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import reference.Card;
import map.Edge;
import map.Milepost;


public class Player {
	private Train train;
	private int money;
	private Rail rail;
	private Card[] cards;
	
	public Player(int startMoney, Card[] hand){
		train = null;
		money = startMoney;
		rail = new Rail();
		cards = hand;
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
	
	public void deliverLoad(Card c){
		//TODO
		//Does every card deliver loads to different cities?
		/* discard a card from your hand 
		 * (verify in correct city w/ correct load)
		 * drop the given load
		 * add given money
		 * draw new card from the deck
		 */
	}
}
