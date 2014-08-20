package map;

public class Ferry extends Edge {
	
	public Ferry(Milepost dest) {
		super(dest, 3 + Edge.generateCost(dest.type));
	}

}
