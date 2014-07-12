package train;

public class RuleSet {
	public final int handSize; //the number of cards in a hand
	public final int startingMoney; //the money each player starts with
	
	RuleSet(int handSize, int startingMoney) {
		this.handSize = handSize;
		this.startingMoney = startingMoney;
	}
}