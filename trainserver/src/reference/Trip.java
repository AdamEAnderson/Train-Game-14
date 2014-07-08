package reference;

public class Trip {
	public final City dest;
	public final String load;
	public final int cost;
	
	public Trip(City dest, String load, int cost){
		this.dest = dest;
		this.load = load;
		this.cost = cost;
	}
}
