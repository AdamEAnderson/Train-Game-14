package train;

public class RuleSet {
	public final int handSize; //the number of cards in a hand
	public final int startingMoney; //the money each player starts with
	public final int numTrains; //the number of trains per player

	public final boolean multiPlayerTrack;	// allow multiple players to build track between the same mileposts
	
	RuleSet(int handSize, int startingMoney, int numTrains, boolean multiPlayerTrack) {
		this.handSize = handSize;
		this.startingMoney = startingMoney;
		this.numTrains = numTrains;
		this.multiPlayerTrack = multiPlayerTrack;
	}
}