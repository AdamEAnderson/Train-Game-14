package map;

public class Ferry extends Edge {
	public final Milepost destination;
	public final int cost;
	
	public Ferry(Milepost dest) {
		super(dest, false, false);
		this.destination = super.destination;
		this.cost = 3 + super.cost;
	}

}
