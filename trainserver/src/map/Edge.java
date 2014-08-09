package map;


public final class Edge {
	public final Milepost destination;
	public final int cost;

	Edge(Milepost source, Milepost destination, boolean river, boolean lake){
		this.destination = destination;
		int temp;
		
		switch (destination.type) {
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
		
		if (river) temp += 2;
		if (lake) temp += 3;
		
		if (source.type == Milepost.Type.BLANK) temp = Integer.MAX_VALUE;
		
		cost = temp;
	}

	// Serialize as a set of json-encoded milepostIds
	public String toString() {
		MilepostId destinationId = new MilepostId(destination.x, destination.y);
		return "{" + "\"destination\":" + destinationId.toString() + "}";
	}
	
}
