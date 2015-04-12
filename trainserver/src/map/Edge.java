package map;


public class Edge {
	public final Milepost destination;
	public final int cost;
	
	Edge(Milepost dest, int cost){
		destination = dest;
		this.cost = cost;
	}

	Edge(Milepost destination, boolean river, boolean lake){
		this.destination = destination;
		int cost = generateCost(destination.type);
		if (river) 
			cost += 2;
		if (lake) 
			cost += 3;
		this.cost = cost;
	}
	
	static int generateCost(Milepost.Type type){
		int temp;
		switch (type) {
			case BLANK:
				temp = Integer.MAX_VALUE;
				break;
			case CHUNNEL:
				temp = 6;
				break;
			case ALPINE:
				temp = 5;
				break;
			case MOUNTAIN:
				temp = 2;
				break;
			case FOREST:
				temp = 2;
				break;
			case JUNGLE:
				temp = 3;
				break;
			case CITY:
				temp = 3;
				break;
			case DESERT:
			case MAJORCITY:
			case NORMAL:
			default:
				temp = 1;
			}
		return temp;
	}

	// Serialize as a set of json-encoded milepostIds
	public String toString() {
//		MilepostId destinationId = new MilepostId(destination.x, destination.y);
		return "{" + "\"destination\":" + destination.id.toString() + "}";
	}
	
}
