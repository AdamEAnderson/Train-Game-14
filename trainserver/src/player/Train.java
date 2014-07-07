package player;
import reference.City;
import reference.Load;
import map.Milepost;


public class Train {
	private int speed; //12, 16, or 20
	private int capacity; //2 or 3
	private Load[] loads; //length the same as the capacity
	private Milepost loc;
	
	public Train(Milepost location){
		speed = 12;
		capacity = 2;
		loads = new Load[2];
		loc = location;
	}
	
	/** Moves the train to the given location.
	 * Does a check for whether the locations have an edge between
	 * them, but NOT if that edge has been built.
	 */
	boolean moveTrain(Milepost location){
		if (location.isNeighbor(loc)){
			loc = location;
			return true;
		}
		return false;
	}
	
	public void upgradeSpeed() /*throws GameException*/{
		if(speed < 20){
			speed += 4;
		} //else throw gameException
	}
	
	public void upgradeLoads() /*throws GameException*/ {
		if(capacity < 3){
			Load[] temp = new Load[capacity + 1];
			for(int i = 0; i < capacity; i++){
				temp[i] = loads[i];
			}
			loads = temp;
			capacity ++;
		} //else throw gameException
	}
	
	/** Returns true if the load was successfully dropped, false 
	 *  otherwise (f'rex, the load was not on the train)
	 * @param load to be dropped
	 */
	public boolean dropLoad(Load load){
		for(int i = 0; i < loads.length; i++){
			if(loads[i].equals(load)){
				loads[i] = null;
				return true;
			}
		}
		return false;
	}
	
	/** Returns true if the given load was successfully added
	 *  to the train, false otherwise (f'rex the train is not on
	 *  a city with that load available, or the train has no 
	 *  room for an additional load).
	 */
	public boolean addLoad(Load load){
		if(loc.city == null) return false;
		else{
			if(loc.city.hasLoad(load)) {
				for(int i = 0; i < loads.length; i++){
					if(loads[i] == null){
						loads[i] = load;
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public int getSpeed(){
		return speed;
	}
	
	public int getCapacity(){
		return capacity;
	}
	
	public Load[] getLoads(){
		return loads;
	}
	
	public Milepost getLocation(){
		return loc;
	}
}
