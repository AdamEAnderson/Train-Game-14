import java.util.ArrayList;
import java.util.List;

import player.Player;

import reference.Card;

import map.Map;


public class State {
	private final Map map;
	private List<Card> deck;
	private List<Card> discard;
	private List<Player> players;
	
	public State(){
		map = new Map("");
		//deck = Card.init();
		discard = new ArrayList<Card>();
		//players = Player.init();
	}
}
