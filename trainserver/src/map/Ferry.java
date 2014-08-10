package map;

public class Ferry extends Edge {
	public final Milepost destination;
	public final int cost;
	
	public Ferry(Milepost dest, int cost) {
		super(dest, false, false);
		this.destination = super.destination;
		this.cost = cost;
	}

}
