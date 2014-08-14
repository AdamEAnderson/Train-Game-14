package player;

import train.GameException;
import map.Milepost;


public class Train {
	private int speed; //12, 16, or 20
	private int capacity; //2 or 3
	private String[] loads; //length the same as the capacity
	private Milepost loc;
	
	public Train(){
		speed = 12;
		capacity = 2;
		loads = new String[2];
		loc = null;
	}
	
	/** Moves the train to the given location.
	 * Does no checks for the legality of the move.
	 */
	void moveTrain(Milepost location)
	{ 
		loc = location;
	}
	
	public void upgradeSpeed() throws GameException {
		if(speed < 20){
			speed += 4;
		} else throw new GameException("InvalidUpgrade");
	}
	
	public void upgradeLoads() throws GameException {
		if(capacity < 3){
			String[] temp = new String[capacity + 1];
			for(int i = 0; i < capacity; i++){
				temp[i] = loads[i];
			}
			loads = temp;
			capacity ++;
		} else throw new GameException("InvalidUpgrade");
	}
	
	/** Returns true if the load was successfully dropped, false 
	 *  otherwise (f'rex, the load was not on the train)
	 * @param load to be dropped
	 */
	public void dropLoad(String load) throws GameException {
		for(int i = 0; i < loads.length; i++){
			if(loads[i] == null ? false : loads[i].equals(load)){
				loads[i] = null;
				return;
			}
		}
		throw new GameException("InvalidLoad");
	}
	
	/** Returns true if the given load was successfully added
	 *  to the train, false otherwise (f'rex the train is not on
	 *  a city with that load available, or the train has no 
	 *  room for an additional load).
	 */
	public void addLoad(String load) throws GameException {
		if(loc.city == null) throw new GameException("CityNotFound");
		else{
			if(loc.city.hasLoad(load)) {
				for(int i = 0; i < loads.length; i++){
					if(loads[i] == null){
						loads[i] = load;
						return;
					}
				}
			}
		}
		throw new GameException("TrainFull");
	}
	
	public int getSpeed(){ return speed;}
	
	public int getCapacity(){ return capacity;}
	
	public String[] getLoads(){	return loads;}
	
	public boolean containsLoad(String l){
		for(int i = 0; i < loads.length; i++){
			if(loads[i] != null && loads[i].equals(l)) return true;
		}
		return false;
	}
	
	public Milepost getLocation(){ return loc;}
}
