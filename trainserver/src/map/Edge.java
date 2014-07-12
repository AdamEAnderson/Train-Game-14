package map;


public final class Edge {
	public final Milepost destination;
	public final int cost;

	Edge(Milepost source, Milepost destination, boolean river, boolean lake){
		this.destination = destination;
		int temp;
		
		switch (destination.type) {
			case ALPINE:
				temp = 5;
				break;
			case MOUNTAIN:
				temp = 2;
				break;
			case BLANK:
				temp = Integer.MAX_VALUE;
				break;
			case DESERT:
				temp = 1;
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
			case MAJORCITY:
				temp = 1;
				break;
			default: 
				temp = 1;
				break;
			}
		
		if (river) temp += 2;
		if (lake) temp += 3;
		
		if (source.type == Milepost.Type.BLANK) temp = Integer.MAX_VALUE;
		
		cost = temp;
	}
	
	/*
	public @Override boolean equals(Object obj){
		if(obj instanceof Edge){
			Edge temp = (Edge) obj;
			if(temp.source)
		}
		return false;
	}*/

	// Serialize as a set of json-encoded milepostIds
	public String toString() {
		MilepostId destinationId = new MilepostId(destination.x, destination.y);
		return "{" + "\"destination\":" + destinationId.toString() + "}";
	}
	
}
