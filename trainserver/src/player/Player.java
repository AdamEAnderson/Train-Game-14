package player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reference.*;
import train.GameException;
import train.RuleSet;
import map.Milepost;


public class Player {
	public final String name;
	public final String color;
	private Train[] trains;
	private int money;
	private Card[] cards;
	private boolean readyToStart;
	private boolean readyToEnd;
	private boolean hasResigned;
	
	private RequestQueue requestQueue = null;
	
	private Stats stats;
	
	private static Logger log = LoggerFactory.getLogger(Player.class);

	public Player(RuleSet ruleSet, String pid, String color, Card[] cards){
		name = pid;
		this.color = color;
		trains = new Train[ruleSet.numTrains];
		for(int i = 0; i < trains.length; i++){
			trains[i] = new Train(i);
		}
		money = ruleSet.startingMoney;
		this.cards = cards;
		readyToStart = false;
		readyToEnd = false;
		hasResigned = false;
		this.stats = new Stats();
		this.requestQueue = null;
	}
	
	public void placeTrain(Milepost m, int t) throws GameException{
		if(trains[t] == null) throw new GameException("GameNotStarted");
		else if (trains[t].getLocation() == null) 
			trains[t].moveTrain(m);
		else 
			throw new GameException("TrainAlreadyPlaced");
		log.info("after place");
	}
	
	public void rent(Player trackOwner) {
		spend(4);
		stats.rentalExpense += 4;
		trackOwner.deposit(4);
		trackOwner.stats.rentalIncome += 4;
	}
	
	public void moveTrain(int t, Milepost target, int milepostCount){
		stats.milesTravelled += milepostCount;
		trains[t].moveTrain(target);
	}
	
	public boolean testUpgradeTrain(int t, UpgradeType u){
		if(trains[t] == null) return false;
		switch(u) {
		case SPEED:
			return trains[t].testUpgradeSpeed();
		case CAPACITY:
			return trains[t].testUpgradeLoads();
		default:
			return false;
		}
	}
		
	public void upgradeTrain(int t, UpgradeType u) throws GameException {
		if(!testUpgradeTrain(t, u)) throw new GameException("InvalidUpgrade");
		switch (u) {
			case SPEED:
				trains[t].upgradeSpeed();
				break;
			case CAPACITY:
				trains[t].upgradeLoads();
				break;
		}
	}
	
	public void pickupLoad(int t, String load) throws GameException{ 
		trains[t].addLoad(load); 
	}
	
	public void dropLoad(int t, String load) throws GameException{ 
		trains[t].dropLoad(load); 
	}
	
	/** Delivers a load on the given card. Returns the money to be deposited.
	 * @param index is the location of the card in the player's hand, array-wise
	 * @param next is the card drawn to replace that one
	 */
	public int deliverLoad(int cIndex, int tIndex, Card next) throws GameException {
		Card c = cards[cIndex];
		Trip t = canDeliver(tIndex, c);
		if(t == null) throw new GameException("InvalidDelivery");
		trains[tIndex].dropLoad(t.load);
		cards[cIndex] = next; 
		++stats.deliveryCount;
		stats.deliveryIncome += t.cost;
		return t.cost;
	}
	
	private Trip canDeliver(int ti, Card c){
		if(trains[ti] == null) return null;
		City city = trains[ti].getLocation().city;
		if(city == null) return null;
		for(int i = 0; i < c.trips.length; i ++){
			Trip t = c.trips[i];
			if(trains[ti].containsLoad(t.load) && t.dest.equals(city.name)) return t;
		}
		return null;
	}
	
	public void resign() {
		hasResigned = true;
		readyToEnd = true;
		readyToStart = true;
	}
	
	void deposit(int deposit){ 
		if(deposit < 0) {
			money += deposit; 
			return;
		}
		if(money < 0){
			if((-2) * money > deposit){
				money += deposit / 2;
			} else{
				deposit += 2 * money;
				money = deposit;
			}
		} else {
			money += deposit;
		}
	}
	
	void spend(int withdrawal){
		money -= withdrawal;
	}
	
	/** Queue up a request for later execution */
	public void queueRequest(String request) {
		if (requestQueue == null)
			requestQueue = new RequestQueue();
		requestQueue.requestQueue. add(request);
	}
	
	/** Pop the next queued request to do, if there is one. Returns null otherwise. */
	public String getQueuedRequest() {
		return requestQueue != null ? requestQueue.requestQueue.poll() : null;
	}
	
	public void clearRequestQueue() {
		if (requestQueue != null)
			requestQueue.requestQueue.clear();
	}
		
	public void readyToStart(boolean ready) { readyToStart = ready;}
	
	public boolean readyToStart() { return readyToStart; }
	
	public void readyToEnd(boolean ready) { readyToEnd = ready;}
	
	public boolean readyToEnd() { return readyToEnd; }
	
	public boolean hasResigned() {return hasResigned; }
		
	public String getPid(){ return name; }
		
	public int getMoney() {	return money; }
	
	public Card[] getCards(){ return cards; }
	
	public Train[] getTrains(){ return trains; }
	
	public Train getTrain(int t){ return trains[t]; }
	
	public Stats stats() { 
		stats.money = money;
		return stats; 
		}
	
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
	
	public int getMaxSpeed(int train){
		return trains[train].getSpeed();
	}
	
	public void turnInCards(Card[] cards)  { this.cards = cards; }
	
}
