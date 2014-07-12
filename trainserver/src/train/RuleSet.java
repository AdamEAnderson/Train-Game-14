package train;

public class RuleSet {
	public final int handSize; //the number of cards in a hand
	public final int startingMoney; //the money each player starts with
	public final int numTrains; //the number of trains per player
	
	RuleSet(int handSize, int startingMoney, int numTrains) {
		this.handSize = handSize;
		this.startingMoney = startingMoney;
		this.numTrains = numTrains;
	}
}